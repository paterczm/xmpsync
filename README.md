# xmpsync

A command line tool which syncs [XMP](https://en.wikipedia.org/wiki/Extensible_Metadata_Platform) star rating from local JPEG files with photographs in [SmugMug](https://www.smugmug.com/about) photo gallery. Lets you create [smart galleries](http://help.smugmug.com/customer/portal/articles/93308-what-are-smart-galleries-gather-photos-based-on-keywords-) with e.g. best photos from last year.

SmugMug does not support XMP star rating, so this tool maps it to tags like so:

```scala
	case 0 => Set()
	case 1 => Set("rating1")
	case 2 => Set("ratingAtLeast2", "rating2")
	case 3 => Set("ratingAtLeast3", "ratingAtLeast2", "rating3")
	case 4 => Set("ratingAtLeast4", "ratingAtLeast3", "ratingAtLeast2", "rating4")
	case 5 => Set("ratingAtLeast5", "ratingAtLeast4", "ratingAtLeast3", "ratingAtLeast2", "rating5")
```

Local JPEGs are mapped to remote SmugMug photographs based on filename and date taken.

Needs Java 8 or greater to run. Tested on Linux and Windows.

# Examples

Sync a local directory with a folder on SmugMug:

```bash
# Link local directory with SmugMug folder (ABCDEF being the folder key):
[mpatercz@localhost 2018]$ xmpsync linkFolder --id ABCDEF

# this will create a folder.conf file in 2018 directory:
[mpatercz@localhost 2018]$ cat folder.conf
{
    "Folder" : {
        "SM" : {
            "FolderKey" : "ABCDEF"
        }
    }
}

# sync xmp rating to tags in SmugMug
xmpsync rate
(...)
[xmpsync-akka.actor.default-dispatcher-7] INFO org.paterczm.xmpsync.akka.Master - Processing of 7 batches complete. 0 batches abandoned due to failures, totalRemoteImages=348, localImagesMatched=345, remoteImagesUpdated=0

# Since the folder is already linked, all you have to do to re-sync rating is to call xmpsync rate

```

You can link to albums in a similar way.

Use searchAlbums command to easly find the album key of the album you want to link:

```bash
[mpatercz@localhost 2018]$ xmpsync searchAlbums -q winter
SMAlbum(Winter something something,http://photo.marek-paterczyk.net/...,ABCDEF)
```
(searchFolders is not available yet)

For a list of all commands and features, call `xmpsync --help`.

# Download

* [version 2.0.0](http://www.marek-paterczyk.net/index.php?show=download&url=images/java/xmpsync/xmpsync-2.0.0-SNAPSHOT-jar-with-dependencies.jar)

# Setup

## Running

```
$ java -jar xmpsync-2.0.0-SNAPSHOT-jar-with-dependencies.jar
```

For convenience, add a [bash script](etc) to your classpath. You can do similar thing using a bat file in Windows, but I have no example.

## Configuration

1. First, you need to apply for [SmugMug api key and secret](https://api.smugmug.com/api/developer/apply). Normally this is done by the owner of the application, but, since this is a local command line client, I'm not giving mine away. They're supposed to be secret after all :)
2. Once you have the SmugMug api key and secret, configure them with `xmpsync config --smKey <key> --smSecret <secret>`
3. Last step is to login to your account. It's an interactive process that can be triggered with any command which requires connection to SmugMug, e.g.:

```bash
[mpatercz@localhost 2018]$ xmpsync searchAlbums -q winter
What is your SmugMug username?
mysmname
Go to https://api.smugmug.com/services/oauth/1.0a/authorize?... to obtain verifier code. Paste it here and press enter.
12345
```

# Build

```bash
mvn clean install
(...)

$ java -jar target/xmpsync-2.0.0-SNAPSHOT-jar-with-dependencies.jar
Error: Command required
Try --help for more information.
```

# Disclaimer

This tool is provided "as is", without any expressed or implied warranty. Use at your own risk.




