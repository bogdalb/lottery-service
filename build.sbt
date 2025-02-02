ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.16"

scalacOptions += "-Ymacro-annotations"

root / Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat


lazy val root = (project in file("."))
  .settings(
    name := "Lottery",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % "10.5.0",
      "com.typesafe.akka" %% "akka-stream" % "2.8.0",
      "com.typesafe.slick" %% "slick" % "3.5.0",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.5.0",
      "org.xerial" % "sqlite-jdbc" % "3.48.0.0",
      "io.circe" %% "circe-core" % "0.14.5",
      "io.circe" %% "circe-generic" % "0.14.5",
      "io.circe" %% "circe-parser" % "0.14.5",
      "com.auth0" % "java-jwt" % "4.0.0",
      "org.mindrot" % "jbcrypt" % "0.4",
      "org.quartz-scheduler" % "quartz" % "2.3.2",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
      "ch.qos.logback" % "logback-classic" % "1.5.6",
      "com.github.ben-manes.caffeine" % "caffeine" % "3.1.1",
      "org.scalamock" %% "scalamock" % "5.2.0" % Test,
      "org.scalatest" %% "scalatest" % "3.2.10" % Test,
      "com.typesafe.akka" %% "akka-http-testkit" % "10.5.0" % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.8.0" % Test
    )
  )