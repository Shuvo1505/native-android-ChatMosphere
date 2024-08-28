package com.devbyheart.chatmosphere.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.devbyheart.chatmosphere.R;
import com.devbyheart.chatmosphere.databinding.ActivitySignInBinding;
import com.devbyheart.chatmosphere.events.KeyboardManager;
import com.devbyheart.chatmosphere.utilities.Constants;
import com.devbyheart.chatmosphere.utilities.EncryptionManager;
import com.devbyheart.chatmosphere.utilities.LockManager;
import com.devbyheart.chatmosphere.utilities.NetworkChangeReceiver;
import com.devbyheart.chatmosphere.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.GeneralSecurityException;
import java.util.Calendar;
import java.util.Objects;

public class SignInActivity extends AppCompatActivity implements NetworkChangeReceiver.
        NetworkChangeListener {
    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;
    private String encryptedPass;
    private EncryptionManager encryptionManager;
    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            preferenceManager = new PreferenceManager(getApplicationContext());
            if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
                Intent intent = new Intent(this, LockManager.class);
                startActivity(intent);
                finish();
            }
            encryptionManager = new EncryptionManager();
            binding = ActivitySignInBinding.inflate(getLayoutInflater());
            if (isTablet() || isSmartwatch()) {
                showDeviceNotSupportedError();
            }
            setContentView(binding.getRoot());
            networkChangeReceiver = new NetworkChangeReceiver(this, this);
            registerReceiver(networkChangeReceiver, new IntentFilter
                    (ConnectivityManager.CONNECTIVITY_ACTION));
            greetUserScreen();
            setListeners();

            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(getApplicationContext(),
                    R.color.background));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    private void setListeners() {
        binding.textCreateNewAccount.setOnClickListener(V -> startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));
        binding.buttonSignIn.setOnClickListener(V -> {
            if (isValidSignInDetails()) {
                SignIn();
            }
        });
        binding.textForgotPassword.setOnClickListener(V -> startActivity(new Intent(getApplicationContext(), ForgotPasswordActivity.class)));
    }

    private void SignIn() {
        loading(true);
        KeyboardManager.hideKeyboard(binding.getRoot());
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String email = Objects.requireNonNull(binding.inputEmail.getText()).toString();
        String password = Objects.requireNonNull(binding.inputPassword.getText()).toString();

        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().getDocuments().isEmpty()) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        String KEY_LOCK = documentSnapshot.getString(Constants.KEY_LOCKER);

                        try {
                            encryptionManager = new EncryptionManager();
                            encryptedPass = encryptionManager.encrypt(KEY_LOCK, password);
                        } catch (GeneralSecurityException ex) {
                            showToast(ex.getMessage());
                            loading(false);
                            return;
                        }

                        if (encryptedPass.equals(documentSnapshot.getString(Constants.KEY_PASSWORD))) {
                            preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                            preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                            preferenceManager.putString(Constants.KEY_FNAME, documentSnapshot.getString(Constants.KEY_FNAME));
                            preferenceManager.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE));
                            preferenceManager.putString(Constants.KEY_EMAIL, documentSnapshot.getString(Constants.KEY_EMAIL));

                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            showToast("Wrong Username or Password");
                            resetInputFields();
                        }
                    } else {
                        showToast("Wrong Username or Password");
                        resetInputFields();
                    }
                    loading(false);
                });
    }

    private void resetInputFields() {
        binding.inputEmail.setText("");
        binding.inputPassword.setText("");
        binding.inputEmail.clearFocus();
        binding.inputPassword.clearFocus();
    }

    private void loading(boolean isLoading) {
        if (isLoading) {
            binding.buttonSignIn.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.buttonSignIn.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }


    private boolean isValidSignInDetails() {
        if (Objects.requireNonNull(binding.inputEmail.getText()).toString().trim().isEmpty()) {
            binding.inputEmail.setError("Required");
            showToast("Please enter your email");
            return false;
        } else if (Objects.requireNonNull(binding.inputPassword.getText()).toString().trim().isEmpty()) {
            showToast("Please enter your password");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("Please enter valid email");
            return false;
        } else {
            return true;
        }
    }

    private void greetUserScreen() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour > 0 && hour < 12) {
            binding.greetUser.setText(R.string.good_morning);
        } else if (hour >= 12 && hour < 17) {
            binding.greetUser.setText(R.string.good_afternoon);
        } else if (hour >= 17 && hour < 21) {
            binding.greetUser.setText(R.string.good_evening);
        } else {
            binding.greetUser.setText(R.string.good_evening);
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

    private boolean isTablet() {
        return (getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    private boolean isSmartwatch() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int widthDp = (int) (metrics.widthPixels / metrics.density);
        int heightDp = (int) (metrics.heightPixels / metrics.density);
        return widthDp < 320 && heightDp < 320;
    }

    private void showDeviceNotSupportedError() {
        if (isSmartwatch()) {
            new AlertDialog.Builder(this, R.style.DarkAlertDialog)
                    .setIcon(R.drawable.warning)
                    .setMessage("Not Supported!")
                    .setCancelable(false)
                    .setPositiveButton("Close", (dialog, which) -> finish())
                    .show();
        } else {
            new AlertDialog.Builder(this, R.style.DarkAlertDialog)
                    .setTitle("General Alert")
                    .setIcon(R.drawable.warning)
                    .setMessage("This application is optimized for mobile devices. " +
                            "While all functionalities will remain fully operational, " +
                            "the user experience may differ on this device. For the best experience, " +
                            "It is recommended to use a mobile phone.")
                    .setCancelable(false)
                    .setPositiveButton("Dismiss", null)
                    .show();
        }
    }
}
