package free

import scalaz._
import Scalaz._

import NIHFree._

sealed trait Action[+A]
case class Output[A](str: String, next: A) extends Action[A]
case class Bell[A](next: A) extends Action[A]

object Action {
  implicit val functor: Functor[Action] = new Functor[Action] {
    def map[A, B](fa: Action[A])(f: A => B): Action[B] = fa match {
      case Output(msg, next) => Output(msg, f(next))
      case Bell(next) => Bell(f(next))
    }
  }
}


object Actions {
  type Program[A] = NIHFree[Action, A]
  
  def output(msg: String): Program[Unit] = liftF(Output(msg, ()))
  def bell: Program[Unit] = liftF(Bell(()))
  
  
  
  
  val interpreter = new (Action ~> Id) {
    def apply[A](action: Action[A]): A = action match { 
      case Bell(next) => println("BELL!"); next
      case Output(msg, next) => println(); next
    }
  }
  
  def run[A](prog: Program[A]): A = prog.runM(action => interpreter(action))
  
}

object NIHFree {
  implicit def monad[F[+_]](implicit F: Functor[F]): Monad[({type f[x] = NIHFree[F,x]})#f] = new Monad[({type f[x] = NIHFree[F,x]})#f] {
    def point[A](a: => A): NIHFree[F,A] = NIHPure[F, A](a)
    
    def bind[A, B](free: NIHFree[F,A])(f: A => NIHFree[F,B]): NIHFree[F,B] = free match {
      case NIHSuspend(ffa) => NIHSuspend(F.map(ffa) { fr => bind(fr)(f) }) // NIHSuspend(ffa.map(_ flatmap f))
      case NIHPure(a) => f(a)
    }
  }
  
  def liftF[F[_], A](value: => F[A])(implicit F: Functor[F]): NIHFree[F, A] = NIHSuspend(value map NIHPure.apply)
  
  
}

sealed trait NIHFree[F[_], A] {
  final def runM[M[_]](f: F[NIHFree[F, A]] => M[NIHFree[F, A]])(implicit F: Functor[F], M: Monad[M]): M[A] = this match {
    case NIHSuspend(ffa) => ??? //M.bind(ffa)(f)
    case NIHPure(a) => M.pure(a)
  }
}
case class NIHSuspend[F[_], A](f: F[NIHFree[F, A]]) extends NIHFree[F, A]
case class NIHPure[F[_], A](a: A) extends NIHFree[F, A]

object FreeTest extends App {
  
  import Actions._
  def program: Program[Unit] = 
    for {
      _ <- bell
      _ <- bell
      _ <- output("coupla bells")
      _ <- bell
    } yield ()
  
  println(program)
  run(program)
}