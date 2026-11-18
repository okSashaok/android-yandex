package ru.netology.nmedia.ui.fragment

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationStatus
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapLoadedListener
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.MapWindow
import com.yandex.mapkit.mapview.MapView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.netology.nmedia.R
import ru.netology.nmedia.data.AppDataBase
import ru.netology.nmedia.data.entity.MarkerEntity
import ru.netology.nmedia.databinding.FragmentMapsBinding
import ru.netology.nmedia.ui.extensions.DrawableImageProvider
import ru.netology.nmedia.ui.extensions.ImageInfo

class MapsFragment : Fragment() {
    private val placemarkTapListener = MapObjectTapListener { mapObject, point ->
        Toast.makeText(
            requireContext(),
            (mapObject.userData as String),
            Toast.LENGTH_LONG
        ).show()
        true
    }

    private var locationListener: LocationListener? = null
    private var safeBoot: MapLoadedListener? = null
    private var statusUserLocation = true
    private val inputListener = object : InputListener {
        override fun onMapTap(yandexMap: Map, point: Point) {
            showAddMarkerDialog(yandexMap, point)
        }

        override fun onMapLongTap(p0: Map, p1: Point) {
            TODO("Not yet implemented")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.initialize(requireContext())
    }

    private fun drawMarker(yandexMap: Map, target: Point, title: String, markerId: Long = 0) {
        val imageProvider = DrawableImageProvider(
            requireContext(),
            ImageInfo(R.drawable.ic_netology_48dp)
        )
        val placemark = yandexMap.mapObjects.addPlacemark().apply {
            geometry = target
            setIcon(imageProvider)
            setText(title)
            userData = markerId
        }
        placemark.addTapListener { mapObject, _ ->
            val id = mapObject.userData as? Long ?: 0
            showMarkerActionDialog(yandexMap, id, title)
            true
        }
    }

    private fun showAddMarkerDialog(yandexMap: Map, point: Point) {
        val dialogView = layoutInflater.inflate(
            R.layout.dialog_add_marker,
            null
        )
        val titleTextView = dialogView.findViewById<TextView>(R.id.newPoint)
        titleTextView.setText(R.string.new_point)
        val titleEditText = dialogView.findViewById<EditText>(R.id.markerTitle)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val title = titleEditText.text.toString().trim()
                    if (title.isEmpty()) {
                        titleEditText.error = getString(R.string.enter_a_name)
                        return@setOnClickListener
                    }
                    lifecycleScope.launch {
                        val id = withContext(Dispatchers.IO) {
                            AppDataBase.getInstance(requireContext())
                                .markerDao()
                                .insertMarker(
                                    MarkerEntity(
                                        title = title,
                                        latitude = point.latitude,
                                        longitude = point.longitude
                                    )
                                )
                        }
                        drawMarker(yandexMap, point, title, id)
                        dialog.dismiss()
                    }
                }
        }
        dialog.show()
    }

    private fun showMarkerActionDialog(yandexMap: Map, markerId: Long, title: String) {
        lifecycleScope.launch {
            val dao = AppDataBase.getInstance(requireContext()).markerDao()
            val market = dao.getMarkerById(markerId)
            val dialogView = layoutInflater.inflate(
                R.layout.dialog_add_marker,
                null
            )
            val titleTextView = dialogView.findViewById<TextView>(R.id.newPoint)
            titleTextView.setText(R.string.editing_a_point)
            val titleEditText = dialogView.findViewById<EditText>(R.id.markerTitle)
            titleEditText.setText(market.title)
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton("Удалить") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        AppDataBase.getInstance(requireContext()).markerDao()
                            .deleteMarkerById(markerId)
                    }
                }
                .create()
            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener {
                        val title = titleEditText.text.toString().trim()
                        if (title.isEmpty()) {
                            titleEditText.error = getString(R.string.enter_a_name)
                            return@setOnClickListener
                        }
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                dao.insertMarker(
                                    market.copy(
                                        title = title
                                    )
                                )
                            }
                            dialog.dismiss()
                        }
                    }
            }
            dialog.show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    private fun observeMarkets(yandexMap: Map) {
        val dao = AppDataBase.getInstance(requireContext()).markerDao()
        viewLifecycleOwner.lifecycleScope.launch {
            dao.getAllMarkers().collect { markers ->
                yandexMap.mapObjects.clear()
                markers.forEach { markerEntity ->
                    val point = Point(markerEntity.latitude, markerEntity.longitude)
                    drawMarker(yandexMap, point, markerEntity.title, markerEntity.id)
                }
            }
        }
    }

    private fun userLocation(
        mapWindow: MapWindow,
        locationPermissionLauncher: ActivityResultLauncher<String>? = null
    ) {
        val permissionGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permissionGranted == PackageManager.PERMISSION_GRANTED) {
            val userLocation = MapKitFactory.getInstance().createUserLocationLayer(mapWindow)
            userLocation.isVisible = true
            userLocation.isHeadingModeActive = true
            val locationManager = MapKitFactory.getInstance().createLocationManager()
            locationListener = object : LocationListener {
                override fun onLocationUpdated(location: Location) {
                    val point = location.position
                    val currentCamera = mapWindow.map.cameraPosition
                    mapWindow.map.move(
                        CameraPosition(
                            point,
                            15f,
                            currentCamera.azimuth,
                            currentCamera.tilt
                        ),
                        Animation(Animation.Type.SMOOTH, 3f),
                        null
                    )
                    locationListener?.let { locationManager.unsubscribe(it) }
                }

                override fun onLocationStatusUpdated(p0: LocationStatus) {
                    //TODO("Not yet implemented")
                }
            }
            locationListener?.let {
                locationManager.requestSingleUpdate(it)
            }
            return
        }
        locationPermissionLauncher?.launch(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentMapsBinding.bind(view)

        val mapView = binding.map
        val mapWindow = mapView.mapWindow
        val yandexMap = mapWindow.map

        subscribeToLifecycle(mapView)
        safeBoot = MapLoadedListener {
            observeMarkets(yandexMap).let {
                yandexMap.addInputListener(inputListener)
            }
        }
        yandexMap.setMapLoadedListener(safeBoot)
        binding.fabList.setOnClickListener {
            statusUserLocation = false
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.map, MarkersListFragment())
                .addToBackStack(null)
                .commit()
        }
        parentFragmentManager.setFragmentResultListener(
            "marker_key",
            viewLifecycleOwner
        ) { _, bundle ->
            val lat = bundle.getDouble("lat")
            val lon = bundle.getDouble("lon")
            val targetPoint = Point(lat, lon)
            moveToMarker(yandexMap, targetPoint)
        }
        if (statusUserLocation){
            val locationPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    userLocation(mapWindow)
                }
            }
            userLocation(mapWindow, locationPermissionLauncher)
        }
    }

    private fun moveToMarker(
        yandexMap: Map,
        target: Point
    ) {
        val currentPosition = yandexMap.cameraPosition
        yandexMap.move(
            CameraPosition(
                target, 15F, currentPosition.azimuth, currentPosition.tilt,
            ),
            Animation(Animation.Type.SMOOTH, 3F),
            null,
        )
    }

    private fun subscribeToLifecycle(mapView: MapView) {
        viewLifecycleOwner.lifecycle.addObserver(
            object : LifecycleEventObserver {
                override fun onStateChanged(
                    source: LifecycleOwner,
                    event: Lifecycle.Event
                ) {
                    when (event) {
                        Lifecycle.Event.ON_START -> {
                            MapKitFactory.getInstance().onStart()
                            mapView.onStart()
                        }

                        Lifecycle.Event.ON_STOP -> {
                            mapView.onStop()
                            MapKitFactory.getInstance().onStop()
                        }

                        Lifecycle.Event.ON_DESTROY -> source.lifecycle.removeObserver(this)

                        else -> Unit
                    }
                }
            }
        )
    }
}