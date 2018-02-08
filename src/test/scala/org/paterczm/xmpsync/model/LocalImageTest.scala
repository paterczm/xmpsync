package org.paterczm.xmpsync.model

import org.junit.Test
import java.io.File
import org.paterczm.xmpsync.xmp.XMP
import org.junit.Assert
import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.junit.Before

@RunWith(classOf[JUnitRunner])
class LocalImageTest extends FlatSpec with Matchers {

	class MockXMP() extends XMP(title = None, description = None, rating = None, dateTimeOriginal = None)

	class Counter(var i: Int) {
	  def ++ = { i+=1 }
	}

	def counted(implicit counter: Counter): XMP = { counter++; new MockXMP() }

	"LocalImage.xmp" should "return XMP" in {
		implicit val counter = new Counter(0)
		val li = LocalImage(new File(""), () => counted)

		li.xmp should be (new MockXMP())
	}

	"LocalImage.xmp" should "initialize XMP only once" in {
		implicit val counter = new Counter(0)
		val li = LocalImage(new File(""), () => counted)

		counter.i should be (0)

		li.xmp
		li.xmp
		li.xmp

		counter.i should be (1)
	}

}