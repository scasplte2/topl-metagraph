package com.my.currency.l0.custom_routes

import cats.effect.Async
import cats.syntax.functor._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import com.my.currency.shared_data.calculated_state.CalculatedState.getCalculatedState
import com.my.currency.shared_data.types.Types.Poll
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import eu.timepit.refined.auto._
import org.http4s.{HttpRoutes, Response}
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.middleware.CORS
import org.tessellation.http.routes.internal.{InternalUrlPrefix, PublicRoutes}
import org.tessellation.schema.address.Address
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

case class CustomRoutes[F[_] : Async]() extends Http4sDsl[F] with PublicRoutes[F] {
  implicit val logger: SelfAwareStructuredLogger[F] = Slf4jLogger.getLogger[F]

  @derive(decoder, encoder)
  case class PollResponse(id: String, name: String, owner: Address, result: Map[String, Long], startSnapshotOrdinal: Long, endSnapshotOrdinal: Long, status: String)

  private def formatPoll(poll: Poll, lastOrdinal: Long): PollResponse = {
    if (poll.endSnapshotOrdinal < lastOrdinal) {
      PollResponse(poll.id, poll.name, poll.owner, poll.pollOptions, poll.startSnapshotOrdinal, poll.endSnapshotOrdinal, "Closed")
    } else {
      PollResponse(poll.id, poll.name, poll.owner, poll.pollOptions, poll.startSnapshotOrdinal, poll.endSnapshotOrdinal, "Open")
    }
  }

  private def getAllPolls: F[Response[F]] = {
    getCalculatedState
      .map { case (ord, state) => state.polls.view.mapValues(formatPoll(_, ord.value.value)).toList }
      .flatMap(Ok(_))
      .handleErrorWith { e =>
        val message = s"An error occurred when getAllPolls: ${e.getMessage}"
        logger.error(message) >> new Exception(message).raiseError[F, Response[F]]
      }
  }

  private def getPollById(pollId: String): F[Response[F]] = {
    getCalculatedState
      .map { case (ord, state) => state.polls.get(pollId).map(formatPoll(_, ord.value.value)) }
      .flatMap(_.fold(NotFound())(Ok(_)))
      .handleErrorWith { e =>
        val message = s"An error occurred when getPollById: ${e.getMessage}"
        logger.error(message) >> new Exception(message).raiseError[F, Response[F]]
      }

  }

  private val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "polls" => getAllPolls
    case GET -> Root / "polls" / poolId => getPollById(poolId)
  }

  val public: HttpRoutes[F] =
    CORS
      .policy
      .withAllowCredentials(false)
      .httpRoutes(routes)

  override protected def prefixPath: InternalUrlPrefix = "/"
}
