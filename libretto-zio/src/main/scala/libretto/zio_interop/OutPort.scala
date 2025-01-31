package libretto.zio_interop

import libretto.scaletto.ScalettoBridge
import libretto.scaletto.StarterKit
import libretto.stream.scaletto.DefaultStreams.ValSource
import zio.stream.{UStream, ZStream}

class OutPort[A](
  val bridge: ScalettoBridge.Of[StarterKit.dsl.type],
  val execution: bridge.Execution,
  val port: execution.OutPort[A],
) {
  import execution.{OutPort => Port}

  def zstream[X](using ev: A =:= ValSource[X]): UStream[X] = {
    def go(port: Port[ValSource[X]]): UStream[X] =
      ZStream.unfoldZIO(port) { port =>
        Port
          .awaitEither(
            Port.map(port)(ValSource.poll)
          )
          .toZIO.absolve.orDie
          .flatMap {
            case Left(port) =>
              Port.awaitDone(port).toZIO.absolve.orDie.as(None)
            case Right(port) =>
              val (px, pxs) = Port.split(port)
              Port.awaitVal(px).toZIO.absolve.orDie.map(x => Some((x, pxs)))
          }
      }

    go(ev.substituteCo(port))
  }
}
