package com.example.kitchenbrain.adapter;

import android.os.Handler;
import android.os.Looper;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 🔥 PRO CHAT ENGINE ADAPTER - Production Level
 * Features: Optimistic UI + Deduplication + Message State Updates
 */
public class ProChatMessageEngineAdapter extends ListAdapter<ChatMessage, RecyclerView.ViewHolder> {
    
    // 🔥 TELEGRAM-LEVEL STATE BUFFER
    private final List<ChatMessage> messages = new ArrayList<>();
    private final List<ChatMessage> pendingMessages = new ArrayList<>();
    private volatile boolean isSubmitting = false;
    
    // 🔥 CRITICAL FIX: Single submitList gate with debounce
    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable pendingSubmit;
    
    private static final int TYPE_MY_MESSAGE = 1;
    private static final int TYPE_OTHER_MESSAGE = 2;
    
    private String currentUserId;
    private OnMessageLongClickListener listener;
    private OnMessageRetryClickListener retryListener;
    
    public interface OnMessageLongClickListener {
        void onMessageLongClick(ChatMessage message, int position);
    }
    
    public interface OnMessageRetryClickListener {
        void onRetryMessage(ChatMessage message);
    }
    
    public ProChatMessageEngineAdapter(String currentUserId, 
                                    OnMessageLongClickListener listener,
                                    OnMessageRetryClickListener retryListener) {
        super(DIFF_CALLBACK);
        this.currentUserId = currentUserId;
        this.listener = listener;
        this.retryListener = retryListener;
    }
    
    @Override
    public int getItemCount() {
        int count = messages.size();
        Log.d("ADAPTER_DEBUG", "getItemCount() called, returning: " + count);
        Log.d("ADAPTER_DEBUG", "messages list contains: " + messages.size() + " items");
        return count;
    }
    
    @Override
    public int getItemViewType(int position) {
        // 🔥 TELEGRAM-SAFE: Bounds protection to prevent crash
        List<ChatMessage> currentList = getCurrentList();
        if (position >= currentList.size() || position < 0) {
            Log.w("ADAPTER_DEBUG", "getItemViewType: position " + position + " out of bounds, list size: " + currentList.size());
            return TYPE_OTHER_MESSAGE; // Safe fallback
        }
        
        ChatMessage message = getItem(position);
        if (message != null && message.getSenderId().equals(currentUserId)) {
            return TYPE_MY_MESSAGE;
        } else {
            return TYPE_OTHER_MESSAGE;
        }
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
        // 🔥 TELEGRAM-SAFE: Use binding adapter position to prevent stale position crashes
        int safePosition = holder.getBindingAdapterPosition();
        
        if (safePosition == RecyclerView.NO_POSITION) {
            Log.w("ADAPTER_DEBUG", "onBindViewHolder: NO_POSITION, skipping bind");
            return;
        }
        
        if (safePosition >= getItemCount()) {
            Log.w("ADAPTER_DEBUG", "onBindViewHolder: position " + safePosition + " out of bounds, skipping bind");
            return;
        }
        
        ChatMessage message = getItem(safePosition);
        
        // 🔥 CRITICAL DEBUG: Track message status for SENDING bug
        Log.d("ADAPTER_DEBUG", "bind id=" + (message != null ? message.getMessageId() : "null") + 
              " status=" + (message != null ? message.getStatus() : "null"));
        
        // 🔥 DEBUG: Check binding
        Log.d("ADAPTER_DEBUG", "onBindViewHolder called for safe position " + safePosition + " (original: " + position + ")");
        Log.d("ADAPTER_DEBUG", "Message: " + (message != null ? message.getText() : "null"));
        
        if (holder instanceof MyMessageViewHolder) {
            ((MyMessageViewHolder) holder).bind(message);
            Log.d("ADAPTER_DEBUG", "Bound MyMessageViewHolder");
        } else if (holder instanceof OtherMessageViewHolder) {
            ((OtherMessageViewHolder) holder).bind(message);
            Log.d("ADAPTER_DEBUG", "Bound OtherMessageViewHolder");
        } else {
            Log.e("ADAPTER_DEBUG", "Unknown ViewHolder type: " + holder.getClass().getSimpleName());
        }
    }
    
    // 🔥 TELEGRAM-SAFE OPTIMISTIC UI - Add message immediately
    public void addMessage(ChatMessage message) {
        // 🔥 DEDUPLICATION CHECK
        if (messageExists(message.getMessageId())) {
            return; // Ignore duplicate
        }
        
        // Create new list for submitList
        List<ChatMessage> newMessages = new ArrayList<>(messages);
        newMessages.add(message);
        Collections.sort(newMessages, new Comparator<ChatMessage>() {
            @Override
            public int compare(ChatMessage a, ChatMessage b) {
                return Long.compare(a.getTimestamp().getSeconds(), b.getTimestamp().getSeconds());
            }
        });
        
        // 🔥 CRITICAL FIX: Use safe submitList with debounce
        safeSubmitList(newMessages);
        messages.clear();
        messages.addAll(newMessages);
    }
    
    // 🔥 TELEGRAM-SAFE MESSAGE STATE UPDATE
    public void updateMessage(ChatMessage updatedMessage) {
        List<ChatMessage> newMessages = new ArrayList<>(messages);
        for (int i = 0; i < newMessages.size(); i++) {
            if (newMessages.get(i).getMessageId().equals(updatedMessage.getMessageId())) {
                newMessages.set(i, updatedMessage);
                break;
            }
        }
        
        // 🔥 CRITICAL FIX: Use safe submitList with debounce
        safeSubmitList(newMessages);
        messages.clear();
        messages.addAll(newMessages);
    }
    
    // 🔥 TELEGRAM-SAFE REMOVE MESSAGE
    public void removeMessage(String messageId) {
        List<ChatMessage> newMessages = new ArrayList<>(messages);
        for (int i = 0; i < newMessages.size(); i++) {
            if (newMessages.get(i).getMessageId().equals(messageId)) {
                newMessages.remove(i);
                break;
            }
        }
        
        // 🔥 CRITICAL FIX: Use safe submitList with debounce
        safeSubmitList(newMessages);
        messages.clear();
        messages.addAll(newMessages);
    }
    
    // 🔥 TELEGRAM-LEVEL SERIALIZED UPDATE - Single Source of Truth
    public void updateMessages(List<ChatMessage> newMessages) {
        // 🔥 DEBUG: Check data flow
        Log.d("ADAPTER_DEBUG", "updateMessages called with " + newMessages.size() + " messages");
        Log.d("ADAPTER_DEBUG", "isSubmitting = " + isSubmitting);
        
        // 🔥 TELEGRAM-LEVEL: Serialize updates to prevent race conditions
        synchronized (pendingMessages) {
            pendingMessages.clear();
            
            // 🔥 CRITICAL DEBUG: Check each message before filtering
            Log.d("ADAPTER_DEBUG", "=== MESSAGE ANALYSIS ===");
            for (int i = 0; i < newMessages.size(); i++) {
                ChatMessage msg = newMessages.get(i);
                Log.d("ADAPTER_DEBUG", "Message " + i + ":");
                Log.d("ADAPTER_DEBUG", "  - messageId: " + (msg != null ? msg.getMessageId() : "null"));
                Log.d("ADAPTER_DEBUG", "  - text: " + (msg != null ? msg.getText() : "null"));
                Log.d("ADAPTER_DEBUG", "  - senderId: " + (msg != null ? msg.getSenderId() : "null"));
                Log.d("ADAPTER_DEBUG", "  - is null: " + (msg == null));
            }
            
            // 🔥 TELEGRAM-LEVEL: Filter and prepare pending list
            List<ChatMessage> filteredMessages = new ArrayList<>();
            for (ChatMessage msg : newMessages) {
                if (msg != null && msg.getMessageId() != null && !messageExistsInList(msg.getMessageId(), filteredMessages)) {
                    filteredMessages.add(msg);
                    Log.d("ADAPTER_DEBUG", "✅ Added message: " + msg.getMessageId() + " text: " + msg.getText());
                } else {
                    Log.w("ADAPTER_DEBUG", "❌ Skipped message: null=" + (msg == null) + " messageId=" + (msg != null ? msg.getMessageId() : "null"));
                }
            }
            
            // Sort the filtered list
            Collections.sort(filteredMessages, new Comparator<ChatMessage>() {
                @Override
                public int compare(ChatMessage a, ChatMessage b) {
                    return Long.compare(a.getTimestamp().getSeconds(), b.getTimestamp().getSeconds());
                }
            });
            
            pendingMessages.addAll(filteredMessages);
            Log.d("ADAPTER_DEBUG", "Pending messages prepared: " + pendingMessages.size());
        }
        
        // 🔥 TELEGRAM-LEVEL: Atomic submit with serialization
        if (!isSubmitting) {
            submitPendingMessages();
        }
    }
    
    /**
     * 🔥 CRITICAL FIX: Safe submitList with debounce (prevents race conditions)
     */
    private void safeSubmitList(List<ChatMessage> list) {
        if (pendingSubmit != null) {
            mainHandler.removeCallbacks(pendingSubmit);
        }

        pendingSubmit = () -> {
            submitList(new ArrayList<>(list));
            pendingSubmit = null;
        };

        mainHandler.post(pendingSubmit);
    }
    
    /**
     * 🔥 TELEGRAM-LEVEL: Atomic pending messages submission
     */
    private void submitPendingMessages() {
        if (isSubmitting) return;
        
        isSubmitting = true;
        
        // 🔥 TELEGRAM-LEVEL: Post to UI thread to ensure proper sequence
        if (Looper.myLooper() == Looper.getMainLooper()) {
            performSubmit();
        } else {
            new Handler(Looper.getMainLooper()).post(this::performSubmit);
        }
    }
    
    // 🔥 TELEGRAM-LEVEL: Perform actual submit on main thread
    private void performSubmit() {
        try {
            List<ChatMessage> messagesToSubmit;
            synchronized (pendingMessages) {
                messagesToSubmit = new ArrayList<>(pendingMessages);
                pendingMessages.clear();
            }
            
            Log.d("ADAPTER_DEBUG", "Submitting " + messagesToSubmit.size() + " messages to ListAdapter");
            
            // 🔥 CRITICAL FIX: Use safe submitList with debounce
            safeSubmitList(messagesToSubmit);
            
            // Update local messages list for compatibility
            messages.clear();
            messages.addAll(messagesToSubmit);
            
            Log.d("ADAPTER_DEBUG", "Final messages list size = " + messages.size());
            Log.d("ADAPTER_DEBUG", "Adapter item count = " + getItemCount());
            
        } catch (Exception e) {
            Log.e("ADAPTER_DEBUG", "Error during submit", e);
        } finally {
            isSubmitting = false;
            
            // 🔥 TELEGRAM-LEVEL: Check if more pending messages arrived during submission
            synchronized (pendingMessages) {
                if (!pendingMessages.isEmpty()) {
                    submitPendingMessages();
                }
            }
        }
    }
    
    // 🔥 DEDUPLICATION CHECK
    private boolean messageExists(String messageId) {
        for (ChatMessage m : messages) {
            if (m.getMessageId().equals(messageId)) {
                return true;
            }
        }
        return false;
    }
    
    // 🔥 TELEGRAM-SAFE: Deduplication for any list
    private boolean messageExistsInList(String messageId, List<ChatMessage> list) {
        for (ChatMessage m : list) {
            if (m.getMessageId().equals(messageId)) {
                return true;
            }
        }
        return false;
    }
    
    // 🔥 MESSAGE ORDER STABILITY
    private void sortMessages() {
        Collections.sort(messages, new Comparator<ChatMessage>() {
            @Override
            public int compare(ChatMessage a, ChatMessage b) {
                return Long.compare(a.getTimestamp().getSeconds(), b.getTimestamp().getSeconds());
            }
        });
    }
    
    // 🔥 MY MESSAGE VIEW HOLDER (right, blue bubble)
    private class MyMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageText;
        private TextView timestampText;
        private TextView statusText;
        private LinearLayout retryLayout;
        
        public MyMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timestampText = itemView.findViewById(R.id.timestampText);
            statusText = itemView.findViewById(R.id.statusText);
            
            // Add retry button for failed messages
            retryLayout = new LinearLayout(itemView.getContext());
            retryLayout.setOrientation(LinearLayout.HORIZONTAL);
            retryLayout.setPadding(8, 4, 8, 4);
        }
        
        public void bind(ChatMessage message) {
            messageText.setText(message.getText());
            timestampText.setText(formatTimestamp(message.getTimestamp()));
            
            // 🔥 MESSAGE STATE DISPLAY
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
                    
                    // 🔥 SHOW RETRY BUTTON
                    if (retryListener != null) {
                        statusText.setOnClickListener(v -> retryListener.onRetryMessage(message));
                        statusText.setClickable(true);
                        statusText.setBackground(statusText.getContext().getDrawable(R.drawable.bg_retry_button));
                    }
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
    
    // 🔥 OTHER MESSAGE VIEW HOLDER (left, gray bubble)
    private class OtherMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageText;
        private TextView timestampText;
        private TextView senderNameText;
        
        public OtherMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timestampText = itemView.findViewById(R.id.timestampText);
            senderNameText = itemView.findViewById(R.id.senderNameText);
        }
        
        public void bind(ChatMessage message) {
            messageText.setText(message.getText());
            timestampText.setText(formatTimestamp(message.getTimestamp()));
            
            // 🔥 TELEGRAM-STYLE: Show sender name
            if (senderNameText != null) {
                String senderName = message.getSenderName();
                if (senderName != null && !senderName.isEmpty()) {
                    senderNameText.setText(senderName);
                } else {
                    // Fallback to user ID prefix
                    String senderId = message.getSenderId();
                    if (senderId != null && senderId.length() > 8) {
                        senderNameText.setText("User_" + senderId.substring(0, 8));
                    } else {
                        senderNameText.setText("Unknown");
                    }
                }
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
