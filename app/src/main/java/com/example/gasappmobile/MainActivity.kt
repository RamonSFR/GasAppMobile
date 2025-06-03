package com.example.gasappmobile

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import android.util.Log
import org.json.JSONException


class MainActivity : AppCompatActivity() {

    private lateinit var gasAutonomy: EditText
    private lateinit var ethAutonomy: EditText
    private lateinit var gasPrice: EditText
    private lateinit var ethPrice: EditText
    private lateinit var resultText: TextView
    private lateinit var calculateButton: Button
    private lateinit var resetButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gasAutonomy = findViewById(R.id.editGasAutonomy)
        ethAutonomy = findViewById(R.id.editEthAutonomy)
        gasPrice = findViewById(R.id.editGasPrice)
        ethPrice = findViewById(R.id.editEthPrice)
        resultText = findViewById(R.id.textResult)
        calculateButton = findViewById(R.id.buttonCalculate)
        resetButton = findViewById(R.id.buttonReset)

        calculateButton.setOnClickListener { sendData() }
        resetButton.setOnClickListener { resetForm() }
    }

    private fun sendData() {
        val gasAut = gasAutonomy.text.toString().toFloatOrNull()
        val ethAut = ethAutonomy.text.toString().toFloatOrNull()
        val gasPr = gasPrice.text.toString().toFloatOrNull()
        val ethPr = ethPrice.text.toString().toFloatOrNull()

        if (gasAut == null || ethAut == null || gasPr == null || ethPr == null) {
            Toast.makeText(this, "Preencha todos os campos corretamente.", Toast.LENGTH_SHORT).show()
            return
        }

        resultText.text = "Calculando..."

        val json = JSONObject().apply {
            put("gasConsume", gasAut)
            put("ethConsume", ethAut)
            put("gasPrice", gasPr)
            put("ethPrice", ethPr)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)

        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://gasapp-api.onrender.com/calc")
            .method("POST", body)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Erro ao conectar com a API.", Toast.LENGTH_SHORT).show()
                    resultText.text = "Erro ao conectar com a API.\n\n${e.message}"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    try {
                        if (!response.isSuccessful || response.body == null) {
                            throw IOException("Resposta inválida: código ${response.code}")
                        }

                        val bodyString = response.body!!.string()

                        val jsonResponse = JSONObject(bodyString)
                        val costGas = jsonResponse.getDouble("costForKmGas")
                        val costEth = jsonResponse.getDouble("costForKmEth")
                        val fuel = jsonResponse.getString("mostEfficentFuel")

                        resultText.text = """
                        Custo/km na gasolina: R$ ${"%.2f".format(costGas)}
                        Custo/km no etanol: R$ ${"%.2f".format(costEth)}
                        Mais eficiente: ${if (fuel == "Ethanol") "Etanol" else "Gasolina"}
                    """.trimIndent()

                        resetButton.visibility = View.VISIBLE
                        calculateButton.isEnabled = false

                    } catch (e: Exception) {
                        e.printStackTrace()
                        resultText.text = "Erro ao processar os dados da API.\n\nDetalhes: ${e.message}"
                        Toast.makeText(this@MainActivity, "Erro ao processar os dados da API.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }





    private fun resetForm() {
        gasAutonomy.text.clear()
        ethAutonomy.text.clear()
        gasPrice.text.clear()
        ethPrice.text.clear()
        resultText.text = ""
        calculateButton.isEnabled = true
        resetButton.visibility = View.GONE
    }
}
