package org.paterczm.xmpsync.xmp

import java.io._
import dk.pkg.xmputil4j.core.JpegXmpInputStream
import scala.io.Source
import scala.collection.JavaConversions._
import scala.xml._
import scala.collection.mutable.ListBuffer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.nio.charset.CharsetDecoder
import java.nio.charset.CodingErrorAction
import java.nio.charset.Charset
import scala.io.Codec

case class XMP(title: Option[String], description: Option[String], rating: Option[Int], tags: Set[String] = Set(), dateTimeOriginal: Option[LocalDateTime]) {

	// Auxiliary Constructors
	def this(that: XMP) = {
		this(that.title, that.description, that.rating, that.tags, that.dateTimeOriginal)
	}

	def this() = {
		this(None, None, None, Set(), None)
	}

}

// companion object
object XMP {

	// 2012-05-05T11:15:04Z
	val dateFormatter = DateTimeFormatter.ISO_DATE_TIME

	val codec = Codec("UTF-8")
	codec.onMalformedInput(CodingErrorAction.REPLACE)
	codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

	private def extractZonerStyleRating(xml: Elem): Option[Int] = {
		try {
			Some((xml \\ "Rating").text.toInt)
		} catch {
			case e: java.lang.NumberFormatException => None
		}
	}

	private def extractAdobeStyleRating(xml: Elem): Option[Int] = {
		for (node <- (xml \\ "Description")) {
			try {
				return Some((node \ "@{http://ns.adobe.com/xap/1.0/}Rating").text.toInt)
			} catch {
				case e: java.lang.NumberFormatException => {}
			}
		}

		None
	}

	private def extractAdobeStyleDateTaken(xml: Elem): Option[LocalDateTime] = {
		for (node <- (xml \\ "Description")) {
			val text = (node \ "@{http://ns.adobe.com/xap/1.0/}CreateDate").text

			if (text.isEmpty()) {
				return None
			}

			return Some(LocalDateTime.parse(trimBelowSecond(text), dateFormatter))
		}

		None
	}

	private def trimBelowSecond(time: String) =
		if (time.lastIndexOf('.') > -1)
		{
			time.substring(0, time.lastIndexOf('.'))
		} else {
			time
		}

	def apply(xmpString: String): XMP = {

		if (xmpString == null || xmpString.isEmpty) {
			return new XMP()
		}

		// parse rdf as xml
		val xml = XML.loadString(xmpString)

        // extract rating
        val rating = extractAdobeStyleRating(xml) match {
			case Some(r) => Some(r)
			case None => extractZonerStyleRating(xml)
		}

		// extract tags
		val tags = (xml \\ "subject" \ "Bag" \ "li") map (tag => tag.text.trim) filter (!_.isEmpty()) toSet

		// extract title
		val title = optionize((xml \\ "title").text.trim)

		// extract description
		val description = optionize((xml \\ "description").text.trim)

		// extract dateTimeOriginal
		val dateTimeOriginal = optionize((xml \\ "DateTimeOriginal").text.trim) match {
			case Some(str) => Some(LocalDateTime.parse(str, dateFormatter))
			case None => extractAdobeStyleDateTaken(xml)
		}

		new XMP(title, description, rating, tags, dateTimeOriginal)
	}

	def apply(xmpFile: File): XMP = {
		XMP(extractXMPHeader(xmpFile))
	}

	def extractXMPHeader(xmpFile: File): String = {
		// TODO: nice try-catch?
		val xmpStream = new JpegXmpInputStream(new FileInputStream(xmpFile), 1024*1024)
		val xmpString = Source.fromInputStream(xmpStream)(codec).mkString
		xmpStream.close()

		xmpString
	}

	private def optionize(str: String): Option[String] = str match {
	    case (x:String) => if (x.isEmpty()) None else Some(x)
	    case _ => None
	}

}