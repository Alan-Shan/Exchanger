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
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Convert_button.setOnClickListener {
            if (Number1.text.isNotEmpty()) {
                val keyboard = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                if (keyboard.isAcceptingText) {
                    keyboard.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
                }
                val okHttpClient = OkHttpClient()
                val url = "https://api.exchangeratesapi.io/latest?base=" + Currency1.selectedItem
                val request: Request = Request.Builder().url(url).build()
                var clipper: String
                fun mathCheck(value: Double): String {
                    return if (value % 1.0 == 0.0) {
                        "%.0f"
                    } else {
                        "%.2f"
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
                okHttpClient.newCall(request).enqueue(object : Callback {
                    @RequiresApi(VERSION_CODES.O)
                    override fun onFailure(call: Call, e: IOException) {
                        if (storage.contains("savedRates")) {
                            val savedRates =
                                JSONObject(storage.getString("savedRates", "def") as String)
                            val selectedDestCurrency = savedRates.getJSONObject("rates")
                                .get(Currency2.selectedItem.toString()).toString().toDouble()
                            val calculatedValue: String
                            calculatedValue =
                                if (savedRates.get("base").toString() == Currency1.selectedItem) {
                                    val mathThingy =
                                        Number1.text.toString().toDouble() * selectedDestCurrency
                                    clipper = mathCheck(mathThingy).format(mathThingy)
                                    Number1.text.toString() + " " + Currency1.selectedItem + " = " + clipper + " " + Currency2.selectedItem
                                } else {
                                    val mathThingy = Number1.text.toString()
                                        .toDouble() / savedRates.getJSONObject("rates")
                                        .get(Currency1.selectedItem.toString()).toString()
                                        .toDouble() * selectedDestCurrency
                                    clipper = mathCheck(mathThingy).format(mathThingy)
                                    Number1.text.toString() + " " + Currency1.selectedItem + " = " + clipper + " " + Currency2.selectedItem

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
                        val json = response?.body()?.string()
                        storage.edit()
                            .putString("savedRates", json).apply()
                        val txt = JSONObject(json as String).getJSONObject("rates")
                            .get(Currency2.selectedItem.toString()).toString()
                        val mathThingy = Number1.text.toString().toDouble() * txt.toDouble()
                        clipper = mathCheck(mathThingy).format(mathThingy)
                        val calculatedValue =
                            Number1.text.toString() + " " + Currency1.selectedItem + " = " + clipper + " " + Currency2.selectedItem
                        showResult(calculatedValue, clipper)
                    }
                })
            } else {
                Number1.error = "Поле не может быть пустым!"
            }
        }
    }
}
