package com.raymi.app.data.remote

import android.annotation.TargetApi
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio Maestro de Documentación PDF (SaaS).
 * Genera comprobantes legales y reportes financieros automáticos.
 */
@Singleton
class PdfService @Inject constructor(
    private val context: Context
) {
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val primaryColor = DeviceRgb(79, 70, 229) // Indigo Premium

    /**
     * Genera un comprobante oficial de alquiler (Boleta de Gestión).
     */
    suspend fun generarComprobanteAlquiler(alquiler: Alquiler, workspace: Workspace?): Resource<Uri> =
        withContext(Dispatchers.IO) {
            try {
                val pdfUri = crearArchivo("Recibo_${alquiler.itemCodigo}")
                buildAlquilerPdf(pdfUri, alquiler, workspace)
                
                val contentValues = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                context.contentResolver.update(pdfUri, contentValues, null, null)
                Resource.Success(pdfUri)
            } catch (e: Exception) {
                Resource.Error("Falla al generar PDF del contrato")
            }
        }

    /**
     * Genera un Reporte Financiero de Rendimiento para el dueño del negocio.
     */
    suspend fun generarPdfResumenFinanciero(alquileres: List<Alquiler>, year: Int): Resource<Uri> =
        withContext(Dispatchers.IO) {
            try {
                val pdfUri = crearArchivo("Reporte_Anual_$year")
                buildFinancialReportPdf(pdfUri, alquileres, year)
                
                val contentValues = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                context.contentResolver.update(pdfUri, contentValues, null, null)
                Resource.Success(pdfUri)
            } catch (e: Exception) {
                Resource.Error("Falla al generar Reporte Financiero")
            }
        }

    private fun buildAlquilerPdf(uri: Uri, alquiler: Alquiler, workspace: Workspace?) {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            PdfWriter(os).use { writer ->
                PdfDocument(writer).use { pdf ->
                    Document(pdf).use { doc ->
                        doc.setMargins(40f, 40f, 40f, 40f)
                        
                        // 1. Encabezado de Negocio
                        val business = workspace?.nombre ?: "RAYMI GESTIÓN"
                        doc.add(Paragraph(business.uppercase()).setBold().setFontSize(20f).setFontColor(primaryColor))
                        doc.add(Paragraph("COMPROBANTE DE OPERACIÓN").setFontSize(10f).setFontColor(ColorConstants.GRAY))
                        doc.add(Paragraph("\n"))

                        // 2. Datos del Cliente y Fechas
                        val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f))).useAllAvailableWidth()
                        infoTable.addCell(Cell().add(Paragraph("CLIENTE\n").setBold().setFontSize(9f).setFontColor(primaryColor))
                            .add(Paragraph("${alquiler.clienteNombre}\nDNI: ${alquiler.clienteDni}")).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        
                        infoTable.addCell(Cell().add(Paragraph("PERIODO\n").setBold().setFontSize(9f).setFontColor(primaryColor))
                            .add(Paragraph("Emisión: ${dateFormat.format(Date())}\nDevolución: ${alquiler.fechaFinFormatted}")).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        
                        doc.add(infoTable)
                        doc.add(Paragraph("\n"))

                        // 3. Tabla de Productos Alquilados
                        val itemsTable = Table(UnitValue.createPercentArray(floatArrayOf(70f, 30f))).useAllAvailableWidth()
                        itemsTable.addHeaderCell(headerCell("Descripción del Ítem"))
                        itemsTable.addHeaderCell(headerCell("Total").setTextAlignment(TextAlignment.RIGHT))

                        itemsTable.addCell(valorCell("${alquiler.itemNombre} (Cant: ${alquiler.cantidad})"))
                        itemsTable.addCell(valorCell(alquiler.precioFormateado).setTextAlignment(TextAlignment.RIGHT))
                        doc.add(itemsTable)

                        // 4. Resumen de Pagos
                        val totalsTable = Table(UnitValue.createPercentArray(floatArrayOf(70f, 30f))).useAllAvailableWidth()
                        totalsTable.addCell(Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        val resume = Cell().add(Paragraph("\nRESUMEN DE PAGO").setBold().setFontSize(9f).setFontColor(primaryColor))
                            .add(Paragraph("Pagado: ${alquiler.adelantoFormateado}\nSaldo Pendiente: ${alquiler.saldoFormateado}").setBold())
                        totalsTable.addCell(resume.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        doc.add(totalsTable)

                        doc.add(Paragraph("\n\n* Este documento no constituye una factura fiscal SUNAT, es un comprobante de gestión interna de $business.").setFontSize(8f).setItalic())
                    }
                }
            }
        }
    }

    private fun buildFinancialReportPdf(uri: Uri, alquileres: List<Alquiler>, year: Int) {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            PdfWriter(os).use { writer ->
                PdfDocument(writer).use { pdf ->
                    Document(pdf).use { doc ->
                        doc.setMargins(40f, 40f, 40f, 40f)
                        doc.add(Paragraph("REPORTE FINANCIERO ANUAL - $year").setBold().setFontSize(22f).setFontColor(primaryColor))
                        doc.add(Paragraph("Consolidado de ingresos y operaciones por mes.\n\n").setFontSize(10f))

                        val table = Table(UnitValue.createPercentArray(floatArrayOf(40f, 30f, 30f))).useAllAvailableWidth()
                        table.addHeaderCell(headerCell("Mes"))
                        table.addHeaderCell(headerCell("Operaciones"))
                        table.addHeaderCell(headerCell("Ingresos (S/.)"))

                        val months = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
                        var totalAnual = 0.0

                        months.forEachIndexed { i, name ->
                            val mesAlq = alquileres.filter { 
                                val cal = java.util.Calendar.getInstance().apply { time = it.createdAt.toDate() }
                                cal.get(java.util.Calendar.MONTH) == i && cal.get(java.util.Calendar.YEAR) == year
                            }
                            val subtotal = mesAlq.sumOf { it.precioTotal }
                            totalAnual += subtotal
                            table.addCell(valorCell(name))
                            table.addCell(valorCell(mesAlq.size.toString()))
                            table.addCell(valorCell(String.format("%.2f", subtotal)))
                        }
                        doc.add(table)
                        doc.add(Paragraph("\nTOTAL RECAUDADO EN $year: S/. ${String.format("%.2f", totalAnual)}").setBold().setFontSize(14f).setFontColor(primaryColor))
                    }
                }
            }
        }
    }

    private fun headerCell(text: String) = Cell().add(Paragraph(text).setBold().setFontSize(10f).setFontColor(ColorConstants.WHITE)).setBackgroundColor(primaryColor).setPadding(8f)
    private fun valorCell(text: String) = Cell().add(Paragraph(text).setFontSize(10f)).setPadding(8f)

    @TargetApi(29)
    private fun crearArchivo(prefijo: String): Uri {
        val name = "${prefijo}_${System.currentTimeMillis()}.pdf"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        return context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: throw Exception()
    }

    // Compatibilidad
    suspend fun generarPdfAlquiler(alquiler: Alquiler): Resource<Uri> = generarComprobanteAlquiler(alquiler, null)

    /**
     * Genera un listado de inventario en PDF.
     */
    suspend fun generarPdfInventario(items: List<com.raymi.app.domain.model.Item>, negocioNombre: String): Resource<Uri> =
        withContext(Dispatchers.IO) {
            try {
                val pdfUri = crearArchivo("Inventario_${negocioNombre.replace(" ", "_")}")
                buildInventoryPdf(pdfUri, items, negocioNombre)
                
                val contentValues = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                context.contentResolver.update(pdfUri, contentValues, null, null)
                Resource.Success(pdfUri)
            } catch (e: Exception) {
                Resource.Error("Falla al generar PDF de Inventario")
            }
        }

    private fun buildInventoryPdf(uri: Uri, items: List<com.raymi.app.domain.model.Item>, business: String) {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            PdfWriter(os).use { writer ->
                PdfDocument(writer).use { pdf ->
                    Document(pdf).use { doc ->
                        doc.setMargins(40f, 40f, 40f, 40f)
                        doc.add(Paragraph(business.uppercase()).setBold().setFontSize(18f).setFontColor(primaryColor))
                        doc.add(Paragraph("REPORTE DE INVENTARIO").setBold().setFontSize(12f))
                        doc.add(Paragraph("Generado el: ${dateFormat.format(Date())}\n\n").setFontSize(9f))

                        val table = Table(UnitValue.createPercentArray(floatArrayOf(15f, 45f, 20f, 20f))).useAllAvailableWidth()
                        table.addHeaderCell(headerCell("Código"))
                        table.addHeaderCell(headerCell("Nombre"))
                        table.addHeaderCell(headerCell("Precio"))
                        table.addHeaderCell(headerCell("Estado"))

                        items.forEach { item ->
                            table.addCell(valorCell(item.codigo))
                            table.addCell(valorCell(item.nombre))
                            table.addCell(valorCell("S/. ${item.precio}"))
                            table.addCell(valorCell(item.estado))
                        }
                        doc.add(table)
                        doc.add(Paragraph("\nTotal de ítems: ${items.size}").setFontSize(10f).setBold())
                    }
                }
            }
        }
    }
}
