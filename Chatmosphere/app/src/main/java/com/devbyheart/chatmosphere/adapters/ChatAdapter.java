package com.devbyheart.chatmosphere.adapters;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.devbyheart.chatmosphere.R;
import com.devbyheart.chatmosphere.databinding.ItemContainerReceiveMessageBinding;
import com.devbyheart.chatmosphere.databinding.ItemContainerSentMessageBinding;
import com.devbyheart.chatmosphere.models.ChatMessage;
import com.devbyheart.chatmosphere.utilities.Constants;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static int VIEW_TYPE_SENT = 2;
    public static int VIEW_TYPE_RECEIVE = 3;
    private final String senderId;
    private final List<ChatMessage> chatMessages;
    private final Bitmap placeholderImage;
    private Bitmap receiverProfileImage;

    public ChatAdapter(List<ChatMessage> chatMessages, Bitmap receiverProfileImage,
                       String senderId) {
        this.chatMessages = chatMessages;
        this.receiverProfileImage = receiverProfileImage;
        this.senderId = senderId;
        this.placeholderImage = getBitmap();
    }

    private Bitmap getBitmap() {
        byte[] bytes = Base64.decode(Constants.KEY_DEFAULT_DP, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setReceiverProfileImage(Bitmap image) {
        if (image != null) {
            this.receiverProfileImage = image;
        } else {
            this.receiverProfileImage = placeholderImage;
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            return new sentMessageViewHolder(
                    ItemContainerSentMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()), parent, false
                    )
            );
        } else {
            return new receivedMessageViewHolder(
                    ItemContainerReceiveMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()), parent, false
                    )
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage chatMessage = chatMessages.get(position);

        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            ((sentMessageViewHolder) holder).setData(chatMessage);
        } else {
            ((receivedMessageViewHolder) holder).setData(chatMessage, receiverProfileImage);
        }
    }


    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (chatMessages.get(position).senderID.equals(senderId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVE;
        }
    }

    static class sentMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerSentMessageBinding binding;

        sentMessageViewHolder(ItemContainerSentMessageBinding itemContainerSentMessageBinding) {
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;
        }

        void setData(ChatMessage chatMessage) {
            if (Boolean.TRUE.equals(chatMessage.isDeletedForEveryone)) {
                ViewGroup.LayoutParams params = binding.textMessage.getLayoutParams();
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                binding.textMessage.setLayoutParams(params);
                binding.textMessage.setTextColor(Color.GRAY);
                binding.textMessage.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
                binding.delspace.setVisibility(View.VISIBLE);
                binding.delicon.setVisibility(View.VISIBLE);
                binding.delicon.setColorFilter(Color.GRAY);
                binding.textMessage.setText(R.string.message_was_deleted);
            } else {
                ViewGroup.LayoutParams params = binding.textMessage.getLayoutParams();
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                binding.textMessage.setLayoutParams(params);
                binding.textMessage.setTextColor(Color.WHITE);
                binding.textMessage.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
                binding.delspace.setVisibility(View.GONE);
                binding.delicon.setVisibility(View.GONE);
                binding.textMessage.setText(chatMessage.message);
            }
            binding.textDateTime.setText(chatMessage.dateTime);
        }
    }


    static class receivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerReceiveMessageBinding binding;

        receivedMessageViewHolder(ItemContainerReceiveMessageBinding itemContainerReceiveMessageBinding) {
            super(itemContainerReceiveMessageBinding.getRoot());
            binding = itemContainerReceiveMessageBinding;
        }

        void setData(ChatMessage chatMessage, Bitmap receiverProfileImage) {
            if (Boolean.TRUE.equals(chatMessage.isDeletedForEveryone)) {
                ViewGroup.LayoutParams params = binding.textMessage.getLayoutParams();
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                binding.textMessage.setLayoutParams(params);
                binding.textMessage.setTextColor(Color.GRAY);
                binding.textMessage.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
                binding.delicon.setVisibility(View.VISIBLE);
                binding.delicon.setColorFilter(Color.GRAY);
                binding.delspace.setVisibility(View.VISIBLE);
                binding.textMessage.setText(R.string.message_was_deleted);
            } else {
                ViewGroup.LayoutParams params = binding.textMessage.getLayoutParams();
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                binding.textMessage.setLayoutParams(params);
                binding.textMessage.setTextColor(Color.BLACK);
                binding.textMessage.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
                binding.delicon.setVisibility(View.GONE);
                binding.delspace.setVisibility(View.GONE);
                binding.textMessage.setText(chatMessage.message);
            }
            binding.textDateTime.setText(chatMessage.dateTime);
            if (receiverProfileImage != null) {
                binding.imageProfile.setImageBitmap(receiverProfileImage);
            }
        }
    }
}