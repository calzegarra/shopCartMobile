package com.example.shopcart.catalog

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.GravityCompat
import com.example.shopcart.R
import com.example.shopcart.auth.ApiConfig
import com.example.shopcart.auth.LoginActivity
import com.example.shopcart.auth.User
import com.example.shopcart.cart.CartStore
import com.example.shopcart.detail.DetailActivity
import com.example.shopcart.databinding.ActivityCatalogBinding
import com.example.shopcart.databinding.ItemCatalogBinding
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

data class Videogame(
    val id: Int,
    val consoleId: Int?,
    val title: String,
    val hasDiscount: Int,
    val price: Double,
    val state: String?,
    val mini: String?
)

data class CatalogResponse(
    val data: List<Videogame>?,
    val status: Boolean?,
    val message: String?,
    val code: Int?
)

class CatalogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCatalogBinding
    private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCatalogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUser = intent.getSerializableExtra(EXTRA_USER) as? User
        setupHeader(currentUser)
        setupDrawer()

        loadCatalog()
    }

    override fun onResume() {
        super.onResume()
        updateCartBadge()
    }

    private fun setupHeader(user: User?) {
        val welcomeName = user?.name ?: user?.username ?: getString(R.string.app_name)
        binding.welcomeText.text = getString(R.string.catalog_welcome, welcomeName)
        val avatarBitmap = decodeBase64Image(user?.avatar)
        if (avatarBitmap != null) {
            binding.avatarImage.setImageBitmap(avatarBitmap)
        } else {
            binding.avatarImage.setImageResource(R.drawable.future)
        }

        binding.cartButton.setOnClickListener {
            startActivity(Intent(this, com.example.shopcart.cart.CartActivity::class.java))
        }
        updateCartBadge()
    }

    private fun setupDrawer() {
        binding.menuButton.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_profile -> {
                    val intent = Intent(this, com.example.shopcart.profile.ProfileActivity::class.java).apply {
                        putExtra(com.example.shopcart.profile.ProfileActivity.EXTRA_USER, currentUser)
                    }
                    startActivity(intent)
                }
                R.id.nav_orders -> Toast.makeText(this, "Mis compras", Toast.LENGTH_SHORT).show()
                R.id.nav_favorites -> Toast.makeText(this, "Favoritos", Toast.LENGTH_SHORT).show()
                R.id.nav_reviews -> Toast.makeText(this, "Mis reseñas", Toast.LENGTH_SHORT).show()
                R.id.nav_logout -> {
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        // clear task to evitar volver con back
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finishAffinity()
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, com.example.shopcart.profile.ProfileActivity::class.java).apply {
                        putExtra(com.example.shopcart.profile.ProfileActivity.EXTRA_USER, currentUser)
                    }
                    startActivity(intent)
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun loadCatalog() {
        showLoading(true, getString(R.string.catalog_loading))
        Thread {
            val result = runCatching { fetchCatalog() }
            runOnUiThread {
                showLoading(false)
                result.onSuccess { response ->
                    val items = response.data.orEmpty()
                    if (response.status == true && items.isNotEmpty()) {
                        binding.catalogMessage.visibility = View.GONE
                        renderCatalog(items)
                    } else {
                        showMessage(response.message ?: getString(R.string.catalog_empty))
                    }
                }.onFailure { error ->
                    showMessage(error.message ?: getString(R.string.catalog_error))
                }
            }
        }.start()
    }

    private fun fetchCatalog(): CatalogResponse {
        val url = URL(ApiConfig.CATALOG_URL)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            connectTimeout = 10000
            readTimeout = 10000
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()

        if (body.isEmpty()) {
            throw IllegalStateException("Respuesta vacia del servidor ($responseCode)")
        }

        val json = JSONObject(body)
        val gamesJson = json.optJSONArray("data")
        return CatalogResponse(
            data = parseGames(gamesJson),
            status = json.optBoolean("status", false),
            message = json.optString("message"),
            code = json.optInt("code", responseCode)
        )
    }

    private fun parseGames(array: JSONArray?): List<Videogame> {
        if (array == null || array.length() == 0) return emptyList()
        val list = mutableListOf<Videogame>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            list.add(
                Videogame(
                    id = item.optInt("id"),
                    consoleId = item.optInt("consoleId"),
                    title = item.optString("title"),
                    hasDiscount = item.optInt("hasDiscount"),
                    price = item.optDouble("price", 0.0),
                    state = item.optString("state"),
                    mini = item.optString("mini")
                )
            )
        }
        return list
    }

    private fun renderCatalog(items: List<Videogame>) {
        binding.catalogGrid.removeAllViews()
        binding.catalogGrid.columnCount = 2
        val inflater = LayoutInflater.from(this)

        items.forEach { item ->
            val cardBinding = ItemCatalogBinding.inflate(inflater, binding.catalogGrid, false)
            val bitmap = decodeBase64Image(item.mini)
            if (bitmap != null) {
                cardBinding.coverImage.setImageBitmap(bitmap)
            } else {
                cardBinding.coverImage.setImageResource(R.drawable.future)
            }
            cardBinding.itemTitle.text = item.title

            val discount = item.hasDiscount
            if (discount != 0) {
                val finalPrice = item.price * (100 - discount) / 100.0
                cardBinding.offerBadge.isVisible = true
                cardBinding.itemOriginalPrice.isVisible = true
                cardBinding.itemOriginalPrice.text = currencyFormatter.format(item.price)
                cardBinding.itemOriginalPrice.paintFlags =
                    cardBinding.itemOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                cardBinding.itemPrice.text = currencyFormatter.format(finalPrice)
            } else {
                cardBinding.offerBadge.isVisible = false
                cardBinding.itemOriginalPrice.isVisible = false
                cardBinding.itemPrice.text = currencyFormatter.format(item.price)
            }

            cardBinding.viewButton.setOnClickListener {
                val intent = Intent(this, DetailActivity::class.java).apply {
                    putExtra(DetailActivity.EXTRA_GAME_ID, item.id)
                    putExtra(DetailActivity.EXTRA_USER, currentUser)
                }
                startActivity(intent)
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            binding.catalogGrid.addView(cardBinding.root, params)
        }
    }

    private fun decodeBase64Image(data: String?): Bitmap? {
        if (data.isNullOrEmpty()) return null
        return runCatching {
            val clean = data.substringAfter("base64,", data)
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    private fun showMessage(message: String) {
        binding.catalogMessage.text = message
        binding.catalogMessage.visibility = View.VISIBLE
    }

    private fun showLoading(state: Boolean, text: String? = null) {
        binding.catalogProgress.isVisible = state
        if (text != null) {
            binding.catalogMessage.text = text
            binding.catalogMessage.visibility = View.VISIBLE
        } else if (state) {
            binding.catalogMessage.visibility = View.VISIBLE
        } else {
            binding.catalogMessage.visibility = View.GONE
        }
    }

    private fun updateCartBadge() {
        val count = CartStore.count()
        binding.cartBadge.text = count.toString()
        binding.cartBadge.isVisible = count > 0
    }

    companion object {
        const val EXTRA_USER = "extra_user"
    }
}
