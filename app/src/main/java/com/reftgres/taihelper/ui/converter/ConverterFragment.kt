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
import com.reftgres.taihelper.R

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


        // Подписка на изменение цвета карточек
        viewModel.isVoltagePrimary.observe(viewLifecycleOwner) { isPrimary ->
            val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary_amethyst)
            val secondaryColor = ContextCompat.getColor(requireContext(), R.color.background_light)
            val primaryTextColor = ContextCompat.getColor(requireContext(), R.color.surface_white)
            val secondaryTextColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)

            binding.card05.setCardBackgroundColor(if (isPrimary) primaryColor else secondaryColor)
            binding.card420.setCardBackgroundColor(if (isPrimary) secondaryColor else primaryColor)

            binding.text05.setTextColor(if (isPrimary) primaryTextColor else secondaryTextColor)
            binding.text420.setTextColor(if (isPrimary) secondaryTextColor else primaryTextColor)
        }

        // Настройка переключения между режимами
        binding.modeSwitch.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_current -> {
                        // Режим "Токовые"
                        updateInputField(
                            hint = "Введите значение токовых",
                            icon = R.drawable.ic_current_input
                        )
                    }
                    R.id.btn_sensor -> {
                        // Режим "Датчик"
                        updateInputField(
                            hint = "Введите значение измерений",
                            icon = R.drawable.ic_sensor_input
                        )
                    }
                }
            }
        }

        // Установка начального состояния (по умолчанию "Токовые")
        updateInputField(
            hint = "Введите значение токовых",
            icon = R.drawable.ic_current_input
        )

        // Подписка на изменение цвета кнопок
        viewModel.isPrimarySelected.observe(viewLifecycleOwner) { isPrimary ->
            val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary_amethyst)
            val secondaryColor = ContextCompat.getColor(requireContext(), R.color.surface_white)
            val primaryTextColor = ContextCompat.getColor(requireContext(), R.color.surface_white)
            val secondaryTextColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
            val primaryIconTint = ContextCompat.getColorStateList(requireContext(), R.color.surface_white)
            val secondaryIconTint = ContextCompat.getColorStateList(requireContext(), R.color.text_secondary)

            binding.btnCurrent.apply {
                setBackgroundColor(if (isPrimary) primaryColor else secondaryColor)
                setTextColor(if (isPrimary) primaryTextColor else secondaryTextColor)
                iconTint = if (isPrimary) primaryIconTint else secondaryIconTint
            }

            binding.btnSensor.apply {
                setBackgroundColor(if (isPrimary) secondaryColor else primaryColor)
                setTextColor(if (isPrimary) secondaryTextColor else primaryTextColor)
                iconTint = if (isPrimary) secondaryIconTint else primaryIconTint
            }
        }

        // Обработчики кликов для карточек
        binding.card05.setOnClickListener {
            viewModel.selectVoltagePrimary(true)
        }
        binding.card420.setOnClickListener {
            viewModel.selectVoltagePrimary(false)
        }

        // Обработчики кликов для кнопок
        binding.btnCurrent.setOnClickListener {
            viewModel.selectPrimary(true)
        }
        binding.btnSensor.setOnClickListener {
            viewModel.selectPrimary(false)
        }

        viewModel.result.observe(viewLifecycleOwner) { result ->
            binding.resultTextView.text = getString(R.string.result_format, result)
        }

        // Обработчик кнопки вычислений
        binding.resultButton.setOnClickListener {
            val userValue = binding.userValue.text.toString().toFloatOrNull()
            val startScaleSens = binding.startValue.text.toString().toFloatOrNull()
            val endScaleSens = binding.finishValue.text.toString().toFloatOrNull()

            // Проверка на null, чтобы избежать падения приложения
            if (userValue == null || startScaleSens == null || endScaleSens == null) {
                binding.resultTextView.text = getString(R.string.error_invalid_input)
                return@setOnClickListener
            }

            viewModel.calculateResult(userValue, startScaleSens, endScaleSens)
        }


    }


    private fun updateInputField(hint: String, icon: Int) {
        binding.enterUsers.apply {
            this.hint = hint
            setStartIconDrawable(icon)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
