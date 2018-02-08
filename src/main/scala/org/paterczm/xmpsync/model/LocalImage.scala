package org.paterczm.xmpsync.model

import org.paterczm.xmpsync.xmp.XMP
import java.io.File

case class LocalImage(file: File, private val lazyXmp: () => XMP) {
	lazy val xmp = lazyXmp.apply()
}