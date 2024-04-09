package com.example.st10055763ice3

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize TextViews and ImageView
        val tvAccEmailPop: TextView = findViewById(R.id.tvAccEmailPop)
        val tvAccFNamePop: TextView = findViewById(R.id.tvAccFNamePop)
        val tvAccLNamePop: TextView = findViewById(R.id.tvAccLNamePop)
        val tvAccStNoPop: TextView = findViewById(R.id.tvAccStNoPop)
        val tvAccQualPop: TextView = findViewById(R.id.tvAccQualPop)
        val ivAccPP: ImageView = findViewById(R.id.ivAccPP)

        // Retrieve currently logged-in user's ID
        val currentUser = auth.currentUser
        val userId = currentUser?.uid

        // Retrieve user data from Firestore based on the user ID
        userId?.let {
            db.collection("profiles").document(it).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val email = document.getString("email")
                        val firstName = document.getString("firstname")
                        val lastName = document.getString("lastname")
                        val studentNumber = document.getString("studentNo")
                        val qualification = document.getString("qualification")
                        val profilePictureUrl = document.getString("imageUrl")

                        // Populate TextViews with retrieved user data
                        tvAccEmailPop.text = email
                        tvAccFNamePop.text = firstName
                        tvAccLNamePop.text = lastName
                        tvAccStNoPop.text = studentNumber
                        tvAccQualPop.text = qualification

                        // Load profile picture from URL
                        profilePictureUrl?.let { url ->
                            DownloadImageTask(ivAccPP).execute(url)
                        }
                    } else {
                        // Document doesn't exist
                        // Handle this case, such as showing a toast or logging a message
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle failures
                    // For example, you can log the error or show a toast
                }
        }
    }


    private inner class DownloadImageTask(val imageView: ImageView) : AsyncTask<String, Void, Bitmap?>() {
        override fun doInBackground(vararg urls: String): Bitmap? {
            val url = urls[0]
            var bitmap: Bitmap? = null
            try {
                val inputStream: InputStream = URL(url).openStream()
                bitmap = BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return bitmap
        }

        override fun onPostExecute(result: Bitmap?) {
            imageView.setImageBitmap(result)
        }
    }
}
