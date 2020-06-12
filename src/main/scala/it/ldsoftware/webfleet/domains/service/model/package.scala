package it.ldsoftware.webfleet.domains.service

import it.ldsoftware.webfleet.domains.actors.model.ValidationError

package object model {
  type ServiceResult[T] = Either[ServiceFailure, ServiceSuccess[T]]

  def success[T](result: T): ServiceResult[T] = Right(Success(result))
  def created(path: String): ServiceResult[String] = Right(Created(path))
  def noOutput: ServiceResult[NoResult] = Right(NoOutput)

  def notFound[T](searched: String): ServiceResult[T] = Left(NotFound(searched))
  def invalid[T](errors: List[ValidationError]): ServiceResult[T] = Left(Invalid(errors))
  def unexpectedError[T](th: Throwable, message: String): ServiceResult[T] =
    Left(UnexpectedError(th, message))
  def forbidden[T]: ServiceResult[T] = Left(ForbiddenError)
  def serviceUnavailable(t: ApplicationHealth): ServiceResult[ApplicationHealth] =
    Left(ServiceUnavailable(t))
}
