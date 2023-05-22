package com.meinema.currencyconverter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.google.gson.Gson
import com.meinema.currencyconverter.databinding.ActivityMainBinding
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {
    private lateinit var request: Request
    private lateinit var binding: ActivityMainBinding
    private lateinit var currencyList: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val inputText = binding.textInput
        val spinner = binding.spinner

        // Listener for the EUR input field
        inputText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateUI(request)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Listener for currency selector list
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = currencyList[position]
                updateUI(request, selectedItem)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle the case when nothing is selected
            }
        }

        fetchCurrencyData().start()
    }

    private fun fetchCurrencyData(): Thread {
        return Thread {
            val url = URL("https://open.er-api.com/v6/latest/eur")
            val connection = url.openConnection() as HttpURLConnection

            if (connection.responseCode == 200) {
                val inputSystem = connection.inputStream
                val inputStreamReader = InputStreamReader(inputSystem, "UTF-8")
                val jsonObject = Gson().fromJson(inputStreamReader, JsonObject::class.java)
                request = Request(
                    jsonObject["time_last_update_utc"].asString,
                    Gson().fromJson(jsonObject["rates"], object : TypeToken<Map<String, Double>>() {}.type)
                )
                currencyList = request.rates.keys.toList()
                setupSpinner(currencyList)
                updateUI(request)
                inputStreamReader.close()
                inputSystem.close()
            }
        }
    }


    private fun setupSpinner(currencyList: List<String>) {
        runOnUiThread {
            val spinner = binding.spinner
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencyList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
    }

    private fun updateUI(request: Request, currency: String = "") {
        runOnUiThread {
            val inputText = binding.textInput
            val numericalValue: Double = inputText.text.toString().toDoubleOrNull() ?: 0.0
            binding.tvLastUpdated.text = request.time_last_update_utc

            for ((rateKey, rateValue) in request.rates) { // Explicitly declare the type of the loop variables
                val convertedValue = rateValue * numericalValue
                val textView = when (rateKey) {
                    "EUR" -> binding.newView
                    else -> createDynamicTextView(rateKey)
                }
                textView.text = String.format("%s: %.2f", rateKey, convertedValue)
            }

            // Get the selected currency from the spinner if not provided
            val selectedRate = request.rates[currency] ?: 1.0
            val convertedValue = selectedRate * numericalValue
            binding.newView.text = String.format("%s: %.2f", currency, convertedValue)
        }
    }

    private fun createDynamicTextView(currency: String): TextView {
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val textView = TextView(this)
        textView.layoutParams = layoutParams
        textView.textSize = 16f
        //binding.linearLayout.addView(textView)
        return textView
    }
}

