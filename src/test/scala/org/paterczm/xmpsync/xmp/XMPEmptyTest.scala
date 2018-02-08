package org.paterczm.xmpsync.xmp

import java.io.FileInputStream
import org.junit._
import org.junit.Assert._

import scala.io.Source
import java.io.File

class XMPEmptyTest {
  val resources = "./src/test/resources"

  var xmp:XMP = null

  @Before
  def setUp {
    val xmpStream = new FileInputStream(resources+"/empty.rdf")
    val xmpString = Source.fromInputStream(xmpStream).mkString
    xmpStream.close()

    xmp = XMP(xmpString)
  }

  @Test
  def testRating {
    Assert.assertEquals(None, xmp.rating)
  }

  @Test
  def testTags {
    assertTrue(xmp.tags.isEmpty)
  }

  @Test
  def testTitle {
    assertEquals(None, xmp.title)
  }

  @Test
  def testDescription {
    assertEquals(None, xmp.description)
  }
}

class XMPEmptyFromImgTest extends XMPEmptyTest {

  override def setUp {
    xmp = XMP(new File(resources+"/empty.jpg"))
  }

}