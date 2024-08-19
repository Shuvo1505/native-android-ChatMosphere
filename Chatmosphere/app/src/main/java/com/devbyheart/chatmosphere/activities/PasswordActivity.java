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
import com.devbyheart.chatmosphere.databinding.ActivityPasswordBinding;
import com.devbyheart.chatmosphere.events.KeyboardManager;
import com.devbyheart.chatmosphere.utilities.Constants;
import com.devbyheart.chatmosphere.utilities.EncryptionManager;
import com.devbyheart.chatmosphere.utilities.NetworkChangeReceiver;
import com.devbyheart.chatmosphere.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PasswordActivity extends AppCompatActivity implements NetworkChangeReceiver.
        NetworkChangeListener {
    private ActivityPasswordBinding binding;
    private EncryptionManager encryptionManager;
    private String encryptedPassNew;
    private String encryptedPassOld;
    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        networkChangeReceiver = new NetworkChangeReceiver(this, this);
        registerReceiver(networkChangeReceiver, new IntentFilter
                (ConnectivityManager.CONNECTIVITY_ACTION));
        loadServices();
        setListeners();
    }

    private void setListeners() {
        binding.actionChangePassword.setOnClickListener(V -> {
            if (isValidSignInDetails()) {
                proceedWithPasswordChange();
            }
        });
        binding.imageBackpcd.setOnClickListener(V -> getOnBackPressedDispatcher().onBackPressed());
    }

    private AlertDialog.Builder getBuilder() {
        AlertDialog.Builder sho = new AlertDialog.Builder(PasswordActivity.this,
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
            binding.actionChangePassword.setVisibility(View.INVISIBLE);
            binding.loadChangePassword.setVisibility(View.VISIBLE);
        } else {
            binding.actionChangePassword.setVisibility(View.VISIBLE);
            binding.loadChangePassword.setVisibility(View.INVISIBLE);
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
        if (Objects.requireNonNull(binding.emailFieldPE.getText()).toString().trim().isEmpty()) {
            binding.emailFieldPE.setError("Required");
            showToast("Please enter your email");
            return false;
        } else if (Objects.requireNonNull(binding.passwordFieldOldPE.getText()).toString().trim().isEmpty()) {
            showToast("Please enter your old password");
            return false;
        } else if (Objects.requireNonNull(binding.passwordFieldPE.getText()).toString().trim().isEmpty()) {
            showToast("Please enter your new password");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.emailFieldPE.getText().toString()).matches()) {
            showToast("Please enter valid email");
            return false;
        } else if (!isValidPassword(binding.passwordFieldPE.getText().toString().trim())) {
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
        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
        KeyboardManager.hideKeyboard(binding.getRoot());
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String email = Objects.requireNonNull(binding.emailFieldPE.getText()).toString();
        String new_password = Objects.requireNonNull(binding.passwordFieldPE.getText()).toString();
        String old_password = Objects.requireNonNull(binding.passwordFieldOldPE.getText()).toString();

        String uid = preferenceManager.getString(Constants.KEY_USER_ID);

        if (uid != null) {
            loading(true);
            database.collection(Constants.KEY_COLLECTION_USERS)
                    .document(uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot documentSnapshot = task.getResult();
                            String storedEncryptedPassword = documentSnapshot.getString(Constants.KEY_PASSWORD);
                            String storedEmail = documentSnapshot.getString(Constants.KEY_EMAIL);
                            String KEY_LOCK = documentSnapshot.getString(Constants.KEY_LOCKER);

                            try {
                                encryptionManager = new EncryptionManager();
                                encryptedPassOld = encryptionManager.encrypt(KEY_LOCK, old_password);
                                encryptedPassNew = encryptionManager.encrypt(KEY_LOCK, new_password);
                            } catch (GeneralSecurityException ex) {
                                showToast(ex.getMessage());
                                loading(false);
                                return;
                            }

                            // Check if the entered old password matches the stored encrypted password
                            if (!encryptedPassOld.equals(storedEncryptedPassword)) {
                                showToast("Old password is not correct");
                                loading(false);
                                return;
                            }

                            // Check if the entered email matches the stored email
                            if (!email.equals(storedEmail)) {
                                showToast("Wrong email");
                                loading(false);
                                return;
                            }

                            // Proceed with updating the password
                            HashMap<String, Object> updates = new HashMap<>();
                            updates.put(Constants.KEY_PASSWORD, encryptedPassNew);
                            database.collection(Constants.KEY_COLLECTION_USERS)
                                    .document(uid) // Reference to the existing document
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        showToast("Password updated successfully");
                                        loading(false);
                                    })
                                    .addOnFailureListener(exception -> {
                                        showToast(exception.getMessage());
                                        loading(false);
                                    });
                        } else {
                            loading(false);
                            showToast("User not registered");
                        }
                    });
        } else {
            loading(false);
            showToast("Something went wrong");
        }
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