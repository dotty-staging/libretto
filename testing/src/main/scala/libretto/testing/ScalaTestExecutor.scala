package libretto.testing

import java.util.concurrent.{Executors, ExecutorService, ScheduledExecutorService}
import libretto.{CoreLib, Monad, ScalaBridge, ScalaExecutor, ScalaDSL, StarterKit}
import libretto.util.{Async, Monad => ScalaMonad}
import libretto.util.Monad.syntax._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object ScalaTestExecutor {

  class ScalaTestKitFromExecutor[DSL <: ScalaDSL, Exec <: ScalaExecutor.Of[DSL]](
    val dsl0: DSL,
    val exec: Exec & ScalaExecutor.Of[dsl0.type],
  ) extends ScalaTestKit {
      override val dsl: exec.dsl.type = exec.dsl
      override val probes: exec.type = exec
      import dsl._
      import probes.Execution

      override type Assertion[A] = Val[String] |+| A

      private val coreLib = CoreLib(this.dsl)
      import coreLib.{Monad => _, _}

      override def success[A]: A -⚬ Assertion[A] =
        injectR

      override def failure[A]: Done -⚬ Assertion[A] =
        failure("Failed")

      override def failure[A](msg: String): Done -⚬ Assertion[A] =
        constVal(msg) > injectL

      override def monadAssertion: Monad[-⚬, Assertion] =
        |+|.right[Val[String]]

      override def extractOutcome(using exn: Execution)(outPort: exn.OutPort[Assertion[Done]]): Outcome[Unit] = {
        import TestResult.{Crash, Success, Failure}
        Outcome.asyncTestResult(
          exn.OutPort
            .awaitEither[Val[String], Done](outPort)
            .flatMap {
              case Left(e) =>
                Async.now(Crash(e))
              case Right(Left(msg)) =>
                exn.OutPort.awaitVal(msg).map {
                  case Left(e)    => Crash(e)
                  case Right(msg) => Failure(msg)
                }
              case Right(Right(d)) =>
                exn.OutPort.awaitDone(d).map {
                  case Left(e)   => Crash(e)
                  case Right(()) => Success(())
                }
            }
        )
      }
  }

  def fromExecutor(
    dsl: ScalaDSL,
    exec: ScalaExecutor.Of[dsl.type],
  ): TestExecutor[ScalaTestKit] =
    new TestExecutor[ScalaTestKit] {
      override val name: String =
        ScalaTestExecutor.getClass.getCanonicalName

      override val testKit: ScalaTestKitFromExecutor[dsl.type, exec.type] =
        new ScalaTestKitFromExecutor[dsl.type, exec.type](dsl, exec)

      import testKit.Outcome
      import testKit.dsl._
      import testKit.probes.Execution

      override def runTestCase[O, X](
        body: Done -⚬ O,
        conduct: (exn: Execution) ?=> exn.OutPort[O] => Outcome[X],
        postStop: X => Outcome[Unit],
      ): TestResult[Unit] =
        TestExecutor
          .usingExecutor(exec)
          .runTestCase[O, X](
            body,
            conduct andThen Outcome.toAsyncTestResult,
            postStop andThen Outcome.toAsyncTestResult,
          )
    }

  def fromJavaExecutors(
    scheduler: ScheduledExecutorService,
    blockingExecutor: ExecutorService,
  ): TestExecutor[ScalaTestKit] = {
    val executor0: libretto.ScalaExecutor.Of[StarterKit.dsl.type] =
      StarterKit.executor(blockingExecutor)(scheduler)

    given monadFuture: ScalaMonad[Future] =
      ScalaMonad.monadFuture(using ExecutionContext.fromExecutor(scheduler))

    fromExecutor(StarterKit.dsl, executor0)
  }

  lazy val global: TestExecutor[ScalaTestKit] =
    fromJavaExecutors(
      scheduler        = Executors.newScheduledThreadPool(4),
      blockingExecutor = Executors.newCachedThreadPool(),
    )
}
