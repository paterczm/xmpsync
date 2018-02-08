package org.paterczm.xmpsync

import org.paterczm.xmpsync.xmp.XMP
import org.paterczm.xmpsync.model.SMImage
import org.slf4j.LoggerFactory
import SMXmpLookup._
import org.paterczm.xmpsync.model.LocalImage
import org.paterczm.xmpsync.model.SMImageMetadata
import java.time.LocalDateTime
import scalaz.Forall

class SMXmpLookup(localImages: Map[String, List[LocalImage]]) {

	private def matchBySize(localImage: LocalImage, remoteImage: SMImage) = localImage.file.length() == remoteImage.OriginalSize

	private def matchByDescription(localImage: LocalImage, remoteImage: SMImage) = equalStrOptions(localImage.xmp.description, remoteImage.Caption)

	private def matchByDateTaken(localImage: LocalImage, remoteImageMetadata: SMImageMetadata) = localImage.xmp.dateTimeOriginal != None && localImage.xmp.dateTimeOriginal == Some(LocalDateTime.parse(remoteImageMetadata.DateDigitized, XMP.dateFormatter))

	private def matchLocalByRatingAndTags(xmp1: XMP, xmp2: XMP) = xmp1.rating == xmp2.rating && xmp1.tags == xmp2.tags

	private def fuzzyMatch(localImage: LocalImage, remoteImage: SMImage) = matchBySize(localImage, remoteImage) || matchByDescription(localImage, remoteImage)

	private def onlyDuplicates(localImages: List[LocalImage]) = {

		val xmps = localImages.map(_.xmp)

		xmps.forall(matchLocalByRatingAndTags(xmps(0), _))
	}

	private def createMultilineStringOfLocalImages(list: List[LocalImage]) = {
		list.map(localImage => s"  |_${localImage.file} ${localImage.xmp}").mkString("\n")
	}

	private def lookup(matchedLocalImages: List[LocalImage], remoteImage: SMImage, remoteImageMetadata: SMImageMetadata): Option[XMP] = {

		if (logger.isDebugEnabled()) {
			logger.debug(s"Found ${matchedLocalImages.size} local candidates for $remoteImage, $remoteImageMetadata\n"+createMultilineStringOfLocalImages(matchedLocalImages))
		}

		matchedLocalImages.filter(localImage => matchByDateTaken(localImage, remoteImageMetadata)) match {
			case matchedByDateTaken: List[LocalImage] if matchedByDateTaken.isEmpty => {
				logger.debug(s"${matchedByDateTaken.size} local image(s) matched by date taken")
				None
			}
			case matchedByDateTaken: List[LocalImage] if matchedByDateTaken.size == 1 => Some(matchedByDateTaken(0).xmp)
			case matchedByDateTaken: List[LocalImage] if matchedByDateTaken.size > 1 => {
				logger.debug(s"${matchedByDateTaken.size} local image(s) matched by date taken")

				if (onlyDuplicates(matchedByDateTaken)) {
					logger.debug("All matched images are the same, so returning the first one")
					return Some(matchedByDateTaken(0).xmp)
				}

				logger.warn(s"$remoteImage, $remoteImageMetadata not matched among following:\n"+createMultilineStringOfLocalImages(matchedByDateTaken))

				None
			}
		}

	}

	def lookup(image: SMImage, metadata: SMImageMetadata): Option[XMP] = {

		localImages.get(image.FileName.toLowerCase()) match {
			case Some(localImages) => lookup(localImages, image, metadata)
			case None => {
				logger.warn(s"$image not found locally")

				None
			}
		}
	}
}

object SMXmpLookup {

	val logger = LoggerFactory.getLogger(this.getClass);

	def equal(xmp: XMP, image: SMImage): Boolean = {
		val equal = equalStrOptions(xmp.description, image.Caption) && equalStrOptions(xmp.title, image.Title)
		if (equal) {
			logger.debug(s"$xmp == $image")
		} else {
			logger.debug(s"$xmp != $image")
		}
		equal
	}

	// TODO: this is ugly... parsing should handle empty strings
	def equalStrOptions(s1: Option[String], s2: Option[String]): Boolean = {

		val _s1 = s1 match {
			case Some(x) if x.isEmpty() => None
			case other => other
		}

		val _s2 = s2 match {
			case Some(x) if x.isEmpty() => None
			case other => other
		}

		_s1 == _s2
	}
}