package com.example.kitchenbrain.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kitchenbrain.R;
import com.example.kitchenbrain.databinding.ItemChatMessageBinding;
import com.example.kitchenbrain.model.ChatMessage;
import com.example.kitchenbrain.model.MessageStatus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatMessageAdapter extends ListAdapter<ChatMessage, ChatMessageAdapter.MessageViewHolder> {

    private String currentUserId;
    private OnMessageClickListener listener;

    public ChatMessageAdapter(String currentUserId) {
        super(DIFF_CALLBACK);
        this.currentUserId = currentUserId;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
        notifyDataSetChanged();
    }

    public void setOnMessageClickListener(OnMessageClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemChatMessageBinding binding = ItemChatMessageBinding.inflate(inflater, parent, false);
        return new MessageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = getItem(position);
        holder.bind(message, currentUserId);
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemChatMessageBinding binding;

        public MessageViewHolder(@NonNull ItemChatMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ChatMessage message, String currentUserId) {
            boolean isMe = message.getSenderId().equals(currentUserId);

            if (isMe) {
                binding.layoutReceived.setVisibility(View.GONE);
                binding.layoutSent.setVisibility(View.VISIBLE);
                binding.textMessageSent.setText(message.getText());
                binding.textTimestampSent.setText(formatTimestamp(message.getTimestamp()));
                updateStatusIcon(message.getStatus().name());

                binding.getRoot().setOnLongClickListener(v -> {
                    if (listener != null) {
                        listener.onMessageLongClick(message, getAdapterPosition());
                        return true;
                    }
                    return false;
                });

                binding.getRoot().setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onMessageClick(message, getAdapterPosition());
                    }
                });

            } else {
                binding.layoutSent.setVisibility(View.GONE);
                binding.layoutReceived.setVisibility(View.VISIBLE);
                binding.textMessageReceived.setText(message.getText());
                binding.textTimestampReceived.setText(formatTimestamp(message.getTimestamp()));

                binding.getRoot().setOnLongClickListener(v -> {
                    if (listener != null) {
                        listener.onMessageLongClick(message, getAdapterPosition());
                        return true;
                    }
                    return false;
                });

                binding.getRoot().setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onMessageClick(message, getAdapterPosition());
                    }
                });
            }
        }

        private void updateStatusIcon(String status) {
            if (binding.imageMessageStatus == null) return;

            switch (status != null ? status : "sent") {
                case "read":
                    binding.imageMessageStatus.setImageResource(R.drawable.ic_check);
                    binding.imageMessageStatus.setColorFilter(0xFF2196F3);
                    break;
                case "delivered":
                    binding.imageMessageStatus.setImageResource(R.drawable.ic_check);
                    binding.imageMessageStatus.setColorFilter(0xFFFFFFFF);
                    break;
                case "sent":
                default:
                    binding.imageMessageStatus.setImageResource(R.drawable.ic_check);
                    binding.imageMessageStatus.setColorFilter(0xB3FFFFFF);
                    break;
                case "failed":
                    binding.imageMessageStatus.setImageResource(R.drawable.ic_error);
                    binding.imageMessageStatus.setColorFilter(0xFFFF5252);
                    break;
                case "sending":
                    binding.imageMessageStatus.setImageResource(R.drawable.ic_access_time);
                    binding.imageMessageStatus.setColorFilter(0xB3FFFFFF);
                    break;
            }
        }

        private String formatTimestamp(com.google.firebase.Timestamp timestamp) {
            if (timestamp == null) return "";

            long now = System.currentTimeMillis();
            long messageTime = timestamp.toDate().getTime();
            long diff = now - messageTime;

            if (diff < 60000) {
                return "now";
            } else if (diff < 3600000) {
                long minutes = diff / 60000;
                return minutes + "m";
            } else if (diff < 86400000) {
                SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
                return sdf.format(timestamp.toDate());
            } else if (diff < 172800000) {
                return "Yesterday";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
                return sdf.format(timestamp.toDate());
            }
        }
    }

    public interface OnMessageClickListener {
        void onMessageClick(ChatMessage message, int position);
        void onMessageLongClick(ChatMessage message, int position);
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

        @Override
        public Object getChangePayload(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return newItem;
        }
    };
}
