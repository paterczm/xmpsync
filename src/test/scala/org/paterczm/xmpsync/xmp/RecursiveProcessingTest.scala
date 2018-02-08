package org.paterczm.xmpsync.xmp

import org.junit._
import org.junit.Assert._
import java.io._

class RecursiveProcessingTest {

	val resources = "./src/test/resources"
	val dir = resources+"/recursive"

	@Test
	def testRecursiveJPGFinder {
		val jpgs = RecursiveJPGFinder(new File(dir))

		assertEquals(6, jpgs.length)
	}

	@Test
	def testRecursiveJPGProcessor {
		val jpgMap = RecursiveJPGProcessor(new File(dir))

		assertEquals(5, jpgMap.size)
		assertTrue(jpgMap.contains("P1070016.JPG".toLowerCase()))
		assertTrue(jpgMap.contains("P1070016-2.JPG".toLowerCase()))
		assertTrue(jpgMap.contains("P1070016-3.JPG".toLowerCase()))
		assertTrue(jpgMap.contains("P1070016-4.JPG".toLowerCase()))
		assertTrue(jpgMap.contains("P1070016-5.JPG".toLowerCase()))
		assertEquals(2, jpgMap("P1070016.JPG".toLowerCase()).size)
	}

}