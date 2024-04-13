package xyz.kd5ujc.shared_data.combine

import cats.effect.Async
import cats.syntax.all._
import org.tessellation.currency.dataApplication.DataState
import org.tessellation.security.SecurityProvider
import org.tessellation.security.signature.Signed
import xyz.kd5ujc.shared_data.lib.Models.ToplMetagraphUpdate.ModifyEvent
import xyz.kd5ujc.shared_data.lib.Models.{CalculatedState, OnChain, ToplMetagraphUpdate}

import scala.collection.immutable.SortedSet

object CombineService {

  def handle[F[_]: Async](
    prevState: DataState[OnChain, CalculatedState],
    updates:   List[Signed[ToplMetagraphUpdate]]
  )(implicit sp: SecurityProvider[F]): F[DataState[OnChain, CalculatedState]] = {

    def modifyOnchain: F[OnChain] =
      updates
        .distinct
        .foldLeftM(prevState.onChain) { (acc, signedUpdate) =>
          signedUpdate.value match {
            case u: ModifyEvent => Combiners.modifyEvent(u, acc)
          }
        }

    def modifyCalculated: F[CalculatedState] = CalculatedState().pure[F]

    for {
      newOnchain    <- modifyOnchain
      newCalculated <- modifyCalculated
      dataState = DataState(newOnchain, newCalculated)
    } yield dataState
  }
}
