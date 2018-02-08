package org.paterczm.xmpsync.xmp

import java.io._

import scala.collection.mutable.ArrayBuffer

import org.paterczm.xmpsync.model.LocalImage

object RecursiveJPGFinder {
	def apply(dir:File):Array[File] = {
		if (!dir.isDirectory()) {
			throw new Exception(dir.getName()+" needs to be a directory!")
		}

		val jpgs = ArrayBuffer[File]()

		processDir(dir, jpgs)

		return jpgs.toArray
	}

	private def processDir(dir:File, jpgs:ArrayBuffer[File]) {

		// append
		jpgs ++= findJPGs(dir)

		for (subdir <- findDirs(dir)) {
			processDir(subdir, jpgs)
		}

	}

	private def findJPGs(dir:File):Array[File] = {
		return dir.listFiles.filter(_.isFile()).filter(_.getName().toLowerCase().endsWith("jpg"))
	}

	private def findDirs(dir:File):Array[File] = {
		return dir.listFiles.filter(_.isDirectory())
	}
}

object RecursiveJPGProcessor {

	def apply(dir:File):Map[String, List[LocalImage]] = {

		val map = scala.collection.mutable.Map[String, List[LocalImage]]()

		RecursiveJPGFinder(dir) foreach {jpg =>

			val key = jpg.getName.toLowerCase()

			if (map.contains(key)) {
				map.put(key, LocalImage(jpg, () => XMP(jpg)) :: map(key))
			} else {
				map.put(key, List(LocalImage(jpg, () => XMP(jpg))))
			}
		}

		map.toMap

	}

	def main(args: Array[String]): Unit = {
	  RecursiveJPGFinder(new File("""/home/mpatercz/mount""")) foreach println
	}
}