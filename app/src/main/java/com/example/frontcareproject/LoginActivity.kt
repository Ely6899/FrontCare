package com.example.frontcareproject

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class LoginActivity : AppCompatActivity() {
    private lateinit var typeText: TextView
    private lateinit var loginBtn: Button
    private lateinit var serverAns: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        typeText = findViewById(R.id.tvLoginType)
        loginBtn = findViewById(R.id.btnLogin)
        serverAns = ""

        val extras = intent.extras
        if (extras != null) {
            val value : String? = extras.getString("token")

            //This looks scary cause android studio is afraid of hardcoded strings :\
            typeText.text = getString(R.string.log_in_welcome).format(value)
        }

        loginBtn.setOnClickListener{
            // start the Thread to connect to server
            //Thread(ClientThread("ping")).start()
            Thread {
                try {
                    val client = Socket("10.0.0.14", 8080)

                    val printReader = BufferedReader(InputStreamReader(client.getInputStream()))
                    val printWriter = PrintWriter(client.getOutputStream(), true)
                    val message = "ping"
                    printWriter.write(message)

                    printWriter.flush()
                    printWriter.close()

                    serverAns = printReader.readLine()
                    //println(serverAns)
                    printReader.close()

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }.start()

        }

    }
//    class ClientThread(private val message: String) : Runnable{
//        override fun run() {
//            try {
//                val client = Socket("10.0.0.14", 8080)
//
//                val printReader = BufferedReader(InputStreamReader(client.getInputStream()))
//                val printWriter = PrintWriter(client.getOutputStream(), true)
//                printWriter.write(message)
//
//                printWriter.flush()
//                printWriter.close()
//
//                val serverResponse = printReader.readLine()
//
//            } catch (e: IOException){
//                e.printStackTrace()
//            }
//        }
//    }
}