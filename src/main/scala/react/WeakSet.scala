/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react

import scala.collection.JavaConversions._

/*trait Dependents {
  def foreach[U](f: Any => U): Unit {
    
  }
}

trait HasDependents {
  def dependents: Dependents
  
  private[react] def clearDependents(): Dependents
}

trait MemorizesDependents {
  private[this] var _dependents = WeakSet.
  def dependents
  
  private[react] def clearDependents() = { 
    val old = _dependents
    _dependents = newDependents
    old
  }
}*/

// TODO: implementation critical:
// - Remove level of indirection.
// - Hash table backing wasteful. Specialized impl. for small element counts 
// - support traverse and remove combi operation (expunge while traversing) 
protected[react] class WeakHashSet[A] extends scala.collection.mutable.Set[A] {
	private val map = new java.util.WeakHashMap[A, AnyRef]
	override def size = map.size
	def iterator = JIteratorWrapper(map.keySet.iterator)
	def contains(elem: A): Boolean = map.containsKey(elem)
	def +=(elem: A): this.type = { map.put(elem, null); this }
	def -=(elem: A): this.type = { map.remove(elem); this }
	override def add(elem: A): Boolean = map.put(elem, null) == null
	override def remove(elem: A): Boolean = map.remove(elem) != null
	override def clear = map.clear
	override def empty = new WeakHashSet[A]
}
/*
import scala.collection.Set
import java.lang.ref.{WeakReference => WeakRef}

object WeakSet {
  override def empty[A]: Set[A] = EmptySet.asInstanceOf[Set[A]]
  
  private object EmptySet extends Set[Any] {
    override def size: Int = 0
    def contains(elem: Any): Boolean = false
    def + (elem: Any): Set[Any] = new WeakSet1(elem)
    def - (elem: Any): Set[Any] = this
    def iterator: Iterator[Any] = Iterator.empty
    override def foreach[U](f: Any =>  U): Unit = {}
  }
  
  private class Iterator[A] extends scala.collection.Iterator[A] {
    private var _idx: Int = 0
    private var _next: A = null
    def next(): A = {
      if (!_hasNext) throw new NoSuchElementException("reached iterator end")
      idx += 1
      _next
    }
  }
}

class WeakSet1[A](a: A) extends WeakRef(a) with Set[A] {
  override def size: Int = 1
  def iterator = new WeakSet.Iterator[A] {
    def hasNext = {
      if(_idx > 0) return false
      _next = this.get
      _next != null  
    }
  }
  def contains(elem: A): Boolean = this.get == elem
  def +(elem: A): Self = 
    if(this.get == elem) this 
    else if(this.get == null) new WeakSet1(elem) 
    else new WeakSet2(this, elem)
  def -(elem: A): Self = 
    if(this.get == null) this 
    else if(this.get == elem) ImSet.empty 
    else this
  override def foreach[U](f: Any =>  U): Unit = {
    val e = this.get
    if (e != null) f(e)
  }
}

class WeakSet2[A](private val ref1: WeakRef[A], a2: A) extends Set[A] {
  override def size: Int = 2
  def iterator = new WeakSet.Iterator[A] {
    def hasNext = {
      _idx match {
        case 0 => _next = ref1.get
        case 1 => _next = ref2.get
      }
      _next != null  
    }
  }
  def contains(elem: A): Boolean = this.get == elem
  def +(elem: A): Self = 
    if(contains(elem)) this 
    else if(ref1.get == null) new WeakSet2(ref2, elem)
    else if(ref2.get == null) new WeakSet2(ref1, elem)
    else new WeakSet3(ref1, ref2, elem)
  def -(elem: A): Self = 
    if(ref1.get == elem) 
      if(ref2.get == null) WeakSet.empty
      else new WeakSet1(ref2)
    else if (ref2.get == elem) 
      if(ref1.get == null) WeakSet.empty
      else new WeakSet1(ref1)
  override def foreach[U](f: Any =>  U): Unit = {
    val e1 = ref1.get
    if (e1 != null) f(e1)
    val e2 = ref2.get
    if (e2 != null) f(e2)
  }
}*/

