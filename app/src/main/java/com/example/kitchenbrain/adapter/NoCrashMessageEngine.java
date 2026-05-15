package com.example.kitchenbrain.adapter;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.model.ChatMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 🔥 NO-CRASH MESSAGE ENGINE - Final Zero-Crash Architecture
 * Features: Full serialization queue + state dropping + deterministic UI pipeline
 */
public class NoCrashMessageEngine extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final String TAG = "NoCrashEngine";
    private static final int TYPE_MY_MESSAGE = 1;
    private static final int TYPE_OTHER_MESSAGE = 2;
    
    // 🔥 CRITICAL: Full serialization queue (eliminates all concurrent submitList)
    private final Queue<List<ChatMessage>> renderQueue = new LinkedList<>();
    private volatile boolean isRendering = false;
    private final Object queueLock = new Object();
    
    // 🔥 CRITICAL: Current state (no async snapshots)
    private final List<ChatMessage> currentList = new ArrayList<>();
    
    // 🔥 CRITICAL: Thread-safe listeners
    private final List<StateListener> listeners = new CopyOnWriteArrayList<>();
    
    // 🔥 CRITICAL: Main handler for serialization
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private String currentUserId;
    private OnMessageLongClickListener longClickListener;
    private OnMessageRetryClickListener retryClickListener;
    
    public interface StateListener {
        void onMessagesChanged(int count);
    }
    
    public interface OnMessageLongClickListener {
        void onMessageLongClick(ChatMessage message, int position);
    }
    
    public interface OnMessageRetryClickListener {
        void onRetryMessage(ChatMessage message);
    }
    
    public NoCrashMessageEngine(String currentUserId, 
                               OnMessageLongClickListener longClickListener,
                               OnMessageRetryClickListener retryClickListener) {
        this.currentUserId = currentUserId;
        this.longClickListener = longClickListener;
        this.retryClickListener = retryClickListener;
        
        Log.d(TAG, "🔥 NO-CRASH ENGINE INITIALIZED");
    }
    
    /**
     * 🔥 CRITICAL: Full serialization queue with state dropping (FINAL FIX)
     */
    public void render(List<ChatMessage> newList) {
        Log.d(TAG, "🔥 SERIALIZED RENDER START: " + newList.size() + " messages");
        
        synchronized (queueLock) {
            // 🔥 KEY FIX: Drop old states (prevents state flapping)
            renderQueue.clear();
            renderQueue.add(new ArrayList<>(newList));
        }
        
        processQueue();
    }
    
    /**
     * 🔥 CRITICAL: Process queue with full serialization
     */
    private void processQueue() {
        if (isRendering) {
            Log.d(TAG, "🔥 ALREADY RENDERING - skipping");
            return;
        }
        
        isRendering = true;
        
        mainHandler.post(() -> {
            List<ChatMessage> latest = null;
            
            synchronized (queueLock) {
                latest = renderQueue.poll();
            }
            
            if (latest != null) {
                performRender(latest);
            }
            
            isRendering = false;
            
            // 🔥 CRITICAL: Check if more states arrived during rendering
            synchronized (queueLock) {
                if (!renderQueue.isEmpty()) {
                    Log.d(TAG, "🔥 MORE STATES IN QUEUE - processing next");
                    processQueue();
                }
            }
        });
    }
    
    /**
     * 🔥 CRITICAL: Perform actual render (synchronous, no async submitList)
     */
    private void performRender(List<ChatMessage> newList) {
        Log.d(TAG, "🔥 PERFORMING RENDER: " + newList.size() + " messages");
        
        List<ChatMessage> oldList = new ArrayList<>(currentList);
        List<ChatMessage> sortedNewList = sortMessages(new ArrayList<>(newList));
        
        // 🔥 CRITICAL: Manual DiffUtil (synchronous)
        ChatDiffCallback diffCallback = new ChatDiffCallback(oldList, sortedNewList);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        
        // 🔥 CRITICAL: Atomic list replacement
        currentList.clear();
        currentList.addAll(sortedNewList);
        
        // 🔥 CRITICAL: Synchronous dispatch (no async submitList)
        diffResult.dispatchUpdatesTo(this);
        
        Log.d(TAG, "✅ RENDER COMPLETE: " + currentList.size() + " messages");
        
        // 🔥 CRITICAL: Notify listeners
        for (StateListener listener : listeners) {
            listener.onMessagesChanged(currentList.size());
        }
    }
    
    /**
     * 🔥 CRITICAL: Add single message (through serialization queue)
     */
    public void addMessage(ChatMessage message) {
        Log.d(TAG, "🔥 ADD MESSAGE THROUGH QUEUE: " + message.getMessageId());
        
        synchronized (queueLock) {
            List<ChatMessage> newList = new ArrayList<>(currentList);
            newList.add(message);
            renderQueue.clear();
            renderQueue.add(newList);
        }
        
        processQueue();
    }
    
    /**
     * 🔥 CRITICAL: Update message status (through serialization queue)
     */
    public void updateMessageStatus(String messageId, ChatMessage.MessageStatus newStatus) {
        Log.d(TAG, "🔥 UPDATE STATUS THROUGH QUEUE: " + messageId + " → " + newStatus);
        
        synchronized (queueLock) {
            List<ChatMessage> newList = new ArrayList<>(currentList);
            boolean messageFound = false;
            
            for (int i = 0; i < newList.size(); i++) {
                ChatMessage msg = newList.get(i);
                if (msg.getMessageId().equals(messageId)) {
                    // 🔥 CRITICAL: Create new message with updated status
                    ChatMessage updatedMessage = createMessageWithNewStatus(msg, newStatus);
                    newList.set(i, updatedMessage);
                    messageFound = true;
                    break;
                }
            }
            
            if (messageFound) {
                renderQueue.clear();
                renderQueue.add(newList);
            }
        }
        
        processQueue();
    }
    
    /**
     * 🔥 CRITICAL: Create message with new status (immutable copy)
     */
    private ChatMessage createMessageWithNewStatus(ChatMessage original, ChatMessage.MessageStatus newStatus) {
        ChatMessage newMessage = new ChatMessage();
        newMessage.setMessageId(original.getMessageId());
        newMessage.setSenderId(original.getSenderId());
        newMessage.setReceiverId(original.getReceiverId());
        newMessage.setText(original.getText());
        newMessage.setTimestamp(original.getTimestamp());
        newMessage.setStatus(newStatus); // 🔥 NEW STATUS
        newMessage.setMessageStatus(original.getMessageStatus());
        newMessage.setSenderName(original.getSenderName());
        return newMessage;
    }
    
    /**
     * 🔥 CRITICAL: Sort messages by timestamp
     */
    private List<ChatMessage> sortMessages(List<ChatMessage> messages) {
        Collections.sort(messages, new Comparator<ChatMessage>() {
            @Override
            public int compare(ChatMessage a, ChatMessage b) {
                if (a.getTimestamp() == null) return 1;
                if (b.getTimestamp() == null) return -1;
                return Long.compare(a.getTimestamp().getSeconds(), b.getTimestamp().getSeconds());
            }
        });
        return messages;
    }
    
    @Override
    public int getItemViewType(int position) {
        // 🔥 CRITICAL: Bounds protection
        if (position < 0 || position >= currentList.size()) {
            Log.w(TAG, "❌ getItemViewType: position " + position + " out of bounds, size=" + currentList.size());
            return TYPE_OTHER_MESSAGE;
        }
        
        ChatMessage message = currentList.get(position);
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
        // 🔥 CRITICAL: Safe position binding (no stale position crashes)
        if (position < 0 || position >= currentList.size()) {
            Log.w(TAG, "❌ onBindViewHolder: position " + position + " out of bounds, size=" + currentList.size());
            return;
        }
        
        ChatMessage message = currentList.get(position);
        
        // 🔥 CRITICAL DEBUG: Track message status
        Log.d(TAG, "bind id=" + message.getMessageId() + " status=" + message.getStatus());
        
        if (holder instanceof MyMessageViewHolder) {
            ((MyMessageViewHolder) holder).bind(message);
        } else if (holder instanceof OtherMessageViewHolder) {
            ((OtherMessageViewHolder) holder).bind(message);
        }
    }
    
    @Override
    public int getItemCount() {
        return currentList.size();
    }
    
    /**
     * 🔥 CRITICAL: Add state listener
     */
    public void addStateListener(StateListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    /**
     * 🔥 CRITICAL: Remove state listener
     */
    public void removeStateListener(StateListener listener) {
        listeners.remove(listener);
    }
    
    // 🔥 VIEW HOLDERS
    private static class MyMessageViewHolder extends RecyclerView.ViewHolder {
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
            statusText.setText(message.getStatus().toString());
        }
        
        private String formatTimestamp(com.google.firebase.Timestamp timestamp) {
            if (timestamp == null) return "";
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            return sdf.format(timestamp.toDate());
        }
    }
    
    private static class OtherMessageViewHolder extends RecyclerView.ViewHolder {
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
            senderNameText.setText(message.getSenderName());
        }
        
        private String formatTimestamp(com.google.firebase.Timestamp timestamp) {
            if (timestamp == null) return "";
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            return sdf.format(timestamp.toDate());
        }
    }
    
    /**
     * 🔥 CRITICAL: Manual DiffUtil callback (synchronous)
     */
    private static class ChatDiffCallback extends DiffUtil.Callback {
        private final List<ChatMessage> oldList;
        private final List<ChatMessage> newList;
        
        public ChatDiffCallback(List<ChatMessage> oldList, List<ChatMessage> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }
        
        @Override
        public int getOldListSize() {
            return oldList.size();
        }
        
        @Override
        public int getNewListSize() {
            return newList.size();
        }
        
        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            ChatMessage oldItem = oldList.get(oldItemPosition);
            ChatMessage newItem = newList.get(newItemPosition);
            return oldItem.getMessageId().equals(newItem.getMessageId());
        }
        
        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            ChatMessage oldItem = oldList.get(oldItemPosition);
            ChatMessage newItem = newList.get(newItemPosition);
            
            return oldItem.getText().equals(newItem.getText()) &&
                   oldItem.getStatus().equals(newItem.getStatus()) &&
                   oldItem.getSenderId().equals(newItem.getSenderId());
        }
    }
    
    /**
     * 🔥 CRITICAL: Get current state for debugging
     */
    public List<ChatMessage> getCurrentMessages() {
        return new ArrayList<>(currentList);
    }
    
    /**
     * 🔥 CRITICAL: Check if currently rendering
     */
    public boolean isRendering() {
        return isRendering;
    }
    
    /**
     * 🔥 CRITICAL: Get queue size for debugging
     */
    public int getQueueSize() {
        synchronized (queueLock) {
            return renderQueue.size();
        }
    }
}
