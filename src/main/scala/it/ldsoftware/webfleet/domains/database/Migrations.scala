package it.ldsoftware.webfleet.domains.database

import java.sql.Connection

import com.typesafe.scalalogging.LazyLogging
import it.ldsoftware.webfleet.domains.database.Migrations._
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.{Contexts, LabelExpression, Liquibase}

class Migrations(connection: Connection) extends LazyLogging {

  private val database = DatabaseFactory
    .getInstance()
    .findCorrectDatabaseImplementation(new JdbcConnection(connection))

  private val liquibase =
    new Liquibase(ChangelogFile, new ClassLoaderResourceAccessor(), database)

  def executeMigration(): Unit = {
    val (contexts, expression) = (new Contexts(), new LabelExpression())
    liquibase.update(contexts, expression)
    connection.close()
  }

}

object Migrations {
  val ChangelogFile = "migrations/0-migrations-index.xml"
}
