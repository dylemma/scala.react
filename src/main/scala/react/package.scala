/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala

import scala.annotation._

package object react {
	def Cache[A](op: => A) = logCreation(new CachedSignal(op))
	implicit def coerceConstant[A](x: A): Val[A] = Val(x)

	type Dependents = scala.collection.Set[Dependent]
	def NoDependents: Dependents = scala.collection.Set.empty

	implicit def id[A](a: A): A = a
	implicit def events2Dataflow[A](e: Events[A]): EventsToDataflow[A] = new EventsToDataflow(e)
	implicit def signal2Dataflow[A](s: Signal[A]): SignalToDataflow[A] = new SignalToDataflow(s)

	// Debugging stuff:

	private val DEBUG = elidable.ALL

	@elidable(DEBUG) def log(msg: String) {
		Console.err.println(msg)
	}

	@elidable(DEBUG) def logCreation[A](obj: A): A = {
		//println(Thread.currentThread.getStackTrace.toList.toString)
		lineNumbers(obj) = parentLineNumber(4) // magic number: hop n steps up the stack trace
		obj match {
			case d: DependencyNode => log("Creating " + dbgInfo(d))
			case _ =>
		}
		obj
	}

	def debugId(obj: AnyRef) = {
		val n = lineNumbers(obj)
		if (n != -1) "Line " + n + " "
		else ""
	}

	def dbgInfo(d: DependencyNode) = d + " (" + debugId(d) + "on level " + d.level + ")"

	def parentLineNumber(n: Int) = Thread.currentThread.getStackTrace()(n).getLineNumber

	// TODO: need a concurrent, weak map
	private val lineNumbers = new scala.collection.mutable.WeakHashMap[Any, Int] {
		override def default(k: Any) = -1
	}
}
