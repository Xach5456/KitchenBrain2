package com.example.kitchenbrain.adapter;

import android.util.Log;
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
import com.example.kitchenbrain.state.ChatState;
import java.util.Collections;
import java.util.List;

/**
 * 🔥 TELEGRAM-STYLE CHAT ADAPTER - Redux Integration
 * Features: Single submitList gate + crash protection + immutable state rendering
 */
public class TelegramChatAdapter extends ListAdapter<ChatMessage, RecyclerView.ViewHolder> {
    
    private static final int TYPE_MY_MESSAGE = 1;
    private static final int TYPE_OTHER_MESSAGE = 2;
    
    private final String currentUserId;
    private final OnMessageLongClickListener longClickListener;
    private final OnMessageRetryClickListener retryClickListener;
    private RecyclerView recyclerView;
    
    public interface OnMessageLongClickListener {
        void onMessageLongClick(ChatMessage message, int position);
    }
    
    public interface OnMessageRetryClickListener {
        void onRetryMessage(ChatMessage message);
    }
    
    public TelegramChatAdapter(String currentUserId, 
                              OnMessageLongClickListener longClickListener,
                              OnMessageRetryClickListener retryClickListener) {
        super(DIFF_CALLBACK);
        this.currentUserId = currentUserId;
        this.longClickListener = longClickListener;
        this.retryClickListener = retryClickListener;
    }
    
    /**
     * 🔥 TELEGRAM-SAFE: Set RecyclerView reference for safe posting
     */
    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }
    
    /**
     * 🔥 TELEGRAM-LEVEL: Render state snapshot (no submitList!)
     */
    public void renderState(ChatState state) {
        if (state == null) {
            Log.e("TELEGRAM_ADAPTER", "❌ renderState: state is null");
            return;
        }
        
        List<ChatMessage> messages = state.getMessages();
        Log.d("TELEGRAM_ADAPTER", "🔥 Rendering state with " + messages.size() + " messages");
        
        // 🔥 TELEGRAM-SAFE: Use recyclerView.post() to prevent layout crashes
        if (recyclerView != null) {
            recyclerView.post(() -> {
                // 🔥 TELEGRAM-LEVEL: Submit immutable snapshot
                submitList(Collections.unmodifiableList(messages));
            });
        } else {
            // Fallback if recyclerView not set
            submitList(Collections.unmodifiableList(messages));
        }
    }
    
    @Override
    public int getItemCount() {
        int count = super.getItemCount();
        Log.d("TELEGRAM_ADAPTER", "getItemCount: " + count);
        return count;
    }
    
    @Override
    public int getItemViewType(int position) {
        // 🔥 TELEGRAM-LEVEL: Bounds protection
        if (position < 0 || position >= getCurrentList().size()) {
            Log.w("TELEGRAM_ADAPTER", "getItemViewType: position " + position + " out of bounds");
            return TYPE_OTHER_MESSAGE;
        }
        
        ChatMessage message = getItem(position);
        if (message == null) {
            Log.w("TELEGRAM_ADAPTER", "getItemViewType: message is null at position " + position);
            return TYPE_OTHER_MESSAGE;
        }
        
        return message.getSenderId().equals(currentUserId) ? TYPE_MY_MESSAGE : TYPE_OTHER_MESSAGE;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        
        if (viewType == TYPE_MY_MESSAGE) {
            View view = inflater.inflate(R.layout.item_message_my, parent, false);
            return new MyMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_other, parent, false);
            return new OtherMessageViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // 🔥 CRITICAL: RecyclerView crash protection
        int safePos = holder.getBindingAdapterPosition();
        if (safePos == RecyclerView.NO_POSITION) {
            Log.w("ADAPTER_DEBUG", "❌ NO_POSITION - skipping bind");
            return;
        }
        
        // 🔥 CRITICAL: Bounds protection (prevents IndexOutOfBoundsException)
        if (safePos >= getItemCount()) {
            Log.w("ADAPTER_DEBUG", "❌ POSITION OUT OF BOUNDS: " + safePos + " >= " + getItemCount());
            return;
        }
        
        ChatMessage message = getItem(safePos);
        
        // 🔥 CRITICAL DEBUG: Track message status
        Log.d("ADAPTER_DEBUG", "bind id=" + message.getMessageId() + " status=" + message.getStatus() + " pos=" + safePos);
        
        if (holder instanceof MyMessageViewHolder) {
            ((MyMessageViewHolder) holder).bind(message);
        } else if (holder instanceof OtherMessageViewHolder) {
            ((OtherMessageViewHolder) holder).bind(message);
        }
    }
    
    /**
     * 🔥 TELEGRAM-LEVEL: My message ViewHolder
     */
    public static class MyMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;
        private final TextView timestampText;
        private final TextView statusText;
        
        public MyMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timestampText = itemView.findViewById(R.id.timestampText);
            statusText = itemView.findViewById(R.id.statusText);
        }
        
        public void bind(ChatMessage message) {
            if (message == null) return;
            
            messageText.setText(message.getText() != null ? message.getText() : "");
            timestampText.setText(formatTimestamp(message.getTimestamp()));
            
            // 🔥 TELEGRAM-LEVEL: Status display
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
                    statusText.setTextColor(0xFFF44336);
                    break;
            }
        }
        
        private String formatTimestamp(com.google.firebase.Timestamp timestamp) {
            if (timestamp == null) return "";
            
            long now = System.currentTimeMillis();
            long messageTime = timestamp.getSeconds() * 1000;
            long diff = now - messageTime;
            
            if (diff < 60000) return "now";
            if (diff < 3600000) return (diff / 60000) + "m";
            if (diff < 86400000) return (diff / 3600000) + "h";
            return (diff / 86400000) + "d";
        }
    }
    
    /**
     * 🔥 TELEGRAM-LEVEL: Other message ViewHolder
     */
    public static class OtherMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView senderNameText;
        private final TextView messageText;
        private final TextView timestampText;
        
        public OtherMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            senderNameText = itemView.findViewById(R.id.senderNameText);
            messageText = itemView.findViewById(R.id.messageText);
            timestampText = itemView.findViewById(R.id.timestampText);
        }
        
        public void bind(ChatMessage message) {
            if (message == null) return;
            
            // 🔥 TELEGRAM-LEVEL: Sender name with fallback
            String senderName = message.getSenderName();
            if (senderName != null && !senderName.isEmpty()) {
                senderNameText.setText(senderName);
            } else {
                senderNameText.setText("User_" + (message.getSenderId() != null ? 
                    message.getSenderId().substring(0, Math.min(8, message.getSenderId().length())) : "unknown"));
            }
            
            messageText.setText(message.getText() != null ? message.getText() : "");
            timestampText.setText(formatTimestamp(message.getTimestamp()));
        }
        
        private String formatTimestamp(com.google.firebase.Timestamp timestamp) {
            if (timestamp == null) return "";
            
            long now = System.currentTimeMillis();
            long messageTime = timestamp.getSeconds() * 1000;
            long diff = now - messageTime;
            
            if (diff < 60000) return "now";
            if (diff < 3600000) return (diff / 60000) + "m";
            if (diff < 86400000) return (diff / 3600000) + "h";
            return (diff / 86400000) + "d";
        }
    }
    
    /**
     * 🔥 TELEGRAM-LEVEL: DiffUtil callback
     */
    private static final DiffUtil.ItemCallback<ChatMessage> DIFF_CALLBACK = new DiffUtil.ItemCallback<ChatMessage>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem.getMessageId() != null && oldItem.getMessageId().equals(newItem.getMessageId());
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem.getText().equals(newItem.getText()) &&
                   oldItem.getStatus().equals(newItem.getStatus()) &&
                   java.util.Objects.equals(oldItem.getTimestamp(), newItem.getTimestamp());
        }
    };
}
