package com.reftgres.taihelper.ui.oxygen

import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.reftgres.taihelper.R
import com.reftgres.taihelper.databinding.AllMeasurementsFragmentBinding


class AllMeasurementsFragment : Fragment() {
    private var _binding: AllMeasurementsFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AllAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AllMeasurementsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        handleBackPress()

        binding.newMeasurensBtn.setOnClickListener{
            findNavController().navigate(R.id.action_all_measurements_to_new_measurement)
        }
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

    private fun setupRecyclerView() {
        val data = listOf(
            Measurement("26.11.1989", listOf("9K-603", "9К-604", "9К-605", "9К-606"),
                listOf("3.57", "3.57", "3.57", "3.57"),
                listOf("3.57", "3.57", "3.57", "3.57")),
            Measurement("27.11.1989", listOf("9K-607", "9К-608", "9К-609", "9К-610"),
                listOf("4.11", "4.12", "4.13", "4.14"),
                listOf("4.11", "4.12", "4.13", "4.14")),
            Measurement("26.11.1989", listOf("9K-603", "9К-604", "9К-605", "9К-606"),
                listOf("3.57", "3.57", "3.57", "3.57"),
                listOf("3.57", "3.57", "3.57", "3.57")),
            Measurement("27.11.1989", listOf("9K-607", "9К-608", "9К-609", "9К-610"),
                listOf("4.11", "4.12", "4.13", "4.14"),
                listOf("4.11", "4.12", "4.13", "4.14")),
            Measurement("26.11.1989", listOf("9K-603", "9К-604", "9К-605", "9К-606"),
                listOf("3.57", "3.57", "3.57", "3.57"),
                listOf("3.57", "3.57", "3.57", "3.57")),
            Measurement("27.11.1989", listOf("9K-607", "9К-608", "9К-609", "9К-610"),
                listOf("4.11", "4.12", "4.13", "4.14"),
                listOf("4.11", "4.12", "4.13", "4.14")),
            Measurement("26.11.1989", listOf("9K-603", "9К-604", "9К-605", "9К-606"),
                listOf("3.57", "3.57", "3.57", "3.57"),
                listOf("3.57", "3.57", "3.57", "3.57")),
            Measurement("27.11.1989", listOf("9K-607", "9К-608", "9К-609", "9К-610"),
                listOf("4.11", "4.12", "4.13", "4.14"),
                listOf("4.11", "4.12", "4.13", "4.14"))
        )

        adapter = AllAdapter()
        binding.recyclerViewSecond.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            isNestedScrollingEnabled = false
            adapter = this@AllMeasurementsFragment.adapter
        }

        adapter.submitList(data)
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
