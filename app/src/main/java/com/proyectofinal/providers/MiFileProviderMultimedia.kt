package com.proyectofinal.providers
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.proyectofinal.R
import java.io.File

class MiFileProviderMultimedia: FileProvider(
    R.xml.file_paths
)  {
    companion object{
        fun getImageUri (ctx : Context): Uri {

            val dirIma = File(ctx.cacheDir, "images")
            dirIma.mkdirs()

            val fileImage = File.createTempFile("img_",
                ".jpg", dirIma)

            val auth = ctx.packageName + ".fileprovidermultimedia"

            return getUriForFile(ctx, auth, fileImage)

        }
    }

}