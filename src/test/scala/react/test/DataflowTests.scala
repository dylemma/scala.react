package scala.react.test

import org.scalatest.FunSuite

trait DataflowTests extends FunSuite with ReactiveTestUtils {
	import scala.react._

	test("simpleReactor") {
		val es = new EventSource[Int]
		val states = new ListLog[Int]
		val res = Reactor.loop { self =>
			states.log(1)
			val x = self next es
			states.log(2)
			self.delay
		}
		states assert 1
		es emit 0
		states assert (1, 2)
		advanceTime()
		states assert (1, 2, 1)
		es emit 1
		states assert (1, 2, 1, 2)
	}

	test("simpleDataflowEvent") {
		val es = new EventSource[Int]
		val states = new ListLog[Int]
		val res = Events.once[Int] { self =>
			states.log(1)
			val x = self next es
			states.log(2)
			self emit 1
			states.log(3)
		}
		val mock = mockOb(res) {}
		states assert 1
		es emit 0
		states assert (1, 2)
		mock.messages assert 1
		advanceTime()
		states assert (1, 2, 3)
	}

	test("basicSeqEvent1") {
		val es1 = new EventSource[Int]
		val es2 = new EventSource[Int]
		val states = new ListLog[Int]
		val res = Events.once[Int] { self =>
			states.log(1)
			val x = self next es1
			states.log(2)
			self emit 1
			states.log(3)
			val y = self next es2
			states.log(4)
			self emit 2
			states.log(5)
		}
		val mock = mockOb(res) {}
		states assert 1
		es2 emit 0 // no effect
		states assert 1
		es1 emit 0
		states assert (1, 2)
		es1 emit 0 // no effect
		states assert (1, 2, 3)
		es2 emit 0
		advanceTime()
		states assert (1, 2, 3, 4, 5)
		mock.messages assert (1, 2)
	}

	test("basicSeqEvent2") {
		val s1 = Var(0)
		val s2 = Var(0)
		val states = new ListLog[Int]
		val res = Events.once[Int] { self =>
			states.log(1)
			val x = self next s1
			states.log(2)
			self emit 1
			states.log(3)
			val y = self next s2
			states.log(4)
			self emit 2
			states.log(5)
		}
		val mock = mockOb(res) {}
		states assert 1
		s2() = 1 // no effect
		states assert 1
		s1() = 0 // no effect
		states assert 1
		mock.messages assert ()
		s1() = 1
		states assert (1, 2)
		mock.messages assert (1)
		s1() = 2 // no effect
		states assert (1, 2, 3)
		mock.messages assert (1)
		s2() = 2
		states assert (1, 2, 3, 4)
		mock.messages assert (1, 2)
		advanceTime()
		states assert (1, 2, 3, 4, 5)
		mock.messages assert (1, 2)
	}

	test("basicSeqEventLoop") {
		val es1 = new EventSource[Int]
		val states = new ListLog[Int]
		val res = Events.loop[Int] { self =>
			states.log(1)
			val x = self next es1
			states.log(2)
			self emit x
		}
		val mock = mockOb(res) {}
		states assert 1
		es1 emit 1
		states assert (1, 2)
		mock.messages assert (1)
		advanceTime()
		states assert (1, 2, 1)
		es1 emit 2
		states assert (1, 2, 1, 2)
		mock.messages assert (1, 2)
	}

	test("basicSeqEventLoopWithDelay") {
		val es1 = new EventSource[Int]
		val states = new ListLog[Int]
		val res = Events.loop[Int] { self =>
			states.log(1)
			val x = self next es1
			states.log(2)
			if (x % 2 == 0) self emit x
			else self.delay
		}
		val mock = mockOb(res) {}
		states assert 1
		es1 emit 2
		states assert (1, 2)
		mock.messages assert (2)
		advanceTime()
		states assert (1, 2, 1)
		es1 emit 1
		states assert (1, 2, 1, 2)
		mock.messages assert (2)
		advanceTime()
		states assert (1, 2, 1, 2, 1)
		es1 emit 4
		states assert (1, 2, 1, 2, 1, 2)
		mock.messages assert (2, 4)
		advanceTime()
		states assert (1, 2, 1, 2, 1, 2, 1)
	}

	test("basicSeqSignal1") {
		val s1 = Var(0)
		val s2 = Var(0)
		val states = new ListLog[Int]
		// DataflowSignal.once(Val(0))
		val res = Val(0) once { self =>
			states.log(1)
			val x = self next s1
			states.log(2)
			self emit 1
			states.log(3)
			val y = self next s2
			states.log(4)
			self emit 2
			states.log(5)
		}
		assert(res.now === 0)
		val mock = mockOb(res) {}
		states assert 1
		s2() = 1 // no effect
		states assert 1
		assert(res.now === 0)
		s1() = 0 // no effect
		states assert 1
		assert(res.now === 0)
		s1() = 1
		states assert (1, 2)
		assert(res.now === 1)
		s1() = 2 // no effect
		states assert (1, 2, 3)
		s2() = 2
		assert(res.now === 2)
		states assert (1, 2, 3, 4)
		advanceTime()
		states assert (1, 2, 3, 4, 5)
		mock.messages assert (1, 2)
	}

	// test that an emit has precedence over a previous switchTo
	// testing also that we evaluate the dataflow signal after its depedencies (level change!)
	test("basicSwitchToSignal") {
		val s1 = Var(1) // level 0
		val s2 = Cache(if (s1() >= 10) s1() else 0) // level 1
		val states = new ListLog[Int]
		val res = Val(0) once { self =>
			states.log(1)
			self switchTo s1 // level 1
			states.log(2)
			val x = self next s2 // level 2
			states.log(3)
			self emit x
			states.log(4)
		}
		assert(res.now === 1)
		states assert 1
		val mock = mockOb(res) {}
		s1() = 2
		states assert (1, 2)
		mock.messages assert (2)
		assert(res.now === 2)
		assert(s2.now === 0)
		s1() = 3
		assert(res.now === 3)
		assert(s2.now === 0)
		states assert (1, 2)
		s1() = 3 // no effect
		assert(res.now === 3)
		states assert (1, 2)
		s1() = 10
		states assert (1, 2, 3)
		assert(res.now === 10)

		mock.messages assert (2, 3, 10)
	}

	// test that an emit has precedence over a previous switchTo
	// testing also that we evaluate the dataflow event stream after its depedencies (level change!)
	test("basicSwitchToEvent") {
		val e1 = EventSource[Int] // level 0
		val e2 = e1 filter (_ >= 10)
		val states = new ListLog[Int]
		val res = Events.once[Int] { self =>
			states.log(1)
			self switchTo e1 // level 1
			states.log(2)
			val x = self next e2 // level 2
			states.log(3)
			self emit x
			states.log(4)
		}
		states assert 1
		val mock = mockOb(res) {}
		e1 emit 2
		states assert (1, 2)
		mock.messages assert (2)
		e1 emit 3
		states assert (1, 2)
		mock.messages assert (2, 3)
		e1 emit 3
		states assert (1, 2)
		mock.messages assert (2, 3, 3)
		e1 emit 10
		states assert (1, 2, 3)

		mock.messages assert (2, 3, 3, 10)
	}

	test("dataflowSignalDotPrevious") {
		val states = new ListLog[Int]
		val res = Val(0) loop { self =>
			states.log(1)
			val x = self.previous
			states.log(2)
			self emit x + 1
			states.log(3)
		}
		states assert (1, 2)
		assert(res.now === 1)
		advanceTime()
		states assert (1, 2, 3, 1, 2)
		assert(res.now === 2)
		advanceTime()
		states assert (1, 2, 3, 1, 2, 3, 1, 2)
		assert(res.now === 3)
	}

	test("dataflowSignalEmitLoop") {
		val states = new ListLog[Int]
		var x = 0
		val res = Val(0) loop { self =>
			states.log(1)
			x += 1
			self emit x
			states.log(2)
		}
		states assert (1)
		assert(res.now === 1)
		advanceTime()
		states assert (1, 2, 1)
		assert(res.now === 2)
		advanceTime()
		states assert (1, 2, 1, 2, 1)
		assert(res.now === 3)
	}

	test("dataflowSignalEmitSameValueLoop") {
		// test that a redundant emit delays
		val states = new ListLog[Int]
		val res = Val(0) loop { self =>
			states.log(1)
			self emit 1
			states.log(2)
		}
		states assert (1)
		assert(res.now === 1)
		advanceTime()
		states assert (1, 2, 1)
		assert(res.now === 1)
		advanceTime()
		states assert (1, 2, 1, 2, 1)
		assert(res.now === 1)
	}

	test("dataflowSignalWithEventNever") {
		val states = new ListLog[Int]
		val res = Val(0) loop { self =>
			states.log(1)
			val x = self next Events.Never
			states.log(2)
		}
		states assert (1)
		assert(res.now === 0)
		advanceTime()
		states assert (1)
		assert(res.now === 0)
	}

	test("dataflowSignalWithEventNow") {
		val states = new ListLog[Int]
		var x = 10
		val res = Val(0) loop { self =>
			states.log(1)
			println(1)
			x += 1
			val y = self next Events.Now(x)
			states.log(2)
			println(2)
			self emit y
			println(3)
			states.log(3)
		}
		states assert (1, 2)
		assert(res.now === 11)
		advanceTime()
		states assert (1, 2, 3, 1, 2)
		assert(res.now === 12)
		advanceTime()
		states assert (1, 2, 3, 1, 2, 3, 1, 2)
		assert(res.now === 13)
	}

}
