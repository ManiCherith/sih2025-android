package com.civicconnect.android.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.civicconnect.android.R
import com.civicconnect.android.databinding.ItemIssueBinding
import com.civicconnect.android.models.Issue
import com.civicconnect.android.utils.Constants
import java.text.SimpleDateFormat
import java.util.*

class IssueAdapter(
    private val onItemClick: (Issue) -> Unit
) : RecyclerView.Adapter<IssueAdapter.IssueViewHolder>() {

    private var issues = listOf<Issue>()

    fun submitList(newIssues: List<Issue>) {
        issues = newIssues
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssueViewHolder {
        val binding = ItemIssueBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IssueViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IssueViewHolder, position: Int) {
        holder.bind(issues[position])
    }

    override fun getItemCount() = issues.size

    inner class IssueViewHolder(private val binding: ItemIssueBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(issue: Issue) {
            binding.apply {
                tvTitle.text = issue.title
                tvLocation.text = issue.location
                tvStatus.text = issue.status.replaceFirstChar { it.uppercase() }
                tvPriority.text = issue.priority.replaceFirstChar { it.uppercase() }
                tvCategory.text = issue.category.replaceFirstChar { it.uppercase() }
                tvCreatedAt.text = formatDate(issue.createdAt)

                tvStatus.setTextColor(getStatusColor(issue.status))

                if (!issue.photoPath.isNullOrEmpty()) {
                    Glide.with(ivPhoto)
                        .load(Constants.BASE_URL.trimEnd('/') + issue.photoPath)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(ivPhoto)
                    ivPhoto.visibility = View.VISIBLE
                } else {
                    ivPhoto.visibility = View.GONE
                }

                root.setOnClickListener { onItemClick(issue) }
            }
        }

        private fun getStatusColor(status: String): Int {
            return when (status.lowercase()) {
                "submitted" -> ContextCompat.getColor(itemView.context, R.color.status_submitted)
                "assigned" -> ContextCompat.getColor(itemView.context, R.color.status_assigned)
                "in-progress" -> ContextCompat.getColor(itemView.context, R.color.status_in_progress)
                "resolved" -> ContextCompat.getColor(itemView.context, R.color.status_resolved)
                else -> ContextCompat.getColor(itemView.context, R.color.text_secondary)
            }
        }

        private fun formatDate(dateString: String): String {
            return try {
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).parse(dateString)
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date!!)
            } catch (e: Exception) {
                dateString
            }
        }
    }
}
