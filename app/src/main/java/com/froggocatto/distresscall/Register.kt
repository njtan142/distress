package com.froggocatto.distresscall

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class Register : AppCompatActivity() {
    private lateinit var nameInput : EditText
    private lateinit var ageInput : EditText
    private lateinit var addressInput : EditText
    private lateinit var phoneInput : EditText
    private lateinit var emailInput : EditText
    private lateinit var passwordInput : EditText
    private lateinit var confirmPasswordInput : EditText
    private lateinit var genderSelect : Spinner
    private lateinit var submitButton : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        getViews()
        setListeners()
    }

    private fun getViews() {
        nameInput = findViewById(R.id.name_input)
        ageInput = findViewById(R.id.age_input)
        addressInput = findViewById(R.id.address_input)
        phoneInput = findViewById(R.id.phone_input)
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        confirmPasswordInput = findViewById(R.id.confirm_password_input)
        genderSelect = findViewById(R.id.gender_spinner)
        submitButton = findViewById(R.id.register_submit_button)
    }

    private fun setListeners() {
        submitButton.setOnClickListener {
            run {
                submitButton.isEnabled = false
                registerAccount()
            }
        }
    }

    private fun registerAccount() {
        if(!passwordInput.text.toString().equals(confirmPasswordInput.text.toString())){
            Toast.makeText(this, "Password and Confirm Password does not match", Toast.LENGTH_SHORT).show()
        }

        val auth = FirebaseAuth.getInstance()
        val email = emailInput.text.toString();
        val password = passwordInput.text.toString()
        auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener {
            run {
                val result = it
                createUserData(result)
            }
        }
    }

    private fun createUserData(result:AuthResult) {
        val uid = result.user!!.uid.toString()

        val db = FirebaseFirestore.getInstance()
        val colref = db.collection("users")

        val data = mapOf(
            "name" to nameInput.text.toString(),
            "age" to ageInput.text.toString(),
            "address" to addressInput.text.toString(),
            "phone" to phoneInput.text.toString(),
            "email" to emailInput.text.toString(),
            "gender" to genderSelect.selectedItem.toString(),
        )

        colref.document().set(data).addOnSuccessListener {
            run {
                setAuthDisplayName(data.get("name").toString())
            }
        }
    }

    private fun setAuthDisplayName(displayName : String) {
        val auth = FirebaseAuth.getInstance()
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        auth.currentUser!!.updateProfile(profileUpdates).addOnSuccessListener {
            run {
                Toast.makeText(this, "Successfully Registered!", Toast.LENGTH_SHORT).show()
                finish();
            }
        }
    }

}