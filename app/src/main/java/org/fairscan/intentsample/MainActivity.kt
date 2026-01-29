package org.fairscan.intentsample

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private var status = mutableStateOf("Idle")
    private var pdfList = mutableStateOf<List<File>>(emptyList())
    private var showMissingAppDialog = mutableStateOf(false)
    private var isFairScanAvailable = mutableStateOf(false)

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            RESULT_OK -> {
                val uri = result.data?.data ?: run {
                    status.value = "No PDF URI returned"
                    return@registerForActivityResult
                }
                savePdf(uri)
                status.value = "SUCCESS"
                loadPdfList()
            }
            RESULT_CANCELED -> status.value = "CANCELLED"
            else -> status.value = "UNKNOWN RESULT"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadPdfList()
        isFairScanAvailable.value = checkFairScanAvailability()

        setContent {
            Scaffold { innerPadding ->
                MainScreen(
                    status = status.value,
                    pdfs = pdfList.value,
                    fairScanAvailable = isFairScanAvailable.value,
                    showDialog = showMissingAppDialog.value,
                    onDismissDialog = { showMissingAppDialog.value = false },
                    onInvoke = {
                        val scanIntent = Intent("org.fairscan.app.action.SCAN_TO_PDF")
                        try {
                            launcher.launch(scanIntent)
                        } catch(_: ActivityNotFoundException) {
                            showMissingAppDialog.value = true
                            status.value = "FairScan not installed"
                        }
                    },
                    onOpenPdf = { file -> openPdf(file) },
                    onDeleteAll = { deleteAllPdfs() },
                    modifier = Modifier.padding(innerPadding).padding(16.dp)
                )
            }
        }
    }

    private fun savePdf(uri: Uri) {
        val dir = File(filesDir, "pdfs").apply { mkdirs() }
        val file = File(dir, "scan_${System.currentTimeMillis()}.pdf")
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
    }

    private fun loadPdfList() {
        val dir = File(filesDir, "pdfs")
        pdfList.value = dir.listFiles()?.toList() ?: emptyList()
    }

    private fun openPdf(file: File) {
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(intent)
    }

    private fun deleteAllPdfs() {
        val dir = File(filesDir, "pdfs")
        dir.listFiles()?.forEach { it.delete() }
        loadPdfList()
        status.value = "All PDFs deleted"
    }

    // On Android 11+, querying available activities requires declaring the
    // intent in the <queries> section of the manifest.
    private fun checkFairScanAvailability(): Boolean {
        val intent = Intent("org.fairscan.app.action.SCAN_TO_PDF")
        return intent.resolveActivity(packageManager) != null
    }
}

@Composable
fun MainScreen(
    status: String,
    pdfs: List<File>,
    fairScanAvailable: Boolean,
    showDialog: Boolean,
    onDismissDialog: () -> Unit,
    onInvoke: () -> Unit,
    onOpenPdf: (File) -> Unit,
    onDeleteAll: () -> Unit,
    modifier: Modifier,
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismissDialog,
            title = { Text("FairScan not installed") },
            text = { Text("This sample app requires FairScan to scan documents.") },
            confirmButton = {
                Button(onClick = onDismissDialog) {
                    Text("OK")
                }
            }
        )
    }

    Column(modifier) {
        if (fairScanAvailable) {
            Button(onClick = onInvoke) { Text("Invoke FairScan") }
        } else {
            Text(
                text = "FairScan is not installed on this device.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text("Last result: $status", Modifier.padding(vertical = 8.dp))

        Spacer(Modifier.height(8.dp))
        Text("Saved PDFs:")

        pdfs.forEach { file ->
            Text(
                text = file.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPdf(file) }
                    .padding(4.dp)
            )
        }

        Spacer(Modifier.height(12.dp))
        Button(onClick = onDeleteAll) { Text("Delete All PDFs") }
    }
}
