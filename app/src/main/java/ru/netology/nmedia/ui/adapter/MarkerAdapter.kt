package ru.netology.nmedia.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.data.entity.MarkerEntity
import ru.netology.nmedia.databinding.ItemMarkerBinding

class MarkerViewHolder(
    private val binding: ItemMarkerBinding,
    private val onMarkerClick: (MarkerEntity) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(marker: MarkerEntity) {
        binding.title.text = marker.title
        binding.root.setOnClickListener {
            onMarkerClick(marker)
        }
    }
}

class MarkerDiffCallback : DiffUtil.ItemCallback<MarkerEntity>() {
    override fun areContentsTheSame(oldItem: MarkerEntity, newItem: MarkerEntity): Boolean {
        return oldItem == newItem
    }

    override fun areItemsTheSame(oldItem: MarkerEntity, newItem: MarkerEntity): Boolean {
        return oldItem == newItem
    }
}

class MarkerAdapter(
    private val onMarkerClick: (MarkerEntity) -> Unit
) : ListAdapter<MarkerEntity, MarkerViewHolder>(MarkerDiffCallback()) {
    override fun onBindViewHolder(holder: MarkerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkerViewHolder {
        val binding = ItemMarkerBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
        )
        return MarkerViewHolder(binding, onMarkerClick)
    }
}