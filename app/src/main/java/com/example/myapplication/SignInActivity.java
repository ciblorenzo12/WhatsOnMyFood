package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Collections;
import java.util.List;

public class SignInActivity extends BaseActivity {

    private static final String TAG = "SignInActivity";

    private TextInputEditText emailEditText, passwordEditText;
    private Spinner languageSpinner;
    private FirebaseAuth mAuth;
    private boolean languageSpinnerReady;
    private boolean googleSignInInProgress;
    private boolean launchingMainActivity;

    // Launcher for the FirebaseUI sign-in flow (used for Google Sign-In)
    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            this::onSignInResult
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        languageSpinner = findViewById(R.id.language_spinner);
        Button signInButton = findViewById(R.id.sign_in_button);
        Button registerButton = findViewById(R.id.register_button);
        TextView forgotPasswordText = findViewById(R.id.forgot_password_text);
        SignInButton googleSignInButton = findViewById(R.id.google_sign_in_button);

        setupLanguageSpinner();

        signInButton.setOnClickListener(v -> signInUser());
        registerButton.setOnClickListener(v -> registerUser());
        forgotPasswordText.setOnClickListener(v -> showForgotPasswordDialog());

        googleSignInButton.setOnClickListener(v -> startGoogleSignIn());
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && !googleSignInInProgress) {
            launchMainActivity();
        }
    }

    private void startGoogleSignIn() {
        saveSelectedLanguage();
        googleSignInInProgress = true;
        List<AuthUI.IdpConfig> providers = Collections.singletonList(
                new AuthUI.IdpConfig.GoogleBuilder()
                        .build());

        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setCredentialManagerEnabled(false)
                .build();
        signInLauncher.launch(signInIntent);
    }

    private void setupLanguageSpinner() {
        List<LanguageItem> languageList = LanguageManager.getSupportedLanguages();
        LanguageSpinnerAdapter adapter = new LanguageSpinnerAdapter(this, languageList);
        languageSpinner.setAdapter(adapter);
        languageSpinner.setSelection(LanguageManager.getLanguagePosition(this));
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!languageSpinnerReady) {
                    languageSpinnerReady = true;
                    return;
                }

                LanguageItem selectedLanguage = (LanguageItem) parent.getItemAtPosition(position);
                if (selectedLanguage != null && !selectedLanguage.getLanguageCode().equals(LanguageManager.getLanguageCode(SignInActivity.this))) {
                    LanguageManager.setLanguageCode(SignInActivity.this, selectedLanguage.getLanguageCode());
                    recreate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void signInUser() {
        saveSelectedLanguage();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (validateForm(email, password)) {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            launchMainActivity();
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(SignInActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void registerUser() {
        saveSelectedLanguage();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (validateForm(email, password)) {
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            Toast.makeText(SignInActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            launchMainActivity();
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(SignInActivity.this, "Registration failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void showForgotPasswordDialog() {
        String email = emailEditText.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Enter your email address first.");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Forgot Password")
                .setMessage("Send a password reset email to " + email + "?")
                .setPositiveButton("Send", (dialog, which) -> {
                    mAuth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(SignInActivity.this, "Password reset email sent.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(SignInActivity.this, "Failed to send reset email.", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        googleSignInInProgress = false;
        IdpResponse response = result.getIdpResponse();
        if (result.getResultCode() == RESULT_OK) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            Log.d(TAG, "Sign-in successful, user: " + (user != null ? user.getDisplayName() : "Unknown"));
            Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show();
            launchMainActivity();
        } else {
            if (response != null && response.getError() != null) {
                int errorCode = response.getError().getErrorCode();
                String errorMessage = response.getError().getMessage();
                Log.e(TAG, "Sign-in failed. Error Code: " + errorCode + ", Message: " + errorMessage);
                
                String displayMessage = "Sign-in failed: " + errorMessage;
                if (errorCode == 10) { // Common code for DEVELOPER_ERROR
                    displayMessage = "Sign-in failed: Developer Error (10). Check SHA-1 in Firebase Console.";
                }
                Toast.makeText(this, displayMessage, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Sign-in canceled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean validateForm(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Required.");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Required.");
            return false;
        }
        return true;
    }

    private void launchMainActivity() {
        if (launchingMainActivity) {
            return;
        }
        launchingMainActivity = true;
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void saveSelectedLanguage() {
        LanguageItem selectedLanguage = (LanguageItem) languageSpinner.getSelectedItem();
        if (selectedLanguage != null) {
            LanguageManager.setLanguageCode(this, selectedLanguage.getLanguageCode());
        }
    }
}
