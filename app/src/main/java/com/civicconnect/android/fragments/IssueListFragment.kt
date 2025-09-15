package com.civicconnect.android.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.civicconnect.android.activities.IssueDetailActivity
import com.civicconnect.android.adapters.IssueAdapter
import com.civicconnect.android.databinding.FragmentIssueListBinding
import com.civicconnect.android.utils.Constants
import com.civicconnect.android.viewmodels.IssueViewModel

class IssueListFragment : Fragment() {

    private var _binding: FragmentIssueListBinding? = null
    private val binding get() = _binding!!

    private lateinit var issueAdapter: IssueAdapter
    private lateinit var viewModel: IssueViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIssueListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[IssueViewModel::class.java]

        setupRecyclerView()
        setupObservers()
        setupRefresh()

        viewModel.loadIssues()
    }

    private fun setupRecyclerView() {
        issueAdapter = IssueAdapter { issue ->
            val intent = Intent(requireContext(), IssueDetailActivity::class.java)
            intent.putExtra(Constants.EXTRA_ISSUE_ID, issue.id)
            startActivity(intent)
        }

        binding.recyclerViewIssues.apply {
            adapter = issueAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupObservers() {
        viewModel.issues.observe(viewLifecycleOwner) { issues ->
            issueAdapter.submitList(issues)
            updateEmptyState(issues.isEmpty())
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg.isNotBlank()) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadIssues()
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerViewIssues.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
