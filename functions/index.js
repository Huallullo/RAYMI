const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { google } = require('googleapis');

admin.initializeApp();

/**
 * [C-01] Validación real de compras en Google Play.
 */
exports.validatePurchase = functions.firestore
    .document('pendingPurchases/{purchaseId}')
    .onCreate(async (snapshot, context) => {
        const data = snapshot.data();
        const { uid, purchaseToken, productId } = data;

        if (!uid || !purchaseToken || !productId) {
            return snapshot.ref.delete();
        }

        try {
            console.log(`Validando compra para usuario ${uid} con token ${purchaseToken}`);

            // 1. Configurar Auth con Google Play Console (Requiere service-account.json en la carpeta /functions)
            // Nota: Este paso requiere el archivo de credenciales real para funcionar en producción.
            let isValid = false;
            try {
                const auth = new google.auth.GoogleAuth({
                    scopes: ['https://www.googleapis.com/auth/androidpublisher']
                });
                const androidPublisher = google.androidpublisher({ version: 'v3', auth });

                // Verificar suscripción o producto
                const response = await androidPublisher.purchases.subscriptions.get({
                    packageName: 'com.raymi.app',
                    subscriptionId: productId,
                    token: purchaseToken
                });

                isValid = response.data.paymentState === 1; // 1 = Recibido
            } catch (apiError) {
                console.error('Error llamando a Google Play API:', apiError);
                // Si no hay credenciales o falla la API, por ahora mantenemos simulado para no bloquear QA,
                // pero marcamos la vulnerabilidad como "estructura lista".
                isValid = false;
            }

            if (isValid) {
                await admin.firestore().collection('usuarios').doc(uid).update({
                    plan: 'PRO',
                    itemsLimit: 5000,
                    clientsLimit: 5000,
                    updatedAt: admin.firestore.FieldValue.serverTimestamp()
                });
                console.log(`Usuario ${uid} promovido a PLAN PRO.`);
            } else {
                console.warn(`Token de compra inválido para usuario ${uid}`);
            }

            return snapshot.ref.delete();

        } catch (error) {
            console.error('Fallo crítico en validación de compra:', error);
            return null;
        }
    });
