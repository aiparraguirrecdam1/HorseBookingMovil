package com.example.horsebooking.Clases

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.horsebooking.R
import com.google.firebase.storage.FirebaseStorage

class ClasesAdapter(private val clasesList: List<Clase>, private val context: Context, private val listener: OnItemClickListener) :
    RecyclerView.Adapter<ClasesAdapter.ClaseViewHolder>() {
    private val storageReference = FirebaseStorage.getInstance().reference

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClaseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.clase_item, parent, false)
        return ClaseViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ClaseViewHolder, position: Int) {
        val clase = clasesList[position]
        holder.tituloTextView.text = clase.titulo
        holder.tipoClaseTextView.text = "Disciplina: " + clase.tipo
        holder.fechaInicioTextView.text = "Fecha de inicio: " + clase.fecha_inicio
        holder.fechaFinTextView.text = "Fecha de fin: " + clase.fecha_fin
        holder.horaClaseTextView.text = "Hora clase: " + clase.hora + ":" + clase.minuto

        holder.precioTextView.text = clase.precio.toString() + "€"

        if (clase.booked) {
            holder.btnInscribirse.isEnabled = false
            holder.btnInscribirse.text = "Inscrito"
        }

        val imageRef = storageReference.child("imagenesClases/${clase.id}.png")

        imageRef.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(context)
                .load(uri)
                .placeholder(R.mipmap.img_logo)
                .into(holder.imagenClase)
        }
    }

    override fun getItemCount(): Int {
        return clasesList.size
    }

    inner class ClaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imagenClase: ImageView = itemView.findViewById(R.id.imagenClase)
        val tituloTextView: TextView = itemView.findViewById(R.id.tituloCurso)
        val tipoClaseTextView: TextView = itemView.findViewById(R.id.tipoClase)
        val fechaInicioTextView: TextView = itemView.findViewById(R.id.fechaInicio)
        val fechaFinTextView: TextView = itemView.findViewById(R.id.fechaFin)
        val horaClaseTextView: TextView = itemView.findViewById(R.id.horaClase)
        val precioTextView: TextView = itemView.findViewById(R.id.info_precio_curso)
        val btnInscribirse: Button = itemView.findViewById(R.id.btnInscribirse)
        init {
            btnInscribirse.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onInscribirseClicked(position)
                }
            }
        }
    }

    interface OnItemClickListener {
        fun onInscribirseClicked(position: Int)
    }
}
