package org.paterczm.xmpsync.akka

import scala.collection.mutable.Set
import scala.concurrent.Await
import scala.concurrent.duration._

import org.junit.Assert
import org.junit.Test

import akka.actor.ActorSystem
import org.paterczm.xmpsync.XmpSyncResult
import org.paterczm.xmpsync.XmpSyncProcessor
import org.paterczm.xmpsync.XmpSyncProcessorException

class AkkaTest {

	@Test
	def batchTest() {

		val batches = Master.createBatches(110, 25)

		Assert.assertEquals(5, batches.size)
		Assert.assertEquals(ImageBatch(1, 25), batches(0))
		Assert.assertEquals(ImageBatch(26, 50), batches(1))
		Assert.assertEquals(ImageBatch(51, 75), batches(2))
		Assert.assertEquals(ImageBatch(76, 100), batches(3))
		Assert.assertEquals(ImageBatch(101, 110), batches(4))
	}

	abstract class TestProcessor extends XmpSyncProcessor() {

		val called: Set[ImageBatch] = Set()

		def rate(from: Int, to: Int): XmpSyncResult = synchronized {
			val batch = ImageBatch(from, to)
			called += batch
			XmpSyncResult(batch.count, batch.count)
		}
	}

	@Test
	def testSingleImageSingleThread() {

		val testProcessor = new TestProcessor() {
			def totalInScope() = 1
		}

		val system = ActorSystem("xmpsync")

		val master = system.actorOf(Master.props(testProcessor, 1, 1), "master")

		master ! Start

		Await.ready(system.whenTerminated, Duration(10, MINUTES))

		Assert.assertEquals(1, testProcessor.called.size)
		Assert.assertTrue(testProcessor.called.contains(ImageBatch(1, 1)))
	}

	@Test
	def testMultiImageMultiThreaded() {

		val testProcessor = new TestProcessor() {
			def totalInScope() = 1001
		}

		val system = ActorSystem("xmpsync")

		val master = system.actorOf(Master.props(testProcessor, 5, 25), "master")

		master ! Start

		Await.ready(system.whenTerminated, Duration(10, MINUTES))

		Master.createBatches(1001, 25) foreach (batch => Assert.assertTrue(s"Expected $batch", testProcessor.called.contains(batch)))

		Assert.assertEquals(41, testProcessor.called.size)
	}

	// smugmug throttling
	@Test
	def testIntermittentError() {
		val testProcessor = new TestProcessor() {

			var failed = false

			def totalInScope() = 1001

			override def rate(from: Int, to: Int): XmpSyncResult = {
				if (!failed && ImageBatch(from, to) == ImageBatch(26, 50)) {
					failed = true
				  throw new XmpSyncProcessorException(XmpSyncResult(0, 0), new Exception("Error!"))
				} else {
				  super.rate(from, to)
				}
			}
		}

		val system = ActorSystem("xmpsync")

		val master = system.actorOf(Master.props(testProcessor, 5, 25), "master")

		master ! Start

		Await.ready(system.whenTerminated, Duration(15, SECONDS))

		Assert.assertEquals(41, testProcessor.called.size)
		Assert.assertTrue(testProcessor.called.contains(ImageBatch(26, 50)))
	}

	@Test
	def testPersistentError() {
		val testProcessor = new TestProcessor() {
			def totalInScope() = 1001

			var failures = 0

			override def rate(from: Int, to: Int): XmpSyncResult = {
				val name = Thread.currentThread().getName
				if (ImageBatch(from, to) == ImageBatch(26, 50)) {
					failures+=1
				  throw new Exception("Error!")
				} else {
				  super.rate(from, to)
				}
			}
		}

		val system = ActorSystem("xmpsync")

		val master = system.actorOf(Master.props(testProcessor, 5, 25), "master")

		master ! Start

		Await.ready(system.whenTerminated, Duration(15, SECONDS))

		Assert.assertEquals(40, testProcessor.called.size)
		Assert.assertEquals(Master.acceptedFailures, testProcessor.failures)
		Assert.assertFalse(testProcessor.called.contains(ImageBatch(26, 50)))
	}
}