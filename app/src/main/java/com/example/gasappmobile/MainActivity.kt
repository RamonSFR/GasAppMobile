package com.example.gasappmobile

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var gasAutonomy: EditText
    private lateinit var ethAutonomy: EditText
    private lateinit var gasPrice: EditText
    private lateinit var ethPrice: EditText
    private lateinit var resultText: TextView
    private lateinit var calculateButton: Button
    private lateinit var carSpinner: Spinner
    private lateinit var dbHelper: CarDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = CarDatabaseHelper(this)

        carSpinner = findViewById(R.id.carSpinner)
        val carNames = mutableListOf("Selecione um carro")
        carNames.addAll(dbHelper.getAllCars())

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, carNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        carSpinner.adapter = adapter

        carSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    gasAutonomy.text.clear()
                    ethAutonomy.text.clear()
                    return
                }

                val selectedCar = carNames[position]
                dbHelper.getAutonomyByCar(selectedCar)?.let { (gas, eth) ->
                    gasAutonomy.setText(gas.toString())
                    ethAutonomy.setText(eth.toString())
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        gasAutonomy = findViewById(R.id.editGasAutonomy)
        ethAutonomy = findViewById(R.id.editEthAutonomy)
        gasPrice = findViewById(R.id.editGasPrice)
        ethPrice = findViewById(R.id.editEthPrice)
        resultText = findViewById(R.id.textResult)
        calculateButton = findViewById(R.id.buttonCalculate)

        calculateButton.setOnClickListener { sendData() }
    }

    @SuppressLint("SetTextI18n")
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

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

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

                    } catch (e: Exception) {
                        resultText.text = "Erro ao processar os dados da API.\n\nDetalhes: ${e.message}"
                        Toast.makeText(this@MainActivity, "Erro ao processar os dados da API.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
