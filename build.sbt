ThisBuild / organization := "org.example"
ThisBuild / scalaVersion := "2.13.16"
ThisBuild / version := "0.1.0-SNAPSHOT"

val catsCore   = "2.13.0"
val catsEffect = "3.5.4"
val circe      = "0.14.10"
val ziov        = "2.1.12"
val zioHttp    = "3.5.1"
val munitCE    = "1.0.7"

lazy val root = (project in file("."))
  .settings(
    name := "cats-effect-3-quick-start",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core"           % catsCore,
      "org.typelevel" %% "cats-effect-kernel"  % catsEffect,
      "org.typelevel" %% "cats-effect"         % catsEffect,
      "org.typelevel" %% "cats-effect-std"     % catsEffect,
      "io.circe"      %% "circe-core"          % circe,
      "io.circe"      %% "circe-parser"        % circe,
      "io.circe"      %% "circe-generic"       % circe,
      "dev.zio"       %% "zio"                 % ziov,
      "dev.zio"       %% "zio-http"            % zioHttp,
      "org.typelevel" %% "munit-cats-effect-3" % munitCE % Test
    ),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
  )