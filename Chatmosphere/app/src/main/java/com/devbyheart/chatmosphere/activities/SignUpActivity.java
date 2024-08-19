package com.devbyheart.chatmosphere.activities;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.devbyheart.chatmosphere.R;
import com.devbyheart.chatmosphere.databinding.ActivitySignUpBinding;
import com.devbyheart.chatmosphere.events.KeyboardManager;
import com.devbyheart.chatmosphere.utilities.Constants;
import com.devbyheart.chatmosphere.utilities.EncryptionManager;
import com.devbyheart.chatmosphere.utilities.GeneratorKey;
import com.devbyheart.chatmosphere.utilities.NetworkChangeReceiver;
import com.devbyheart.chatmosphere.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity implements NetworkChangeReceiver.
        NetworkChangeListener {
    private ActivitySignUpBinding binding;
    private String encryptedPass;
    private String encodedImage;
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            if (result.getData() != null) {
                Uri imageUri = result.getData().getData();
                try {
                    assert imageUri != null;
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(inputStream, null, options);
                    assert inputStream != null;
                    inputStream.close();

                    inputStream = getContentResolver().openInputStream(imageUri);

                    int maxWidth = 800;
                    int maxHeight = 800;

                    options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);
                    options.inJustDecodeBounds = false;
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                    assert inputStream != null;
                    inputStream.close();

                    binding.imageProfile.setImageBitmap(bitmap);
                    binding.textAddImage.setVisibility(View.GONE);
                    assert bitmap != null;
                    encodedImage = encodeImage(bitmap);
                } catch (FileNotFoundException exception) {
                    showToast(exception.getMessage());
                } catch (IOException e) {
                    showToast(e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        }
    });
    private String KEY_LOCK;
    private PreferenceManager preferenceManager;
    private EncryptionManager encryptionManager;
    private NetworkChangeReceiver networkChangeReceiver;

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        networkChangeReceiver = new NetworkChangeReceiver(this, this);
        registerReceiver(networkChangeReceiver, new IntentFilter
                (ConnectivityManager.CONNECTIVITY_ACTION));
        preferenceManager = new PreferenceManager(getApplicationContext());
        encryptionManager = new EncryptionManager();
        setListeners();

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(getApplicationContext(),
                R.color.background));
    }

    private void setListeners() {
        binding.textSignIn.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.buttonSignUp.setOnClickListener(V -> {
            if (isValidSignUpDetails()) {
                signUp();
            }
        });
        binding.layoutImage.setOnClickListener(V -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void signUp() {
        loading(true);
        KeyboardManager.hideKeyboard(binding.getRoot());
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, Objects.requireNonNull(binding.inputEmail.getText()).toString())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        loading(false);
                        showToast("Provided email is already registered");
                    } else {
                        if (encodedImage == null || encodedImage.isEmpty()) {
                            Bitmap defaultBitmap = BitmapFactory.decodeResource(getResources(),
                                    R.drawable.default_dp);
                            encodedImage = encodeImage(defaultBitmap);
                        }
                        if (Objects.requireNonNull(binding.inputAbout.getText()).toString().isEmpty()) {
                            binding.inputAbout.setText("");
                        }
                        proceedWithSignUp();
                    }
                })
                .addOnFailureListener(exception -> {
                    loading(false);
                    showToast(exception.getMessage());
                });
    }

    private void proceedWithSignUp() {
        try {
            KEY_LOCK = GeneratorKey.generateSecureKey(20);
            encryptedPass = encryptionManager.encrypt(KEY_LOCK, Objects.requireNonNull(binding.inputPassword.getText()).toString());
        } catch (GeneralSecurityException ex) {
            showToast(ex.getMessage());
        }
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_FNAME, Objects.requireNonNull(binding.inputFName.getText()).toString());
        user.put(Constants.KEY_EMAIL, Objects.requireNonNull(binding.inputEmail.getText()).toString());
        user.put(Constants.KEY_ABOUT, Objects.requireNonNull(binding.inputAbout.getText()).toString());
        user.put(Constants.KEY_PASSWORD, encryptedPass);
        user.put(Constants.KEY_LOCKER, KEY_LOCK);
        user.put(Constants.KEY_IMAGE, encodedImage);
        database.collection(Constants.KEY_COLLECTION_USERS).add(user).addOnSuccessListener(documentReference -> {
            loading(false);
            preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
            preferenceManager.putBoolean(Constants.KEY_IS_CHAT_LOCKED, false);
            preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
            preferenceManager.putString(Constants.KEY_FNAME, binding.inputFName.getText().toString());
            preferenceManager.putString(Constants.KEY_EMAIL, binding.inputEmail.getText().toString());
            preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }).addOnFailureListener(exception -> {
            loading(false);
            showToast(exception.getMessage());
        });
    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.PNG, 90, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private boolean isValidSignUpDetails() {
        if (Objects.requireNonNull(binding.inputFName.getText()).toString().trim().isEmpty()) {
            binding.inputFName.setError("Required");
            showToast("Please enter your name");
            return false;
        } else if (binding.inputFName.getText().toString().length() > 22) {
            showToast("Name shouldn't be more than 22 characters");
            return false;
        } else if (Objects.requireNonNull(binding.inputEmail.getText()).toString().trim().isEmpty()) {
            binding.inputEmail.setError("Required");
            showToast("Please enter your email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().
                toString()).matches()) {
            showToast("Please enter valid email");
            return false;
        } else if (Objects.requireNonNull(binding.inputPassword.getText()).toString().trim().isEmpty()) {
            showToast("Please enter your password");
            return false;
        } else if (Objects.requireNonNull(binding.inputConfirmPassword.getText()).toString().trim().isEmpty()) {
            showToast("Please confirm your password");
            return false;
        } else if (!binding.inputPassword.getText().toString().
                equals(binding.inputConfirmPassword.getText().toString())) {
            showToast("Your password didn't match");
            return false;
        } else if (!isValidPassword(binding.inputPassword.getText().toString().trim())) {
            AlertDialog.Builder sho = getBuilder();
            sho.setPositiveButton("Dismiss", null);
            sho.show();
            return false;
        } else {
            return true;
        }
    }

    private AlertDialog.Builder getBuilder() {
        AlertDialog.Builder sho = new AlertDialog.Builder(SignUpActivity.this, R.style.DarkAlertDialog);
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
            binding.buttonSignUp.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.buttonSignUp.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
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