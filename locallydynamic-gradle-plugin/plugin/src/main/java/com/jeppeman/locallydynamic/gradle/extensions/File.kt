package com.jeppeman.locallydynamic.gradle.extensions

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private fun newFile(destinationDir: File, zipEntry: ZipEntry): File {
    val destFile = File(destinationDir, zipEntry.name)

    val destDirPath = destinationDir.canonicalPath
    val destFilePath = destFile.canonicalPath

    if (!destFilePath.startsWith(destDirPath + File.separator)) {
        throw IOException("Entry is outside of the target dir: ${zipEntry.name}")
    }

    if (zipEntry.isDirectory) {
        destFile.mkdirs()
    } else {
        destFile.createNewFileAndParentDirectory()
    }

    return destFile
}

fun File.createNewFileAndParentDirectory() {
    if (!parentFile.exists()) {
        parentFile.mkdirs()
    }
    createNewFile()
}

fun File.unzip(destinationDir: String) {
    val buffer = ByteArray(1024)
    val destinationDirFile = File(destinationDir)
    ZipInputStream(inputStream()).use { zis ->
        var zipEntry = zis.nextEntry
        while (zipEntry != null) {
            val newFile = newFile(destinationDirFile, zipEntry)
            newFile.outputStream().use { fos ->
                var len: Int = zis.read(buffer)
                while (len > 0) {
                    fos.write(buffer, 0, len)
                    len = zis.read(buffer)
                }
            }
            zipEntry = zis.nextEntry
        }
        zis.closeEntry()
    }
}