package com.devbyheart.chatmosphere.activities;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.devbyheart.chatmosphere.R;
import com.devbyheart.chatmosphere.databinding.ActivityProfileBinding;
import com.devbyheart.chatmosphere.utilities.Constants;
import com.devbyheart.chatmosphere.utilities.PreferenceManager;

public class ProfileActivity extends BaseActivity {
    private ActivityProfileBinding binding;
    private PreferenceManager preferenceManager;
    private ActivityResultLauncher<Intent> authenticationLauncherForEmail,
            authenticationLauncherForPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
        loadUserDetails();
        loadLockStatus();
        authenticationLauncherForEmail = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent intent = new Intent(this, EmailActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
        );
        authenticationLauncherForPassword = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent intent = new Intent(this, PasswordActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
        );
    }

    private void loadUserDetails() {
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE),
                Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.userImage.setImageBitmap(bitmap);
        String FNAME = preferenceManager.getString(Constants.KEY_FNAME);
        binding.userName.setText(FNAME);
        binding.userEmail.setText(preferenceManager.getString(Constants.KEY_EMAIL));
        binding.profileName.setText(R.string.user_profile);
    }

    private void loadLockStatus() {
        boolean isLocked = preferenceManager.getBoolean(Constants.KEY_IS_CHAT_LOCKED);
        updateSwitchAppearance(isLocked);
    }

    private void updateSwitchAppearance(boolean isLocked) {
        binding.lockSwitch.setChecked(isLocked);
        int thumbColor = isLocked ? R.color.primary : R.color.white;
        int trackColor = isLocked ? R.color.primary : R.color.black;

        binding.lockSwitch.setThumbTintList(ColorStateList.valueOf(ContextCompat.getColor(getApplicationContext(), thumbColor)));
        binding.lockSwitch.setTrackTintList(ColorStateList.valueOf(ContextCompat.getColor(getApplicationContext(), trackColor)));
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.aboutelement.setOnClickListener(V -> startActivity(new Intent(getApplicationContext(),
                AboutActivity.class)));
        binding.userImage.setOnClickListener(V -> startActivity(new Intent(getApplicationContext(),
                ViewProfile.class)));

        binding.chatlockelement.setOnClickListener(v -> {
            boolean isLocked = !binding.lockSwitch.isChecked();
            updateSwitchAppearance(isLocked);
            preferenceManager.putBoolean(Constants.KEY_IS_CHAT_LOCKED, isLocked);
        });

        binding.changeEmail.setOnClickListener(V -> authenticateUserForEmail());
        binding.changePassword.setOnClickListener(V -> authenticateUserForPassword());
        binding.imageMore.setOnClickListener(this::showPopupMenu);
    }

    private void authenticateUserForEmail() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        if (keyguardManager.isKeyguardSecure()) {
            Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(
                    "Authentication Required",
                    "You requested to change your email address"
            );
            if (intent != null) {
                authenticationLauncherForEmail.launch(intent);
            }
        } else {
            Toast.makeText(this, "No secure lock screen setup was found", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
            finish();
        }
    }

    private void authenticateUserForPassword() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        if (keyguardManager.isKeyguardSecure()) {
            Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(
                    "Authentication Required",
                    "You requested to change your password"
            );
            if (intent != null) {
                authenticationLauncherForPassword.launch(intent);
            }
        } else {
            Toast.makeText(this, "No secure lock screen setup was found", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
            finish();
        }
    }

    private void proceedWithAccountDeletion() {
        Intent intent = new Intent(ProfileActivity.this,
                AccountDeletionActivity.class);
        startActivity(intent);
    }

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.profile_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.delete_account) {
                askDeleteConfirmation();
                return true;
            }
            return false;
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popupMenu.setForceShowIcon(true);
        }
        popupMenu.show();
    }

    private void askDeleteConfirmation() {
        android.app.AlertDialog.Builder sho = new AlertDialog.Builder(ProfileActivity.this, R.style.DarkAlertDialog);
        sho.setIcon(R.drawable.warning);
        sho.setTitle("Account Deletion");
        sho.setMessage("Deleting your account will permanently remove your chats and data. This action cannot be undone. Do you wish to proceed ?");
        sho.setPositiveButton("Proceed", (dialog, which) -> proceedWithAccountDeletion());
        sho.setNegativeButton("Abort", null);
        sho.show();
    }
}