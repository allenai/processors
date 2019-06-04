name := "processors-allenai"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.19",
  "com.typesafe.akka" %% "akka-stream" % "2.5.19",
  "com.typesafe.akka" %% "akka-http" % "10.1.6",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.6",
  "io.spray" %%  "spray-json" % "1.3.5"
)

//mainClass in Compile := Some("org.allenai.odin.OdinService")