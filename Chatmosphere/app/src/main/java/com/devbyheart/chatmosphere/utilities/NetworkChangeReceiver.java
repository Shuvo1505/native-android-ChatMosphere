package com.devbyheart.chatmosphere.utilities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import androidx.appcompat.app.AlertDialog;

import com.devbyheart.chatmosphere.R;

public class NetworkChangeReceiver extends BroadcastReceiver {

    private final NetworkChangeListener networkChangeListener;
    private final Activity activity;
    private AlertDialog alertDialog;

    public NetworkChangeReceiver(Activity activity, NetworkChangeListener networkChangeListener) {
        this.networkChangeListener = networkChangeListener;
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (isConnected(context)) {
            networkChangeListener.onNetworkConnected();
            dismissNetworkDisconnectedDialog();
        } else {
            networkChangeListener.onNetworkDisconnected();
            showNetworkDisconnectedDialog();
        }
    }

    private boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    private void showNetworkDisconnectedDialog() {
        activity.runOnUiThread(() -> {
            if (alertDialog == null || !alertDialog.isShowing()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                        R.style.DarkAlertDialog);
                builder.setCancelable(true);
                builder.setMessage("It seems that you're currently not connected to the internet." +
                        " Please check your connection and try again.");
                builder.setTitle("Connectivity Issue");
                builder.setIcon(R.drawable.no_internet);
                alertDialog = builder.create();
                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.setCancelable(false);
                alertDialog.show();
            }
        });
    }

    private void dismissNetworkDisconnectedDialog() {
        activity.runOnUiThread(() -> {
            if (alertDialog != null && alertDialog.isShowing()) {
                alertDialog.dismiss();
                alertDialog = null;
            }
        });
    }

    public interface NetworkChangeListener {
        void onNetworkConnected();

        void onNetworkDisconnected();
    }
}