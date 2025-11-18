package com.example.shopcart.cart

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.shopcart.databinding.ActivityCartBinding
import java.text.NumberFormat
import java.util.Locale

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("es", "PE"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buyButton.setOnClickListener {
            // placeholder
        }
        binding.backButton.setOnClickListener { finish() }
        renderCart()
    }

    private fun renderCart() {
        val items = CartStore.all()
        binding.cartList.removeAllViews()

        if (items.isEmpty()) {
            binding.emptyText.isVisible = true
            binding.totalText.text = currencyFormatter.format(0)
            return
        }
        binding.emptyText.isVisible = false

        items.forEach { item ->
            val row = layoutInflater.inflate(android.R.layout.simple_list_item_2, binding.cartList, false)
            val title = row.findViewById<TextView>(android.R.id.text1)
            val price = row.findViewById<TextView>(android.R.id.text2)
            title.text = item.game.title
            title.setTextColor(resources.getColor(android.R.color.white, theme))
            price.text = currencyFormatter.format(item.finalPrice)
            price.setTextColor(resources.getColor(android.R.color.white, theme))
            binding.cartList.addView(row)
        }
        binding.totalText.text = currencyFormatter.format(CartStore.total())
    }
}
