/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react

import scala.util.continuations._

/** Defines the data-flow language for reactives.
  */
trait DataflowReactive[M, N, R <: Reactive[M, N]] extends Reactive[M, N] with DataflowBase with LevelCachingDependent {
	protected var _reactive: R

	override def initialContinuation = () => reset {
		checkDelegate()
		body()
	}

	/** Emit the given message immediately, i.e., in the current propagation cycle.
	  */
	def emit(m: M): Unit @suspendable

	/** Switch to the given reactive immediately, i.e., in the current propagation cycle.
	  * This reactive will behave like the the given reactive until the next call to `emit`
	  * or `switchTo`.
	  */
	def switchTo(r: R): Unit @suspendable =
		continueNow[Unit] { k =>
			_reactive = r
			checkDelegate()
			continueLater { checkDelegate(); k() }
		}

	protected def handleLevelMismatch(l: LevelMismatchNow) {
		_level = l.dependency.level + 1
		Engine lift this
	}
}