package com.yash.gify.interactors

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import com.yash.gify.domain.DataState
import com.yash.gify.domain.DataState.*
import com.yash.gify.domain.DataState.Loading.LoadingState.*
import com.yash.gify.domain.VersionProvider
import com.yash.gify.domain.util.AnimatedGIFWriter
import com.yash.gify.domain.util.CacheProvider
import com.yash.gify.domain.util.FileNameBuilder
import com.yash.gify.interactors.util.GifUtil.buildGifAndSaveToInternalStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.ByteArrayOutputStream
import java.io.File

interface BuildGif {
    fun execute(
        contentResolver: ContentResolver,
        bitmaps:List<Bitmap>
    ):Flow<DataState<BuildGifResult>>


    data class BuildGifResult(
        var uri: Uri,
        var gifSize:Int
    )
}

class BuildGifInteractor constructor(
    var versionProvider: VersionProvider,
    var cacheProvider: CacheProvider

    ):BuildGif{
    override fun execute(contentResolver: ContentResolver,bitmaps: List<Bitmap>): Flow<DataState<BuildGif.BuildGifResult>> =flow{
        emit(Loading(Active()))

        try{
            var result=buildGifAndSaveToInternalStorage(
                versionProvider=versionProvider,
                contentResolver=contentResolver,
                cacheProvider=cacheProvider,
                bitmaps=bitmaps,

            )
            emit(DataState.Data(result))
        }
        catch (e:Exception){
            emit(Error(e.message?:BUILD_GIF_ERROR))
        }
        emit(Loading(Idle))

    }
    companion object{
        var BUILD_GIF_ERROR="Error gif building"
        var N0_BITMAPS_ERROR="No error"
        var SAVE_GIF_TO_INTERNAL_STORAGE_ERROR="Error saving the gif to cache"

    }

}