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
                            table.addCell(valorCell(String.format(Locale.US, "%.2f", subtotal)))
                        }
                        doc.add(table)
                        doc.add(Paragraph("\nTOTAL RECAUDADO EN $year: S/. ${String.format(Locale.US, "%.2f", totalAnual)}").setBold().setFontSize(14f).setFontColor(primaryColor))
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

    /**
     * Genera un comprobante oficial (Ticket, Boleta o Factura).
     */
    suspend fun generarPdfComprobante(
        comprobante: com.raymi.app.domain.model.Comprobante,
        alquiler: Alquiler,
        workspace: Workspace?
    ): Resource<Uri> = withContext(Dispatchers.IO) {
        try {
            val prefijo = when (comprobante.tipo) {
                com.raymi.app.domain.model.TipoComprobante.TICKET -> "Ticket"
                com.raymi.app.domain.model.TipoComprobante.BOLETA -> "Boleta"
                com.raymi.app.domain.model.TipoComprobante.FACTURA -> "Factura"
            }
            val pdfUri = crearArchivo("${prefijo}_${comprobante.correlativoCompleto}")
            
            if (comprobante.tipo == com.raymi.app.domain.model.TipoComprobante.TICKET) {
                buildTicketPremiumPdf(pdfUri, comprobante, alquiler, workspace)
            } else {
                buildComprobantePdf(pdfUri, comprobante, alquiler, workspace)
            }

            val contentValues = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            context.contentResolver.update(pdfUri, contentValues, null, null)
            Resource.Success(pdfUri)
        } catch (e: Exception) {
            Resource.Error("Falla al generar PDF del comprobante: ${e.message}")
        }
    }

    private fun buildComprobantePdf(
        uri: Uri,
        comprobante: com.raymi.app.domain.model.Comprobante,
        alquiler: Alquiler,
        workspace: Workspace?
    ) {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            PdfWriter(os).use { writer ->
                PdfDocument(writer).use { pdf ->
                    Document(pdf).use { doc ->
                        doc.setMargins(40f, 40f, 40f, 40f)

                        // 1. Cabecera del Negocio
                        val businessName = workspace?.nombre ?: "RAYMI GESTIÓN"
                        doc.add(Paragraph(businessName.uppercase()).setBold().setFontSize(18f).setFontColor(primaryColor))
                        
                        workspace?.let {
                             val infoNegocio = StringBuilder()
                             if (it.ruc.isNotBlank()) infoNegocio.append("RUC: ${it.ruc}\n")
                             if (it.direccion.isNotBlank()) infoNegocio.append("${it.direccion}\n")
                             if (it.telefono.isNotBlank()) infoNegocio.append("Telf: ${it.telefono}")
                             if (infoNegocio.isNotEmpty()) {
                                 doc.add(Paragraph(infoNegocio.toString()).setFontSize(9f).setFontColor(ColorConstants.DARK_GRAY))
                             }
                        }
                        doc.add(Paragraph("\n"))

                        // 2. Título y Numeración
                        val title = when (comprobante.tipo) {
                            com.raymi.app.domain.model.TipoComprobante.TICKET -> "COMPROBANTE INTERNO (TICKET)"
                            com.raymi.app.domain.model.TipoComprobante.BOLETA -> "BOLETA DE VENTA REFERENCIAL"
                            com.raymi.app.domain.model.TipoComprobante.FACTURA -> "FACTURA REFERENCIAL"
                        }
                        
                        val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f))).useAllAvailableWidth()
                        headerTable.addCell(Cell().add(Paragraph(title).setBold().setFontSize(12f)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        headerTable.addCell(Cell().add(Paragraph(comprobante.correlativoCompleto).setBold().setFontSize(14f).setTextAlignment(TextAlignment.RIGHT)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        doc.add(headerTable)
                        doc.add(Paragraph("\n"))

                        // 3. Datos del Cliente
                        val clientTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f))).useAllAvailableWidth()
                        val clientInfo = StringBuilder()
                        clientInfo.append("CLIENTE: ${comprobante.clienteNombre}\n")
                        if (comprobante.clienteDocumento.isNotBlank()) {
                            val tipoDoc = when (comprobante.clienteTipoDocumento) {
                                com.raymi.app.domain.model.TipoDocumentoCliente.DNI -> "DNI"
                                com.raymi.app.domain.model.TipoDocumentoCliente.RUC -> "RUC"
                                else -> "DOC"
                            }
                            clientInfo.append("$tipoDoc: ${comprobante.clienteDocumento}\n")
                        }
                        if (!comprobante.razonSocial.isNullOrBlank()) clientInfo.append("R. SOCIAL: ${comprobante.razonSocial}\n")
                        if (!comprobante.direccionFiscal.isNullOrBlank()) clientInfo.append("DIR: ${comprobante.direccionFiscal}")
                        
                        clientTable.addCell(Cell().add(Paragraph(clientInfo.toString()).setFontSize(9f)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        clientTable.addCell(Cell().add(Paragraph("FECHA EMISIÓN: ${dateFormat.format(comprobante.createdAt.toDate())}\nMETODO PAGO: ${comprobante.metodoPago}").setFontSize(9f).setTextAlignment(TextAlignment.RIGHT)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        doc.add(clientTable)
                        doc.add(Paragraph("\n"))

                        // 4. Detalle de Items
                        val itemsTable = Table(UnitValue.createPercentArray(floatArrayOf(15f, 55f, 15f, 15f))).useAllAvailableWidth()
                        itemsTable.addHeaderCell(headerCell("CANT"))
                        itemsTable.addHeaderCell(headerCell("DESCRIPCIÓN"))
                        itemsTable.addHeaderCell(headerCell("P. UNIT").setTextAlignment(TextAlignment.RIGHT))
                        itemsTable.addHeaderCell(headerCell("TOTAL").setTextAlignment(TextAlignment.RIGHT))

                        itemsTable.addCell(valorCell(alquiler.cantidad.toString()))
                        itemsTable.addCell(valorCell("${alquiler.itemNombre} (${alquiler.itemCodigo})"))
                        itemsTable.addCell(valorCell(String.format(Locale.US, "%.2f", alquiler.precioUnitario)).setTextAlignment(TextAlignment.RIGHT))
                        itemsTable.addCell(valorCell(String.format(Locale.US, "%.2f", alquiler.precioTotal)).setTextAlignment(TextAlignment.RIGHT))
                        doc.add(itemsTable)

                        // 5. Totales
                        val totalsTable = Table(UnitValue.createPercentArray(floatArrayOf(70f, 30f))).useAllAvailableWidth()
                        totalsTable.addCell(Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        
                        val totalsCol = Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                        if (comprobante.tipo == com.raymi.app.domain.model.TipoComprobante.FACTURA) {
                            totalsCol.add(Paragraph("SUBTOTAL: S/. ${String.format(Locale.US, "%.2f", comprobante.subtotal)}").setFontSize(9f).setTextAlignment(TextAlignment.RIGHT))
                            totalsCol.add(Paragraph("IGV (18%): S/. ${String.format(Locale.US, "%.2f", comprobante.igv)}").setFontSize(9f).setTextAlignment(TextAlignment.RIGHT))
                        }
                        totalsCol.add(Paragraph("TOTAL: S/. ${String.format(Locale.US, "%.2f", comprobante.total)}").setBold().setFontSize(11f).setTextAlignment(TextAlignment.RIGHT))
                        totalsCol.add(Paragraph("PAGADO: S/. ${String.format(Locale.US, "%.2f", comprobante.pagado)}").setFontSize(9f).setTextAlignment(TextAlignment.RIGHT))
                        if (comprobante.saldo > 0) {
                            totalsCol.add(Paragraph("SALDO: S/. ${String.format(Locale.US, "%.2f", comprobante.saldo)}").setFontColor(com.itextpdf.kernel.colors.ColorConstants.RED).setFontSize(9f).setTextAlignment(TextAlignment.RIGHT))
                        }
                        
                        totalsTable.addCell(totalsCol)
                        doc.add(totalsTable)

                        doc.add(Paragraph("\n\nDOCUMENTO GENERADO POR RAYMI").setFontSize(8f).setItalic().setTextAlignment(TextAlignment.CENTER))
                        doc.add(Paragraph("Este no es un comprobante electrónico válido ante SUNAT.").setFontSize(7f).setTextAlignment(TextAlignment.CENTER))
                    }
                }
            }
        }
    }

    private fun buildTicketPremiumPdf(
        uri: Uri,
        comprobante: com.raymi.app.domain.model.Comprobante,
        alquiler: Alquiler,
        workspace: Workspace?
    ) {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            PdfWriter(os).use { writer ->
                PdfDocument(writer).use { pdf ->
                    Document(pdf).use { doc ->
                        // Diseño tipo Ticket de 80mm (aprox 226 pts) o A4 compacto
                        doc.setMargins(20f, 20f, 20f, 20f)
                        
                        // 1. Logo o Nombre de Negocio Centrado
                        doc.add(Paragraph(workspace?.nombre?.uppercase() ?: "RAYMI GESTIÓN")
                            .setBold().setFontSize(16f).setFontColor(primaryColor).setTextAlignment(TextAlignment.CENTER))
                        
                        workspace?.let {
                            if (it.ruc.isNotBlank()) doc.add(Paragraph("RUC: ${it.ruc}").setFontSize(8f).setTextAlignment(TextAlignment.CENTER))
                            if (it.direccion.isNotBlank()) doc.add(Paragraph(it.direccion).setFontSize(8f).setTextAlignment(TextAlignment.CENTER))
                            if (it.telefono.isNotBlank()) doc.add(Paragraph("Telf: ${it.telefono}").setFontSize(8f).setTextAlignment(TextAlignment.CENTER))
                        }
                        
                        doc.add(Paragraph("------------------------------------------------------------------").setFontColor(ColorConstants.LIGHT_GRAY).setTextAlignment(TextAlignment.CENTER))
                        
                        // 2. Info del Ticket
                        doc.add(Paragraph("NOTA DE VENTA").setBold().setFontSize(10f).setTextAlignment(TextAlignment.CENTER))
                        doc.add(Paragraph(comprobante.correlativoCompleto).setBold().setFontSize(12f).setTextAlignment(TextAlignment.CENTER))
                        doc.add(Paragraph("Fecha: ${dateFormat.format(comprobante.createdAt.toDate())}").setFontSize(8f).setTextAlignment(TextAlignment.CENTER))
                        
                        doc.add(Paragraph("\nCLIENTE: ${comprobante.clienteNombre}").setBold().setFontSize(9f))
                        if (comprobante.clienteDocumento.isNotBlank()) doc.add(Paragraph("DNI/RUC: ${comprobante.clienteDocumento}").setFontSize(8f))
                        
                        doc.add(Paragraph("\nDETALLE").setBold().setFontSize(9f).setFontColor(primaryColor))
                        doc.add(Paragraph("------------------------------------------------------------------").setFontColor(ColorConstants.LIGHT_GRAY))

                        // 3. Items
                        val table = Table(UnitValue.createPercentArray(floatArrayOf(10f, 60f, 30f))).useAllAvailableWidth()
                        table.addCell(Cell().add(Paragraph("Cant")).setBold().setFontSize(8f).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        table.addCell(Cell().add(Paragraph("Producto")).setBold().setFontSize(8f).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        table.addCell(Cell().add(Paragraph("Importe")).setBold().setFontSize(8f).setTextAlignment(TextAlignment.RIGHT).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        
                        table.addCell(Cell().add(Paragraph(alquiler.cantidad.toString())).setFontSize(8f).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        table.addCell(Cell().add(Paragraph("${alquiler.itemNombre}\n(${alquiler.itemCodigo})")).setFontSize(8f).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        table.addCell(Cell().add(Paragraph("S/. ${String.format(Locale.US, "%.2f", alquiler.precioTotal)}")).setFontSize(8f).setTextAlignment(TextAlignment.RIGHT).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        
                        doc.add(table)
                        doc.add(Paragraph("------------------------------------------------------------------").setFontColor(ColorConstants.LIGHT_GRAY))

                        // 4. Totales
                        val totals = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f))).useAllAvailableWidth()
                        totals.addCell(Cell().add(Paragraph("TOTAL A PAGAR:")).setBold().setFontSize(10f).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        totals.addCell(Cell().add(Paragraph("S/. ${String.format(Locale.US, "%.2f", comprobante.total)}")).setBold().setFontSize(10f).setTextAlignment(TextAlignment.RIGHT).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        
                        totals.addCell(Cell().add(Paragraph("ADELANTO:")).setFontSize(8f).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        totals.addCell(Cell().add(Paragraph("S/. ${String.format(Locale.US, "%.2f", comprobante.pagado)}")).setFontSize(8f).setTextAlignment(TextAlignment.RIGHT).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        
                        if (comprobante.saldo > 0) {
                            totals.addCell(Cell().add(Paragraph("SALDO PENDIENTE:")).setFontSize(8f).setFontColor(ColorConstants.RED).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                            totals.addCell(Cell().add(Paragraph("S/. ${String.format(Locale.US, "%.2f", comprobante.saldo)}")).setFontSize(8f).setFontColor(ColorConstants.RED).setTextAlignment(TextAlignment.RIGHT).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                        }
                        doc.add(totals)
                        
                        doc.add(Paragraph("\n¡Gracias por su preferencia!").setItalic().setFontSize(9f).setTextAlignment(TextAlignment.CENTER))
                        doc.add(Paragraph("------------------------------------------------------------------").setFontColor(ColorConstants.LIGHT_GRAY))
                        doc.add(Paragraph("Desarrollado por RAYMI SaaS").setFontSize(7f).setFontColor(ColorConstants.GRAY).setTextAlignment(TextAlignment.CENTER))
                    }
                }
            }
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
