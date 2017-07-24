import Dependencies._

lazy val akkaVersion = "2.5.3"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.taxis99",
      scalaVersion := "2.12.2",
      version      := "1.0.0-SNAPSHOT"
    )),
    name := "common-sqs",
    libraryDependencies ++= Seq(
      "javax.inject"       % "javax.inject"             % "1",
      "com.amazonaws"      %  "aws-java-sdk-sqs"        % "1.11.140",
      "com.typesafe"       % "config"                   % "1.3.1",
      "com.typesafe.play"  %% "play-json"               % "2.6.2",
      "com.typesafe.akka"  %% "akka-stream"             % akkaVersion,
      "com.typesafe.akka"  %% "akka-stream-testkit"     % akkaVersion,
      "com.lightbend.akka" %% "akka-stream-alpakka-sns" % "0.10",
      "com.lightbend.akka" %% "akka-stream-alpakka-sqs" % "0.10",
      "com.github.xuwei-k" %% "msgpack4z-play"          % "0.5.1",
      "org.elasticmq"      %% "elasticmq-server"        % "0.13.8",
      "com.iheart"         %% "ficus"                   % "1.4.1",
      "com.typesafe.akka"  %% "akka-testkit"            % akkaVersion % Test,
      scalaTest % Test
    ),
    resolvers += "velvia maven" at "http://dl.bintray.com/velvia/maven",
    fork in Test := true,
    javaOptions ++= Seq("-Xms512M", "-Xmx2048M", "-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled")
  )
