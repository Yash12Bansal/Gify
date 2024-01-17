package com.yash.gify.interactors

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import com.yash.gify.interactors.PixelCopyJob.PixelCopyJobState.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.roundToInt

interface PixelCopyJob {

    suspend fun execute(
        capturingViewBound: androidx.compose.ui.geometry.Rect?,
        view: View,
        window:Window
    ):PixelCopyJobState

    sealed class PixelCopyJobState{
        data class Done(
            val bitmap: Bitmap
        ):PixelCopyJobState()
        data class Error(
            val message:String
        ):PixelCopyJobState()


    }
}

class PixelCopyJobInteractor:PixelCopyJob{
    
    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun execute(
        capturingViewBound: androidx.compose.ui.geometry.Rect?,
        view: View,
        window: Window
    ): PixelCopyJob.PixelCopyJobState= suspendCancellableCoroutine {cont->

        try{
            check(capturingViewBound!=null){
                "Invalid Area"
            }
            var bitmap=Bitmap.createBitmap(
                view.width,view.height,Bitmap.Config.ARGB_8888
            )
            var locationOfViewWindow=IntArray(2)
            view.getLocationInWindow(locationOfViewWindow)
            var xCoordinate=locationOfViewWindow[0]
            var yCoordinate=locationOfViewWindow[1]

            var scope=android.graphics.Rect(
                xCoordinate,
                yCoordinate,
                xCoordinate+view.width,
                yCoordinate+view.height
            )

            PixelCopy.request(
                window,
                scope,
                bitmap,
                {
                    p0->
                    if(p0==PixelCopy.SUCCESS){

                        var bmp = Bitmap.createBitmap(
                            bitmap,
                            capturingViewBound.left.toInt(),
                            capturingViewBound.top.toInt(),
                            capturingViewBound.width.roundToInt(),
                            capturingViewBound.height.roundToInt(),

                        )
                        cont.resume(Done(bmp))
                    }
                    else
                    {
                        cont.resume(Error(PIXEL_COPY_ERROR))
                    }
                },
                Handler(Looper.getMainLooper())
            )
        }
        catch (e:Exception){
            cont.resume(Error(PIXEL_COPY_ERROR))
        }
    }

    companion object{
        var PIXEL_COPY_ERROR="hee"
    }
}