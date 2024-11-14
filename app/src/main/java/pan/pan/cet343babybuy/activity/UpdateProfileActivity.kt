package pan.pan.cet343babybuy.activity

import AppConstants
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pan.pan.cet343babybuy.R
import pan.pan.cet343babybuy.databinding.ActivityUpdateProfileBinding
import pan.pan.cet343babybuy.dto.FormKey
import pan.pan.cet343babybuy.models.UserData
import pan.pan.cet343babybuy.utils.FirebaseDatabaseHelper
import pan.pan.cet343babybuy.utils.FirebaseStorageHelper
import pan.pan.cet343babybuy.utils.createImageFile
import pan.pan.cet343babybuy.utils.getUriForFile

class UpdateProfileActivity : AppCompatActivity() {
    private var TAG = "UpdateProfile"
    private lateinit var binding: ActivityUpdateProfileBinding
    private var userId: String? = null
    private var userDetails:UserData? = null
    private lateinit var contextView: View

    private lateinit var imageView: ImageView
    private lateinit var captureImageLauncher: ActivityResultLauncher<Uri>
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private var currentImageUri: Uri? = null
    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateProfileBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        imageView = binding.productImage


        val items = listOf("Male", "Female", "Others")
        val genderAdapter = ArrayAdapter(this, R.layout.list_item, items)
        binding.actvGender.setAdapter(genderAdapter)

        // Check for userId
        if (intent.hasExtra(AppConstants.USERID)) {
            userId = intent.getStringExtra(AppConstants.USERID)
        }

        userId?.let {
            Log.i(TAG, "Fetching user...")
            fetchUser()
        }

        captureImageLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) {
                    imageView.setImageURI(currentImageUri)
                    Log.i(TAG, "Image captured successfully using camera $currentImageUri")
                }
            }

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                currentImageUri = it
                imageView.setImageURI(it)
            }
        }


        binding.bReset.setOnClickListener {
            // if user is focus in any field then make them out of focus
            binding.tietName.clearFocus()
            binding.tietEmail.clearFocus()
            binding.actvGender.clearFocus()
            populateUserDetails()
        }

        binding.bUpdate.setOnClickListener {
            uploadProfileImage()
        }

        binding.productImage.setOnClickListener {
            // select image from gallery
            pickImageFromGallery()
        }

        binding.editImageIcon.setOnClickListener {
            // capture image from camera
            //captureImage()
            requestCameraPermission()
        }

        binding.ibBack.setOnClickListener {
            finish()
        }

    }

    private fun populateUserDetails() {
        binding.tietName.setText(userDetails?.name)
        binding.tietEmail.setText(userDetails?.email)
        //binding.actvGender.setText(userDetails?.gender)
        // set productImage if available from firebase
        userDetails?.profileImage?.let {
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.user_icon)
                .error(R.drawable.ic_baseline_24)
                .into(binding.productImage)
        } ?: run {
            when (userDetails?.gender) {
                "Male" -> {
                    binding.productImage.setImageResource(R.drawable.maleprofile)
                }
                "Female" -> {
                    binding.productImage.setImageResource(R.drawable.femaleprofile)
                }
                else -> {
                    binding.productImage.setImageResource(R.drawable.user_icon)
                }
            }
        }
    }

    private fun captureImage() {
        val imageFile = createImageFile(this)
        currentImageUri = getUriForFile(this, imageFile)
        captureImageLauncher.launch(currentImageUri)
    }

    private fun pickImageFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun isUniqueEmail(email: String): Boolean {
        var isUnique = true
        FirebaseDatabaseHelper.getDatabaseReference("users")
            .orderByChild("email")
            .equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (data in dataSnapshot.children) {
                            val user = data.getValue(UserData::class.java)
                            if (user?.email == email && user.id != userId) {
                                isUnique = false
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Error fetching data", error.toException())
                }
            })
        return isUnique
    }

    private fun uploadProfileImage() {
        currentImageUri?.let {
            coroutineScope.launch {
                try {
                    val url = withContext(Dispatchers.IO){
                        FirebaseStorageHelper.uploadImage(it, "profile/${userId}.jpg")
                    }
                    updateUser(url.toString())
                }catch (e: Exception) {
                    Log.e(TAG, "Error uploading image", e)
                }
            }
        } ?: run {
            updateUser(null);
        }
    }

    private fun updateUser(profileImage: String? = null) {
        val name = binding.tietName.text.toString()
        val email = binding.tietEmail.text.toString()
        val gender = binding.actvGender.text.toString()

        if (email.isEmpty()) {
            validForm(FormKey.EMAIL, "Email is required")
        }

        if (name.isEmpty()) {
            validForm(FormKey.NAME, "Name is required")
        }

        val databaseReference = FirebaseDatabaseHelper.getDatabaseReference("users/$userId")

        // check user entered email is unique or not

        if (!isUniqueEmail(email)) {
            validForm(FormKey.EMAIL, "Email is already taken")
            return
        }

        var userData = UserData(
            id = userId,
            name = name,
            email = email,
            gender = gender,
            profileImage = profileImage,
            password = userDetails?.password
        )

        databaseReference.setValue(userData).addOnCompleteListener {
            if (it.isSuccessful) {
                Log.i(TAG, "User updated successfully ${it.result}")

                val resultIntent = Intent()
                resultIntent.putExtra("updateSuccess", true)
                setResult(Activity.RESULT_OK, resultIntent)

                finish()
            } else {
                Log.e(TAG, "Error updating user", it.exception)
            }
        }

    }

    private fun validForm(key: FormKey, value: String) {
        when (key) {
            FormKey.EMAIL -> binding.tilEmailLayout.error = value
            FormKey.NAME -> binding.tilNameLayout.error = value
            else -> {}
        }
    }

    private fun fetchUser() {
        showProgressBar(true)
        val databaseReference = FirebaseDatabaseHelper.getDatabaseReference("users/$userId")
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                userDetails = dataSnapshot.getValue(UserData::class.java)
                Log.i(TAG, "User response is: $userDetails")
                populateUserDetails()

                showProgressBar(false)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error fetching data", error.toException())
                showProgressBar(false)
            }

        })
    }

    private fun showProgressBar(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.clMain.visibility = View.GONE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.clMain.visibility = View.VISIBLE
        }
    }

    private fun requestCameraPermission() {
        if (hasCameraPermission()) {
            captureImage()
        } else {
            ActivityCompat.requestPermissions(
                this, CAMERA_PERMISSION, 0
            )
            requestPermissions(CAMERA_PERMISSION, 0)
        }
    }
    private fun hasCameraPermission(): Boolean {
        return CAMERA_PERMISSION.all {
            ContextCompat.checkSelfPermission(
                applicationContext,
                it
            ) == PackageManager.PERMISSION_GRANTED

        }
    }

    companion object {
        private val CAMERA_PERMISSION = arrayOf(
            Manifest.permission.CAMERA
        )
    }

}