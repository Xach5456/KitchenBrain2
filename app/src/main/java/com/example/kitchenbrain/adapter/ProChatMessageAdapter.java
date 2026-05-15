package com.example.kitchenbrain.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.model.ChatMessage;
import java.util.List;

public class ProChatMessageAdapter extends ListAdapter<ChatMessage, RecyclerView.ViewHolder> {
    
    private static final int TYPE_MY_MESSAGE = 1;
    private static final int TYPE_OTHER_MESSAGE = 2;
    
    private String currentUserId;
    private OnMessageLongClickListener listener;
    
    public interface OnMessageLongClickListener {
        void onMessageLongClick(ChatMessage message, int position);
    }
    
    public ProChatMessageAdapter(String currentUserId, OnMessageLongClickListener listener) {
        super(DIFF_CALLBACK);
        this.currentUserId = currentUserId;
        this.listener = listener;
    }
    
    @Override
    public int getItemViewType(int position) {
        ChatMessage message = getItem(position);
        if (message.getSenderId().equals(currentUserId)) {
            return TYPE_MY_MESSAGE;
        } else {
            return TYPE_OTHER_MESSAGE;
        }
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_MY_MESSAGE) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_my_message, parent, false);
            return new MyMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_other_message, parent, false);
            return new OtherMessageViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = getItem(position);
        
        if (holder instanceof MyMessageViewHolder) {
            ((MyMessageViewHolder) holder).bind(message);
        } else if (holder instanceof OtherMessageViewHolder) {
            ((OtherMessageViewHolder) holder).bind(message);
        }
    }
    
    // My Message ViewHolder (right, blue bubble)
    private class MyMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageText;
        private TextView timestampText;
        private TextView statusText;
        
        public MyMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timestampText = itemView.findViewById(R.id.timestampText);
            statusText = itemView.findViewById(R.id.statusText);
        }
        
        public void bind(ChatMessage message) {
            messageText.setText(message.getText());
            timestampText.setText(formatTimestamp(message.getTimestamp()));
            
            // Show message status
            switch (message.getStatus()) {
                case SENDING:
                    statusText.setText("sending...");
                    statusText.setTextColor(0xFF888888);
                    break;
                case SENT:
                    statusText.setText("sent");
                    statusText.setTextColor(0xFF4CAF50);
                    break;
                case FAILED:
                    statusText.setText("failed");
                    statusText.setTextColor(0xFFFF5252);
                    break;
            }
            
            // Long press listener
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onMessageLongClick(message, position);
                    }
                }
                return true;
            });
        }
    }
    
    // Other Message ViewHolder (left, gray bubble)
    private class OtherMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageText;
        private TextView timestampText;
        
        public OtherMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timestampText = itemView.findViewById(R.id.timestampText);
        }
        
        public void bind(ChatMessage message) {
            messageText.setText(message.getText());
            timestampText.setText(formatTimestamp(message.getTimestamp()));
        }
    }
    
    private String formatTimestamp(com.google.firebase.Timestamp timestamp) {
        if (timestamp == null) return "";
        
        long now = System.currentTimeMillis();
        long messageTime = timestamp.getSeconds() * 1000;
        long diff = now - messageTime;
        
        if (diff < 60000) { // less than 1 minute
            return "now";
        } else if (diff < 3600000) { // less than 1 hour
            return (diff / 60000) + "m";
        } else if (diff < 86400000) { // less than 1 day
            return (diff / 3600000) + "h";
        } else {
            return (diff / 86400000) + "d";
        }
    }
    
    private static final DiffUtil.ItemCallback<ChatMessage> DIFF_CALLBACK = new DiffUtil.ItemCallback<ChatMessage>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem.getMessageId().equals(newItem.getMessageId());
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem.getText().equals(newItem.getText()) &&
                   oldItem.getStatus().equals(newItem.getStatus()) &&
                   oldItem.getTimestamp().equals(newItem.getTimestamp());
        }
    };
}
