package com.froggocatto.distresscall

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth

class Login : AppCompatActivity() {

    private lateinit var createAccountButton : TextView
    private lateinit var emailInput : EditText
    private lateinit var passwordInput : EditText
    private lateinit var loginButton : Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        getViews()
        setListeners()
    }

    private fun getViews() {
        createAccountButton = findViewById(R.id.create_account_textview)
        emailInput = findViewById(R.id.login_email_input)
        passwordInput = findViewById(R.id.login_password_input)
        loginButton = findViewById(R.id.login_button)
    }

    private fun setListeners() {
        createAccountButton.setOnClickListener {
            run {
                goToRegister()
            }
        }
        loginButton.setOnClickListener {
            run {
                login()
            }
        }
    }

    private fun goToRegister(){
        val intent = Intent(this, Register::class.java);
        startActivity(intent)
    }

    private fun login() {
        val auth = FirebaseAuth.getInstance()
        val email = emailInput.text.toString()
        val password = passwordInput.text.toString()
        auth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
            run {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

}