/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react

import scala.util.continuations._

object Reactive {
	private[react]type ObSet = scala.collection.mutable.HashSet[Dependent]

	trait Nil[+A, +B] extends Dependency[A, B] {
		//override def subscribe(dep: Dependent) {}
		def valid = true
		def level = 0
		def clearDependents() = NoDependents
		def dependents = NoDependents
		override protected[react] def message(dep: Dependent): Option[A] = None
	}
}

/** The base class for all reactive entities that hold values
  * (instantly, continuously, or periodically) and on which Dependents
  * can depend on. Type parameter `Msg` specifies the type of messages this
  * reactive emits during propagation cycles, parameter `Now` specifies the
  * type of value this reactive holds in and between propagation cycles. For
  * plain signals, these types would be the same, for events `Now` is `Unit`
  * as events do not hold values, and for incremental collections, `Msg` is
  * the type of incremental changes, whereas `Now` is the underlying non-reactive
  * collection type. The following relation must hold for every subclass:
  * given the value (of type `Now`) of a reactive before a
  * propagation cycle and a valid message (of type `Msg`) during a propagation cycle, one can
  * uniquely determine the future current value (of type `Now`) of this reactive.
  * Note that the message must be ''valid'', i.e., it must be of the correct type (enured by the
  * typesystem) but also contain valid information (not ensured by the compiler).
  *
  * In order to permit lightweight reactives, this base class does not
  * offer information about its dependents or its dependency level. See
  * subclass `Dependency`.
  *
  * Two reactives are said to ''behave'' the same when they hold the same messages at the
  * same time throughout their life time.
  */
trait Reactive[+Msg, +Now] { outer =>
	/** The current value of this reactive.
	  */
	protected[react] def _value: Now

	/** Adds a dependent to this reactive.
	  *
	  * subclasses could trade memory for speed by memorizing dependencies,
	  * but the vast majority of times we are also interested in the value
	  * on subscription anyways
	  */
	def subscribe(dep: Dependent)

	/** Returns the current value of this reactive and subscribes the given observer.
	  * Might be more efficient than consecutively calling now and subscribe.
	  *
	  * Implementation note: All code coming from outside of this signal accessing
	  * the current value cache *must* go through this method in order to be able to catch level
	  * mismatches.
	  */
	def current(dep: Dependent): Now = {
		subscribe(dep)
		_value
	}

	/** The current value of this reactive.
	  */
	def now: Now = current(Observer.Nil)

	/** Fast path of `msg`, which also subscribes the given dependent.
	  */
	protected[react] def message(dep: Dependent): Option[Msg]

	/** The message being reported during the current propagation cycle.
	  * Not allowed to be called outside of a propagation cycle, i.e., code initiated by the reactive
	  * engine.
	  */
	def msg: Option[Msg] = message(Observer.Nil)

	/** A signal view for this reactive. Returns a signal which always holds the current value of
	  * this reactive.
	  */
	def toSignal: Signal[Now] = new Signal[Now] {
		protected[react] def _value = outer._value
		def cached: Signal[Now] = new ValueCachingSignal[Now] {
			protected def evaluateAndConnect() = _value = outer.current(this)
		}
		def subscribe(dep: Dependent) = outer subscribe dep
		def message(dep: Dependent): Option[Now] = outer message dep map (_ => outer.now)
	}

	/** An event view for this reactive. Returns an event stream that emits the new value of
	  * this reactive every time it changes.
	  */
	def toEvents: Events[Msg] = new Events[Msg] {
		def message(dep: Dependent) = outer message dep
		def subscribe(dep: Dependent) = outer subscribe dep
	}
}