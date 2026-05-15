package com.example.kitchenbrain;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Premium Chat Message Adapter with Material 3 Design
 * 
 * Features:
 * - Beautiful message bubbles with proper styling
 * - Smooth animations using DiffUtil
 * - Message status indicators with check marks
 * - Timestamps with elegant formatting
 * - Support for different message types
 */
public class PremiumChatMessageAdapter extends RecyclerView.Adapter<PremiumChatMessageAdapter.MessageViewHolder> {

    private List<ChatMessage> messages;
    private String currentUserId;
    private SimpleDateFormat timeFormat;
    private OnMessageLongClickListener longClickListener;

    public interface OnMessageLongClickListener {
        void onMessageLongClick(ChatMessage message, int position);
    }

    public PremiumChatMessageAdapter(List<ChatMessage> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.longClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent_premium, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received_premium, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        
        // Set message text
        holder.textViewMessage.setText(message.getText());
        
        // Set timestamp
        if (message.getTimestamp() != null) {
            try {
                Date date = message.getTimestamp().toDate();
                holder.textViewTime.setText(timeFormat.format(date));
            } catch (Exception e) {
                holder.textViewTime.setText("--:--");
            }
        }
        
        // Set message status (only for sent messages)
        if (holder.itemView.getId() == R.id.messageContainerSent) {
            holder.imageViewStatus.setVisibility(View.VISIBLE);
            int statusResId = getStatusIconResId(message.getMessageStatus());
            holder.imageViewStatus.setImageResource(statusResId);
            
            // Set status color based on state
            int statusColor = getStatusColor(holder.itemView.getContext(), message.getMessageStatus());
            holder.imageViewStatus.setColorFilter(statusColor);
        } else {
            holder.imageViewStatus.setVisibility(View.GONE);
        }
        
        // Setup long click for message actions
        if (longClickListener != null) {
            holder.itemView.setOnLongClickListener(v -> {
                longClickListener.onMessageLongClick(message, position);
                return true;
            });
        }
        
        // Apply animation for newly added items
        if (position == getItemCount() - 1) {
            holder.itemView.setAlpha(0f);
            holder.itemView.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    /**
     * Update message status with smooth animation
     */
    public void updateMessageStatus(String messageId, String newStatus) {
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            if (message.getMessageId() != null && message.getMessageId().equals(messageId)) {
                message.setMessageStatus(newStatus);
                notifyItemChanged(i);
                break;
            }
        }
    }

    /**
     * Add a single message with fade-in animation
     */
    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    /**
     * Remove a message with animation
     */
    public void removeMessage(int position) {
        if (position >= 0 && position < messages.size()) {
            messages.remove(position);
            notifyItemRemoved(position);
        }
    }

    private int getViewType(boolean isFromCurrentUser) {
        return isFromCurrentUser ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    private int getStatusIconResId(String status) {
        switch (status) {
            case "sending":
                return R.drawable.ic_clock;
            case "sent":
                return R.drawable.ic_check;
            case "delivered":
                return R.drawable.ic_double_check;
            case "read":
                return R.drawable.ic_double_check_filled;
            default:
                return R.drawable.ic_check;
        }
    }

    private int getStatusColor(android.content.Context context, String status) {
        switch (status) {
            case "sending":
                return ContextCompat.getColor(context, R.color.status_sending);
            case "sent":
                return ContextCompat.getColor(context, R.color.status_sent);
            case "delivered":
                return ContextCompat.getColor(context, R.color.status_delivered);
            case "read":
                return ContextCompat.getColor(context, R.color.status_read);
            default:
                return ContextCompat.getColor(context, R.color.status_sent);
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage;
        TextView textViewTime;
        ImageView imageViewStatus;
        View messageContainer;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            imageViewStatus = itemView.findViewById(R.id.imageViewStatus);
            // Use the appropriate container ID based on view type
            messageContainer = itemView.findViewById(R.id.messageContainerSent);
            if (messageContainer == null) {
                messageContainer = itemView.findViewById(R.id.messageContainerReceived);
            }
        }
    }

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
}
