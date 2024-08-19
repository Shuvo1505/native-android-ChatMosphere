package com.devbyheart.chatmosphere.activities;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.devbyheart.chatmosphere.R;
import com.devbyheart.chatmosphere.databinding.ActivityForgotPasswordBinding;
import com.devbyheart.chatmosphere.events.KeyboardManager;
import com.devbyheart.chatmosphere.utilities.Constants;
import com.devbyheart.chatmosphere.utilities.EncryptionManager;
import com.devbyheart.chatmosphere.utilities.NetworkChangeReceiver;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ForgotPasswordActivity extends AppCompatActivity implements NetworkChangeReceiver.
        NetworkChangeListener {
    private ActivityForgotPasswordBinding binding;
    private EncryptionManager encryptionManager;
    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        networkChangeReceiver = new NetworkChangeReceiver(this, this);
        registerReceiver(networkChangeReceiver, new IntentFilter
                (ConnectivityManager.CONNECTIVITY_ACTION));
        loadServices();
        setListeners();
    }

    private void setListeners() {
        binding.actionForgotPassword.setOnClickListener(V -> {
            if (isValidSignInDetails()) {
                proceedWithPasswordChange();
            }
        });
        binding.imageBackpcdx.setOnClickListener(V -> getOnBackPressedDispatcher().onBackPressed());
    }

    private AlertDialog.Builder getBuilder() {
        AlertDialog.Builder sho = new AlertDialog.Builder(ForgotPasswordActivity.this,
                R.style.DarkAlertDialog);
        sho.setCancelable(true);
        sho.setMessage("Password Requirements:\n\n" +
                "-> Password should contain at least length of 8 characters\n" +
                "-> A digit must occur at least once\n" +
                "-> A lower case letter must occur at least once\n" +
                "-> An upper case letter must occur at least once\n" +
                "-> A special character must occur at least once\n" +
                "-> No whitespace allowed in the entire password");
        sho.setTitle("Password Pattern Mismatch");
        sho.setIcon(R.drawable.warning);
        return sho;
    }

    private void loading(boolean isLoading) {
        if (isLoading) {
            binding.actionForgotPassword.setVisibility(View.INVISIBLE);
            binding.loadForgotPassword.setVisibility(View.VISIBLE);
        } else {
            binding.actionForgotPassword.setVisibility(View.VISIBLE);
            binding.loadForgotPassword.setVisibility(View.INVISIBLE);
        }
    }

    private boolean isValidPassword(final String password) {
        Pattern pattern;
        Matcher matcher;
        final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])" +
                "(?=.*[@#$%^&+=])(?=\\S+$).{6,}$";
        pattern = Pattern.compile(PASSWORD_PATTERN);
        matcher = pattern.matcher(password);
        return matcher.matches();
    }

    private boolean isValidSignInDetails() {
        if (Objects.requireNonNull(binding.emailFieldFP.getText()).toString().trim().isEmpty()) {
            binding.emailFieldFP.setError("Required");
            showToast("Please enter your email");
            return false;
        } else if (Objects.requireNonNull(binding.passwordFieldOldFP.getText()).toString().trim().isEmpty()) {
            showToast("Please enter your old password");
            return false;
        } else if (Objects.requireNonNull(binding.passwordFieldFP.getText()).toString().trim().isEmpty()) {
            showToast("Please enter your new password");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.emailFieldFP.getText().toString()).matches()) {
            showToast("Please enter valid email");
            return false;
        } else if (!isValidPassword(binding.passwordFieldFP.getText().toString().trim())) {
            AlertDialog.Builder sho = getBuilder();
            sho.setPositiveButton("Dismiss", null);
            sho.show();
            return false;
        } else {
            return true;
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void loadServices() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(getApplicationContext(),
                R.color.background));
    }

    private void proceedWithPasswordChange() {
        KeyboardManager.hideKeyboard(binding.getRoot());
        FirebaseFirestore database = FirebaseFirestore.getInstance();

        String enteredEmail = Objects.requireNonNull(binding.emailFieldFP.getText()).toString();
        String newPassword = Objects.requireNonNull(binding.passwordFieldFP.getText()).toString();
        String currentPassword = Objects.requireNonNull(binding.passwordFieldOldFP.getText()).toString();

        loading(true);

        // Query Firestore to find the user based on email
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, enteredEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        String uid = documentSnapshot.getId();
                        String storedEncryptedPassword = documentSnapshot.getString(Constants.KEY_PASSWORD);
                        String keyLocker = documentSnapshot.getString(Constants.KEY_LOCKER);

                        try {
                            encryptionManager = new EncryptionManager();
                            String encryptedPassOld = encryptionManager.encrypt(keyLocker, currentPassword);
                            String encryptedPassNew = encryptionManager.encrypt(keyLocker, newPassword);

                            // Check if the entered old password matches the stored encrypted password
                            if (!encryptedPassOld.equals(storedEncryptedPassword)) {
                                showToast("Old password is not correct");
                                loading(false);
                                return;
                            }

                            // Proceed with updating the password
                            updatePassword(uid, encryptedPassNew);

                        } catch (GeneralSecurityException ex) {
                            showToast("Encryption error: " + ex.getMessage());
                            loading(false);
                        }
                    } else {
                        loading(false);
                        showToast("User not registered or incorrect email");
                    }
                })
                .addOnFailureListener(exception -> {
                    loading(false);
                    showToast(exception.getMessage());
                });
    }

    private void updatePassword(String uid, String encryptedPassNew) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();

        // Prepare the updates
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_PASSWORD, encryptedPassNew);

        // Update the user's password
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    showToast("Password reset successfully");
                    loading(false);
                })
                .addOnFailureListener(exception -> {
                    showToast("Failed to reset password: " + exception.getMessage());
                    loading(false);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkChangeReceiver != null) {
            unregisterReceiver(networkChangeReceiver);
        }
    }

    @Override
    public void onNetworkConnected() {
    }

    @Override
    public void onNetworkDisconnected() {
    }
}