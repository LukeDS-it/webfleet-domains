package it.ldsoftware.webfleet.domains.http.utils

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.Credentials
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import it.ldsoftware.webfleet.domains.http.model.out.RestError
import it.ldsoftware.webfleet.domains.security.User
import it.ldsoftware.webfleet.domains.service.model._

import scala.concurrent.Future

trait RouteHelper extends LazyLogging with FailFastCirceSupport with Directives {

  type Mapper[T, R] = T => R

  def Identity[T]: Mapper[T, T] = x => x

  val extractor: UserExtractor

  def svcCall[T](serviceCall: => Future[ServiceResult[T]])(
      implicit marshaller: ToEntityMarshaller[T]
  ): Route = svcCall[T, T](serviceCall, Identity)

  def svcCall[T, R](serviceCall: => Future[ServiceResult[T]], mapper: Mapper[T, R])(
      implicit marshaller: ToEntityMarshaller[R]
  ): Route =
    onSuccess(serviceCall) {
      case Right(result) => handleSuccess(result, mapper)
      case Left(error)   => handleFailure(error)
    }

  def handleSuccess[T, R](result: ServiceSuccess[T], mapper: T => R)(
      implicit marshaller: ToEntityMarshaller[R]
  ): Route = result match {
    case Success(result) => complete(mapper(result))
    case Created(path)   => complete(StatusCodes.Created -> List(Location(path)))
    case NoOutput        => complete(StatusCodes.NoContent)
  }

  def handleFailure(failure: ServiceFailure): Route = failure match {
    case Invalid(errors) => complete(StatusCodes.BadRequest -> errors)
    case NotFound(searched) =>
      complete(
        StatusCodes.NotFound -> RestError(s"Requested resource $searched could not be found")
      )
    case UnexpectedError(th, message) =>
      logger.error("An unexpected error has happened", th)
      complete(StatusCodes.InternalServerError -> RestError(message))
    case ForbiddenError             => complete(StatusCodes.Forbidden)
    case ServiceUnavailable(status) => complete(StatusCodes.ServiceUnavailable -> status)
  }

  def authenticator(credentials: Credentials): Option[User] = credentials match {
    case Credentials.Missing              => None
    case Credentials.Provided(identifier) => extractor.extractUser(identifier)
  }

  val rejectionHandler: RejectionHandler = RejectionHandler
    .newBuilder()
    .handle {
      case AuthenticationFailedRejection(_, _) => complete(StatusCodes.Unauthorized)
    }
    .result()

  def login(proceed: User => Route): Route = handleRejections(rejectionHandler) {
    authenticateOAuth2("realm", authenticator) { user => proceed(user) }
  }

}
