package com.raymi.app.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.raymi.app.R
import com.raymi.app.adapters.ClienteAdapter
import com.raymi.app.models.Cliente

class ClientesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ClienteAdapter
    private lateinit var btnAgregarCliente: FloatingActionButton
    private val db = FirebaseFirestore.getInstance()
    private var listaClientes = mutableListOf<Cliente>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_clientes, container, false)

        recyclerView = view.findViewById(R.id.rvClientes)
        btnAgregarCliente = view.findViewById(R.id.btnAgregarCliente)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ClienteAdapter(listaClientes) { cliente ->
            eliminarCliente(cliente)
        }
        recyclerView.adapter = adapter

        btnAgregarCliente.setOnClickListener {
            mostrarDialogoAgregar()
        }

        cargarClientes()
        return view
    }

    private fun cargarClientes() {
        db.collection("clientes").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener

            listaClientes.clear()
            for (doc in snapshot) {
                val cliente = Cliente(
                    id = doc.id,
                    dni = doc.getString("dni") ?: "",
                    nombre = doc.getString("nombre") ?: "",
                    telefono = doc.getString("telefono") ?: ""
                )
                listaClientes.add(cliente)
            }
            adapter.actualizarLista(listaClientes)
        }
    }

    private fun mostrarDialogoAgregar() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Nuevo cliente")

        // Crear los EditText
        val inputDni = EditText(requireContext())
        val inputNombre = EditText(requireContext())
        val inputTelefono = EditText(requireContext())

        inputDni.hint = "DNI (8 dígitos)"
        inputDni.inputType = InputType.TYPE_CLASS_NUMBER
        inputDni.filters = arrayOf(InputFilter.LengthFilter(8))

        inputNombre.hint = "Nombre completo"


        inputTelefono.hint = "Teléfono (9 digitos)"
        inputTelefono.inputType = InputType.TYPE_CLASS_PHONE
        inputTelefono.filters = arrayOf(InputFilter.LengthFilter(9))

        // Crear el contenedor LinearLayout
        val layoutContainer = LinearLayout(requireContext())
        layoutContainer.orientation = LinearLayout.VERTICAL
        layoutContainer.setPadding(48, 16, 48, 0)
        layoutContainer.addView(inputDni)
        layoutContainer.addView(inputNombre)
        layoutContainer.addView(inputTelefono)

        builder.setView(layoutContainer)

        builder.setPositiveButton("Guardar") { _, _ ->
            val dni = inputDni.text.toString().trim()
            val nombre = inputNombre.text.toString().trim()
            val telefono = inputTelefono.text.toString().trim()

            if (dni.length != 8) {
                Toast.makeText(requireContext(), "El DNI debe tener 8 dígitos", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            if (nombre.isEmpty() || telefono.isEmpty()) {
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            if (telefono.length != 9) {
            Toast.makeText(requireContext(), "El telefono debe tener 9 dígitos", Toast.LENGTH_SHORT).show()
            return@setPositiveButton
        }

            verificarDuplicadoYGuardar(dni, nombre, telefono)
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun verificarDuplicadoYGuardar(dni: String, nombre: String, telefono: String) {
        db.collection("clientes")
            .whereEqualTo("dni", dni)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    Toast.makeText(requireContext(), "Ya existe un cliente con ese DNI", Toast.LENGTH_SHORT).show()
                } else {
                    val nuevoCliente = hashMapOf(
                        "dni" to dni,
                        "nombre" to nombre,
                        "telefono" to telefono
                    )
                    db.collection("clientes").add(nuevoCliente)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Cliente agregado correctamente", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Error al agregar", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun eliminarCliente(cliente: Cliente) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar cliente")
            .setMessage("¿Deseas eliminar a ${cliente.nombre}?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.collection("clientes").document(cliente.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Cliente eliminado", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Error al eliminar", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}