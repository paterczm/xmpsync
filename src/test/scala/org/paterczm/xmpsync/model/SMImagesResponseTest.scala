package org.paterczm.xmpsync.model

import java.io.FileInputStream
import scala.io.Source
import org.junit.Before
import org.junit.Test
import org.junit.Assert
import play.api.libs.json.Json

class SMImagesResponseTest {

	val responsePath = "./src/test/resources/smugmug/searchImagesResponse.json"

	var responseText: String = _

	@Before
	def setUp() {
		val stream = new FileInputStream(responsePath)
    responseText = Source.fromInputStream(stream).mkString
    stream.close()
	}

	@Test
	def parseTest() {

		val json = Json.parse(responseText)

		val response = SMImagesResponse(json)

		Assert.assertEquals(5, response.Image.size)

	}

}