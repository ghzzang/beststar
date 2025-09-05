package com.example.good.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.good.R   // 최상위 패키지 R import
import com.example.good.board.BoardFragment
import com.example.good.home.HomeFragment
import com.example.good.map.MapFragment
import com.example.good.profile.ProfileFragment
import com.example.good.rank.RankFragment

import com.google.android.material.bottomnavigation.BottomNavigationView
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    private val homeFragment = HomeFragment()
    private val boardFragment = BoardFragment()
    private val rankFragment = RankFragment()
    private val mapFragment = MapFragment()
    private val profileFragment = ProfileFragment()




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        setContentView(R.layout.activity_main)


        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // 첫 화면을 HomeFragment로 설정
        replaceFragment(homeFragment)

        // 수정된 리스너 사용
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> replaceFragment(homeFragment)
                R.id.navigation_board -> replaceFragment(boardFragment)
                R.id.navigation_rank -> replaceFragment(rankFragment)
                R.id.navigation_map -> replaceFragment(mapFragment)
                R.id.navigation_profile -> replaceFragment(profileFragment)
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        return true
    }
    // Toolbar 뒤로가기 클릭 처리
    override fun onSupportNavigateUp(): Boolean {
        supportFragmentManager.popBackStack()
        return true
    }
}