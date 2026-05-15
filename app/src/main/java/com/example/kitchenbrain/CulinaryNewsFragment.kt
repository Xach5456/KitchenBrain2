package com.example.kitchenbrain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kitchenbrain.adapter.ModernNewsAdapter
import com.example.kitchenbrain.databinding.FragmentCulinaryNewsBinding
import com.example.kitchenbrain.viewmodel.NewsViewModel
import kotlinx.coroutines.launch

/**
 * CulinaryNewsFragment - Modern News Feed
 * 
 * ✅ PRODUCTION FEATURES:
 * - Shimmer loading effect
 * - Swipe-to-refresh
 * - Offline-first data display
 * - API limit handling (via ViewModel/Repository)
 */
class CulinaryNewsFragment : Fragment() {

    private var _binding: FragmentCulinaryNewsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewsViewModel by viewModels()
    private lateinit var newsAdapter: ModernNewsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCulinaryNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        newsAdapter = ModernNewsAdapter { news ->
            // Handle sharing to chat or external
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, "${news.title}\n\n${news.url}")
            }
            startActivity(android.content.Intent.createChooser(shareIntent, "Share News"))
        }

        binding.recyclerViewNews.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = newsAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh(force = true)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.news.collect { articles ->
                        newsAdapter.submitList(articles)
                        binding.layoutEmpty.visibility = if (articles.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is NewsViewModel.NewsUiState.Loading -> {
                                if (newsAdapter.itemCount == 0) {
                                    binding.shimmerLoading.visibility = View.VISIBLE
                                    binding.shimmerLoading.startShimmer()
                                }
                            }
                            is NewsViewModel.NewsUiState.Success -> {
                                binding.shimmerLoading.stopShimmer()
                                binding.shimmerLoading.visibility = View.GONE
                                binding.swipeRefresh.isRefreshing = false
                            }
                            is NewsViewModel.NewsUiState.Error -> {
                                binding.shimmerLoading.stopShimmer()
                                binding.shimmerLoading.visibility = View.GONE
                                binding.swipeRefresh.isRefreshing = false
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
