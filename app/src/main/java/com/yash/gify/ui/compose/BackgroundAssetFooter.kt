package com.yash.gify.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.unit.dp

@Composable
fun BackgroundAssetFooter(
    modifier: Modifier,
    isRecording:Boolean,
    launchImagePicker:()->Unit
){

    if(!isRecording){
        Column(
            modifier=modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 5.dp)
        ) {
            Button(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(vertical = 5.dp),
                onClick = {
                    launchImagePicker()
                }
            ){
                Text("Change Background")
            }

        }
    }

}

