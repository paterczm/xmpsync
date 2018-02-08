package org.paterczm.xmpsync

import org.junit.Test
import SMXmpLookup._
import org.junit.Assert
import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

import org.paterczm.xmpsync.model.LocalImage
import java.io.File
import org.paterczm.xmpsync.model.SMImage
import org.paterczm.xmpsync.xmp.XMP
import org.paterczm.xmpsync.model.SMImageMetadata
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RunWith(classOf[JUnitRunner])
class SMXmpLookupTest extends FlatSpec with Matchers {

	"equalStrOptions" should "handle Option[String] equality correctly" in {
		equalStrOptions(None, None) should be(true)
		equalStrOptions(Some(""), Some("")) should be(true)
		equalStrOptions(None, Some("")) should be(true)
		equalStrOptions(Some(""), None) should be(true)
		equalStrOptions(Some("a"), None) should be(false)
	}

	implicit class LocalDateTimeConversion(date: String) {
		def asDate() = LocalDateTime.parse(date, DateTimeFormatter.ISO_DATE_TIME)
		def asSomeDate() = Some(asDate())
	}

	class MockFile(size: Long, path: String = "") extends File(path) {
		override def length(): Long = size
	}

	class MockXMP(rating: Option[Int] = None, dateTimeOriginal: Option[LocalDateTime]) extends XMP(title = None, description = None, rating = rating, dateTimeOriginal = dateTimeOriginal)

	class MockImage(fileName: String) extends SMImage(Title = None, Caption = None, WebUri = null, KeywordArray = null, FileName = fileName, ImageKey = null, OriginalSize = 13)

	val localImages: Map[String, List[LocalImage]] = Map(
		"dfc01" -> List(
			LocalImage(new MockFile(2), () => new MockXMP(dateTimeOriginal = "2018-01-01T09:12:01".asSomeDate(), rating = Some(1))),
			LocalImage(new MockFile(3), () => new MockXMP(dateTimeOriginal = "2018-01-01T09:12:01".asSomeDate(), rating = Some(2))),
			LocalImage(new MockFile(2), () => new MockXMP(dateTimeOriginal = "2018-02-02T09:12:01".asSomeDate(), rating = Some(3)))),
		"dfc02" -> List(
			LocalImage(new MockFile(2), () => new MockXMP(dateTimeOriginal = "2018-01-01T09:12:01".asSomeDate(), rating = Some(2))),
			LocalImage(new MockFile(2), () => new MockXMP(dateTimeOriginal = "2018-01-01T09:12:01".asSomeDate(), rating = Some(2))),
			LocalImage(new MockFile(2), () => new MockXMP(dateTimeOriginal = "2017-01-01T09:12:01".asSomeDate(), rating = Some(2)))))

	val smXmpLookup = new SMXmpLookup(localImages)

	"SMXmpLookup" should "return None when image not found locally by file name" in {
		smXmpLookup.lookup(new MockImage("DFC00"), SMImageMetadata("2018-01-01T09:12:01")) should be(None)
	}

	it should "return None when image found locally by file name but date taken does not match" in {
		smXmpLookup.lookup(new MockImage("DFC01"), SMImageMetadata("2017-01-01T09:12:01")) should be(None)
	}

	it should "return None when there are 2 images matching by filename and date taken" in {
		smXmpLookup.lookup(new MockImage("DFC01"), SMImageMetadata("2018-01-01T09:12:01")) should be(None)
	}

	it should "return matching image when only one exists (for given file name and date taken)" in {
		smXmpLookup.lookup(new MockImage("DFC01"), SMImageMetadata("2018-02-02T09:12:01")) should be(Some(localImages("dfc01")(2).xmp))
	}

	it should "return first matching image when 2 images are matching but have the same rating and tags" in {
		smXmpLookup.lookup(new MockImage("DFC01"), SMImageMetadata("2018-02-02T09:12:01")) should be(Some(localImages("dfc01")(2).xmp))
	}

}