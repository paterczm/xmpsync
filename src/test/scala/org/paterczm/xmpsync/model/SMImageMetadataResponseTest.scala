package org.paterczm.xmpsync.model

import java.io.FileInputStream
import scala.io.Source
import org.junit.Before
import org.junit.Test
import org.junit.Assert
import play.api.libs.json.Json

class SMImageMetadataResponseTest {

	val responsePath = "./src/test/resources/smugmug/imageMetadataResponse.json"

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

		val response = SMImageMetadataResponse(json)

		Assert.assertEquals("2022-05-08T13:35:55-04:00", response.ImageMetadata.DateDigitized)

	}

}