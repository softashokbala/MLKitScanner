package com.ashokbala.android.mlkitcamera

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.ashokbala.android.mlkitcamera.ui.theme.MLKitCameraTheme
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    var filepath:File?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            //.setPageLimit(5)
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .setScannerMode(SCANNER_MODE_FULL)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)

        setContent {
            MLKitCameraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
                    var showButton by remember { mutableStateOf(false) }
                    val context = LocalContext.current

                    val scannerLauncher = rememberLauncherForActivityResult(
                        contract =ActivityResultContracts.StartIntentSenderForResult(),
                        onResult = { result ->
                            if (result.resultCode == RESULT_OK) {
                                val resultData :GmsDocumentScanningResult? = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                                //alternative
                                imageUris = resultData?.pages?.map { it.imageUri } ?: emptyList()
                                resultData?.pdf?.let { pdf ->
                                    val fileName = "scan_${System.currentTimeMillis()}.pdf"
                                    val file = File(filesDir, fileName)
                                    val fos = FileOutputStream(file)
                                    contentResolver.openInputStream(pdf.uri)?.use { it.copyTo(fos) }
                                    filepath=file
                                    showButton=true

                                }
                            }
                            if (result.resultCode == RESULT_CANCELED) {
                                Toast.makeText(this@MainActivity,"User Close the Scanner",Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            //.verticalScroll(rememberScrollState())
                        ,
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ){
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .horizontalScroll(rememberScrollState()),
                            Arrangement.Center,Alignment.CenterVertically,
                            ){
                            imageUris.forEach{ uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillBounds,
                                    modifier = Modifier.size(100.dp).padding(4.dp)
                                )
                            }

                        }

                        Button(onClick = {
                            scanner.getStartScanIntent(this@MainActivity)
                                .addOnSuccessListener { intentSender ->
                                    scannerLauncher.launch(
                                        IntentSenderRequest.Builder(intentSender).build()
                                    )
                                }
                                .addOnFailureListener {
                                    Log.e("Error", it.stackTrace.toString())
                                    Toast.makeText(this@MainActivity,it.message,Toast.LENGTH_SHORT).show()

                                }
                        }) {

                            Text("Document File", style = TextStyle(fontSize = 16.sp))
                        }

                        Button(onClick = {
                            if(showButton){
                                val fileUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    filepath!!
                                )
                            // Create an intent to share the file via WhatsApp
                            val shareIntent = Intent(Intent.ACTION_SEND)
                            shareIntent.type = "application/pdf"
                            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
                            shareIntent.setPackage("com.whatsapp") // Specify WhatsApp package
                            // Launch the intent
                            startActivity(Intent.createChooser(shareIntent, "Share PDF via"))
                            }
                        }) {
                            Text("Share Scanner", style = TextStyle(fontSize = 16.sp))
                        }


                    }
                   // Greeting("Android")

                }
            }
        }
    }
}

//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    MLKitCameraTheme {
//        Greeting("Android")
//    }
//}