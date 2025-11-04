package com.raymi.app.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.raymi.app.R
import com.raymi.app.adapters.VestuarioAdapter
import com.raymi.app.models.Vestuario

class VestuariosFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VestuarioAdapter
    private lateinit var btnAgregarVestuario: FloatingActionButton
    private val db = FirebaseFirestore.getInstance()
    private var listaVestuario = mutableListOf<Vestuario>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_vestuarios, container, false)

        recyclerView = view.findViewById(R.id.rvVestuario)
        btnAgregarVestuario = view.findViewById(R.id.btnAgregarVestuario)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = VestuarioAdapter(listaVestuario) { vestuario ->
            eliminarVestuario(vestuario)
        }
        recyclerView.adapter = adapter

        btnAgregarVestuario.setOnClickListener {
            mostrarDialogoAgregar()
        }

        cargarVestuario()
        return view
    }

    private fun cargarVestuario() {
        db.collection("vestuarios").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener

            listaVestuario.clear()
            for (doc in snapshot) {
                val vestuario = Vestuario(
                    id = doc.id,
                    departamento = doc.getString("departamento") ?: "",
                    danza = doc.getString("danza") ?: ""
                )
                listaVestuario.add(vestuario)
            }
            adapter.actualizarLista(listaVestuario)
        }
    }

    private fun mostrarDialogoAgregar() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Nuevo vestuario")

        val inputDanza = EditText(requireContext())
        inputDanza.hint = "Nombre de la danza"

        // Spinner para departamentos
        val spinnerDepartamento = Spinner(requireContext())
        val departamentos = resources.getStringArray(R.array.departamentos_peru)
        val adapterSpinner = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, departamentos)
        spinnerDepartamento.adapter = adapterSpinner

        val layoutContainer = LinearLayout(requireContext())
        layoutContainer.orientation = LinearLayout.VERTICAL
        layoutContainer.setPadding(48, 16, 48, 0)
        layoutContainer.addView(inputDanza)
        layoutContainer.addView(spinnerDepartamento)

        builder.setView(layoutContainer)

        builder.setPositiveButton("Guardar") { _, _ ->
            val danza = inputDanza.text.toString().trim()
            val departamento = spinnerDepartamento.selectedItem.toString()

            if ( danza.isEmpty()) {
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            verificarDuplicadoYGuardar(danza, departamento)
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun verificarDuplicadoYGuardar( danza: String, departamento: String) {
        db.collection("vestuarios")

            .whereEqualTo("danza", danza)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    Toast.makeText(requireContext(), "Ya existe un vestuario con ese nombre y danza", Toast.LENGTH_SHORT).show()
                } else {
                    val nuevoVestuario = hashMapOf(
                        "departamento" to departamento,
                        "danza" to danza
                    )
                    db.collection("vestuarios").add(nuevoVestuario)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Vestuario agregado correctamente", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Error al agregar", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun eliminarVestuario(vestuario: Vestuario) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar vestuario")
            .setMessage("¿Deseas eliminar '${vestuario.danza}' de ${vestuario.departamento}?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.collection("vestuarios").document(vestuario.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Vestuario eliminado", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Error al eliminar", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
