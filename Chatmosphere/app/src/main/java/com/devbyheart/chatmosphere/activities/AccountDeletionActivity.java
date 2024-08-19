package com.devbyheart.chatmosphere.activities;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.devbyheart.chatmosphere.R;
import com.devbyheart.chatmosphere.databinding.ActivityAccountDeletionBinding;
import com.devbyheart.chatmosphere.utilities.Constants;
import com.devbyheart.chatmosphere.utilities.EncryptionManager;
import com.devbyheart.chatmosphere.utilities.NetworkChangeReceiver;
import com.devbyheart.chatmosphere.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.GeneralSecurityException;
import java.util.Objects;

public class AccountDeletionActivity extends AppCompatActivity
        implements NetworkChangeReceiver.
        NetworkChangeListener {
    private ActivityAccountDeletionBinding binding;
    private ActivityResultLauncher<Intent> authenticationLauncherForAccountDeletion;
    private PreferenceManager preferenceManager;
    private EncryptionManager encryptionManager;
    private String encryptedPass;
    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAccountDeletionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        networkChangeReceiver = new NetworkChangeReceiver(this, this);
        registerReceiver(networkChangeReceiver, new IntentFilter
                (ConnectivityManager.CONNECTIVITY_ACTION));
        loadServices();
        setListeners();
        authenticationLauncherForAccountDeletion = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        takeActionForAccountDeletion();
                    } else {
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
        );
    }

    private void doUnauthorizedUser() {
        String uid = preferenceManager.getString(Constants.KEY_USER_ID);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constants.KEY_COLLECTION_USERS).document(uid)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    preferenceManager.clear();
                    Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    showToast("Account deleted successfully");
                })
                .addOnFailureListener(e -> showToast("Failed to delete user data"));
    }

    private void takeActionForAccountDeletion() {
        String uid = preferenceManager.getString(Constants.KEY_USER_ID);
        String email = Objects.requireNonNull(binding.emailFieldDA.getText()).toString();
        String password = Objects.requireNonNull(binding.passwordFieldOldDA.getText()).toString();
        FirebaseFirestore database = FirebaseFirestore.getInstance();
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

                            if (encryptedPass.equals(documentSnapshot.getString(Constants.KEY_PASSWORD))
                                    && email.equals(documentSnapshot.getString(Constants.KEY_EMAIL))) {
                                doUnauthorizedUser();
                            } else {
                                loading(false);
                                showToast("Wrong Credentials");
                            }
                        } else {
                            loading(false);
                            showToast("Something went wrong");
                        }
                    });
        }
    }

    private void setListeners() {
        binding.actionDeleteAccount.setOnClickListener(V -> {
            if (isValidSignUpDetails()) {
                authenticateUserForAccountDeletion();
            }
        });
        binding.imageBackpcde.setOnClickListener(V -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void authenticateUserForAccountDeletion() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        if (keyguardManager.isKeyguardSecure()) {
            Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(
                    "Authentication Required",
                    "You requested to delete your account"
            );
            if (intent != null) {
                authenticationLauncherForAccountDeletion.launch(intent);
            }
        } else {
            Toast.makeText(this, "No secure lock screen setup was found", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
            finish();
        }
    }

    private void loadServices() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(getApplicationContext(),
                R.color.background));
    }

    private boolean isValidSignUpDetails() {
        if (Objects.requireNonNull(binding.emailFieldDA.getText()).toString().trim().isEmpty()) {
            binding.emailFieldDA.setError("Required");
            showToast("Please enter your email");
            return false;
        } else if (Objects.requireNonNull(binding.passwordFieldOldDA.getText()).toString().trim().isEmpty()) {
            showToast("Please enter your password");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.emailFieldDA.getText().
                toString()).matches()) {
            showToast("Please enter valid email");
            return false;
        } else {
            return true;
        }
    }

    private void loading(boolean isLoading) {
        if (isLoading) {
            binding.actionDeleteAccount.setVisibility(View.INVISIBLE);
            binding.loadDeleteAccount.setVisibility(View.VISIBLE);
        } else {
            binding.actionDeleteAccount.setVisibility(View.VISIBLE);
            binding.loadDeleteAccount.setVisibility(View.INVISIBLE);
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