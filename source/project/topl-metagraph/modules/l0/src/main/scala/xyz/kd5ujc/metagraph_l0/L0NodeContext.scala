package xyz.kd5ujc.metagraph_l0

import cats.data.EitherT
import cats.effect.Async
import cats.implicits.toFunctorOps
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import org.tessellation.currency.dataApplication.{DataApplicationValidationError, L0NodeContext}
import org.tessellation.currency.schema.currency.CurrencyIncrementalSnapshot
import org.tessellation.schema.SnapshotOrdinal
import org.tessellation.security.Hashed
import xyz.kd5ujc.shared_data.lib.ByteCodecs
import xyz.kd5ujc.shared_data.lib.Models.OnChain
import xyz.kd5ujc.shared_data.lib.syntax.CurrencyIncrementalSnapshotOps

object L0NodeContext {

  object syntax {

    implicit class L0NodeContextOps[F[_] : Async](ctx: L0NodeContext[F]) {

      def getOnChainState: F[Either[DataApplicationValidationError, OnChain]] =
        getLatestCurrencySnapshot.map {
          _.flatMap { snapshot =>
            snapshot.dataApplication
              .toRight(Errors.L0CtxCouldNotGetLatestState)
              .flatMap(part => ByteCodecs.decodeState(part.onChainState).left.map(_ => Errors.L0CtxFailedToDecodeState))
          }
        }

      def getLatestCurrencySnapshot: F[Either[DataApplicationValidationError, CurrencyIncrementalSnapshot]] =
        EitherT
          .fromOptionF[F, DataApplicationValidationError, Hashed[CurrencyIncrementalSnapshot]](
            ctx.getLastCurrencySnapshot,
            Errors.L0CtxCouldNotGetLatestCurrencySnapshot
          )
          .map(_.signed.value)
          .value

      def getCurrencySnapshotAt(ordinal: SnapshotOrdinal): F[Either[DataApplicationValidationError, CurrencyIncrementalSnapshot]] =
        EitherT
          .fromOptionF[F, DataApplicationValidationError, Hashed[CurrencyIncrementalSnapshot]](
            ctx.getCurrencySnapshot(ordinal),
            Errors.L0CtxCouldNotGetLatestGlobalSnapshot
          )
          .map(_.signed.value)
          .value

      def countUpdatesInSnapshotAt(ordinal: SnapshotOrdinal): F[Either[DataApplicationValidationError, Long]] =
        getCurrencySnapshotAt(ordinal).map(_.map(_.countUpdates))
    }
  }

  object Errors {

    @derive(decoder, encoder)
    case object L0CtxCouldNotGetLatestCurrencySnapshot extends DataApplicationValidationError {
      val message = "Failed to retrieve latest currency snapshot from L0 node context!"
    }

    case object L0CtxCouldNotGetLatestState extends DataApplicationValidationError {
      val message = "Failed to retrieve latest state from L0 node context!"
    }

    @derive(decoder, encoder)
    case object L0CtxFailedToDecodeState extends DataApplicationValidationError {
      val message = "An error was encountered while decoding the state from L0 node context"
    }

    @derive(decoder, encoder)
    case object L0CtxCouldNotGetLatestGlobalSnapshot extends DataApplicationValidationError {
      val message = "Failed to retrieve latest global snapshot from L0 node context!"
    }
  }
}
