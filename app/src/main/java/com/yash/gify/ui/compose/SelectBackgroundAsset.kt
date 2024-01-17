package com.yash.gify.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun SelectBackgroundAsset(
    launchImagePicker:()->Unit

){
    Box(
        modifier= Modifier.fillMaxSize()
    ){
        Button(
            onClick={
                launchImagePicker()
            },
            modifier=Modifier.align(Alignment.Center)
        ){
            Text(text="Select Image From Gallery")
        }
    }

}