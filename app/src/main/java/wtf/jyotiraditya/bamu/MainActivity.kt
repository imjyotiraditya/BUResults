package wtf.jyotiraditya.bamu

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.*
import org.json.JSONObject
import org.json.JSONTokener
import wtf.jyotiraditya.bamu.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

@DelicateCoroutinesApi
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView(this)
    }

    private fun initView(context: Context) {
        fetchData(batchUrl, "batch_code", batches)
        for (i in 0 until binding.toolbar.childCount) {
            val view = binding.toolbar.getChildAt(i)
            if (view is TextView) {
                val typeface = Typeface.createFromAsset(assets, "productsans_bold.ttf")
                view.typeface = typeface
            }
        }
        val typesAdapter = ArrayAdapter(context, R.layout.list_item, types)
        (binding.typeMenu.editText as? MaterialAutoCompleteTextView)?.setAdapter(typesAdapter)

        val batchAdapter = ArrayAdapter(context, R.layout.list_item, batches)
        (binding.batchMenu.editText as? MaterialAutoCompleteTextView)?.setAdapter(batchAdapter)

        (binding.batchMenu.editText as? MaterialAutoCompleteTextView)?.onItemClickListener =
            OnItemClickListener { _, _, position, _ ->
                val selectedValue: String? = batchAdapter.getItem(position)
                fetchData("$semesterUrl$selectedValue", "semester_code", semesters)
            }

        val semesterAdapter = ArrayAdapter(context, R.layout.list_item, semesters)
        (binding.semesterMenu.editText as? MaterialAutoCompleteTextView)?.setAdapter(semesterAdapter)

        binding.clearButton.setOnClickListener {
            binding.studentRollNo.editText?.setText("")
            binding.studentRollNo.error = null
            binding.studentRollNo.isErrorEnabled = false
            binding.semesterMenu.editText?.setText("")
            binding.semesterMenu.error = null
            binding.semesterMenu.isErrorEnabled = false
            binding.batchMenu.editText?.setText("")
            binding.batchMenu.error = null
            binding.batchMenu.isErrorEnabled = false
            binding.typeMenu.editText?.setText("")
            binding.typeMenu.error = null
            binding.typeMenu.isErrorEnabled = false
        }

        binding.downloadPdfButton.setOnClickListener {
            if (binding.typeMenu.editText?.text.isNullOrEmpty()) {
                binding.typeMenu.error = getString(R.string.select_type)
                return@setOnClickListener
            } else {
                binding.typeMenu.error = null
                binding.typeMenu.isErrorEnabled = false
            }
            if (binding.batchMenu.editText?.text.isNullOrEmpty()) {
                binding.batchMenu.error = getString(R.string.select_batch)
                return@setOnClickListener
            } else {
                binding.batchMenu.error = null
                binding.batchMenu.isErrorEnabled = false
            }
            if (binding.semesterMenu.editText?.text.isNullOrEmpty()) {
                binding.semesterMenu.error = getString(R.string.select_semester)
                return@setOnClickListener
            } else {
                binding.semesterMenu.error = null
                binding.semesterMenu.isErrorEnabled = false
            }
            if (binding.studentRollNo.editText?.text.isNullOrEmpty()) {
                binding.studentRollNo.error = getString(R.string.enter_student_roll_number)
                return@setOnClickListener
            } else {
                binding.studentRollNo.error = null
                binding.studentRollNo.isErrorEnabled = false
            }
            val url = genMarkSheetUrl(
                binding.studentRollNo.editText?.text.toString(),
                binding.semesterMenu.editText?.text.toString(),
                binding.batchMenu.editText?.text.toString(),
                binding.typeMenu.editText?.text.toString(),
                "PDF",
                markSheetPdfUrl
            )
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
        binding.viewMarksheetButton.setOnClickListener {
            if (binding.typeMenu.editText?.text.isNullOrEmpty()) {
                binding.typeMenu.error = getString(R.string.select_type)
                return@setOnClickListener
            } else {
                binding.typeMenu.error = null
                binding.typeMenu.isErrorEnabled = false
            }
            if (binding.batchMenu.editText?.text.isNullOrEmpty()) {
                binding.batchMenu.error = getString(R.string.select_batch)
                return@setOnClickListener
            } else {
                binding.batchMenu.error = null
                binding.batchMenu.isErrorEnabled = false
            }
            if (binding.semesterMenu.editText?.text.isNullOrEmpty()) {
                binding.semesterMenu.error = getString(R.string.select_semester)
                return@setOnClickListener
            } else {
                binding.semesterMenu.error = null
                binding.semesterMenu.isErrorEnabled = false
            }
            if (binding.studentRollNo.editText?.text.isNullOrEmpty()) {
                binding.studentRollNo.error = getString(R.string.enter_student_roll_number)
                return@setOnClickListener
            } else {
                binding.studentRollNo.error = null
                binding.studentRollNo.isErrorEnabled = false
            }
            val url = genMarkSheetUrl(
                binding.studentRollNo.editText?.text.toString(),
                binding.semesterMenu.editText?.text.toString(),
                binding.batchMenu.editText?.text.toString(),
                binding.typeMenu.editText?.text.toString(),
                "",
                markSheetDbUrl
            )
            val intent = Intent(this, WebViewActivity::class.java)
            intent.putExtra("url", url)
            startActivity(intent)
        }
    }

    private fun genMarkSheetUrl(
        studentRollNo: String,
        semester: String,
        batch: String,
        cmbType: String,
        type: String?,
        baseUrl: String,
    ): String {
        return if (type == "PDF") {
            "$baseUrl?regn_no=${studentRollNo.encodeToBase64()}&sem=${semester.encodeToBase64()}&batch=${batch.encodeToBase64()}&cmbType=${cmbType}"
        } else {
            "$baseUrl?regn_no=$studentRollNo&sem=$semester&batch=$batch&cmbType=$cmbType&type=GET_DETAILS"
        }
    }

    private fun fetchData(link: String, id: String, arrayList: ArrayList<String>) {
        GlobalScope.launch(Dispatchers.IO) {
            val url = URL(link)
            val urlConnection = url.openConnection() as HttpsURLConnection
            urlConnection.setRequestProperty("Accept", "application/json")
            urlConnection.requestMethod = "GET"
            urlConnection.doInput = true
            urlConnection.doOutput = false
            val responseCode = urlConnection.responseCode
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                val response = urlConnection.inputStream.bufferedReader()
                    .use(BufferedReader::readText)
                withContext(Dispatchers.Main) {
                    val jsonObject = JSONTokener(response).nextValue() as JSONObject
                    val jsonArray = jsonObject.getJSONArray("aaData")
                    if (arrayList.isNotEmpty()) {
                        arrayList.removeAll(arrayList)
                    }
                    for (i in 0 until jsonArray.length()) {
                        arrayList.add(jsonArray.getJSONObject(i).getString(id))
                    }
                }
            }
        }
    }

    companion object {
        private const val markSheetPdfUrl =
            "https://berhampuruniversity.silicontechlab.com/buerp/build/examination/mark_sheet_pdf.php"
        private const val markSheetDbUrl =
            "https://berhampuruniversity.silicontechlab.com/buerp/build/examination/mark_sheet_db.php"

        private const val batchUrl = "${markSheetDbUrl}?type=SELECT_BATCH"
        private const val semesterUrl = "${markSheetDbUrl}?type=SELECT_SEMESTER&batch="

        private val types = arrayListOf("Regular", "Back", "Improvement", "Special", "Re-Back")
        private val batches = ArrayList<String>()
        private val semesters = ArrayList<String>()
    }
}

fun String.encodeToBase64(): String {
    return Base64.encodeToString(this.toByteArray(), Base64.DEFAULT)
}