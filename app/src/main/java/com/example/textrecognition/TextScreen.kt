package com.example.textrecognition

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraTextRecognitionScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboardManager : ClipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
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
        onDispose {
           textRecognizer.close()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetContent = {
           Card(
               Modifier.fillMaxWidth()
                   .padding(16.dp),
               shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
           ) {

           }
        }


    ) {}
}