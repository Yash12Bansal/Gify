package com.yash.gify.ui.compose

import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontVariation.width
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yash.gify.domain.DataState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip

@Composable
fun StandardLoadingUI(
    loadingState:DataState.Loading.LoadingState
){
    when(loadingState){
        is DataState.Loading.LoadingState.Active->{
            Box(

                modifier= Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .zIndex(3f)
            ){

                CircularProgressIndicator(
                    modifier = Modifier
                        .width(50.dp)
                        .height(50.dp),
                    color=Color.Blue,
                    strokeWidth = 4.dp,
                )
            }
        }
        else -> {}
    }

}

@Composable
fun ResizingGifLoadingUI(
    gifResizingLoadingState: DataState.Loading.LoadingState
){
    if(gifResizingLoadingState is DataState.Loading.LoadingState.Active && gifResizingLoadingState.progress!=null){
        Box(
            modifier = Modifier
                .fillMaxSize(
                )
                .background(Color.Black)
        ){
            Column(modifier= Modifier
                .wrapContentSize()
                .align(Alignment.Center)) {
                Text(modifier = Modifier
                    .align(Alignment.Start)
                    .padding(vertical = 12.dp),
                text="Resizing...",
                style = MaterialTheme.typography.headlineMedium,
                color=Color.White)
                LinearProgressIndicator(modifier=Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(Color.White)
                    .clip(RoundedCornerShape(16.dp)),
                    progress = gifResizingLoadingState.progress,
                    color = Color.Green,

                )
            }
        }
    }
}