package com.yash.gify.ui.compose

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import com.yash.gify.domain.DataState

@Composable
fun Gif(
    imageLoader: ImageLoader,
    gifUri: Uri?,
    discardGif:()->Unit,
    onSaveGif:()->Unit,
    resetToOriginal:()->Unit,
    isResizedGif:Boolean,
    currentGifSize:Int,
    adjustedBytes:Int,
    updateAdjustedBytes:(Int)->Unit,
    sizePercentage:Int,
    updateSizePercentage:(Int)->Unit,
    resizeGif:()->Unit,
    gifResizingLoadingState: DataState.Loading.LoadingState,
    loadingState: DataState.Loading.LoadingState

){
    StandardLoadingUI(loadingState = loadingState)
    ResizingGifLoadingUI(gifResizingLoadingState = gifResizingLoadingState)
    if(gifUri!=null){
        Column(
            modifier = Modifier
                .fillMaxSize()
        ){
            val configuration= LocalConfiguration.current

            Row(
                modifier= Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ){
                Button(
                    onClick={
                            discardGif()
                    },
                    colors =
                    ButtonDefaults.elevatedButtonColors(containerColor = Color.Red)
                ){
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Close button"
                    ,modifier=Modifier.size(ButtonDefaults.IconSize))
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick=onSaveGif,
                    colors =
                    ButtonDefaults.elevatedButtonColors(containerColor = Color.Green)
                ){
                    Icon(imageVector = Icons.Filled.Check, contentDescription = "Save button"
                        ,modifier=Modifier.size(ButtonDefaults.IconSize))
                }

            }

            var image= rememberAsyncImagePainter(model = gifUri,imageLoader)

            Image(
                modifier= Modifier
                    .fillMaxWidth()
                    .height((configuration.screenHeightDp * 0.6).dp),
                contentScale = ContentScale.Crop,
                painter=image,
                contentDescription = "Gif created"
            )
            GifFooter(
                adjustedBytes=adjustedBytes,
                updateAdjustedBytes=updateAdjustedBytes,
                sizePercentage=sizePercentage,
                updateSizePercentage=updateSizePercentage,
                gifSize=currentGifSize,
                isResizedGif=isResizedGif,
                resetResizing=resetToOriginal,
                resizeGif=resizeGif,

            ){

            }
        }
    }

}

@Composable
fun GifFooter(
    adjustedBytes: Int,
    updateAdjustedBytes: (Int) -> Unit,
    sizePercentage: Int,
    updateSizePercentage: (Int) -> Unit,
    gifSize: Int,
    isResizedGif: Boolean,
    resetResizing: () -> Unit,
    resizeGif: () -> Unit,
    function: () -> Unit) {

    Column(
        modifier= Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ){
        Text(modifier = Modifier.align(Alignment.End),
            style = MaterialTheme.typography.headlineSmall,
            text = "Approximate Gif Size"
        )
        Text(modifier = Modifier.align(Alignment.End),
            style = MaterialTheme.typography.bodyMedium,
            text = "${adjustedBytes/1024} KB"
        )

        if(isResizedGif){
            Button(modifier = Modifier.align(Alignment.End), onClick = resetResizing) {
                Text(text="$sizePercentage % ")

            }
        }
        else{
            Text(text = "$sizePercentage %")

            var sliderPosition by remember{ mutableStateOf(100f)}
            Slider(
                value=sliderPosition,
                valueRange = 1f..100f,
                onValueChange = {
                    sliderPosition=it
                    updateSizePercentage(sliderPosition.toInt())
                    updateAdjustedBytes(gifSize*sliderPosition.toInt()/100)
                }
            )

            Button(modifier = Modifier.align(Alignment.End),
                onClick = {
                    resizeGif()
                }
            ){
                Text(text = "Resize")
            }
        }

    }

}
