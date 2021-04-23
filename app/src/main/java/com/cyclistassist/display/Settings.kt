package com.cyclistassist.display

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import java.io.File

class Settings : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        var diameter: Double = 0.0;

        //if(fileExists(this, "wheelSize")) {
            this.openFileInput("wheelSize").bufferedReader().useLines { lines ->
                val value = lines.fold("") { s, t ->
                    s.plus(t)
                }
                diameter = value.toDouble()
            }
        //}

        findViewById<EditText>(R.id.diameter_input).setText(diameter.toString())

        val saveBtn: Button = findViewById(R.id.save_btn)
        saveBtn.setOnClickListener {
            saveData()
        }
    }

    private fun saveData() {
        val filename = "wheelSize"
        val fileContents: String = findViewById<EditText>(R.id.diameter_input).text.toString()
        this.openFileOutput(filename, Context.MODE_PRIVATE).use {
            it.write(fileContents.toByteArray())
        }

        val intent = Intent(this, MainActivity::class.java)

        startActivity(intent)
    }

    private fun fileExists(context: Context, filename: String?): Boolean {
        val file = context.getFileStreamPath(filename)
        return !(file == null || !file.exists())
    }
}
