package com.devbyheart.chatmosphere.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.devbyheart.chatmosphere.R;
import com.devbyheart.chatmosphere.adapters.RecentConversationAdapter;
import com.devbyheart.chatmosphere.databinding.ActivityMainBinding;
import com.devbyheart.chatmosphere.listeners.ConversationListeners;
import com.devbyheart.chatmosphere.models.ChatMessage;
import com.devbyheart.chatmosphere.models.UserSection;
import com.devbyheart.chatmosphere.utilities.Constants;
import com.devbyheart.chatmosphere.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends BaseActivity implements ConversationListeners {
    private PreferenceManager preferenceManager;
    private ActivityMainBinding binding;
    private List<ChatMessage> conversations;
    private RecentConversationAdapter conversationAdapter;

    @SuppressLint("NotifyDataSetChanged")
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    assert senderId != null;
                    if (!senderId.equals(receiverId)) {
                        ChatMessage chatMessage = new ChatMessage();
                        chatMessage.senderID = senderId;
                        chatMessage.receiverID = receiverId;
                        if (preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)) {
                            chatMessage.conversationImage = documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                            chatMessage.conversationName = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                            chatMessage.conversationID = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        } else {
                            chatMessage.conversationImage = documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                            chatMessage.conversationName = documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                            chatMessage.conversationID = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        }
                        chatMessage.message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                        chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                        conversations.add(chatMessage);
                    }
                } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    for (int i = 0; i < conversations.size(); i++) {
                        String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        if (conversations.get(i).senderID.equals(senderId) && conversations.get(i).receiverID.equals(receiverId)) {
                            conversations.get(i).message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                            conversations.get(i).dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                            break;
                        }
                    }
                }

            }
            Collections.sort(conversations, (obj1, obj2) -> obj2.dateObject.compareTo(obj1.dateObject));
            conversationAdapter.notifyDataSetChanged();
            binding.conversationsRecycler.smoothScrollToPosition(0);
            binding.conversationsRecycler.setVisibility(View.VISIBLE);
            if (conversationAdapter.getItemCount() == 0) {
                showEmpty();
                binding.progressBar.setVisibility(View.GONE);
            } else if (conversationAdapter.getItemCount() > 0) {
                binding.textError.setVisibility(View.GONE);
                binding.imageError.setVisibility(View.GONE);
                binding.progressBar.setVisibility(View.GONE);
            }
            binding.progressBar.setVisibility(View.GONE);
        }
    };
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        init();
        loadUserProfile();
        getToken();
        setListeners();
        listenConversation();
        checkForNotification();
    }

    private void init() {
        conversations = new ArrayList<>();
        conversationAdapter = new RecentConversationAdapter(conversations, this);
        binding.conversationsRecycler.setAdapter(conversationAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void setListeners() {
        binding.imageProfile.setOnClickListener(V -> startActivity(new Intent(getApplicationContext(), ProfileActivity.class)));
        binding.imageSignOut.setOnClickListener(V -> SignOut());
        binding.fabNewChat.setOnClickListener(V -> startActivity(new Intent(getApplicationContext(), UserActivity.class)));
    }

    private void loadUserProfile() {
        binding.greetDisplay.setText(R.string.app_name);
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }

    private void SignOut() {
        android.app.AlertDialog.Builder sho = new AlertDialog.Builder(MainActivity.this, R.style.DarkAlertDialog);
        sho.setCancelable(false);
        sho.setIcon(R.drawable.warning);
        sho.setTitle("Alert");
        sho.setMessage("Do you want to logout ?");
        sho.setPositiveButton("Yes", (dialog, which) -> {
            FirebaseFirestore database = FirebaseFirestore.getInstance();
            DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID));
            HashMap<String, Object> updates = new HashMap<>();
            updates.put(Constants.KEY_FCM, FieldValue.delete());
            documentReference.update(updates).addOnSuccessListener(unused -> {
                preferenceManager.clear();
                finish();
                showToast("Logged out");
            }).addOnFailureListener(e -> showToast("Sorry, Unable to log out !"));
        });
        sho.setNegativeButton("No", null);
        sho.show();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void listenConversation() {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID)).addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID)).addSnapshotListener(eventListener);
    }

    private void getToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token) {
        preferenceManager.putString(Constants.KEY_FCM, token);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID));
        documentReference.update(Constants.KEY_FCM, token).addOnFailureListener(e -> {
        });
    }

    private void checkForNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(15052001);
    }

    private void showEmpty() {
        binding.textError.setText(String.format("%s", "You don't have any conversation yet\nTap the \" + \" icon to start a conversation"));
        binding.textError.setVisibility(View.VISIBLE);
        binding.imageError.setVisibility(View.VISIBLE);
    }

    @Override
    public void onConversationClicked(UserSection user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
    }
}