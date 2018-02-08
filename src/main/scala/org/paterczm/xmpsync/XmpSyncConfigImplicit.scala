package org.paterczm.xmpsync

import com.typesafe.config.ConfigValueFactory
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import configs.Result
import configs.syntax._
import org.paterczm.xmpsync.SMAuthenticator.SMAuthentication
import java.io.PrintWriter
import java.io.File
import com.typesafe.config.ConfigRenderOptions

object XmpSyncConfigImplicit {

	/**
	 * Config enrichment.
	 *
	 */
	implicit class XmpSyncConfig(val underlying: Config) {

		import collection.JavaConversions._

		// convert case class to a map
		private def ccToMap(cc: AnyRef) =
			(Map[String, Any]() /: cc.getClass.getDeclaredFields) {
				(a, f) =>
					f.setAccessible(true)
					a + (f.getName -> f.get(cc))
			}

		def smApi = underlying.get[SMApi]("SM.SMApi")
		def smApi(smApi: SMApi) = underlying.withValue("SM.SMApi", ConfigValueFactory.fromMap(ccToMap(smApi)))

		def SM(): Config = {
			ConfigFactory.empty().withValue("SM", ConfigValueFactory.fromAnyRef(underlying.getAnyRef("SM")))
		}
		def smAuthenticaton = underlying.get[SMAuthentication]("SM.SMAuthentication")
		def smAuthenticaton(auth: SMAuthentication): Config = {
			underlying.withValue("SM.SMAuthentication", ConfigValueFactory.fromMap(ccToMap(auth), "SmugMug OAuth"))
		}
		def smUsername = underlying.get[String]("SM.SMUsername")
		def smUsername(username: String): Config = {
			underlying.withValue("SM.SMUsername", ConfigValueFactory.fromAnyRef(username))
		}
		def albumSMAlbumKey: Result[String] = {
			underlying.get[String]("Album.SM.AlbumKey")
		}
		def folderSMFolderKey: Result[String] = {
			underlying.get[String]("Folder.SM.FolderKey")
		}
		
		def albumConfFileName = "album.conf"
		def folderConfFileName = "folder.conf"

		def save(as: File) {
			val writer = new PrintWriter(as)
			writer.write(underlying.root().render(ConfigRenderOptions.defaults().setOriginComments(false)))
			writer.close()
		}
		
	}
}