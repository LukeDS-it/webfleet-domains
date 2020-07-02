import sbt.Keys._
import sbt._

object ResolverSettings {
  lazy val settings = Seq(
    resolvers += Resolver.bintrayRepo("lukeds-it", "maven")
  )
}
