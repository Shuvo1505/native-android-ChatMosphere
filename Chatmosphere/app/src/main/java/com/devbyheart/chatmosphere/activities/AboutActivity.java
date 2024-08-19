package com.devbyheart.chatmosphere.activities;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.devbyheart.chatmosphere.R;
import com.devbyheart.chatmosphere.utilities.NetworkChangeReceiver;

public class AboutActivity extends AppCompatActivity implements NetworkChangeReceiver.
        NetworkChangeListener {
    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        networkChangeReceiver = new NetworkChangeReceiver(this, this);
        registerReceiver(networkChangeReceiver, new IntentFilter
                (ConnectivityManager.CONNECTIVITY_ACTION));
        ImageView back = findViewById(R.id.imageBack);
        TextView textViewLink = findViewById(R.id.redirectGit);
        back.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        String fullText = "Check Source Code on GitHub";
        SpannableString spannableString = new SpannableString(fullText);
        int start = fullText.indexOf("GitHub");
        int end = start + "GitHub".length();

        spannableString.setSpan(new ForegroundColorSpan(getColor(R.color.primary)), start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannableString.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                launchLink();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.linkColor = getColor(R.color.primary);
                ds.setUnderlineText(false);
                super.updateDrawState(ds);
            }
        }, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        textViewLink.setText(spannableString);
        textViewLink.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void launchLink() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/Shuvo1505/native-android-ChatMosphere"));
        startActivity(browserIntent);
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