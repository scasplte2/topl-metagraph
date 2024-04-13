package xyz.kd5ujc.shared_data

import cats.implicits.toTraverseOps
import com.google.protobuf.ByteString
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, HCursor, Json}
import org.tessellation.currency.dataApplication.DataUpdate
import org.tessellation.currency.schema.currency.CurrencyIncrementalSnapshot
import org.tessellation.security.hex.Hex
import xyz.kd5ujc.shared_data.lib.Models.ToplMetagraphUpdate

package object lib {

  implicit val dataUpateEncoder: Encoder[DataUpdate] = {
    case event: ToplMetagraphUpdate => event.asJson
    case _                   => Json.Null
  }

  implicit val dataUpdateDecoder: Decoder[DataUpdate] = (c: HCursor) => c.as[ToplMetagraphUpdate]

  object syntax {

    implicit class HexOps(hex: Hex) {
      def toByteString: ByteString = ByteString.copyFrom(hex.toBytes)
    }

    implicit class ByteArrayOps(arr: Array[Byte]) {
      def toByteString: ByteString = ByteString.copyFrom(arr)
    }

    implicit class CurrencyIncrementalSnapshotOps(cis: CurrencyIncrementalSnapshot) {
      def countUpdates: Long =
        cis.dataApplication.flatMap {
            _.blocks
              .traverse(ByteCodecs.decodeBlock)
              .map(_.map(_.updates.size.toLong).sum)
              .toOption
          }
          .getOrElse(0L)
    }
  }
}
