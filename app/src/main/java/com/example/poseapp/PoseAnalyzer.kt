package com.example.poseapp

import android.media.Image
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PoseAnalyzer (
    private val onImageAnalyzed: (Pose, Int, Int) -> Unit
): ImageAnalysis.Analyzer {
    private val options = AccuratePoseDetectorOptions.Builder()
        .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
        .build()
    private val poseDetector = PoseDetection.getClient(options)

    private val THROTTLE_TIMEOUT_MS = 1_00L
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        scope.launch {
            val mediaImage: Image = imageProxy.image ?: run { imageProxy.close(); return@launch }
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            val width = if (rotationDegrees == 90 || rotationDegrees == 270) {
                imageProxy.height
            } else {
                imageProxy.width
            }
            val height = if (rotationDegrees == 90 || rotationDegrees == 270) {
                imageProxy.width
            } else {
                imageProxy.height
            }

            val inputImage: InputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)


            suspendCoroutine { continuation ->
                poseDetector.process(inputImage)
                    .addOnSuccessListener { pose: Pose ->
                        onImageAnalyzed(pose, width, height)
                    }.addOnCompleteListener {
                        continuation.resume(Unit)
                    }
            }

            delay(THROTTLE_TIMEOUT_MS)
        }.invokeOnCompletion {exception ->
            exception?.printStackTrace()
            imageProxy.close()
        }
    }
}