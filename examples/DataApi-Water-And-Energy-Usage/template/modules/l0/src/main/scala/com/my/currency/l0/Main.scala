package com.my.currency.l0

import cats.data.NonEmptyList
import cats.effect.IO
import com.my.currency.shared_data.Data
import com.my.currency.shared_data.Data.{State, Usage}
import io.circe.{Decoder, Encoder}
import org.http4s.HttpRoutes
import org.tessellation.BuildInfo
import org.tessellation.currency.dataApplication.dataApplication.DataApplicationValidationErrorOr
import org.tessellation.currency.dataApplication.{BaseDataApplicationL0Service, DataApplicationL0Service, L0NodeContext}
import org.tessellation.currency.l0.CurrencyL0App
import org.tessellation.schema.cluster.ClusterId
import org.tessellation.security.SecurityProvider
import org.tessellation.security.signature.Signed

import java.util.UUID

object Main
  extends CurrencyL0App(
    "currency-l0",
    "currency L0 node",
    ClusterId(UUID.fromString("517c3a05-9219-471b-a54c-21b7d72f4ae5")),
    version = BuildInfo.version
  ) {
  def dataApplication:  Option[BaseDataApplicationL0Service[IO]] = Option(BaseDataApplicationL0Service(new DataApplicationL0Service[IO, Usage, State] {
    override def genesis: State = State(0,0)

    override def validateData(oldState: State, updates: NonEmptyList[Signed[Usage]])(implicit context: L0NodeContext[IO]): IO[DataApplicationValidationErrorOr[Unit]] = Data.validateData(oldState, updates)

    override def validateUpdate(update: Usage)(implicit context: L0NodeContext[IO]): IO[DataApplicationValidationErrorOr[Unit]] = Data.validateUpdate(update)

    override def combine(oldState: State, updates: NonEmptyList[Signed[Usage]])(implicit context: L0NodeContext[IO]): IO[State] = Data.combine(oldState, updates)

    override def serializeState(state: State): IO[Array[Byte]] = Data.serializeState(state)

    override def deserializeState(bytes: Array[Byte]): IO[Either[Throwable, State]] = Data.deserializeState(bytes)

    override def serializeUpdate(update: Usage): IO[Array[Byte]] = Data.serializeUpdate(update)

    override def deserializeUpdate(bytes: Array[Byte]): IO[Either[Throwable, Usage]] = Data.deserializeUpdate(bytes)

    override def dataEncoder: Encoder[Usage] = Data.dataEncoder

    override def dataDecoder: Decoder[Usage] = Data.dataDecoder

    override def routes: HttpRoutes[IO] = Data.routes
  }))

  def rewards(implicit sp: SecurityProvider[IO]) = None
}