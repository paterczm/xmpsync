package org.paterczm.xmpsync

object AppCli {

	case class Cli(smKey: String, smSecret: String, action: String, query: String, id: String, folderPath: String, dryRun: Boolean = false, home: Option[String] = None, debug: Boolean = false, threads: Option[Int] = None, pageSize: Int = 50) {
		def this() = this(null, null, null, null, null, null)
	}

	// -DFolder.SM.FolderKey=foo should work as override
	val parser = new scopt.OptionParser[Cli]("xmpsync") {
		head("xmpsync", "2.0")

		opt[Unit]('d', "dry-run").action((_, config) =>
			config.copy(dryRun = true)).text("Do not make any changes")

		opt[String]("home").action((str, config) =>
			config.copy(home = Some(str))).text("Home directory")

		opt[Unit]("debug").action((_, config) =>
			config.copy(debug = true)).text("Turn on debug logging")

		help("help").text("Prints usage")

		version("version").text("Version")

		cmd("config").action((_, config) => config.copy(action = "config"))
			.text("Configure smugmug client")
			.children(
				opt[String]("smKey").required().valueName("<SmugMug api key>")
					.action((str, config) =>
						config.copy(smKey = str)),
				opt[String]("smSecret").required().valueName("<SmugMug api secret>")
					.action((str, config) =>
						config.copy(smSecret = str)))

		cmd("searchAlbums").action((_, config) => config.copy(action = "searchAlbums"))
			.text("Album text search.")
			.children(
				opt[String]('q', "query").required().valueName("<query>")
					.action((str, config) =>
						config.copy(query = str)))

		cmd("linkAlbum").action((_, config) => config.copy(action = "linkAlbum"))
			.text("Link current directory to specified album.")
			.children(
				opt[String]('i', "id").required().valueName("<album id>")
					.action((str, config) =>
						config.copy(id = str)))

		cmd("linkFolder").action((_, config) => config.copy(action = "linkFolder"))
			.text("Link current directory to specified folder.")
			.children(
				opt[String]('i', "id").required().valueName("<folder id>")
					.action((str, config) =>
						config.copy(id = str)))

		cmd("rate").action((_, config) => config.copy(action = "rate"))
			.text("Rate images in a linked album")
			.children(
				opt[Int]('t', "threads").optional().valueName("<number of threads>")
					.action((t, config) => config.copy(threads = Some(t))),

				opt[Int]('p', "pageSize").optional().valueName("<page size>")
					.action((p, config) => config.copy(pageSize = p)),

				opt[String]('f', "folderPath").optional().valueName("<folder path>")
					.action((f, config) => config.copy(folderPath = f)))

		// TODO: can't I just make command required?
		checkConfig { conf => conf match {
			case Cli(_,_,null, _, _, _, _, _, _, _, _) => failure("Command required")
			case _ => success
		}}

	}

}