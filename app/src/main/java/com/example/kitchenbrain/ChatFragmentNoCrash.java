package com.example.kitchenbrain;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kitchenbrain.adapter.NoCrashMessageEngine;
import com.example.kitchenbrain.coordinator.ChatRealtimeCoordinator;
import com.example.kitchenbrain.model.ChatMessage;
import com.example.kitchenbrain.repository.ChatRepository;
import com.example.kitchenbrain.state.ChatStateManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.List;

/**
 * 🔥 NO-CRASH CHAT FRAGMENT - Final Zero-Crash Architecture
 * Features: Full serialization queue + state dropping + deterministic UI pipeline
 */
public class ChatFragmentNoCrash extends Fragment {
    
    private static final String TAG = "NoCrashChat";
    
    // Core Systems
    private ChatRepository chatRepository;
    private ChatRealtimeCoordinator coordinator;
    private ChatStateManager stateManager;
    private NoCrashMessageEngine noCrashEngine;
    
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
    
    public ChatFragmentNoCrash() {
        // Required empty public constructor
    }
    
    public static ChatFragmentNoCrash newInstance(String otherUserId) {
        ChatFragmentNoCrash fragment = new ChatFragmentNoCrash();
        Bundle args = new Bundle();
        args.putString("otherUserId", otherUserId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "🚀 NO-CRASH ChatFragment - FINAL ZERO-CRASH ARCHITECTURE");
        
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
        
        // Initialize engines
        initEngines();
        
        // Start message listening
        startMessageListening();
        
        return view;
    }
    
    /**
     * 🔥 INITIALIZE UI - No-crash setup
     */
    private void initUI(View view) {
        recyclerViewMessages = view.findViewById(R.id.recyclerViewMessages);
        editTextMessage = view.findViewById(R.id.editTextMessage);
        buttonSend = view.findViewById(R.id.buttonSend);
        
        // 🔥 NO-CRASH ENGINE - Full serialization queue
        noCrashEngine = new NoCrashMessageEngine(currentUserId,
            new NoCrashMessageEngine.OnMessageLongClickListener() {
                @Override
                public void onMessageLongClick(ChatMessage message, int position) {
                    Log.d(TAG, "Long click on message: " + message.getMessageId());
                }
            },
            new NoCrashMessageEngine.OnMessageRetryClickListener() {
                @Override
                public void onRetryMessage(ChatMessage message) {
                    Log.d(TAG, "Retry message: " + message.getMessageId());
                }
            });
        
        // 🔥 CRITICAL: Add state listener for debugging
        noCrashEngine.addStateListener(new NoCrashMessageEngine.StateListener() {
            @Override
            public void onMessagesChanged(int count) {
                Log.d(TAG, "🔥 NO-CRASH ENGINE COUNT: " + count + " messages");
                Log.d(TAG, "🔥 Queue size: " + noCrashEngine.getQueueSize() + " states pending");
                Log.d(TAG, "🔥 Is rendering: " + noCrashEngine.isRendering());
            }
        });
        
        // 🔥 TELEGRAM-LEVEL: Set RecyclerView with no-crash engine
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewMessages.setAdapter(noCrashEngine);
        
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
        
        Log.d(TAG, "✅ NO-CRASH UI initialized");
    }
    
    /**
     * 🔥 INITIALIZE ENGINES - Single source of truth + serialization
     */
    private void initEngines() {
        // 🔥 ChatRealtimeCoordinator - Master orchestrator
        coordinator = new ChatRealtimeCoordinator();
        coordinator.setChatRepository(chatRepository);
        
        // 🔥 CRITICAL: Initialize state manager (single source of truth)
        stateManager = ChatStateManager.getInstance();
        stateManager.addStateListener(new ChatStateManager.StateListener() {
            @Override
            public void onStateChanged(List<ChatMessage> messages) {
                // 🔥 FINAL: Use no-crash engine (full serialization queue)
                if (noCrashEngine != null) {
                    Log.d(TAG, "🔥 STATE MANAGER → NO-CRASH ENGINE: " + messages.size() + " messages");
                    noCrashEngine.render(messages);
                } else {
                    Log.e(TAG, "❌ No-crash engine is null in state listener");
                }
            }
        });
        
        coordinator.addListener(new ChatRealtimeCoordinator.CoordinatorListener() {
            @Override
            public void onMessageAdded(ChatMessage message) {
                // 🔥 CRITICAL: Route through state manager (single source of truth)
                if (stateManager != null) {
                    stateManager.addMessage(message);
                    Log.d(TAG, "🔥 MESSAGE ROUTED TO STATE MANAGER: " + message.getMessageId());
                } else {
                    Log.e(TAG, "❌ State manager is null");
                }
            }
            
            @Override
            public void onMessageUpdated(ChatMessage message) {
                // 🔥 CRITICAL: Route through state manager (single source of truth)
                if (stateManager != null) {
                    stateManager.updateMessages(coordinator.getOrderedMessages());
                    Log.d(TAG, "🔥 MESSAGES ROUTED TO STATE MANAGER: " + coordinator.getOrderedMessages().size());
                } else {
                    Log.e(TAG, "❌ State manager is null");
                }
            }
            
            @Override
            public void onMessageRemoved(String messageId) {
                Log.d(TAG, "Message removed: " + messageId);
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
        
        Log.d(TAG, "✅ NO-CRASH engines initialized");
    }
    
    /**
     * 🔥 START MESSAGE LISTENING - Firebase → State Manager → Serialization Queue
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
                
                // 🔥 CRITICAL: Route through state manager (single source of truth)
                if (stateManager != null) {
                    stateManager.updateMessages(messages);
                    Log.d(TAG, "🔥 FIREBASE → STATE MANAGER: " + messages.size() + " messages");
                } else {
                    Log.e(TAG, "❌ State manager is null in Firebase listener");
                }
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "❌ Firebase listener error", error);
            }
        });
        
        Log.d(TAG, "✅ NO-CRASH message listening started");
    }
    
    /**
     * 🚀 SEND MESSAGE - No-crash pipeline
     */
    private void sendMessage() {
        String text = editTextMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        
        Log.d(TAG, "🔥 NO-CRASH SEND START: " + text.substring(0, Math.min(20, text.length())) + "...");
        
        // Create optimistic message
        ChatMessage optimisticMessage = new ChatMessage();
        optimisticMessage.setMessageId(java.util.UUID.randomUUID().toString());
        optimisticMessage.setSenderId(currentUserId);
        optimisticMessage.setReceiverId(otherUserId);
        optimisticMessage.setText(text);
        optimisticMessage.setTimestamp(new com.google.firebase.Timestamp(System.currentTimeMillis() / 1000, 0));
        optimisticMessage.setStatus(ChatMessage.MessageStatus.SENDING);
        
        Log.d(TAG, "🔥 OPTIMISTIC MESSAGE CREATED: " + optimisticMessage.getMessageId());
        
        // 🔥 Route through coordinator for optimistic UI + deduplication
        if (coordinator != null) {
            Log.d(TAG, "🔥 ROUTING THROUGH COORDINATOR");
            coordinator.processOptimisticMessage(optimisticMessage, chatId);
        } else {
            Log.e(TAG, "❌ COORDINATOR IS NULL");
        }
        
        // Clear input
        editTextMessage.setText("");
        
        Log.d(TAG, "✅ NO-CRASH SEND COMPLETE");
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
        
        Log.d(TAG, "✅ NO-CRASH ChatFragment destroyed");
    }
    
    /**
     * 🔥 DEBUG: Get current engine state
     */
    public void debugEngineState() {
        if (noCrashEngine != null) {
            Log.d(TAG, "🔥 ENGINE DEBUG:");
            Log.d(TAG, "  - Current messages: " + noCrashEngine.getCurrentMessages().size());
            Log.d(TAG, "  - Queue size: " + noCrashEngine.getQueueSize());
            Log.d(TAG, "  - Is rendering: " + noCrashEngine.isRendering());
        }
    }
}
