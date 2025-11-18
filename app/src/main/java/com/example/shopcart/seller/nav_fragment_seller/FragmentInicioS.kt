package com.example.shopcart.seller.nav_fragment_seller

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.shopcart.R
import com.example.shopcart.databinding.ActivityMainSellerBinding
import com.example.shopcart.databinding.FragmentInicioVBinding
import com.example.shopcart.databinding.FragmentProductsVBinding
import com.example.shopcart.seller.nav_fragment_seller.button_nav_fragment_seller.FragmentOrdersS
import com.example.shopcart.seller.nav_fragment_seller.button_nav_fragment_seller.FragmentProductsS

class FragmentInicioS : Fragment() {

    private lateinit var binding: FragmentInicioVBinding
    private lateinit var mContext: Context

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View? {

        binding = FragmentInicioVBinding.inflate(inflater,container,false)
        binding.bottomNavigator.setOnItemSelectedListener {
            when(it.itemId){
                R.id.op_mis_productos_v->{
                    replaceFragment(FragmentProductsS())
                }
                R.id.op_mis_ordenes_v->{
                    replaceFragment(FragmentOrdersS())
                }
            }
            true
        }
        replaceFragment(FragmentProductsS())
        binding.bottomNavigator.selectedItemId = R.id.op_mis_productos_v
        binding.addFab.setOnClickListener {
            Toast.makeText(mContext,"Has presionado un boton flotante", Toast.LENGTH_SHORT).show()
        }
        return binding.root
    }

    private fun replaceFragment(fragment: Fragment){
        parentFragmentManager
            .beginTransaction()
            .replace(R.id.bottomFragment,fragment)
            .commit()
    }
}

