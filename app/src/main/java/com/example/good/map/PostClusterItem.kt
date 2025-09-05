package com.example.good.map

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

class PostClusterItem(
    lat: Double,
    lng: Double,
    private val itemTitle: String,   // 기존 title 대신 itemTitle
    val postId: String,
    val category: String,
    val province: String
) : ClusterItem {

    private val positionLatLng = LatLng(lat, lng)

    override fun getPosition(): LatLng = positionLatLng

    // ClusterItem의 getTitle() 구현
    override fun getTitle(): String = itemTitle

    override fun getSnippet(): String? = null
}