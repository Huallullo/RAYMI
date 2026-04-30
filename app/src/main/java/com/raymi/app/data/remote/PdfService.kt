package com.raymi.app.data.remote

import android.annotation.TargetApi
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
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
     * el directorio Downloads público del dispositivo.
     *
     * @return [Resource.Success] con el [Uri] generado, o [Resource.Error] con el mensaje.
     */
    suspend fun generarPdfAlquiler(alquiler: Alquiler): Resource<Uri> =
        withContext(Dispatchers.IO) {
            try {
                val pdfUri = crearArchivo("alquiler_${alquiler.id}")
                buildPdf(pdfUri, alquiler)
                // Marcar el archivo como completado
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.IS_PENDING, 0)
                }
                context.contentResolver.update(pdfUri, contentValues, null, null)
                Resource.Success(pdfUri)
            } catch (e: Exception) {
                Resource.Error("Error al generar PDF: ${e.localizedMessage ?: e.message}")
            }
        }
    suspend fun generarPdfResumenFinanciero(
        alquileres: List<Alquiler>,
        year: Int
    ): Resource<Uri> = withContext(Dispatchers.IO) {
        try {
            val pdfUri = crearArchivo("resumen_financiero_$year")
            buildPdfResumenFinanciero(pdfUri, alquileres, year)
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            context.contentResolver.update(pdfUri, contentValues, null, null)
            Resource.Success(pdfUri)
        } catch (e: Exception) {
            Resource.Error("Error al generar PDF financiero: ${e.localizedMessage ?: e.message}")
        }
    }

    // ─── Construcción del PDF ────────────────────────────────────────────────

    private fun buildPdf(uri: Uri, alquiler: Alquiler) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            PdfWriter(outputStream).use { writer ->
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
    private fun buildPdfResumenFinanciero(uri: Uri, alquileres: List<Alquiler>, year: Int) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            PdfWriter(outputStream).use { writer ->
                PdfDocument(writer).use { pdf ->
                    Document(pdf).use { doc ->
                        doc.setMargins(36f, 36f, 36f, 36f)
                        agregarLogo(doc)
                        doc.add(
                            Paragraph("RESUMEN FINANCIERO ANUAL - $year")
                                .setFontSize(18f)
                                .setBold()
                                .setTextAlignment(TextAlignment.CENTER)
                                .setFontColor(colorInca)
                                .setMarginBottom(16f)
                        )

                        val months = listOf(
                            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
                        )

                        val tabla = Table(UnitValue.createPercentArray(floatArrayOf(45f, 25f, 30f)))
                            .setWidth(UnitValue.createPercentValue(100f))
                        tabla.addHeaderCell(labelCell("Mes"))
                        tabla.addHeaderCell(labelCell("Alquileres"))
                        tabla.addHeaderCell(labelCell("Ingresos (S/.)"))

                        var totalAnual = 0.0
                        months.forEachIndexed { monthIndex, monthName ->
                            val delMes = alquileres.filter {
                                val cal = java.util.Calendar.getInstance().apply { time = it.createdAt.toDate() }
                                cal.get(java.util.Calendar.YEAR) == year && cal.get(java.util.Calendar.MONTH) == monthIndex
                            }
                            val totalMes = delMes.sumOf { it.precioTotal }
                            totalAnual += totalMes

                            tabla.addCell(valorCell(monthName))
                            tabla.addCell(valorCell(delMes.size.toString()))
                            tabla.addCell(valorCell(formato(totalMes)))
                        }

                        doc.add(tabla)
                        doc.add(
                            Paragraph("TOTAL ANUAL: S/. ${formato(totalAnual)}")
                                .setBold()
                                .setFontSize(13f)
                                .setMarginTop(12f)
                        )
                        agregarPieDePagina(doc)
                    }
                }
            }
        }
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

    @TargetApi(29)
    private fun crearArchivo(prefijo: String): Uri {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${prefijo}_$timestamp.pdf"

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw Exception("No se pudo crear el archivo PDF")

        return uri
    }
}
