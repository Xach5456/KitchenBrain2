package com.example.kitchenbrain;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private List<ChatMessage> messages;
    private String currentUserId;
    private SimpleDateFormat dateFormat;

    public ChatMessageAdapter(List<ChatMessage> messages, String currentUserId) {
        this.messages = messages != null ? messages : new ArrayList<>();
        this.currentUserId = currentUserId;
        this.dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 1) { // Outgoing message (current user)
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_outgoing, parent, false);
        } else { // Incoming message (other user)
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_incoming, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        if (messages == null || position < 0 || position >= messages.size()) {
            return;
        }
        
        ChatMessage message = messages.get(position);
        
        if (message != null) {
            holder.textViewMessage.setText(message.getText() != null ? message.getText() : "");
            
            if (message.getTimestamp() != null) {
                try {
                    Date timestampAsDate = message.getTimestamp().toDate();
                    holder.textViewTimestamp.setText(dateFormat.format(timestampAsDate));
                } catch (Exception e) {
                    holder.textViewTimestamp.setText("Just now");
                }
            } else {
                holder.textViewTimestamp.setText("Just now");
            }
            
            // Set message status if it's outgoing (only show status for outgoing messages)
            if (holder.textViewStatus != null) { // Check if the status view exists (outgoing messages have it, incoming may not)
                if (message.getSenderId() != null && currentUserId != null && message.getSenderId().equals(currentUserId)) {
                    holder.textViewStatus.setVisibility(View.VISIBLE);
                    holder.textViewStatus.setText(getStatusText(message.getMessageStatus()));
                } else {
                    // For incoming messages, hide the status view
                    holder.textViewStatus.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (messages == null || position < 0 || position >= messages.size()) {
            return 0; // Default to incoming message
        }
        
        ChatMessage message = messages.get(position);
        if (message != null && message.getSenderId() != null && currentUserId != null && message.getSenderId().equals(currentUserId)) {
            return 1; // Outgoing
        }
        return 0; // Incoming
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }
    
    public void updateMessageStatus(String messageId, String newStatus) {
        if (messageId == null || messages == null || newStatus == null) {
            return;
        }
        
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            if (message != null && message.getMessageId() != null && messageId.equals(message.getMessageId())) {
                message.setMessageStatus(newStatus);
                notifyItemChanged(i);
                break;
            }
        }
    }

    private String getStatusText(String status) {
        switch (status) {
            case "sent":
                return "✓";
            case "delivered":
                return "✓✓";
            case "read":
                return "✓✓"; // Could be a different icon in a real app
            default:
                return "";
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage;
        TextView textViewTimestamp;
        TextView textViewStatus;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
            textViewStatus = itemView.findViewById(R.id.textViewStatus); // This may be null for incoming messages
        }
    }
}