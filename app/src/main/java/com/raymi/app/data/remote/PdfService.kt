package com.raymi.app.data.remote
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
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
import com.raymi.app.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val primaryColor = DeviceRgb(63, 81, 181)

    suspend fun generarComprobanteAlquiler(alquiler: Alquiler, workspace: Workspace?): Resource<Uri> =
        withContext(Dispatchers.IO) {
            try {
                val pdfUri = crearArchivo("Recibo_${alquiler.itemCodigo}")
                buildAlquilerPdf(pdfUri, alquiler, workspace)
                finalizarArchivo(pdfUri)
                Resource.Success(pdfUri)
            } catch (e: Exception) {
                Resource.Error("Falla al generar PDF del contrato: ${e.message}")
            }
        }

    private fun finalizarArchivo(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            context.contentResolver.update(uri, contentValues, null, null)
        }
    }

    private fun buildAlquilerPdf(uri: Uri, alquiler: Alquiler, workspace: Workspace?) {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            PdfWriter(os).use { writer ->
                PdfDocument(writer).use { pdf ->
                    Document(pdf).use { doc ->
                        doc.add(Paragraph("CONTRATO DE ALQUILER").setBold().setFontSize(20f).setFontColor(primaryColor))
                        doc.add(Paragraph("Negocio: ${workspace?.nombre ?: "RAYMI"}"))
                        doc.add(Paragraph("Fecha Emisión: ${dateFormat.format(Date())}"))
                        doc.add(Paragraph("\nDATOS DEL CLIENTE"))
                        doc.add(Paragraph("Nombre: ${alquiler.clienteNombre}"))
                        doc.add(Paragraph("DNI: ${alquiler.clienteDni}"))
                        doc.add(Paragraph("\nDETALLE DEL ALQUILER"))
                        doc.add(Paragraph("Producto: ${alquiler.itemNombre}"))
                        doc.add(Paragraph("Precio Total: S/. ${alquiler.precioTotal}"))
                        doc.add(Paragraph("Adelanto: S/. ${alquiler.adelanto}"))
                        doc.add(Paragraph("Saldo: S/. ${alquiler.saldo}"))
                    }
                }
            }
        }
    }

    private fun crearArchivo(prefijo: String): Uri {
        val name = "${prefijo}_${System.currentTimeMillis()}.pdf"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: throw Exception("Error al insertar en MediaStore")
        } else {
            val file = java.io.File(context.cacheDir, "pdfs")
            if (!file.exists()) file.mkdirs()
            val pdfFile = java.io.File(file, name)
            androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
        }
    }

    suspend fun generarPdfComprobante(
        comprobante: Comprobante,
        alquiler: Alquiler,
        workspace: Workspace?
    ): Resource<Uri> = withContext(Dispatchers.IO) {
        try {
            val prefijo = comprobante.tipo.name.lowercase().replaceFirstChar { it.uppercase() }
            val pdfUri = crearArchivo("${prefijo}_${comprobante.correlativoCompleto.replace("-", "_")}")
            
            if (comprobante.tipo == TipoComprobante.TICKET) {
                buildTicketPremiumPdf(pdfUri, comprobante, alquiler, workspace)
            } else {
                buildComprobantePdf(pdfUri, comprobante, alquiler, workspace)
            }

            finalizarArchivo(pdfUri)
            Resource.Success(pdfUri)
        } catch (e: Exception) {
            Log.e("PdfService", "Error generando PDF: ${e.message}", e)
            Resource.Error("Falla al generar PDF del comprobante: ${e.message}")
        }
    }

    private fun buildComprobantePdf(uri: Uri, comprobante: Comprobante, alquiler: Alquiler, workspace: Workspace?) {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            PdfWriter(os).use { writer ->
                PdfDocument(writer).use { pdf ->
                    Document(pdf).use { doc ->
                        doc.add(Paragraph("${comprobante.tipo} ELECTRÓNICA").setBold().setFontSize(18f))
                        doc.add(Paragraph("Número: ${comprobante.correlativoCompleto}"))
                        doc.add(Paragraph("Negocio: ${workspace?.nombre ?: ""}"))
                        doc.add(Paragraph("Cliente: ${comprobante.clienteNombre}"))
                        doc.add(Paragraph("Detalle: ${alquiler.itemNombre}"))
                        doc.add(Paragraph("Total: S/. ${comprobante.total}").setBold())
                    }
                }
            }
        }
    }

    private fun buildTicketPremiumPdf(uri: Uri, comprobante: Comprobante, alquiler: Alquiler, workspace: Workspace?) {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            PdfWriter(os).use { writer ->
                PdfDocument(writer).use { pdf ->
                    Document(pdf).use { doc ->
                        doc.setMargins(20f, 20f, 20f, 20f)
                        
                        // 1. Header
                        doc.add(Paragraph(workspace?.nombreComercial?.uppercase() ?: workspace?.nombre?.uppercase() ?: "RAYMI GESTIÓN")
                            .setBold().setFontSize(16f).setFontColor(primaryColor).setTextAlignment(TextAlignment.CENTER))
                        
                        workspace?.let {
                            if (it.ruc.isNotBlank()) doc.add(Paragraph("RUC: ${it.ruc}").setFontSize(8f).setTextAlignment(TextAlignment.CENTER))
                            if (it.direccion.isNotBlank()) doc.add(Paragraph(it.direccion).setFontSize(8f).setTextAlignment(TextAlignment.CENTER))
                            if (it.telefono.isNotBlank()) doc.add(Paragraph("WhatsApp: ${it.telefono}").setFontSize(8f).setTextAlignment(TextAlignment.CENTER))
                        }
                        
                        doc.add(Paragraph("------------------------------------------------------------------").setFontColor(ColorConstants.LIGHT_GRAY).setTextAlignment(TextAlignment.CENTER))
                        doc.add(Paragraph("NOTA DE VENTA").setBold().setFontSize(10f).setTextAlignment(TextAlignment.CENTER))
                        doc.add(Paragraph(comprobante.correlativoCompleto).setBold().setFontSize(12f).setTextAlignment(TextAlignment.CENTER))
                        doc.add(Paragraph("Emisión: ${dateFormat.format(Date())}").setFontSize(8f).setTextAlignment(TextAlignment.CENTER))
                        doc.add(Paragraph("Ref Alquiler: ${alquiler.id.takeLast(8).uppercase()}").setFontSize(7f).setTextAlignment(TextAlignment.CENTER))
                        
                        doc.add(Paragraph("\nCLIENTE: ${comprobante.clienteNombre}").setBold().setFontSize(9f))
                        if (comprobante.clienteDocumento.isNotBlank()) doc.add(Paragraph("DNI/RUC: ${comprobante.clienteDocumento}").setFontSize(8f))
                        doc.add(Paragraph("------------------------------------------------------------------").setFontColor(ColorConstants.LIGHT_GRAY))

                        val table = Table(UnitValue.createPercentArray(floatArrayOf(10f, 60f, 30f))).useAllAvailableWidth()
                        table.addCell(Cell().add(Paragraph(alquiler.cantidad.toString())).setFontSize(8f).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        table.addCell(Cell().add(Paragraph(alquiler.itemNombre)).setFontSize(8f).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        table.addCell(Cell().add(Paragraph("S/. ${String.format(Locale.US, "%.2f", alquiler.precioTotal)}")).setFontSize(8f).setTextAlignment(TextAlignment.RIGHT).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        
                        doc.add(table)
                        doc.add(Paragraph("------------------------------------------------------------------").setFontColor(ColorConstants.LIGHT_GRAY))
                        doc.add(Paragraph("TOTAL: S/. ${String.format(Locale.US, "%.2f", comprobante.total)}").setBold().setTextAlignment(TextAlignment.RIGHT))
                        
                        // Términos y Condiciones
                        workspace?.terminosCondiciones?.let { terms ->
                            if (terms.isNotBlank()) {
                                doc.add(Paragraph("\nTérminos y Condiciones:").setBold().setFontSize(7f))
                                doc.add(Paragraph(terms).setFontSize(6f))
                            }
                        }

                        doc.add(Paragraph("\n--- Gracias por su preferencia ---").setItalic().setFontSize(9f).setTextAlignment(TextAlignment.CENTER))
                        doc.add(Paragraph("Desarrollado por RAYMI SaaS").setFontSize(6f).setFontColor(ColorConstants.GRAY).setTextAlignment(TextAlignment.CENTER))
                    }
                }
            }
        }
    }

    suspend fun generarPdfInventario(items: List<Item>, negocioNombre: String): Resource<Uri> =
        withContext(Dispatchers.IO) {
            try {
                val pdfUri = crearArchivo("Inventario_${negocioNombre.replace(" ", "_")}")
                buildInventoryPdf(pdfUri, items, negocioNombre)
                finalizarArchivo(pdfUri)
                Resource.Success(pdfUri)
            } catch (e: Exception) {
                Resource.Error("Falla al generar PDF de Inventario")
            }
        }

    private fun buildInventoryPdf(uri: Uri, items: List<Item>, business: String) {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            PdfWriter(os).use { writer ->
                PdfDocument(writer).use { pdf ->
                    Document(pdf).use { doc ->
                        doc.add(Paragraph("INVENTARIO: $business").setBold().setFontSize(18f))
                        items.forEach { doc.add(Paragraph("${it.codigo} - ${it.nombre} - S/. ${it.precio}")) }
                    }
                }
            }
        }
    }
    
    suspend fun generarPdfResumenFinanciero(alquileres: List<Alquiler>, year: Int): Resource<Uri> =
        withContext(Dispatchers.IO) {
            try {
                val pdfUri = crearArchivo("Reporte_Anual_$year")
                buildFinancialReportPdf(pdfUri, alquileres, year)
                finalizarArchivo(pdfUri)
                Resource.Success(pdfUri)
            } catch (e: Exception) {
                Resource.Error("Falla al generar Reporte Financiero")
            }
        }

    private fun buildFinancialReportPdf(uri: Uri, alquileres: List<Alquiler>, year: Int) {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            PdfWriter(os).use { writer ->
                PdfDocument(writer).use { pdf ->
                    Document(pdf).use { doc ->
                        doc.add(Paragraph("REPORTE $year").setBold().setFontSize(22f))
                        doc.add(Paragraph("Total: S/. ${alquileres.sumOf { it.precioTotal }}"))
                    }
                }
            }
        }
    }
}
