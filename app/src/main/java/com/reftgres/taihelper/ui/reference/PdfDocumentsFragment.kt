package com.reftgres.taihelper.ui.reference

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.reftgres.taihelper.R
import com.reftgres.taihelper.data.model.PdfDocument
import com.reftgres.taihelper.data.model.ResourceState
import com.reftgres.taihelper.databinding.ReferencesFragmentBinding
import com.reftgres.taihelper.service.NetworkConnectivityService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PdfDocumentsFragment : Fragment() {

    private var _binding: ReferencesFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PdfDocumentsViewModel by viewModels()

    @Inject
    lateinit var networkService: NetworkConnectivityService

    private lateinit var adapter: PdfDocumentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ReferencesFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        setupSearchView()
        setupCategoryButtons()
        observeViewModel()
        observeNetwork()

        binding.buttonAddPdf.setOnClickListener {
            findNavController().navigate(R.id.action_referenceFragment_to_addPdfDocumentFragment)
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    viewModel.searchDocuments(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    viewModel.clearSearch()
                }
                return true
            }
        })


    }

    private fun setupRecyclerView() {
        adapter = PdfDocumentAdapter(
            onDocumentClick = { document ->
                openPdfViewer(document)
            }
        )

        binding.recyclerViewDocuments.apply {
            this.adapter = this@PdfDocumentsFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            if (networkService.isNetworkAvailable()) {
                refreshData()
            } else {
                binding.swipeRefreshLayout.isRefreshing = false
                Toast.makeText(context, "Сеть недоступна", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refreshData() {
        val currentCategoryId = viewModel.selectedCategoryId.value
        if (currentCategoryId != null) {
            viewModel.loadDocumentsByCategory(currentCategoryId)
        } else {
            viewModel.loadAllDocuments()
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    viewModel.searchDocuments(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    viewModel.clearSearch()
                }
                return true
            }
        })
    }

    private fun setupCategoryButtons() {
        binding.buttonClearCategory.setOnClickListener {
            viewModel.clearCategoryFilter()
            updateCategorySelection(null)
        }
    }

    private fun observeViewModel() {
        // 🔍 Наблюдаем за результатами поиска (в приоритете)
        viewModel.searchResults.observe(viewLifecycleOwner) { result ->
            binding.swipeRefreshLayout.isRefreshing = false

            when (result) {
                is ResourceState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }

                is ResourceState.Success -> {
                    binding.progressBar.visibility = View.GONE

                    if (result.data.isEmpty()) {
                        if (binding.searchView.query.isNotEmpty()) {
                            binding.textViewEmpty.visibility = View.VISIBLE
                            binding.textViewEmpty.text = "Ничего не найдено"
                            binding.recyclerViewDocuments.visibility = View.GONE
                        } else {
                            // Если поиск пустой — загружаем обычный список
                            viewModel.loadAllDocuments()
                        }
                    } else {
                        binding.textViewEmpty.visibility = View.GONE
                        binding.recyclerViewDocuments.visibility = View.VISIBLE
                        adapter.submitList(result.data)
                    }
                }

                is ResourceState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Ошибка поиска: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 📚 Наблюдаем за основным списком документов (если нет поиска)
        viewModel.documents.observe(viewLifecycleOwner) { result ->
            // Только если поле поиска пустое!
            if (binding.searchView.query.isEmpty()) {
                binding.swipeRefreshLayout.isRefreshing = false

                when (result) {
                    is ResourceState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.textViewEmpty.visibility = View.GONE
                    }

                    is ResourceState.Success -> {
                        binding.progressBar.visibility = View.GONE

                        if (result.data.isEmpty()) {
                            binding.textViewEmpty.visibility = View.VISIBLE
                            binding.recyclerViewDocuments.visibility = View.GONE
                        } else {
                            binding.textViewEmpty.visibility = View.GONE
                            binding.recyclerViewDocuments.visibility = View.VISIBLE
                            adapter.submitList(result.data)
                        }
                    }

                    is ResourceState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.textViewEmpty.visibility = View.VISIBLE
                        binding.textViewEmpty.text = "Ошибка: ${result.message}"
                        Toast.makeText(context, "Ошибка: ${result.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // 🧩 Наблюдаем за категориями
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            binding.categoriesContainer.visibility = if (categories.isEmpty()) View.GONE else View.VISIBLE
            binding.categoryButtonsContainer.removeAllViews()

            categories.forEach { category ->
                val button = layoutInflater.inflate(
                    R.layout.category_button,
                    binding.categoryButtonsContainer,
                    false
                )
                button.findViewById<android.widget.TextView>(R.id.textViewCategoryName).text = category.name
                button.isSelected = viewModel.selectedCategoryId.value == category.id
                button.setOnClickListener {
                    viewModel.loadDocumentsByCategory(category.id)
                    updateCategorySelection(category.id)
                }
                binding.categoryButtonsContainer.addView(button)
            }
        }
    }


    private fun observeNetwork() {
        viewModel.networkAvailable.observe(viewLifecycleOwner) { isAvailable ->
            binding.offlineIndicator.visibility = if (isAvailable) View.GONE else View.VISIBLE
        }
    }

    private fun updateCategorySelection(selectedCategoryId: String?) {
        // Очищаем выделение всех кнопок категорий
        for (i in 0 until binding.categoryButtonsContainer.childCount) {
            val button = binding.categoryButtonsContainer.getChildAt(i)
            val category = viewModel.categories.value?.getOrNull(i)
            button.isSelected = category?.id == selectedCategoryId
        }
    }

    private fun openPdfViewer(document: PdfDocument) {
        if (networkService.isNetworkAvailable() || document.isDownloaded) {
            val intent = PdfViewerActivity.createIntent(requireContext(), document.documentId)
            startActivity(intent)
        } else {
            Toast.makeText(context, "Нет подключения к сети. Документ не загружен локально.", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}