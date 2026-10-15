package com.zenith26.fest.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zenith26.fest.databinding.ItemCategoryStatBinding

class CategoryStatAdapter : RecyclerView.Adapter<CategoryStatAdapter.CategoryStatViewHolder>() {

    private val items = mutableListOf<Pair<String, Int>>()

    private val categoryEmoji = mapOf(
        "Technical" to "💻",
        "Cultural" to "🎭",
        "Sports" to "⚽",
        "Gaming" to "🎮",
        "Workshop" to "⚡"
    )

    fun submitMap(map: Map<String, Int>) {
        items.clear()
        items.addAll(map.toList())
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryStatViewHolder {
        val binding = ItemCategoryStatBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryStatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryStatViewHolder, position: Int) {
        val (category, count) = items[position]
        holder.bind(category, count)
    }

    override fun getItemCount(): Int = items.size

    inner class CategoryStatViewHolder(
        private val binding: ItemCategoryStatBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: String, count: Int) {
            binding.catStatIcon.text = categoryEmoji[category] ?: "📌"
            binding.catStatName.text = category
            binding.catStatCount.text = "$count events"
        }
    }
}
