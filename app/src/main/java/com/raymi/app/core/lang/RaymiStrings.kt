package com.raymi.app.core.lang

import androidx.compose.runtime.staticCompositionLocalOf

interface RaymiStrings {
    val appName: String
    val dashboard: String
    val clients: String
    val inventory: String
    val rentals: String
    val history: String
    val profile: String
    
    // Dashboard
    val monthlyEarnings: String
    val totalEarnings: String
    val todayDeliveries: String
    val todayReturns: String
    val pendingPayments: String
    val weeklyActivity: String
    val operationalStatus: String
    val rented: String
    val activeClients: String
    val newRental: String
    val newClient: String
    
    // Profile
    val myBusiness: String
    val myBusinessSub: String
    val changeBusiness: String
    val changeBusinessSub: String
    val personalData: String
    val personalDataSub: String
    val subscription: String
    val subscriptionSub: String
    val security: String
    val securitySub: String
    val helpCenter: String
    val helpCenterSub: String
    val logout: String
    val statusAccount: String
    val proBusiness: String
    val basicPlan: String
    val bePro: String
    
    // Inventory / Items
    val addItem: String
    val newItem: String 
    val editItem: String
    val categories: String
    val itemName: String
    val skuCode: String
    val price: String
    val stock: String
    val category: String
    val specifications: String
    val addField: String
    val globalInventory: String
    val centralizedManagement: String
    val searchPlaceholder: String
    val all: String
    val emptyInventory: String
    val emptyInventoryDesc: String
    val searchNoResults: String
    val showMoreProducts: String
    val categoryRequiredTitle: String
    val categoryRequiredDesc: String
    val createCategoryNow: String
    val units: String
    val addPhoto: String
    val imageReady: String
    val selectCategory: String
    val basicInfo: String
    val newField: String
    val isNumeric: String
    val add: String
    
    // Clients
    val addClient: String
    val editClient: String
    val dni: String
    val phone: String
    val email: String
    val address: String
    val searchClient: String
    val registeredContacts: String
    val searchClientPlaceholder: String
    val searchHint: String
    val recent: String
    val emptyClients: String
    val emptyClientsDesc: String
    val registerNow: String
    val showMoreClients: String
    val idDocument: String
    val names: String
    val surnames: String
    val optional: String
    val saveClient: String
    
    // Rentals
    val createRental: String
    val selectClient: String
    val selectItem: String
    val rentalPeriod: String
    val startDate: String
    val endDate: String
    val totalRental: String
    val advance: String
    val balance: String
    val guarantee: String
    val paymentMethod: String
    val initialStatus: String
    val notes: String
    val confirmRental: String
    val rentalsManagement: String
    val rentalDesc: String
    val filter: String
    val filterByStatus: String
    val noMovements: String
    val noMovementsDesc: String
    val active: String
    val reserved: String
    val duration: String
    val days: String
    
    // History
    val accountingHistory: String
    val totalRevenue: String
    val movements: String
    val searchHistory: String
    val compilingRecords: String
    val noRecords: String
    val noRecordsDesc: String
    val update: String
    val exportCsv: String
    val exportInventory: String
    
    // Billing
    val generateReceipt: String
    val receiptType: String
    val ticket: String
    val invoice: String
    val bill: String
    val shareReceipt: String
    val voidReceipt: String
    val generatedReceipts: String
    val noReceipts: String
    val duplicateReceiptWarning: String
    val continueText: String
    val registerAbono: String
    val abonoDesc: String
    val operationNumber: String
    val confirmPayment: String

    // Workspace Selection
    val welcome: String
    val selectBusiness: String
    val registerNewBusiness: String
    val limitReachedTitle: String
    val limitReachedDesc: String
    val viewProPlans: String
    val noBusinessesYet: String
    val startManagingToday: String

    // Common / Notifications
    val search: String
    val save: String
    val cancel: String
    val edit: String
    val delete: String
    val loading: String
    val error: String
    val success: String
    val language: String
    val adTitle: String
    val close: String
    val back: String
    val understood: String
    
    // Auth
    val loginTitle: String
    val loginSubtitle: String
    val registerTitle: String
    val registerSubtitle: String
    val emailLabel: String
    val passwordLabel: String
    val businessNameLabel: String
    val loginButton: String
    val registerButton: String
    val noAccount: String
    val hasAccount: String
    val goToRegister: String
    val goToLogin: String
    val forgotPassword: String
    val syncCloud: String
    val botChallenge: String
    val solveChallenge: String
    val invalidChallenge: String
    val iamNotARobot: String
    val botOpPlus: String
    val botOpMinus: String
    val botOpMult: String
    val numberWords: List<String>
    
    // Error Messages
    val errorNetwork: String
    val errorGeneric: String
    val errorUnauthorized: String
    val errorInvalidEmail: String
    val errorWrongPassword: String
    val errorPendingBalance: String
    val errorInsufficientStock: String
    val errorLimitReached: String
    val errorFillAll: String
    val errorDniLength: String
    val errorNamesRequired: String
    val errorSurnamesRequired: String
    val errorPhoneLength: String
    
    // Success Messages
    val successSave: String
    val successDelete: String
    val successUpdate: String
    val successPayment: String
    
    // Auditoría de Localización
    val permissionDenied: String
    val cameraPermissionDesc: String
    val openSettings: String
    val gpsDisabled: String
    val gpsDisabledDesc: String
    val noConnection: String
    val noConnectionDesc: String
    val clientProfile: String
    val identityBackup: String
    val contactData: String
    val unitPrice: String
    val overdueDetected: String
    val registerPayment: String
    val amountToPay: String
    val selectLanguage: String
    val forCollecting: String
    val suggestion: String
    val note: String
    val returnText: String
    val ok: String
    val total: String
    val paid: String
    val maintenanceHistory: String
    val registerMaintenance: String
    val reason: String
    val cost: String
    val responsible: String
    val description: String
    val finalState: String
    val idFront: String
    val idBack: String
    val facePhoto: String
    val idNotFound: String
    val deleteClient: String
    val transactions: String
    val afterPayment: String
    val newAdvance: String
    val newBalance: String
    val fullPaymentMessage: String
    val subtotal: String
    val debt: String
    val businessNameInternal: String
    val tradeName: String
    val taxId: String
    val businessDescription: String
    val googleMapsLink: String
    val businessCurrency: String
    val legalPolicies: String
    val termsConditions: String
    val penaltyPolicy: String
    val internalPolicyMessage: String
    val brandIdentity: String
    val businessLogo: String
    val financeRegion: String
    val rentalOverdueMessage: String
    val noInternetConnection: String
}

class SpanishStrings : RaymiStrings {
    override val appName = "RAYMI"
    override val dashboard = "Inicio"
    override val clients = "Clientes"
    override val inventory = "Inventario"
    override val rentals = "Alquileres"
    override val history = "Historial"
    override val profile = "Perfil"
    
    override val monthlyEarnings = "Ingresos del Mes"
    override val totalEarnings = "Total histórico"
    override val todayDeliveries = "Entregas"
    override val todayReturns = "Retornos"
    override val pendingPayments = "Cobros"
    override val weeklyActivity = "Actividad últimos 7 días"
    override val operationalStatus = "Estado Operativo"
    override val rented = "Alquilados"
    override val activeClients = "Clientes Activos"
    override val newRental = "Nuevo Alquiler"
    override val newClient = "Nuevo Cliente"

    override val myBusiness = "Mi Negocio"
    override val myBusinessSub = "Personaliza nombre, moneda y rubro"
    override val changeBusiness = "Cambiar de Negocio"
    override val changeBusinessSub = "Gestiona múltiples locales (PRO)"
    override val personalData = "Datos Personales"
    override val personalDataSub = "Nombre, correo y teléfono"
    override val subscription = "Suscripción y Pagos"
    override val subscriptionSub = "Gestiona tu plan PRO y facturas"
    override val security = "Seguridad"
    override val securitySub = "Contraseña y autenticación"
    override val helpCenter = "Manual de Usuario"
    override val helpCenterSub = "Guías y tutoriales de uso"
    override val logout = "Cerrar Sesión Segura"
    override val statusAccount = "ESTATUS DE CUENTA"
    override val proBusiness = "PRO BUSINESS"
    override val basicPlan = "PLAN BÁSICO"
    override val bePro = "Ser PRO"

    override val addItem = "Nuevo Producto"
    override val newItem = "Nuevo"
    override val editItem = "Editar Producto"
    override val categories = "Categorías"
    override val itemName = "Nombre del Ítem"
    override val skuCode = "Código / SKU"
    override val price = "Precio"
    override val stock = "Stock"
    override val category = "Categoría"
    override val specifications = "Especificaciones"
    override val addField = "Añadir Campo"
    override val globalInventory = "Inventario Global"
    override val centralizedManagement = "Gestión centralizada de activos"
    override val searchPlaceholder = "Nombre, código o marca..."
    override val all = "Todos"
    override val emptyInventory = "Inventario Vacío"
    override val emptyInventoryDesc = "Comienza agregando los productos que alquilas."
    override val searchNoResults = "No hay resultados para tu búsqueda."
    override val showMoreProducts = "Ver más productos"
    override val categoryRequiredTitle = "Categoría Requerida"
    override val categoryRequiredDesc = "Para registrar un producto, primero debes definir al menos una categoría (Ej: Vestidos, Herramientas, etc.)."
    override val createCategoryNow = "Crear Categoría Ahora"
    override val units = "und."
    override val addPhoto = "Añadir Foto del Producto"
    override val imageReady = "Imagen lista"
    override val selectCategory = "Selecciona una Categoría"
    override val basicInfo = "Información Básica"
    override val newField = "Nuevo Campo"
    override val isNumeric = "Es numérico"
    override val add = "Añadir"

    override val addClient = "Nuevo Cliente"
    override val editClient = "Editar Cliente"
    override val dni = "DNI / Documento"
    override val phone = "Teléfono / WhatsApp"
    override val email = "Correo Electrónico"
    override val address = "Dirección"
    override val searchClient = "Buscar Cliente"
    override val registeredContacts = "contactos registrados"
    override val searchClientPlaceholder = "Nombre o DNI del cliente..."
    override val searchHint = "Escribe al menos 2 caracteres para búsqueda rápida"
    override val recent = "Recientes"
    override val emptyClients = "Sin Contactos"
    override val emptyClientsDesc = "Comienza a registrar clientes para tu negocio."
    override val registerNow = "Registrar Ahora"
    override val showMoreClients = "Ver más clientes"
    override val idDocument = "DNI / Documento"
    override val names = "Nombres"
    override val surnames = "Apellidos"
    override val optional = "(Opcional)"
    override val saveClient = "Guardar Cliente"

    override val createRental = "Registrar Alquiler"
    override val selectClient = "Seleccionar Cliente"
    override val selectItem = "Seleccionar Producto"
    override val rentalPeriod = "Periodo de Alquiler"
    override val startDate = "Entrega"
    override val endDate = "Devolución"
    override val totalRental = "Total Alquiler"
    override val advance = "Adelanto"
    override val balance = "Saldo Pendiente"
    override val guarantee = "Garantía"
    override val paymentMethod = "Método de Pago"
    override val initialStatus = "Estado Inicial"
    override val notes = "Notas / Observaciones"
    override val confirmRental = "Confirmar Alquiler"
    override val rentalsManagement = "Alquileres"
    override val rentalDesc = "Control de préstamos y devoluciones"
    override val filter = "Filtrar"
    override val filterByStatus = "Filtrar por Estado"
    override val noMovements = "Sin Movimientos"
    override val noMovementsDesc = "No hay alquileres que coincidan con la búsqueda."
    override val active = "Activo"
    override val reserved = "Reserva"
    override val duration = "Duración"
    override val days = "días"

    override val accountingHistory = "Historial Contable"
    override val totalRevenue = "RECAUDACIÓN TOTAL"
    override val movements = "MOVIMIENTOS"
    override val searchHistory = "Buscar en el historial..."
    override val compilingRecords = "Compilando registros..."
    override val noRecords = "Sin Registros"
    override val noRecordsDesc = "No se encontraron movimientos cerrados."
    override val update = "Actualizar"
    override val exportCsv = "Exportar CSV"
    override val exportInventory = "Exportar Inventario"

    override val generateReceipt = "Generar Comprobante"
    override val receiptType = "Tipo de Comprobante"
    override val ticket = "Ticket"
    override val invoice = "Factura"
    override val bill = "Boleta"
    override val shareReceipt = "Compartir Comprobante"
    override val voidReceipt = "Anular Comprobante"
    override val generatedReceipts = "Comprobantes Generados"
    override val noReceipts = "No se han generado comprobantes para este alquiler."
    override val duplicateReceiptWarning = "Este alquiler ya tiene comprobantes generados. ¿Deseas generar uno nuevo de todos modos?"
    override val continueText = "Continuar"
    override val registerAbono = "Registrar Abono"
    override val abonoDesc = "Ingresa el monto a pagar y selecciona el medio."
    override val operationNumber = "N° Operación / Referencia"
    override val confirmPayment = "Confirmar Pago"

    override val welcome = "Bienvenido"
    override val selectBusiness = "Selecciona tu negocio"
    override val registerNewBusiness = "Registrar Nuevo Negocio"
    override val limitReachedTitle = "Límite Alcanzado"
    override val limitReachedDesc = "Tu plan actual solo permite un negocio activo. Actualiza a PLAN PRO para gestionar múltiples centros de alquiler de forma centralizada."
    override val viewProPlans = "Ver Planes PRO"
    override val noBusinessesYet = "No tienes negocios aún"
    override val startManagingToday = "Empieza a gestionar tus alquileres hoy mismo."

    override val search = "Buscar"
    override val save = "Guardar"
    override val cancel = "Cancelar"
    override val edit = "Editar"
    override val delete = "Eliminar"
    override val loading = "Cargando..."
    override val error = "Error"
    override val success = "Éxito"
    override val language = "Idioma"
    override val adTitle = "Publicidad"
    override val close = "Cerrar"
    override val back = "Volver"
    override val understood = "Entendido"

    override val errorNetwork = "Error de red. Verifica tu conexión."
    override val errorGeneric = "Algo salió mal. Inténtalo de nuevo."
    override val errorUnauthorized = "No tienes permiso para esto."
    override val errorInvalidEmail = "El correo no es válido."
    override val errorWrongPassword = "La contraseña es incorrecta."
    override val errorPendingBalance = "Existe un saldo pendiente. Liquida la deuda primero."
    override val errorInsufficientStock = "Stock insuficiente para esta operación."
    override val errorLimitReached = "Límite del plan alcanzado."
    override val errorFillAll = "Por favor, completa todos los campos obligatorios."
    override val errorDniLength = "El DNI debe tener 8 dígitos"
    override val errorNamesRequired = "El nombre es requerido"
    override val errorSurnamesRequired = "Los apellidos son requeridos"
    override val errorPhoneLength = "El teléfono debe tener 9 dígitos"

    override val successSave = "Guardado con éxito."
    override val successDelete = "Eliminado correctamente."
    override val successUpdate = "Actualizado correctamente."
    override val successPayment = "Abono registrado con éxito."

    override val permissionDenied = "Permiso Denegado"
    override val cameraPermissionDesc = "Para tomar fotos del DNI o del cliente, RAYMI necesita acceso a la cámara."
    override val openSettings = "Abrir Ajustes"
    override val gpsDisabled = "GPS Desactivado"
    override val gpsDisabledDesc = "Para capturar tu ubicación exacta, necesitas activar el interruptor de 'Ubicación' en tu dispositivo."
    override val noConnection = "Sin Conexión"
    override val noConnectionDesc = "No hemos detectado una conexión a internet activa. Revisa tu WiFi o Datos Móviles."
    override val clientProfile = "Ficha del Cliente"
    override val identityBackup = "Respaldo de Identidad"
    override val contactData = "Datos de Contacto"
    override val unitPrice = "Unitario"
    override val overdueDetected = "Atraso detectado"
    override val registerPayment = "Registrar Pago"
    override val amountToPay = "Monto a Pagar"
    override val selectLanguage = "Seleccionar Idioma"
    override val forCollecting = "POR COBRAR"
    override val suggestion = "Sugerido"
    override val note = "Nota"
    override val returnText = "Retorno"
    override val ok = "Aceptar"
    override val total = "Total"
    override val paid = "Pagado"
    override val maintenanceHistory = "Historial de Mantenimiento"
    override val registerMaintenance = "Registrar Mantenimiento"
    override val reason = "Motivo"
    override val cost = "Costo"
    override val responsible = "Responsable"
    override val description = "Descripción"
    override val finalState = "Estado Final del Ítem"
    override val idFront = "DNI Frontal"
    override val idBack = "DNI Posterior"
    override val facePhoto = "Foto del Cliente (Rostro)"
    override val idNotFound = "DNI no encontrado"
    override val deleteClient = "Eliminar Cliente"
    override val transactions = "oper."
    override val afterPayment = "Después del pago:"
    override val newAdvance = "Nuevo adelanto:"
    override val newBalance = "Nuevo saldo:"
    override val fullPaymentMessage = "¡Pago completo! Ya puedes devolver el equipo."
    override val subtotal = "Subtotal"
    override val debt = "Debe"
    override val businessNameInternal = "Nombre del Negocio (Interno)"
    override val tradeName = "Nombre Comercial (Para Comprobantes)"
    override val taxId = "RUC del Negocio"
    override val businessDescription = "Descripción del Negocio"
    override val googleMapsLink = "Link de Google Maps (Ubicación)"
    override val businessCurrency = "Moneda del Negocio"
    override val legalPolicies = "Legal y Políticas"
    override val termsConditions = "Términos y Condiciones"
    override val penaltyPolicy = "Política de Penalidades"
    override val internalPolicyMessage = "* Las políticas legales son gestionadas internamente por RAYMI."
    override val brandIdentity = "Imagen de Marca"
    override val businessLogo = "Logo del Negocio"
    override val financeRegion = "Finanzas y Región"

    override val loginTitle = "Bienvenido de nuevo"
    override val loginSubtitle = "Accede a tu panel de control central"
    override val registerTitle = "Crea tu Cuenta"
    override val registerSubtitle = "Únete a la mejor gestión de alquileres"
    override val emailLabel = "Correo Electrónico"
    override val passwordLabel = "Contraseña"
    override val businessNameLabel = "Nombre de tu Negocio"
    override val loginButton = "ENTRAR AL SISTEMA"
    override val registerButton = "COMENZAR AHORA"
    override val noAccount = "¿No tienes un negocio?"
    override val hasAccount = "¿Ya tienes cuenta?"
    override val goToRegister = "Regístrate aquí"
    override val goToLogin = "Inicia Sesión"
    override val forgotPassword = "Recuperar mi contraseña"
    override val syncCloud = "Sincronizando con la nube..."
    override val botChallenge = "Verificación de Seguridad"
    override val solveChallenge = "¿Cuánto es %s %s %s?"
    override val invalidChallenge = "La respuesta es incorrecta. Inténtalo de nuevo."
    override val iamNotARobot = "No soy un robot"
    override val botOpPlus = "más"
    override val botOpMinus = "menos"
    override val botOpMult = "por"
    override val numberWords = listOf(
        "cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez",
        "once", "doce", "trece", "catorce", "quince", "dieciséis", "diecisiete", "dieciocho", "diecinueve", "veinte"
    )
    override val rentalOverdueMessage = "El alquiler de %s ha vencido."
    override val noInternetConnection = "Sin conexión a Internet"
}

class EnglishStrings : RaymiStrings {
    override val appName = "RAYMI"
    override val dashboard = "Home"
    override val clients = "Clients"
    override val inventory = "Inventory"
    override val rentals = "Rentals"
    override val history = "History"
    override val profile = "Profile"
    
    override val monthlyEarnings = "Monthly Income"
    override val totalEarnings = "Total Income"
    override val todayDeliveries = "Deliveries"
    override val todayReturns = "Returns"
    override val pendingPayments = "Payments"
    override val weeklyActivity = "Weekly Activity"
    override val operationalStatus = "Operational Status"
    override val rented = "Rented"
    override val activeClients = "Active Clients"
    override val newRental = "New Rental"
    override val newClient = "New Client"

    override val myBusiness = "My Business"
    override val myBusinessSub = "Customize name, currency and sector"
    override val changeBusiness = "Switch Business"
    override val changeBusinessSub = "Manage multiple locations (PRO)"
    override val personalData = "Personal Data"
    override val personalDataSub = "Name, email and phone"
    override val subscription = "Subscription & Payments"
    override val subscriptionSub = "Manage your PRO plan and invoices"
    override val security = "Security"
    override val securitySub = "Password and authentication"
    override val helpCenter = "User Manual"
    override val helpCenterSub = "Guides and usage tutorials"
    override val logout = "Secure Logout"
    override val statusAccount = "ACCOUNT STATUS"
    override val proBusiness = "PRO BUSINESS"
    override val basicPlan = "BASIC PLAN"
    override val bePro = "Go PRO"

    override val addItem = "New Product"
    override val newItem = "New"
    override val editItem = "Edit Product"
    override val categories = "Categories"
    override val itemName = "Item Name"
    override val skuCode = "Code / SKU"
    override val price = "Price"
    override val stock = "Stock"
    override val category = "Category"
    override val specifications = "Specifications"
    override val addField = "Add Field"
    override val globalInventory = "Global Inventory"
    override val centralizedManagement = "Centralized asset management"
    override val searchPlaceholder = "Name, code or brand..."
    override val all = "All"
    override val emptyInventory = "Empty Inventory"
    override val emptyInventoryDesc = "Start by adding the products you rent."
    override val searchNoResults = "No results found for your search."
    override val showMoreProducts = "See more products"
    override val categoryRequiredTitle = "Category Required"
    override val categoryRequiredDesc = "To register a product, you must first define at least one category (e.g., Dresses, Tools, etc.)."
    override val createCategoryNow = "Create Category Now"
    override val units = "units"
    override val addPhoto = "Add Product Photo"
    override val imageReady = "Image ready"
    override val selectCategory = "Select a Category"
    override val basicInfo = "Basic Information"
    override val newField = "New Field"
    override val isNumeric = "Is Numeric"
    override val add = "Add"

    override val addClient = "New Client"
    override val editClient = "Edit Client"
    override val dni = "DNI / ID Document"
    override val phone = "Phone / WhatsApp"
    override val email = "Email Address"
    override val address = "Address"
    override val searchClient = "Search Client"
    override val registeredContacts = "registered contacts"
    override val searchClientPlaceholder = "Client name or ID..."
    override val searchHint = "Type at least 2 characters for quick search"
    override val recent = "Recent"
    override val emptyClients = "No Contacts"
    override val emptyClientsDesc = "Start registering clients for your business."
    override val registerNow = "Register Now"
    override val showMoreClients = "See more clients"
    override val idDocument = "ID Document"
    override val names = "First Name"
    override val surnames = "Last Name"
    override val optional = "(Optional)"
    override val saveClient = "Save Client"

    override val createRental = "Register Rental"
    override val selectClient = "Select Client"
    override val selectItem = "Select Product"
    override val rentalPeriod = "Rental Period"
    override val startDate = "Delivery"
    override val endDate = "Return"
    override val totalRental = "Total Rental"
    override val advance = "Advance"
    override val balance = "Balance Due"
    override val guarantee = "Security Deposit"
    override val paymentMethod = "Payment Method"
    override val initialStatus = "Initial Status"
    override val notes = "Notes / Observations"
    override val confirmRental = "Confirm Rental"
    override val rentalsManagement = "Rentals"
    override val rentalDesc = "Loan and return control"
    override val filter = "Filter"
    override val filterByStatus = "Filter by Status"
    override val noMovements = "No Movements"
    override val noMovementsDesc = "No rentals matching your search."
    override val active = "Active"
    override val reserved = "Reserved"
    override val duration = "Duration"
    override val days = "days"

    override val accountingHistory = "Accounting History"
    override val totalRevenue = "TOTAL REVENUE"
    override val movements = "MOVEMENTS"
    override val searchHistory = "Search history..."
    override val compilingRecords = "Compiling records..."
    override val noRecords = "No Records"
    override val noRecordsDesc = "No closed movements found."
    override val update = "Update"
    override val exportCsv = "Export CSV"
    override val exportInventory = "Export Inventory"

    override val generateReceipt = "Generate Receipt"
    override val receiptType = "Receipt Type"
    override val ticket = "Ticket"
    override val invoice = "Invoice"
    override val bill = "Bill"
    override val shareReceipt = "Share Receipt"
    override val voidReceipt = "Void Receipt"
    override val generatedReceipts = "Generated Receipts"
    override val noReceipts = "No receipts generated for this rental."
    override val duplicateReceiptWarning = "This rental already has generated receipts. Do you want to generate a new one anyway?"
    override val continueText = "Continue"
    override val registerAbono = "Register Payment"
    override val abonoDesc = "Enter the amount to pay and select the method."
    override val operationNumber = "Operation N° / Reference"
    override val confirmPayment = "Confirm Payment"

    override val welcome = "Welcome"
    override val selectBusiness = "Select your business"
    override val registerNewBusiness = "Register New Business"
    override val limitReachedTitle = "Limit Reached"
    override val limitReachedDesc = "Your current plan only allows one active business. Upgrade to PRO PLAN to manage multiple rental centers centrally."
    override val viewProPlans = "View PRO Plans"
    override val noBusinessesYet = "No businesses yet"
    override val startManagingToday = "Start managing your rentals today."

    override val search = "Search"
    override val save = "Save"
    override val cancel = "Cancel"
    override val edit = "Edit"
    override val delete = "Delete"
    override val loading = "Loading..."
    override val error = "Error"
    override val success = "Success"
    override val language = "Language"
    override val adTitle = "Advertisement"
    override val close = "Close"
    override val back = "Back"
    override val understood = "Understood"

    override val errorNetwork = "Network error. Check your connection."
    override val errorGeneric = "Something went wrong. Please try again."
    override val errorUnauthorized = "You don't have permission for this."
    override val errorInvalidEmail = "Invalid email format."
    override val errorWrongPassword = "Incorrect password."
    override val errorPendingBalance = "There is a pending balance. Settle the debt first."
    override val errorInsufficientStock = "Insufficient stock for this operation."
    override val errorLimitReached = "Plan limit reached."
    override val errorFillAll = "Please fill in all required fields."
    override val errorDniLength = "ID must be 8 digits"
    override val errorNamesRequired = "First name is required"
    override val errorSurnamesRequired = "Last name is required"
    override val errorPhoneLength = "Phone must be 9 digits"

    override val successSave = "Saved successfully."
    override val successDelete = "Deleted successfully."
    override val successUpdate = "Updated successfully."
    override val successPayment = "Payment registered successfully."

    override val permissionDenied = "Permission Denied"
    override val cameraPermissionDesc = "To take photos of ID or client, RAYMI needs camera access."
    override val openSettings = "Open Settings"
    override val gpsDisabled = "GPS Disabled"
    override val gpsDisabledDesc = "To capture your exact location, you need to enable 'Location' on your device."
    override val noConnection = "No Connection"
    override val noConnectionDesc = "We haven't detected an active internet connection. Check your WiFi or Mobile Data."
    override val clientProfile = "Client Profile"
    override val identityBackup = "Identity Backup"
    override val contactData = "Contact Data"
    override val unitPrice = "Unit Price"
    override val overdueDetected = "Overdue detected"
    override val registerPayment = "Register Payment"
    override val amountToPay = "Amount to Pay"
    override val selectLanguage = "Select Language"
    override val forCollecting = "FOR COLLECTING"
    override val suggestion = "Suggested"
    override val note = "Note"
    override val returnText = "Return"
    override val ok = "OK"
    override val total = "Total"
    override val paid = "Paid"
    override val maintenanceHistory = "Maintenance History"
    override val registerMaintenance = "Register Maintenance"
    override val reason = "Reason"
    override val cost = "Cost"
    override val responsible = "Responsible"
    override val description = "Description"
    override val finalState = "Final Item State"
    override val idFront = "ID Front"
    override val idBack = "ID Back"
    override val facePhoto = "Client Photo (Face)"
    override val idNotFound = "ID not found"
    override val deleteClient = "Delete Client"
    override val transactions = "trans."
    override val afterPayment = "After payment:"
    override val newAdvance = "New advance:"
    override val newBalance = "New balance:"
    override val fullPaymentMessage = "Full payment! You can now return the equipment."
    override val subtotal = "Subtotal"
    override val debt = "Owes"
    override val businessNameInternal = "Business Name (Internal)"
    override val tradeName = "Trade Name (For Receipts)"
    override val taxId = "Tax ID / RUC"
    override val businessDescription = "Business Description"
    override val googleMapsLink = "Google Maps Link (Location)"
    override val businessCurrency = "Business Currency"
    override val legalPolicies = "Legal & Policies"
    override val termsConditions = "Terms and Conditions"
    override val penaltyPolicy = "Penalty Policy"
    override val internalPolicyMessage = "* Legal policies are managed internally by RAYMI."
    override val brandIdentity = "Brand Identity"
    override val businessLogo = "Business Logo"
    override val financeRegion = "Finance & Region"

    override val loginTitle = "Welcome back"
    override val loginSubtitle = "Access your central control panel"
    override val registerTitle = "Create your Account"
    override val registerSubtitle = "Join the best rental management"
    override val emailLabel = "Email Address"
    override val passwordLabel = "Password"
    override val businessNameLabel = "Your Business Name"
    override val loginButton = "LOGIN TO SYSTEM"
    override val registerButton = "START NOW"
    override val noAccount = "Don't have a business?"
    override val hasAccount = "Already have an account?"
    override val goToRegister = "Register here"
    override val goToLogin = "Sign In"
    override val forgotPassword = "Reset my password"
    override val syncCloud = "Syncing with cloud..."
    override val botChallenge = "Security Verification"
    override val solveChallenge = "What is %s %s %s?"
    override val invalidChallenge = "Incorrect answer. Try again."
    override val iamNotARobot = "I am not a robot"
    override val botOpPlus = "plus"
    override val botOpMinus = "minus"
    override val botOpMult = "times"
    override val numberWords = listOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty"
    )
    override val rentalOverdueMessage = "Rental for %s has expired."
    override val noInternetConnection = "No Internet Connection"
}

val LocalRaymiStrings = staticCompositionLocalOf<RaymiStrings> { SpanishStrings() }
