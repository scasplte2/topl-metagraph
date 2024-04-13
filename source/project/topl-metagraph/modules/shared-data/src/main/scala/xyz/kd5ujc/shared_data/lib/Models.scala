package xyz.kd5ujc.shared_data.lib

import co.topl.brambl.models.transaction.IoTransaction
import derevo.circe.magnolia.{decoder, encoder, keyDecoder, keyEncoder}
import derevo.derive
import org.tessellation.currency.dataApplication.{DataCalculatedState, DataOnChainState, DataUpdate}
import scalapb_circe.codec._

object Models {

  @derive(decoder, encoder)
  sealed abstract class ToplMetagraphUpdate extends DataUpdate {
    val account: Account
    val data: IoTransaction
  }

  @derive(decoder, encoder, keyDecoder, keyEncoder)
  case class Account(id: String)

  @derive(decoder, encoder)
  case class EventRecord(creationTimestamp: Long, hash: String)

  @derive(decoder, encoder)
  case class OnChain(records: Map[Account, List[EventRecord]]) extends DataOnChainState

  @derive(decoder, encoder)
  case class CalculatedState() extends DataCalculatedState

  object ToplMetagraphUpdate {

    @derive(decoder, encoder)
    final case class ModifyEvent(account: Account, data: IoTransaction) extends ToplMetagraphUpdate
  }

}
