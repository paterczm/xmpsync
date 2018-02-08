package org.paterczm.xmpsync

import org.paterczm.xmpsync.xmp.XMP
import org.paterczm.xmpsync.xmp.RecursiveJPGProcessor
import java.io.File
import org.slf4j.LoggerFactory

import org.paterczm.xmpsync.model.SMRating

class SMXmpSyncProcessor(localImages: SMXmpLookup, smClient: SMClient, scope: SMClient.SMScope, dryRun: Boolean = true) extends XmpSyncProcessor {

	val logger = LoggerFactory.getLogger(this.getClass);

	def totalInScope() = smClient.searchImages(scope, 1, 0).Pages.Total

	// thread safe
	def rate(from: Int, to: Int): XmpSyncResult = {

		logger.debug(s"""Rating $scope""")

		val response = smClient.searchImages(scope, from, to-from+1)

		var remoteImagesUpdated = 0
		var localImagesMatched = 0

		try {
		response.Image.zipWithIndex foreach {
			case (image, i) => {

				if (image.FileName.toLowerCase().endsWith("jpg")) {

					val metadata = smClient.getMetadata(image).ImageMetadata

					logger.debug(s"Processing $image $metadata")

					localImages.lookup(image, metadata) match {
						case None => {
							logger.debug(s"""Ignoring remote ${image}, because it's not found locally""")
						}
						case Some(xmp) => {
							localImagesMatched += 1

							xmp.rating match {
								case Some(rating) => {
									val ratedImage = xmp.rating match {
										case Some(r) => image.keywords(xmp.tags).rate(SMRating(r))
										case None => image.keywords(xmp.tags)
									}

									if (image.KeywordArray.sorted != ratedImage.KeywordArray.sorted) {
										if (!dryRun) {
											logger.info(s"""Updating tags for ${image} to ${ratedImage.KeywordArray}""")
											smClient.updateImageKeywords(ratedImage)
											remoteImagesUpdated += 1
										} else {
											logger.info(s"""$i: Would update tags for ${image} to ${ratedImage.KeywordArray}""")
										}
									} else {
										logger.debug(s"""Ignoring ${image}, because it's local and remote tags are matching""")
									}
								}
								case None => logger.debug(s"""Ignoring ${image}, because it's not rated""")
							}
						}
					}

				} else {
					logger.debug(s"Ignoring remote ${image.FileName}, because it's not a jpg")
				}
			}
		}

		XmpSyncResult(localImagesMatched, remoteImagesUpdated, None)
		} catch {
			case e: Exception => throw new XmpSyncProcessorException(XmpSyncResult(localImagesMatched, remoteImagesUpdated, Some(e)), e)
		}
	}

}