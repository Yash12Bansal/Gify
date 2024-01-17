package com.yash.gify

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.canhub.cropper.options
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.imageLoader
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.yash.gify.ui.compose.BackgroundAsset
import com.yash.gify.ui.compose.SelectBackgroundAsset
import com.yash.gify.ui.theme.GifyTheme
import java.lang.Math.*
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageActivity
import com.yash.gify.domain.util.RealCacheProvider
import com.yash.gify.ui.compose.Gif
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainActivity : ComponentActivity() {


    val cropAssetLauncher:ActivityResultLauncher<CropImageContractOptions> =registerForActivityResult(CropImageContract()){result->
        if(result.isSuccessful){
            result.uriContent.let { uri->
                when(val state=viewModel.state.value){
                    is MainState.DisplayBackgroundAsset,
                    is MainState.DisplaySelectBackgroundAsset->{
                        viewModel.updateState(MainState.DisplayBackgroundAsset(backgroundAssetUri = uri))
//                        _state.value=MainState.DisplayBackgroundAsset(
//                            backgroundAssetUri =uri
//                        )

                    }
                    else->
                        throw Exception("Invalid")
                }


            }
        }
        else{
            viewModel.toastShow(
                message="Something went wrong"
            )
        }
    }
    var backgroundAssetPL:ActivityResultLauncher<String> =registerForActivityResult(ActivityResultContracts.GetContent()){uri->
    }

    val viewModel:MainViewModel by viewModels()
    lateinit var imageLoader: ImageLoader

    fun checkFilePermission():Boolean{
        var writePermission=ContextCompat.checkSelfPermission(this,android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        var readPermission=ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)

        return writePermission==PackageManager.PERMISSION_GRANTED && readPermission==PackageManager.PERMISSION_GRANTED
    }

    var externalStoragePermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()){permissions->
        permissions.entries.forEach {
            if(!it.value){
                viewModel.toastShow(message="Permission needed")
            }
        }


    }

    fun launchPermissionRequest(){
        externalStoragePermissionRequest.launch(
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        )
    }


    @SuppressLint("NewApi")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageLoader=ImageLoader.Builder(application)
            .components{
                if(Build.VERSION.SDK_INT>=28){
                    add(ImageDecoderDecoder.Factory())
                }
                else{
                    add(GifDecoder.Factory())
                }

            }.build()
        viewModel.setCacheProvider(RealCacheProvider(application))

        viewModel.toastEventRelay.onEach { toastEvent->
            toastEvent.let {
                Toast.makeText(this@MainActivity,it?.message,Toast.LENGTH_LONG).show()
            }
        }.launchIn(lifecycleScope)
        setContent {
            GifyTheme {
                Surface(modifier=Modifier.fillMaxSize()){
                    var state=viewModel.state.value
                    var view= LocalView.current
                    Column(modifier=Modifier.fillMaxSize()){
                        when(state){
                            is MainState.Initial ->{
                                viewModel.updateState(MainState.DisplaySelectBackgroundAsset)
                            }
                            is MainState.DisplaySelectBackgroundAsset->
                                SelectBackgroundAsset(
                                    launchImagePicker={

                                        cropAssetLauncher.launch(
                                            options {
                                                Intent.EXTRA_ALLOW_MULTIPLE
                                                setGuidelines(CropImageView.Guidelines.ON)

                                            }
                                        )
                                    }

                                //                                    launchImagePicker={
//                                        backgroundAssetPL.launch("image/*")
//                                    }
                                )
                            is MainState.DisplayBackgroundAsset-> state.backgroundAssetUri?.let {
                                BackgroundAsset(
                                    backgroundAssetUri= it,
//                                    capturedBitmap=state.capturedBitmap,
                                    updateCapturingViewBounds={
                                            rect->
                                        viewModel.updateState(
                                            state.copy(capturingViewBounds = rect)
                                        )
                                    },
                                    startBitmapCaptureJob={
                                        viewModel.runBitmapCaptureJob(
                                            contentResolver=contentResolver,
                                            view=view,
                                            window=window
                                        )
                                    },
                                    stopBitmapCaptureJob = viewModel::endBitmapCaptureJob,
                                    bitmapCaptureLoadingState = state.bitmapCaptureLoadingState,
                                    launchImagePicker={
                                        cropAssetLauncher.launch(
                                            options {
                                                setGuidelines(CropImageView.Guidelines.ON)
                                            }
                                        )
                                    },
                                    loadingState = state.loadingState
                                )
                            }
                            is MainState.DisplayGif-> Gif(
                                imageLoader =imageLoader ,
                                gifUri =state.resizedGifUri?:state.gifUri ,
                                discardGif = viewModel::deleteGif,
                                onSaveGif ={
                                    viewModel.saveGif(
                                        context = this@MainActivity,
                                        contentResolver = contentResolver,
                                        launchPermissionRequest = ::launchPermissionRequest,
                                        checkFilePermission=::checkFilePermission,
                                    )
                                },
                                resetToOriginal=viewModel::resetGifToOriginal,
                                isResizedGif=state.resizedGifUri!=null,
                                currentGifSize=state.originalGifSize,
                                adjustedBytes=state.adjustedBytes,
                                updateAdjustedBytes =viewModel::updateAdjustedBytes,
                                sizePercentage=state.sizePercentage,
                                updateSizePercentage=viewModel::updateSizePercentage,
                                resizeGif={
                                          viewModel.resizeGif(contentResolver=contentResolver)
                                },
                                gifResizingLoadingState=state.resizeGifLoadingState,


                                loadingState = state.loadingState
                            )
                        }
                    }
                }
            }
//            GifyTheme {
////                var counter by remember{ mutableStateOf(0)}
////                var updateCounter={ countt:Int ->counter=countt}
////                Column() {
////                    button(counter = counter, updateCounter = updateCounter)
////                    if(counter>5){
////                        Text("5 SE ELRKJ")
////                    }
////                    lll(listOf("Steve Jobs","Bill Gates","Larry Page","Elon Musk","Jeff Bezos","Jeff Bezos","Jeff Bezos","Jeff Bezos","Larry Ellison","Bernard Arnault","Mukesh Ambani","Even Speigel","Jeff Bezos","Jeff Bezos","Jeff Bezos","Jeff Bezos","Jeff Bezos","Jeff Bezos","Jeff Bezos"))
////
////                }
//            }
        }
    }
}
@Composable
fun button(counter:Int,updateCounter:(Int)->Unit){
    Button(onClick = {
        updateCounter(counter+1)
    }){
        Text("Hello $counter")
    }
}
@Composable
fun lll(founders: List<String>){
    LazyColumn(modifier = Modifier.fillMaxWidth()){
        items(items=founders){
            Text(it,modifier=Modifier.padding(15.dp))
        }
    }
}
@Composable
fun List1(founders:List<String>){

    Column(){
        LazyColumn(modifier=Modifier.weight(1f)){

            items(items=founders){founder->
                var isSelected by remember {
                    mutableStateOf(false)
                }
                var tc=if(isSelected) Color.Blue else Color.Magenta

                Surface(color = tc){
                    Text(text=founder,
                        modifier= Modifier
                            .padding(20.dp)
                            .fillMaxWidth()
                            .clickable {
                                isSelected = !isSelected

                            }
                    )

                }
            }
        }

    }
}
fun Offset.rotateBy(angle: Float): Offset {
    val angleInRadians = angle * PI / 180
    return Offset(
        (x * cos(angleInRadians) - y * sin(angleInRadians)).toFloat(),
        (x * sin(angleInRadians) + y * cos(angleInRadians)).toFloat()
    )
}