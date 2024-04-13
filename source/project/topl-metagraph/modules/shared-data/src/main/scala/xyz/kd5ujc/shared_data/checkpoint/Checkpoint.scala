package xyz.kd5ujc.shared_data.checkpoint

import org.tessellation.schema.SnapshotOrdinal
import xyz.kd5ujc.shared_data.lib.Models.CalculatedState

case class Checkpoint(ordinal: SnapshotOrdinal, state: CalculatedState)

object Checkpoint {
  val empty: Checkpoint =
    Checkpoint(SnapshotOrdinal.MinValue, CalculatedState())
}
