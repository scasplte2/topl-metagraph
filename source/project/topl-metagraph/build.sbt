import Dependencies.*
import sbt.*
import sbt.Keys.*

ThisBuild / organization := "xyz.kd5ujc"
ThisBuild / scalaVersion := "2.13.10"
ThisBuild / evictionErrorLevel := Level.Warn

ThisBuild / assemblyMergeStrategy := {
  case "logback.xml" => MergeStrategy.first
  case x if x.contains("io.netty.versions.properties") => MergeStrategy.discard
  case PathList("xyz", "kd5ujc", "buildinfo", xs @ _*) => MergeStrategy.first
  case PathList(xs@_*) if xs.last == "module-info.class" => MergeStrategy.first
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

lazy val commonSettings = Seq(
  scalacOptions ++= List("-Ymacro-annotations", "-Yrangepos", "-Wconf:cat=unused:info", "-language:reflectiveCalls"),
  resolvers += Resolver.mavenLocal,
  resolvers += Resolver.githubPackages("abankowski", "http-request-signer"),
) ++ Defaults.itSettings

lazy val commonLibraryDependencies: Seq[ModuleID] = Seq(
  CompilerPlugin.kindProjector,
  CompilerPlugin.betterMonadicFor,
  CompilerPlugin.semanticDB,
  Libraries.tessellationNodeShared,
  Libraries.cats,
  Libraries.catsEffect,
  Libraries.scalapbCirce,
  Libraries.scalapbCirceMacro,
  Libraries.scalapbRuntime,
  Libraries.scalapbValidate,
  Libraries.pureconfigCore,
  Libraries.pureconfigCats,
  Libraries.toplBrambl,
  Libraries.toplServiceKit
)

lazy val commonTestSettings = Seq(
  testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
  libraryDependencies ++= Seq(
    Libraries.weaverCats,
    Libraries.weaverDiscipline,
    Libraries.weaverScalaCheck,
    Libraries.catsEffectTestkit
  ).map(_ % Test)
)

lazy val buildInfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    scalaVersion,
    sbtVersion
  ),
  buildInfoPackage := "xyz.kd5ujc.buildinfo"
)

lazy val root = (project in file("."))
  .settings(
    name := "topl-metagraph"
  ).aggregate(sharedData, currencyL0, currencyL1, dataL1)

lazy val sharedData = (project in file("modules/shared-data"))
  .enablePlugins(AshScriptPlugin, BuildInfoPlugin, JavaAppPackaging)
  .settings(
    buildInfoSettings,
    commonSettings,
    commonTestSettings,
    name := "topl-metagraph-shared-data",
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb",
      scalapb.validate.gen() -> (Compile / sourceManaged).value / "scalapb"
    ),
    libraryDependencies ++= commonLibraryDependencies
  )

lazy val currencyL0 = (project in file("modules/l0"))
  .enablePlugins(AshScriptPlugin, BuildInfoPlugin, JavaAppPackaging)
  .dependsOn(sharedData)
  .settings(
    buildInfoSettings,
    commonSettings,
    commonTestSettings,
    name := "topl-metagraph-currency-l0",
    libraryDependencies ++= (commonLibraryDependencies ++ Seq(Libraries.tessellationCurrencyL0))
  )

lazy val currencyL1 = (project in file("modules/l1"))
  .enablePlugins(AshScriptPlugin, BuildInfoPlugin, JavaAppPackaging)
  .dependsOn(sharedData)
  .settings(
    buildInfoSettings,
    commonSettings,
    commonTestSettings,
    name := "topl-metagraph-currency-l1",
    libraryDependencies ++= (commonLibraryDependencies ++ Seq(Libraries.tessellationCurrencyL1))
  )

lazy val dataL1 = (project in file("modules/data_l1"))
  .enablePlugins(AshScriptPlugin, BuildInfoPlugin, JavaAppPackaging)
  .dependsOn(sharedData)
  .settings(
    buildInfoSettings,
    commonSettings,
    commonTestSettings,
    name := "topl-metagraph-data-l1",
    libraryDependencies ++= (commonLibraryDependencies ++ Seq(Libraries.tessellationCurrencyL1))
  )
