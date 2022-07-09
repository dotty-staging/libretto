package libretto.mashup

import libretto.{ScalaExecutor, StarterKit, scalasource}
import libretto.util.Async
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import java.util.concurrent.ScheduledExecutorService

object MashupKitImpl extends MashupKit { kit =>
  import StarterKit.dsl.{-⚬, =⚬, |*|, |+|, One, Val}
  import StarterKit.dsl.$.>

  override object dsl extends MashupDsl {
    override type Fun[A, B] = A -⚬ B // for now, let's just use libretto's linear functions

    override type EmptyResource = One

    override type or[A, B] = A |+| B

    override type -->[A, B] = A =⚬ B

    override type Text = Val[String]

    override type Float64 = Val[Double]

    override type Expr[A] = StarterKit.dsl.$[A]

    override type Record = One

    override type ##[A, B] = A |*| B

    override type of[Name <: String, T] = T

    override type Unlimited[A] = StarterKit.coreLib.Unlimited[A]

    override def fun[A, B](f: Expr[A] => Expr[B]): Fun[A, B] =
      StarterKit.dsl.λ(f)

    override def closure[A, B](f: Expr[A] => Expr[B]): Expr[A --> B] =
      StarterKit.dsl.Λ(f)

    override def id[A]: Fun[A, A] =
      StarterKit.dsl.id[A]

    override def left[A, B]: Fun[A, A or B] =
      StarterKit.dsl.injectL[A, B]

    override def right[A, B]: Fun[B, A or B] =
      StarterKit.dsl.injectR[A, B]

    override object Record extends Records {
      override def apply()(using pos: scalasource.Position): Expr[Record] =
        StarterKit.dsl.$.one(using pos)
    }

    override object Float64 extends Float64s {
      override def apply(value: Double)(using pos: scalasource.Position): Expr[Float64] =
        StarterKit.dsl.$.one(using pos) > StarterKit.dsl.done > StarterKit.dsl.constVal(value)
    }

    override object Expr extends Exprs {
      override def eliminateSecond[A](a: Expr[A], empty: Expr[EmptyResource])(pos: scalasource.Position): Expr[A] =
        StarterKit.dsl.$.eliminateSecond(a, empty)(pos)

      override def extendRecord[A, N <: String, T](init: Expr[A], last: (N, Expr[T]))(pos: scalasource.Position): Expr[A ## (N of T)] =
        StarterKit.dsl.$.zip(init, last._2)(pos)
    }

    override object Unlimited extends Unlimiteds {
      export StarterKit.coreLib.Unlimited.map
    }
  }

  override def createRuntime(executor: ScheduledExecutorService): MashupRuntime[dsl.type] = {
    val exec = StarterKit.executor(executor)(executor)
    new RuntimeImpl(exec)
  }

  private class RuntimeImpl(
    executor: ScalaExecutor.Of[StarterKit.dsl.type, Future],
  ) extends MashupRuntime[dsl.type] {
    override val dsl: kit.dsl.type = kit.dsl
    import dsl.{-->, EmptyResource, Unlimited}

    override opaque type Execution <: MashupExecution = ExecutionImpl[? <: executor.Execution]

    override def run[A, B](prg: dsl.Fun[A, B]): Executing[A, B] = {
      val executing = executor.execute(prg)
      type EXN = executing.execution.type
      val execution: EXN = executing.execution
      new Executing(new ExecutionImpl[EXN](execution), executing.inPort, executing.outPort)
    }


    private class ExecutionImpl[EXN <: executor.Execution](val underlying: EXN) extends MashupExecution {
      override type InPort[A]  = underlying.InPort[A]
      override type OutPort[A] = underlying.OutPort[A]

      override object InPort extends InPorts {
        override def emptyResourceIgnore(port: InPort[EmptyResource]): Unit =
          underlying.InPort.discardOne(port)

        override def functionInputOutput[I, O](port: InPort[I --> O]): (OutPort[I], InPort[O]) =
            underlying.InPort.functionInputOutput(port)

        override def unlimitedAwaitChoice[A](
          port: InPort[Unlimited[A]],
        ): Async[Try[Option[Either[InPort[A], (InPort[Unlimited[A]], InPort[Unlimited[A]])]]]] = {
          val port1 = underlying.InPort.contramap(port)(StarterKit.coreLib.Unlimited.fromChoice[A])
          val ft: Future[Try[Option[Either[InPort[A], (InPort[Unlimited[A]], InPort[Unlimited[A]])]]]] =
            underlying.InPort.supplyChoice(port1)
              .flatMap {
                case Left(e) =>
                  Future.successful(Failure(e))
                case Right(Left(empty)) =>
                  underlying.InPort.discardOne(empty)
                  Future.successful(Success(None))
                case Right(Right(port)) =>
                  underlying.InPort.supplyChoice(port)
                    .flatMap {
                      case Left(e) =>
                        Future.successful(Failure(e))
                      case Right(Left(portSingle)) =>
                        Future.successful(Success(Some(Left(portSingle))))
                      case Right(Right(portSplit)) =>
                        val (port1, port2) = underlying.InPort.split(portSplit)
                        Future.successful(Success(Some(Right((port1, port2)))))
                    } (ExecutionContext.parasitic) // XXX
              } (ExecutionContext.parasitic) // XXX
          Async.unsafeFromFuture(
            ft,
            e => Console.err.println(s"Unexpected failure of InPort.supplyChoice: $e"), // XXX
          )
        }
      }

      override object OutPort extends OutPorts {
        override def emptyResourceIgnore(port: OutPort[EmptyResource]): Unit =
          underlying.OutPort.discardOne(port)

        override def functionInputOutput[I, O](port: OutPort[I --> O]): (InPort[I], OutPort[O]) =
          underlying.OutPort.functionInputOutput(port)

        override def unlimitedIgnore[A](port: OutPort[Unlimited[A]]): Unit = {
          val port1 = underlying.OutPort.map(port)(StarterKit.coreLib.Unlimited.discard[A])
          underlying.OutPort.discardOne(port1)
        }

        override def unlimitedGetSingle[A](port: OutPort[Unlimited[A]]): OutPort[A] =
          underlying.OutPort.map(port)(StarterKit.coreLib.Unlimited.single[A])

        override def unlimitedSplit[A](port: OutPort[Unlimited[A]]): (OutPort[Unlimited[A]], OutPort[Unlimited[A]]) = {
          val ports = underlying.OutPort.map(port)(StarterKit.coreLib.Unlimited.split)
          underlying.OutPort.split(ports)
        }
      }
    }
  }
}
