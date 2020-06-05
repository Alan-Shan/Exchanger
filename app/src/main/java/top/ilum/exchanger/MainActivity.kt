package top.ilum.exchanger

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val storage = getSharedPreferences("savedRates", MODE_PRIVATE)
        val okHttpClient = OkHttpClient()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Convert_button.setOnClickListener {
            if (Number1.text.isNotEmpty()) {
                var clipper: String
                var calculatedValue: String
                val currencyValue = Number1.text.toString()
                val currencyFrom = Currency1.selectedItem.toString()
                val currencyTo = Currency2.selectedItem.toString()
                val url = "https://api.exchangeratesapi.io/latest?base=$currencyFrom"
                val request: Request = Request.Builder().url(url).build()
                val keyboard = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                if (keyboard.isAcceptingText) {
                    keyboard.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
                }

                fun mathCheck(value: Double): String {
                    return if (value % 1.0 == 0.0) {
                        "%.0f".format(value)
                    } else {
                        "%.2f".format(value)
                    }
                }

                fun showResult(calculatedValue: String, clipper: String) {
                    val popUp = Snackbar
                        .make(
                            it,
                            calculatedValue,
                            Snackbar.LENGTH_LONG
                        )
                        .setAction("Копировать") {
                            val clipboard =
                                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("currency", clipper)
                            clipboard.setPrimaryClip(clip)
                        }
                    val textView =
                        popUp.view.findViewById<View>(R.id.snackbar_text) as TextView
                    textView.maxLines = 3
                    popUp.show()

                }
                if (currencyFrom !== currencyTo) {
                okHttpClient.newCall(request).enqueue(object : Callback {
                    @RequiresApi(VERSION_CODES.O)
                    override fun onFailure(call: Call, e: IOException) {
                        if (storage.contains("savedRates")) {
                            val savedRates =
                                JSONObject(storage.getString("savedRates", "def") as String)
                            if (currencyTo !== currencyFrom) {
                                val selectedDestCurrency = savedRates.getJSONObject("rates")
                                    .get(currencyTo).toString().toDouble()
                                calculatedValue =
                                    if (savedRates.get("base").toString() == currencyFrom) {
                                        val mathThingy =
                                            currencyValue.toDouble() * selectedDestCurrency
                                        clipper = mathCheck(mathThingy)
                                        "$currencyValue $currencyFrom = $clipper $currencyTo"
                                    } else {
                                        val mathThingy = currencyValue
                                            .toDouble() / savedRates.getJSONObject("rates")
                                            .get(currencyFrom).toString()
                                            .toDouble() * selectedDestCurrency
                                        clipper = mathCheck(mathThingy).format(mathThingy)
                                        "$currencyValue $currencyFrom = $clipper $currencyTo"

                                    }
                            } else {
                                calculatedValue =
                                    "$currencyValue $currencyFrom = $currencyValue $currencyTo"
                                clipper = currencyValue
                            }
                            val date = LocalDate.parse(savedRates.get("date") as CharSequence?)
                                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                            showResult(
                                "$calculatedValue\nНет подключения к сети\nДанные на $date",
                                clipper
                            )
                        } else {
                            Snackbar.make(
                                it,
                                "К сожалению, при первом запуске требуется подключение к сети",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onResponse(call: Call?, response: Response?) {
                        if (currencyFrom !== currencyTo) {
                            val json = response?.body()?.string()
                            storage.edit()
                                .putString("savedRates", json).apply()
                            val txt = JSONObject(json as String).getJSONObject("rates")
                                .get(currencyTo).toString()
                            val mathThingy = currencyValue.toDouble() * txt.toDouble()
                            clipper = mathCheck(mathThingy)
                            calculatedValue =
                                "$currencyValue $currencyFrom = $clipper $currencyTo"
                        }
                        else {
                            clipper = currencyValue
                            calculatedValue = "$currencyValue $currencyFrom = $clipper $currencyTo"
                        }
                        showResult(calculatedValue, clipper)
                    }
                }) }
                else {
                    clipper = currencyValue
                    calculatedValue = "$currencyValue $currencyFrom = $clipper $currencyTo"
                    showResult(calculatedValue, clipper)
                }
            }
            else {
                Number1.error = "Поле не может быть пустым!"
            }
        }
    }
}
