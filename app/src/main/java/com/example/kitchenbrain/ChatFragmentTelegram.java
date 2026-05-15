package com.example.kitchenbrain;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kitchenbrain.adapter.TelegramChatAdapter;
import com.example.kitchenbrain.coordinator.ChatRealtimeCoordinator;
import com.example.kitchenbrain.model.ChatMessage;
import com.example.kitchenbrain.repository.ChatRepository;
import com.example.kitchenbrain.state.ChatState;
import com.example.kitchenbrain.state.TelegramChatReducer;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.List;

/**
 * 🔥 TELEGRAM-STYLE CHAT FRAGMENT - Ideal Architecture
 * Features: Redux-style reducer + single data source + single submitList gate + zero crashes
 */
public class ChatFragmentTelegram extends Fragment {
    
    private static final String TAG = "TelegramChat";
    
    // Core Systems
    private ChatRepository chatRepository;
    private ChatRealtimeCoordinator coordinator;
    private TelegramChatReducer reducer;
    private TelegramChatAdapter adapter;
    
    // Firebase
    private FirebaseAuth auth;
    private String currentUserId;
    private String otherUserId;
    private String chatId;
    
    // UI Components
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    
    // Listeners
    private ListenerRegistration messagesListener;
    
    public ChatFragmentTelegram() {
        // Required empty public constructor
    }
    
    public static ChatFragmentTelegram newInstance(String otherUserId) {
        ChatFragmentTelegram fragment = new ChatFragmentTelegram();
        Bundle args = new Bundle();
        args.putString("otherUserId", otherUserId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "🚀 TELEGRAM-STYLE ChatFragment - IDEAL ARCHITECTURE");
        
        // Get arguments
        if (getArguments() != null) {
            otherUserId = getArguments().getString("otherUserId");
        }
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        }
        
        // Initialize repository
        chatRepository = new ChatRepository();
        
        // Generate chat ID
        chatId = generateChatId(currentUserId, otherUserId);
        
        Log.d(TAG, "Chat initialized: " + chatId);
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        
        // Initialize UI
        initUI(view);
        
        // Initialize reducer system
        initReducerSystem();
        
        // Start message listening
        startMessageListening();
        
        return view;
    }
    
    /**
     * 🔥 INITIALIZE UI - Telegram-style setup
     */
    private void initUI(View view) {
        recyclerViewMessages = view.findViewById(R.id.recyclerViewMessages);
        editTextMessage = view.findViewById(R.id.editTextMessage);
        buttonSend = view.findViewById(R.id.buttonSend);
        
        // 🔥 TELEGRAM-STYLE ADAPTER - Single submitList gate
        adapter = new TelegramChatAdapter(currentUserId,
            new TelegramChatAdapter.OnMessageLongClickListener() {
                @Override
                public void onMessageLongClick(ChatMessage message, int position) {
                    Log.d(TAG, "Long click on message: " + message.getMessageId());
                }
            },
            new TelegramChatAdapter.OnMessageRetryClickListener() {
                @Override
                public void onRetryMessage(ChatMessage message) {
                    Log.d(TAG, "Retry message: " + message.getMessageId());
                }
            });
        
        // 🔥 CRITICAL: Set RecyclerView with adapter
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewMessages.setAdapter(adapter);
        
        // Send button setup
        buttonSend.setOnClickListener(v -> sendMessage());
        
        // Text watcher for send button state
        editTextMessage.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.toString().trim().length() > 0;
                buttonSend.setEnabled(hasText);
                
                // Color change
                int color = androidx.core.content.ContextCompat.getColor(
                    requireContext(), 
                    hasText ? android.R.color.holo_blue_dark : android.R.color.darker_gray
                );
                buttonSend.setColorFilter(color);
            }
            
            @Override 
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override 
            public void afterTextChanged(android.text.Editable s) {}
        });
        
        Log.d(TAG, "✅ TELEGRAM-STYLE UI initialized");
    }
    
    /**
     * 🔥 INITIALIZE REDUCER SYSTEM - Single source of truth
     */
    private void initReducerSystem() {
        // 🔥 CRITICAL: Create Redux-style reducer
        reducer = new TelegramChatReducer();
        
        // 🔥 CRITICAL: Set single state listener (adapter only)
        reducer.setStateListener(new TelegramChatReducer.StateListener() {
            @Override
            public void onStateChanged(ChatState newState) {
                // 🔥 FINAL: Single submitList gate (only adapter updates)
                if (adapter != null) {
                    Log.d(TAG, "🔥 REDUCER → ADAPTER: " + newState.getMessages().size() + " messages");
                    adapter.submitList(newState.getMessages());
                } else {
                    Log.e(TAG, "❌ Adapter is null in reducer listener");
                }
            }
        });
        
        // 🔥 ChatRealtimeCoordinator - Routes to reducer only
        coordinator = new ChatRealtimeCoordinator();
        coordinator.setChatRepository(chatRepository);
        
        coordinator.addListener(new ChatRealtimeCoordinator.CoordinatorListener() {
            @Override
            public void onMessageAdded(ChatMessage message) {
                // 🔥 CRITICAL: Route through reducer (single source of truth)
                reducer.dispatch(TelegramChatReducer.Action.addMessage(message));
                Log.d(TAG, "🔥 MESSAGE ROUTED TO REDUCER: " + message.getMessageId());
            }
            
            @Override
            public void onMessageUpdated(ChatMessage message) {
                // 🔥 CRITICAL: Route through reducer (single source of truth)
                reducer.dispatch(TelegramChatReducer.Action.updateMessage(
                    message.getMessageId(), 
                    message.getStatus()
                ));
                Log.d(TAG, "🔥 MESSAGE STATUS ROUTED TO REDUCER: " + message.getMessageId());
            }
            
            @Override
            public void onMessageRemoved(String messageId) {
                reducer.dispatch(TelegramChatReducer.Action.removeMessage(messageId));
                Log.d(TAG, "🔥 MESSAGE REMOVAL ROUTED TO REDUCER: " + messageId);
            }
            
            @Override
            public void onStateChanged(String chatId, ChatRealtimeCoordinator.ChatState state) {
                Log.d(TAG, "Chat state changed: " + chatId + " -> " + state);
            }
            
            @Override
            public void onConflictResolved(String messageId, ChatMessage resolved) {
                Log.d(TAG, "Conflict resolved: " + messageId);
            }
        });
        
        Log.d(TAG, "✅ TELEGRAM-STYLE reducer system initialized");
    }
    
    /**
     * 🔥 START MESSAGE LISTENING - Firebase → Reducer (single path)
     */
    private void startMessageListening() {
        if (chatId == null) {
            Log.e(TAG, "STOP: chatId is null - cannot start listener");
            return;
        }
        
        Log.d(TAG, "🔥 STARTING FIREBASE LISTENER: " + chatId);
        
        // Listen to messages
        messagesListener = chatRepository.listenMessages(chatId, new ChatRepository.MessageListener() {
            @Override
            public void onMessages(List<ChatMessage> messages) {
                Log.d(TAG, "🔥 FIREBASE MESSAGES RECEIVED: " + messages.size());
                
                // 🔥 CRITICAL: Route through reducer (single source of truth)
                reducer.dispatch(TelegramChatReducer.Action.loadMessages(messages));
                Log.d(TAG, "🔥 FIREBASE → REDUCER: " + messages.size() + " messages");
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "❌ Firebase listener error", error);
            }
        });
        
        Log.d(TAG, "✅ TELEGRAM-STYLE message listening started");
    }
    
    /**
     * 🚀 SEND MESSAGE - Telegram-style pipeline
     */
    private void sendMessage() {
        String text = editTextMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        
        Log.d(TAG, "🔥 TELEGRAM-STYLE SEND START: " + text.substring(0, Math.min(20, text.length())) + "...");
        
        // Create optimistic message
        ChatMessage optimisticMessage = new ChatMessage();
        optimisticMessage.setMessageId(java.util.UUID.randomUUID().toString());
        optimisticMessage.setSenderId(currentUserId);
        optimisticMessage.setReceiverId(otherUserId);
        optimisticMessage.setText(text);
        optimisticMessage.setTimestamp(new com.google.firebase.Timestamp(System.currentTimeMillis() / 1000, 0));
        optimisticMessage.setStatus(ChatMessage.MessageStatus.SENDING);
        
        Log.d(TAG, "🔥 OPTIMISTIC MESSAGE CREATED: " + optimisticMessage.getMessageId());
        
        // 🔥 CRITICAL: Add optimistic message through reducer (single source of truth)
        reducer.dispatch(TelegramChatReducer.Action.addMessage(optimisticMessage));
        
        // 🔥 Route through coordinator for Firebase sync
        if (coordinator != null) {
            Log.d(TAG, "🔥 ROUTING THROUGH COORDINATOR FOR FIREBASE SYNC");
            coordinator.processOptimisticMessage(optimisticMessage, chatId);
        } else {
            Log.e(TAG, "❌ COORDINATOR IS NULL");
        }
        
        // Clear input
        editTextMessage.setText("");
        
        Log.d(TAG, "✅ TELEGRAM-STYLE SEND COMPLETE");
    }
    
    /**
     * 🔥 GENERATE CHAT ID - Simple deterministic approach
     */
    private String generateChatId(String uid1, String uid2) {
        if (uid1 == null || uid2 == null) return null;
        
        // Sort user IDs for consistent chat ID
        String[] users = {uid1, uid2};
        java.util.Arrays.sort(users);
        
        return users[0] + "_" + users[1];
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Clean up listeners
        if (messagesListener != null) {
            messagesListener.remove();
        }
        
        Log.d(TAG, "✅ TELEGRAM-STYLE ChatFragment destroyed");
    }
    
    /**
     * 🔥 DEBUG: Get current reducer state
     */
    public void debugReducerState() {
        if (reducer != null) {
            reducer.debugState();
        }
    }
    
    /**
     * 🔥 DEBUG: Get current adapter state
     */
    public void debugAdapterState() {
        if (adapter != null) {
            Log.d(TAG, "🔥 ADAPTER DEBUG:");
            Log.d(TAG, "  - Item count: " + adapter.getItemCount());
            Log.d(TAG, "  - Current list: " + adapter.getCurrentList().size());
        }
    }
}
