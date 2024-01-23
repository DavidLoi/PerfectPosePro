package com.example.poseapp

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.poseapp.ui.theme.PoseAppTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import java.io.IOException
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt


class MainActivity : ComponentActivity() {
    private val positions = listOf(0, 11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28, 33, 34)
    private val start = listOf(0, 33, 33, 11, 12, 13, 14, 33, 34, 34, 23, 24, 25, 26)
    private val end = listOf(33, 11, 12, 13, 14, 15, 16, 34, 23, 24, 25, 26, 27, 28)
    private val angleStart = listOf(0, 0, 33, 33, 11, 12, 0, 33, 33, 34, 34, 23, 24)
    private val angleCenter = listOf(33, 33, 11, 12, 13, 14, 33, 34, 34, 23, 24, 25, 26)
    private val angleEnd = listOf(11, 12, 13, 14, 15, 16, 34, 23, 24, 25, 26, 27, 28)
    private var imageHeight = 0
    private var imageWidth = 0
    private val angleThreshold = 5 * 13f
    private val imageOpacity = 0.5f
    private val imageAngles = ArrayList<Float>()
    private var photoCounter = 10


    @OptIn(ExperimentalGetImage::class) override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, 0)
            Log.d("Camera Permission",
                (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED).toString())
            Log.d("Write Permission",
                (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED).toString())
        }

        setContent {
            PoseAppTheme {
                var imageX by remember { mutableStateOf(arrayListOf<Float>()) }
                var imageY by remember { mutableStateOf(arrayListOf<Float>()) }
                var bodyX by remember { mutableStateOf(arrayListOf<Float>()) }
                var bodyY by remember { mutableStateOf(arrayListOf<Float>()) }
                val goodAngles by remember { mutableStateOf(arrayListOf<Int>()) }
                val controller = remember {
                    LifecycleCameraController(applicationContext).apply {
                        setEnabledUseCases(
                            CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS
                        )
                    }
                }
                controller.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                var selectedImageUri by remember {
                    mutableStateOf<Uri?>(null)
                }
                val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia(),
                    onResult = { uri ->
                        selectedImageUri = uri

                        // Accurate pose detector on static images, when depending on the pose-detection-accurate sdk
                        val options = AccuratePoseDetectorOptions.Builder()
                            .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
                            .build()

                        val poseDetector = PoseDetection.getClient(options)

                        if (selectedImageUri != null) {
                            val image = InputImage.fromFilePath(this, selectedImageUri!!)
                            poseDetector.process(image)
                                .addOnSuccessListener {
                                    Log.d("Success", "Image has been processed.")
                                    Log.d("Size", image.width.toString() + " " + image.height.toString())
                                    imageWidth = image.width
                                    imageHeight = image.height
                                    val newX = ArrayList<Float>()
                                    val newY = ArrayList<Float>()

                                    for (i in 0..32) {
                                        if (it.getPoseLandmark(i) != null) {
                                            Log.d(
                                                "Position", i.toString() + ": " +
                                                        it.getPoseLandmark(i)!!.position.x.toString() + " " +
                                                        it.getPoseLandmark(i)!!.position.y.toString()
                                            )
                                            newX.add(it.getPoseLandmark(i)!!.position.x / image.width)
                                            newY.add(it.getPoseLandmark(i)!!.position.y / image.height)
                                        }
                                    }
                                    if (newX.size > 0) {
                                        // Midpoint of shoulders = 33
                                        newX.add((newX[11] + newX[12]) / 2)
                                        newY.add((newY[11] + newY[12]) / 2)
                                        // Midpoint of hips = 34
                                        newX.add((newX[23] + newX[24]) / 2)
                                        newY.add((newY[23] + newY[24]) / 2)
                                        imageX = newX
                                        imageY = newY
                                        if (imageX.size == 0) {
                                            Toast.makeText(this, "No points found, please change image size", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(this, "Success ", Toast.LENGTH_LONG).show()
                                        }
                                        Log.d("x", imageX.size.toString())
                                        Log.d("y", imageY.size.toString())

                                        // Joint angles
                                        for (i in angleStart.indices) {
                                            var angle = angleThreshold
                                            if (imageX[angleStart[i]] in 0.0..1.0 && imageY[angleStart[i]] in 0.0 .. 1.0 &&
                                                imageX[angleCenter[i]] in 0.0..1.0 && imageY[angleCenter[i]] in 0.0 .. 1.0 &&
                                                imageX[angleEnd[i]] in 0.0..1.0 && imageY[angleEnd[i]] in 0.0 .. 1.0) {
                                                val a = sqrt((imageX[angleStart[i]] - imageX[angleCenter[i]]).pow(2) +
                                                        (imageY[angleStart[i]] - imageY[angleCenter[i]]).pow(2))
                                                val b = sqrt((imageX[angleEnd[i]] - imageX[angleCenter[i]]).pow(2) +
                                                        (imageY[angleEnd[i]] - imageY[angleCenter[i]]).pow(2))
                                                val c = sqrt((imageX[angleStart[i]] - imageX[angleEnd[i]]).pow(2) +
                                                        (imageY[angleStart[i]] - imageY[angleEnd[i]]).pow(2))
                                                angle = (acos((a.pow(2) + b.pow(2) - c.pow(2)) / (2 * a * b)) * 180 / PI).toFloat()
                                            }
                                            imageAngles.add(angle)
                                            Log.d("Angle$i", angle.toString())
                                        }
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this,"Failed",Toast.LENGTH_LONG).show()
                                }
                        } else {
                            Log.d("Image", "Null image received")
                        }
                    }
                )

                Column (
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ){
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .weight(1f),
                        horizontalArrangement = Arrangement.Center
                    ){
                        Text(text = "PerfectPosePro", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }

                    Box(
                        modifier = Modifier
                            .weight(10f)
                    ){
                        CameraPreview(
                            controller = controller,
                            onImageAnalyzed = {pose, width, height ->
                                val newX = ArrayList<Float>()
                                val newY = ArrayList<Float>()
//                                Log.d("Size", width.toString() + " " + height.toString())

                                for (i in 0..32) {
                                    if (pose.getPoseLandmark(i) != null) {
//                                        Log.d("Position",
//                                            i.toString() + ": " + pose.getPoseLandmark(i)!!.position.x.toString() + " " + pose.getPoseLandmark(i)!!.position.y.toString())

                                        // Invert points if front camera
                                        if (controller.cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                                            newX.add(1 - pose.getPoseLandmark(i)!!.position.x / width)
                                        } else {
                                            newX.add(pose.getPoseLandmark(i)!!.position.x / width)
                                        }
                                        newY.add(pose.getPoseLandmark(i)!!.position.y / height)
                                    }
                                }
                                if (newX.size > 0) {
                                    // Midpoint of shoulders = 33
                                    newX.add((newX[11] + newX[12]) / 2)
                                    newY.add((newY[11] + newY[12]) / 2)
                                    // Midpoint of hips = 34
                                    newX.add((newX[23] + newX[24]) / 2)
                                    newY.add((newY[23] + newY[24]) / 2)
                                    bodyX = newX
                                    bodyY = newY

                                    // Joint angles
                                    if (imageAngles.size > 0) {
                                        goodAngles.clear()
                                        val angles = ArrayList<Float>()
                                        var angleDiff = 0f
                                        for (i in angleStart.indices) {
                                            var angle = angleThreshold
                                            if (bodyX[angleStart[i]] in 0.0..1.0 && bodyY[angleStart[i]] in 0.0 .. 1.0 &&
                                                bodyX[angleCenter[i]] in 0.0..1.0 && bodyY[angleCenter[i]] in 0.0 .. 1.0 &&
                                                bodyX[angleEnd[i]] in 0.0..1.0 && bodyY[angleEnd[i]] in 0.0 .. 1.0) {
                                            val a = sqrt((bodyX[angleStart[i]] - bodyX[angleCenter[i]]).pow(2) +
                                                    (bodyY[angleStart[i]] - bodyY[angleCenter[i]]).pow(2))
                                            val b = sqrt((bodyX[angleEnd[i]] - bodyX[angleCenter[i]]).pow(2) +
                                                    (bodyY[angleEnd[i]] - bodyY[angleCenter[i]]).pow(2))
                                            val c = sqrt((bodyX[angleStart[i]] - bodyX[angleEnd[i]]).pow(2) +
                                                    (bodyY[angleStart[i]] - bodyY[angleEnd[i]]).pow(2))
                                                angle = (acos((a.pow(2) + b.pow(2) - c.pow(2)) / (2 * a * b)) * 180 / PI).toFloat()
                                            }
                                            angles.add(angle)
                                        }

                                        // Swap angles if front camera
                                        if (controller.cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                                            val angleSwap = listOf(0, 2, 4, 7, 9, 11)
                                            for (i in angleSwap) {
                                                val swap = angles[i]
                                                angles[i] = angles[i + 1]
                                                angles[i + 1] = swap
                                            }
                                        }

                                        // Get angle difference
                                        for (i in angles.indices) {
                                            angleDiff += abs(angles[i] - imageAngles[i])
                                            if (abs(angles[i] - imageAngles[i]) < angleThreshold / angleStart.size) {
                                                goodAngles.add(angleCenter[i])
                                            }
                                        }

                                        if (angleDiff < angleThreshold && photoCounter % 10 == 0) {
                                            Log.d("Difference", angleDiff.toString())
                                            Log.d("Picture", "Take Photo ")
                                            for (i in angles.indices) {
                                                Log.d("Angle $i", angles[i].toString())
                                            }
                                            takePhoto(controller)
                                            photoCounter = 0
                                        }
                                        photoCounter++
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                        )

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
//                            Log.d("Draw", imageX.size.toString() + " " + imageY.size.toString())
//                            Log.d("Size", this.size.width.toString() + " " + this.size.height.toString())
                            if (imageX.isNotEmpty() && imageY.isNotEmpty()) {
                                val scale = 1
                                val xOffset = 0
                                val yOffset = 0
//                                Log.d("Scale", scale.toString())
                                for (i in positions) {
//                                    Log.d(i.toString(), imageX[i].toString() + " " + imageY[i].toString())
                                    if (imageX[i] in 0.0..1.0 && imageY[i] in 0.0..1.0) {
                                        drawCircle(
                                            color = Color.White,
                                            radius = 10f,
                                            center = Offset(
                                                this.size.width * imageX[i] * scale + xOffset,
                                                this.size.height * imageY[i] * scale + yOffset
                                            )
                                        )
                                    }
                                }
                                for (i in start.indices) {
                                    if (imageX[start[i]] in 0.0..1.0 && imageY[start[i]] in 0.0..1.0 &&
                                        imageX[end[i]] in 0.0..1.0 && imageY[end[i]] in 0.0..1.0) {
                                        drawLine(
                                            color = Color.White,
                                            start = Offset(
                                                this.size.width * imageX[start[i]] * scale + xOffset,
                                                this.size.height * imageY[start[i]] * scale + yOffset
                                            ),
                                            end = Offset(
                                                this.size.width * imageX[end[i]] * scale + xOffset,
                                                this.size.height * imageY[end[i]] * scale + yOffset
                                            ),
                                            strokeWidth = 10f
                                        )
                                    }
                                }
                            }
                        }

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
//                            Log.d("Draw", bodyX.size.toString() + " " + bodyY.size.toString())
//                            Log.d("Size", this.size.width.toString() + " " + this.size.height.toString())
                            if (bodyX.isNotEmpty() && bodyY.isNotEmpty()) {
                                val scale = 1
                                val xOffset = 0
                                val yOffset = 0
//                                Log.d("Scale", scale.toString())
                                for (i in positions) {
//                                    Log.d(i.toString(), bodyX[i].toString() + " " + bodyY[i].toString())
                                    if (bodyX[i] in 0.0..1.0 && bodyY[i] in 0.0..1.0) {
                                        drawCircle(
                                            color = Color.Black,
                                            radius = 10f,
                                            center = Offset(
                                                this.size.width * bodyX[i] * scale + xOffset,
                                                this.size.height * bodyY[i] * scale + yOffset
                                            )
                                        )
                                    }
                                }
                                for (i in start.indices) {
                                    val color = if (start[i] in goodAngles || (i == 0 && 33 in goodAngles)) {
                                        Color.Green
                                    } else {
                                        Color.Red
                                    }
                                    if (bodyX[start[i]] in 0.0..1.0 && bodyY[start[i]] in 0.0..1.0 &&
                                        bodyX[end[i]] in 0.0..1.0 && bodyY[end[i]] in 0.0..1.0) {
                                        drawLine(
                                            color = color,
                                            start = Offset(
                                                this.size.width * bodyX[start[i]] * scale + xOffset,
                                                this.size.height * bodyY[start[i]] * scale + yOffset
                                            ),
                                            end = Offset(
                                                this.size.width * bodyX[end[i]] * scale + xOffset,
                                                this.size.height * bodyY[end[i]] * scale + yOffset
                                            ),
                                            strokeWidth = 10f
                                        )
                                    }
                                }
                            }
                        }

                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize(),
                            alpha = imageOpacity
                        )
                    }


                    Row (
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .weight(1f),
                        horizontalArrangement = Arrangement.SpaceAround
                    ){
                        IconButton(
                            onClick = {
                                controller.cameraSelector =
                                    if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                        CameraSelector.DEFAULT_FRONT_CAMERA
                                    } else {
                                        CameraSelector.DEFAULT_BACK_CAMERA
                                    }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch camera",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = {
                                singlePhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Photo,
                                contentDescription = "Open gallery",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    private fun takePhoto(
        controller: LifecycleCameraController
    ) {
        controller.takePicture(
            ContextCompat.getMainExecutor(applicationContext),
            object: OnImageCapturedCallback() {
                @RequiresApi(Build.VERSION_CODES.Q)
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    val matrix = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                        postScale(-1f, 1f, image.width / 2f, image.height / 2f)
                    }
                    val rotatedBitmap = Bitmap.createBitmap(
                        image.toBitmap(),
                        0,
                        0,
                        image.width,
                        image.height,
                        matrix,
                        true
                    )

                    if (saveBitmapIntoStorage(rotatedBitmap)) {
                        Toast.makeText(applicationContext, "Photo capture succeeded", Toast.LENGTH_SHORT).show()
                    }

                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e("Camera", "Couldn't take photo: ", exception)
                }
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveBitmapIntoStorage(bitmap: Bitmap): Boolean {
        val imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CANADA).format(System.currentTimeMillis()) + ".jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
        }

        return try {
            contentResolver.insert(imageCollection, contentValues)?.also { uri ->
                contentResolver.openOutputStream(uri).use { outputStream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream!!)) {
                        throw IOException("Couldn't save bitmap")
                    }
                }
            } ?: throw IOException("Couldn't create Mediastore entry")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return PERMISSIONS.all {
            ContextCompat.checkSelfPermission(applicationContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private val PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
    }
}