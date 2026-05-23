package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ProfileActivity extends BaseActivity {

    private static final String TAG = "ProfileActivity";

    private TextInputEditText nameEditText, emailEditText;
    private Spinner languageSpinner;
    private ImageView profileImageView;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private StorageReference storageReference;
    private boolean languageSpinnerReady;

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
        List<LanguageItem> languageList = LanguageManager.getSupportedLanguages();
        LanguageSpinnerAdapter adapter = new LanguageSpinnerAdapter(this, languageList);
        languageSpinner.setAdapter(adapter);
        languageSpinner.setSelection(LanguageManager.getLanguagePosition(this));
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (!languageSpinnerReady) {
                    languageSpinnerReady = true;
                    return;
                }

                LanguageItem selectedLanguage = (LanguageItem) parent.getItemAtPosition(position);
                if (selectedLanguage != null && !selectedLanguage.getLanguageCode().equals(LanguageManager.getLanguageCode(ProfileActivity.this))) {
                    LanguageManager.setLanguageCode(ProfileActivity.this, selectedLanguage.getLanguageCode());
                    Toast.makeText(ProfileActivity.this, getString(R.string.language_updated), Toast.LENGTH_SHORT).show();
                    recreate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void updateUserProfile() {
        String displayName = nameEditText.getText().toString().trim();
        LanguageItem selectedLanguage = (LanguageItem) languageSpinner.getSelectedItem();

        if (TextUtils.isEmpty(displayName)) {
            nameEditText.setError("Display name cannot be empty.");
            return;
        }

        if (selectedLanguage != null) {
            LanguageManager.setLanguageCode(this, selectedLanguage.getLanguageCode());
        }

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User profile updated.");
                        Toast.makeText(this, getString(R.string.profile_updated), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, getString(R.string.profile_update_failed), Toast.LENGTH_SHORT).show();
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
