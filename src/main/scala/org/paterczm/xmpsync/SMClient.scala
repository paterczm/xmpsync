
package org.paterczm.xmpsync

import java.net.URLEncoder

import scala.concurrent.ExecutionContext

import org.slf4j.LoggerFactory

import com.hunorkovacs.koauth.domain.KoauthRequest
import com.hunorkovacs.koauthsync.service.consumer.DefaultConsumerService

import play.api.libs.json._
import scalaj.http.Http
import org.paterczm.xmpsync.SMAuthenticator.SMAuthentication
import org.paterczm.xmpsync.model.SMAlbum
import org.paterczm.xmpsync.model.SMAlbumExtractor
import org.paterczm.xmpsync.model.SMAlbumsExtractor
import org.paterczm.xmpsync.model.SMImage
import org.paterczm.xmpsync.model.SMImagesResponse
import org.paterczm.xmpsync.SMClient._
import org.paterczm.xmpsync.model.SMImageMetadataResponse

object SMClient {

	case class SMScope(uri: String)

	object SMScope {
		def user(username: String): SMScope = SMScope(s"""/api/v2/user/$username""")
		def album(albumKey: String): SMScope = SMScope(s"""/api/v2/album/$albumKey""")
		def folder(folderKey: String): SMScope = SMScope(s"""/api/v2/folder/id/$folderKey""")
	}

}

/**
 *
 * @author mpatercz
 */
class SMClient(val auth: SMAuthentication, val api: SMApi) {

	val logger = LoggerFactory.getLogger(this.getClass);

	val pretty = 1

	val apiUrl = "https://api.smugmug.com"

	val consumer = new DefaultConsumerService(ExecutionContext.global)

	private def call(uri: String, method: String, body: Option[String]): String = {
		val url = s"""$apiUrl$uri"""

		val req = body match {
			case Some(b) => Http(url).headers(createHeadersMap(url, method)).postData(b).method(method)
			case None => Http(url).headers(createHeadersMap(url, method)).method(method)
		}

		logger.debug(s"""req=${req}""")

		val resp = req.asString

		logger.debug(s"""resp=${resp}""")

		if (!resp.is2xx && !resp.is3xx) {
			throw new Exception("HTTP Status=" + resp.statusLine)
		}

		resp.body
	}

	private def get(uri: String) = call(uri, "GET", None)

	private def patch(uri: String, body: String) = call(uri, "PATCH", Some(body))

	private def createHeadersMap(url: String, method: String): Map[String, String] = {
		val authenticatedReqestInfo = consumer.createOauthenticatedRequest(KoauthRequest(method, url, None), api.key, api.secret, auth.oauth_token, auth.oauth_token_secret)

		Map("Authorization" -> authenticatedReqestInfo.header, "Accept" -> "application/json", "Content-Type" -> "application/json")
	}

	def searchAlbums(text: String, scope: SMScope) {

		val uri = s"/api/v2/album!search?Scope=${scope.uri}&SortDirection=Descending&SortMethod=LastUpdated&Text=${URLEncoder.encode(text, "UTF-8")}"

		Json.parse(get(uri)) \ "Response" match {
			case SMAlbumsExtractor(albums) => {
				albums.foreach(println _)
			}
		}

	}

	def findAlbum(albumId: String): Option[SMAlbum] = {
		val uri = s"/api/v2/album/$albumId"

		Json.parse(get(uri)) \ "Response" match {
			case SMAlbumExtractor(album) => Some(album)
			case _ => None
		}

	}

	def searchImages(scope: SMScope, start: Int = 1, pageSize: Int = 10): SMImagesResponse = {
		val uri = s"""/api/v2/image!search?Scope=${scope.uri}&SortDirection=Descending&SortMethod=DateTaken&_verbosity=0&_pretty=${pretty}&start=${start}&count=${pageSize}&_filter=Title,Caption,WebUri,KeywordArray,FileName,ImageKey,OriginalSize&_filteruri="""

		SMImagesResponse(Json.parse(get(uri)))
	}

	def getMetadata(image: SMImage): SMImageMetadataResponse = {
		val uri = s"""/api/v2/image/${image.ImageKey}!metadata"""

		SMImageMetadataResponse(Json.parse(get(uri)))
	}

	def updateImageKeywords(image: SMImage) {
		val uri = s"""/api/v2/image/${image.ImageKey}"""

		patch(uri, s"""{"Keywords": "${image.KeywordArray.mkString(",")}"}""")
	}

}