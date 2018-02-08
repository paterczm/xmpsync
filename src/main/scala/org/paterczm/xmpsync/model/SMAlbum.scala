package org.paterczm.xmpsync.model

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class SMAlbum(Title: String, WebUri: String, AlbumKey: String)

object SMAlbumExtractor {
	val resultName = "Album"

	implicit val albumReader: Reads[SMAlbum] = (
		(JsPath \ "Title").read[String] and
		(JsPath \ "WebUri").read[String] and
		(JsPath \ "AlbumKey").read[String])(SMAlbum.apply _)

	def unapply(jsonResponse: JsDefined): Option[SMAlbum] = {

		(jsonResponse \ resultName).asOpt[SMAlbum]
	}
}

object SMAlbumsExtractor {
	val resultName = "Album"

	def unapply(jsonResponse: JsDefined): Option[List[SMAlbum]] = {

		implicit val albumReader = SMAlbumExtractor.albumReader

		(jsonResponse \ resultName).asOpt[List[SMAlbum]]
	}
}