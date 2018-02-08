package org.paterczm.xmpsync.xmp

import java.io.File
import org.junit.Test
import java.io.FileInputStream
import dk.pkg.xmputil4j.core.JpegXmpInputStream
import scala.io.Source

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import java.time.LocalDateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class XMPPixelTest extends FlatSpec with Matchers {

	val xmpString = Source.fromFile(new File("src/test/resources/rdf/pixel.rdf")).mkString

	val xmp:XMP = XMP(xmpString)

	"Time taken set by pixel" should "be rounded down to second" in {
		xmp.dateTimeOriginal should be (Some(LocalDateTime.parse("2018-01-01T10:09:25", XMP.dateFormatter)))
	}

}