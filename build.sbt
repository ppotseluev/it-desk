val isCI = sys.env.get("CI").contains("true")

val ciScalacOptions = Seq(
  "-Wunused:imports"
)

lazy val settings = Seq(
  resolvers += "jitpack" at "https://jitpack.io",
  organization := "com.github.ppotseluev",
  version := "1.0-SNAPSHOT",
  scalaVersion := "2.13.8",
  Compile / scalaSource := baseDirectory.value / "src/main/scala",
  Test / scalaSource := baseDirectory.value / "src/test/scala",
  ThisBuild / scalafixDependencies += Dependency.organizeImports,
  ThisBuild / semanticdbEnabled := true,
  ThisBuild / semanticdbVersion := scalafixSemanticdb.revision,
  ThisBuild / credentials += Credentials(
    "Repsy Managed Repository",
    "repo.repsy.io", "ppotseluev",
    sys.env.getOrElse("REPSY_PWD", "UNDEFINED")
  ),
  ThisBuild / resolvers ++= List(
    Resolver.mavenLocal,
    "Repsy Managed Repository" at "https://repo.repsy.io/mvn/ppotseluev/default"
  ),
  useCoursier := false,
  scalacOptions := Seq(
    "-target:jvm-17",
    "-Ymacro-annotations",
    "-language:higherKinds",
    "-Xfatal-warnings",
    "-deprecation"
  ) ++ (if (isCI) ciScalacOptions else Seq.empty),
  libraryDependencies ++= Seq(
    Dependency.kittens,
    Dependency.munit,
    Dependency.parCollections
  ),
  addCompilerPlugin(Dependency.kindProjector),
  assembly / assemblyMergeStrategy := {
    case x if x.contains("io.netty.versions.properties") => MergeStrategy.concat
    case PathList(ps@_*) if ps.last endsWith "pom.properties" => MergeStrategy.first
    case PathList("module-info.class") => MergeStrategy.discard
    case PathList("META-INF", "versions", xs@_, "module-info.class") => MergeStrategy.discard
    case x =>
      val oldStrategy = (assembly / assemblyMergeStrategy).value
      oldStrategy(x)
  }
)

lazy val root = project
  .in(file("."))
  .settings(name := "it-desk")
  .aggregate(
    `model`,
    `app`
  )

lazy val `model` = project
  .settings(
    name := "model",
    settings,
    libraryDependencies ++= Seq(
      Dependency.enumeratrum
    )
  )

lazy val `app` = project
  .settings(
    name := "app",
    settings,
    libraryDependencies ++= Seq(
      Dependency.enumeratrum,
      Dependency.botgen
    ),
//    assembly / mainClass := Some("com.github.ppotseluev.algorate.trader.app.AkkaTradingApp") TODO
  )