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
    "repo.repsy.io",
    "ppotseluev",
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
    "-language:postfixOps",
    "-Xfatal-warnings",
    "-deprecation"
  ) ++ (if (isCI) ciScalacOptions else Seq.empty),
  libraryDependencies ++= Seq(
    Dependency.kittens,
    Dependency.munit,
    Dependency.parCollections
  ),
  dependencyOverrides ++= Seq(
    "org.http4s" %% "http4s-core" % "0.23.25"
  ),
  addCompilerPlugin(Dependency.kindProjector),
  evictionWarningOptions in update := EvictionWarningOptions.default
    .withWarnTransitiveEvictions(false)
    .withWarnDirectEvictions(false)
    .withWarnScalaVersionEviction(false),
  assembly / assemblyMergeStrategy := {
    case x if x.contains("io.netty.versions.properties")               => MergeStrategy.concat
    case PathList(ps @ _*) if ps.last endsWith "pom.properties"        => MergeStrategy.first
    case PathList("module-info.class")                                 => MergeStrategy.discard
    case PathList("META-INF", "versions", xs @ _, "module-info.class") => MergeStrategy.discard
    case x =>
      val oldStrategy = (assembly / assemblyMergeStrategy).value
      oldStrategy(x)
  }
)

lazy val root = project
  .in(file("."))
  .settings(name := "it-desk")
  .aggregate(
    `storage`,
    `bots-lib`,
    `core`,
    `api`
  )

lazy val `bots-lib` = project
  .settings(
    name := "bots",
    settings,
    libraryDependencies ++= Seq(
      Dependency.enumeratrum,
      Dependency.kittens,
      Dependency.catsFree,
      Dependency.scalaGraph
    ) ++ Dependency.sttp.all
  )
  .dependsOn(
    `storage`
  )

lazy val `storage` = project
  .settings(
    name := "storage",
    settings,
    libraryDependencies ++= Seq(
      Dependency.pgConnector
    ) ++ Dependency.doobie.all
  )
  .dependsOn(
    `serialization`
  )

lazy val `serialization` = project
  .settings(
    name := "serialization",
    settings,
    libraryDependencies ++= Seq(
    ) ++ Dependency.circe.all
  )

lazy val `core` = project
  .settings(
    name := "core",
    settings,
    libraryDependencies ++= Seq(Dependency.enumeratrum)
  )
  .dependsOn(
    `bots-lib`
  )

lazy val `api` = project
  .settings(
    name := "api",
    settings,
    libraryDependencies ++= Seq(
      Dependency.enumeratrum,
      Dependency.scalaLogging,
      Dependency.logback
    ) ++ Dependency.httpServer.all ++ Dependency.pureconfig.all,
    assembly / mainClass := Some("com.github.ppotseluev.itdesk.api.Main")
  )
  .dependsOn(
    `core`
  )
