package com.raymi.app.domain.usecase.pdf

import android.content.Context
import android.content.Intent
import android.net.Uri
import javax.inject.Inject

class SharePdfUseCase @Inject constructor(
    private val context: Context
) {
    operator fun invoke(uri: Uri, title: String = "Comprobante RAYMI") {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Te comparto el comprobante de alquiler generado por RAYMI.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = Intent.createChooser(shareIntent, "Enviar comprobante por:")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
