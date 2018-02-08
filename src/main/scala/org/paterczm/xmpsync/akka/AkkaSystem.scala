package org.paterczm.xmpsync.akka

import akka.actor._
import org.paterczm.xmpsync.model.SMImage
import org.slf4j.LoggerFactory
import akka.routing.ActorRefRoutee
import akka.routing.Router
import akka.routing.RoundRobinRoutingLogic
import org.paterczm.xmpsync.xmp.XMP
import org.paterczm.xmpsync.model.SMRating
import org.paterczm.xmpsync.XmpSyncProcessor
import org.paterczm.xmpsync.XmpSyncProcessor
import org.paterczm.xmpsync.XmpSyncResult
import org.paterczm.xmpsync.XmpSyncProcessorException
import akka.dispatch.sysmsg.Resume
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import scala.concurrent.duration._
import java.net.SocketTimeoutException
import akka.routing.RoundRobinPool

case class ImageBatch(from: Int, to: Int) {
	def count = to - from + 1
}

case class ImageBatchCompleted(batch: ImageBatch, result: XmpSyncResult)

case class ImageBatchFailed(batch: ImageBatch, error: Throwable)

case object Start

class Master(processor: XmpSyncProcessor, threads: Int, batchSize: Int) extends Actor {

	val logger = LoggerFactory.getLogger(this.getClass);

	var batches: List[ImageBatch] = _

	var failures = 0

	var total: Int = _
	var batchesProcessed: Int = 0
	var batchesAbandoned: Int = 0
	var localImagesMatched: Int = 0
	var remoteImagesUpdated: Int = 0


	// TODO: worker is checking all exceptions, so this will never be used
	val supervisor = OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = 1 minute) {
    case _ ⇒ SupervisorStrategy.Restart
  }

	val poolProps = RoundRobinPool(threads, supervisorStrategy = supervisor).props(Worker.props(processor))

	val router: ActorRef = context.actorOf(poolProps, "router")

	private def onBatchProcessedSuccessfully(result: XmpSyncResult) {
		batchesProcessed += 1
		localImagesMatched += result.localImagesMatched
		remoteImagesUpdated += result.remoteImagesUpdated

		shutdownIfDone()
	}

	private def onBatchFailed(batch: ImageBatch) {
		failures += 1

		if (failures >= Master.acceptedFailures) {
			logger.error(s"Failed processing $batch, but will not retry")

			batchesProcessed += 1
			batchesAbandoned += 1

			shutdownIfDone()
		} else {
			router ! batch
		}
	}

	private def shutdownIfDone() {
		if (batchesProcessed == batches.size) {
			logger.info(s"Processing of ${batchesProcessed} batches complete. ${batchesAbandoned} batches abandoned due to failures, totalRemoteImages=$total, localImagesMatched=$localImagesMatched, remoteImagesUpdated=$remoteImagesUpdated")

			context.system.terminate()
		}
	}

	def receive = {
		case ImageBatchCompleted(batch, result) => {
			logger.debug(s"""Processed $batch""")

			onBatchProcessedSuccessfully(result)
		}
		case ImageBatchFailed(batch, error) => {
			logger.warn(s"Failed processing $batch, but will retry", error)

			onBatchFailed(batch)
		}
		case Start => {

			total = processor.totalInScope()

			batches = Master.createBatches(total, batchSize)

			logger.info(s"""About to process total images in ${batches.size} batches""")

			batches foreach { batch =>
				router ! batch
			}

		}
	}

}

object Master {

	val acceptedFailures = 5

	def props(processor: XmpSyncProcessor, threads: Int, batchSize: Int): Props = Props(new Master(processor: XmpSyncProcessor, threads: Int, batchSize: Int))

	def createBatches(total: Int, batchSize: Int): List[ImageBatch] = {
		var i = 1

		val list = scala.collection.mutable.MutableList[ImageBatch]()

		while (i <= total) {
			if (i + batchSize <= total) {
				list += ImageBatch(i, i + batchSize - 1)
			} else {
				list += ImageBatch(i, total)
			}

			i += batchSize
		}

		list.toList
	}

}

class Worker(processor: XmpSyncProcessor) extends Actor {

	val logger = LoggerFactory.getLogger(this.getClass);

	def receive = {
		case batch: ImageBatch => {

			try {
				logger.debug(s"""${self.path.name}: Got $batch to process""")

				val result = processor.rate(batch.from, batch.to)

				sender() ! ImageBatchCompleted(batch, result)
			} catch {
				case t: Throwable => sender() ! ImageBatchFailed(batch, t)
			}
		}

	}

}

object Worker {
	def props(processor: XmpSyncProcessor): Props = Props(new Worker(processor: XmpSyncProcessor))
}
