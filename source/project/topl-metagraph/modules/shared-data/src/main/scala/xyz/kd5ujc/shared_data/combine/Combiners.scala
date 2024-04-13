package xyz.kd5ujc.shared_data.combine

import cats.effect.Async
import co.topl.brambl.syntax._
import monocle.Monocle.toAppliedFocusOps
import org.bouncycastle.util.encoders.Hex
import xyz.kd5ujc.shared_data.lib.Models.ToplMetagraphUpdate.ModifyEvent
import xyz.kd5ujc.shared_data.lib.Models.{EventRecord, OnChain}

object Combiners {

  def modifyEvent[F[_]: Async](
    update:       ModifyEvent,
    currentState: OnChain
  ): F[OnChain] =
    Async[F].delay {

      val recordKey = update.account

      val nowInTime = System.currentTimeMillis()

      val transactionId = update.data.id

      val modifiedEventRecord =
        currentState.records(recordKey) :+
        EventRecord(
          nowInTime,
          Hex.toHexString(transactionId.value.toByteArray)
        )

      currentState
        .focus(_.records)
        .modify(r => r ++ Map(recordKey -> modifiedEventRecord))
    }
}
