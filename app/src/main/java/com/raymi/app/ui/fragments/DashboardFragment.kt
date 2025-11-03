package com.raymi.app.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.raymi.app.LoginActivity
import com.raymi.app.R

class DashboardFragment : Fragment() {

    private lateinit var tvClientesCount: TextView
    private lateinit var tvVestuariosCount: TextView
    private lateinit var tvAlquileresCount: TextView
    private lateinit var btnLogout: ImageButton

    private val db = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Referencias a las vistas
        tvClientesCount = view.findViewById(R.id.tvClientesCount)
        tvVestuariosCount = view.findViewById(R.id.tvVestuariosCount)
        tvAlquileresCount = view.findViewById(R.id.tvAlquileresCount)
        btnLogout = view.findViewById(R.id.btnLogout)

        // Configurar botón de logout
        btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        // Cargar métricas
        cargarMetricas()

        return view
    }

    private fun cargarMetricas() {
        // Cargar clientes
        db.collection("clientes").get()
            .addOnSuccessListener { documents ->
                tvClientesCount.text = documents.size().toString()
            }
            .addOnFailureListener { e ->
                Log.e("Dashboard", "Error clientes: ${e.message}")
                tvClientesCount.text = "0"
            }

        // Cargar vestuarios
        db.collection("vestuarios").get()
            .addOnSuccessListener { documents ->
                tvVestuariosCount.text = documents.size().toString()
            }
            .addOnFailureListener { e ->
                Log.e("Dashboard", "Error vestuarios: ${e.message}")
                tvVestuariosCount.text = "0"
            }

        // Cargar alquileres activos
        db.collection("alquileres")
            .whereEqualTo("estado", "Activo")
            .get()
            .addOnSuccessListener { documents ->
                tvAlquileresCount.text = documents.size().toString()
            }
            .addOnFailureListener { e ->
                Log.e("Dashboard", "Error alquileres: ${e.message}")
                tvAlquileresCount.text = "0"
            }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun logout() {
        auth.signOut()
        Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show()

        // Redirigir al login y limpiar el stack
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}