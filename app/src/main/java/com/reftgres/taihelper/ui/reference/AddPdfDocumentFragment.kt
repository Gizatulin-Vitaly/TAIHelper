package com.reftgres.taihelper.ui.reference

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.reftgres.taihelper.databinding.FragmentAddPdfDocumentBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddPdfDocumentFragment : Fragment() {

    private var _binding: FragmentAddPdfDocumentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PdfDocumentsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddPdfDocumentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.buttonSubmit.setOnClickListener {
            val title = binding.editTextTitle.text.toString().trim()
            val description = binding.editTextDescription.text.toString().trim()
            val category = binding.editTextCategory.text.toString().trim()
            val fileUrl = binding.editTextFileUrl.text.toString().trim()

            if (title.isBlank() || fileUrl.isBlank()) {
                Snackbar.make(requireView(), "Заполните обязательные поля: название и ссылка", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Вызов метода ViewModel
            viewModel.submitManualPdfDocument(title, description, category, fileUrl)

            // ✅ Сообщение об успешной отправке
            Snackbar.make(requireView(), "Документ добавлен", Snackbar.LENGTH_SHORT).show()

            // (необязательно) можно очистить поля:
            binding.editTextTitle.text?.clear()
            binding.editTextDescription.text?.clear()
            binding.editTextCategory.text?.clear()
            binding.editTextFileUrl.text?.clear()

        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
