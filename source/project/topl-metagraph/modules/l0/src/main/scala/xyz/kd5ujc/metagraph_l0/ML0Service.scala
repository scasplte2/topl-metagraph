package xyz.kd5ujc.metagraph_l0

import cats.Applicative
import cats.data.NonEmptyList
import cats.effect.Async
import cats.syntax.all._
import io.circe.{Decoder, Encoder}
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.{EntityDecoder, HttpRoutes}
import org.tessellation.currency.dataApplication._
import org.tessellation.currency.dataApplication.dataApplication.{DataApplicationBlock, DataApplicationValidationErrorOr}
import org.tessellation.currency.schema.currency.CurrencyIncrementalSnapshot
import org.tessellation.schema.SnapshotOrdinal
import org.tessellation.security.hash.Hash
import org.tessellation.security.signature.Signed
import org.tessellation.security.{Hashed, Hasher, SecurityProvider}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import xyz.kd5ujc.shared_data.checkpoint.{Checkpoint, CheckpointService}
import xyz.kd5ujc.shared_data.combine.CombineService
import xyz.kd5ujc.shared_data.lib.ByteCodecs
import xyz.kd5ujc.shared_data.lib.Models.{CalculatedState, OnChain, ToplMetagraphUpdate}
import xyz.kd5ujc.shared_data.lib.syntax.CurrencyIncrementalSnapshotOps
import xyz.kd5ujc.shared_data.validate.ValidateService

object ML0Service {

  def make[F[+_]: Async: SecurityProvider: Hasher]: F[BaseDataApplicationL0Service[F]] = for {
    checkpointService <- CheckpointService.make[F]
    dataApplicationL0Service = makeBaseApplicationL0Service(checkpointService)
  } yield dataApplicationL0Service

  private def makeBaseApplicationL0Service[F[+_]: Async: SecurityProvider](
    checkpointService: CheckpointService[F],
  ): BaseDataApplicationL0Service[F] =
    BaseDataApplicationL0Service[F, ToplMetagraphUpdate, OnChain, CalculatedState](
      new DataApplicationL0Service[F, ToplMetagraphUpdate, OnChain, CalculatedState] {

        private val logger: SelfAwareStructuredLogger[F] = Slf4jLogger.getLoggerFromClass(ML0Service.getClass)

        override def genesis: DataState[OnChain, CalculatedState] =
          DataState(OnChain(Map.empty), Checkpoint.empty.state)

        override def serializeState(state: OnChain): F[Array[Byte]] =
          Async[F].delay(ByteCodecs.encodeState(state))

        override def deserializeState(bytes: Array[Byte]): F[Either[Throwable, OnChain]] =
          Async[F].delay(ByteCodecs.decodeState(bytes))

        override def serializeUpdate(update: ToplMetagraphUpdate): F[Array[Byte]] =
          Async[F].delay(ByteCodecs.encodeUpdate(update))

        override def deserializeUpdate(bytes: Array[Byte]): F[Either[Throwable, ToplMetagraphUpdate]] =
          Async[F].delay(ByteCodecs.decodeUpdate(bytes))

        override def serializeBlock(
          block: Signed[DataApplicationBlock]
        ): F[Array[Byte]] =
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

        override def routes(implicit context: L0NodeContext[F]): HttpRoutes[F] = new ML0CustomRoutes[F].public

        override def onSnapshotConsensusResult(snapshot: Hashed[CurrencyIncrementalSnapshot])(implicit
          A: Applicative[F]
        ): F[Unit] = {
//          def process(snapshot: Signed[CurrencyIncrementalSnapshot]): F[Unit] = for {
//            pushResult <- notifierService.push(snapshot)
//            _          <- logger.debug(s"Simba request id: ${pushResult.getRequestIdentitier}")
//            submission <- EvmSubmission(snapshot.ordinal, pushResult.getRequestIdentitier).pure[F]
//            _          <- evmTxQueue.offer(submission)
//          } yield ()

          for {
            _ <- logger.debug("Evaluating onSnapshotConsensusResult")
            numberOfUpdates = snapshot.signed.value.countUpdates
            _ <- logger.info(s"Got $numberOfUpdates updates for ordinal: ${snapshot.ordinal.value}")
//            _ <- Monad[F].whenA(numberOfUpdates > 0L)(process(snapshot.signed))
          } yield ()
        }

        override def validateData(
          state:   DataState[OnChain, CalculatedState],
          updates: NonEmptyList[Signed[ToplMetagraphUpdate]]
        )(implicit context: L0NodeContext[F]): F[DataApplicationValidationErrorOr[Unit]] =
          ValidateService.checkOnChainState[F](state.onChain, updates)

        override def validateUpdate(update: ToplMetagraphUpdate)(implicit
                                                                 context: L0NodeContext[F]
        ): F[DataApplicationValidationErrorOr[Unit]] = ().validNec.pure[F]

        override def combine(
          state:   DataState[OnChain, CalculatedState],
          updates: List[Signed[ToplMetagraphUpdate]]
        )(implicit context: L0NodeContext[F]): F[DataState[OnChain, CalculatedState]] =
          CombineService.handle[F](state, updates)

        override def getCalculatedState(implicit
          context: L0NodeContext[F]
        ): F[(SnapshotOrdinal, CalculatedState)] =
          checkpointService.getCheckpoint.map(checkpoint => (checkpoint.ordinal, checkpoint.state))

        override def setCalculatedState(ordinal: SnapshotOrdinal, state: CalculatedState)(implicit
          context: L0NodeContext[F]
        ): F[Boolean] = checkpointService.setCheckpoint(Checkpoint(ordinal, state))

        override def hashCalculatedState(state: CalculatedState)(implicit context: L0NodeContext[F]): F[Hash] =
          checkpointService.hashCalculatedState(state)
      }
    )

}
