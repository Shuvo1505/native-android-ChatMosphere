package com.devbyheart.chatmosphere.utilities;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.devbyheart.chatmosphere.activities.MainActivity;

public class LockManager extends AppCompatActivity {
    private ActivityResultLauncher<Intent> authenticationLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());

        authenticationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent intent = new Intent(LockManager.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
        );

        if (preferenceManager.getBoolean(Constants.KEY_IS_CHAT_LOCKED)) {
            authenticateUser();
        } else {
            Intent intent = new Intent(LockManager.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void authenticateUser() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        if (keyguardManager.isKeyguardSecure()) {
            Intent intent = keyguardManager.createConfirmDeviceCredentialIntent("Authentication Required", "You turned on chat protection");
            if (intent != null) {
                authenticationLauncher.launch(intent);
            }
        } else {
            Toast.makeText(this, "No secure lock screen setup was found", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
            finish();
        }
    }
}