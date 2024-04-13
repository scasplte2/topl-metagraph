package xyz.kd5ujc.data_l1

import cats.effect.Async
import cats.implicits.{toFlatMapOps, toFunctorOps}
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import org.http4s.HttpRoutes
import org.tessellation.currency.dataApplication.{DataApplicationValidationError, L1NodeContext}
import scalapb_circe.codec._
import xyz.kd5ujc.data_l1.L1NodeContext.syntax._
import xyz.kd5ujc.shared_data.MetagraphPublicRoutes
import xyz.kd5ujc.shared_data.lib.Models.{Account, EventRecord}

class DataL1CustomRoutes[F[_]: Async](implicit context: L1NodeContext[F]) extends MetagraphPublicRoutes[F] {

  protected val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "event" / "record" / "all" =>
      getAllEventRecords.flatMap(prepareResponse(_))

    case GET -> Root / "snapshot" / "global" / "latest" =>
      context.getLatestGlobalSnapshot.flatMap(prepareResponse(_))

    case GET -> Root / "snapshot" / "currency" / "latest" =>
      context.getLatestCurrencySnapshot.flatMap(prepareResponse(_))
  }

  private def getAllEventRecords: F[Either[DataApplicationValidationError, List[(Account, List[EventRecord])]]] =
    context.getOnChainState.map(_.map(_.records.toList))

}

object DataL1CustomRoutes {

  object Errors {

    @derive(decoder, encoder)
    case class EventIdNotFound(eventId: String) extends DataApplicationValidationError {
      val message: String = s"Event ID $eventId was not found"
    }

    @derive(decoder, encoder)
    case class EventRecordNotFound(eventId: String, nonce: Long) extends DataApplicationValidationError {
      val message: String = s"Event Record with ID $eventId and nonce $nonce was not found"
    }
  }
}
