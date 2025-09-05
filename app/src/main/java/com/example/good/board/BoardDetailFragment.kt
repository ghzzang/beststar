package com.example.good.board

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import com.google.android.gms.maps.model.LatLng
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.good.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BoardDetailFragment : Fragment(R.layout.fragment_board_detail), OnMapReadyCallback {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var commentAdapter: CommentAdapter
    private lateinit var commentRecyclerView: RecyclerView
    private lateinit var editComment: EditText
    private lateinit var btnCommentSend: Button
    private lateinit var btnLike: ImageView
    private lateinit var likeCountText: TextView
    private lateinit var titleTextView: TextView
    private lateinit var authorTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var contentTextView: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var imgPost: ImageView
    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    private lateinit var locationTextView: TextView // 위치 텍스트

    private var postId: String? = null
    private var currentUserNickname: String = "사용자"
    private var isLikedByCurrentUser = false
    private var postType = "normal"
    private var authorUid: String? = null
    private var postGeoPoint: GeoPoint? = null
    private var postLocationName: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        (activity as? AppCompatActivity)?.apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = ""
        }
        setHasOptionsMenu(true)

        // UI 초기화
        titleTextView = view.findViewById(R.id.titleTextView)
        authorTextView = view.findViewById(R.id.authorTextView)
        dateTextView = view.findViewById(R.id.dateTextView)
        contentTextView = view.findViewById(R.id.contentTextView)
        commentRecyclerView = view.findViewById(R.id.commentRecyclerView)
        editComment = view.findViewById(R.id.editComment)
        btnCommentSend = view.findViewById(R.id.btnCommentSend)
        btnLike = view.findViewById(R.id.btnLike)
        likeCountText = view.findViewById(R.id.likeCountText)
        ratingBar = view.findViewById(R.id.ratingBar)
        imgPost = view.findViewById(R.id.imgPost)
        mapView = view.findViewById(R.id.detailMapView)
        locationTextView = view.findViewById(R.id.locationTextView)

        // MapView 초기화
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        postId = arguments?.getString("postId") ?: return
        postType = arguments?.getString("type") ?: "normal"
        authorUid = arguments?.getString("authorUid")

        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    currentUserNickname = doc.getString("username") ?: "사용자"
                    setupCommentsOrRating()
                    setupLikes()
                }
        }

        loadPostData()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.apply {
            isZoomControlsEnabled = false
            isScrollGesturesEnabled = false
            isZoomGesturesEnabled = false
            isRotateGesturesEnabled = false
        }
        displayPostLocation()
    }

    private fun displayPostLocation() {
        if (googleMap == null || postId == null) return

        db.collection("posts").document(postId!!).get()
            .addOnSuccessListener { doc ->
                postGeoPoint = doc.getGeoPoint("location")
                postLocationName = doc.getString("locationName")

                if (postGeoPoint == null) {
                    mapView.visibility = View.GONE
                    locationTextView.visibility = View.GONE
                    return@addOnSuccessListener
                }

                val title = doc.getString("title") ?: "제목 없음"
                val rating = doc.getLong("rating")?.toInt() ?: 0
                val pos = LatLng(postGeoPoint!!.latitude, postGeoPoint!!.longitude)

                mapView.visibility = View.VISIBLE
                googleMap!!.clear()
                googleMap!!.addMarker(MarkerOptions().position(pos).title("$title (${rating}점)"))
                googleMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 12f))

                locationTextView.text = postLocationName ?: "위치 없음"
                locationTextView.visibility = View.VISIBLE

                locationTextView.setOnClickListener {
                    val uri = Uri.parse(
                        "geo:${pos.latitude},${pos.longitude}?q=${pos.latitude},${pos.longitude}(${postLocationName})"
                    )
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    if (intent.resolveActivity(requireContext().packageManager) != null) {
                        startActivity(intent)
                    } else {
                        Toast.makeText(requireContext(), "지도 앱이 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun loadPostData() {
        db.collection("posts").document(postId!!).addSnapshotListener { doc, _ ->
            if (doc != null && doc.exists()) {
                titleTextView.text = doc.getString("title") ?: ""
                contentTextView.text = doc.getString("content") ?: ""

                val ts = doc.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                dateTextView.text = sdf.format(Date(ts))

                // 작성자 닉네임 가져오기
                val authorUid = doc.getString("authorUid")
                if (authorUid != null) {
                    db.collection("users").document(authorUid).get()
                        .addOnSuccessListener { userDoc ->
                            authorTextView.text = userDoc.getString("username") ?: "사용자"
                        }
                } else authorTextView.text = "사용자"

                // 별점 표시
                if (postType == "review") {
                    val rating = doc.getDouble("rating")?.toFloat() ?: 0f
                    ratingBar.rating = rating
                    ratingBar.visibility = View.VISIBLE
                } else {
                    ratingBar.visibility = View.GONE
                }

                val imageUrl = doc.getString("imageUrl")
                if (!imageUrl.isNullOrEmpty()) {
                    imgPost.visibility = View.VISIBLE
                    Glide.with(this).load(imageUrl).into(imgPost)
                } else imgPost.visibility = View.GONE

                displayPostLocation()
            }
        }
    }

    private fun setupCommentsOrRating() {
        commentRecyclerView.visibility = View.VISIBLE
        editComment.visibility = View.VISIBLE
        btnCommentSend.visibility = View.VISIBLE

        setupComments()
        setupCommentSendButton()
    }

    private fun setupComments() {
        commentAdapter = CommentAdapter(emptyList(), currentUserNickname, postId!!, db)
        commentRecyclerView.adapter = commentAdapter
        commentRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        db.collection("posts").document(postId!!)
            .collection("comments")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                val comments = snapshot?.documents?.map { doc ->
                    val ts = doc.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()
                    BoardComment(
                        id = doc.id,
                        postId = postId!!,
                        authorUid = doc.getString("authorUid"),
                        authorNickname = doc.getString("authorNickname"),
                        nickname = doc.getString("nickname") ?: "사용자",
                        content = doc.getString("content") ?: "",
                        timestamp = ts
                    )
                } ?: emptyList()
                commentAdapter.updateComments(comments)
            }
    }

    private fun setupCommentSendButton() {
        btnCommentSend.setOnClickListener {
            val content = editComment.text.toString().trim()
            if (content.isEmpty()) return@setOnClickListener

            val commentData = hashMapOf(
                "postId" to postId,
                "authorUid" to auth.currentUser?.uid,
                "authorNickname" to currentUserNickname,
                "nickname" to currentUserNickname,
                "content" to content,
                "timestamp" to FieldValue.serverTimestamp()
            )

            db.collection("posts").document(postId!!)
                .collection("comments")
                .add(commentData)
                .addOnSuccessListener { editComment.text.clear() }
        }
    }

    private fun setupLikes() {
        val currentUid = auth.currentUser?.uid ?: return

        db.collection("posts").document(postId!!).addSnapshotListener { doc, _ ->
            if (doc != null && doc.exists()) {
                val likesMap = doc.get("likes") as? Map<String, Boolean> ?: emptyMap()
                isLikedByCurrentUser = likesMap[currentUid] ?: false
                val count = likesMap.values.count { it }
                likeCountText.text = count.toString()
                btnLike.setColorFilter(if (isLikedByCurrentUser) Color.RED else Color.GRAY)
            }
        }

        btnLike.setOnClickListener {
            val updates = hashMapOf<String, Any>("likes.$currentUid" to !isLikedByCurrentUser)
            db.collection("posts").document(postId!!).update(updates)
        }
    }

    // 툴바 메뉴
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (auth.currentUser?.uid == authorUid) {
            menu.add("삭제").apply { setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM) }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.title) {
        "삭제" -> {
            AlertDialog.Builder(requireContext())
                .setTitle("글 삭제")
                .setMessage("정말 삭제하시겠습니까?")
                .setPositiveButton("삭제") { _, _ ->
                    postId?.let {
                        db.collection("posts").document(it).delete()
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                parentFragmentManager.popBackStack()
                            }
                    }
                }
                .setNegativeButton("취소", null)
                .show()
            true
        }
        else -> {
            if (item.itemId == android.R.id.home) {
                parentFragmentManager.popBackStack()
                true
            } else super.onOptionsItemSelected(item)
        }
    }

    // MapView 라이프사이클
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onDestroy() { mapView.onDestroy(); super.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState) }
}