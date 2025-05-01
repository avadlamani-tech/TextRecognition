package com.example.textrecognition

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraTextRecognitionScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboardManager : ClipboardManager = LocalClipboardManager.current
    val snackBarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA

        ) == PackageManager.PERMISSION_GRANTED
    ) }

    var flashEnabled by remember { mutableStateOf(false) }
    var recognitionState by remember { mutableStateOf<RecognitionState>(RecognitionState.idle) }
    var isScanning by remember { mutableStateOf(false) }

    val cameraProviderFeature = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val textRecognizer = remember { mutableStateOf(false) }

    val scaffoldState = rememberBottomSheetScaffoldState()
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (! hasCameraPermission) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(Unit) {
       onDispose{
           //To be checked later
           textRecognizer
       }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetContent = {
           Card(
               Modifier
                   .fillMaxWidth()
                   .padding(16.dp),
               shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
           ) {
               Column (
                   Modifier
                       .fillMaxWidth()
                       .padding(16.dp)
                       .verticalScroll(scrollState)
               ){
                   Row (
                       Modifier.fillMaxWidth(),
                       horizontalArrangement = Arrangement.SpaceBetween,
                       verticalAlignment = Alignment.CenterVertically
                   ){
                       Text(
                           text = "Recognize text",
                           style = MaterialTheme.typography.titleLarge
                       )
                       if (recognitionState is RecognitionState.Success) {
                           IconButton(onClick = {
                               val text = (recognitionState as RecognitionState.Success).text
                               clipboardManager.setText(annotatedString = AnnotatedString(text))
                               coroutineScope.launch {
                                   snackBarHostState.showSnackbar("Text copied to clipboard")
                               }
                           }) {
                               Icon(
                                   imageVector = Icons.Default.ContentCopy,
                                   contentDescription = "Copy Text"
                               )
                           }

                       }
                   }
                   Spacer(modifier = Modifier.height(8.dp))

                   when(val state = recognitionState ) {
                       is RecognitionState.idle -> {

                           LinearProgressIndicator(Modifier.fillMaxWidth())
                           Spacer(Modifier.height(8.dp))
                           Text("Processing image...")
                       }
                       is RecognitionState.Success -> {

                           Text(
                               text = state.text,
                               style = MaterialTheme.typography.bodyLarge,
                              modifier =  Modifier.padding(vertical = 8.dp)
                           )
                       }
                       is RecognitionState.error -> {

                           Text(
                               text = state.message,
                               color = MaterialTheme.colorScheme.error,
                               textAlign = TextAlign.Center,
                               modifier = Modifier.fillMaxWidth()
                           )
                       }

                       RecognitionState.loading -> TODO()
                   }
               }

           }
        },

        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Text Scanner"
                    )
                }
            )
        }

    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = {ctx ->
                        val previewView = PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }
                        val executor = Executors.newSingleThreadScheduledExecutor()

                        cameraProviderFeature.addListener({
                            val cameraProvider = cameraProviderFeature.get()

                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            if(isScanning) {
                                imageAnalysis.setAnalyzer(executor,{imageProxy ->
                                    imageProxy.close()
                                })
                            }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture
                                )
                            } catch (e:Exception) {
                                Log.e("Camera text Recognition", "Binding Failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()

                )
                AnimatedVisibility(
                    visible = isScanning,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(250.dp)
                                .border(
                                    width = 2.dp,
                                    color = Color.White,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                    }
                }

                Column (
                    modifier = Modifier
                        .align (Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    Row (
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        IconButton(
                            onClick = {flashEnabled = !flashEnabled},
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                if (!isScanning) {
                                    isScanning = true
                                    recognitionState = RecognitionState.loading

                                    val outputOptions = ImageCapture.OutputFileOptions.Builder(
                                        context.cacheDir
                                    ).build()

                                    imageCapture.takePicture(
                                        outputOptions,
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageSavedCallback{
                                            override fun ImageCapture.OnImageSaved(outputFileResults : ImageCapture.OutputFileResults) {

                                                coroutineScope.launch {
                                                    delay(500)

                                                    scaffoldState.bottomSheetState.expand()

                                                    delay(1000)

                                                    isScanning = false
                                                }

                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                isScanning = false
                                                recognitionState = RecognitionState.error(
                                                    "Fill to capture image: ${exception.message}"
                                                )
                                                coroutineScope.launch {
                                                    snackBarHostState.showSnackbar("Failed to capture image")
                                                }
                                            }
                                        }

                                    )
                                }
                            }
                        ) { }
                    }

                }
            }
        }

    }
}