package com.example.shopcart.detail

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.shopcart.R
import com.example.shopcart.auth.ApiConfig
import com.example.shopcart.auth.User
import com.example.shopcart.cart.CartItem
import com.example.shopcart.cart.CartStore
import com.example.shopcart.databinding.ActivityDetailBinding
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private var currentUser: User? = null
    private var gameId: Int = 0
    private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
    private var currentDetail: GameDetail? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gameId = intent.getIntExtra(EXTRA_GAME_ID, 0)
        currentUser = intent.getSerializableExtra(EXTRA_USER) as? User

        binding.backButton.setOnClickListener { finish() }
        binding.addToCartButton.setOnClickListener { addToCart() }

        loadDetail()
    }

    private fun loadDetail() {
        showLoading(true)
        Thread {
            val result = runCatching { fetchDetail() }
            runOnUiThread {
                showLoading(false)
                result.onSuccess { detail ->
                    currentDetail = detail
                    renderDetail(detail)
                }.onFailure { error ->
                    Toast.makeText(this, error.message ?: "No se pudo cargar", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun fetchDetail(): GameDetail {
        val url = URL(ApiConfig.GAME_DETAIL_URL + gameId)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            connectTimeout = 10000
            readTimeout = 10000
        }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (body.isEmpty()) throw IllegalStateException("Respuesta vacía ($code)")

        val json = JSONObject(body)
        val data = json.getJSONObject("data")
        return parseDetail(data)
    }

    private fun parseDetail(obj: JSONObject): GameDetail {
        val consoleJson = obj.optJSONObject("console")
        val promosArray = obj.optJSONArray("detailsPromo")
        val categoriesArray = obj.optJSONArray("detailsCategories")
        return GameDetail(
            id = obj.optInt("id"),
            title = obj.optString("title"),
            description = obj.optString("description"),
            console = consoleJson?.let { DetailConsole(it.optInt("id"), it.optString("description")) },
            hasDiscount = obj.optBoolean("hasDiscount", false),
            price = obj.optDouble("price", 0.0),
            image = obj.optString("image"),
            image2 = obj.optString("image2"),
            image3 = obj.optString("image3"),
            mini = obj.optString("mini"),
            detailsPromo = parsePromos(promosArray),
            detailsCategories = parseCategories(categoriesArray)
        )
    }

    private fun parsePromos(array: JSONArray?): List<DetailPromo> {
        if (array == null) return emptyList()
        val list = mutableListOf<DetailPromo>()
        for (i in 0 until array.length()) {
            val it = array.optJSONObject(i) ?: continue
            list.add(
                DetailPromo(
                    id = it.optInt("id"),
                    description = it.optString("description"),
                    discount = it.optDouble("discount", 0.0),
                    startDate = it.optString("startDate"),
                    endDate = it.optString("endDate"),
                    state = it.optBoolean("state"),
                    imagePromo = it.optString("imagePromo")
                )
            )
        }
        return list
    }

    private fun parseCategories(array: JSONArray?): List<DetailCategory> {
        if (array == null) return emptyList()
        val list = mutableListOf<DetailCategory>()
        for (i in 0 until array.length()) {
            val it = array.optJSONObject(i) ?: continue
            list.add(
                DetailCategory(
                    id = it.optInt("id"),
                    description = it.optString("description")
                )
            )
        }
        return list
    }

    private fun renderDetail(detail: GameDetail) {
        binding.titleText.text = detail.title
        binding.consoleText.text = detail.console?.description ?: "-"
        binding.descriptionText.text = detail.description.orEmpty()

        val images = listOfNotNull(detail.image, detail.image2, detail.image3, detail.mini)
        if (images.isNotEmpty()) {
            setImage(binding.mainImage, images.first())
            renderThumbnails(images)
        }

        val discount = detail.detailsPromo?.firstOrNull()?.discount ?: 0.0
        if (detail.hasDiscount && discount > 0) {
            val finalPrice = detail.price * (1 - discount)
            binding.originalPrice.isVisible = true
            binding.originalPrice.text = currencyFormatter.format(detail.price)
            binding.originalPrice.paintFlags =
                binding.originalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.priceText.text = currencyFormatter.format(finalPrice)
        } else {
            binding.originalPrice.isVisible = false
            binding.priceText.text = currencyFormatter.format(detail.price)
        }

        binding.categoryChips.removeAllViews()
        detail.detailsCategories?.forEach { cat ->
            val tv = layoutInflater.inflate(R.layout.view_chip, binding.categoryChips, false) as android.widget.TextView
            tv.text = cat.description
            binding.categoryChips.addView(tv)
        }
    }

    private fun renderThumbnails(images: List<String>) {
        binding.thumbnails.removeAllViews()
        images.forEachIndexed { index, data ->
            val imageView = layoutInflater.inflate(R.layout.view_thumbnail, binding.thumbnails, false) as ImageView
            setImage(imageView, data)
            imageView.setOnClickListener {
                setImage(binding.mainImage, data)
            }
            binding.thumbnails.addView(imageView)
        }
    }

    private fun setImage(imageView: ImageView, data: String) {
        val bmp = decodeBase64(data)
        if (bmp != null) {
            imageView.setImageBitmap(bmp)
        } else {
            imageView.setImageResource(R.drawable.future)
        }
    }

    private fun decodeBase64(data: String?): Bitmap? {
        if (data.isNullOrEmpty()) return null
        return runCatching {
            val clean = data.substringAfter("base64,", data)
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    private fun addToCart() {
        val detail = currentDetail ?: return
        val discount = detail.detailsPromo?.firstOrNull()?.discount ?: 0.0
        val finalPrice = if (detail.hasDiscount && discount > 0) detail.price * (1 - discount) else detail.price
        CartStore.add(CartItem(detail, finalPrice))
        Toast.makeText(this, "${detail.title} agregado al carrito", Toast.LENGTH_SHORT).show()
    }

    private fun showLoading(state: Boolean) {
        binding.progress.isVisible = state
        binding.addToCartButton.isEnabled = !state
    }

    companion object {
        const val EXTRA_GAME_ID = "extra_game_id"
        const val EXTRA_USER = "extra_user_detail"
    }
}
