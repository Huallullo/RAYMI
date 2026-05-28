package com.raymi.app.data.remote

import com.google.android.gms.tasks.Task
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
        val alquilerRef = mockk<DocumentReference>(relaxed = true)
        val itemRef = mockk<DocumentReference>(relaxed = true)
        val statsRef = mockk<DocumentReference>(relaxed = true)
        val alquilerSnap = mockk<DocumentSnapshot>(relaxed = true)
        val itemSnap = mockk<DocumentSnapshot>(relaxed = true)
        val mockTask = mockk<Task<Unit>>(relaxed = true)

        every { firestore.collection("negocios").document(workspaceId).collection("alquileres").document(alquilerId) } returns alquilerRef
        every { firestore.collection("negocios").document(workspaceId).collection("items").document(itemId) } returns itemRef
        every { firestore.collection("negocios").document(workspaceId).collection("metadata").document("stats") } returns statsRef
        
        every { transaction.get(alquilerRef) } returns alquilerSnap
        every { transaction.get(itemRef) } returns itemSnap
        every { alquilerSnap.exists() } returns true
        every { alquilerSnap.getString("itemId") } returns itemId
        every { itemSnap.exists() } returns true
        every { itemSnap.get("unidadesAlquiladas") } returns 1L
        every { alquilerSnap.getString("observaciones") } returns "Nota previa"

        val transactionSlot = slot<Transaction.Function<Unit>>()
        every { firestore.runTransaction<Unit>(capture(transactionSlot)) } answers {
            transactionSlot.captured.apply(transaction)
            mockTask
        }

        // Act
        rentalDataSource.registrarDevolucionTransactional(workspaceId, alquilerId, 10.0, "Dañado")

        // Assert
        verify { transaction.update(alquilerRef, any<Map<String, Any>>()) }
        verify { transaction.update(itemRef, any<Map<String, Any>>()) }
        verify { transaction.update(statsRef, "alquileresActivos", any()) }
    }
}
