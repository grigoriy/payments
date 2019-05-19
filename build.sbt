lazy val akkaHttpVersion = "10.1.8"
lazy val akkaVersion = "2.5.22"

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    name := "payments",
    version := "0.1",
    scalaVersion := "2.12.8",
    scalacOptions ++= Seq(
      "-target:jvm-1.8",
      "-encoding", "UTF-8",
      "-unchecked",
      "-deprecation",
      "-feature",
      "-Xfatal-warnings",
      "-Xfuture",
      "-Yno-adapted-args",
      "-Ypartial-unification",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Ywarn-unused"
    ),
    mainClass in assembly := Some("com.galekseev.payments.Main"),
    Defaults.itSettings,
    libraryDependencies ++= Seq(
      "com.typesafe.akka"           %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka"           %% "akka-stream"          % akkaVersion,
      "com.typesafe.play"           %% "play-json"            % "2.7.3",
      "de.heikoseeberger"           %% "akka-http-play-json"  % "1.25.2",
      "eu.timepit"                  %% "refined"              % "0.9.5",
      "org.julienrf"                %% "play-json-derived-codecs" % "5.0.0",
      "com.typesafe"                 % "config"               % "1.3.4",
      "com.typesafe.scala-logging"  %% "scala-logging"        % "3.9.2",
      "ch.qos.logback"               % "logback-classic"      % "1.2.3",

      "com.typesafe.akka"           %% "akka-http-testkit"    % akkaHttpVersion % "test,it",
      "com.typesafe.akka"           %% "akka-testkit"         % akkaVersion     % "test,it",
      "com.typesafe.akka"           %% "akka-stream-testkit"  % akkaVersion     % "test,it",
      "org.scalatest"               %% "scalatest"            % "3.0.7"         % "test,it",
      "org.scalacheck"              %% "scalacheck"           % "1.14.0"        % "test,it"
    ),

    coverageMinimum := 95,
    coverageFailOnMinimum := true,
    coverageExcludedPackages := "com\\.galekseev\\.payments\\.Main",

    scalastyleFailOnWarning := true,
    wartremoverErrors in (Compile, compile) ++= Warts.unsafe
  )
