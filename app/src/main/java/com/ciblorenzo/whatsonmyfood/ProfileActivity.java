package com.ciblorenzo.whatsonmyfood;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Executors;

public class ProfileActivity extends BaseActivity {

    private static final String TAG = "ProfileActivity";

    private TextInputEditText nameEditText, emailEditText;
    private Spinner languageSpinner;
    private ImageView profileImageView;
    private ProgressBar profileImageProgress;
    private MaterialCardView profileImageCard;
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
            Toast.makeText(this, R.string.not_signed_in, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        nameEditText = findViewById(R.id.name_edit_text);
        emailEditText = findViewById(R.id.email_edit_text_profile);
        languageSpinner = findViewById(R.id.language_spinner_profile);
        profileImageView = findViewById(R.id.profile_image);
        profileImageProgress = findViewById(R.id.profile_image_progress);
        profileImageCard = findViewById(R.id.profile_image_card);

        Button updateProfileButton = findViewById(R.id.update_profile_button);
        Button changePasswordButton = findViewById(R.id.change_password_button);
        Button bitwisePlusButton = findViewById(R.id.bitwise_plus_button);
        Button privacyPolicyButton = findViewById(R.id.privacy_policy_button);
        Button clearCachedDataButton = findViewById(R.id.clear_cached_data_button);
        Button logoutButton = findViewById(R.id.logout_button);
        Button deleteAccountButton = findViewById(R.id.delete_account_button);

        setupLanguageSpinner();
        setupThemeSelector();
        loadUserProfile();

        profileImageCard.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        updateProfileButton.setOnClickListener(v -> updateUserProfile());
        changePasswordButton.setOnClickListener(v -> sendPasswordReset());
        bitwisePlusButton.setOnClickListener(v -> startActivity(new Intent(this, SubscriptionActivity.class)));
        privacyPolicyButton.setOnClickListener(v -> startActivity(new Intent(this, PrivacyPolicyActivity.class)));
        clearCachedDataButton.setOnClickListener(v -> confirmCachedDataDeletion(clearCachedDataButton));
        logoutButton.setOnClickListener(v -> logout());
        deleteAccountButton.setOnClickListener(v -> confirmAccountDeletion(deleteAccountButton));
    }

    private void loadUserProfile() {
        nameEditText.setText(currentUser.getDisplayName());
        emailEditText.setText(currentUser.getEmail());
        profileImageView.setImageResource(android.R.drawable.ic_menu_myplaces);

        // Check local cache first for faster loading
        File localFile = new File(getFilesDir(), "profile_cache_" + currentUser.getUid() + ".jpg");
        if (localFile.exists()) {
            Picasso.get()
                    .load(localFile)
                    .placeholder(R.drawable.ic_launcher_background)
                    .resize(300, 300)
                    .centerCrop()
                    .into(profileImageView);
            Log.d(TAG, "Loaded profile picture from local cache.");
        } else {
            Uri photoUrl = currentUser.getPhotoUrl();
            if (photoUrl != null) {
                String highResUrl = photoUrl.toString().replace("=s96-c", "=s300-c");
                Picasso.get()
                        .load(highResUrl)
                        .placeholder(R.drawable.ic_launcher_background)
                        .resize(300, 300)
                        .centerCrop()
                        .into(profileImageView);
                Log.d(TAG, "Loaded profile picture from URL.");
            }
        }
    }

    private void saveImageLocally(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
            File localFile = new File(getFilesDir(), "profile_cache_" + currentUser.getUid() + ".jpg");
            try (FileOutputStream out = new FileOutputStream(localFile)) {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out);
            }
            if (inputStream != null) inputStream.close();
            Log.d(TAG, "Saved profile picture to local cache.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to save image locally", e);
        }
    }

    private void uploadImageToFirebase(Uri imageUri) {
        // Ensure path is correct and bucket is default
        final String path = "profile_images/" + currentUser.getUid() + ".jpg";
        final StorageReference profileImageRef = storageReference.child(path);

        setProfileImageBusy(true);

        Log.d(TAG, "Uploading to: " + path);
        profileImageRef.putFile(imageUri)
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    Log.d(TAG, "Upload progress: " + progress + "%");
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        if (task.getException() != null) throw task.getException();
                        throw new Exception("Upload task failed");
                    }
                    Log.d(TAG, "Upload successful, getting download URL...");
                    return profileImageRef.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        Log.d(TAG, "Download URL obtained: " + downloadUri);
                        updateUserProfilePhoto(downloadUri, imageUri);
                    } else {
                        setProfileImageBusy(false);
                        Exception e = task.getException();
                        String errorMsg = e != null ? e.getMessage() : "Unknown error";
                        Log.e(TAG, "Final step failed: " + errorMsg, e);
                        
                        Toast.makeText(
                                ProfileActivity.this,
                                R.string.profile_photo_update_failed,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void updateUserProfilePhoto(Uri photoUrl, Uri localImageUri) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setPhotoUri(photoUrl)
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveImageLocally(localImageUri);
                        Toast.makeText(ProfileActivity.this, R.string.profile_photo_updated, Toast.LENGTH_SHORT).show();
                        loadUserProfile(); // Reload to show the new image
                    } else {
                        Toast.makeText(ProfileActivity.this, R.string.profile_photo_update_failed, Toast.LENGTH_SHORT).show();
                    }
                    setProfileImageBusy(false);
                });
    }

    private void setProfileImageBusy(boolean busy) {
        profileImageCard.setEnabled(!busy);
        profileImageView.setAlpha(busy ? 0.55f : 1f);
        profileImageProgress.setVisibility(busy ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void setupThemeSelector() {
        MaterialButtonToggleGroup themeToggleGroup = findViewById(R.id.theme_toggle_group);
        int selectedId = ThemeManager.isDarkMode(this)
                ? R.id.theme_dark_button
                : R.id.theme_light_button;
        themeToggleGroup.check(selectedId);
        themeToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            boolean darkMode = checkedId == R.id.theme_dark_button;
            if (darkMode != ThemeManager.isDarkMode(ProfileActivity.this)) {
                ThemeManager.setDarkMode(ProfileActivity.this, darkMode);
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
            nameEditText.setError(getString(R.string.display_name_required));
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
        String email = currentUser.getEmail();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, R.string.password_reset_email_unavailable, Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.change_password)
                .setMessage(getString(R.string.password_reset_confirmation, email))
                .setPositiveButton(R.string.send, (dialog, which) -> {
                    mAuth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, R.string.password_reset_sent, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, R.string.password_reset_failed, Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void confirmCachedDataDeletion(Button clearCachedDataButton) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_cached_data)
                .setMessage(R.string.clear_cached_data_confirmation)
                .setPositiveButton(R.string.clear_cached_data, (dialog, which) ->
                        clearCachedData(clearCachedDataButton))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void clearCachedData(Button clearCachedDataButton) {
        clearCachedDataButton.setEnabled(false);
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean success = true;
            try {
                AppDatabase.getDatabase(getApplicationContext())
                        .productDao()
                        .clearCachedProductData();
                success = deleteDirectoryContents(getCacheDir());

                File cachedProfile = new File(
                        getFilesDir(),
                        "profile_cache_" + currentUser.getUid() + ".jpg"
                );
                if (cachedProfile.exists() && !cachedProfile.delete()) {
                    success = false;
                    Log.w(TAG, "Could not remove the cached profile image");
                }
            } catch (Exception error) {
                success = false;
                Log.e(TAG, "Could not clear all cached data", error);
            }

            boolean completed = success;
            runOnUiThread(() -> {
                clearCachedDataButton.setEnabled(true);
                Toast.makeText(
                        this,
                        completed ? R.string.cached_data_cleared : R.string.cached_data_clear_failed,
                        Toast.LENGTH_LONG
                ).show();
            });
        });
    }

    private boolean deleteDirectoryContents(File directory) {
        if (directory == null || !directory.exists()) return true;
        File[] children = directory.listFiles();
        if (children == null) return false;

        boolean success = true;
        for (File child : children) {
            if (child.isDirectory() && !deleteDirectoryContents(child)) {
                success = false;
            }
            if (!child.delete()) {
                success = false;
                Log.w(TAG, "Could not remove cached path: " + child.getName());
            }
        }
        return success;
    }

    private void confirmAccountDeletion(Button deleteAccountButton) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_account)
                .setMessage(R.string.delete_account_confirmation)
                .setPositiveButton(R.string.delete_permanently, (dialog, which) ->
                        deleteAccount(deleteAccountButton))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteAccount(Button deleteAccountButton) {
        if (currentUser == null) return;

        deleteAccountButton.setEnabled(false);
        String uid = currentUser.getUid();
        StorageReference profileImage = storageReference.child("profile_images/" + uid + ".jpg");
        profileImage.delete().addOnCompleteListener(storageTask -> {
            if (!storageTask.isSuccessful() && !isMissingStorageObject(storageTask.getException())) {
                deleteAccountButton.setEnabled(true);
                Toast.makeText(this, R.string.account_delete_storage_failed, Toast.LENGTH_LONG).show();
                return;
            }
            deleteFirebaseAccount(uid, deleteAccountButton);
        });
    }

    private boolean isMissingStorageObject(Exception exception) {
        return exception instanceof StorageException
                && ((StorageException) exception).getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND;
    }

    private void deleteFirebaseAccount(String uid, Button deleteAccountButton) {
        currentUser.delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                clearLocalAccountData(uid);
                return;
            }

            deleteAccountButton.setEnabled(true);
            Exception error = task.getException();
            if (error instanceof FirebaseAuthRecentLoginRequiredException) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.sign_in_again_title)
                        .setMessage(R.string.account_delete_reauth_required)
                        .setPositiveButton(R.string.log_out, (dialog, which) -> logout())
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            } else {
                Log.e(TAG, "Account deletion failed", error);
                Toast.makeText(this, R.string.account_delete_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void clearLocalAccountData(String uid) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase.getDatabase(getApplicationContext()).clearAllTables();
                getSharedPreferences("bitwise_plus", MODE_PRIVATE).edit().clear().apply();
                getSharedPreferences("open_food_facts_contribution", MODE_PRIVATE).edit().clear().apply();
                File cachedProfile = new File(getFilesDir(), "profile_cache_" + uid + ".jpg");
                if (cachedProfile.exists() && !cachedProfile.delete()) {
                    Log.w(TAG, "Could not remove cached profile image after account deletion");
                }
            } catch (Exception error) {
                Log.e(TAG, "Account was deleted, but local cleanup was incomplete", error);
            }

            runOnUiThread(() -> {
                mAuth.signOut();
                Toast.makeText(this, R.string.account_deleted, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, SignInActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        });
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
