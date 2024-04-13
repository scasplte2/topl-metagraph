package xyz.kd5ujc.shared_data.lib

import io.circe.parser
import io.circe.syntax.EncoderOps
import org.tessellation.currency.dataApplication.dataApplication.DataApplicationBlock
import org.tessellation.security.signature.Signed
import org.tessellation.security.signature.Signed._
import xyz.kd5ujc.shared_data.lib.Models.{CalculatedState, OnChain, ToplMetagraphUpdate}

import java.nio.charset.StandardCharsets

object ByteCodecs {

  def encodeUpdate(update: ToplMetagraphUpdate): Array[Byte] =
    update.asJson.deepDropNullValues.noSpaces.getBytes(StandardCharsets.UTF_8)

  def decodeUpdate(bytes: Array[Byte]): Either[Throwable, ToplMetagraphUpdate] =
    parser.parse(new String(bytes, StandardCharsets.UTF_8)).flatMap(_.as[ToplMetagraphUpdate])

  def encodeState(state: OnChain): Array[Byte] =
    state.asJson.deepDropNullValues.noSpaces.getBytes(StandardCharsets.UTF_8)

  def decodeState(bytes: Array[Byte]): Either[Throwable, OnChain] =
    parser.parse(new String(bytes, StandardCharsets.UTF_8)).flatMap(_.as[OnChain])

  def encodeBlock(block: Signed[DataApplicationBlock]): Array[Byte] =
    block.asJson.deepDropNullValues.noSpaces.getBytes(StandardCharsets.UTF_8)

  def decodeBlock(bytes: Array[Byte]): Either[Throwable, Signed[DataApplicationBlock]] =
    parser.parse(new String(bytes, StandardCharsets.UTF_8)).flatMap(_.as[Signed[DataApplicationBlock]])

  def encodeCalculateState(calculatedState: CalculatedState): Array[Byte] =
    calculatedState.asJson.deepDropNullValues.noSpaces.getBytes(StandardCharsets.UTF_8)

  def decodeCalculatedState(bytes: Array[Byte]): Either[Throwable, CalculatedState] =
    parser.parse(new String(bytes, StandardCharsets.UTF_8)).flatMap(_.as[CalculatedState])

}
