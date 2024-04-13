package xyz.kd5ujc.data_l1

import cats.effect.{IO, Resource}
import cats.syntax.all._
import org.tessellation.currency.dataApplication._
import org.tessellation.currency.l1.CurrencyL1App
import org.tessellation.ext.cats.effect.ResourceIO
import org.tessellation.schema.cluster.ClusterId
import org.tessellation.schema.semver.{MetagraphVersion, TessellationVersion}
import xyz.kd5ujc.buildinfo.BuildInfo
import xyz.kd5ujc.shared_data.app.ApplicationConfigOps

import java.util.UUID

object Main
    extends CurrencyL1App(
      name = "data-app-l1",
      header = "Metagraph Data L1 node",
      clusterId = ClusterId(UUID.fromString("517c3a05-9219-471b-a54c-21b7d72f4ae5")),
      tessellationVersion = TessellationVersion.unsafeFrom(org.tessellation.BuildInfo.version),
      metagraphVersion = MetagraphVersion.unsafeFrom(BuildInfo.version)
    ) {

  override def dataApplication: Option[Resource[IO, BaseDataApplicationL1Service[IO]]] = (for {
    config <- ApplicationConfigOps.readDefault[IO].asResource
    l1Service <- DataL1Service.make[IO].asResource
  } yield l1Service).some
}
