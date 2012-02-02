/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react

/** For explicit observer handling, such as in library wrappers, mix in this trait into an
  * 'observing' object. The life time of observers created by methods such as `observe` and `on` are
  * automatically bound to `this` inheriting object.
  */
trait Observing { outer =>
	protected val _obRefs = new scala.collection.mutable.HashSet[Observer]
	/** An observer with a given identity */
	protected abstract class ProxyObserver(id: Any) extends PersistentObserver
		with Proxy {
		def self = id
		override def toString = "ProxyOb for " + self + " from " + outer
	}
	/* An observer that will be automatically referenced by this (enclosing) object */
	protected abstract class PersistentObserver extends Observer {
		ref(this)
		/** Unreference this observer */
		def dispose() { unref(this) }
		override def toString = "PersistentOb from " + outer
	}

	private def ref(ob: Observer) {
		// TODO: optimize for singleton and small sets
		_obRefs += ob
	}

	private def unref(ob: Observer) {
		// TODO: fall back to singletons/small sets here if possible?
		_obRefs -= ob
	}

	/** Removes all strong observer references held by `this` observing object.
	  */
	protected def clearObRefs() {
		_obRefs.clear()
	}

	/* Convenient method to observe a given publisher. The given handler 
   * should return true as long as wants to observe the publisher */
	protected def observe[M](p: Reactive[M, _])(op: M => Boolean) = {
		val ob = new PersistentObserver { pob =>
			def run() {
				p.message(Observer.Nil) match {
					case Some(msg) =>
						if (op(msg)) p.subscribe(this)
						else pob.dispose()
					case _ =>
						// stay subscribed here!
						p.subscribe(this)
				}
			}
		}
		p.subscribe(ob)
		ob
	}

	/** Applies a given function f to each event from the given stream as long
	  * as f evaluates to true.
	  */
	protected def on[A](es: Events[A])(f: A => Boolean) = observe(es)(f)

	/** Applies a given function only once with the first event from the given
	  * stream.
	  */
	protected def onceOn[A](es: Events[A])(f: A => Unit) =
		on(es) { a => f(a); false }
}
