package com.devbyheart.chatmosphere.activities;

import static io.grpc.okhttp.internal.Platform.logger;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.GridView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.devbyheart.chatmosphere.R;
import com.devbyheart.chatmosphere.adapters.ChatAdapter;
import com.devbyheart.chatmosphere.adapters.EmojiAdapter;
import com.devbyheart.chatmosphere.databinding.ActivityChatBinding;
import com.devbyheart.chatmosphere.listeners.RecyclerItemClickListener;
import com.devbyheart.chatmosphere.models.ChatMessage;
import com.devbyheart.chatmosphere.models.UserSection;
import com.devbyheart.chatmosphere.network.APIClient;
import com.devbyheart.chatmosphere.network.APIService;
import com.devbyheart.chatmosphere.utilities.Constants;
import com.devbyheart.chatmosphere.utilities.EmojiBase;
import com.devbyheart.chatmosphere.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nonnull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends BaseActivity {
    private ActivityChatBinding binding;
    private UserSection receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String conversationID = null;
    private final OnCompleteListener<QuerySnapshot> conversationComplete = task -> {
        if (task.isSuccessful() && task.getResult() != null && !task.getResult().getDocuments().isEmpty()) {
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversationID = documentSnapshot.getId();
        }
    };
    @SuppressLint("NotifyDataSetChanged")
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) return;

        if (value != null) {
            HashSet<String> existingMessageIds = new HashSet<>();
            for (ChatMessage message : chatMessages) {
                existingMessageIds.add(message.conversationID);
            }
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                String messageId = documentChange.getDocument().getId();
                Boolean isDeletedForEveryone = documentChange.getDocument().getBoolean("isDeletedForEveryone");

                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.conversationID = messageId;
                    chatMessage.senderID = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverID = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessage.isDeletedForEveryone = isDeletedForEveryone;

                    if (!existingMessageIds.contains(chatMessage.conversationID)) {
                        chatMessages.add(chatMessage);
                    }
                }

                // Handle deletions
                if (isDeletedForEveryone != null && isDeletedForEveryone) {
                    for (int i = 0; i < chatMessages.size(); i++) {
                        ChatMessage chatMessage = chatMessages.get(i);
                        if (chatMessage.conversationID.equals(messageId)) {
                            chatMessage.isDeletedForEveryone = true;
                            chatMessage.message = getResources().getString(R.string.message_was_deleted);

                            chatAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                }
            }
            database.collection(Constants.KEY_COLLECTION_USERS)
                    .document(receiverUser.id)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document == null || !document.exists()) {
                                binding.textAvailability.setVisibility(View.VISIBLE);
                                binding.textAvailability.setTextColor(ContextCompat.getColor(getApplicationContext(),
                                        R.color.error));
                                binding.textAvailability.setText(R.string.not_an_active_user);
                            }
                        }
                    });

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Collections.sort(chatMessages, Comparator.comparing(obj -> obj.dateObject));
            }
            if (count == 0) {
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size() - (chatMessages.size() - count), chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if (conversationID == null) {
            checkForConversation();
        }
    };
    private Boolean isReceiverAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadReceiverDetails();
        init();
        listenMessage();
    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages, getBitmap(receiverUser.image), preferenceManager.getString(
                Constants.KEY_USER_ID)
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void sendMessage() {
        String messageText = binding.inputMessage.getText().toString();
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_MESSAGE, messageText);
        message.put(Constants.KEY_TIMESTAMP, new Date());

        database.collection(Constants.KEY_COLLECTION_CHAT).add(message)
                .addOnSuccessListener(documentReference -> {
                    if (conversationID != null) {
                        updateConversation(messageText);
                    } else {
                        HashMap<String, Object> conversation = new HashMap<>();
                        conversation.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                        conversation.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_FNAME));
                        conversation.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
                        conversation.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
                        conversation.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
                        conversation.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
                        conversation.put(Constants.KEY_LAST_MESSAGE, messageText);
                        conversation.put(Constants.KEY_TIMESTAMP, new Date());
                        addConversation(conversation);
                    }
                    if (!isReceiverAvailable) {
                        try {
                            JSONArray tokens = new JSONArray();
                            tokens.put(receiverUser.token);
                            JSONObject data = new JSONObject();
                            data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                            data.put(Constants.KEY_FNAME, preferenceManager.getString(Constants.KEY_FNAME));
                            data.put(Constants.KEY_FCM, preferenceManager.getString(Constants.KEY_FCM));
                            data.put(Constants.KEY_MESSAGE, messageText);
                            JSONObject body = new JSONObject();
                            body.put(Constants.REMOTE_MESSAGE_DATA, data);
                            body.put(Constants.REMOTE_MESSAGE_REGISTRATION_ID, tokens);
                            sendNotification(body.toString());
                        } catch (Exception excep) {
                            showToast(excep.getMessage());
                        }
                    }
                    binding.inputMessage.setText(null);
                })
                .addOnFailureListener(e -> showToast("Failed to send message"));
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void sendNotification(String messageBody) {
        APIClient.getClient().create(APIService.class).sendMessage(
                Constants.getRemoteMsgHeaders(), messageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@Nonnull Call<String> call, @Nonnull Response<String> response) {
                if (response.isSuccessful()) {
                    logger.info("Message sent");
                } else {
                    showToast("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(@Nonnull Call<String> call, @Nonnull Throwable t) {
                showToast(t.getMessage());
            }
        });
    }

    private void listenAvailabilityOfReceiver() {
        database.collection(Constants.KEY_COLLECTION_USERS).document(
                receiverUser.id
        ).addSnapshotListener(ChatActivity.this, (value, error) -> {
            if (error != null) {
                return;
            }
            if (value != null) {
                if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                    int availability = Objects.requireNonNull(
                            value.getLong(Constants.KEY_AVAILABILITY)
                    ).intValue();
                    isReceiverAvailable = availability == 1;
                }
                String updatedImage = value.getString(Constants.KEY_IMAGE);
                if (updatedImage != null) {
                    receiverUser.image = updatedImage;
                    chatAdapter.setReceiverProfileImage(getBitmap(updatedImage));
                } else {
                    receiverUser.image = Constants.KEY_DEFAULT_DP;
                    chatAdapter.setReceiverProfileImage(getBitmap(Constants.KEY_DEFAULT_DP));
                }
                receiverUser.token = value.getString(Constants.KEY_FCM);
            }
            if (isReceiverAvailable) {
                binding.textAvailability.setVisibility(View.VISIBLE);
            } else {
                binding.textAvailability.setVisibility(View.GONE);
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void deleteMessageForEveryone(String messageID) {
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_CHAT).document(messageID);
        documentReference.update("isDeletedForEveryone", true)
                .addOnSuccessListener(aVoid -> {
                    showToast("Message deleted");

                    // Update the specific message in the chatMessages list
                    for (int i = 0; i < chatMessages.size(); i++) {
                        ChatMessage message = chatMessages.get(i);
                        if (message.conversationID.equals(messageID)) {
                            message.isDeletedForEveryone = true;
                            message.message = getResources().getString(R.string.message_was_deleted_cp);
                            updateConversation(message.message);
                            chatAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                })
                .addOnFailureListener(e -> showToast("Failed to delete message"));
    }


    private void listenMessage() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.
                        getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id)
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.
                        getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private Bitmap getBitmap(String encodedImage) {
        if (encodedImage != null) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return null;
        }
    }

    private void loadReceiverDetails() {
        receiverUser = (UserSection) getIntent().getSerializableExtra(Constants.KEY_USER);
        assert receiverUser != null;
        binding.textName.setText(receiverUser.name);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.layoutSend.setOnClickListener(V -> {
            if (binding.inputMessage.getText().toString().isEmpty()) {
                Animation blink = AnimationUtils.loadAnimation(getApplicationContext()
                        , R.anim.blink_effect);
                binding.inputMessage.startAnimation(blink);
            } else {
                sendMessage();
            }
        });
        binding.chatRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this,
                binding.chatRecyclerView, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
            }

            @Override
            public void onLongItemClick(View view, int position) {
                ChatMessage chatMessage = chatMessages.get(position);
                if (chatMessage.senderID.equals(preferenceManager.getString(Constants.KEY_USER_ID))) {
                    new AlertDialog.Builder(ChatActivity.this, R.style.DarkAlertDialog)
                            .setTitle("Delete")
                            .setIcon(R.drawable.delete)
                            .setMessage("Are you sure you want to delete this message for everyone?")
                            .setPositiveButton("Delete", (dialog, which) -> deleteMessageForEveryone(chatMessage.conversationID))
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            }
        }));
        binding.inputMessage.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int drawableLeftWidth = binding.inputMessage.getCompoundDrawables()[0].getBounds().width();
                if (event.getX() <= (drawableLeftWidth + binding.inputMessage.getPaddingStart())) {
                    showEmojiPicker();
                    return true;
                }
            }
            return false;
        });
    }

    private void showEmojiPicker() {
        EmojiAdapter adapter = getEmojiAdapter();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        GridView emojiGrid = (GridView) LayoutInflater.from(this).inflate(R.layout.emoji_picker, null);

        emojiGrid.setAdapter(adapter);
        AlertDialog dialog = builder.setView(emojiGrid).create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
        emojiGrid.setOnItemClickListener((parent, view, position, id) -> {
            String selectedEmoji = adapter.getItem(position);
            int cursorPosition = binding.inputMessage.getSelectionStart();
            binding.inputMessage.getText().insert(cursorPosition, selectedEmoji);
            dialog.dismiss();
        });
    }

    private @NonNull EmojiAdapter getEmojiAdapter() {
        String[] emojis = {
                EmojiBase.SMILING_FACE,
                EmojiBase.GRINNING_FACE,
                EmojiBase.ANGRY_FACE,
                EmojiBase.BEAMING_FACE,
                EmojiBase.CRYING_FACE,
                EmojiBase.BLESSED_FACE,
                EmojiBase.FACE_WITH_TEARS,
                EmojiBase.HEART_EYES,
                EmojiBase.KISSING_FACE,
                EmojiBase.RED_HEART,
                EmojiBase.SCREAMING_FACE,
                EmojiBase.SMILING_WITH_SUNGLASSES,
                EmojiBase.THUMBS_UP,
                EmojiBase.THUMBS_DOWN,
                EmojiBase.VICTORY_HAND,
                EmojiBase.WAVING_HAND
        };

        return new EmojiAdapter(this, emojis);
    }


    private String getDateTime(Date date) {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).
                format(date);
    }

    private void addConversation(HashMap<String, Object> conversation) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversation)
                .addOnSuccessListener
                        (documentReference -> conversationID = documentReference.getId());
    }

    private void updateConversation(String message) {
        DocumentReference documentReference = database.collection
                (Constants.KEY_COLLECTION_CONVERSATIONS).document(conversationID);
        documentReference.update(
                Constants.KEY_LAST_MESSAGE, message,
                Constants.KEY_TIMESTAMP, new Date()
        );
    }

    private void checkForConversation() {
        if (!chatMessages.isEmpty()) {
            checkConversationRemotely(
                    preferenceManager.getString(Constants.KEY_USER_ID),
                    receiverUser.id
            );
            checkConversationRemotely(
                    receiverUser.id, preferenceManager.getString(Constants.KEY_USER_ID)
            );
        }
    }

    private void checkConversationRemotely(String senderId, String receiverId) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversationComplete);
    }

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }
}