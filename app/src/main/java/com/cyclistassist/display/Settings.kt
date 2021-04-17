package com.cyclistassist.display

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class Settings : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        this.openFileInput("wheelSize").bufferedReader().useLines { lines ->
            val value = lines.fold("") { s, t ->
                s.plus(t)
            }

            findViewById<EditText>(R.id.diameter_input).setText(value)
        }

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
}
