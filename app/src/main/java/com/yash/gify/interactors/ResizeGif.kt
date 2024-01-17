package com.yash.gify.interactors

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import com.yash.gify.domain.DataState
import com.yash.gify.domain.VersionProvider
import com.yash.gify.domain.util.CacheProvider
import com.yash.gify.interactors.util.GifUtil.buildGifAndSaveToInternalStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface ResizeGif {
    fun execute(
        contentResolver: ContentResolver,
        captureBitmaps:List<Bitmap>,
        originalGifSize:Int,
        targetSize:Float,
        bilinearFiltering:Boolean=true,
        discardCachedGif:(Uri)->Unit,

    ):Flow<DataState<ResizeGifResult>>

    data class ResizeGifResult(
        var uri: Uri,
        var gifSize:Int,
    )
}

class ResizeGifInteractor constructor(
    var versionProvider: VersionProvider,
    var cacheProvider: CacheProvider,

):ResizeGif{
    override fun execute(
        contentResolver: ContentResolver,
        captureBitmaps: List<Bitmap>,
        originalGifSize: Int,
        targetSize: Float,
        bilinearFiltering: Boolean,
        discardCachedGif: (Uri) -> Unit
    ): Flow<DataState<ResizeGif.ResizeGifResult>> =flow{

        var previousUri:Uri?=null
        var progress:Float
        var percentageLoss=percentageLossIncrementSize

        emit(DataState.Loading(DataState.Loading.LoadingState.Active(percentageLoss)))

        try{
            var resizing=true
            while(resizing){
                previousUri?.let{
                    discardCachedGif(it)


                }

                var resizedBitmaps:MutableList<Bitmap> = mutableListOf()
                for(bitmap in captureBitmaps){
                    var resizedBitmap=resizeBimap(
                        bitmap=bitmap,
                        sizePercentage=1-percentageLoss,
                        bilinearFiltering=bilinearFiltering,

                    )
                    resizedBitmaps.add(resizedBitmap)
                }

                var result=buildGifAndSaveToInternalStorage(
                    contentResolver=contentResolver,
                    versionProvider=versionProvider,
                    cacheProvider=cacheProvider,
                    bitmaps = resizedBitmaps
                )
                var uri=result.uri
                var newSize=result.gifSize
                progress=(originalGifSize-newSize)/(originalGifSize-targetSize)
                emit(DataState.Loading(DataState.Loading.LoadingState.Active(progress)))
                if(newSize>targetSize){
                    previousUri=uri
                    percentageLoss+= percentageLossIncrementSize
                }
                else{
                    emit(DataState.Data(ResizeGif.ResizeGifResult(uri=uri, gifSize = newSize)))
                    resizing=false
                }
            }
            emit(DataState.Loading(DataState.Loading.LoadingState.Idle))
        }
        catch (e:Exception){

        }
    }
    fun resizeBimap(
        bitmap: Bitmap,
        sizePercentage:Float,
        bilinearFiltering: Boolean
    ):Bitmap{

        var targetWidth=(bitmap.width*sizePercentage).toInt()
        var targetHeight=(bitmap.height*sizePercentage).toInt()
        return Bitmap.createScaledBitmap(bitmap,targetWidth,targetHeight,bilinearFiltering)
    }
    companion object{
        var percentageLossIncrementSize=0.05f
    }

}