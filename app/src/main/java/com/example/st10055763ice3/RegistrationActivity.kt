package com.example.st10055763ice3

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class RegistrationActivity : AppCompatActivity() {

    lateinit var etEmail: EditText
    lateinit var etFName: EditText
    lateinit var etLName: EditText
    lateinit var etStNo: EditText
    lateinit var etConfPass: EditText
    lateinit var etQualification: EditText
    private lateinit var etPass: EditText
    private lateinit var btnSignUp: Button
    lateinit var tvRedirectLogin: TextView
    private lateinit var imgButtonPP: ImageButton

    // for picture ref
    private var selectedImageUri: Uri? = null


    // create Firebase authentication object
    private lateinit var auth: FirebaseAuth

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registration)

        // View Bindings
        etEmail = findViewById(R.id.edtEmailR)
        etFName = findViewById(R.id.edtFNameReg)
        etLName = findViewById(R.id.edtLNameReg)
        etStNo = findViewById(R.id.edtStudNoReg)
        etQualification = findViewById(R.id.edtQualificationReg)
        etConfPass = findViewById(R.id.edtCPassR)
        etPass = findViewById(R.id.edtPassR)
        btnSignUp = findViewById(R.id.btnReg)
        tvRedirectLogin = findViewById(R.id.tvRetLogR)
        imgButtonPP = findViewById(R.id.imgBtnUserReg)

        // Initialising auth object
        auth = Firebase.auth

        btnSignUp.setOnClickListener {
            signUpUser()
        }

        // switching from signUp Activity to Login Activity
        tvRedirectLogin.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


        imgButtonPP.setOnClickListener{
            // show image pick options
            val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            val chooserIntent = Intent.createChooser(pickIntent, "Select Image")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(takePictureIntent))

            startActivityForResult(chooserIntent, PICK_IMAGE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE -> {
                    if (data != null) {
                        val imageUri = data.data
                        // Set the selected image to the ImageButton
                        imgButtonPP.setImageURI(imageUri)
                    }
                }
                REQUEST_IMAGE_CAPTURE -> {
                    // Check if the image is captured successfully
                    if (data != null && data.extras != null) {
                        val imageBitmap = data.extras!!.get("data") as Bitmap
                        // Convert Bitmap to URI and set it to ImageButton
                        val imageUri = getImageUriFromBitmap(imageBitmap)
                        imgButtonPP.setImageURI(imageUri)
                    }
                }
            }
        }
    }



    companion object {
        const val PICK_IMAGE = 100 // You can use any integer value
        const val REQUEST_IMAGE_CAPTURE = 101
    }

    private fun signUpUser() {
        val email = etEmail.text.toString()
        val pass = etPass.text.toString()
        val confirmPassword = etConfPass.text.toString()

        // check pass
        if (email.isBlank() || pass.isBlank() || confirmPassword.isBlank()) {
            Toast.makeText(this, "Email and Password can't be blank", Toast.LENGTH_SHORT).show()
            return
        }

        if (pass != confirmPassword) {
            Toast.makeText(this, "Password and Confirm Password do not match", Toast.LENGTH_SHORT)
                .show()
            return
        }
        // If all credential are correct
        // We call createUserWithEmailAndPassword
        // using auth object and pass the
        // email and pass in it.
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(this) {
            if (it.isSuccessful) {
                val userId = auth.currentUser?.uid // Get the user ID
                if (userId != null) {
                    createProfile(email, userId) // Pass the user ID to createProfile
                } else {
                    Toast.makeText(this, "Failed to get user ID", Toast.LENGTH_SHORT).show()
                }
                finish()
            } else {
                Toast.makeText(this, "Singed Up Failed!", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun createProfile(email: String, userId: String) {
        val fName = etFName.text.toString().trim()
        val lName = etLName.text.toString().trim()
        val stNo = etStNo.text.toString().trim()
        val qualification = etQualification.text.toString().trim()

        // Get the URI of the selected image from the ImageButton
        val imageUri = getImageUri()

        // Check if an image is selected
        val hasProfilePicture = imageUri != Uri.EMPTY

        if (email.isNotEmpty() && fName.isNotEmpty() && lName.isNotEmpty() && stNo.isNotEmpty() && qualification.isNotEmpty()) {
            // Save profile info in Firestore
            val profile = hashMapOf(
                "email" to email,
                "firstname" to fName,
                "lastname" to lName,
                "studentNo" to stNo,
                "qualification" to qualification
            )

            // Add imageUrl to the profile if a profile picture is selected
            if (hasProfilePicture) {
                // Upload image to Firebase Storage
                val storageRef = FirebaseStorage.getInstance().reference.child("profile_images/$userId") // Use userId as document name
                val uploadTask = storageRef.putFile(imageUri)

                uploadTask.continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            throw it
                        }
                    }
                    storageRef.downloadUrl
                }.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUri = task.result
                        profile["imageUrl"] = downloadUri.toString() // Save image URL

                        // Add the profile to Firestore with the user's ID as the document name
                        db.collection("profiles")
                            .document(userId)
                            .set(profile) // Use userId as document name
                            .addOnSuccessListener {
                                Toast.makeText(this, "Profile Created Successfully", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, HomeActivity::class.java)
                                startActivity(intent)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to Create Profile", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Failed to Upload Image", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Add the profile to Firestore without the imageUrl
                db.collection("profiles")
                    .document(userId)
                    .set(profile) // Use userId as document name
                    .addOnSuccessListener {
                        Toast.makeText(this, "Profile Created Successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, HomeActivity::class.java)
                        startActivity(intent)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to Create Profile", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }



    // Function to get the URI of the selected image from the ImageButton
    private fun getImageUri(): Uri {
        // Get the drawable of the ImageButton
        val drawable = imgButtonPP.drawable
        // Check if the drawable is a BitmapDrawable
        if (drawable is BitmapDrawable) {
            // Get the Bitmap from the BitmapDrawable
            val bitmap = drawable.bitmap
            // Convert the Bitmap to a URI
            val tempUri = getImageUriFromBitmap(bitmap)
            return tempUri ?: Uri.parse("") // Return the URI
        }
        return Uri.parse("") // Return an empty URI if no image is set
    }

    // Function to convert Bitmap to URI
    private fun getImageUriFromBitmap(bitmap: Bitmap): Uri? {
        var uri: Uri? = null
        try {
            val bytes = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
            val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Title", null)
            uri = Uri.parse(path)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return uri
    }
}