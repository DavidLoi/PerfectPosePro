package com.example.poseapp

import android.content.Context
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.pose.Pose

@Composable
fun CameraPreview (
    controller: LifecycleCameraController,
    onImageAnalyzed: (Pose, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = {context ->
            PreviewView(context).apply {
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