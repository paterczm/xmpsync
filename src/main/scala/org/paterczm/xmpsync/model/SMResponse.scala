package org.paterczm.xmpsync.model

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class SMPages(Total: Int)

object SMPages {

	// single field case class handling is crazy here...
	// solution taken from https://stackoverflow.com/questions/14754092/how-to-turn-json-to-case-class-when-case-class-has-only-one-field
	implicit val pagesReader: Reads[SMPages] = (__ \ 'Total).read[Int].map{ t => SMPages(t) } // covariant map

	def unapply(jsonResponse: JsDefined): Option[SMPages] = {
		(jsonResponse \ "Pages").asOpt[SMPages]
	}
}

trait SMPageable {
	def Pages: SMPages
}

case class SMImagesResponse(override val Pages: SMPages, Image: List[SMImage]) extends SMPageable

object SMImagesResponse {

	implicit val responseReader: Reads[SMImagesResponse] = (
			(JsPath \ "Pages").read[SMPages] and
			(JsPath \ "Image").read[List[SMImage]])(SMImagesResponse.apply(_, _))
			// pages and images are using their own implicit readers

	def apply(jsonResponse: JsValue): SMImagesResponse = {
		(jsonResponse \ "Response").validate[SMImagesResponse] match {
			case s: JsSuccess[SMImagesResponse] => s.get
			case e: JsError => throw new Exception(JsError.toJson(e).toString())
		}
	}
}

case class SMImageMetadata(DateDigitized: String)

object SMImageMetadata {

	implicit val metadataReader: Reads[SMImageMetadata] = (__ \ 'DateDigitized).read[String].map{ t => SMImageMetadata(t) } // covariant map

	def apply(jsonResponse: JsValue): SMImageMetadata = {
		(jsonResponse \ "ImageMetadata").validate[SMImageMetadata] match {
			case s: JsSuccess[SMImageMetadata] => s.get
			case e: JsError => throw new Exception(JsError.toJson(e).toString())
		}
	}
}

case class SMImageMetadataResponse(ImageMetadata: SMImageMetadata)

object SMImageMetadataResponse {

	implicit val imageMetadataReader: Reads[SMImageMetadataResponse] = (__ \ 'ImageMetadata).read[SMImageMetadata].map{ t => SMImageMetadataResponse(t) } // covariant map

	def apply(jsonResponse: JsValue): SMImageMetadataResponse = {
		(jsonResponse \ "Response").validate[SMImageMetadataResponse] match {
			case s: JsSuccess[SMImageMetadataResponse] => s.get
			case e: JsError => throw new Exception(JsError.toJson(e).toString())
		}
	}
}