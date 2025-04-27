package com.reftgres.taihelper.ui.reference

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.reftgres.taihelper.MainActivity
import com.reftgres.taihelper.R
import com.reftgres.taihelper.data.model.PdfDocument
import com.reftgres.taihelper.data.model.ResourceState
import com.reftgres.taihelper.databinding.ReferencesFragmentBinding
import com.reftgres.taihelper.service.NetworkConnectivityService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonDisposableHandle.parent
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

        binding.buttonClearCategory.setOnClickListener {
            viewModel.clearCategoryFilter()
        }

        val mainActivity = requireActivity() as MainActivity

        val addPdfButton = view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.buttonAddPdf)

        if (!mainActivity.hasFullAccess()) {
            addPdfButton.visibility = View.GONE
        }


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
                val query = newText.orEmpty()
                viewModel.updateSearchQuery(query)
                adapter.currentSearchQuery = query
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
                Snackbar.make(requireView(), "Сеть недоступна", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun refreshData() {
        viewModel.loadAllDocuments()
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
                if (!newText.isNullOrBlank()) {
                    viewModel.searchDocumentsLocally(newText)
                    adapter.currentSearchQuery = newText
                } else {
                    viewModel.clearSearch()
                    adapter.currentSearchQuery = null
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
                    Snackbar.make(requireView(), "Ошибка поиска: ${result.message}", Snackbar.LENGTH_SHORT).show()
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
                        Snackbar.make(requireView(), "Ошибка: ${result.message}", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Наблюдаем за категориями
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            binding.categoriesContainer.visibility = if (categories.isEmpty()) View.GONE else View.VISIBLE
            binding.categoryButtonsContainer.removeAllViews()

            val allButton = createCategoryButton("Все", null)
            binding.categoryButtonsContainer.addView(allButton)

            categories.forEach { category ->
                val button = createCategoryButton(category, category)
                binding.categoryButtonsContainer.addView(button)
            }
        }


        viewModel.selectedCategoryId.observe(viewLifecycleOwner) { selectedId ->
            for (i in 0 until binding.categoryButtonsContainer.childCount) {
                val button = binding.categoryButtonsContainer.getChildAt(i) as? MaterialButton ?: continue
                val buttonCategory = if (button.text == "Все") null else button.text.toString()
                button.isChecked = buttonCategory == selectedId
            }
        }

    }


    private fun observeNetwork() {
        viewModel.networkAvailable.observe(viewLifecycleOwner) { isAvailable ->
            binding.offlineIndicator.visibility = if (isAvailable) View.GONE else View.VISIBLE
        }
    }

    private fun updateCategorySelection(selectedCategoryId: String?) {
        for (i in 0 until binding.categoryButtonsContainer.childCount) {
            val btn = binding.categoryButtonsContainer.getChildAt(i) as? MaterialButton ?: continue
            val isAllButton = btn.text == "Все"
            val matchedCategory = btn.text.toString()
            btn.isChecked = if (isAllButton) selectedCategoryId == null else matchedCategory == selectedCategoryId
        }
    }



    private fun openPdfViewer(document: PdfDocument) {
        if (networkService.isNetworkAvailable() || document.isDownloaded) {
            val intent = PdfViewerActivity.createIntent(requireContext(), document.documentId)
            startActivity(intent)
        } else {
            Snackbar.make(requireView(), "Нет подключения к сети. Документ не загружен локально.", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun createCategoryButton(title: String, categoryId: String?): MaterialButton {
        val button = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = title
            isCheckable = true
            isChecked = viewModel.selectedCategoryId.value == categoryId
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 8, 8, 8)
            }
            setPadding(24, 0, 24, 0)
            setTextColor(resources.getColor(R.color.text_on_primary, null))
            backgroundTintList = resources.getColorStateList(R.color.primary, null)
            rippleColor = resources.getColorStateList(R.color.primary_light, null)
            setOnClickListener {
                viewModel.selectCategory(categoryId)
            }
        }

        return button
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}