import sbt._
import sbt.Keys._
import sbt.librarymanagement._
import com.typesafe.sbt.GitPlugin.autoImport.git
import sbtrelease.ReleasePlugin.autoImport.releaseCommitMessage
import com.typesafe.sbt.SbtNativePackager.autoImport.packageName
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._

object ReleaseSettings extends LibraryManagementSyntax {

  lazy val settings = Seq(
    packageName in Docker := sys.env.getOrElse("DOCKER_IMAGE_NAME", name.value),
    dockerAlias := DockerAlias(
      dockerRepository.value,
      dockerUsername.value,
      packageName.value,
      Option(git.gitDescribedVersion.value.getOrElse((version in ThisBuild).value))
    ),
    packagedArtifacts := Map.empty,
    publish := (Docker / publish).value,
    publishMavenStyle := false,
    pomIncludeRepository := { _ => false },
    publishArtifact := false,
    publishTo := Some("releases" at "https://example.com/"), // Needs to be set by sbt-release plugin
    releaseCommitMessage := s"Setting version to ${(version in ThisBuild).value} [ci-skip]"
  )
}
