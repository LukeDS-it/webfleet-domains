import com.tapad.docker.DockerComposePlugin.autoImport.{dockerImageCreationTask, variablesForSubstitution}
import com.typesafe.sbt.SbtGit.git
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}
import com.typesafe.sbt.packager.linux.LinuxKeys
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import sbt.Keys._
import sbt._

object DockerSettings extends LinuxKeys {

  val containerHttpProxy: Seq[String] =
    sys.env
      .get("HTTP_PROXY")
      .orElse(sys.env.get("http_proxy"))
      .map(proxy => Seq("--build-arg", s"http_proxy=$proxy"))
      .toSeq
      .flatten
  val containerHttpsProxy: Seq[String] =
    sys.env
      .get("HTTPS_PROXY")
      .orElse(sys.env.get("https_proxy"))
      .map(proxy => Seq("--build-arg", s"https_proxy=$proxy"))
      .toSeq
      .flatten

  lazy val settings: Seq[Def.Setting[_]] = Seq(
    // Copy files contained in scripts directory into target.
    // These files are used during the test phase.
    unmanagedResourceDirectories in Test += baseDirectory.value / "scripts",
    javaOptions in Universal += "-Dpidfile.path=/dev/null",
    dockerBaseImage := "openjdk:8-jre-alpine",
    dockerRepository := Some("index.docker.io"),
    dockerUpdateLatest := false,
    daemonUser in Docker := "daemon",
    dockerLabels := Map(
      "BuildBranch" -> git.gitCurrentBranch.value,
      "BuildCommit" -> git.gitHeadCommit.value.getOrElse(""),
      "ServiceName" -> sys.env.getOrElse("SERVICE_NAME", "akka-service")
    ),
    mappings in Docker ++= {
      // copy all files into scripts to /opt/docker
      val scriptDir = baseDirectory.value / "scripts"
      val targetScriptDir = "/opt/docker/"
      scriptDir.listFiles.toSeq map { srcFile =>
        srcFile -> (targetScriptDir + srcFile.name)
      }
    },
    dockerExposedPorts := Seq(8080),
    dockerBuildOptions ++= containerHttpProxy ++ containerHttpsProxy,
    dockerCommands := {
      dockerCommands.value.flatMap {
        case eq @ Cmd("FROM", _) =>
          Seq(
            Some(eq),
            Some(Cmd("RUN", "apk add --no-cache bash"))
          ).flatten
        case ep @ ExecCmd("ENTRYPOINT", _*) =>
          Seq(
            Cmd("RUN", "chmod +x /opt/docker/docker-entrypoint.sh"),
            ExecCmd("ENTRYPOINT", "/opt/docker/docker-entrypoint.sh" :: ep.args.toList: _*)
          )
        case other => Seq(other)
      }
    },
    dockerImageCreationTask := (publishLocal in Docker).value,
    variablesForSubstitution := Map(
      "IP_HOST" -> sys.env.getOrElse("IP_DOCKER_HOST", java.net.InetAddress.getLocalHost.getHostAddress)
    ),
  )

}
