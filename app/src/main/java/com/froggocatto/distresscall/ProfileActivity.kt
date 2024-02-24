package com.froggocatto.distresscall

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private var userData: Map<String, Any>? = null

    private lateinit var nameView: TextView
    private lateinit var contactView: TextView
    private lateinit var timelineButton: Button
    private lateinit var profileButton: Button
    private lateinit var logoutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        getViews()
        setListeners()
        getUser()
    }


    private fun getViews() {
        nameView = findViewById(R.id.profile_name)
        contactView = findViewById(R.id.profile_contact)
        timelineButton = findViewById(R.id.incident_timeline_button)
        profileButton = findViewById(R.id.edit_profile_button)
        logoutButton = findViewById(R.id.log_out_button)
    }

    private fun setListeners() {
        timelineButton.setOnClickListener {
            run {
                val intent = Intent(this, TimelineActivity::class.java)
                startActivity(intent)
            }
        }
        logoutButton.setOnClickListener {
            run {
                logoutButton.isEnabled = false
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, Login::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun getUser() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener {
                run {
                    val data = it?.data ?: return@addOnSuccessListener
                    userData = data
                    onUser()
                }
            }
    }

    private fun onUser() {
        if(userData == null) {
            return
        }

        val name = userData!!["name"].toString()
        val contact = userData!!["phone"].toString()

        nameView.text = name
        contactView.text = contact
    }

}