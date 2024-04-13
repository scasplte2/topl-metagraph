package xyz.kd5ujc.metagraph_l0

import cats.effect.std.Supervisor
import cats.effect.{IO, Resource}
import cats.implicits._
import org.tessellation.currency.dataApplication._
import org.tessellation.currency.l0.CurrencyL0App
import org.tessellation.ext.cats.effect.ResourceIO
import org.tessellation.json.JsonSerializer
import org.tessellation.kryo.KryoSerializer
import org.tessellation.schema.cluster.ClusterId
import org.tessellation.schema.semver.{MetagraphVersion, TessellationVersion}
import org.tessellation.security.{Hasher, JsonHash, KeyPairGenerator, SecurityProvider}
import org.tessellation.shared
import xyz.kd5ujc.buildinfo.BuildInfo
import xyz.kd5ujc.shared_data.app.ApplicationConfigOps

import java.util.UUID

object Main
    extends CurrencyL0App(
      name = "metagraph-l0",
      header = "Metagraph L0 node",
      clusterId = ClusterId(UUID.fromString("517c3a05-9219-471b-a54c-21b7d72f4ae5")),
      tessellationVersion = TessellationVersion.unsafeFrom(org.tessellation.BuildInfo.version),
      metagraphVersion = MetagraphVersion.unsafeFrom(BuildInfo.version)
    ) {

  override def dataApplication: Option[Resource[IO, BaseDataApplicationL0Service[IO]]] = (for {
    config                                  <- ApplicationConfigOps.readDefault[IO].asResource
    implicit0(kryo: KryoSerializer[IO])     <- KryoSerializer.forAsync[IO](shared.sharedKryoRegistrar)
    implicit0(json2bin: JsonSerializer[IO]) <- JsonSerializer.forSync[IO].asResource
    implicit0(hasher: Hasher[IO])           <- IO(Hasher.forSync[IO](_ => JsonHash)).asResource
    implicit0(sp: SecurityProvider[IO])     <- SecurityProvider.forAsync[IO]
    l0Service                               <- ML0Service.make[IO].asResource
  } yield l0Service).some
}
