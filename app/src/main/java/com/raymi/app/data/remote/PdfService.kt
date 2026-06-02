package com.raymi.app.data.remote

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.raymi.app.core.utils.QrCodeGenerator
import com.raymi.app.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val primaryColor = DeviceRgb(98, 0, 238) // Morado RAYMI
    private val lightGray = DeviceRgb(245, 245, 245)

    private fun getLogoImage(logoUrl: String?): Image? {
        if (logoUrl.isNullOrBlank()) return null
        return try {
            val connection = URL(logoUrl).openConnection()
            connection.connectTimeout = 2500 // OPTIMIZACIÓN: Timeout reducido para evitar bloqueo largo
            connection.readTimeout = 2500
            val bytes = connection.getInputStream().use { it.readBytes() }
            val imageData = ImageDataFactory.create(bytes)
            Image(imageData)
        } catch (e: Exception) {
            Log.e("PdfService", "Error cargando logo, continuando sin él: ${e.message}")
            null
        }
    }

    private fun convertBitmapToImage(bitmap: Bitmap): Image {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val imageData = ImageDataFactory.create(stream.toByteArray())
        return Image(imageData)
    }

    suspend fun generarComprobanteAlquiler(alquiler: Alquiler, workspace: Workspace?, pagos: List<Pago> = emptyList()): Resource<Uri> =
        withContext(Dispatchers.IO) {
            try {
                val pdfUri = crearArchivo("Recibo_${alquiler.itemCodigo}")
                val logo = getLogoImage(workspace?.logoUrl)
                buildAlquilerPremiumPdf(pdfUri, alquiler, workspace, logo, pagos)
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

    private fun buildAlquilerPremiumPdf(uri: Uri, alquiler: Alquiler, workspace: Workspace?, logo: Image?, pagos: List<Pago>) {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            PdfWriter(os).use { writer ->
                PdfDocument(writer).use { pdf ->
                    Document(pdf).use { doc ->
                        doc.setMargins(30f, 30f, 30f, 30f)

                        // 1. Header con Logo si existe
                        val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(20f, 50f, 30f))).useAllAvailableWidth()
                        
                        // Celda 1: Logo
                        val logoCell = Cell().setBorder(Border.NO_BORDER)
                        logo?.let { 
                            it.scaleToFit(60f, 60f)
                            logoCell.add(it) 
                        }
                        headerTable.addCell(logoCell)

                        // Celda 2: Nombre Negocio
                        val businessInfo = Cell().add(Paragraph(workspace?.nombreComercial ?: workspace?.nombre ?: "RAYMI GESTIÓN")
                            .setBold().setFontSize(18f).setFontColor(primaryColor))
                            .add(Paragraph("CONTRATO DE ALQUILER").setItalic().setFontSize(9f))
                            .setBorder(Border.NO_BORDER)
                        headerTable.addCell(businessInfo)
                        
                        // Celda 3: Info Documento
                        val docInfo = Cell().add(Paragraph("N° ${alquiler.id.takeLast(8).uppercase()}")
                            .setBold().setFontSize(12f).setTextAlignment(TextAlignment.RIGHT))
                            .add(Paragraph("Fecha: ${dateFormat.format(Date())}").setFontSize(8f).setTextAlignment(TextAlignment.RIGHT))
                            .setBorder(Border.NO_BORDER)
                        headerTable.addCell(docInfo)
                        
                        doc.add(headerTable)
                        doc.add(Paragraph("\n"))

                        // 2. Información del Cliente (Caja con borde)
                        val clientBox = Table(1).useAllAvailableWidth()
                        clientBox.addCell(Cell().add(Paragraph("DATOS DEL CLIENTE").setBold().setFontSize(9f).setFontColor(ColorConstants.WHITE))
                            .setBackgroundColor(primaryColor).setPaddingLeft(10f).setBorder(Border.NO_BORDER))
                        
                        val clientDetails = Cell().add(Paragraph("Nombre: ${alquiler.clienteNombre}").setFontSize(10f))
                            .add(Paragraph("Documento: ${alquiler.clienteDni}").setFontSize(10f))
                            .setPadding(10f).setBorder(SolidBorder(primaryColor, 0.5f))
                        
                        clientBox.addCell(clientDetails)
                        doc.add(clientBox)
                        
                        doc.add(Paragraph("\n"))

                        // 3. Detalle del Alquiler (Tabla Profesional)
                        val mainTable = Table(UnitValue.createPercentArray(floatArrayOf(10f, 50f, 20f, 20f))).useAllAvailableWidth()
                        mainTable.addHeaderCell(Cell().add(Paragraph("CANT.").setBold().setFontSize(9f).setFontColor(ColorConstants.WHITE)).setBackgroundColor(primaryColor).setTextAlignment(TextAlignment.CENTER))
                        mainTable.addHeaderCell(Cell().add(Paragraph("DESCRIPCIÓN").setBold().setFontSize(9f).setFontColor(ColorConstants.WHITE)).setBackgroundColor(primaryColor))
                        mainTable.addHeaderCell(Cell().add(Paragraph("UNIT.").setBold().setFontSize(9f).setFontColor(ColorConstants.WHITE)).setBackgroundColor(primaryColor).setTextAlignment(TextAlignment.RIGHT))
                        mainTable.addHeaderCell(Cell().add(Paragraph("TOTAL").setBold().setFontSize(9f).setFontColor(ColorConstants.WHITE)).setBackgroundColor(primaryColor).setTextAlignment(TextAlignment.RIGHT))

                        mainTable.addCell(Cell().add(Paragraph(alquiler.cantidad.toString())).setTextAlignment(TextAlignment.CENTER).setFontSize(10f))
                        mainTable.addCell(Cell().add(Paragraph(alquiler.itemNombre)).setFontSize(10f))
                        mainTable.addCell(Cell().add(Paragraph("S/. ${alquiler.precioUnitario}")).setTextAlignment(TextAlignment.RIGHT).setFontSize(10f))
                        mainTable.addCell(Cell().add(Paragraph("S/. ${alquiler.precioTotal}")).setTextAlignment(TextAlignment.RIGHT).setFontSize(10f).setBold())

                        doc.add(mainTable)
                        doc.add(Paragraph("\n"))

                        // --- NUEVO: Cuadro de Pagos (Historial) ---
                        if (pagos.isNotEmpty()) {
                            val paymentsTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 40f, 30f))).useAllAvailableWidth()
                            paymentsTable.addHeaderCell(Cell(1, 3).add(Paragraph("HISTORIAL DE ABONOS").setBold().setFontSize(9f).setFontColor(primaryColor)).setBorder(Border.NO_BORDER).setPaddingBottom(5f))
                            
                            paymentsTable.addHeaderCell(Cell().add(Paragraph("FECHA").setBold().setFontSize(8f)).setBackgroundColor(lightGray))
                            paymentsTable.addHeaderCell(Cell().add(Paragraph("MÉTODO / REF").setBold().setFontSize(8f)).setBackgroundColor(lightGray))
                            paymentsTable.addHeaderCell(Cell().add(Paragraph("MONTO").setBold().setFontSize(8f).setTextAlignment(TextAlignment.RIGHT)).setBackgroundColor(lightGray))

                            pagos.forEach { pago ->
                                paymentsTable.addCell(Cell().add(Paragraph(dateFormat.format(pago.fecha.toDate())).setFontSize(8f)))
                                paymentsTable.addCell(Cell().add(Paragraph("${pago.metodoPago.name} ${if(pago.referencia.isNotBlank()) "(${pago.referencia})" else ""}").setFontSize(8f)))
                                paymentsTable.addCell(Cell().add(Paragraph("S/. ${String.format(Locale.US, "%.2f", pago.monto)}")).setTextAlignment(TextAlignment.RIGHT).setFontSize(8f))
                            }
                            doc.add(paymentsTable)
                            doc.add(Paragraph("\n"))
                        }

                        // 4. Resumen Financiero
                        val footerTable = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f))).useAllAvailableWidth()
                        
                        // Celda vacía o con QR
                        val qrCell = Cell().setBorder(Border.NO_BORDER)
                        QrCodeGenerator.generateQrCode("ALQ-${alquiler.id}", 150)?.let { bitmap ->
                            val qrImage = convertBitmapToImage(bitmap).setHeight(80f).setWidth(80f)
                            qrCell.add(qrImage)
                        }
                        footerTable.addCell(qrCell)

                        val summary = Cell().setBorder(Border.NO_BORDER)
                        summary.add(Paragraph("SUBTOTAL: S/. ${String.format(Locale.US, "%.2f", alquiler.precioTotal / 1.18)}").setTextAlignment(TextAlignment.RIGHT).setFontSize(9f))
                        summary.add(Paragraph("IGV (18%): S/. ${String.format(Locale.US, "%.2f", alquiler.precioTotal - (alquiler.precioTotal / 1.18))}").setTextAlignment(TextAlignment.RIGHT).setFontSize(9f))
                        summary.add(Paragraph("PRECIO TOTAL: S/. ${String.format(Locale.US, "%.2f", alquiler.precioTotal)}").setTextAlignment(TextAlignment.RIGHT).setBold().setFontSize(12f).setFontColor(primaryColor))
                        summary.add(Paragraph("ADELANTO: S/. ${String.format(Locale.US, "%.2f", alquiler.adelanto)}").setTextAlignment(TextAlignment.RIGHT).setFontSize(10f).setFontColor(ColorConstants.DARK_GRAY))
                        summary.add(Paragraph("SALDO PENDIENTE: S/. ${String.format(Locale.US, "%.2f", alquiler.saldoPendienteReal)}").setTextAlignment(TextAlignment.RIGHT).setBold().setFontSize(11f).setFontColor(ColorConstants.RED))
                        
                        footerTable.addCell(summary)
                        doc.add(footerTable)

                        // 5. Footer / Branding
                        doc.add(Paragraph("\n\n---------------------------------------------------------")
                            .setTextAlignment(TextAlignment.CENTER).setFontColor(ColorConstants.LIGHT_GRAY))
                        doc.add(Paragraph("Gracias por confiar en ${workspace?.nombre ?: "nosotros"}").setItalic().setTextAlignment(TextAlignment.CENTER).setFontSize(9f))
                        doc.add(Paragraph("Potenciado por RAYMI SaaS").setFontSize(7f).setFontColor(ColorConstants.GRAY).setTextAlignment(TextAlignment.CENTER))
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
            androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
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
            val logo = getLogoImage(workspace?.logoUrl)
            
            if (comprobante.tipo == TipoComprobante.TICKET) {
                buildTicketPremiumPdf(pdfUri, comprobante, alquiler, workspace, logo)
            } else {
                buildComprobantePdf(pdfUri, comprobante, alquiler, workspace, logo)
            }

            finalizarArchivo(pdfUri)
            Resource.Success(pdfUri)
        } catch (e: Exception) {
            Log.e("PdfService", "Error generando PDF: ${e.message}", e)
            Resource.Error("Falla al generar PDF del comprobante: ${e.message}")
        }
    }

    private fun buildComprobantePdf(uri: Uri, comprobante: Comprobante, alquiler: Alquiler, workspace: Workspace?, logo: Image?) {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            PdfWriter(os).use { writer ->
                PdfDocument(writer).use { pdf ->
                    Document(pdf).use { doc ->
                        doc.setMargins(30f, 30f, 30f, 30f)

                        // Cabecera Premium
                        val header = Table(UnitValue.createPercentArray(floatArrayOf(15f, 45f, 40f))).useAllAvailableWidth()
                        
                        // Logo
                        val logoCell = Cell().setBorder(Border.NO_BORDER)
                        logo?.let { logoCell.add(it.scaleToFit(50f, 50f)) }
                        header.addCell(logoCell)

                        header.addCell(Cell().add(Paragraph(workspace?.nombre?.uppercase() ?: "RAYMI GESTIÓN").setBold().setFontSize(18f).setFontColor(primaryColor)).setBorder(Border.NO_BORDER))
                        header.addCell(Cell().add(Paragraph("${comprobante.tipo} ELECTRÓNICA").setBold().setFontSize(12f).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER))
                        
                        // Fila 2 de cabecera
                        header.addCell(Cell().setBorder(Border.NO_BORDER))
                        header.addCell(Cell().add(Paragraph(workspace?.direccion ?: "").setFontSize(8f)).setBorder(Border.NO_BORDER))
                        header.addCell(Cell().add(Paragraph(comprobante.correlativoCompleto).setBold().setFontSize(14f).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER))
                        
                        doc.add(header)
                        doc.add(Paragraph("\n"))

                        // Datos del Cliente
                        val clientTable = Table(2).useAllAvailableWidth()
                        clientTable.addCell(Cell().add(Paragraph("RECEPTOR").setBold().setFontSize(8f).setFontColor(ColorConstants.GRAY)).setBorder(Border.NO_BORDER))
                        clientTable.addCell(Cell().add(Paragraph("FECHA EMISIÓN").setBold().setFontSize(8f).setFontColor(ColorConstants.GRAY).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER))
                        clientTable.addCell(Cell().add(Paragraph(comprobante.clienteNombre).setBold().setFontSize(10f)).setBorder(Border.NO_BORDER))
                        clientTable.addCell(Cell().add(Paragraph(dateFormat.format(comprobante.createdAt.toDate())).setFontSize(10f).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER))
                        clientTable.addCell(Cell().add(Paragraph("${comprobante.clienteTipoDocumento}: ${comprobante.clienteDocumento}").setFontSize(9f)).setBorder(Border.NO_BORDER))
                        clientTable.addCell(Cell().setBorder(Border.NO_BORDER))
                        doc.add(clientTable)

                        doc.add(Paragraph("\n"))

                        // Tabla de Items
                        val itemsTable = Table(UnitValue.createPercentArray(floatArrayOf(10f, 60f, 30f))).useAllAvailableWidth()
                        itemsTable.addHeaderCell(Cell().add(Paragraph("CANT.").setBold().setFontSize(9f)).setBackgroundColor(lightGray))
                        itemsTable.addHeaderCell(Cell().add(Paragraph("PRODUCTO / SERVICIO").setBold().setFontSize(9f)).setBackgroundColor(lightGray))
                        itemsTable.addHeaderCell(Cell().add(Paragraph("TOTAL").setBold().setFontSize(9f).setTextAlignment(TextAlignment.RIGHT)).setBackgroundColor(lightGray))

                        itemsTable.addCell(Cell().add(Paragraph(alquiler.cantidad.toString())).setFontSize(10f))
                        itemsTable.addCell(Cell().add(Paragraph(alquiler.itemNombre)).setFontSize(10f))
                        itemsTable.addCell(Cell().add(Paragraph("S/. ${comprobante.total}")).setFontSize(10f).setTextAlignment(TextAlignment.RIGHT))
                        doc.add(itemsTable)

                        // Totales y QR
                        val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(70f, 30f))).useAllAvailableWidth()
                        
                        val qrCell = Cell().setBorder(Border.NO_BORDER)
                        QrCodeGenerator.generateQrCode("CPE|${workspace?.ruc}|${comprobante.tipo}|${comprobante.serie}|${comprobante.numero}|${comprobante.total}", 200)?.let {
                            qrCell.add(convertBitmapToImage(it).setWidth(100f).setHeight(100f))
                        }
                        summaryTable.addCell(qrCell)

                        val totalCell = Cell().setBorder(Border.NO_BORDER)
                        totalCell.add(Paragraph("OP. GRAVADA: S/. ${String.format(Locale.US, "%.2f", comprobante.subtotal)}").setTextAlignment(TextAlignment.RIGHT).setFontSize(9f))
                        totalCell.add(Paragraph("IGV (18%): S/. ${String.format(Locale.US, "%.2f", comprobante.igv)}").setTextAlignment(TextAlignment.RIGHT).setFontSize(9f))
                        totalCell.add(Paragraph("TOTAL: S/. ${String.format(Locale.US, "%.2f", comprobante.total)}").setTextAlignment(TextAlignment.RIGHT).setBold().setFontSize(14f).setFontColor(primaryColor))
                        summaryTable.addCell(totalCell)
                        
                        doc.add(summaryTable)
                        
                        doc.add(Paragraph("\nRepresentación impresa de la ${comprobante.tipo} Electrónica.").setFontSize(7f).setTextAlignment(TextAlignment.CENTER))
                    }
                }
            }
        }
    }

    private fun buildTicketPremiumPdf(uri: Uri, comprobante: Comprobante, alquiler: Alquiler, workspace: Workspace?, logo: Image?) {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            PdfWriter(os).use { writer ->
                PdfDocument(writer).use { pdf ->
                    Document(pdf).use { doc ->
                        doc.setMargins(20f, 20f, 20f, 20f)
                        
                        // 1. Header Premium con Logo
                        if (logo != null) {
                            logo.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER)
                            logo.scaleToFit(50f, 50f)
                            doc.add(logo)
                        }

                        val titleTable = Table(1).useAllAvailableWidth()
                        titleTable.addCell(Cell().add(Paragraph(workspace?.nombreComercial?.uppercase() ?: workspace?.nombre?.uppercase() ?: "RAYMI GESTIÓN")
                            .setBold().setFontSize(14f).setFontColor(ColorConstants.WHITE))
                            .setBackgroundColor(primaryColor).setTextAlignment(TextAlignment.CENTER).setBorder(Border.NO_BORDER).setPadding(5f))
                        doc.add(titleTable)
                        
                        workspace?.let {
                            if (it.ruc.isNotBlank()) doc.add(Paragraph("RUC: ${it.ruc}").setFontSize(8f).setTextAlignment(TextAlignment.CENTER).setMarginTop(5f))
                            if (it.direccion.isNotBlank()) doc.add(Paragraph(it.direccion).setFontSize(7f).setTextAlignment(TextAlignment.CENTER))
                            if (it.telefono.isNotBlank()) doc.add(Paragraph("WhatsApp: ${it.telefono}").setFontSize(8f).setTextAlignment(TextAlignment.CENTER).setFontColor(primaryColor))
                        }
                        
                        doc.add(Paragraph("------------------------------------------------------------------").setFontColor(ColorConstants.LIGHT_GRAY).setTextAlignment(TextAlignment.CENTER))
                        
                        val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f))).useAllAvailableWidth()
                        infoTable.addCell(Cell().add(Paragraph(comprobante.tipo.name).setBold().setFontSize(9f)).setBorder(Border.NO_BORDER))
                        infoTable.addCell(Cell().add(Paragraph(comprobante.correlativoCompleto).setBold().setFontSize(11f).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER))
                        doc.add(infoTable)
                        
                        doc.add(Paragraph("Fecha: ${dateFormat.format(Date())}").setFontSize(8f))
                        doc.add(Paragraph("Cliente: ${comprobante.clienteNombre}").setBold().setFontSize(9f))
                        if (comprobante.clienteDocumento.isNotBlank()) doc.add(Paragraph("${comprobante.clienteTipoDocumento}: ${comprobante.clienteDocumento}").setFontSize(8f))
                        
                        doc.add(Paragraph("------------------------------------------------------------------").setFontColor(ColorConstants.LIGHT_GRAY))

                        val table = Table(UnitValue.createPercentArray(floatArrayOf(10f, 60f, 30f))).useAllAvailableWidth()
                        table.addCell(Cell().add(Paragraph(alquiler.cantidad.toString())).setFontSize(8f).setBorder(Border.NO_BORDER))
                        table.addCell(Cell().add(Paragraph(alquiler.itemNombre)).setFontSize(8f).setBorder(Border.NO_BORDER))
                        table.addCell(Cell().add(Paragraph("S/. ${String.format(Locale.US, "%.2f", alquiler.precioTotal)}")).setFontSize(8f).setTextAlignment(TextAlignment.RIGHT).setBorder(Border.NO_BORDER))
                        doc.add(table)
                        
                        doc.add(Paragraph("------------------------------------------------------------------").setFontColor(ColorConstants.LIGHT_GRAY))
                        
                        val totals = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f))).useAllAvailableWidth()
                        
                        // QR Pequeño en el ticket
                        val qrCell = Cell().setBorder(Border.NO_BORDER)
                        QrCodeGenerator.generateQrCode("${comprobante.correlativoCompleto}|${comprobante.total}", 100)?.let {
                            qrCell.add(convertBitmapToImage(it).setWidth(50f).setHeight(50f))
                        }
                        totals.addCell(qrCell)
                        
                        val priceCell = Cell().setBorder(Border.NO_BORDER)
                        priceCell.add(Paragraph("TOTAL: S/. ${String.format(Locale.US, "%.2f", comprobante.total)}").setBold().setFontSize(12f).setTextAlignment(TextAlignment.RIGHT).setFontColor(primaryColor))
                        totals.addCell(priceCell)
                        doc.add(totals)

                        // Términos y Condiciones
                        workspace?.terminosCondiciones?.let { terms ->
                            if (terms.isNotBlank()) {
                                doc.add(Paragraph("\nAVISO:").setBold().setFontSize(7f).setFontColor(primaryColor))
                                doc.add(Paragraph(terms).setFontSize(6f).setItalic())
                            }
                        }

                        doc.add(Paragraph("\n¡Vuelva pronto!").setTextAlignment(TextAlignment.CENTER).setFontSize(8f))
                        doc.add(Paragraph("RAYMI SaaS - Gestión Inteligente").setFontSize(5f).setFontColor(ColorConstants.GRAY).setTextAlignment(TextAlignment.CENTER))
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
    
    suspend fun generarPdfResumenFinanciero(alquileres: List<Alquiler>, pagos: List<Pago>, year: Int): Resource<Uri> =
        withContext(Dispatchers.IO) {
            try {
                val cal = Calendar.getInstance()
                val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("es", "PE"))?.uppercase() ?: "MES"
                val pdfUri = crearArchivo("Reporte_${monthName}_$year")
                buildDetailedFinancialReportPdf(pdfUri, alquileres, pagos, year, monthName)
                finalizarArchivo(pdfUri)
                Resource.Success(pdfUri)
            } catch (e: Exception) {
                Log.e("PdfService", "Error reporte financiero", e)
                Resource.Error("Falla al generar Reporte Financiero")
            }
        }

    private fun buildDetailedFinancialReportPdf(uri: Uri, alquileres: List<Alquiler>, pagos: List<Pago>, year: Int, monthName: String) {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            PdfWriter(os).use { writer ->
                PdfDocument(writer).use { pdf ->
                    Document(pdf).use { doc ->
                        doc.setMargins(40f, 40f, 40f, 40f)

                        // 1. Título y Periodo
                        doc.add(Paragraph("REPORTE FINANCIERO EJECUTIVO")
                            .setBold().setFontSize(20f).setFontColor(primaryColor).setTextAlignment(TextAlignment.CENTER))
                        doc.add(Paragraph("Periodo: $monthName $year")
                            .setFontSize(12f).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20f))

                        // 2. KPIs de Resumen (Basados en Pagos Reales)
                        val kpiTable = Table(UnitValue.createPercentArray(floatArrayOf(33f, 33f, 34f))).useAllAvailableWidth()
                        
                        val cal = Calendar.getInstance()
                        val mesActual = cal.get(Calendar.MONTH)
                        val anioActual = cal.get(Calendar.YEAR)

                        val recaudadoMes = pagos.filter {
                            val c = Calendar.getInstance().apply { time = it.fecha.toDate() }
                            c.get(Calendar.YEAR) == anioActual && c.get(Calendar.MONTH) == mesActual
                        }.sumOf { it.monto }

                        val totalHistorico = pagos.sumOf { it.monto }
                        val porCobrar = alquileres.filter { it.estado != EstadoAlquiler.DEVUELTO && it.estado != EstadoAlquiler.CANCELADO }
                            .sumOf { it.saldoPendienteReal }
                        
                        kpiTable.addCell(Cell().add(Paragraph("RECAUDADO ESTE MES").setFontSize(8f).setFontColor(ColorConstants.GRAY))
                            .add(Paragraph("S/. ${String.format(Locale.US, "%,.2f", recaudadoMes)}").setBold().setFontSize(12f).setFontColor(DeviceRgb(16, 185, 129)))
                            .setBorder(Border.NO_BORDER).setBackgroundColor(lightGray).setPadding(8f))
                            
                        kpiTable.addCell(Cell().add(Paragraph("POR COBRAR (SALDO)").setFontSize(8f).setFontColor(ColorConstants.GRAY))
                            .add(Paragraph("S/. ${String.format(Locale.US, "%,.2f", porCobrar)}").setBold().setFontSize(12f).setFontColor(ColorConstants.RED))
                            .setBorder(Border.NO_BORDER).setBackgroundColor(lightGray).setPadding(8f))

                        kpiTable.addCell(Cell().add(Paragraph("HISTÓRICO TOTAL").setFontSize(8f).setFontColor(ColorConstants.GRAY))
                            .add(Paragraph("S/. ${String.format(Locale.US, "%,.2f", totalHistorico)}").setBold().setFontSize(12f).setFontColor(primaryColor))
                            .setBorder(Border.NO_BORDER).setBackgroundColor(lightGray).setPadding(8f))
                        
                        doc.add(kpiTable)
                        doc.add(Paragraph("\n"))

                        // 3. Tabla de Auditoría (Movimientos de este mes)
                        doc.add(Paragraph("HISTORIAL DE PAGOS DEL MES").setBold().setFontSize(10f).setFontColor(primaryColor))
                        
                        val mainTable = Table(UnitValue.createPercentArray(floatArrayOf(15f, 30f, 25f, 15f, 15f))).useAllAvailableWidth()
                        mainTable.addHeaderCell(Cell().add(Paragraph("FECHA").setBold().setFontSize(8f).setFontColor(ColorConstants.WHITE)).setBackgroundColor(primaryColor))
                        mainTable.addHeaderCell(Cell().add(Paragraph("CLIENTE").setBold().setFontSize(8f).setFontColor(ColorConstants.WHITE)).setBackgroundColor(primaryColor))
                        mainTable.addHeaderCell(Cell().add(Paragraph("PRODUCTO").setBold().setFontSize(8f).setFontColor(ColorConstants.WHITE)).setBackgroundColor(primaryColor))
                        mainTable.addHeaderCell(Cell().add(Paragraph("MÉTODO").setBold().setFontSize(8f).setFontColor(ColorConstants.WHITE)).setBackgroundColor(primaryColor))
                        mainTable.addHeaderCell(Cell().add(Paragraph("MONTO").setBold().setFontSize(8f).setFontColor(ColorConstants.WHITE)).setBackgroundColor(primaryColor).setTextAlignment(TextAlignment.RIGHT))

                        pagos.filter {
                            val c = Calendar.getInstance().apply { time = it.fecha.toDate() }
                            c.get(Calendar.YEAR) == anioActual && c.get(Calendar.MONTH) == mesActual
                        }.sortedByDescending { it.fecha }.forEach { pago ->
                            val alq = alquileres.find { it.id == pago.alquilerId }
                            mainTable.addCell(Cell().add(Paragraph(SimpleDateFormat("dd/MM/yy", Locale.US).format(pago.fecha.toDate())).setFontSize(8f)))
                            mainTable.addCell(Cell().add(Paragraph(alq?.clienteNombre ?: "N/A").setFontSize(8f)))
                            mainTable.addCell(Cell().add(Paragraph(alq?.itemNombre ?: "Varios").setFontSize(8f)))
                            mainTable.addCell(Cell().add(Paragraph(pago.metodoPago.name).setFontSize(8f)))
                            mainTable.addCell(Cell().add(Paragraph("S/. ${String.format(Locale.US, "%.2f", pago.monto)}")).setTextAlignment(TextAlignment.RIGHT).setFontSize(8f))
                        }

                        doc.add(mainTable)

                        // 4. Pie de página
                        doc.add(Paragraph("\nReporte generado por RAYMI SaaS el ${dateFormat.format(Date())}")
                            .setFontSize(7f).setItalic().setFontColor(ColorConstants.GRAY).setTextAlignment(TextAlignment.RIGHT))
                    }
                }
            }
        }
    }
}
