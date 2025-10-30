package com.raymi.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var tvClientesCount: TextView
    private lateinit var tvVestuariosCount: TextView
    private lateinit var tvAlquileresCount: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        db = FirebaseFirestore.getInstance()
        tvClientesCount = findViewById(R.id.tvClientesCount)
        tvVestuariosCount = findViewById(R.id.tvVestuariosCount)
        tvAlquileresCount = findViewById(R.id.tvAlquileresCount)
        auth = FirebaseAuth.getInstance()

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

    override fun onStop() {
        super.onStop()
        // Cerrar sesión cuando el usuario deja de usar la app
        FirebaseAuth.getInstance().signOut()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cerrar sesión si la actividad se destruye
        FirebaseAuth.getInstance().signOut()
    }

    // Opcional: Si tienes un botón para cerrar sesión en el dashboard
    private fun logOut() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
