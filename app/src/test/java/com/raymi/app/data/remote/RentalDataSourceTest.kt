package com.raymi.app.data.remote

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class RentalDataSourceTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var rentalDataSource: RentalDataSource
    private val workspaceId = "test-ws"
    private val itemId = "item-123"
    private val alquilerId = "alq-456"

    @Before
    fun setup() {
        firestore = mockk(relaxed = true)
        rentalDataSource = RentalDataSource(firestore)
    }

    @Test
    fun `registrarDevolucionTransactional should update item and alquiler state`() = runBlocking {
        // Arrange
        val transaction = mockk<Transaction>(relaxed = true)
        val negociosRef = mockk<CollectionReference>()
        val negocioRef = mockk<DocumentReference>()
        val alquileresRef = mockk<CollectionReference>()
        val itemsRef = mockk<CollectionReference>()
        val metadataRef = mockk<CollectionReference>()
        val alquilerRef = mockk<DocumentReference>(relaxed = true)
        val itemRef = mockk<DocumentReference>(relaxed = true)
        val statsRef = mockk<DocumentReference>(relaxed = true)
        val alquilerSnap = mockk<DocumentSnapshot>(relaxed = true)
        val itemSnap = mockk<DocumentSnapshot>(relaxed = true)

        every { firestore.collection("negocios") } returns negociosRef
        every { negociosRef.document(workspaceId) } returns negocioRef
        every { negocioRef.collection("alquileres") } returns alquileresRef
        every { negocioRef.collection("items") } returns itemsRef
        every { negocioRef.collection("metadata") } returns metadataRef
        every { alquileresRef.document(alquilerId) } returns alquilerRef
        every { itemsRef.document(itemId) } returns itemRef
        every { metadataRef.document("stats") } returns statsRef

        every { transaction.get(alquilerRef) } returns alquilerSnap
        every { transaction.get(itemRef) } returns itemSnap
        every { alquilerSnap.exists() } returns true
        every { alquilerSnap.get("items") } returns listOf(mapOf("itemId" to itemId, "cantidad" to 1))
        every { alquilerSnap.get("cantidad") } returns 1
        every { alquilerSnap.get("garantia") } returns 0.0
        every { alquilerSnap.get("saldo") } returns 50.0
        every { alquilerSnap.get("fechaDevolucion") } returns null
        every { itemSnap.exists() } returns true
        every { itemSnap.get("cantidad") } returns 5
        every { itemSnap.get("unidadesAlquiladas") } returns 1L
        every { itemSnap.getString("estado") } returns "ALQUILADO"
        every { alquilerSnap.getString("observaciones") } returns "Nota previa"

        val transactionSlot = slot<Transaction.Function<Any>>()
        every { firestore.runTransaction<Any>(capture(transactionSlot)) } answers {
            val result = transactionSlot.captured.apply(transaction)
            Tasks.forResult(result)
        }

        // Act
        rentalDataSource.registrarDevolucionTransactional(workspaceId, alquilerId, 10.0, "Dañado")

        // Assert
        verify { transaction.update(alquilerRef, any<Map<String, Any>>()) }
        verify { transaction.update(itemRef, any<Map<String, Any>>()) }
        verify(atLeast = 1) { transaction.update(statsRef, any<Map<String, Any>>()) }
    }
}
