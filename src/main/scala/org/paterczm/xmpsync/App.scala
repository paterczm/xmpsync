package org.paterczm.xmpsync

import java.io.File
import java.io.PrintWriter

import scala.io.StdIn

import org.slf4j.LoggerFactory

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigValueFactory

import org.paterczm.xmpsync.SMClient.SMScope
import org.paterczm.xmpsync.XmpSyncConfigImplicit._
import org.paterczm.xmpsync.xmp.RecursiveJPGProcessor
import org.paterczm.xmpsync.model.SMRating
import _root_.akka.actor._
import scala.concurrent.Await
import scala.concurrent.duration._
import org.paterczm.xmpsync.akka.Master
import org.paterczm.xmpsync.akka.Start

object App extends App {

	AppCli.parser.parse(args, new AppCli.Cli()) match {
		case Some(config) => {

			val HOME = config.home match {
				case None => System.getProperty("user.home") + "/.config/xmpsync/"
				case Some(path) => path
			}

			if (new File(HOME).exists() || new File(HOME).mkdirs()) {

				val CONF_FILE = HOME + "xmpsync.conf"

				if (config.debug) {
					System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG")
				}
				val logger = LoggerFactory.getLogger(App.getClass);
				logger.debug(s"Loading configs from $CONF_FILE")

				var conf = ConfigFactory.parseFile(new File(CONF_FILE))

				// set key and secret
				if (config.action == "config") {
					logger.debug(s"Saving configs back to $CONF_FILE")
					val writer = new PrintWriter(new File(CONF_FILE))
					writer.write(conf.smApi(SMApi(config.smKey, config.smSecret)).root().render(ConfigRenderOptions.defaults().setOriginComments(false)))
					writer.close()
					logger.debug("exit(0)")
					System.exit(0)
				}

				try {

					val api = conf.smApi.value

					// TODO: move this to config
					val username = conf.smUsername.valueOrElse({
						println("What is your SmugMug username?")
						val username = StdIn.readLine()
						conf = conf.smUsername(username)
						username
					})

					val auth = conf.smAuthenticaton.valueOrElse({
						logger.debug("Authenticating with SmugMug");
						val auth = SMAuthenticator(username, api)
						conf = conf.smAuthenticaton(auth)
						auth
					})

					val smClient = new SMClient(auth, api)

					config.action match {
						case null => ;
						case "searchAlbums" => {
							smClient.searchAlbums(config.query, SMScope.user(conf.smUsername.value))
						}
						case "linkAlbum" => {

							smClient.findAlbum(config.id) match {
								case Some(album) => {

									ConfigFactory.parseFile(new File(conf.albumConfFileName))
										.withValue("Album.SM.AlbumKey", ConfigValueFactory.fromAnyRef(config.id))
										.withValue("Album.Title", ConfigValueFactory.fromAnyRef(album.Title))
										.withValue("Album.Url", ConfigValueFactory.fromAnyRef(album.WebUri))
										.save(new File(conf.albumConfFileName))
								}
								case _ => throw new Exception(s"""Could not find albumId=${config.id}""")
							}
						}
						case "linkFolder" => {

							// TODO: fetch folder
							ConfigFactory.parseFile(new File(conf.folderConfFileName))
								.withValue("Folder.SM.FolderKey", ConfigValueFactory.fromAnyRef(config.id))
								.save(new File("folder.conf"))

						}
						case "rate" => {

							val imageSearchScope = new File(conf.albumConfFileName).exists() match {
								case true => SMScope.album(ConfigFactory.parseFile(new File(conf.albumConfFileName)).albumSMAlbumKey.value)
								case false => SMScope.folder(ConfigFactory.parseFile(new File(conf.folderConfFileName)).folderSMFolderKey.value)
							}

							logger.debug(s"""Rating $imageSearchScope""")

							// process JPGs
							val lazyXMPsMap = RecursiveJPGProcessor(new File("."))

							val processor = new SMXmpSyncProcessor(new SMXmpLookup(lazyXMPsMap), smClient, imageSearchScope, config.dryRun)

							val system = ActorSystem("xmpsync")

							val coreCount = config.threads match {
								case None => Runtime.getRuntime.availableProcessors()
								case Some(t) => t
							}
							logger.info(s"Using $coreCount threads and ${config.pageSize} page size")
							val master = system.actorOf(Master.props(processor, coreCount, config.pageSize), "master")

							master ! Start

							Await.ready(system.whenTerminated, Duration(5, MINUTES))

						}
					}

				} finally {
					logger.debug(s"Saving configs back to $CONF_FILE")

					val writer = new PrintWriter(new File(CONF_FILE))
					writer.write(conf.SM.root().render(ConfigRenderOptions.defaults().setOriginComments(false)))
					writer.close()
				}
			} else {
				throw new Exception(s"""Cannot create $HOME!""")
			}
		}

		case None => {}
	}
}
