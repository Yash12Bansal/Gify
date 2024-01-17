package com.yash.gify.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.unit.dp
import com.yash.gify.domain.DataState
import com.yash.gify.domain.DataState.Loading
import com.yash.gify.domain.DataState.Loading.*

@Composable
fun RecordActionBar(
    modifier:Modifier,
    bitmapCaptureLoadingState: LoadingState,
    startBitmapCaptureJob:()->Unit,
    stopBitmapCaptureJob:()->Unit,
){

    Row(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier
            .weight(3f)
            .border(2.dp, Color.Red)
            .height(50.dp)
            .background(Color.Transparent)
        ){
            when(bitmapCaptureLoadingState){
                is LoadingState.Active->{
                    LinearProgressIndicator(
                        modifier=Modifier
                            .border(width=1.dp,color=Color.Blue, shape = RoundedCornerShape(4.dp))
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(40.dp)
                            .padding(end = 10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        progress = bitmapCaptureLoadingState.progress?:0f,
//                        backgroundColor=Color.White,
//                        color=MaterialTheme.colorScheme.primary
                    )
                }
                else -> {}
            }
        }
        RecordButton(
            modifier=Modifier.weight(1f),
            bitmapCaptureLoadingState=bitmapCaptureLoadingState,
            startBitmapCaptureJob=startBitmapCaptureJob,
            stopBitmapCaptureJob=stopBitmapCaptureJob
        )
    }

}

@Composable
fun RecordButton(
    modifier: Modifier,
    bitmapCaptureLoadingState: LoadingState,
    startBitmapCaptureJob:()->Unit,
    stopBitmapCaptureJob:()->Unit,
){

    var isRecording= when(bitmapCaptureLoadingState){
        is LoadingState.Active->true
        LoadingState.Idle->false
    }
    Button(
        modifier=modifier.wrapContentSize(),
        colors = if(isRecording){
            ButtonDefaults.elevatedButtonColors(containerColor = Color.Blue)
        }
    else{
            ButtonDefaults.elevatedButtonColors(containerColor = Color.Blue)

        },
        onClick ={
            if(!isRecording){
                startBitmapCaptureJob()
            }
            else{
                stopBitmapCaptureJob()
            }
        }
    ){
        Text(text=if(isRecording){
            "STOP"
        }
        else{
            "Record"
    })

    }


}