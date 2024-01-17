package com.yash.gify

import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.canhub.cropper.CropImageContractOptions
import com.yash.gify.domain.DataState
import com.yash.gify.domain.DataState.Loading.LoadingState
import com.yash.gify.domain.DataState.Loading.LoadingState.*

sealed class MainState {
    object Initial:MainState()
    object DisplaySelectBackgroundAsset:MainState()
    data class DisplayBackgroundAsset(

        var backgroundAssetUri: Uri?,
        var capturingViewBounds:androidx.compose.ui.geometry.Rect?=null,
        var capturedBitmap: List<Bitmap> = listOf(),
        var bitmapCaptureLoadingState: LoadingState= Idle,
        var loadingState:LoadingState=Idle

        ):MainState()

    data class DisplayGif(
        var gifUri:Uri?,
        var originalGifSize:Int,
        var resizedGifUri:Uri?,
        var adjustedBytes:Int,
        var sizePercentage:Int,
        var capturedBitmaps: List<Bitmap> =listOf(),
        var loadingState: LoadingState=Idle,

        var resizeGifLoadingState:LoadingState=Idle,
        var backgroundAssetUri: Uri?,

    ):MainState()


}