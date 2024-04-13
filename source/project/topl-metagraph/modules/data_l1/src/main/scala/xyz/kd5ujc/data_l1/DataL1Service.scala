package xyz.kd5ujc.data_l1

import cats.data.NonEmptyList
import cats.effect.Async
import cats.syntax.all._
import io.circe.{Decoder, Encoder}
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.tessellation.currency.dataApplication._
import org.tessellation.currency.dataApplication.dataApplication.{DataApplicationBlock, DataApplicationValidationErrorOr}
import org.tessellation.schema.SnapshotOrdinal
import org.tessellation.security.hash.Hash
import org.tessellation.security.signature.Signed
import xyz.kd5ujc.data_l1.L1NodeContext.syntax.L1NodeContextOps
import xyz.kd5ujc.shared_data.checkpoint.{Checkpoint, CheckpointService}
import xyz.kd5ujc.shared_data.lib.ByteCodecs
import xyz.kd5ujc.shared_data.lib.Models.{CalculatedState, OnChain, ToplMetagraphUpdate}
import xyz.kd5ujc.shared_data.validate.ValidateService
object DataL1Service {

  def make[F[+_]: Async]: F[BaseDataApplicationL1Service[F]] =
    for {
      checkpointService <- CheckpointService.make[F]
      dataApplicationL1Service = makeBaseApplicationL1Service(
        checkpointService
      )
    } yield dataApplicationL1Service

  private def makeBaseApplicationL1Service[F[+_]: Async](
    checkpointService:  CheckpointService[F]
  ): BaseDataApplicationL1Service[F] =
    BaseDataApplicationL1Service[F, ToplMetagraphUpdate, OnChain, CalculatedState](
      new DataApplicationL1Service[F, ToplMetagraphUpdate, OnChain, CalculatedState] {

        override def validateData(
          state:   DataState[OnChain, CalculatedState],
          updates: NonEmptyList[Signed[ToplMetagraphUpdate]]
        )(implicit context: L1NodeContext[F]): F[DataApplicationValidationErrorOr[Unit]] =
          ().validNec[DataApplicationValidationError].pure[F]

        override def combine(
          state:   DataState[OnChain, CalculatedState],
          updates: List[Signed[ToplMetagraphUpdate]]
        )(implicit context: L1NodeContext[F]): F[DataState[OnChain, CalculatedState]] = state.pure[F]

        override def getCalculatedState(implicit
          context: L1NodeContext[F]
        ): F[(SnapshotOrdinal, CalculatedState)] =
          checkpointService.getCheckpoint.map(checkpoint => (checkpoint.ordinal, checkpoint.state))

        override def setCalculatedState(ordinal: SnapshotOrdinal, state: CalculatedState)(implicit
          context: L1NodeContext[F]
        ): F[Boolean] = checkpointService.setCheckpoint(Checkpoint(ordinal, state))

        override def hashCalculatedState(state: CalculatedState)(implicit context: L1NodeContext[F]): F[Hash] =
          checkpointService.hashCalculatedState(state)

        override def serializeState(state: OnChain): F[Array[Byte]] =
          Async[F].delay(ByteCodecs.encodeState(state))

        override def deserializeState(bytes: Array[Byte]): F[Either[Throwable, OnChain]] =
          Async[F].delay(ByteCodecs.decodeState(bytes))

        override def serializeUpdate(update: ToplMetagraphUpdate): F[Array[Byte]] =
          Async[F].delay(ByteCodecs.encodeUpdate(update))

        override def deserializeUpdate(bytes: Array[Byte]): F[Either[Throwable, ToplMetagraphUpdate]] =
          Async[F].delay(ByteCodecs.decodeUpdate(bytes))

        override def serializeBlock(block: Signed[DataApplicationBlock]): F[Array[Byte]] =
          Async[F].delay(ByteCodecs.encodeBlock(block))

        override def deserializeBlock(
          bytes: Array[Byte]
        ): F[Either[Throwable, Signed[DataApplicationBlock]]] =
          Async[F].delay(ByteCodecs.decodeBlock(bytes))

        override def serializeCalculatedState(calculatedState: CalculatedState): F[Array[Byte]] =
          Async[F].delay(ByteCodecs.encodeCalculateState(calculatedState))

        override def deserializeCalculatedState(bytes: Array[Byte]): F[Either[Throwable, CalculatedState]] =
          Async[F].delay(ByteCodecs.decodeCalculatedState(bytes))

        override def dataEncoder: Encoder[ToplMetagraphUpdate] = implicitly(Encoder[ToplMetagraphUpdate])

        override def dataDecoder: Decoder[ToplMetagraphUpdate] = implicitly(Decoder[ToplMetagraphUpdate])

        override def calculatedStateEncoder: Encoder[CalculatedState] = implicitly(Encoder[CalculatedState])

        override def calculatedStateDecoder: Decoder[CalculatedState] = implicitly(Decoder[CalculatedState])

        override val signedDataEntityDecoder: EntityDecoder[F, Signed[ToplMetagraphUpdate]] = circeEntityDecoder

        override def validateUpdate(
          update: ToplMetagraphUpdate
        )(implicit context: L1NodeContext[F]): F[DataApplicationValidationErrorOr[Unit]] =
          context.getOnChainState.flatMap {
            _.fold(
              err => err.invalidNec[Unit].pure[F],
              state => ValidateService.checkUpdate[F](state, update)
            )
          }

        override def routes(implicit context: L1NodeContext[F]): HttpRoutes[F] =
          new DataL1CustomRoutes[F].public
      }
    )

}
