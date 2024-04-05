import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.dockerBaseImage

import scala.sys.process.*
import scala.sys.process.ProcessLogger

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.13"

val tapirVersion = "1.10.0"
val sttpVersion = "3.9.2"

addCommandAlias("startAll", "server/reStart;frontend/fastLinkJS")

val npmBuild = taskKey[Unit]("builds es modules with `npm build`")

lazy val npmBin =
  if (scala.util.Properties.isWin) "npm.cmd"
  else "npm"


val commonDependencies = Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-sttp-client" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % tapirVersion
)

lazy val common = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("common"))
  .settings(
    libraryDependencies ++= commonDependencies
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.5.0" // implementations of java.time classes for Scala.JS,
    )
  )


val serverDependencies = commonDependencies ++ Seq(
  // server dependencies
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-sttp-client" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-jdkhttp-server" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-files" % tapirVersion,

  "ch.qos.logback" % "logback-classic" % "1.4.14",
)

lazy val root = project
  .in(file("."))
  .aggregate(
    common.jvm,
    common.js,
    server,
    frontend
  )

lazy val server = (project in file("server"))
  .settings(
    Compile / mainClass := Some("com.mlynik.Main"),
    name := "scala-laminar-tapir",
    libraryDependencies ++= serverDependencies,
    watchSources ++= (frontend / watchSources).value,
    Compile / products += (frontend / baseDirectory).value / "dist",
    fullLinkJS / reStart := reStart.dependsOn(frontend / Compile / fullLinkJS / npmBuild).evaluated,
    Universal / packageBin := (Universal / packageBin)
      .dependsOn(frontend / Compile / fullLinkJS / npmBuild)
      .value,
    reStart / javaOptions += "-Xmx512m",
    dockerExposedPorts ++= Seq(9000),
    dockerBaseImage := "amazoncorretto:17-alpine",
    dockerUpdateLatest := true,
  )
  .dependsOn(common.jvm)
  .enablePlugins(JavaAppPackaging, AshScriptPlugin, DockerPlugin)

lazy val frontend = (project in file("frontend"))
  .settings(
    externalNpm := {
      Process(npmBin, baseDirectory.value.getParentFile) ! ProcessLogger(line => ())
      baseDirectory.value.getParentFile
    },
    npmBuild := {
      Process(s"$npmBin build").!
    },
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %%% "tapir-sttp-client" % tapirVersion,
      "com.softwaremill.sttp.tapir" %%% "tapir-json-zio" % tapirVersion,
      "dev.zio"                       %%% "zio-json"          % "0.4.2",
      "io.frontroute" %%% "frontroute" % "0.18.1" // Brings in Laminar 16
    ),
    semanticdbEnabled := true,
    autoAPIMappings := true,
    scalaJSUseMainModuleInitializer := true,
    Compile / mainClass := Some("com.mlynik.App")
  )
  .enablePlugins(
    ScalaJSPlugin,
    ScalablyTypedConverterExternalNpmPlugin
  )
  .dependsOn(common.js)
