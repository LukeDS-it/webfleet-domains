package it.ldsoftware.webfleet.domains.actors.model

import it.ldsoftware.webfleet.commons.service.model.ValidationError
import it.ldsoftware.webfleet.domains.actors.serialization.CborSerializable

case class CreateForm(
    title: String,
    id: String,
    icon: String = "default.png"
) extends CborSerializable {

  def validationErrors: List[ValidationError] =
    List(
      idValidationError
    ).flatten

  private def idValidationError: Option[ValidationError] =
    if (!id.matches("""^[\w\-]*$"""))
      Some(ValidationError("id", "The site id cannot contain symbols except - and _", "id.pattern"))
    else
      None

}
