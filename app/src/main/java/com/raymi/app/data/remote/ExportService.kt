package com.raymi.app.data.remote

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.raymi.app.domain.model.Alquiler
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportService @Inject constructor(
    private val context: Context
) {
    fun generarCsvAlquileres(alquileres: List<Alquiler>): Uri? {
        val filename = "Raymi_Alquileres_${System.currentTimeMillis()}.csv"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            // Fallback for older versions: use Files collection
            MediaStore.Files.getContentUri("external")
        }

        val uri = context.contentResolver.insert(collection, contentValues) ?: return null

        try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    writer.write("Fecha,Cliente,DNI,Item,Cantidad,Total,Adelanto,Saldo,Estado\n")
                    alquileres.forEach { alq ->
                        val row = "${alq.fechaInicioFormatted},\"${alq.clienteNombre}\",${alq.clienteDni},\"${alq.itemNombre}\",${alq.cantidad},${alq.precioTotal},${alq.adelanto},${alq.saldo},${alq.estado}\n"
                        writer.write(row)
                    }
                    writer.flush()
                }
            }
            return uri
        } catch (_: Exception) {
            return null
        }
    }
}
