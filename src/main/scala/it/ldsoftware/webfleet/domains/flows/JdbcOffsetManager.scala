package it.ldsoftware.webfleet.domains.flows

import akka.persistence.query.{Offset, Sequence}
import it.ldsoftware.webfleet.commons.flows.OffsetManager
import it.ldsoftware.webfleet.domains.database.ExtendedProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class JdbcOffsetManager(db: Database)(implicit ec: ExecutionContext) extends OffsetManager {
  def writeOffset(consumer: String, offset: Offset): Future[Int] =
    db.run(writeOffsetSql(consumer, offset))

  def getLastOffset(consumer: String): Future[Long] =
    db.run(
        sql"select last_offset from offset_store where consumer_name = $consumer"
          .as[Long]
          .headOption
      )
      .map(_.getOrElse(0L))

  def writeOffsetSql(consumer: String, offset: Offset): DBIO[Int] =
    offset match {
      case Sequence(value) =>
        sqlu"""
        insert into offset_store(consumer_name, last_offset)
         values ($consumer, $value)
          on conflict (consumer_name) do
           update set last_offset = $value
        """
      case _ => throw new IllegalArgumentException(s"unexpected offset $offset")
    }
}
