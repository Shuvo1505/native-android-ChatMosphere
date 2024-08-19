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
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.devbyheart.chatmosphere.R;
import com.devbyheart.chatmosphere.databinding.ActivityViewProfileBinding;
import com.devbyheart.chatmosphere.utilities.Constants;
import com.devbyheart.chatmosphere.utilities.NetworkChangeReceiver;
import com.devbyheart.chatmosphere.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class ViewProfile extends AppCompatActivity implements NetworkChangeReceiver.
        NetworkChangeListener {
    private ActivityViewProfileBinding binding;
    private PreferenceManager preferenceManager;
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

                    binding.userProfileImage.setImageBitmap(bitmap);
                    binding.imageSet.setVisibility(View.VISIBLE);
                    binding.optionGap.setVisibility(View.VISIBLE);
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
        binding = ActivityViewProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        networkChangeReceiver = new NetworkChangeReceiver(this, this);
        registerReceiver(networkChangeReceiver, new IntentFilter
                (ConnectivityManager.CONNECTIVITY_ACTION));
        preferenceManager = new PreferenceManager(getApplicationContext());
        loadUserImage();
        setListeners();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
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

    private void loadUserImage() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(getApplicationContext(),
                R.color.background));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE),
                Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.userProfileImage.setImageBitmap(bitmap);
    }

    private void loading(boolean isLoading) {
        if (isLoading) {
            binding.optionSetter.setVisibility(View.INVISIBLE);
            binding.loadImage.setVisibility(View.VISIBLE);
        } else {
            binding.optionSetter.setVisibility(View.VISIBLE);
            binding.loadImage.setVisibility(View.INVISIBLE);
        }
    }

    private void setListeners() {
        binding.imageBackp.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.imageUpload.setOnClickListener(V -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
        binding.imageSet.setOnClickListener(v -> {
            loading(true);
            FirebaseFirestore database = FirebaseFirestore.getInstance();
            String userDocId = preferenceManager.getString(Constants.KEY_USER_ID);

            if (userDocId != null) {
                HashMap<String, Object> updates = new HashMap<>();
                updates.put(Constants.KEY_IMAGE, encodedImage);

                database.collection(Constants.KEY_COLLECTION_USERS)
                        .document(userDocId) // Reference to the existing document
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            showToast("Profile photo updated");
                        })
                        .addOnFailureListener(exception -> showToast(exception.getMessage()));
            } else {
                showToast("Error: User document ID not found.");
                loading(false);
            }
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