package com.devbyheart.chatmosphere.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.devbyheart.chatmosphere.R;

public class EmojiAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final String[] emojis;

    public EmojiAdapter(Context context, String[] emojis) {
        super(context, R.layout.emoji_item, emojis);
        this.context = context;
        this.emojis = emojis;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.emoji_item, parent,
                    false);
        }

        TextView emojiTextView = convertView.findViewById(R.id.emojiTextView);
        emojiTextView.setText(emojis[position]);

        return convertView;
    }
}