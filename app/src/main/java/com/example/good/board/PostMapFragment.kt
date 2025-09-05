package com.example.good.board

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.good.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class PostMapFragment : Fragment(R.layout.fragment_post_map), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val geo = doc.getGeoPoint("location")
                    val title = doc.getString("title") ?: "제목 없음"
                    val rating = doc.getLong("rating")?.toInt() ?: 0

                    if (geo != null) {
                        val marker = LatLng(geo.latitude, geo.longitude)
                        googleMap.addMarker(
                            MarkerOptions()
                                .position(marker)
                                .title("$title (${rating}점)")
                        )
                    }
                }

                snapshot.documents.firstOrNull()?.getGeoPoint("location")?.let {
                    val first = LatLng(it.latitude, it.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(first, 12f))
                }
            }
    }

    // MapView 라이프사이클 처리
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onDestroy() { mapView.onDestroy(); super.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}