/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react

trait SourceReactive[M, N] extends Publisher[M, N] {
	def emit(msg: M) {
		propagate(msg)
	}

	def level = 0
	def valid = true
	private[react] def invalidate() {}
}