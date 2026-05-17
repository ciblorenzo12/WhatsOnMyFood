package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private TextInputEditText nameEditText, emailEditText;
    private Spinner languageSpinner;
    private ImageView profileImageView;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private StorageReference storageReference;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadImageToFirebase(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.profile_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        storageReference = FirebaseStorage.getInstance().getReference();

        if (currentUser == null) {
            Toast.makeText(this, "Not signed in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        nameEditText = findViewById(R.id.name_edit_text);
        emailEditText = findViewById(R.id.email_edit_text_profile);
        languageSpinner = findViewById(R.id.language_spinner_profile);
        profileImageView = findViewById(R.id.profile_image);

        Button updateProfileButton = findViewById(R.id.update_profile_button);
        Button changePasswordButton = findViewById(R.id.change_password_button);
        Button logoutButton = findViewById(R.id.logout_button);

        setupLanguageSpinner();
        loadUserProfile();

        profileImageView.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        updateProfileButton.setOnClickListener(v -> updateUserProfile());
        changePasswordButton.setOnClickListener(v -> sendPasswordReset());
        logoutButton.setOnClickListener(v -> logout());
    }

    private void loadUserProfile() {
        nameEditText.setText(currentUser.getDisplayName());
        emailEditText.setText(currentUser.getEmail());
        
        Uri photoUrl = currentUser.getPhotoUrl();
        if (photoUrl != null) {
            String highResUrl = photoUrl.toString().replace("=s96-c", "=s300-c"); // 300 is enough for a profile pic
            Picasso.get()
                    .load(highResUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .resize(300, 300)
                    .centerCrop()
                    .into(profileImageView);
        }
    }

    private void uploadImageToFirebase(Uri imageUri) {
        final StorageReference profileImageRef = storageReference.child("profile_images/" + currentUser.getUid() + ".jpg");

        profileImageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> profileImageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    updateUserProfilePhoto(downloadUri);
                }))
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateUserProfilePhoto(Uri photoUrl) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setPhotoUri(photoUrl)
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ProfileActivity.this, "Profile picture updated.", Toast.LENGTH_SHORT).show();
                        loadUserProfile(); // Reload to show the new image
                    } else {
                        Toast.makeText(ProfileActivity.this, "Failed to update profile picture.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupLanguageSpinner() {
        List<LanguageItem> languageList = new ArrayList<>();
        languageList.add(new LanguageItem("English", android.R.drawable.ic_dialog_map));
        languageList.add(new LanguageItem("Spanish", android.R.drawable.ic_dialog_map));
        languageList.add(new LanguageItem("French", android.R.drawable.ic_dialog_map));

        LanguageSpinnerAdapter adapter = new LanguageSpinnerAdapter(this, languageList);
        languageSpinner.setAdapter(adapter);
    }

    private void updateUserProfile() {
        String displayName = nameEditText.getText().toString().trim();

        if (TextUtils.isEmpty(displayName)) {
            nameEditText.setError("Display name cannot be empty.");
            return;
        }

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User profile updated.");
                        Toast.makeText(this, "Display name updated successfully.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to update display name.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendPasswordReset() {
        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setMessage("A password reset link will be sent to your email address. Proceed?")
                .setPositiveButton("Send", (dialog, which) -> {
                    mAuth.sendPasswordResetEmail(currentUser.getEmail())
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Password reset email sent.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Failed to send reset email.", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
