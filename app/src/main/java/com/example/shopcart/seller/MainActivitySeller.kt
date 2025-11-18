package com.example.shopcart.seller


import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.shopcart.R
import com.example.shopcart.databinding.ActivityMainSellerBinding
import com.example.shopcart.seller.nav_fragment_seller.FragmentInicioS
import com.example.shopcart.seller.nav_fragment_seller.FragmentReseniaS
import com.example.shopcart.seller.nav_fragment_seller.FragmentTiendaS
import com.example.shopcart.seller.nav_fragment_seller.button_nav_fragment_seller.FragmentOrdersS
import com.example.shopcart.seller.nav_fragment_seller.button_nav_fragment_seller.FragmentProductsS
import com.google.android.material.navigation.NavigationView

class MainActivitySeller : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityMainSellerBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainSellerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        binding.navigationView.setNavigationItemSelectedListener(this)
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerlayout,toolbar,
            R.string.open_drawer,
            R.string.close_drawer
        )
        binding.drawerlayout.addDrawerListener(toggle)
        toggle.syncState()
        replaceFragment(FragmentInicioS())
        binding.navigationView.setCheckedItem(R.id.op_inicio_v)
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.navFragment, fragment).commit()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.op_inicio_v->{
                replaceFragment(FragmentInicioS())
            }
            R.id.op_mi_tienda_v->{
                replaceFragment(FragmentTiendaS())
            }
            R.id.op_resenia_v->{
                replaceFragment(FragmentReseniaS())
            }
            R.id.op_cerrar_sesion_v->{
                Toast.makeText(applicationContext,"Saliste de la aplicacion",
                    Toast.LENGTH_SHORT).show()

            }
            R.id.op_mis_productos_v->{
                replaceFragment(FragmentProductsS())
            }
            R.id.op_mis_ordenes_v->{
                replaceFragment(FragmentOrdersS())
            }
        }
        binding.drawerlayout.closeDrawer((GravityCompat.START))
        return true
    }
}