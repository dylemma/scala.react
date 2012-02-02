/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react

/** A signal which can be mutated and emit change messages. Serves as an interface between
  * the reactive and non-reactive world.
  */
abstract class SourceSignal[A] extends SourceReactive[A, A] with Signal[A] {
	/** Convenience method. Emits a change message.
	  */
	protected def emit() { emit(_value) }
	override def cached = this
}