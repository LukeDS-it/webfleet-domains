val akkaVersion = "2.6.5"
val akkaJdbcVersion = "3.5.3"
val akkaHttpVersion = "10.1.12"
val akkaHttpCirceVersion = "1.31.0"
val pureconfigVersion = "0.12.3"
val scalaLoggingVersion = "3.9.2"
val logbackVersion = "1.2.3"
val logstashLogbackEncoderVersion = "5.2"
val scalatestVersion = "3.1.1"
val scalatestMockitoVersion = "1.0.0-M2"
val testcontainersVersion = "1.14.2"
val testcontainersScalaVersion = "0.37.0"
val circeVersion = "0.13.0"
val janinoVersion = "3.1.0"
val postgresqlVersion = "42.2.12"
val slickVersion = "3.3.2"
val webfleetCommonsVersion = "0.1.1"
val slickPgVersion = "0.19.0"
val liquibaseVersion = "3.9.0"
val amqpClientVersion = "5.9.0"

val akkaDependencies = Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
  "com.github.dnvriend" %% "akka-persistence-jdbc" % akkaJdbcVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-persistence-testkit" % akkaVersion % Test,
)

val baseDependencies = Seq(
  "de.heikoseeberger" %% "akka-http-circe" % akkaHttpCirceVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.github.pureconfig" %% "pureconfig" % pureconfigVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "org.codehaus.janino" % "janino" % janinoVersion,
  "net.logstash.logback" % "logstash-logback-encoder" % logstashLogbackEncoderVersion,
  "org.scalatest" %% "scalatest" % scalatestVersion % "test,it",
  "org.scalatestplus" %% "scalatestplus-mockito" % scalatestMockitoVersion,
  "org.testcontainers" % "testcontainers" % testcontainersVersion % "it",
  "com.dimafeng" %% "testcontainers-scala" % testcontainersScalaVersion % "it"
)

val customDependencies = Seq(
  "it.ldsoftware" %% "webfleet-commons" % webfleetCommonsVersion,
  "org.postgresql" % "postgresql" % postgresqlVersion,
  "com.typesafe.slick" %% "slick" % slickVersion,
  "com.github.tminglei" %% "slick-pg" % slickPgVersion,
  "com.auth0" % "jwks-rsa" % "0.8.2",
  "com.auth0" % "java-jwt" % "3.8.1",
  "org.liquibase" % "liquibase-core" % liquibaseVersion,
  "com.rabbitmq" % "amqp-client" % amqpClientVersion,
  "com.dimafeng" %% "testcontainers-scala-postgresql" % testcontainersScalaVersion % "it",
  "org.testcontainers" % "mockserver" % testcontainersVersion % "it",
)

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .enablePlugins(JavaAppPackaging, DockerPlugin, AshScriptPlugin, DockerComposePlugin)
  .settings(Defaults.itSettings)
  .settings(CommonSettings.settings)
  .settings(DockerSettings.settings)
  .settings(ReleaseSettings.settings)
  .settings(ResolverSettings.settings)
  .settings(
    organization := "it.ldsoftware",
    name := "webfleet-domains",
    mainClass in Compile := Some("it.ldsoftware.webfleet.domains.WebfleetDomainsApp"),
    scalaVersion := "2.13.1",
    fork in IntegrationTest := true,
    envVars in IntegrationTest := Map(
      "APP_VERSION" -> git.gitDescribedVersion.value.getOrElse((version in ThisBuild).value)
    ),
    libraryDependencies ++= akkaDependencies ++ baseDependencies ++ customDependencies
  )

addCommandAlias("fullInt", ";clean;compile;integration;")

addCommandAlias("checks", ";scalafmtCheck;clean;compile;")
addCommandAlias("report", ";coverage;test;coverageReport")
addCommandAlias("coverageoff", ";set coverageEnabled := false;")
addCommandAlias("integration", ";docker:publishLocal;it:test;")
