/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react

import scala.util.continuations._

/** Defines the base data-flow language shared by reactors and reactives.
  */
trait DataflowBase extends Dependent {
	private var _disposed = false

	protected var _continue = initialContinuation

	protected def initialContinuation: () => Unit = () => reset {
		body()
		//dispose()
	}

	def body(): Unit @suspendable

	/** Returns the next message emitted by the given reactive. Immediately returns, if one is
	  * available in the current propagation cycle.
	  */
	def next[B](r: Reactive[B, _]): B @suspendable = continueNow[B] { k =>
		r message this match {
			case Some(x) => if (!isDisposed) k(x)
			case None => checkDelegate()
		}
	}

	//def nextVal[A](r: Reactive[_,A]): A @suspendable

	/** Repeatedly executes the given `body` until the given stream emits.
	  * Immediately returns if it is currently emitting. Otherwise, it finishes the iteration in
	  * which the stream emits, i.e., the execution of the body is always completed before returning.
	  */
	def loopUntil[A](es: Events[A])(body: => Unit @suspendable): A @suspendable = {
		val x = es switch (None, Some(es.msg.get))
		while (x.now == None) {
			body
		}
		x.now.get
	}

	protected def checkDelegate()

	/** Suspends this dependent and continues execution in the next propagation cycle.
	  */
	def delay: Unit @suspendable = shift { (k: Unit => Unit) => continueLater { k() } }

	/** Has this reactive been terminated or did it end naturally? If `true`, this stream will behave
	  * like an `Reactive.Nil` until it's garbage collected.
	  */
	def isDisposed: Boolean = _disposed

	def isAlive = !isDisposed

	/** Will dispose this stream immediately. Permanently releases all allocated resources
	  * to be reclaimed by the garbage collector. This stream will behave like an `Event.Never`
	  * afterwards.
	  */
	def dispose(): Unit @suspendable = shift { (k: Unit => Unit) =>
		_disposed = true
	}

	protected def mayAbort(op: => Unit) = try {
		op
	} catch {
		case e: LevelMismatchNow =>
			handleLevelMismatch(e)
	}

	protected def handleLevelMismatch(l: LevelMismatchNow)

	protected def continueNow[A](body: (A => Unit) => Unit) = shift { (k: A => Unit) =>
		_continue = { () => mayAbort { body(k) } }
		_continue()
	}

	protected def continueLater(k: => Unit) = {
		_continue = { () => mayAbort { k } }
		Engine nextTurn this
	}
}