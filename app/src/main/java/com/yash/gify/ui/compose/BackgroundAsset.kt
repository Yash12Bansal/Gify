package com.yash.gify.ui.compose

import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import com.yash.gify.R
import androidx.compose.ui.focus.FocusRequester.Companion.createRefs
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.modifierElementOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout
import coil.compose.rememberAsyncImagePainter
import com.yash.gify.MainState
import com.yash.gify.domain.DataState
import com.yash.gify.rotateBy

@Composable
fun BackgroundAsset(
    backgroundAssetUri: Uri,
    updateCapturingViewBounds:(androidx.compose.ui.geometry.Rect)->Unit,
    bitmapCaptureLoadingState: DataState.Loading.LoadingState,
    startBitmapCaptureJob:()->Unit,
    stopBitmapCaptureJob:()->Unit,
    loadingState:DataState.Loading.LoadingState,

    launchImagePicker:()->Unit
){
    ConstraintLayout(modifier = Modifier.fillMaxSize()){

        var (topBar,assetContainer,bottomBar) = createRefs()

        var topBarHeight=remember{56+16}
//        var isRecording by remember{ mutableStateOf(false)}

        RecordActionBar(
            modifier= Modifier
                .fillMaxWidth()
                .height(topBarHeight.dp)
                .constrainAs(topBar) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .zIndex(2f)
                .background(Color.Black),

            bitmapCaptureLoadingState = bitmapCaptureLoadingState,
            startBitmapCaptureJob=startBitmapCaptureJob,
            stopBitmapCaptureJob=stopBitmapCaptureJob

        )
        var configuration= LocalConfiguration.current
        var assetContainerHeight= remember {
            (configuration.screenHeightDp*0.7).toInt()
        }
        RenderBackground(
            modifier= Modifier
                .constrainAs(assetContainer) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(topBar.bottom)
                }
                .zIndex(1f),
            updateCapturingViewBounds=updateCapturingViewBounds,
//            capturedBitmap=capturedBitmap,
            backgroundAssetUri=backgroundAssetUri,
            assetContainerHeight =assetContainerHeight
        )
        StandardLoadingUI(loadingState = loadingState)
        var bottomBarHeight=remember{
            configuration.screenHeightDp-topBarHeight-assetContainerHeight
        }
        BackgroundAssetFooter(
            modifier= Modifier
                .fillMaxWidth()
                .height(bottomBarHeight.dp)
                .zIndex(2f)
                .constrainAs(bottomBar) {
                    top.linkTo(assetContainer.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                }
                .background(Color.White),
            isRecording=bitmapCaptureLoadingState is DataState.Loading.LoadingState.Active,
            launchImagePicker={
                launchImagePicker()
            }
        )




    }


}
@Composable
fun RenderBackground(
    modifier:Modifier,
    assetContainerHeight:Int,
    backgroundAssetUri: Uri ,
//    capturedBitmap: Bitmap?,
    updateCapturingViewBounds:(androidx.compose.ui.geometry.Rect)->Unit,
    ){
    Box(modifier=modifier.wrapContentSize()){
        var background= rememberAsyncImagePainter(model = backgroundAssetUri)
//        else{
//            rememberAsyncImagePainter(model = backgroundAssetUri)
//        }
        Image(modifier = Modifier
            .fillMaxWidth()
            .height(assetContainerHeight.dp)
            .onGloballyPositioned {
                updateCapturingViewBounds(it.boundsInRoot())
            }
            , painter = background, contentDescription = "",
            contentScale = ContentScale.Crop
        )
        RenderAsset(assetContainerHeight=assetContainerHeight)
    }

}
@Composable
fun RenderAsset(assetContainerHeight: Int){
    var offset by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableStateOf(1f) }
    var angle by remember { mutableStateOf(0f) }
    var asset= painterResource(id= com.yash.gify.R.drawable.deal_with_it_sunglasses_default)
    Box(
        modifier= Modifier
            .fillMaxWidth()
            .height(assetContainerHeight.dp)
    ){
        Image(
            modifier = Modifier
                .graphicsLayer {
                    var rotatedOffset = offset.rotateBy(angle)
                    translationX = -rotatedOffset.x
                    translationY = -rotatedOffset.y
                    scaleX = zoom
                    scaleY = zoom
                    rotationZ = angle
                    transformOrigin = TransformOrigin(0f, 0f)
                }

                .pointerInput(Unit) {
                    detectTransformGestures(
                        onGesture = { centroid, pan, gestureZoom, gestureRotate ->
                            val oldScale = zoom
                            val newScale = zoom * gestureZoom

                            // For natural zooming and rotating, the centroid of the gesture should
                            // be the fixed point where zooming and rotating occurs.
                            // We compute where the centroid was (in the pre-transformed coordinate
                            // space), and then compute where it will be after this delta.
                            // We then compute what the new offset should be to keep the centroid
                            // visually stationary for rotating and zooming, and also apply the pan.
                            offset = (offset - centroid * oldScale).rotateBy(-gestureRotate) +
                                    (centroid * newScale - pan * oldScale)
                            zoom = newScale
                            angle += gestureRotate
                        }
                    )
                }
                .size(150.dp, 150.dp),
            painter = asset,
            contentDescription = ""
            )

    }

}

fun Offset.rotateBy(angle: Float): Offset {
    val angleInRadians = angle * Math.PI / 180
    return Offset(
        (x * Math.cos(angleInRadians) - y * Math.sin(angleInRadians)).toFloat(),
        (x * Math.sin(angleInRadians) + y * Math.cos(angleInRadians)).toFloat()
    )
}