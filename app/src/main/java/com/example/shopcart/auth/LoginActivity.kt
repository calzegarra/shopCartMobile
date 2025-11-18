package com.example.shopcart.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.shopcart.R
import com.example.shopcart.catalog.CatalogActivity
import com.example.shopcart.databinding.ActivityLoginBinding
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener { attemptLogin() }
        binding.passwordEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin()
                true
            } else {
                false
            }
        }
    }

    private fun attemptLogin() {
        val username = binding.usernameEdit.text?.toString()?.trim().orEmpty()
        val password = binding.passwordEdit.text?.toString()?.trim().orEmpty()

        var hasError = false
        if (username.isEmpty()) {
            binding.usernameLayout.error = getString(R.string.login_username_required)
            hasError = true
        } else {
            binding.usernameLayout.error = null
        }

        if (password.isEmpty()) {
            binding.passwordLayout.error = getString(R.string.login_password_required)
            hasError = true
        } else {
            binding.passwordLayout.error = null
        }

        if (hasError) {
            return
        }

        toggleLoading(true)
        binding.loginMessage.visibility = View.GONE

        Thread {
            val result = runCatching { doLoginRequest(username, password) }
            runOnUiThread {
                toggleLoading(false)
                result.onSuccess { response ->
                    if (response.status == true) {
                        val user = response.data
                        Toast.makeText(
                            this,
                            "Bienvenido ${user?.name ?: user?.username ?: username}",
                            Toast.LENGTH_SHORT
                        ).show()
                        goToCatalog(user)
                    } else {
                        showError(response.message ?: getString(R.string.login_error_default))
                    }
                }.onFailure { error ->
                    showError(error.message ?: getString(R.string.login_error_default))
                }
            }
        }.start()
    }

    private fun doLoginRequest(username: String, password: String): ResponseData {
        val url = URL(ApiConfig.LOGIN_URL)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            doInput = true
            doOutput = true
            connectTimeout = 10000
            readTimeout = 10000
        }

        val body = JSONObject().apply {
            put("username", username)
            put("password", password)
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
            throw IllegalStateException("Respuesta vacia del servidor ($responseCode)")
        }

        val json = JSONObject(responseBody)
        val userJson = json.optJSONObject("data")
        return ResponseData(
            data = parseUser(userJson),
            status = json.optBoolean("status", false),
            message = json.optString("message"),
            code = json.optInt("code", responseCode)
        )
    }

    private fun parseUser(data: JSONObject?): User? {
        data ?: return null
        val roleJson = data.optJSONObject("role")
        val role = roleJson?.let {
            Role(
                id = it.optInt("id"),
                description = it.optString("description")
            )
        }

        return User(
            id = data.optInt("id"),
            name = data.optString("name"),
            lastname = data.optString("lastname"),
            dni = data.optString("dni"),
            address = data.optString("address"),
            email = data.optString("email"),
            username = data.optString("username"),
            password = data.optString("password"),
            role = role,
            avatar = data.optString("avatar")
        )
    }

    private fun showError(message: String) {
        binding.loginMessage.text = message
        binding.loginMessage.visibility = View.VISIBLE
    }

    private fun toggleLoading(state: Boolean) {
        binding.loginButton.isEnabled = !state
        binding.loginProgress.isVisible = state
    }

    private fun goToCatalog(user: User?) {
        val intent = Intent(this, CatalogActivity::class.java)
        intent.putExtra(CatalogActivity.EXTRA_USER, user)
        startActivity(intent)
        finish()
    }
}
