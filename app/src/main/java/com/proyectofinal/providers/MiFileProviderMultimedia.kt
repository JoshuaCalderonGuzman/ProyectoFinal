package com.proyectofinal.providers
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.proyectofinal.R
import java.io.File
import java.util.UUID

data class MediaFile(
    val contentUri: Uri,
    val relativePath: String // e.g., "images/img_1678886400000.jpg"
)
class MiFileProviderMultimedia: FileProvider(
    R.xml.file_paths
)  {
    companion object{
        fun getImageUri (ctx : Context): MediaFile {

            val dirIma = File(ctx.filesDir, "images")
            dirIma.mkdirs()
            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val fileImage = File(dirIma, fileName)

            val auth = ctx.packageName + ".fileprovidermultimedia"
            val contentUri = getUriForFile(ctx, auth, fileImage)
            val relativePath = "images/$fileName"

            return MediaFile(contentUri, relativePath)
        }

        fun getVideoUri (ctx : Context): MediaFile {
            // 1. Crear el directorio específico para videos en la caché
            val dirVideo = File(ctx.filesDir, "videos")
            dirVideo.mkdirs()

            // 2. Crear un archivo  con extensión .mp4
            val fileName = "vid_${System.currentTimeMillis()}.mp4"
            val fileVideo = File(dirVideo, fileName)

            // 3. Definir la autoridad del FileProvider
            val auth = ctx.packageName + ".fileprovidermultimedia"
            val contentUri = getUriForFile(ctx, auth, fileVideo)
            // 4. Devolver la Uri de contenido FileProvider
            val relativePath = "videos/$fileName"

            return MediaFile(contentUri, relativePath)        }

    }

}
