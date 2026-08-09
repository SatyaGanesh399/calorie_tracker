package com.satya.calorietracker.ui.scanner

import android.annotation.SuppressLint
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * ML Kit barcode analyzer restricted to the symbologies actually printed on food
 * packaging. Narrowing the format list measurably speeds up detection and avoids
 * false positives from QR codes on the same label.
 */
class BarcodeAnalyzer(
    private val onBarcode: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_ITF
            )
            .build()
    )

    @OptIn(ExperimentalGetImage::class)
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.firstNotNullOfOrNull { it.rawValue }
                    ?.takeIf { it.isNotBlank() }
                    ?.let(onBarcode)
            }
            .addOnFailureListener {
                // A failed frame is not an error worth surfacing — the next frame will do.
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    fun release() {
        runCatching { scanner.close() }
    }
}
