package scala.react.test

import org.scalatest.junit.ShouldMatchersForJUnit._

import org.scalatest.FunSuite

trait ReactiveTests extends FunSuite with ReactiveTestUtils {
	import scala.react._

	test("basicDef1") {
		val x = Var(1)
		assert(x.now === 1)
		assert(x.current(Observer.Nil) === 1)
		assert(x.message(Observer.Nil) === None)
		val y = Signal { assert(x() === 1) }
		y.now
	}

	test("basicDef2") {
		val x = Var(1)
		val y = Var(2)
		val sum = Signal { x() + y() }
		assert(sum.now === 3)
	}

	test("basicNonInjective") {
		val msgs = new MsgLog

		// mostly testing messages here:
		val x = Var(2)
		val y = Var(0)
		val xy = Cache { x() * y() }
		val mock = mockOb(xy) {}
		val mockX = mockOb(x) { msgs.log(xy.message(Observer.Nil)) }
		val mockY = mockOb(y) { msgs.log(xy.message(Observer.Nil)) }
		assert(xy.now === 0)
		x() = 3
		assert(xy.now === 0)
		y() = 10
		assert(xy.now === 30)
		mock.currents.assert(30)
		msgs.assert(None, Some(30))
	}

	test("basicGlitch") {
		val x = Var(true)
		val y = Cache { !x() }
		val res = Cache { x() && y() } // always false!
		val mock = mockOb(res) {}
		assert(res.now === false)
		x() = false
		assert(res.now === false)
		x() = true
		assert(res.now === false)
		mock.currents.assert() // no currents please
		mock.messages.assert() // no messages please
	}

	test("basicPropagate1") {
		val x = Var(1)
		val x2 = Cache { 2 * x() }
		val mock = mockOb(x2) {}
		assert(x2.now === 2)
		assert(x2.message(Observer.Nil) == None)
		assert(x2.level === 1)
		x2.dependents should equal(Set(mock))
		x.dependents should equal(Set(x2))
		x() = 3
		assert(x2.now === 6)
		x() = 10
		assert(x2.now === 20)
		mock.currents.assert(6, 20)
		mock.messages.assert(6, 20)
	}

	test("basicPropagate2") {
		val x = Var(1)
		val y = Var(2)
		val sum = Cache { x() + y() }
		val mock = mockOb(sum)()
		assert(sum.current(mock) === 3)
		assert(sum.level === 1)
		sum.dependents should equal(Set(mock))
		x.dependents should equal(Set(sum))
		y.dependents should equal(Set(sum))
		assert(sum.now === 3)
		x() = 10
		assert(sum.now === 12)
		y() = 3
		assert(sum.now === 13)
		x() = 20
		assert(sum.now === 23)
		x() = 30
		y() = 4
		assert(sum.now === 34)
		mock.currents.assert(12, 13, 23, 33, 34)
	}

	// do not subscribe (so don't trigger any subscription side effects!)
	test("basicPropagate3") {
		val x = Var(1)
		val x2 = Cache { 2 * x() }
		assert(x2.now === 2)
		assert(x2.message(Observer.Nil) == None)
		assert(x2.level === 1)
		x2.dependents should equal(Set())
		x.dependents should equal(Set(x2))
		x() = 3
		assert(x2.now === 6)
		x() = 10
		assert(x2.now === 20)
	}

	test("deepCachedPropagate") {
		val x = Var(1)
		val y = Var(2)
		val sum = Cache { x() + y() }
		val prod = Cache { x() * y() }
		val res = Cache { sum() + prod() }
		assert(sum.now === 3)
		assert(prod.now === 2)
		assert(res.now === 5)
		val mock = mockOb(res)()

		x.dependents should equal(Set(sum, prod))
		y.dependents should equal(Set(sum, prod))
		sum.dependents should equal(Set(res))
		prod.dependents should equal(Set(res))

		x() = 7
		assert(sum.now === 9)
		assert(prod.now === 14)
		assert(res.now === 23)
		mock.currents.assert(23)
	}

	test("deepCachedPropagateWithIntermediateVar") {
		val x = Var(1)
		val y = Var(2)
		val sum = Cache { x() + y() }
		val prod = Cache { x() * y() }
		val z = Var(3)
		val res = Cache { z() * (sum() + prod()) }
		assert(sum.now === 3)
		assert(prod.now === 2)
		assert(res.now === 3 * 5)

		val mock = mockOb(res)()

		x() = 7
		assert(sum.now === 9)
		assert(prod.now === 14)
		assert(res.now === 3 * 23)
		z() = 10
		assert(sum.now === 9)
		assert(prod.now === 14)
		assert(res.now === 10 * 23)
		mock.currents.assert(3 * 23, 10 * 23)
	}

	test("deepMixedPropagateWithIntermediateVar") {
		val x = Var(1)
		val y = Var(2)
		val sum = Signal { x() + y() }
		val prod = Cache { x() * y() }
		val z = Var(3)
		val res = Cache { z() * (sum() + prod()) }
		assert(sum.now === 3)
		assert(prod.now === 2)
		assert(res.now === 3 * 5)

		val mock = mockOb(res)()

		x() = 7
		assert(sum.now === 9)
		assert(prod.now === 14)
		assert(res.now === 3 * 23)
		z() = 10
		assert(sum.now === 9)
		assert(prod.now === 14)
		assert(res.now === 10 * 23)

		mock.currents.assert(3 * 23, 10 * 23)
	}

	test("levelMismatch") {
		val x = Var(2) // level 0
		val x2 = Cache { 2 * x() } // level 1
		val x6 = Cache { 3 * x2() } // level 2
		val cond = Cache { if (x() == 2) x2() else x6() } // level 2 or 3
		assert(x.now === 2)
		assert(x2.now === 4)
		assert(x6.now === 12)
		assert(cond.now === 4)

		val mock = mockOb(cond)()

		x.dependents should equal(Set(x2, cond))
		x2.dependents should equal(Set(cond, x6))
		x6.dependents should equal(Set())
		assert(x.level === 0)
		assert(x2.level === 1)
		assert(x6.level === 2)
		assert(cond.level === 2)
		// x6 has no dependents, so cond will try to reevaluate with an outdated (but seemingly valid) x6

		x() = 3
		assert(x.now === 3)
		assert(cond.now === 18)
		assert(x6.now === 18)
		assert(cond.level === 3)

		mock.currents.assert(18)
	}

	test("levelMismatchWithNow") {
		val x = Var(1) // level 0
		val x2 = Cache { 2 * x() } // level 1
		val x6 = Cache { 3 * x2() } // level 2
		val x24 = Cache { 4 * x6() } // level 3
		val cond = Cache { if (x() <= 2) x2() else x24.now } // level 2 or 4

		assert(x.now === 1)
		assert(x2.now === 2)
		assert(x6.now === 6)
		assert(x24.now === 24)
		assert(cond.now === 2)
		assert(cond.level === 2)

		x() = 2
		assert(x.now === 2)
		assert(x2.now === 4)
		assert(x6.now === 12)
		assert(x24.now === 48)
		assert(cond.now === 4)
		assert(cond.level === 2)

		val mock = mockOb(cond)()

		x.dependents should equal(Set(x2, cond))
		x2.dependents should equal(Set(cond, x6))
		x6.dependents should equal(Set(x24))
		x24.dependents should equal(Set())

		x() = 3
		assert(x.now === 3)
		assert(cond.now === 72)
		assert(x2.now === 6)
		assert(x6.now === 18)
		assert(x24.now === 72)
		assert(cond.level === 4)

		mock.currents.assert(72)
	}

	test("mutuallyRecursiveSignals") {
		val c = Var(true)
		object o {
			val x: CachedSignal[Int] = Cache { if (c()) 0 else y() }
			val y: CachedSignal[Int] = Cache { if (c()) x() else 1 }
		}
		import o._
		val mockX = mockOb(o.x) {}
		val mockY = mockOb(o.y) {}
		assert(x.now === 0)
		assert(y.now === 0)
		assert(x.level === 1)
		assert(y.level === 2)
		c() = false
		assert(x.now === 1)
		assert(y.now === 1)
		c() = true
		c() = false
		c() = true
		assert(x.level === 5)
		assert(y.level === x.level + 1)
	}

	test("signalDotChanges") {
		val x = Var(2)
		val y = Var(0)
		val es = Cache { x() * y() }.changes
		val mock = mockOb(es) {}
		x() = 3 // nope
		y() = 10
		y() = 0
		x() = 4 // nope
		x() = 5 // nope
		y() = 10
		x() = 5 // nope
		mock.messages.assert(30, 0, 5 * 10)
	}

	test("eventSource") {
		val es = new EventSource[Int]
		val mock = mockOb(es) {}
		es.dependents should equal(Set(mock))

		es emit 1
		es emit 2
		es emit 2
		es emit 3
		mock.messages.assert(1, 2, 2, 3)
	}

	test("eventsDotSelect") {
		val es = new EventSource[Int]
		val res = es collect {
			case 1 => "Yeah"
			case 2 => "Yes"
		}
		val mock = mockOb(res) {}
		es emit 1
		es emit 2
		es emit 2
		es emit 3
		es emit 10
		es emit 1
		mock.messages.assert("Yeah", "Yes", "Yes", "Yeah")
	}

	test("eventsDotHold") {
		val es = new EventSource[Int]
		val res = es.hold(0)
		val mock = mockOb(res) {}
		assert(res.now === 0)
		es emit 1
		es emit 2
		es emit 2
		es emit 3
		es emit 10
		mock.currents.assert(1, 2, 3, 10)
		mock.messages.assert(1, 2, 3, 10)
	}

	test("eventsDotTake") {
		val es = new EventSource[Int]
		val res = es.take(3)
		val mock = mockOb(res) {}
		es emit 1
		es emit 2
		es emit 3
		mock.messages.assert(1, 2, 3)
		es emit 4
		es emit 5
		mock.messages.assert(1, 2, 3)
	}

	test("eventsDotHappended") {
		val es = new EventSource[Int]
		val res = es.happened
		val mock = mockOb(res) {}
		assert(res.now === false)
		es emit 1
		assert(res.now === true)
		es emit 0
		assert(res.now === true)
		mock.currents.assert(true)
	}

	test("eventsDotSwitch") {
		val es = new EventSource[Int]
		val sig1 = Var(0)
		val sig2 = Var(100)
		val res = es switch (sig1, sig2)
		val mock = mockOb(res) {}
		assert(res.now === 0)
		sig1() = 1
		assert(res.now === 1)
		sig2() = 101 // nope
		assert(res.now === 1)
		es emit 1
		assert(res.now === 101)
		sig1() = 2 // nope
		assert(res.now === 101)
		es emit 2 // nope
		assert(res.now === 101)
		sig2() = 102
		assert(res.now === 102)
	}

	test("signalDotFlattenEvents") {
		val es1 = new EventSource[Int]
		val es2 = new EventSource[Int]

		val sig = Var(es1)
		val res = sig.flatten //(eventConstructor _)//Events
		val mock = mockOb(res) {}
		es1 emit 1
		sig() = es1
		es1 emit 2
		es1 emit 2
		sig() = es2
		mock.messages.assert(1, 2, 2)
	}

	test("signalDotFlattenSignal") {
		val s1 = Var(0)
		val s2 = Var(10)

		val sig = Var(s1)
		val res = sig.flatten //(eventConstructor _)//Events
		val mock = mockOb(res) {}
		s1() = 1
		sig() = s1
		s1() = 2
		s1() = 2 // no
		s1() = 3
		sig() = s2
		s1() = 4 // no
		s2() = 11
		mock.messages.assert(1, 2, 3, 10, 11)
	}

}