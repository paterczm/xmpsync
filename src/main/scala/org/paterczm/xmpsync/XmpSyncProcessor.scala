package org.paterczm.xmpsync

case class XmpSyncResult(localImagesMatched: Int, remoteImagesUpdated: Int, error: Option[Exception] = None)

class XmpSyncProcessorException(val result: XmpSyncResult, cause: Throwable) extends Exception(cause: Throwable)

trait XmpSyncProcessor {
  def rate(from: Int, to: Int): XmpSyncResult
  def totalInScope(): Int
}