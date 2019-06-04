# This build uses SBT to create jars.
FROM openjdk:8u212-jdk-stretch as builder

# Install sbt
WORKDIR /sbt
RUN wget https://piccolo.link/sbt-1.2.8.tgz && tar xzvf sbt-1.2.8.tgz
ENV PATH="/sbt/sbt/bin:${PATH}"

WORKDIR /local
COPY build.sbt /local/build.sbt
COPY project /local/project
COPY allenai /local/allenai
COPY corenlp /local/corenlp
COPY main /local/main
COPY modelscorenlp /local/modelscorenlp
COPY modelsmain /local/modelsmain

# use the stage pluging to place the jars and executables in target/universal/stage
RUN sbt allenai/update
RUN sbt allenai/compile
RUN sbt allenai/stage

# This build copies over the jars and sets up the path to run the application.
FROM openjdk:8u212-jdk-stretch

WORKDIR /local

COPY --from=builder /local/allenai/target/universal/stage/ /local

ENTRYPOINT ["/local/bin/processors-allenai"]
