package com.bookmydoc;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileSetupActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 100;
    private static final int GALLERY_REQUEST = 200;
    private static final int PERMISSION_REQUEST_CODE = 300;

    private CircleImageView profileImageView;
    private EditText fullNameInput, ageInput, emailInput;
    private RadioGroup genderGroup;
    private TextView dobInput;
    private LinearLayout cameraLayout, galleryLayout, selectLayout;
    private ProgressBar progressBar;
    private Uri imageUri;
    private Bitmap profileBitmap;

    private final String AES_KEY = "1234567890123BMD"; // 16-char key

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        profileImageView = findViewById(R.id.profileImageView);
        fullNameInput = findViewById(R.id.fullNameInput);
        ageInput = findViewById(R.id.ageInput);
        emailInput = findViewById(R.id.MDR);
        dobInput = findViewById(R.id.dobInput);
        genderGroup = findViewById(R.id.genderGroup);
        selectLayout = findViewById(R.id.selectImage);
        cameraLayout = findViewById(R.id.camera);
        galleryLayout = findViewById(R.id.gallery);
        progressBar = findViewById(R.id.progressbar);
        Button saveBtn = findViewById(R.id.saveProfileBtn);

        dobInput.setOnClickListener(v -> showDatePicker());

        profileImageView.setOnClickListener(v -> selectLayout.setVisibility(View.VISIBLE));

        cameraLayout.setOnClickListener(v -> {
            if (checkPermission()) openCamera();
            else requestPermission();
        });

        galleryLayout.setOnClickListener(v -> {
            if (checkPermission()) openGallery();
            else requestPermission();
        });

        saveBtn.setOnClickListener(v -> {
            saveBtn.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            if (fullNameInput.getText().toString().isEmpty() && ageInput.getText().toString().isEmpty() && genderGroup.getCheckedRadioButtonId()==-1 ) {
                saveBtn.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(this, "Please fill the all Details", Toast.LENGTH_SHORT).show();
            } else {
                saveBtn.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                saveProfile();
            }
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            dobInput.setText(sdf.format(calendar.getTime()));

            int age = Calendar.getInstance().get(Calendar.YEAR) - year;
            ageInput.setText(String.valueOf(age));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_REQUEST);
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            selectLayout.setVisibility(View.INVISIBLE);
            if (requestCode == CAMERA_REQUEST) {
                profileBitmap = (Bitmap) data.getExtras().get("data");
                profileImageView.setImageBitmap(profileBitmap);
            } else if (requestCode == GALLERY_REQUEST) {
                imageUri = data.getData();
                profileImageView.setImageURI(imageUri);
            }
        }
    }

    private void saveProfile() {
        String name = encrypt(fullNameInput.getText().toString());
        String age = encrypt(ageInput.getText().toString());
        String email = emailInput.getText().toString();

        int selectedGenderId = genderGroup.getCheckedRadioButtonId();
        RadioButton selectedGender = findViewById(selectedGenderId);
        String gender = selectedGender.getText().toString();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser().getUid();

        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("fullName", name);
        userProfile.put("age", age);
        userProfile.put("email", email);
        userProfile.put("gender", gender);
        userProfile.put("dob", dobInput.getText().toString());

        if (profileBitmap != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            profileBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] data = baos.toByteArray();

            StorageReference ref = FirebaseStorage.getInstance().getReference("profile_images/" + userId + ".jpg");
            UploadTask uploadTask = ref.putBytes(data);

            uploadTask.addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                userProfile.put("profileImage", uri.toString());
                db.collection("users").document(userId).set(userProfile).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK|FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }
                });
            }));
        } else {
            // Use default image URL (already uploaded default in Firebase)
            userProfile.put("profileImage", "https://firebasestorage.googleapis.com/v0/b/bookmydoc-a3191.firebasestorage.app/o/profile_lu.png");
            db.collection("users").document(userId).set(userProfile).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK|FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }
            });
        }

        Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show();
    }

    private String encrypt(String data) {
        try {
            Key key = new SecretKeySpec(AES_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encVal = cipher.doFinal(data.getBytes());
            return Base64.encodeToString(encVal, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
