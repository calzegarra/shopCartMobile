package com.example.shopcart.profile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.shopcart.auth.ApiConfig
import com.example.shopcart.auth.User
import com.example.shopcart.catalog.CatalogActivity
import com.example.shopcart.databinding.ActivityProfileBinding
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private var currentUser: User? = null
    private var avatarBase64: String? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@registerForActivityResult
            contentResolver.openInputStream(uri)?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input)
                binding.avatarPreview.setImageBitmap(bitmap)
                avatarBase64 = encodeBase64(bitmap)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUser = intent.getSerializableExtra(EXTRA_USER) as? User
        fillForm(currentUser)

        binding.chooseImageButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        binding.backButton.setOnClickListener { goBack() }
        binding.saveButton.setOnClickListener { attemptUpdate() }
    }

    private fun fillForm(user: User?) {
        user ?: return
        binding.nameEdit.setText(user.name.orEmpty())
        binding.lastnameEdit.setText(user.lastname.orEmpty())
        binding.dniEdit.setText(user.dni.orEmpty())
        binding.addressEdit.setText(user.address.orEmpty())
        binding.emailEdit.setText(user.email.orEmpty())
        binding.usernameEdit.setText(user.username.orEmpty())
        binding.passwordEdit.setText(user.password.orEmpty())
        binding.confirmPasswordEdit.setText(user.password.orEmpty())
        val avatarBitmap = decodeBase64(user.avatar)
        avatarBase64 = user.avatar
        if (avatarBitmap != null) {
            binding.avatarPreview.setImageBitmap(avatarBitmap)
        }
    }

    private fun attemptUpdate() {
        val name = binding.nameEdit.text?.toString()?.trim().orEmpty()
        val lastname = binding.lastnameEdit.text?.toString()?.trim().orEmpty()
        val dni = binding.dniEdit.text?.toString()?.trim().orEmpty()
        val address = binding.addressEdit.text?.toString()?.trim().orEmpty()
        val email = binding.emailEdit.text?.toString()?.trim().orEmpty()
        val username = binding.usernameEdit.text?.toString()?.trim().orEmpty()
        val password = binding.passwordEdit.text?.toString()?.trim().orEmpty()
        val confirmPassword = binding.confirmPasswordEdit.text?.toString()?.trim().orEmpty()

        var hasError = false

        if (name.isEmpty()) {
            binding.nameLayout.error = getString(com.example.shopcart.R.string.profile_required)
            hasError = true
        } else {
            binding.nameLayout.error = null
        }

        if (lastname.isEmpty()) {
            binding.lastnameLayout.error = getString(com.example.shopcart.R.string.profile_required)
            hasError = true
        } else {
            binding.lastnameLayout.error = null
        }

        if (dni.isEmpty()) {
            binding.dniLayout.error = getString(com.example.shopcart.R.string.profile_required)
            hasError = true
        } else {
            binding.dniLayout.error = null
        }

        if (email.isEmpty()) {
            binding.emailLayout.error = getString(com.example.shopcart.R.string.profile_required)
            hasError = true
        } else {
            binding.emailLayout.error = null
        }

        if (username.isEmpty()) {
            binding.usernameLayout.error = getString(com.example.shopcart.R.string.profile_required)
            hasError = true
        } else {
            binding.usernameLayout.error = null
        }

        if (password.isEmpty()) {
            binding.passwordLayout.error = getString(com.example.shopcart.R.string.profile_required)
            hasError = true
        } else {
            binding.passwordLayout.error = null
        }

        if (password != confirmPassword) {
            binding.confirmPasswordLayout.error = getString(com.example.shopcart.R.string.profile_password_mismatch)
            hasError = true
        } else {
            binding.confirmPasswordLayout.error = null
        }

        if (hasError) return

        val userId = currentUser?.id ?: 0

        showLoading(true)

        Thread {
            val result = runCatching {
                doUpdateRequest(
                    id = userId,
                    name = name,
                    lastname = lastname,
                    dni = dni,
                    address = address,
                    email = email,
                    username = username,
                    password = password,
                    avatar = avatarBase64
                )
            }
            runOnUiThread {
                showLoading(false)
                result.onSuccess { response ->
                    if (response.status == true) {
                        val updatedUser = (currentUser ?: User()).copy(
                            id = userId,
                            name = name,
                            lastname = lastname,
                            dni = dni,
                            address = address,
                            email = email,
                            username = username,
                            password = password,
                            avatar = avatarBase64
                        )
                        Toast.makeText(this, response.message ?: "Perfil actualizado", Toast.LENGTH_SHORT).show()
                        goToCatalog(updatedUser)
                    } else {
                        Toast.makeText(this, response.message ?: "No se pudo actualizar", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { error ->
                    Toast.makeText(this, error.message ?: "Error al actualizar", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun doUpdateRequest(
        id: Int,
        name: String,
        lastname: String,
        dni: String,
        address: String,
        email: String,
        username: String,
        password: String,
        avatar: String?
    ): com.example.shopcart.auth.ResponseData {
        val url = URL(ApiConfig.USER_UPDATE_URL)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            doOutput = true
            doInput = true
            connectTimeout = 10000
            readTimeout = 10000
        }

        val body = JSONObject().apply {
            put("id", id)
            put("name", name)
            put("lastname", lastname)
            put("dni", dni)
            put("address", address)
            put("email", email)
            put("username", username)
            put("password", password)
            put("avatar", avatar ?: "")
            // se envía role si existe
            currentUser?.role?.let { role ->
                val roleJson = JSONObject()
                roleJson.put("id", role.id)
                roleJson.put("description", role.description)
                put("role", roleJson)
            }
        }.toString()

        connection.outputStream.use { output ->
            output.write(body.toByteArray())
            output.flush()
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()

        if (responseBody.isEmpty()) {
            throw IllegalStateException("Respuesta vacía del servidor ($responseCode)")
        }

        val json = JSONObject(responseBody)
        val userJson = json.optJSONObject("data")
        return com.example.shopcart.auth.ResponseData(
            data = com.example.shopcart.auth.User(
                id = userJson?.optInt("id"),
                name = userJson?.optString("name"),
                lastname = userJson?.optString("lastname"),
                dni = userJson?.optString("dni"),
                address = userJson?.optString("address"),
                email = userJson?.optString("email"),
                username = userJson?.optString("username"),
                password = userJson?.optString("password"),
                role = currentUser?.role,
                avatar = userJson?.optString("avatar")
            ),
            status = json.optBoolean("status", false),
            message = json.optString("message"),
            code = json.optInt("code", responseCode)
        )
    }

    private fun goBack() {
        finish()
    }

    private fun goToCatalog(user: User) {
        val intent = Intent(this, CatalogActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(CatalogActivity.EXTRA_USER, user)
        }
        startActivity(intent)
        finishAffinity()
    }

    private fun showLoading(state: Boolean) {
        binding.saveButton.isEnabled = !state
        binding.progress.isVisible = state
    }

    private fun decodeBase64(data: String?): Bitmap? {
        if (data.isNullOrEmpty()) return null
        return runCatching {
            val clean = data.substringAfter("base64,", data)
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    private fun encodeBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val bytes = stream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    companion object {
        const val EXTRA_USER = "extra_user_profile"
    }
}
