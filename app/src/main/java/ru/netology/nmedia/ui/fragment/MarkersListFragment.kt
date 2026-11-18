package ru.netology.nmedia.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nmedia.data.AppDataBase
import ru.netology.nmedia.databinding.FragmentMarkersListBinding
import ru.netology.nmedia.ui.adapter.MarkerAdapter

class MarkersListFragment : Fragment(){
    private var _binding: FragmentMarkersListBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarkersListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = MarkerAdapter { marker ->
            val result = Bundle().apply {
                putDouble("lat", marker.latitude)
                putDouble("lon", marker.longitude)
            }
            parentFragmentManager.setFragmentResult("marker_key", result)
            parentFragmentManager.popBackStack()
        }
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
        val dao = AppDataBase.getInstance(requireContext()).markerDao()
        viewLifecycleOwner.lifecycleScope.launch {
            dao.getAllMarkers().collectLatest { markers ->
                adapter.submitList(markers)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}