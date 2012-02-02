/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react

import scala.util.continuations._

class EventsToDataflow[A](val init: Events[A]) extends ReactiveToDataflow[A, Unit, Events[A], DataflowEvents[A]] with Events[A] {
	protected def make(code: DataflowEvents[A] => Unit @suspendable) = new DataflowEvents(init) {
		def body() = code(this)
	}
	override def current(dep: Dependent) = super.current(dep)
}

/** Never call any methods of this stream outside of its body.
  */
abstract class DataflowEvents[A](protected var _reactive: Events[A]) extends Events[A]
	with DataflowReactive[A, Unit, Events[A]]
	with InvalidatableDependent
	with LevelCachingDependent
	with Publisher[A, Unit] {
	// eagerly start this stream
	_continue.apply()

	def receive(eng: Engine) {
		if (isAlive) {
			mayAbort { _continue() }
		}
	}

	override def message(dep: Dependent) = {
		checkLevelNow()
		subscribe(dep)
		Engine messageFor this match {
			case None => _reactive message dep
			case x => x
		}
	}

	def emit(a: A): Unit @suspendable = switchTo(Events.Now(a))

	protected def checkDelegate() {
		_reactive message this match {
			case Some(x) =>
				Engine.setMessage(this, x)
				Engine invalidate dependents
			case None =>
		}
	}
}
