package com.example.good.board

import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.example.good.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import java.util.Locale
import java.util.UUID

class WritePostFragment : Fragment(R.layout.fragment_write_post), OnMapReadyCallback {

    private lateinit var edtTitle: EditText
    private lateinit var edtContent: EditText
    private lateinit var spnCategory: Spinner
    private lateinit var btnSubmit: Button
    private lateinit var ratingBar: RatingBar
    private lateinit var ratingLayout: LinearLayout
    private lateinit var mapView: MapView
    private lateinit var tvRatingValue: TextView
    private lateinit var locationSwitchLayout: LinearLayout
    private lateinit var switchLocation: SwitchCompat
    private lateinit var btnSelectImage: Button
    private lateinit var imgPreview: ImageView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storageRef = FirebaseStorage.getInstance().reference

    private var selectedGeoPoint: GeoPoint? = null
    private var selectedLocationName: String? = null
    private var googleMap: GoogleMap? = null
    private var selectedImageUri: Uri? = null

    companion object { const val IMAGE_PICK_CODE = 1000 }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뷰 초기화
        edtTitle = view.findViewById(R.id.edtTitle)
        edtContent = view.findViewById(R.id.edtContent)
        spnCategory = view.findViewById(R.id.spnCategory)
        btnSubmit = view.findViewById(R.id.btnSubmit)
        ratingBar = view.findViewById(R.id.ratingBar)
        ratingLayout = view.findViewById(R.id.ratingLayout)
        mapView = view.findViewById(R.id.postMapView)
        tvRatingValue = view.findViewById(R.id.tvRatingValue)
        locationSwitchLayout = view.findViewById(R.id.locationSwitchLayout)
        switchLocation = view.findViewById(R.id.switchLocation)
        btnSelectImage = view.findViewById(R.id.btnSelectImage)
        imgPreview = view.findViewById(R.id.imgPreview)

        val categories = listOf("별점", "자유", "질문과답변", "홍보")
        spnCategory.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)

        spnCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val category = categories[position]
                if (category == "별점") {
                    ratingLayout.visibility = View.VISIBLE
                    locationSwitchLayout.visibility = View.GONE
                    mapView.visibility = View.VISIBLE
                } else {
                    ratingLayout.visibility = View.GONE
                    locationSwitchLayout.visibility = View.VISIBLE
                    mapView.visibility = if (switchLocation.isChecked) View.VISIBLE else View.GONE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        ratingBar.setOnRatingBarChangeListener { _, rating, _ -> tvRatingValue.text = rating.toInt().toString() }

        switchLocation.setOnCheckedChangeListener { _, isChecked -> mapView.visibility = if (isChecked) View.VISIBLE else View.GONE }

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_PICK_CODE)
        }

        btnSubmit.setOnClickListener {
            val title = edtTitle.text.toString().trim()
            val content = edtContent.text.toString().trim()
            val category = spnCategory.selectedItem.toString()
            val currentUser = auth.currentUser

            if (currentUser == null) {
                Toast.makeText(requireContext(), "로그인 후 작성 가능합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(requireContext(), "제목과 내용을 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedImageUri != null) {
                val imageRef = storageRef.child("post_images/${UUID.randomUUID()}")
                imageRef.putFile(selectedImageUri!!)
                    .addOnSuccessListener {
                        imageRef.downloadUrl.addOnSuccessListener { uri ->
                            savePostToFirestore(title, content, category, currentUser.uid, currentUser.displayName ?: "사용자", uri.toString())
                        }
                    }.addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "이미지 업로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                savePostToFirestore(title, content, category, currentUser.uid, currentUser.displayName ?: "사용자", null)
            }
        }
    }

    private fun savePostToFirestore(title: String, content: String, category: String, uid: String, nickname: String, imageUrl: String?) {
        // province 자동 변환
        val provinceToSave = selectedGeoPoint?.let { getProvinceFromLatLng(it.latitude, it.longitude) } ?: "전체"

        // 별점 글도 위치 없으면 기본 위치
        val locationToSave = if (category == "별점") selectedGeoPoint ?: GeoPoint(35.0, 127.0)
        else if (switchLocation.isChecked) selectedGeoPoint else null

        val postData = hashMapOf(
            "title" to title,
            "content" to content,
            "authorUid" to uid,
            "authorNickname" to nickname,
            "category" to category,
            "timestamp" to FieldValue.serverTimestamp(),
            "type" to if (category == "별점") "review" else "post",
            "rating" to if (category == "별점") ratingBar.rating else null,
            "location" to locationToSave,
            "locationName" to if (locationToSave != null) "${locationToSave.latitude},${locationToSave.longitude}" else null,
            "province" to provinceToSave,
            "imageUrl" to imageUrl
        )

        db.collection("posts").add(postData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "글이 등록되었습니다.", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "등록 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Geocoder로 좌표 → province 변환, Spinner 값과 일치시키기
    private fun getProvinceFromLatLng(lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(requireContext(), Locale.KOREA)
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                when (val admin = addresses[0].adminArea) {
                    "서울특별시" -> "서울"
                    "부산광역시" -> "부산"
                    "대구광역시" -> "대구"
                    "인천광역시" -> "인천"
                    "광주광역시" -> "광주"
                    "대전광역시" -> "대전"
                    "울산광역시" -> "울산"
                    "세종특별자치시" -> "세종"
                    "강원도" -> "강원"
                    "충청북도" -> "충북"
                    "충청남도" -> "충남"
                    "전라북도" -> "전북"
                    "전라남도" -> "전남"
                    "경상북도" -> "경북"
                    "경상남도" -> "경남"
                    "제주특별자치도" -> "제주"
                    else -> "전체"
                }
            } else "전체"
        } catch (e: Exception) { "전체" }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            imgPreview.setImageURI(selectedImageUri)
            imgPreview.visibility = View.VISIBLE
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isScrollGesturesEnabled = true
        map.uiSettings.isZoomControlsEnabled = false

        map.setOnMapClickListener { latLng ->
            map.clear()
            map.addMarker(MarkerOptions().position(latLng).title("선택 위치"))
            selectedGeoPoint = GeoPoint(latLng.latitude, latLng.longitude)
            selectedLocationName = "${latLng.latitude},${latLng.longitude}"
        }

        val default = LatLng(35.0, 127.0)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(default, 7f))
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onDestroy() { mapView.onDestroy(); super.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState) }
}
