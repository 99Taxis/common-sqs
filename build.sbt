import Dependencies._

lazy val akkaVersion = "2.5.6"

lazy val TestAndIntegration = s"it,${Test}"

lazy val root = (project in file(".")).
  configs(IntegrationTest).
  settings(
    Defaults.itSettings,
    inThisBuild(List(
      organization := "com.taxis99",
      scalaVersion := "2.12.3",
      version      := "0.3.4"
    )),
    name := "common-sqs",
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    crossScalaVersions := Seq("2.11.11", "2.12.3"),
    libraryDependencies ++= Seq(
      "javax.inject"       % "javax.inject"             % "1",
      "com.amazonaws"      % "aws-java-sdk-sqs"         % "1.11.225",
      "com.typesafe"       % "config"                   % "1.3.1" % Provided,
      "com.typesafe.play"  %% "play-json"               % "2.6.2" % Provided,
      "com.lightbend.akka" %% "akka-stream-alpakka-sns" % "0.14",
      "com.lightbend.akka" %% "akka-stream-alpakka-sqs" % "0.14",
      "com.github.xuwei-k" %% "msgpack4z-core"          % "0.3.7",
      "com.github.xuwei-k" %% "msgpack4z-play"          % "0.5.1",
      "com.github.xuwei-k" %% "msgpack4z-native"        % "0.3.3",
      "org.elasticmq"      %% "elasticmq-server"        % "0.13.8",
      "com.typesafe.akka"  %% "akka-stream"             % akkaVersion,
      "com.typesafe.akka"  %% "akka-stream-testkit"     % akkaVersion % TestAndIntegration,
      "com.typesafe.akka"  %% "akka-testkit"            % akkaVersion % TestAndIntegration,
      scalaTest % TestAndIntegration
    ),
    dependencyOverrides += "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    fork in Test := true,
    fork in IntegrationTest := true,
    javaOptions ++= Seq("-Xms512M", "-Xmx2048M", "-XX:+CMSClassUnloadingEnabled")
  )
