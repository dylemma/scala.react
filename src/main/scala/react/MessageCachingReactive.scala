/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react

trait MessageCachingReactive[+Msg, +Now] extends Dependency[Msg, Now] {
	protected[react] def message(dep: Dependent): Option[Msg] = {
		checkLevelNow()
		subscribe(dep)
		Engine.messageFor(this)
	}

	protected[this] def cacheMessage(msg: Msg) { Engine.setMessage(this, msg) }
}