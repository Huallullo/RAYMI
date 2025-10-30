package com.raymi.app

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class DashboardActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var tvClientesCount: TextView
    private lateinit var tvVestuariosCount: TextView
    private lateinit var tvAlquileresCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        db = FirebaseFirestore.getInstance()
        tvClientesCount = findViewById(R.id.tvClientesCount)
        tvVestuariosCount = findViewById(R.id.tvVestuariosCount)
        tvAlquileresCount = findViewById(R.id.tvAlquileresCount)

        loadMetrics()
    }

    private fun loadMetrics() {
        // Contar clientes
        db.collection("clientes").get()
            .addOnSuccessListener { docs ->
                tvClientesCount.text = docs.size().toString()
            }
            .addOnFailureListener { e ->
                Log.e("Dashboard", "Error clientes: ${e.message}")
            }

        // Contar vestuarios
        db.collection("vestuarios").get()
            .addOnSuccessListener { docs ->
                tvVestuariosCount.text = docs.size().toString()
            }
            .addOnFailureListener { e ->
                Log.e("Dashboard", "Error vestuarios: ${e.message}")
            }

        // Contar alquileres activos
        db.collection("alquileres").whereEqualTo("estado", "Activo").get()
            .addOnSuccessListener { docs ->
                tvAlquileresCount.text = docs.size().toString()
            }
            .addOnFailureListener { e ->
                Log.e("Dashboard", "Error alquileres: ${e.message}")
            }
    }
}
