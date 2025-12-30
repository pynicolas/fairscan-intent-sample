# FairScan Intent Sample

A minimal Android application demonstrating how to invoke **[FairScan](https://github.com/pynicolas/FairScan)** using an implicit intent.

This project is meant as a simple reference or testing playground for integrating with FairScan from other apps.

## What it does

- Creates an intent with the action: `org.fairscan.app.action.SCAN_TO_PDF`
- Launches FairScan using [`registerForActivityResult`](https://developer.android.com/training/basics/intents/result)
- Retrieves the PDF returned by FairScan and saves it locally
- Shows a fallback message if FairScan is not installed

Useful to experiment with integration behavior and how Android resolves implicit intents.

## How the intent is invoked

```kotlin
val launcher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    when (result.resultCode) {
        RESULT_OK -> {
            val uri = result.data?.data ?: return@registerForActivityResult
            // Save the content of the PDF pointed by uri
         }
        RESULT_CANCELED -> { /* User cancelled the scan */ }
        else -> {}
    }
}

val scanIntent = Intent("org.fairscan.app.action.SCAN_TO_PDF")
try {
    launcher.launch(scanIntent)
} catch(_: ActivityNotFoundException) {
    // Fairscan not found: show a message to the user
}
```
See the code for [MainActivity](app/src/main/java/org/fairscan/intentsample/MainActivity.kt).

