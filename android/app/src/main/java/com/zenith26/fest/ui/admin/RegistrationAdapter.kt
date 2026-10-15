package com.zenith26.fest.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zenith26.fest.data.model.Registration
import com.zenith26.fest.databinding.ItemRegistrationRowBinding

class RegistrationAdapter(
    private val onCancelClick: (Registration) -> Unit
) : RecyclerView.Adapter<RegistrationAdapter.RegistrationViewHolder>() {

    private val registrations = mutableListOf<Registration>()

    fun submitList(newList: List<Registration>) {
        registrations.clear()
        registrations.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegistrationViewHolder {
        val binding = ItemRegistrationRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RegistrationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RegistrationViewHolder, position: Int) {
        holder.bind(registrations[position])
    }

    override fun getItemCount(): Int = registrations.size

    inner class RegistrationViewHolder(
        private val binding: ItemRegistrationRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(reg: Registration) {
            binding.regId.text = "#${reg.id}"
            binding.regStudentName.text = reg.studentName
            binding.regEventTitle.text = reg.eventTitle
            binding.regRollNumber.text = "Roll: ${reg.rollNumber}"
            binding.regEmail.text = "✉️ ${reg.email}"
            binding.regPhone.text = if (!reg.phone.isNullOrBlank()) "📞 ${reg.phone}" else "📞 -"
            binding.regRegisteredAt.text = "Registered: ${reg.registeredAt.replace("T", " ").take(19)}"

            binding.btnCancelReg.setOnClickListener {
                onCancelClick(reg)
            }
        }
    }
}
