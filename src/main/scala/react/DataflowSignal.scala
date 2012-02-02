/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react

import scala.util.continuations._

class SignalToDataflow[A](val init: Signal[A]) extends ReactiveToDataflow[A, A, Signal[A], DataflowSignal[A]] with Signal[A] {
	protected def make(code: DataflowSignal[A] => Unit @suspendable) = new DataflowSignal(init) {
		def body() = code(this)
	}
	override def subscribe(dep: Dependent) = super.subscribe(dep)
	def cached: Signal[A] = init.cached
}

abstract class DataflowSignal[A](protected var _reactive: Signal[A]) extends ValueCachingSignal[A]
	with DataflowReactive[A, A, Signal[A]] {
	// eagerly start this signal
	validate()

	@throws(classOf[LevelMismatchNow]) protected def evaluateAndConnect() =
		if (isAlive) {
			_continue()
		}

	def body(): Unit @suspendable

	protected def checkDelegate() {
		/*_reactive message this match {
      case Some(x) =>
        _value = x
      case None =>
    }*/
		// we might have been notified by the underlying signal, so eval and reinstall
		_value = _reactive.current(this)
	}

	def previous: A = {
		if (_level < 0) _level = 0
		_value
	}

	override protected def handleLevelMismatch(l: LevelMismatchNow) {
		// rethrow: handled by value caching signal
		throw l
	}

	def emit(a: A): Unit @suspendable =
		if (_value != a) switchTo(Val(a))
		else delay // do a delay in any event to avoid surprises
}
