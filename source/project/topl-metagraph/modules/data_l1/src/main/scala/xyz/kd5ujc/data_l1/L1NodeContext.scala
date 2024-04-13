package xyz.kd5ujc.data_l1

import cats.data.EitherT
import cats.effect.Async
import cats.implicits.toFunctorOps
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import org.tessellation.currency.dataApplication.{DataApplicationValidationError, L1NodeContext}
import org.tessellation.currency.schema.currency.CurrencyIncrementalSnapshot
import org.tessellation.schema.GlobalIncrementalSnapshot
import org.tessellation.security.Hashed
import xyz.kd5ujc.shared_data.lib.ByteCodecs
import xyz.kd5ujc.shared_data.lib.Models.OnChain

object L1NodeContext {

  object syntax {

    implicit class L1NodeContextOps[F[_]: Async](ctx: L1NodeContext[F]) {

      def getLatestCurrencySnapshot: F[Either[DataApplicationValidationError, CurrencyIncrementalSnapshot]] =
        EitherT
          .fromOptionF[F, DataApplicationValidationError, Hashed[CurrencyIncrementalSnapshot]](
            ctx.getLastCurrencySnapshot,
            Errors.L1CtxCouldNotGetLatestCurrencySnapshot
          )
          .map(_.signed.value)
          .value

      def getLatestGlobalSnapshot: F[Either[DataApplicationValidationError, GlobalIncrementalSnapshot]] =
        EitherT
          .fromOptionF[F, DataApplicationValidationError, Hashed[GlobalIncrementalSnapshot]](
            ctx.getLastGlobalSnapshot,
            Errors.L1CtxCouldNotGetLatestGlobalSnapshot
          )
          .map(_.signed.value)
          .value

      def getOnChainState: F[Either[DataApplicationValidationError, OnChain]] =
        getLatestCurrencySnapshot.map {
          _.flatMap { snapshot =>
            snapshot.dataApplication
              .toRight(Errors.L1CtxCouldNotGetLatestState)
              .flatMap(part => ByteCodecs.decodeState(part.onChainState).left.map(_ => Errors.L1CtxFailedToDecodeState))
          }
        }
    }
  }

  object Errors {

    @derive(decoder, encoder)
    case object L1CtxCouldNotGetLatestCurrencySnapshot extends DataApplicationValidationError {
      val message = "Failed to retrieve latest currency snapshot from L1 node context!"
    }

    case object L1CtxCouldNotGetLatestState extends DataApplicationValidationError {
      val message = "Failed to retrieve latest state from L1 node context!"
    }

    @derive(decoder, encoder)
    case object L1CtxFailedToDecodeState extends DataApplicationValidationError {
      val message = "An error was encountered while decoding the state from L1 node context"
    }

    @derive(decoder, encoder)
    case object L1CtxCouldNotGetLatestGlobalSnapshot extends DataApplicationValidationError {
      val message = "Failed to retrieve latest global snapshot from L1 node context!"
    }
  }
}
