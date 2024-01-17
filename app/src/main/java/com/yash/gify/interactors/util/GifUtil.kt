package com.yash.gify.interactors.util

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import com.yash.gify.domain.VersionProvider
import com.yash.gify.domain.util.AnimatedGIFWriter
import com.yash.gify.domain.util.CacheProvider
import com.yash.gify.domain.util.FileNameBuilder
import com.yash.gify.interactors.BuildGif
import com.yash.gify.interactors.BuildGifInteractor
import java.io.ByteArrayOutputStream
import java.io.File

object GifUtil {
    fun buildGifAndSaveToInternalStorage(
        contentResolver: ContentResolver,
        versionProvider: VersionProvider,
        cacheProvider: CacheProvider,
        bitmaps: List<Bitmap>

    ): BuildGif.BuildGifResult{

        check(bitmaps.isNotEmpty()){
            "Error"
        }

        val writer= AnimatedGIFWriter(true)

        val bos= ByteArrayOutputStream()

        writer.prepareForWrite(bos,-1,-1)
        for(bitmap in bitmaps){
            writer.writeFrame(bos,bitmap)
        }
        writer.finishWrite(bos)
        var byteArray=bos.toByteArray()
        var uri=saveGifToInternalStorage(
            contentResolver=contentResolver,
            versionProvider=versionProvider,
            cacheProvider=cacheProvider,
            bytes = byteArray
        )

        return BuildGif.BuildGifResult(uri,byteArray.size)

    }

    @SuppressLint("NewApi")
    fun saveGifToInternalStorage(
        contentResolver: ContentResolver,
        versionProvider: VersionProvider,
        cacheProvider: CacheProvider,
        bytes: ByteArray
    ): Uri {
        var fileName= if(versionProvider.provideVersion()>= Build.VERSION_CODES.O){
            "${FileNameBuilder.buildFileNameAPI26()}.gif"
        }
        else{
            "${FileNameBuilder.buildFileName()}.gif"
        }
        var file= File.createTempFile(fileName,null,cacheProvider.gifCache())
        var uri=file.toUri()

        return contentResolver.openOutputStream(uri)?.let { os->
            os.write(bytes)
            os.flush()
            os.close()
            uri
        }?:throw Exception(BuildGifInteractor.SAVE_GIF_TO_INTERNAL_STORAGE_ERROR)
    }

}