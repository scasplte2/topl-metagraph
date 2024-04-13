package xyz.kd5ujc.shared_data.checkpoint

import cats.Semigroup
import cats.effect.Ref
import cats.effect.kernel.Async
import cats.implicits.catsSyntaxSemigroup
import cats.syntax.functor._
import org.tessellation.security.hash.Hash
import xyz.kd5ujc.shared_data.lib.ByteCodecs
import xyz.kd5ujc.shared_data.lib.Models.CalculatedState

trait CheckpointService[F[_]] {
  def getCheckpoint: F[Checkpoint]

  def setCheckpoint(
    checkpoint: Checkpoint
  ): F[Boolean]

  def hashCalculatedState(
    state: CalculatedState
  ): F[Hash]
}

object CheckpointService {

  implicit val checkpointSemigroup: Semigroup[Checkpoint] = (a, b) => Checkpoint(b.ordinal, a.state)

  def make[F[_]: Async]: F[CheckpointService[F]] =
    Ref.of[F, Checkpoint](Checkpoint.empty).map { stateRef =>
      new CheckpointService[F] {
        override def getCheckpoint: F[Checkpoint] = stateRef.get

        override def setCheckpoint(latest: Checkpoint): F[Boolean] =
          stateRef
            .modify { prevCheckpoint =>
              (prevCheckpoint.combine(latest), true)
            }

        override def hashCalculatedState(
          state: CalculatedState
        ): F[Hash] = Async[F].delay {
          val bytes = ByteCodecs.encodeCalculateState(state)
          Hash.fromBytes(bytes)
        }
      }
    }
}
