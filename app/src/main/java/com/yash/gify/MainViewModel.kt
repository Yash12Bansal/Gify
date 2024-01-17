package com.yash.gify

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.view.Display
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.gify.MainState.*
import com.yash.gify.domain.DataState
import com.yash.gify.domain.DataState.Loading.LoadingState.*
import com.yash.gify.domain.RealVersionProvider
import com.yash.gify.domain.VersionProvider
import com.yash.gify.domain.util.CacheProvider
import com.yash.gify.interactors.*
import com.yash.gify.interactors.CaptureBitmapsInteractor.Companion.CAPTURE_BITMAP_ERROR
import com.yash.gify.interactors.CaptureBitmapsInteractor.Companion.CAPTURE_BITMAP_SUCCESS
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FilePermission
import java.util.*

class MainViewModel: ViewModel() {
    private var cacheProvider: CacheProvider?=null
    var dispatcher=IO
    var versionProvider:VersionProvider=RealVersionProvider()
    var mainDispatcher=Dispatchers.Main
    var pixelCopy:PixelCopyJob=PixelCopyJobInteractor()

    var saveGifToExternalStorage=SaveGifToExternalStorageInteractor(
        versionProvider=versionProvider,

    )

    var captureBitmaps:CaptureBitmaps=CaptureBitmapsInteractor(
        pixelCopyJob =  pixelCopy,
        versionProvider=versionProvider,
        mainDispatcher=mainDispatcher
    )
    var _state: MutableState<MainState> = mutableStateOf(Initial)
    val state:State<MainState>
        get()=_state

    val _toastEventRelay: MutableStateFlow<ToastEvent?> =MutableStateFlow(null)
    val toastEventRelay:StateFlow<ToastEvent?> get()=_toastEventRelay
    val _errorEventRelay: MutableStateFlow<Set<ErrorEvent>> =MutableStateFlow(emptySet())
    val errorEventRelay:StateFlow<Set<ErrorEvent>> get()=_errorEventRelay


    fun setCacheProvider(
        cacheProvider:CacheProvider
    ){
        this.cacheProvider=cacheProvider

    }

    fun resizeGif(
        contentResolver: ContentResolver
    ){
        check(state.value is DisplayGif){"Invalid State"}

        (state.value as DisplayGif).let{
            var targetSize=it.originalGifSize*(it.sizePercentage.toFloat()/100)
            var resize=ResizeGifInteractor(
                versionProvider=versionProvider,
                cacheProvider=cacheProvider!!
            )
            resize.execute(
                contentResolver=contentResolver,
                captureBitmaps=it.capturedBitmaps,
                originalGifSize = it.originalGifSize,
                targetSize=targetSize,
                discardCachedGif={
                    discardCachedGif(it)
                }
            ).onEach{dataState->
                when(dataState){
                    is DataState.Data ->{

                        dataState.data?.let{data->
                            updateState(
                                (state.value as DisplayGif).copy(
                                    resizedGifUri = data.uri,
                                    adjustedBytes = data.gifSize
                                )
                            )
                        }?:publishErrorEvent(
                            ErrorEvent(
                                id=UUID.randomUUID().toString(),
                                message = "heelo"
                            )
                        )
                    }
                    is DataState.Error->{
                        publishErrorEvent(
                            ErrorEvent(
                                id=UUID.randomUUID().toString(),
                                message = dataState.message
                            )

                        )
                        updateState(
                            (state.value as DisplayBackgroundAsset).copy(loadingState = Idle)
                        )

                    }
                    is DataState.Loading->{
                            updateState(
                                (state.value as DisplayGif).copy(resizeGifLoadingState = dataState.loading)
                            )

//                    else{
//
//                    }
                    }
                }

            }
                .onCompletion{
                    updateState(
                        (state.value as DisplayGif).copy(loadingState = Idle)
                    )
                }
                .flowOn(dispatcher).launchIn(viewModelScope)
        }

    }
    fun buildGif(
        contentResolver: ContentResolver,

    ){
        check(state.value is DisplayBackgroundAsset){"Invalid state"}

        var captureBitmaps= (state.value as DisplayBackgroundAsset).capturedBitmap

        check(captureBitmaps.isNotEmpty()){
            "You have no bitmaps to build a gif with"
        }

        updateState(
            (state.value as DisplayBackgroundAsset).copy(loadingState = Active())
        )

        var buildGif:BuildGif=BuildGifInteractor(
            cacheProvider=cacheProvider!!,
            versionProvider = versionProvider
        )
        buildGif.execute(
            contentResolver = contentResolver,
            bitmaps=captureBitmaps
        ).onEach {dataState->
            when(dataState){
                is DataState.Data ->{
                    (state.value as DisplayBackgroundAsset).let{
                        var gifSize=dataState.data?.gifSize?:0
                        var gifUri=dataState.data?.uri
                        updateState(
                            DisplayGif(
                                gifUri=gifUri,
                                originalGifSize = gifSize,
                                backgroundAssetUri = it.backgroundAssetUri,
                                resizedGifUri = null,
                                adjustedBytes = gifSize,
                                sizePercentage = 100,
                                capturedBitmaps = it.capturedBitmap
                            )
                        )


                    }
                }
                is DataState.Error->{
                    publishErrorEvent(
                        ErrorEvent(
                            id=UUID.randomUUID().toString(),
                            message = dataState.message
                        )

                    )
                    updateState(
                        (state.value as DisplayBackgroundAsset).copy(loadingState = Idle)
                    )

                }
                is DataState.Loading->{
                    if (state.value  is DisplayBackgroundAsset){
                        updateState(
                            (state.value as DisplayBackgroundAsset).copy(loadingState = dataState.loading)
                        )

                    }
//                    else{
//
//                    }
                }
            }
        }.flowOn(dispatcher).launchIn(viewModelScope)

    }
    fun runBitmapCaptureJob(
        contentResolver:ContentResolver,
        view: View,
        window:Window
    ){
//        var state=state.value
        check(state.value is DisplayBackgroundAsset){
            "Invalid State"
        }
        updateState(
            (state.value as DisplayBackgroundAsset).copy(bitmapCaptureLoadingState=Active(0f))
        )
        var bitmapCaptureJob= Job()

        var checkShouldCancelJob:(MainState)->Unit={mainState->
            var shouldCancel=when(mainState){
                is DisplayBackgroundAsset->{
                    mainState.bitmapCaptureLoadingState !is Active
                }
                else->true

            }

            if(shouldCancel){
                bitmapCaptureJob.cancel(CAPTURE_BITMAP_SUCCESS)

            }

        }

        captureBitmaps.execute(
            capturingViewBound=(state.value as DisplayBackgroundAsset).capturingViewBounds,
            window=window,
            view=view
        ).onEach { dataState->
            checkShouldCancelJob(state.value)
            when(dataState){
                is DataState.Data->{
                    dataState.data?.let { bitmaps->
                        updateState(
                            (state.value as DisplayBackgroundAsset).copy(
                                capturedBitmap = bitmaps
                            )
                        )

//                        _state.value=state.copy(
//                            capturedBitmap = bitmaps
//                        )

                    }
                }
                is DataState.Error->{
                    bitmapCaptureJob.cancel(CAPTURE_BITMAP_ERROR)
                    updateState(
                        (state.value as DisplayBackgroundAsset).copy(
                            bitmapCaptureLoadingState = Idle
                        )
                    )

                    publishErrorEvent(
                        ErrorEvent(
                            id=UUID.randomUUID().toString(),
                            message =  dataState.message
                        )
                    )
                }
                is DataState.Loading->{

                    updateState(
                        (state.value as DisplayBackgroundAsset).copy(
                            bitmapCaptureLoadingState = dataState.loading
                        )
                    )

                }
            }
        }.flowOn(dispatcher).launchIn(viewModelScope+bitmapCaptureJob)
            .invokeOnCompletion{throwable->
                updateState(
                    (state.value as DisplayBackgroundAsset).copy(
                        bitmapCaptureLoadingState = Idle
                    )
                )

                var onSuccess:()->Unit ={
                    buildGif(contentResolver = contentResolver)
//                    var newState=_state.value
//                    if(newState is DisplayBackgroundAsset){
//
//
//                    }

                }

                when(throwable){
                    null-> onSuccess()
                    else->{
                        if(throwable.message==CAPTURE_BITMAP_SUCCESS){
                            onSuccess()
                        }else{
                            publishErrorEvent(
                                ErrorEvent(
                                    id=UUID.randomUUID().toString(),
                                    message = throwable.message?:CAPTURE_BITMAP_ERROR
                                )
                            )
                        }
                    }
                }
            }


//
//        CoroutineScope(dispacher).launch {
//            var result=pixelCopy.execute(
//                capturingViewBound = state.capturingViewBounds,
//                view=view,
//                window=window
//            )
//
//            when(result){
//                is PixelCopyJob.PixelCopyJobState.Done->{
//                    _state.value=state.copy(capturedBitmap = result.bitmap)
//
//                }
//                is PixelCopyJob.PixelCopyJobState.Error->{
//                    publishErrorEvent(ErrorEvent(id=UUID.randomUUID().toString(),result.message
//                    ))
//
//                }
//            }
//        }
    }
    fun publishErrorEvent(errorEvent: ErrorEvent){

        var current=_errorEventRelay.value.toMutableSet()
        current.add(errorEvent)
        _errorEventRelay.value=current
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveGif(
        contentResolver:ContentResolver,
        context:Context,
        launchPermissionRequest:()->Unit,
        checkFilePermission:()->Boolean,
    ){

        check(state.value is DisplayGif){"Invalid state"}

        if(!checkFilePermission()){
            launchPermissionRequest()
            return
        }
        var uriToSave=(state.value as DisplayGif).let{
            it.resizedGifUri?:it.gifUri
        } ?: throw Exception("Save error")
        saveGifToExternalStorage.execute(
            contentResolver = contentResolver,
            context = context,
            cachedUri = uriToSave,
            checkFilesPermission=checkFilePermission
        ).onEach {dataState->
            when(dataState){
                is DataState.Data->toastShow(message = "Saved Successuflly to Stod")
                is DataState.Loading->{
                    updateState(
                        (state.value as DisplayGif).copy(loadingState = dataState.loading)
                    )

                }
                is DataState.Error->{
                    publishErrorEvent(
                        ErrorEvent(
                            id = UUID.randomUUID().toString(),
                            message = dataState.message
                        )
                    )

                }
            }
        }
            .onCompletion {
                clearCachedFiles()
                updateState(
                    DisplayBackgroundAsset(
                        backgroundAssetUri = (state.value as DisplayGif).backgroundAssetUri
                    )
                )
            }
            .flowOn(dispatcher).launchIn(viewModelScope)



    }
    fun clearCachedFiles(){
        var clearGifCache:ClearGifCache=ClearGifCacheInteractor(
            cacheProvider=cacheProvider!!
        )
        clearGifCache.execute(

        ).onEach {

        }.flowOn(dispatcher).launchIn(viewModelScope)

    }

    fun deleteGif(){
        clearCachedFiles()
        check(state.value is DisplayGif){"Invalid"}
        updateState(
            DisplayBackgroundAsset(
                backgroundAssetUri = (state.value as DisplayGif).backgroundAssetUri
            )
        )
    }
    fun endBitmapCaptureJob(){
        check(state.value is DisplayBackgroundAsset){
            "Invalid"
        }
        updateState((state.value as DisplayBackgroundAsset).copy(bitmapCaptureLoadingState=Idle))
    }
    fun updateState(mainState: MainState){
        _state.value=mainState

    }
    fun toastShow(
        id:String=UUID.randomUUID().toString(),
        message:String
    ){
        _toastEventRelay.tryEmit(
            ToastEvent(
                id=id,
                message=message
            )
        )
    }


    fun resetGifToOriginal(){
        check(state.value is DisplayGif){
            "Invalid state"
        }

        (state.value as DisplayGif).run {
            resizedGifUri.let {
                discardCachedGif(it!!)
            }
            updateState(
                this.copy(
                    resizedGifUri=null,
                    adjustedBytes = originalGifSize,
                    sizePercentage = 100
                )
            )

        }

    }

    fun updateAdjustedBytes(adjustedBytes:Int){

        check(state.value is DisplayGif){
            "Invalid state"
        }

        updateState(
            (state.value as DisplayGif).copy(
                adjustedBytes = adjustedBytes
            )
        )


    }

    fun updateSizePercentage(sizePercentage:Int){
        check(state.value is DisplayGif){
            "Invalid state"
        }

        updateState(
            (state.value as DisplayGif).copy(
                sizePercentage = sizePercentage
            )
        )

    }

    companion object{
        fun discardCachedGif(uri:Uri){
            var file= File(uri.path)
            var success=file.delete()
            if(!success){
                throw Exception("Error ho gayo")
            }
        }
    }
}