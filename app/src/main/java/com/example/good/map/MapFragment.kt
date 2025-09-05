package com.example.good.map

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.good.R
import com.example.good.board.BoardDetailFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer

class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    private val db = FirebaseFirestore.getInstance()
    private lateinit var clusterManager: ClusterManager<PostClusterItem>
    private var allItems: List<PostClusterItem> = emptyList()

    private lateinit var spinnerCategory: AutoCompleteTextView
    private lateinit var spinnerRegion: AutoCompleteTextView

    private var selectedCategory: String? = null
    private var selectedProvince: String? = null

    private val categories = listOf("전체", "별점", "자유", "질문과답변", "홍보")
    private val provinces = listOf("전체", "서울", "경기", "부산", "대구", "인천", "광주",
        "대전", "울산", "세종", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        spinnerRegion = view.findViewById(R.id.spinnerRegion)

        // 값 복원
        savedInstanceState?.let {
            selectedCategory = it.getString("selectedCategory")
            selectedProvince = it.getString("selectedProvince")
        }

        setupSpinners()

        // AutoCompleteTextView에 선택값 복원
        selectedCategory?.let { spinnerCategory.setText(it, false) }
        selectedProvince?.let { spinnerRegion.setText(it, false) }
    }

    private fun setupSpinners() {
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        val regionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, provinces)

        spinnerCategory.setAdapter(categoryAdapter)
        spinnerRegion.setAdapter(regionAdapter)

        spinnerCategory.setOnClickListener { spinnerCategory.showDropDown() }
        spinnerRegion.setOnClickListener { spinnerRegion.showDropDown() }

        // 카테고리 선택
        spinnerCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = categories.getOrNull(position)?.trim()
            filterAndShowItems()
        }

        // 텍스트 입력만 했을 때 처리
        spinnerCategory.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val input = spinnerCategory.text.toString().trim()
                selectedCategory = if (categories.contains(input)) input else null
                filterAndShowItems()
            }
        }

        // 지역 선택
        spinnerRegion.setOnItemClickListener { _, _, position, _ ->
            selectedProvince = provinces.getOrNull(position)?.trim()
            filterAndShowItems()
        }

        spinnerRegion.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val input = spinnerRegion.text.toString().trim()
                selectedProvince = if (provinces.contains(input)) input else null
                filterAndShowItems()
            }
        }
    }

    private fun filterAndShowItems() {
        val filtered = allItems.filter { item ->
            val categoryMatch = selectedCategory == null || selectedCategory == "전체" || item.category == selectedCategory
            val provinceMatch = selectedProvince == null || selectedProvince == "전체" || item.province == selectedProvince
            categoryMatch && provinceMatch
        }
        showItems(filtered)
    }

    private fun showItems(items: List<PostClusterItem>) {
        clusterManager.clearItems()
        clusterManager.addItems(items)
        clusterManager.cluster()

        val builder = LatLngBounds.Builder()
        items.forEach { builder.include(it.position) }
        if (items.isNotEmpty()) {
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100))
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.apply {
            isZoomControlsEnabled = true
            isScrollGesturesEnabled = true
        }
        setupClusterManager()
        loadAllPostLocations()
    }

    private fun setupClusterManager() {
        clusterManager = ClusterManager(requireContext(), googleMap)
        googleMap?.setOnCameraIdleListener(clusterManager)
        googleMap?.setOnMarkerClickListener(clusterManager)
        clusterManager.renderer = object : DefaultClusterRenderer<PostClusterItem>(
            requireContext(), googleMap, clusterManager
        ) {
            override fun onBeforeClusterItemRendered(item: PostClusterItem, markerOptions: com.google.android.gms.maps.model.MarkerOptions) {
                val color = when (item.category) {
                    "별점" -> BitmapDescriptorFactory.HUE_YELLOW
                    "자유" -> BitmapDescriptorFactory.HUE_GREEN
                    "질문과답변" -> BitmapDescriptorFactory.HUE_AZURE
                    "홍보" -> BitmapDescriptorFactory.HUE_ORANGE
                    else -> BitmapDescriptorFactory.HUE_RED
                }
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(color))
                markerOptions.title(item.title)
            }
        }
        clusterManager.setOnClusterItemClickListener {
            openPostDetail(it.postId)
            true
        }
    }

    private fun loadAllPostLocations() {
        db.collection("posts")
            .whereNotEqualTo("location", null)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(requireContext(), "게시물 위치 로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    allItems = snapshot.documents.mapNotNull { doc ->
                        val geo = doc.getGeoPoint("location")
                        val prov = doc.getString("province") ?: ""
                        val title = doc.getString("title") ?: "제목 없음"
                        val category = doc.getString("category") ?: "normal"
                        if (geo != null) PostClusterItem(geo.latitude, geo.longitude, title, doc.id, category, prov) else null
                    }
                    filterAndShowItems() // 필터 적용 후 지도 갱신
                }
            }
    }
    private fun openPostDetail(postId: String) {
        val fragment = BoardDetailFragment().apply {
            arguments = Bundle().apply { putString("postId", postId) }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    // MapView 라이프사이클
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onDestroy() { mapView.onDestroy(); super.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState) }
}