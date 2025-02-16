package com.reftgres.taihelper.ui.converter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.reftgres.taihelper.databinding.FragmentConverterBinding

@AndroidEntryPoint
class FragmentConverter : Fragment() {

    private var _binding: FragmentConverterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConverterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConverterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Подписка на изменение цвета
        viewModel.cardColor.observe(viewLifecycleOwner) { colorResId ->
            binding.changeVoltageLayout.card05.setCardBackgroundColor(ContextCompat.getColor(requireContext(), colorResId))
        }

        // Обработчик клика
        binding.changeVoltageLayout.card05.setOnClickListener {
            viewModel.changeColor()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
