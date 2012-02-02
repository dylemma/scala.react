/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react

import scala.util.continuations._

object Reactor {
	/** Creates a new reactor.
	  */
	def once(code: Reactor => Unit @suspendable) = new Reactor {
		def body(): Unit @suspendable = code(this)
	}

	def loop(code: Reactor => Unit @suspendable) = new Reactor {
		def body(): Unit @suspendable = while (isAlive) { code(this) }
	}
}

/** Reactors can be used to deal with complex event handling issues such as sequencing,
  * looping and branching. They can be seen as a high-level extension to observers.
  */
abstract class Reactor extends DataflowBase with Observer {
	import Reactor._

	_continue()

	protected def handleLevelMismatch(l: LevelMismatchNow) {
		// leaf node: never need to handle level mismatches
	}

	def run() {
		if (isAlive) {
			_continue()
		}
	}

	def checkDelegate() {}
}