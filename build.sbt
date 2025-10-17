ThisBuild / organization := "org.example"
ThisBuild / scalaVersion := "2.13.16"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "cats-effect-3-quick-start",

    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.13.0", // latest compatible for Scala 2.13
      "org.typelevel" %% "cats-effect-kernel_sjs1" % "3.7-4972921",
      "org.typelevel" %% "cats-effect" % "3.7-4972921",
      "org.typelevel" %% "cats-effect-std" % "3.7-4972921",
      "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test
    ),

    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
  )
