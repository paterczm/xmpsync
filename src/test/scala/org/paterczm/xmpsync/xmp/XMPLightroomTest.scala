package org.paterczm.xmpsync.xmp

import java.io.FileInputStream
import org.junit.Test
import scala.io.Source
import org.junit._
import org.junit.Assert._

class XMPLightroomTest {

  val resources = "./src/test/resources"

  var xmp: XMP = null

  setUp

  def setUp {
    val xmpStream = new FileInputStream(resources + "/rdf/lightroom.rdf")
    val xmpString = Source.fromInputStream(xmpStream).mkString
    xmpStream.close()

    xmp = XMP(xmpString)
  }

  @Test
  def testRating {
    assertEquals(Some(1), xmp.rating)
  }

  @Test
  def testTags {
    val tags = xmp.tags
    assertTrue(tags.contains("Birds"))
    assertTrue(tags.contains("Park"))
  }

  @Test
  def testTitle {
    assertEquals(Some("This is a title"), xmp.title)
  }

  @Test
  def testDescription {
    assertEquals(Some("Birds of Riverwoods"), xmp.description)
  }

}

class XMPLightroom2Test {

  val resources = "./src/test/resources"

  var xmp: XMP = null

  setUp

  def setUp {
    val xmpStream = new FileInputStream(resources + "/rdf/lightroom2.rdf")
    val xmpString = Source.fromInputStream(xmpStream).mkString
    xmpStream.close()

    xmp = XMP(xmpString)
  }

  @Test
  def testRating {
    assertEquals(Some(4), xmp.rating)
  }

}