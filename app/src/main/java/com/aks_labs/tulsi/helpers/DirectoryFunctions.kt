package com.aks_labs.tulsi.helpers

import android.os.Environment
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.aks_labs.tulsi.datastore.AlbumInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

private const val TAG = "DIRECTORY_FUNCTIONS"
const val EXTERNAL_DOCUMENTS_AUTHORITY = "com.android.externalstorage.documents"

/** returns null if the folder doesn't exist ,
 * otherwise returns true if it has files, false if not */
fun Path.checkHasFiles(
    flipDotFileMatch: Boolean = false,
    matchSubDirs: Boolean = false
): Boolean? {
    var hasFiles = false

    Log.d(TAG, "Trying to traverse path $this")

    val folder = try {
        Files.walkFileTree(this, object : FileVisitor<Path> {
            override fun preVisitDirectory(
                dir: Path?,
                attrs: BasicFileAttributes?
            ): FileVisitResult {
                if (dir?.startsWith(this@checkHasFiles) == true && !dir.endsWith(this@checkHasFiles) && matchSubDirs) {
                    throw IOException("won't search path that's a subdir of ${this@checkHasFiles}")
                }

                val dataPath = baseInternalStorageDirectory + "Android/data"
                val obbPath = baseInternalStorageDirectory + "Android/obb"
                return if (dir?.startsWith(dataPath) == true || dir?.startsWith(obbPath) == true) {
                    if (this@checkHasFiles.startsWith(dataPath) || this@checkHasFiles.startsWith(
                            obbPath
                        )
                    ) {
                        throw IOException("Can't access file with path $this")
                    } else {
                        FileVisitResult.SKIP_SUBTREE
                    }
                } else {
                    FileVisitResult.CONTINUE
                }
            }

            override fun visitFile(path: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                if (path != null) {
                    val matchForDotFiles = Regex("\\.[A-z]")
                    val file = path.toFile()
                    val isDotFile =
                        file.absolutePath.contains(matchForDotFiles) && file.name.startsWith(".")

                    Log.d(TAG, "Trying to scan file ${file.absolutePath}.")

                    val matches = if (flipDotFileMatch) isDotFile else !isDotFile

                    if (matches) {
                        val isNormal = path.isRegularFile(LinkOption.NOFOLLOW_LINKS)
                        val mimeType = Files.probeContentType(path)

                        if (mimeType != null) {
                            val isMedia = mimeType.contains("image") || mimeType.contains("video")

                            if (isNormal && isMedia) {
                                Log.d(
                                    TAG,
                                    "Scanned file ${file.absolutePath} matches all prerequisites, exiting...."
                                )

                                hasFiles = true
                                return FileVisitResult.TERMINATE
                            }
                        }
                    }
                }

                hasFiles = false
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
                return FileVisitResult.CONTINUE
            }
        })
    } catch (e: Throwable) {
        Log.e(TAG, "The needed folder for this PhotoGrid doesn't exist!")
        Log.e(TAG, "Path for folder $this")
        Log.e(TAG, e.toString())

        null
    }

    if (folder == null) return null

    return hasFiles
}

fun String.checkPathIsDownloads(): Boolean =
    toRelativePath().startsWith(Environment.DIRECTORY_DOWNLOADS)
            && toRelativePath().removeSuffix("/").endsWith(Environment.DIRECTORY_DOWNLOADS)

fun String.getFileNameFromPath(): String = trim().removeSuffix("/").split("/").last()

fun String.getParentFromPath(): String =
    trim().replace(this.getFileNameFromPath(), "").removeSuffix("/")

val File.relativePath: String
    get() = this.absolutePath.replace(baseInternalStorageDirectory, "")

fun String.toRelativePath() = trim().replace(baseInternalStorageDirectory, "")

fun Path.getAllAlbumsOnDevice(): Flow<AlbumInfo> = channelFlow {
    val albums = mutableStateListOf<AlbumInfo>()

    val coroutineScope = CoroutineScope(context = coroutineContext)
    fun emitAlbums(info: AlbumInfo) = coroutineScope.launch {
        send(info)
    }

    try {
        Files.walkFileTree(this@getAllAlbumsOnDevice, object : FileVisitor<Path> {
            override fun preVisitDirectory(
                dir: Path?,
                attrs: BasicFileAttributes?
            ): FileVisitResult {
                val dataPath = baseInternalStorageDirectory + "Android/data"
                val obbPath = baseInternalStorageDirectory + "Android/obb"

                if (dir == null) return FileVisitResult.CONTINUE

                val album =
                    AlbumInfo(
                        id = dir.hashCode(),
                        name = dir.name,
                        paths = listOf(dir.toString().toRelativePath())
                    )

                return if (album in albums) {
                    FileVisitResult.SKIP_SUBTREE
                } else if (dir.startsWith(dataPath) || dir.startsWith(obbPath)) {
                    FileVisitResult.SKIP_SUBTREE
                } else {
                    FileVisitResult.CONTINUE
                }
            }

            override fun visitFile(path: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                if (path != null) {
                    val matchForDotFiles = Regex("\\.[A-z]")
                    val file = path.toFile()

                    val fileParentPath = file.absolutePath.toRelativePath().getParentFromPath()
                    val album =
                        AlbumInfo(
                            id = fileParentPath.hashCode(),
                            name = fileParentPath.getFileNameFromPath(),
                            paths = listOf(fileParentPath)
                        )

                    if (album in albums) return FileVisitResult.CONTINUE

                    val isDotFile =
                        file.absolutePath.contains(matchForDotFiles) && file.name.startsWith(".")

                    Log.d(TAG, "Trying to scan file ${file.absolutePath}.")

                    if (!isDotFile) {
                        val isNormal = path.isRegularFile(LinkOption.NOFOLLOW_LINKS)
                        val mimeType = Files.probeContentType(path)

                        if (mimeType != null) {
                            val isMedia = mimeType.contains("image") || mimeType.contains("video")

                            if (isNormal && isMedia) {
                                Log.d(
                                    TAG,
                                    "Scanned file ${file.absolutePath} matches all prerequisites, moving on...."
                                )

                                if (!albums.contains(album)) {
                                    albums.add(album)
                                    emitAlbums(album)
                                }

                                return FileVisitResult.CONTINUE
                            }
                        }
                    }
                }

                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
                return FileVisitResult.CONTINUE
            }
        })
    } catch (e: Throwable) {
        Log.e(TAG, "cannot traverse device directory tree")
        e.printStackTrace()
    }

    Log.d(TAG, "Got all albums on device.")
}.flowOn(Dispatchers.IO)


