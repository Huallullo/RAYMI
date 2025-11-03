package com.raymi.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.raymi.app.ui.fragments.AlquileresFragment


import com.raymi.app.ui.fragments.ClientesFragment
import com.raymi.app.ui.fragments.DashboardFragment
import com.raymi.app.ui.fragments.HistorialFragment
import com.raymi.app.ui.fragments.VestuariosFragment

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Fragment inicial
        loadFragment(DashboardFragment())

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> loadFragment(DashboardFragment())
                R.id.nav_vestuarios -> loadFragment(VestuariosFragment())
                R.id.nav_clientes -> loadFragment(ClientesFragment())
                R.id.nav_historial -> loadFragment(HistorialFragment())
                R.id.nav_alquileres -> loadFragment(AlquileresFragment())
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    override fun onDestroy() {
        super.onDestroy()
        FirebaseAuth.getInstance().signOut()
    }

}
