package com.raymi.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.raymi.app.R
import com.raymi.app.models.Vestuario

class VestuarioAdapter(
    private var lista: MutableList<Vestuario>,
    private val onEliminarClick: (Vestuario) -> Unit
) : RecyclerView.Adapter<VestuarioAdapter.VestuarioViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VestuarioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vestuario, parent, false)
        return VestuarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: VestuarioViewHolder, position: Int) {
        val vestuario = lista[position]

        holder.tvDanza.text = "Danza: ${vestuario.danza}"
        holder.tvDepartamento.text = "Departamento: ${vestuario.departamento}"

        holder.btnEliminar.setOnClickListener {
            onEliminarClick(vestuario)
        }
    }

    override fun getItemCount(): Int = lista.size

    fun actualizarLista(nuevaLista: MutableList<Vestuario>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }

    class VestuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvDanza: TextView = itemView.findViewById(R.id.tvDanzaVestuario)
        val tvDepartamento: TextView = itemView.findViewById(R.id.tvDepartamentoVestuario)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminarVestuario)
    }
}
