package com.yash.gify.interactors

import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.Window
import androidx.compose.ui.geometry.Rect
import androidx.core.graphics.applyCanvas
import com.yash.gify.ErrorEvent
import com.yash.gify.domain.DataState
import com.yash.gify.domain.DataState.*
import com.yash.gify.domain.DataState.Loading.LoadingState.*
import com.yash.gify.domain.VersionProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.roundToInt

interface CaptureBitmaps{

    fun execute(
        capturingViewBound: androidx.compose.ui.geometry.Rect?,
        view: View?,
        window: Window
    ):Flow<DataState<List<Bitmap>>>

}


class CaptureBitmapsInteractor constructor(
    var pixelCopyJob:PixelCopyJob,
    var versionProvider: VersionProvider,
    var mainDispatcher:CoroutineDispatcher
):CaptureBitmaps{
    override fun execute(
        capturingViewBound: Rect?,
        view: View?,
        window: Window)
    : Flow<DataState<List<Bitmap>>> =flow{
        emit(Loading(Active()))
        try{
            check(capturingViewBound!=null){"Invalid "}
            check(view!=null){"Invalid "}

            var elaspedTime=0f

            var bitmaps:MutableList<Bitmap> = mutableListOf()
            while(elaspedTime<=TOTAL_CAPTURE_TIME){
                delay(CAPTURE_INTERVAL_MS.toLong())
                elaspedTime+= CAPTURE_INTERVAL_MS
                emit(Loading(Active(elaspedTime/ TOTAL_CAPTURE_TIME )))
                var bitmap= if (versionProvider.provideVersion()>=Build.VERSION_CODES.O){
                    check(window!=null){
                        "Window required"
                    }
                    var pixelCopyJobState=pixelCopyJob.execute(
                        capturingViewBound=capturingViewBound,
                        view = view,
                        window=window

                    )
                    when(pixelCopyJobState){
                        is PixelCopyJob.PixelCopyJobState.Done->{
                            pixelCopyJobState.bitmap

                        }
                        is PixelCopyJob.PixelCopyJobState.Error->{
                            throw java.lang.Exception(pixelCopyJobState.message)

                        }
                    }
                }
                else{
                    captureBitmap(
                        rect=capturingViewBound,
                        view=view,
                    )

                }
                bitmaps.add(bitmap)
                emit(DataState.Data(bitmaps.toList()))
            }
        }
        catch (e:Exception){
            emit(Error(e.message?:CAPTURE_BITMAP_ERROR))
        }

        emit(Loading(Idle))
    }
    suspend fun captureBitmap(rect: Rect, view: View)= withContext(mainDispatcher){
        check(rect!=null){
            "Invalid rect"
        }
        var bitmap=Bitmap.createBitmap(
            rect.width.roundToInt(),
            rect.height.roundToInt(),
            Bitmap.Config.ARGB_8888
        ).applyCanvas {
            translate(-rect.left,-rect.top)
            view.draw(this)
        }
        return@withContext bitmap

    }


    companion object{
        var CAPTURE_BITMAP_ERROR="Error bitmap capturing"
        const val TOTAL_CAPTURE_TIME=4000f
        const val CAPTURE_INTERVAL_MS=250f
        val CAPTURE_BITMAP_SUCCESS="Success bitmap"
    }

}