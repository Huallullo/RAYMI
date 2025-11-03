package com.raymi.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.raymi.app.R
import com.raymi.app.models.Cliente

class ClienteAdapter(
    private var listaClientes: MutableList<Cliente>,
    private val onDelete: (Cliente) -> Unit
) : RecyclerView.Adapter<ClienteAdapter.ClienteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClienteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cliente, parent, false)
        return ClienteViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClienteViewHolder, position: Int) {
        val cliente = listaClientes[position]
        holder.tvNombre.text = cliente.nombre
        holder.tvDni.text = "DNI: ${cliente.dni}"
        holder.tvTelefono.text = "Tel: ${cliente.telefono}"

        holder.btnEliminar.setOnClickListener {
            onDelete(cliente)
        }
    }

    override fun getItemCount(): Int = listaClientes.size

    fun actualizarLista(nuevaLista: MutableList<Cliente>) {
        listaClientes = nuevaLista
        notifyDataSetChanged()
    }

    inner class ClienteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        val tvDni: TextView = view.findViewById(R.id.tvDni)
        val tvTelefono: TextView = view.findViewById(R.id.tvTelefono)
        val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminar)
    }
}
