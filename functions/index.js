const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

/**
 * TAREA 15: Validación server-side de compras.
 * Se dispara cuando se guarda un token en 'pendingPurchases/'.
 */
exports.validatePurchase = functions.firestore
    .document('pendingPurchases/{purchaseId}')
    .onCreate(async (snapshot, context) => {
        const data = snapshot.data();
        const { uid, purchaseToken, productId } = data;

        if (!uid || !purchaseToken) {
            return snapshot.ref.delete();
        }

        try {
            console.log(`Validando compra para usuario ${uid} con token ${purchaseToken}`);

            // NOTA: En un entorno real, aquí se integraría googleapis para consultar
            // la Google Play Developer API: androidPublisher.purchases.products.get

            // Simulación de validación exitosa
            const isValid = true;

            if (isValid) {
                // 1. Actualizar el perfil del usuario a PRO
                await admin.firestore().collection('usuarios').doc(uid).update({
                    plan: 'PRO',
                    itemsLimit: 5000, // Límite extendido para suscriptores
                    updatedAt: admin.firestore.FieldValue.serverTimestamp()
                });

                console.log(`Usuario ${uid} promovido a PLAN PRO satisfactoriamente.`);
            } else {
                console.warn(`Token de compra inválido para usuario ${uid}`);
            }

            // 2. Eliminar el documento de la cola de pendientes independientemente del resultado
            return snapshot.ref.delete();

        } catch (error) {
            console.error('Fallo crítico en validación de compra:', error);
            // Si hay error técnico, mantenemos el documento para re-intento o debugging manual
            return null;
        }
    });
