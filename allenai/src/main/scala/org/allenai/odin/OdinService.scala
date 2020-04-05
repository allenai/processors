package org.allenai.odin

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._
import akka.http.scaladsl.model.StatusCodes._
import DefaultJsonProtocol._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.clulab.serialization.json._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import org.clulab.processors.corenlp.CoreNLPProcessor
import scala.concurrent.duration._

object OdinService extends App {
  implicit val system: ActorSystem = ActorSystem("odin-wrapper")
  implicit val executor: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  final case class TextDocument(text: String)
  final case class TokenizedDocument(tokens: List[List[String]])
  final case class ExtractInput(docJson: String, extractorId: String)
  final case class CreateExtractorInput(rulesFile: String, rules: String)
  implicit val TextDocumentFormat: RootJsonFormat[TextDocument] = jsonFormat1(TextDocument)
  implicit val TokenizedDocumentFormat: RootJsonFormat[TokenizedDocument] = jsonFormat1(TokenizedDocument)


  val sConfig = ConfigFactory.load().getConfig("OdinService")
  val corefMaxSentDist = sConfig.getInt("coref.maxSentDistance")
  println(s"coref.maxSentDistance ${corefMaxSentDist}")
  val proc = new CoreNLPProcessor(corefMaxSentDistance=corefMaxSentDist)

  val host = "0.0.0.0"
  var port = if (args.length > 0) args(0).toInt else 9090

  def getListOfFiles(dir: File, extensions: List[String]): List[File] = {
    dir.listFiles.filter(_.isFile).toList.filter { file =>
      extensions.exists(file.getName.endsWith(_))
    }
  }

  /**
    * Annotate a raw text document with linguistic annotations using the Odin default pipeline. (CoreNLP)
    */
  val annotateTextRoute: Route =

    post {
      path("annotate-text") {
        withRequestTimeout(600.seconds) {
          entity(as[TextDocument]) { doc =>
            // annotate the document using the default linguistic annotation pipeline.
            val annotatedDoc = proc.annotate(doc.text, keepText = true)
            complete(HttpEntity(ContentTypes.`application/json`, annotatedDoc.json()));
          }
        }
      }

    }

  /**
    * Annotate a tokenized document with linguistic annotations using the Odin default pipeline. (CoreNLP)
    */
  val annotateTokensRoute: Route =

    post {
      path("annotate-tokens") {
        withRequestTimeout(600.seconds) {
          entity(as[TokenizedDocument]) { doc =>
            // annotate the document using the default linguistic annotation pipeline.
            val annotatedDoc = proc.annotateFromTokens(doc.tokens, keepText = true)
            complete(HttpEntity(ContentTypes.`application/json`, annotatedDoc.json()));
          }
        }
      }

    }

  val healthRoute =
    get {
      path("health") {
        complete(OK)
      }
    }

  val infoRoute =
    get {
      path("info") {
        val infoResponseString = if (System.getenv().containsKey("GIT_SHA")) {
          val git_sha = System.getenv("GIT_SHA")
          "{\"sha\":\"" + git_sha + "\"}"
        } else "{}"

        complete(HttpEntity(ContentTypes.`application/json`, infoResponseString))
      }
    }

  val bindingFuture = Http().bindAndHandle(annotateTextRoute ~ annotateTokensRoute ~ healthRoute ~ infoRoute, host, port)

  bindingFuture.onComplete {
    case Success(serverBinding) => println(s"listening to ${serverBinding.localAddress}")
    case Failure(error) => println(s"error: ${error.getMessage}")
  }
}
