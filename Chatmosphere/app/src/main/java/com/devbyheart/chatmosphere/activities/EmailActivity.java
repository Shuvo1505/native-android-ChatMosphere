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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.devbyheart.chatmosphere.R;
import com.devbyheart.chatmosphere.databinding.ActivityEmailBinding;
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

public class EmailActivity extends AppCompatActivity implements NetworkChangeReceiver.
        NetworkChangeListener {
    private ActivityEmailBinding binding;
    private EncryptionManager encryptionManager;
    private String encryptedPass;
    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        networkChangeReceiver = new NetworkChangeReceiver(this, this);
        registerReceiver(networkChangeReceiver, new IntentFilter
                (ConnectivityManager.CONNECTIVITY_ACTION));
        loadServices();
        setListeners();
    }

    private void setListeners() {
        binding.actionChangeEmail.setOnClickListener(V -> {
            if (isValidSignUpDetails()) {
                checkRedundancy();
            }
        });
        binding.imageBackpc.setOnClickListener(V -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void loadServices() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(getApplicationContext(),
                R.color.background));
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private boolean isValidSignUpDetails() {
        if (Objects.requireNonNull(binding.emailField.getText()).toString().trim().isEmpty()) {
            binding.emailField.setError("Required");
            showToast("Please enter your email");
            return false;
        } else if (Objects.requireNonNull(binding.passwordField.getText()).toString().trim().isEmpty()) {
            showToast("Please enter your password");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.emailField.getText().
                toString()).matches()) {
            showToast("Please enter valid email");
            return false;
        } else {
            return true;
        }
    }

    private void loading(boolean isLoading) {
        if (isLoading) {
            binding.actionChangeEmail.setVisibility(View.INVISIBLE);
            binding.loadChangeEmail.setVisibility(View.VISIBLE);
        } else {
            binding.actionChangeEmail.setVisibility(View.VISIBLE);
            binding.loadChangeEmail.setVisibility(View.INVISIBLE);
        }
    }

    private void checkRedundancy() {
        loading(true);
        KeyboardManager.hideKeyboard(binding.getRoot());
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, Objects.requireNonNull(binding.emailField.getText()).toString())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        loading(false);
                        showToast("Provided email is already registered");
                        binding.emailField.setText("");
                    } else {
                        proceedWithEmailUpdate();
                    }
                })
                .addOnFailureListener(exception -> {
                    loading(false);
                    showToast(exception.getMessage());
                });
    }

    private void proceedWithEmailUpdate() {
        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
        KeyboardManager.hideKeyboard(binding.getRoot());
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String email = Objects.requireNonNull(binding.emailField.getText()).toString();
        String password = Objects.requireNonNull(binding.passwordField.getText()).toString();

        String uid = preferenceManager.getString(Constants.KEY_USER_ID);

        if (uid != null) {
            loading(true);
            database.collection(Constants.KEY_COLLECTION_USERS)
                    .document(uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot documentSnapshot = task.getResult();
                            String KEY_LOCK = documentSnapshot.getString(Constants.KEY_LOCKER);

                            try {
                                encryptionManager = new EncryptionManager();
                                encryptedPass = encryptionManager.encrypt(KEY_LOCK, password);
                            } catch (GeneralSecurityException ex) {
                                showToast(ex.getMessage());
                                return;
                            }

                            if (encryptedPass.equals(documentSnapshot.getString(Constants.KEY_PASSWORD))) {
                                HashMap<String, Object> updates = new HashMap<>();
                                updates.put(Constants.KEY_EMAIL, email);
                                database.collection(Constants.KEY_COLLECTION_USERS)
                                        .document(uid) // Reference to the existing document
                                        .update(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            preferenceManager.putString(Constants.KEY_EMAIL, email);
                                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            showToast("Email updated successfully");
                                            loading(false);
                                        })
                                        .addOnFailureListener(exception -> showToast(exception.getMessage()));
                            } else {
                                loading(false);
                                showToast("Wrong Password");
                            }
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