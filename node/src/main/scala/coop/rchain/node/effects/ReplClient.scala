package coop.rchain.node.effects

import java.io.{Closeable, FileNotFoundException}
import java.nio.file._
import java.util.concurrent.TimeUnit

import scala.io.Source

import cats.implicits._

import coop.rchain.node.model.repl._
import coop.rchain.shared.Resources

import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import monix.eval.Task

trait ReplClient[F[_]] {
  def run(line: String): F[Either[Throwable, String]]
  def eval(fileNames: List[String]): F[List[Either[Throwable, String]]]
}

object ReplClient {
  def apply[F[_]](implicit ev: ReplClient[F]): ReplClient[F] = ev
}

class GrpcReplClient(host: String, port: Int) extends ReplClient[Task] with Closeable {

  private val channel: ManagedChannel =
    ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build
  private val stub = ReplGrpc.stub(channel)

  def run(line: String): Task[Either[Throwable, String]] =
    Task
      .fromFuture(stub.run(CmdRequest(line)))
      .map(_.output)
      .attempt
      .map(_.leftMap(processError))

  def eval(fileNames: List[String]): Task[List[Either[Throwable, String]]] =
    fileNames
      .traverse(eval)

  def eval(fileName: String): Task[Either[Throwable, String]] = {
    val filePath = Paths.get(fileName)
    if (Files.exists(filePath))
      Task
        .fromFuture(stub.eval(EvalRequest(readContent(filePath))))
        .map(_.output)
        .attempt
        .map(_.leftMap(processError))
    else Task.now(Left(new FileNotFoundException("File not found")))
  }

  private def readContent(filePath: Path): String =
    Resources.withResource(Source.fromFile(filePath.toFile))(_.mkString)

  private def processError(t: Throwable): Throwable =
    Option(t.getCause).getOrElse(t)

  override def close(): Unit = channel.shutdown().awaitTermination(3, TimeUnit.SECONDS)
}
