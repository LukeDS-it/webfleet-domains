import sbt.Keys._
import sbt._
import scoverage.ScoverageKeys._

/**
 * Default coverage settings
 */
object CoverageSettings {

  lazy val settings: Seq[Def.Setting[_]] = Seq(
    coverageEnabled in (Test, test) := true,
    coverageMinimum := 70,
    coverageFailOnMinimum := false,
    coverageExcludedPackages := {
      val excludedClassRegexes = Seq()
      excludedClassRegexes.mkString(";")
    }
  )
}
