package com.example.poseapp

import android.content.Context
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL
import androidx.camera.core.ImageCapture
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.size
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions

@Composable
fun CameraPreview (
    controller: LifecycleCameraController,
    onImageAnalyzed: (Pose, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context: Context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    var frameSkipCounter = 0
    AndroidView(
        factory = {context ->
            val options = AccuratePoseDetectorOptions.Builder()
                .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
                .build()
            val poseDetector = PoseDetection.getClient(options)

            PreviewView(context).apply {
//                this.controller = controller
//                controller.bindToLifecycle(lifeCycleOwner)
//                controller.setImageAnalysisAnalyzer(
//                    executor,
//                    MlKitAnalyzer(
//                        listOf(poseDetector),
//                        COORDINATE_SYSTEM_ORIGINAL,
//                        executor
//                    ) {result: MlKitAnalyzer.Result ->
//
//                        if (frameSkipCounter % 30 == 0) {
//                            val pose = result.getValue(poseDetector)
//                            if (pose != null) {
//                                Log.d("Image", "Analyzing")
//                                onImageAnalyzed(pose)
//                            } else {
//                                Log.d("Image", "Null image received")
//                            }
//                        }
//                        frameSkipCounter++;
//                    }
//                )
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_START
            }.also {previewView ->
                startImageRecognition(
                    context = context,
                    cameraController = controller,
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    onImageAnalyzed = onImageAnalyzed
                )
            }
        },
        modifier = modifier
    )
}

private fun startImageRecognition(
    context: Context,
    cameraController: LifecycleCameraController,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onImageAnalyzed: (Pose, Int, Int) -> Unit
) {
    cameraController.setImageAnalysisAnalyzer(
        ContextCompat.getMainExecutor(context),
        PoseAnalyzer(onImageAnalyzed = onImageAnalyzed)
    )

    cameraController.bindToLifecycle(lifecycleOwner)
    previewView.controller = cameraController
}