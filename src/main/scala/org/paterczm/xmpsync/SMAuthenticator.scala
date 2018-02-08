
package org.paterczm.xmpsync

import scala.concurrent.ExecutionContext
import com.hunorkovacs.koauthsync.service.consumer.DefaultConsumerService
import scala.io.StdIn
import com.hunorkovacs.koauth.domain.KoauthRequest
import java.net.URL
import com.netaporter.uri.Uri.parseQuery
import org.slf4j.LoggerFactory
import scalaj.http.Http

object SMAuthenticator {

	case class SMAuthentication(oauth_token: String, oauth_token_secret: String)

	val logger = LoggerFactory.getLogger(SMAuthenticator.getClass);

	val requestTokenUrl = "https://api.smugmug.com/services/oauth/1.0a/getRequestToken"
	val authorizationUrl = "https://api.smugmug.com/services/oauth/1.0a/authorize"
	val accessTokenUrl = "https://api.smugmug.com/services/oauth/1.0a/getAccessToken"

	def apply(username: String, api: SMApi): SMAuthentication = {
		val consumer = new DefaultConsumerService(ExecutionContext.global)

		val requestWithInfo = consumer.createRequestTokenRequest(
			KoauthRequest("POST", requestTokenUrl, None), api.key, api.secret, "oob")

		logger.debug(s"request token: Authorization: $requestWithInfo.header")

		val resp = Http(requestTokenUrl).method("POST").headers(Map("Authorization" -> requestWithInfo.header)).asString

		// TODO: http error handling

		logger.debug(s"Got response status=$resp.status, body=$resp.body")

		val queryString = parseQuery(resp.body)

		val oauth_token = queryString.param("oauth_token") match {
			case Some(str) => str
			case None => throw new Exception("No oauth_token found")
		}
		val oauth_token_secret = queryString.param("oauth_token_secret") match {
			case Some(str) => str
			case None => throw new Exception("No oauth_token_secret found")
		}

		logger.debug(s"Temporary oauth_token: $oauth_token")
		logger.debug(s"Temporary oauth_token_secret: $oauth_token_secret")

		// TODO: improve this desc?
		println(s"Go to $authorizationUrl?oauth_token=$oauth_token&Access=Full&Permissions=Modify&username=$username to obtain verifier code. Paste it here and press enter.")
		val verifierCode = StdIn.readLine()

		val accessTokenRequestInfo = consumer.createAccessTokenRequest(KoauthRequest("GET", accessTokenUrl, None), api.key, api.secret, oauth_token, oauth_token_secret, verifierCode)

		logger.debug(s"request access token: Authorization: accessTokenRequestInfo.header")

		val resp2 = Http(accessTokenUrl).headers(Map("Authorization" -> accessTokenRequestInfo.header)).asString

		val queryString2 = parseQuery(resp2.body)
		val verified_oauth_token = queryString2.param("oauth_token") match {
			case Some(str) => str
			case None => throw new Exception("No oauth_token found")
		}
		val verified_oauth_token_secret = queryString2.param("oauth_token_secret") match {
			case Some(str) => str
			case None => throw new Exception("No oauth_token_secret found")
		}

		logger.trace(s"oauth_token: $verified_oauth_token")
		logger.trace(s"oauth_token_secret: $verified_oauth_token_secret")

		SMAuthentication(verified_oauth_token, verified_oauth_token_secret)
	}
}