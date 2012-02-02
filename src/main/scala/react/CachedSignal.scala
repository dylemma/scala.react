/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react

class CachedSignal[A](op: => A) extends ValueCachingSignal[A] {
	evaluateAndConnect()
	// Attribute to dynamic dep graphs:
	// always subscribe this if it has dependents, as dependencies might have changed
	protected def evaluateAndConnect() = {
		_value = Signal.withDep(this)(op)
		_value
	}
}
