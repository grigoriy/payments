name := "payments"

version := "0.1"

scalaVersion := "2.12.8"

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused"
)

mainClass in assembly := Some("com.galekseev.payments.Main")

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalatest" %% "scalatest" % "3.0.7" % Test,
  "org.scalacheck" %% "scalacheck" % "1.14.0" % Test
)

coverageMinimum := 100
coverageFailOnMinimum := true
coverageExcludedPackages := "com\\.galekseev\\.payments\\.Main"

scalastyleFailOnWarning := true
wartremoverErrors ++= Warts.unsafe
