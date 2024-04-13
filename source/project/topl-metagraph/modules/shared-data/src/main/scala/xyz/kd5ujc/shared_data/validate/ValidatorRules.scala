package xyz.kd5ujc.shared_data.validate

import cats.implicits.catsSyntaxValidatedIdBinCompat0
import co.topl.brambl.models.transaction.IoTransaction
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import org.tessellation.currency.dataApplication.DataApplicationValidationError
import org.tessellation.currency.dataApplication.dataApplication.DataApplicationValidationErrorOr
import scalapb.validate.{Failure, Success, Validator}
import xyz.kd5ujc.shared_data.lib.Models.ToplMetagraphUpdate

object ValidatorRules {

  /**
   * Uses the Validator typeclass on the generated instance to validate the data
   *
   * @param update instance of event data to check against the scalapb-validators
   * @return returns an error if the instance validation fails, otherwise Unit if validation succeeds
   */
  def isValidUpdateData(update: ToplMetagraphUpdate): DataApplicationValidationErrorOr[Unit] =
    Validator[IoTransaction].validate(update.data) match {
      case Success             => ().validNec
      case Failure(violations) => Errors.FailedToDecodeEventData(violations.toString()).invalidNec
    }

  object Errors {

    @derive(decoder, encoder)
    case class InvalidFieldSize(fieldName: String, maxSize: Long) extends DataApplicationValidationError {
      val message = s"Invalid field size: $fieldName, maxSize: $maxSize"
    }

    @derive(decoder, encoder)
    case class FailedToDecodeEventData(violations: String) extends DataApplicationValidationError {
      val message = s"Failed to decode data with: ${violations}"
    }

    @derive(decoder, encoder)
    case object RequiredNonzeroFieldContainsZero extends DataApplicationValidationError {
      val message = s"A required non-zero field(s) found to contain zero"
    }

    @derive(decoder, encoder)
    case object RecordAlreadyExists extends DataApplicationValidationError {
      val message = s"Failed to create event, previous record found."
    }

    @derive(decoder, encoder)
    case object RecordDoesNotExist extends DataApplicationValidationError {
      val message = s"Failed to create event, no previous record found."
    }

    @derive(decoder, encoder)
    case object UpdateFailsToAdvanceMessageCounter extends DataApplicationValidationError {
      val message = s"Failed to create event, message counter is not advanced."
    }
  }
}
