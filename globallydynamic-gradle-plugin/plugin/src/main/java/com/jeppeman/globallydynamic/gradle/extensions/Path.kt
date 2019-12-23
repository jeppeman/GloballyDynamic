package com.jeppeman.globallydynamic.gradle.extensions

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

internal fun Path.deleteCompletely() {
    if (Files.isDirectory(this)) {
        Files.walkFileTree(this, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.delete(file)
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                Files.delete(dir)
                return FileVisitResult.CONTINUE
            }
        })
    } else {
        Files.deleteIfExists(this)
    }
}

internal fun Path.unzip(destinationDir: String) = toFile().unzip(destinationDir)