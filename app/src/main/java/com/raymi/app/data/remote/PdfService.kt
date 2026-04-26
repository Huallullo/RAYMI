package com.raymi.app.data.remote

import android.content.Context
import android.os.Environment
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio para generar documentos PDF de alquileres usando iText 7.
 *
 * Requiere en build.gradle.kts:
 *   - isCoreLibraryDesugaringEnabled = true
 *   - coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")
 *   - implementation("com.itextpdf:itext7-core:7.2.6") { ... }
 */
@Singleton
class PdfService @Inject constructor(
    private val context: Context
) {
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val colorInca  = DeviceRgb(139, 30, 63)   // #8B1E3F  (burdeos inca)
    private val colorGold  = DeviceRgb(255, 215, 0)   // #FFD700

    /**
     * Genera un PDF con el detalle completo del alquiler y lo guarda en
     * el directorio externo privado de la app (no requiere permiso WRITE_EXTERNAL_STORAGE).
     *
     * @return [Resource.Success] con el [File] generado, o [Resource.Error] con el mensaje.
     */
    suspend fun generarPdfAlquiler(alquiler: Alquiler): Resource<File> =
        withContext(Dispatchers.IO) {
            try {
                val pdfFile = crearArchivo(alquiler.id)
                buildPdf(pdfFile, alquiler)
                Resource.Success(pdfFile)
            } catch (e: Exception) {
                Resource.Error("Error al generar PDF: ${e.localizedMessage ?: e.message}")
            }
        }

    // ─── Construcción del PDF ────────────────────────────────────────────────

    private fun buildPdf(file: File, alquiler: Alquiler) {
        PdfWriter(file).use { writer ->
            PdfDocument(writer).use { pdf ->
                Document(pdf).use { doc ->
                    doc.setMargins(36f, 36f, 36f, 36f)
                    agregarLogo(doc)
                    agregarTitulo(doc)
                    agregarInfoAlquiler(doc, alquiler)
                    agregarInfoPago(doc, alquiler)
                    if (alquiler.observaciones.isNotBlank()) {
                        agregarObservaciones(doc, alquiler.observaciones)
                    }
                    agregarPieDePagina(doc)
                }
            }
        }
    }

    private fun agregarLogo(doc: Document) {
        try {
            val resId = context.resources.getIdentifier(
                "ic_raymi_logo", "drawable", context.packageName
            )
            if (resId == 0) return

            val bitmap = android.graphics.BitmapFactory.decodeResource(context.resources, resId)
            val stream = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)

            val img = Image(ImageDataFactory.create(stream.toByteArray()))
                .setWidth(70f)
                .setHeight(70f)
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setMarginBottom(8f)
            doc.add(img)
        } catch (_: Exception) {
            // El logo es opcional; si falla, continuar sin él
        }
    }

    private fun agregarTitulo(doc: Document) {
        doc.add(
            Paragraph("RAYMI – Sistema de Alquiler de Vestuarios")
                .setFontSize(10f)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY)
        )
        doc.add(
            Paragraph("DETALLE DE ALQUILER")
                .setFontSize(20f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(colorInca)
                .setMarginBottom(16f)
        )
    }

    private fun agregarInfoAlquiler(doc: Document, a: Alquiler) {
        doc.add(seccionTitulo("INFORMACIÓN DEL ALQUILER"))

        val tabla = tabla2Col()
        tabla.addCell(labelCell("Cliente"))
        tabla.addCell(valorCell(a.clienteNombre))

        tabla.addCell(labelCell("Vestuario"))
        tabla.addCell(valorCell("${a.vestuarioNombre} (${a.vestuarioCodigo})"))

        tabla.addCell(labelCell("Cantidad"))
        tabla.addCell(valorCell("${a.cantidad} unidad(es)"))

        tabla.addCell(labelCell("Fecha de inicio"))
        tabla.addCell(valorCell(dateFormat.format(a.fechaInicio.toDate())))

        tabla.addCell(labelCell("Fecha prevista devolución"))
        tabla.addCell(valorCell(dateFormat.format(a.fechaFinPrevista.toDate())))

        if (a.fechaDevolucion != null) {
            tabla.addCell(labelCell("Fecha real devolución"))
            tabla.addCell(valorCell(dateFormat.format(a.fechaDevolucion!!.toDate())))
        }

        tabla.addCell(labelCell("Estado"))
        tabla.addCell(valorCell(a.estado.name))

        doc.add(tabla)
    }

    private fun agregarInfoPago(doc: Document, a: Alquiler) {
        doc.add(seccionTitulo("INFORMACIÓN DE PAGO").setMarginTop(12f))

        val tabla = tabla2Col()
        tabla.addCell(labelCell("Precio unitario"))
        tabla.addCell(valorCell("S/. ${formato(a.precioUnitario)}"))

        tabla.addCell(labelCell("Cantidad"))
        tabla.addCell(valorCell("× ${a.cantidad}"))

        tabla.addCell(labelCell("Precio total"))
        tabla.addCell(valorCell("S/. ${formato(a.precioTotal)}").setBold())

        tabla.addCell(labelCell("Adelanto recibido"))
        tabla.addCell(valorCell("S/. ${formato(a.adelanto)}"))

        tabla.addCell(labelCell("Saldo pendiente"))
        tabla.addCell(
            valorCell("S/. ${formato(a.saldo)}").apply {
                if (a.saldo > 0) setFontColor(DeviceRgb(214, 40, 40))
                else setFontColor(DeviceRgb(0, 128, 0))
            }
        )

        doc.add(tabla)
    }

    private fun agregarObservaciones(doc: Document, texto: String) {
        doc.add(seccionTitulo("OBSERVACIONES").setMarginTop(12f))
        doc.add(
            Paragraph(texto)
                .setFontSize(11f)
                .setMarginBottom(12f)
        )
    }

    private fun agregarPieDePagina(doc: Document) {
        doc.add(
            Paragraph(
                "Generado el ${dateFormat.format(Date())}  |  " +
                        "RAYMI – Gestión de Vestuarios Folklóricos Peruanos  |  Lima, Perú"
            )
                .setFontSize(9f)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY)
                .setMarginTop(24f)
        )
    }

    // ─── Helpers de estilo ───────────────────────────────────────────────────

    private fun tabla2Col(): Table =
        Table(UnitValue.createPercentArray(floatArrayOf(35f, 65f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(4f)

    private fun seccionTitulo(texto: String): Paragraph =
        Paragraph(texto)
            .setFontSize(13f)
            .setBold()
            .setFontColor(colorInca)
            .setMarginBottom(6f)

    private fun labelCell(texto: String): Cell =
        Cell().add(Paragraph(texto).setBold().setFontSize(10f))
            .setBackgroundColor(DeviceRgb(240, 240, 240))
            .setPadding(6f)

    private fun valorCell(texto: String): Cell =
        Cell().add(Paragraph(texto).setFontSize(10f))
            .setPadding(6f)

    private fun formato(valor: Double): String =
        String.format(Locale.getDefault(), "%.2f", valor)

    // ─── Sistema de archivos ─────────────────────────────────────────────────

    private fun crearArchivo(alquilerId: String): File {
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "RAYMI_PDFs"
        )
        if (!dir.exists()) dir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(dir, "alquiler_${alquilerId}_$timestamp.pdf")
    }
}