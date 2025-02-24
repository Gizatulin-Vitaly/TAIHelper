package com.reftgres.taihelper.ui.oxygen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.reftgres.taihelper.R
import com.reftgres.taihelper.databinding.NewMeasuremensBinding


class NewMeasurementsFragment : Fragment() {
    private var _binding: NewMeasuremensBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = NewMeasuremensBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        handleBackPress()
    }

    private fun setupToolbar() {
        val activity = requireActivity() as AppCompatActivity
        activity.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        requireActivity().findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun handleBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigateUp()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}