package org.paterczm.xmpsync.xmp

import org.junit._
import org.junit.Assert._
import java.io._
import dk.pkg.xmputil4j.core.JpegXmpInputStream
import scala.io.Source

import java.time.LocalDateTime

class XMPTest {

  val resources = "./src/test/resources"
  val imagePath = resources+"/P1070016.JPG"

  var xmp:XMP = XMP(new File(imagePath))

  @Before
  def setUp {

  }

  @Test
  def extractXMPTest {
    val xmpStream = new JpegXmpInputStream(new FileInputStream(imagePath))
    val xmpString = Source.fromInputStream(xmpStream).mkString
    xmpStream.close()

    println(xmpString)

    assertTrue(xmpString != null)
    assertTrue(xmpString.length > 0)
  }

  @Test
  def testRating {
    Assert.assertEquals(Some(4), xmp.rating)
  }

  @Test
  def testTags {
    assertEquals(xmp.tags, Set("tag1","tag2","tag3","tag4","tag5"))
  }

  @Test
  def testTitle {
    assertEquals(Some("Caroline duck couple"), xmp.title)
  }

  @Test
  def testDescription {
    assertEquals(Some("Description. ĄĘŚĆÓŁŹĆąęśćółżźć."), xmp.description)
  }

  @Test
  def parser {
	println("parsed="+LocalDateTime.parse("2018-01-01T09:12:01", XMP.dateFormatter))
  }

  @Test
  def testDateTaken {
    assertEquals(Some(LocalDateTime.parse("2012-05-05T11:15:04", XMP.dateFormatter)), xmp.dateTimeOriginal)
  }

  @Test
  def testMatch {
      var str:String = "foo"

      println(str match {
          case (x:String) => x
          case _ => "_"
      })

      str = null

      println(str match {
          case (x:String) => x
          case _ => "_"
      })


  }

}
