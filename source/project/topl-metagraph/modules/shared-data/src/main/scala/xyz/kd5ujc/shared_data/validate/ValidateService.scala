package xyz.kd5ujc.shared_data.validate

import cats.data.NonEmptyList
import cats.effect.{Async, Resource}
import cats.syntax.all._
import co.topl.brambl.validation.TransactionSyntaxInterpreter
import co.topl.brambl.validation.algebras.TransactionSyntaxVerifier
import org.tessellation.currency.dataApplication.dataApplication.DataApplicationValidationErrorOr
import org.tessellation.security.signature.Signed
import xyz.kd5ujc.shared_data.lib.Models.ToplMetagraphUpdate.ModifyEvent
import xyz.kd5ujc.shared_data.lib.Models.{OnChain, ToplMetagraphUpdate}

object ValidateService {

  def checkOnChainState[F[_]: Async](
    state:   OnChain,
    updates: NonEmptyList[Signed[ToplMetagraphUpdate]]
  ): F[DataApplicationValidationErrorOr[Unit]] =
    updates
      .traverse(signedUpdate => checkUpdate(state, signedUpdate.value))
      .map(_.reduce)

  def checkUpdate[F[_]: Async](
    state:  OnChain,
    update: ToplMetagraphUpdate
  ): F[DataApplicationValidationErrorOr[Unit]] = {
    def modifyEvent(
      update: ModifyEvent,
      state:  OnChain
    ): DataApplicationValidationErrorOr[Unit] = List(
      ValidatorRules.isValidUpdateData(update)
    ).combineAll

    update match {
      case u: ModifyEvent => modifyEvent(u, state).pure[F]
    }
  }

}

case class Validators[F[_]](transactionSyntax: TransactionSyntaxVerifier[F])

object Validators {

  def make[F[_]: Async]: Resource[F, Validators[F]] =
    Resource.eval(Async[F].delay {
      Validators(
        TransactionSyntaxInterpreter.make[F]()
      )
    })
}
