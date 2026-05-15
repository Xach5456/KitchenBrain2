package com.example.kitchenbrain;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Premium Chat Message Adapter with DiffUtil
 * High-performance adapter with smooth animations
 */
public class ChatMessageAdapterPremium extends ListAdapter<ChatMessage, ChatMessageAdapterPremium.MessageViewHolder> {

    private String currentUserId;

    public ChatMessageAdapterPremium() {
        super(DIFF_CALLBACK);
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message_premium, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = getItem(position);
        holder.bind(message, currentUserId);
    }

    /**
     * Update message status for a specific message ID (called from Fragment)
     */
    public void updateMessageStatus(String messageId, String newStatus) {
        for (int i = 0; i < getItemCount(); i++) {
            ChatMessage message = getItem(i);
            if (message.getMessageId().equals(messageId)) {
                message.setMessageStatus(newStatus);
                notifyItemChanged(i);
                break;
            }
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout layoutReceived;
        private TextView textMessageReceived;
        private TextView textTimestampReceived;
        private LinearLayout layoutSent;
        private TextView textMessageSent;
        private TextView textTimestampSent;
        private ImageView imageMessageStatus;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutReceived = itemView.findViewById(R.id.layoutReceived);
            textMessageReceived = itemView.findViewById(R.id.textMessageReceived);
            textTimestampReceived = itemView.findViewById(R.id.textTimestampReceived);
            layoutSent = itemView.findViewById(R.id.layoutSent);
            textMessageSent = itemView.findViewById(R.id.textMessageSent);
            textTimestampSent = itemView.findViewById(R.id.textTimestampSent);
            imageMessageStatus = itemView.findViewById(R.id.imageMessageStatus);
        }

        public void bind(ChatMessage message, String currentUserId) {
            boolean isMe = message.getSenderId().equals(currentUserId);

            if (isMe) {
                // Show sent message
                layoutReceived.setVisibility(View.GONE);
                layoutSent.setVisibility(View.VISIBLE);

                textMessageSent.setText(message.getText());
                textTimestampSent.setText(formatTimestamp(message.getTimestamp()));

                // Update message status icon
                updateStatusIcon(message.getMessageStatus());

            } else {
                // Show received message
                layoutSent.setVisibility(View.GONE);
                layoutReceived.setVisibility(View.VISIBLE);

                textMessageReceived.setText(message.getText());
                textTimestampReceived.setText(formatTimestamp(message.getTimestamp()));
            }
        }

        private void updateStatusIcon(String status) {
            if (imageMessageStatus == null) return;

            // Status lifecycle: sending → sent → delivered → read
            switch (status != null ? status : "sending") {
                case "read":
                    // Read - double check mark in green
                    imageMessageStatus.setImageResource(R.drawable.ic_check);
                    imageMessageStatus.setColorFilter(0xFF2196F3); // Blue for read
                    imageMessageStatus.setContentDescription("Read");
                    break;
                    
                case "delivered":
                    // Delivered - single check mark solid white
                    imageMessageStatus.setImageResource(R.drawable.ic_check);
                    imageMessageStatus.setColorFilter(0xFFFFFFFF); // Solid white
                    imageMessageStatus.setContentDescription("Delivered");
                    break;
                    
                case "sent":
                    // Sent - single check mark gray
                    imageMessageStatus.setImageResource(R.drawable.ic_check);
                    imageMessageStatus.setColorFilter(0xB3FFFFFF); // White with opacity
                    imageMessageStatus.setContentDescription("Sent");
                    break;
                    
                case "failed":
                    // Failed - exclamation mark or red X
                    imageMessageStatus.setImageResource(R.drawable.ic_error);
                    imageMessageStatus.setColorFilter(0xFFFF5252); // Red for error
                    imageMessageStatus.setContentDescription("Failed");
                    break;
                    
                case "sending":
                default:
                    // Sending - spinner or clock icon
                    imageMessageStatus.setImageResource(R.drawable.ic_access_time);
                    imageMessageStatus.setColorFilter(0xB3FFFFFF); // White with opacity
                    imageMessageStatus.setContentDescription("Sending...");
                    break;
            }
        }

        private String formatTimestamp(com.google.firebase.Timestamp timestamp) {
            if (timestamp == null) return "";

            long now = System.currentTimeMillis();
            long messageTime = timestamp.toDate().getTime();
            long diff = now - messageTime;

            // Less than a minute
            if (diff < 60000) {
                return "now";
            }
            // Less than an hour
            else if (diff < 3600000) {
                long minutes = diff / 60000;
                return minutes + "m";
            }
            // Today
            else if (diff < 86400000) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault());
                return sdf.format(timestamp.toDate());
            }
            // Yesterday
            else if (diff < 172800000) {
                return "Yesterday";
            }
            // Older
            else {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault());
                return sdf.format(timestamp.toDate());
            }
        }
    }

    // DiffUtil callback for efficient list updates
    private static final DiffUtil.ItemCallback<ChatMessage> DIFF_CALLBACK = new DiffUtil.ItemCallback<ChatMessage>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem.getMessageId().equals(newItem.getMessageId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem.getText().equals(newItem.getText()) &&
                   oldItem.getMessageStatus().equals(newItem.getMessageStatus()) &&
                   oldItem.getTimestamp().equals(newItem.getTimestamp());
        }

        @Override
        public Object getChangePayload(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return newItem;
        }
    };
}
