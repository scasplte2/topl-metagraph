package xyz.kd5ujc.metagraph_l0

import cats.effect.Async
import cats.syntax.all._
import xyz.kd5ujc.metagraph_l0.L0NodeContext.syntax.L0NodeContextOps
import xyz.kd5ujc.shared_data.MetagraphPublicRoutes
import org.http4s.HttpRoutes
import org.tessellation.currency.dataApplication.L0NodeContext
import org.tessellation.node.shared.ext.http4s.SnapshotOrdinalVar
import scalapb_circe.codec._

class ML0CustomRoutes[F[_]: Async](implicit context: L0NodeContext[F]) extends MetagraphPublicRoutes[F] {

  protected val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "snapshot" / "currency" / "latest" =>
      context.getLatestCurrencySnapshot.flatMap(prepareResponse(_))

    case GET -> Root / "snapshot" / "currency" / SnapshotOrdinalVar(ordinal) =>
      context.getCurrencySnapshotAt(ordinal).flatMap(prepareResponse(_))

    case GET -> Root / "snapshot" / "currency" / SnapshotOrdinalVar(ordinal) / "count-updates" =>
      context.countUpdatesInSnapshotAt(ordinal).flatMap(prepareResponse(_))
  }
}
