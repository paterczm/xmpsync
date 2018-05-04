package org.paterczm.xmpsync.model

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class SMRating(rating: Int) {
	def asTags: Set[String] = rating match {
		case 0 => Set()
		case 1 => Set("rating1")
		case 2 => Set("ratingAtLeast2", "rating2")
		case 3 => Set("ratingAtLeast3", "ratingAtLeast2", "rating3")
		case 4 => Set("ratingAtLeast4", "ratingAtLeast3", "ratingAtLeast2", "rating4")
		case 5 => Set("ratingAtLeast5", "ratingAtLeast4", "ratingAtLeast3", "ratingAtLeast2", "rating5")
	}
}

case class SMImage(Title: Option[String], Caption: Option[String], WebUri: String, KeywordArray: List[String], FileName: String, ImageKey: String) {

	def trueKeywords = KeywordArray.filter(!_.startsWith("rating")).toSet

	def rate(rating: SMRating): SMImage = {
		val ratedTags = trueKeywords ++ rating.asTags

		SMImage(Title, Caption, WebUri, ratedTags.toList.sorted, FileName, ImageKey)
	}

	def keywords(tags: Set[String]) = SMImage(Title, Caption, WebUri, tags.toList.sorted, FileName, ImageKey)

	def keywords = KeywordArray.toSet

}

object SMImage {
	val resultName = "Image"

	implicit val imageReader: Reads[SMImage] = (
		(JsPath \ "Title").readNullable[String] and
		(JsPath \ "Caption").readNullable[String] and
		(JsPath \ "WebUri").read[String] and
		(JsPath \ "KeywordArray").read[List[String]] and
		(JsPath \ "FileName").read[String] and
		(JsPath \ "ImageKey").read[String])(SMImage.apply _)

	def unapply(jsonResponse: JsDefined): Option[List[SMImage]] = {
		(jsonResponse \ resultName).asOpt[List[SMImage]]
	}

}