package com.example.kitchenbrain;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for AI Chat messages
 */
public class AIChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI = 2;

    private List<AIChatMessage> messageList;
    private String currentUserId;

    public AIChatMessageAdapter(List<AIChatMessage> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        AIChatMessage message = messageList.get(position);
        return message.getSenderType().equals("user") ? VIEW_TYPE_USER : VIEW_TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ai_chat_user, parent, false);
            return new UserMessageHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ai_chat_ai, parent, false);
            return new AIMessageHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AIChatMessage message = messageList.get(position);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String time = timeFormat.format(new Date(message.getTimestamp()));

        if (holder instanceof UserMessageHolder) {
            ((UserMessageHolder) holder).bind(message.getMessage(), time);
        } else if (holder instanceof AIMessageHolder) {
            if ("typing".equals(message.getSenderType())) {
                ((AIMessageHolder) holder).bindTyping();
            } else {
                ((AIMessageHolder) holder).bind(message.getMessage(), time);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class UserMessageHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        TextView textTime;

        UserMessageHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
            textTime = itemView.findViewById(R.id.textTime);
        }

        void bind(String message, String time) {
            textMessage.setText(message);
            textTime.setText(time);
            textTime.setVisibility(View.VISIBLE);
        }
    }

    static class AIMessageHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        TextView textTime;

        AIMessageHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
            textTime = itemView.findViewById(R.id.textTime);
        }

        void bind(String message, String time) {
            textMessage.setText(message);
            textMessage.setAlpha(1.0f);
            textTime.setText(time);
            textTime.setVisibility(View.VISIBLE);
        }
        
        void bindTyping() {
            textMessage.setText("AI is typing...");
            textMessage.setAlpha(0.6f);
            textTime.setVisibility(View.GONE);
        }
    }
}
