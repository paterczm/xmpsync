package org.paterczm.xmpsync.model

import org.junit._
import org.junit.Assert._

class SMImageTest {

	val image = SMImage(Some("title"), Some("caption"), "uri", List("foo", "bar", "rating2", "ratingAtLeast2"), "file", "key")

	@Test
	def ratingTest {
		Assert.assertEquals(List("bar","foo","rating3","ratingAtLeast2","ratingAtLeast3"), image.rate(SMRating(3)).KeywordArray)
	}

	@Test
	def removeRatingTest {
		Assert.assertEquals(List("bar","foo"), image.rate(SMRating(0)).KeywordArray)
	}

}