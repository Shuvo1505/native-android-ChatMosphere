package com.devbyheart.chatmosphere.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.devbyheart.chatmosphere.R;
import com.devbyheart.chatmosphere.databinding.ItemContainerRecentChatsBinding;
import com.devbyheart.chatmosphere.listeners.ConversationListeners;
import com.devbyheart.chatmosphere.models.ChatMessage;
import com.devbyheart.chatmosphere.models.UserSection;
import com.devbyheart.chatmosphere.utilities.Constants;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class RecentConversationAdapter extends RecyclerView.Adapter<RecentConversationAdapter.ConversationViewHolder> {
    private final ConversationListeners conversationListeners;
    private final List<ChatMessage> chatMessages;
    private ListenerRegistration listenerRegistration;

    public RecentConversationAdapter(List<ChatMessage> chatMessages, ConversationListeners conversationListeners) {
        this.chatMessages = chatMessages;
        this.conversationListeners = conversationListeners;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversationViewHolder(
                ItemContainerRecentChatsBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        holder.setData(chatMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    private Bitmap getConversationImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    @Override
    public void onViewRecycled(@NonNull ConversationViewHolder holder) {
        super.onViewRecycled(holder);
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    public class ConversationViewHolder extends RecyclerView.ViewHolder {
        ItemContainerRecentChatsBinding binding;

        ConversationViewHolder(ItemContainerRecentChatsBinding itemContainerRecentChatsBinding) {
            super(itemContainerRecentChatsBinding.getRoot());
            binding = itemContainerRecentChatsBinding;
        }

        void setData(ChatMessage chatMessage) {
            binding.imageProfile.setImageResource(R.drawable.default_dp);
            binding.textName.setText(chatMessage.conversationName);
            binding.imageProfile.setTag(chatMessage.conversationID);
            listenerRegistration = FirebaseFirestore.getInstance()
                    .collection(Constants.KEY_COLLECTION_USERS)
                    .document(chatMessage.conversationID)
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (error != null) {
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            String updatedImage = documentSnapshot.getString(Constants.KEY_IMAGE);
                            if (binding.imageProfile.getTag().equals(chatMessage.conversationID)) {
                                binding.imageProfile.setImageBitmap(getConversationImage(updatedImage));
                            }
                        } else {
                            binding.imageProfile.setImageResource(R.drawable.default_dp);
                        }
                    });
            if (chatMessage.message.equals("Something was deleted")) {
                binding.textRecentMessage.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
                binding.iconMessage.setVisibility(View.VISIBLE);
            } else {
                binding.textRecentMessage.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
                binding.iconMessage.setVisibility(View.GONE);
            }
            binding.textRecentMessage.setText(chatMessage.message);
            binding.getRoot().setOnClickListener(V -> {
                UserSection user = new UserSection();
                user.id = chatMessage.conversationID;
                user.image = chatMessage.conversationImage;
                user.name = chatMessage.conversationName;
                conversationListeners.onConversationClicked(user);
            });
        }
    }
}