package com.example.kitchenbrain;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.text.TextUtils;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kitchenbrain.repository.ChatRepository;
import com.example.kitchenbrain.repository.UserRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

// Cloudinary not used anymore


import androidx.lifecycle.LifecycleOwner;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 🚨 DEPRECATED - DO NOT USE
 * 
 * This fragment is DEPRECATED and will be removed in a future version.
 * All chat navigation should use ChatFragment instead.
 * 
 * @deprecated Use ChatFragment instead. All chat openings should go through
 *             MainActivity.openChat(userId, username) for consistent behavior.
 * 
 * @see ChatFragment
 * @see com.example.kitchenbrain.MainActivity#openChat(String, String)
 */
@Deprecated
public class ChatFragmentPremium extends Fragment {

    private static final String TAG = "ChatFragmentPremium";
    
    // UI Components
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private ImageButton buttonBack;
    private TextView textViewOnlineStatus;
    private TextView textViewUserName;
    private View emptyStateView;
    private View typingIndicatorView;
    
    // Data & Firebase
    private PremiumChatMessageAdapter messageAdapter;
    private List<ChatMessage> messageList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;
    private String otherUserId;
    private String chatId;
    private ListenerRegistration messagesListener;
    
    // State
    private boolean isTyping = false;
    private long typingStartTime = 0;
    
    // Debounce for message sending
    private AtomicBoolean isSendingMessage = new AtomicBoolean(false);
    private static final long MIN_SEND_INTERVAL_MS = 300;
    private long lastSendTime = 0;
    
    // Throttle online status updates
    private boolean lastKnownOnlineStatus = false;
    private long lastStatusUpdateTime = 0;
    private static final long STATUS_UPDATE_THROTTLE_MS = 5000;
    
    // Background executor for heavy ops
    private ExecutorService backgroundExecutor = Executors.newFixedThreadPool(4);
    
    // Handler for main thread operations
    private android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    
    // Message sender service
    private MessageSenderService messageSenderService;
    private Map<String, String> queueIdToMessageIdMap = new HashMap<>(); // Track queue IDs
    



    public static ChatFragmentPremium newInstance(String currentUserId, String otherUserId) {
        ChatFragmentPremium fragment = new ChatFragmentPremium();
        Bundle args = new Bundle();
        args.putString("current_user_id", currentUserId);
        args.putString("other_user_id", otherUserId);
        fragment.setArguments(args);
        return fragment;
    }

    private void loadOtherUserName() {
        if (otherUserId == null || db == null) {
            Log.w(TAG, "loadOtherUserName called but otherUserId or db is null");
            return;
        }
        
        Log.d(TAG, "Loading user name for userId: " + otherUserId);
        
        // Set placeholder text while loading
        textViewUserName.setText("Loading...");
        
        db.collection("users").document(otherUserId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            String nickname = documentSnapshot.getString("nickname");
                            String fullName = documentSnapshot.getString("fullName");
                            
                            Log.d(TAG, "User data loaded - username: " + username + ", nickname: " + nickname + ", fullName: " + fullName);
                            
                            // Priority: nickname > username > fullName
                            String displayName = "User";
                            if (nickname != null && !nickname.isEmpty()) {
                                displayName = nickname;
                                Log.d(TAG, "Using nickname: " + displayName);
                            } else if (username != null && !username.isEmpty()) {
                                displayName = username;
                                Log.d(TAG, "Using username: " + displayName);
                            } else if (fullName != null && !fullName.isEmpty()) {
                                displayName = fullName;
                                Log.d(TAG, "Using fullName: " + displayName);
                            }
                            
                            textViewUserName.setText(displayName);
                            Log.d(TAG, "Username set to: " + displayName);
                        } else {
                            Log.w(TAG, "User document does not exist for userId: " + otherUserId);
                            textViewUserName.setText("User");
                        }
                    });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to load user name", e);
                if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        textViewUserName.setText("User");
                    });
                }
            });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_premium, container, false);
        
        // Initialize Firebase FIRST
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        // ✅ REMOVED: Firestore settings now configured in MyApplication (global, once)
        // Calling setFirestoreSettings() here causes crash if Firestore already initialized elsewhere
        Log.d(TAG, "Firebase initialized (settings configured in Application)");
        
        // Setup presence system
        setupFirebasePresence();
        
        // Get current user ID from Firebase Auth if not provided
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
            Log.d(TAG, "Current user ID from Auth: " + currentUserId);
        } else {
            Log.e(TAG, "User not authenticated - Firebase Auth returned null");
        }
        
        // Get user IDs from arguments
        if (getArguments() != null) {
            otherUserId = getArguments().getString("other_user_id");
            String argCurrentUserId = getArguments().getString("current_user_id");
            
            Log.d(TAG, "Arguments received - otherUserId: " + otherUserId);
            
            // Use argument if provided, otherwise use Firebase Auth
            if (argCurrentUserId != null) {
                currentUserId = argCurrentUserId;
                Log.d(TAG, "Using currentUserId from arguments: " + currentUserId);
            }
        }
        
        // Validate user IDs
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "VALIDATION FAILED: currentUserId is null or empty");
            Toast.makeText(getContext(), "Error: User not logged in", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
            return view;
        }
        
        if (otherUserId == null || otherUserId.isEmpty()) {
            Log.e(TAG, "VALIDATION FAILED: otherUserId is null or empty");
            Toast.makeText(getContext(), "Error: No recipient specified", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
            return view;
        }
        
        Log.d(TAG, "Validation passed - currentUserId: " + currentUserId + ", otherUserId: " + otherUserId);
        
        // Check mutual follow before allowing chat
        checkMutualFollowBeforeOpening();
        
        // Initialize reliable message sender service
        initializeMessageSenderService();
        
        initViews(view);
        setupRecyclerView();
        setupClickListeners();
        
        // Create chat ID and setup listeners
        createOrGetChatId();
        
        // Load other user's name
        loadOtherUserName();
        
        return view;
    }

    private void initViews(View view) {
        recyclerViewMessages = view.findViewById(R.id.recyclerViewMessages);
        editTextMessage = view.findViewById(R.id.editTextMessage);
        buttonSend = view.findViewById(R.id.buttonSend);
        buttonBack = view.findViewById(R.id.buttonBack);
        textViewOnlineStatus = view.findViewById(R.id.textUserStatus);
        textViewUserName = view.findViewById(R.id.textUserName);  // Initialize user name
        emptyStateView = view.findViewById(R.id.emptyStateView);
        typingIndicatorView = view.findViewById(R.id.layoutTyping);
        
        // Firebase is already initialized in onCreateView
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new PremiumChatMessageAdapter(messageList, currentUserId);
        
        // Use standard LinearLayoutManager
        // setStackFromEnd(true) causes messages to be hidden at the top
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(false); // FALSE - allows seeing all messages from top
        layoutManager.setReverseLayout(false);
        
        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setAdapter(messageAdapter);
        
        // Add item animator for smooth animations
        recyclerViewMessages.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());
        
        // FIX: Ensure RecyclerView is properly configured for chat
        recyclerViewMessages.setHasFixedSize(false);
        recyclerViewMessages.setItemViewCacheSize(20);
        
        Log.d(TAG, "RecyclerView setup complete. LayoutManager: " + layoutManager.getClass().getSimpleName());
        Log.d(TAG, "stackFromEnd: " + layoutManager.getStackFromEnd());
        Log.d(TAG, "reverseLayout: " + layoutManager.getReverseLayout());
        
        // Scroll listener for FAB and empty state
        recyclerViewMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                if (!recyclerView.canScrollVertically(-1)) {
                    // At top - hide empty state
                    if (emptyStateView != null) {
                        emptyStateView.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    /**
     * Setup Firebase presence for online/offline status
     */
    private void setupFirebasePresence() {
        if (auth.getCurrentUser() == null) {
            Log.w(TAG, "Cannot setup presence - user not authenticated");
            return;
        }
        
        String currentUserId = auth.getCurrentUser().getUid();
        
        // Create a reference to the database location for presence
        // Note: For Realtime Database presence, you would use:
        // FirebaseDatabase.getInstance().getReference().child("presence")
        // But since we're using Firestore, we'll use a hybrid approach
        
        Log.d(TAG, "Setting up Firebase presence system for userId: " + currentUserId);
        
        // For Firestore-based presence, we need to handle it manually
        // The key improvement is setting offline in onDestroyView (already done)
        
        // TODO: If you add Firebase Realtime Database later, implement full presence here:
        /*
        DatabaseReference connectedRef = FirebaseDatabase.getInstance()
            .getReference(".info/connected");
        
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (!connected) {
                    // Connection lost - will trigger onDisconnect automatically
                    return;
                }
                
                // Set up onDisconnect BEFORE setting online
                DatabaseReference myStatusRef = FirebaseDatabase.getInstance()
                    .getReference("presence").child(currentUserId);
                    
                myStatusRef.onDisconnect().set(false);
                
                // Set online status
                myStatusRef.set(true);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Presence connection error", error.toException());
            }
        });
        */
    }

    private void setupClickListeners() {
        // Send button
        buttonSend.setOnClickListener(v -> sendMessage());
        
        // Back button - ENHANCED WITH ERROR HANDLING
        buttonBack.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            
            try {
                // Method 1: Try using Fragment's built-in back navigation
                if (getActivity() != null && !getActivity().isFinishing()) {
                    Log.d(TAG, "Calling getActivity().onBackPressed()");
                    getActivity().onBackPressed();
                } else if (getActivity() != null && getActivity().isFinishing()) {
                    Log.w(TAG, "Activity is finishing, cannot navigate back");
                } else {
                    Log.e(TAG, "Activity is null, cannot navigate back");
                }
            } catch (Exception e) {
                Log.e(TAG, "CRASH: Error in back button click listener", e);
                Log.e(TAG, "Error type: " + e.getClass().getSimpleName());
                Log.e(TAG, "Error message: " + e.getMessage());
                
                // Fallback: Try to finish the parent activity directly
                if (getActivity() != null) {
                    try {
                        Log.d(TAG, "Fallback: Finishing activity directly");
                        getActivity().finish();
                    } catch (Exception e2) {
                        Log.e(TAG, "Fallback also failed", e2);
                    }
                }
            }
        });
        
        editTextMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
        
        // Typing detection
        editTextMessage.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    stopTyping();
                } else {
                    startTyping();
                }
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    /**
     * Init message sender service
     */
    private void initializeMessageSenderService() {
        messageSenderService = MessageSenderService.getInstance();
        messageSenderService.initialize(db);
        
        // Register lifecycle observer for proper cleanup
        if (getActivity() instanceof LifecycleOwner) {
            ((LifecycleOwner) getActivity()).getLifecycle().addObserver(messageSenderService);
        }
        
        // Set up status change listener to update UI
        messageSenderService.setStatusChangeListener(new MessageSenderService.OnMessageStatusChangeListener() {
            @Override
            public void onMessageQueued(PendingMessage message) {
                Log.d(TAG, "Message queued: " + message.getQueueId());
                // Add pending message to UI
                addPendingMessageToUI(message);
            }

            @Override
            public void onMessageSent(PendingMessage message) {
                Log.d(TAG, "Message sent successfully: " + message.getMessageId());
                // Update message status in UI
                updateMessageStatusInUI(message.getMessageId(), MessageStatus.SENT);
                
                // Track the mapping
                queueIdToMessageIdMap.put(message.getQueueId(), message.getMessageId());
            }

            @Override
            public void onMessageFailed(PendingMessage message, Exception error) {
                Log.w(TAG, "Message send failed (will retry): " + message.getQueueId());
                // Update UI to show failed status
                updateMessageStatusInUI(message.getMessageId(), MessageStatus.FAILED);
            }

            @Override
            public void onMessageMaxRetriesReached(PendingMessage message) {
                Log.e(TAG, "Message failed after max retries: " + message.getQueueId());
                // Show error to user
                updateMessageStatusInUI(message.getMessageId(), MessageStatus.FAILED);
                Toast.makeText(getContext(), 
                    "Failed to send message after multiple attempts", 
                    Toast.LENGTH_LONG).show();
            }
        });
        
        Log.d(TAG, "MessageSenderService initialized and lifecycle registered");
    }

    /**
     * Check if users have mutual follow relationship before allowing chat
     */
    private void checkMutualFollowBeforeOpening() {
        UserRepository userRepository = new UserRepository();
        userRepository.checkMutualFollow(currentUserId, otherUserId, result -> {
            if (result.isError()) {
                Log.e(TAG, "❌ Error checking mutual follow: " + result.getError());
                if (isAdded() && getContext() != null) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), 
                            "Error: " + result.getError(), 
                            Toast.LENGTH_LONG).show();
                        requireActivity().onBackPressed();
                    });
                }
                return;
            }
            
            boolean isMutual = result.getData();
            if (!isMutual) {
                Log.w(TAG, "⚠️ Users do not have mutual follow relationship");
                if (isAdded() && getContext() != null) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), 
                            "You can only chat with users you mutually follow", 
                            Toast.LENGTH_LONG).show();
                        requireActivity().onBackPressed();
                    });
                }
            } else {
                Log.d(TAG, "✅ Mutual follow confirmed - proceeding with chat");
                createOrGetChatId();
            }
        });
    }

    /**
     * Create or get existing chat ID based on user IDs
     */
    private void createOrGetChatId() {
        // Create consistent chat ID (alphabetically sorted to ensure uniqueness)
        if (currentUserId.compareTo(otherUserId) < 0) {
            chatId = currentUserId + "_" + otherUserId;
        } else {
            chatId = otherUserId + "_" + currentUserId;
        }
        
        Log.d(TAG, "Chat ID created: " + chatId);
        
        // Load existing messages
        loadExistingMessages();
        
        // Start listening for new messages in real-time
        startListeningForNewMessages();
        
        // Listen for other user's online status
        listenToUserStatus();
    }

    /**
     * Add a pending message to the UI (shows "sending" state)
     */
    private void addPendingMessageToUI(PendingMessage pendingMsg) {
        if (getActivity() == null || getActivity().isFinishing()) return;
        
        getActivity().runOnUiThread(() -> {
            try {
                ChatMessage chatMsg = pendingMsg.toChatMessage();
                messageList.add(chatMsg);
                int position = messageList.size() - 1;
                
                messageAdapter.notifyItemInserted(position);
                scrollToBottom();
                
                Log.d(TAG, "Pending message added to UI at position: " + position);
            } catch (Exception e) {
                Log.e(TAG, "Error adding pending message to UI", e);
            }
        });
    }

    /**
     * Update message status in UI (sent, delivered, read, failed)
     */
    private void updateMessageStatusInUI(String messageId, MessageStatus newStatus) {
        if (getActivity() == null || getActivity().isFinishing()) return;
        
        getActivity().runOnUiThread(() -> {
            try {
                for (int i = 0; i < messageList.size(); i++) {
                    ChatMessage msg = messageList.get(i);
                    if (msg.getMessageId().equals(messageId)) {
                        msg.setMessageStatus(newStatus.getValue());
                        messageAdapter.notifyItemChanged(i);
                        Log.d(TAG, "Message status updated: " + messageId + " -> " + newStatus);
                        break;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating message status in UI", e);
            }
        });
    }

    /**
     * Send a text message - RELIABLE IMPLEMENTATION WITH RETRY LOGIC
     * Uses MessageSenderService for guaranteed delivery
     */
    private void sendMessage() {
        // Debounce rapid clicks
        long currentTime = System.currentTimeMillis();
        
        if (!isSendingMessage.compareAndSet(false, true)) {
            Log.w(TAG, "Message send already in progress - ignoring duplicate click");
            return; // Already sending, ignore this click
        }
        
        // Check minimum interval between messages
        if (currentTime - lastSendTime < MIN_SEND_INTERVAL_MS) {
            Log.w(TAG, "Message sent too quickly - debouncing");
            isSendingMessage.set(false);
            return;
        }
        lastSendTime = currentTime;
        
        try {
            String text = editTextMessage.getText().toString().trim();
            
            // Comprehensive validation
            if (TextUtils.isEmpty(text)) {
                Log.w(TAG, "Message text is empty");
                isSendingMessage.set(false);
                return;
            }
            
            if (chatId == null || chatId.isEmpty()) {
                Log.e(TAG, "Chat not initialized");
                Toast.makeText(getContext(), "Chat not initialized. Please wait...", Toast.LENGTH_SHORT).show();
                isSendingMessage.set(false);
                return;
            }
            
            if (currentUserId == null || currentUserId.isEmpty()) {
                Log.e(TAG, "User not authenticated");
                Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
                isSendingMessage.set(false);
                return;
            }
            
            if (otherUserId == null || otherUserId.isEmpty()) {
                Log.e(TAG, "Recipient not found");
                Toast.makeText(getContext(), "Recipient not found", Toast.LENGTH_SHORT).show();
                isSendingMessage.set(false);
                return;
            }
            
            Log.d(TAG, "=== SEND MESSAGE STARTED ===");
            Log.d(TAG, "Text: '" + text + "'");
            Log.d(TAG, "Chat ID: " + chatId);
            
            // Stop typing indicator
            stopTyping();
            
            // Use reliable MessageSenderService with automatic retry
            String queueId = messageSenderService.sendMessage(
                chatId, 
                currentUserId, 
                otherUserId, 
                text, 
                "text"
            );
            
            if (queueId != null) {
                Log.d(TAG, "Message queued with ID: " + queueId);
                // Clear input field immediately - message is safely queued
                editTextMessage.setText("");
            } else {
                Log.e(TAG, "Failed to queue message");
                Toast.makeText(getContext(), "Failed to send message", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in sendMessage", e);
            Log.e(TAG, "Error type: " + e.getClass().getSimpleName());
            Log.e(TAG, "Error message: " + e.getMessage());
            
            if (getContext() != null && isAdded()) {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
            }
        } finally {
            // Always reset sending flag
            isSendingMessage.set(false);
        }
    }

    /**
     * Create chat metadata document
     */
    private Map<String, Object> createChatMetadata() {
        Map<String, Object> chatData = new HashMap<>();
        List<String> participants = new ArrayList<>();
        
        if (currentUserId != null && !currentUserId.isEmpty()) {
            participants.add(currentUserId);
        }
        if (otherUserId != null && !otherUserId.isEmpty()) {
            participants.add(otherUserId);
        }
        
        chatData.put("participants", participants);
        chatData.put("lastUpdated", com.google.firebase.Timestamp.now());
        return chatData;
    }

    /**
     * Load existing messages from Firestore - OPTIMIZED WITH BACKGROUND THREAD
     */
    private void loadExistingMessages() {
        if (chatId == null) return;
        
        // Run on background thread
        backgroundExecutor.execute(() -> {
            try {
                db.collection("messages")
                    .document(chatId)
                    .collection("user_messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .limit(50)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        // Process data on background first
                        backgroundExecutor.execute(() -> {
                            try {
                                List<ChatMessage> newMessages = new ArrayList<>();
                                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                    ChatMessage message = document.toObject(ChatMessage.class);
                                    if (message != null) {
                                        message.setMessageId(document.getId());
                                        // Ensure required fields exist
                                        if (message.getText() == null) message.setText("");
                                        if (message.getSenderId() == null) message.setSenderId("");
                                        if (message.getReceiverId() == null) message.setReceiverId("");
                                        if (message.getTimestamp() == null) message.setTimestamp(Timestamp.now());
                                        if (message.getMessageStatus() == null) message.setMessageStatus("sent");
                                        
                                        newMessages.add(message);
                                    }
                                }
                                
                                // NOW update UI on main thread with pre-processed data
                                mainHandler.post(() -> {
                                    messageList.clear();
                                    messageList.addAll(newMessages);
                                    
                                    Log.d(TAG, "Loaded " + messageList.size() + " messages from Firestore");
                                    
                                    // Notify adapter before scrolling
                                    messageAdapter.notifyDataSetChanged();
                                    checkEmptyState();
                                    
                                    // Scroll to bottom AFTER layout is complete (150ms delay ensures views are ready)
                                    recyclerViewMessages.postDelayed(() -> {
                                        scrollToBottom();
                                        Log.d(TAG, "Initial scroll to bottom after loading " + messageList.size() + " messages");
                                    }, 150);
                                });
                            } catch (Exception e) {
                                Log.e(TAG, "Background message processing error", e);
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load messages", e);
                        mainHandler.post(() -> {
                            if (getContext() != null && isAdded()) {
                                Toast.makeText(getContext(), "Failed to load messages", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
            } catch (Exception e) {
                Log.e(TAG, "Background load error", e);
            }
        });
    }

    /**
     * Listen for new messages
     */
    private void startListeningForNewMessages() {
        if (chatId == null) {
            Log.w(TAG, "startListeningForNewMessages called but chatId is null");
            return;
        }
        
        Log.d(TAG, "Starting real-time message listener for chatId: " + chatId);
        
        messagesListener = db.collection("messages")
            .document(chatId)
            .collection("user_messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener((snapshot, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error listening for messages", error);
                    return;
                }
                
                if (snapshot != null && !snapshot.isEmpty()) {
                    Log.d(TAG, "Received snapshot with " + snapshot.getDocumentChanges().size() + " changes");
                    
                    for (DocumentChange dc : snapshot.getDocumentChanges()) {
                        switch (dc.getType()) {
                            case ADDED:
                                ChatMessage newMessage = dc.getDocument().toObject(ChatMessage.class);
                                if (newMessage != null) {
                                    newMessage.setMessageId(dc.getDocument().getId());
                                    
                                    // Skip own messages
                                    if (newMessage.getSenderId() != null && 
                                        newMessage.getSenderId().equals(currentUserId)) {
                                        Log.d(TAG, "Skipping own message from listener: " + newMessage.getMessageId());
                                        break; // Skip - already added to UI when sent
                                    }
                                    
                                    // Check if message already exists to avoid duplicates
                                    boolean exists = false;
                                    for (ChatMessage msg : messageList) {
                                        if (msg.getMessageId() != null && 
                                            msg.getMessageId().equals(newMessage.getMessageId())) {
                                            exists = true;
                                            break;
                                        }
                                    }
                                    if (!exists) {
                                        Log.d(TAG, "Adding new message from listener: " + newMessage.getMessageId());
                                        messageList.add(newMessage);
                                        messageAdapter.notifyItemInserted(messageList.size() - 1);
                                        checkEmptyState();
                                        scrollToBottom();
                                    } else {
                                        Log.d(TAG, "Message already exists, skipping: " + newMessage.getMessageId());
                                    }
                                }
                                break;
                            case MODIFIED:
                                // Handle message status updates (delivered/read)
                                ChatMessage updatedMessage = dc.getDocument().toObject(ChatMessage.class);
                                if (updatedMessage != null) {
                                    updatedMessage.setMessageId(dc.getDocument().getId());
                                    messageAdapter.updateMessageStatus(
                                        updatedMessage.getMessageId(), 
                                        updatedMessage.getMessageStatus()
                                    );
                                }
                                break;
                            case REMOVED:
                                // Handle message deletion if needed
                                String docId = dc.getDocument().getId();
                                for (int i = 0; i < messageList.size(); i++) {
                                    ChatMessage msg = messageList.get(i);
                                    if (msg != null && msg.getMessageId() != null && 
                                        msg.getMessageId().equals(docId)) {
                                        messageList.remove(i);
                                        messageAdapter.notifyItemRemoved(i);
                                        break;
                                    }
                                }
                                break;
                        }
                    }
                } else {
                    Log.d(TAG, "Snapshot is null or empty");
                }
            });
        
        Log.d(TAG, "Real-time message listener registered successfully");
    }

    /**
     * Listen to other user's online status with improved presence tracking
     */
    private void listenToUserStatus() {
        if (otherUserId == null) return;
        
        db.collection("users").document(otherUserId)
            .addSnapshotListener((documentSnapshot, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error getting user status", error);
                    return;
                }
                
                if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            Boolean isOnline = documentSnapshot.getBoolean("isOnline");
                            
                            if (isOnline != null && isOnline) {
                                textViewOnlineStatus.setText("Online");
                                textViewOnlineStatus.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_green));
                            } else {
                                // Try to get lastSeen timestamp
                                Object lastSeenObj = documentSnapshot.get("lastSeen");
                                if (lastSeenObj instanceof Timestamp) {
                                    Timestamp lastSeen = (Timestamp) lastSeenObj;
                                    String lastSeenText = formatLastSeen(lastSeen);
                                    textViewOnlineStatus.setText(lastSeenText);
                                    textViewOnlineStatus.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
                                } else {
                                    textViewOnlineStatus.setText("Offline");
                                    textViewOnlineStatus.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
                                }
                            }
                        } else {
                            textViewOnlineStatus.setText("Offline");
                            textViewOnlineStatus.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
                        }
                    });
                }
            });
    }

    /**
     * Format last seen timestamp
     */
    private String formatLastSeen(Timestamp timestamp) {
        if (timestamp == null) return "Offline";
        
        try {
            Date date = timestamp.toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            return "Last seen " + sdf.format(date);
        } catch (Exception e) {
            return "Offline";
        }
    }

    /**
     * Check and update empty state visibility
     */
    private void checkEmptyState() {
        if (emptyStateView != null) {
            if (messageList.isEmpty()) {
                emptyStateView.setVisibility(View.VISIBLE);
            } else {
                emptyStateView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Show delete confirmation
     */
    private void showDeleteConfirmationDialog() {
        Log.d("CHAT_DEBUG", "=== DELETE BUTTON CLICKED ===");
        
        // Check fragment state
        if (getContext() == null) {
            Log.e("CHAT_ERROR", "Context is NULL - cannot show dialog");
            return;
        }
        
        if (!isAdded()) {
            Log.e("CHAT_ERROR", "Fragment not added - cannot show dialog");
            return;
        }
        
        Log.d("CHAT_DEBUG", "Context and fragment state OK");
        Log.d("CHAT_DEBUG", "Showing delete confirmation dialog");
        
        try {
            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Delete Chat?")
                .setMessage("This action cannot be undone. All messages will be permanently deleted.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Log.d("CHAT_DEBUG", "User confirmed delete");
                    // Apply haptic feedback
                    vibrateIfPossible();
                    deleteChatHistory();
                })
                .setNegativeButton("Cancel", null)
                .setIcon(R.drawable.ic_delete)
                .show();
                
            Log.d("CHAT_DEBUG", "Dialog shown successfully");
        } catch (Exception e) {
            Log.e("CHAT_ERROR", "Failed to show dialog", e);
        }
    }

    /**
     * Delete chat history from Firestore
     */
    private void deleteChatHistory() {
        Log.d("CHAT_DEBUG", "=== DELETE CHAT HISTORY STARTED ===");
        
        // Check required fields
        Log.d("CHAT_DEBUG", "Checking initialization...");
        Log.d("CHAT_DEBUG", "chatId: " + (chatId != null ? chatId : "NULL"));
        Log.d("CHAT_DEBUG", "db: " + (db != null ? "OK" : "NULL"));
        Log.d("CHAT_DEBUG", "getContext(): " + (getContext() != null ? "OK" : "NULL"));
        Log.d("CHAT_DEBUG", "isAdded(): " + isAdded());
        
        if (chatId == null || db == null || getContext() == null || !isAdded()) {
            Log.e("CHAT_ERROR", "Cannot delete chat - not properly initialized");
            Log.e("CHAT_ERROR", "chatId=" + (chatId == null ? "null" : "valid"));
            Log.e("CHAT_ERROR", "db=" + (db == null ? "null" : "valid"));
            Log.e("CHAT_ERROR", "context=" + (getContext() == null ? "null" : "valid"));
            Log.e("CHAT_ERROR", "isAdded=" + isAdded());
            
            if (getContext() != null) {
                Toast.makeText(getContext(), "Chat not initialized", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        
        Log.d("CHAT_DEBUG", "All checks passed");
        
        // Check if button initialized
        // Deleted: Log.d("CHAT_DEBUG", "buttonDeleteChat: " + (buttonDeleteChat != null ? "OK" : "NULL"));
        
        // Deleted: if (buttonDeleteChat == null) {
        // Deleted:     Log.e("CHAT_ERROR", "Delete button is null - cannot update UI state");
        // Deleted:     return;
        // Deleted: }
        
        Log.d("CHAT_DEBUG", "Button exists, proceeding with deletion");
        
        try {
            // Show loading state - MUST be on UI thread
            Log.d("CHAT_DEBUG", "Setting button to loading state");
            
            if (getActivity() != null && !getActivity().isFinishing()) {
                getActivity().runOnUiThread(() -> {
                    Log.d("CHAT_DEBUG", "Running on UI thread");
                    // Deleted: if (buttonDeleteChat != null) {
                    // Deleted:     buttonDeleteChat.setEnabled(false);
                    // Deleted:     buttonDeleteChat.setAlpha(0.5f);
                    // Deleted:     Log.d("CHAT_DEBUG", "Button state updated");
                    // Deleted: }
                });
            } else {
                Log.e("CHAT_ERROR", "Activity not available for UI update");
            }
            
            // Get all messages in the chat
            Log.d("CHAT_DEBUG", "Fetching messages from Firestore: chats/" + chatId + "/messages");
            
            db.collection("chats")
                .document(chatId)
                .collection("messages")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("CHAT_DEBUG", "Successfully fetched messages");
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d("CHAT_DEBUG", "No messages to delete");
                        clearLocalChatData();
                        return;
                    }
                    
                    int messageCount = queryDocumentSnapshots.size();
                    Log.d("CHAT_DEBUG", "Found " + messageCount + " messages to delete");
                    
                    // Delete each message
                    int[] deleteCount = {0};
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String messageId = document.getId();
                        Log.d("CHAT_DEBUG", "Deleting message: " + messageId);
                        
                        try {
                            db.collection("chats")
                                .document(chatId)
                                .collection("messages")
                                .document(messageId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    deleteCount[0]++;
                                    Log.d("CHAT_DEBUG", "Message deleted: " + deleteCount[0] + "/" + messageCount);
                                    
                                    // When all messages are deleted
                                    if (deleteCount[0] >= messageCount) {
                                        Log.d("CHAT_DEBUG", "All messages deleted successfully");
                                        clearLocalChatData();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("CHAT_ERROR", "Failed to delete message: " + messageId, e);
                                    resetDeleteButtonState();
                                    if (getContext() != null && isAdded()) {
                                        Toast.makeText(getContext(), "Failed to delete some messages", Toast.LENGTH_SHORT).show();
                                    }
                                });
                        } catch (Exception e) {
                            Log.e("CHAT_ERROR", "Exception while deleting message: " + messageId, e);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("CHAT_ERROR", "Failed to get messages for deletion", e);
                    Log.e("CHAT_ERROR", "Error type: " + e.getClass().getSimpleName());
                    Log.e("CHAT_ERROR", "Error message: " + e.getMessage());
                    resetDeleteButtonState();
                    if (getContext() != null && isAdded()) {
                        Toast.makeText(getContext(), "Failed to delete chat: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
                
            Log.d("CHAT_DEBUG", "Delete operation initiated successfully");
            
        } catch (Exception e) {
            Log.e("CHAT_ERROR", "CRASH: Exception in deleteChatHistory", e);
            Log.e("CHAT_ERROR", "Error type: " + e.getClass().getSimpleName());
            Log.e("CHAT_ERROR", "Error message: " + e.getMessage());
            e.printStackTrace();
            
            if (getContext() != null && isAdded()) {
                Toast.makeText(getContext(), "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Reset delete button state after operation completes
     */
    private void resetDeleteButtonState() {
        if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
            getActivity().runOnUiThread(() -> {
                // Deleted: if (buttonDeleteChat != null) {
                // Deleted:     buttonDeleteChat.setEnabled(true);
                // Deleted:     buttonDeleteChat.setAlpha(1.0f);
                // Deleted: }
            });
        }
    }

    /**
     * Clear local chat data
     */
    private void clearLocalChatData() {
        Log.d("CHAT_DEBUG", "=== CLEARING LOCAL CHAT DATA ===");
        
        if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
            getActivity().runOnUiThread(() -> {
                try {
                    Log.d("CHAT_DEBUG", "Running on UI thread");
                    
                    // Clear message list
                    Log.d("CHAT_DEBUG", "Clearing message list (size: " + messageList.size() + ")");
                    messageList.clear();
                    messageAdapter.notifyDataSetChanged();
                    Log.d("CHAT_DEBUG", "Message list cleared and adapter notified");
                    
                    // Show empty state
                    checkEmptyState();
                    Log.d("CHAT_DEBUG", "Empty state checked");
                    
                    // Reset button state safely
                    // Deleted: if (buttonDeleteChat != null) {
                    // Deleted:     buttonDeleteChat.setEnabled(true);
                    // Deleted:     buttonDeleteChat.setAlpha(1.0f);
                    // Deleted: } else {
                    // Deleted:     Log.w("CHAT_WARNING", "buttonDeleteChat is NULL - cannot reset");
                    // Deleted: }
                    
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Chat deleted successfully", Toast.LENGTH_SHORT).show();
                        Log.d("CHAT_DEBUG", "Success toast shown");
                    }
                    
                    Log.d("CHAT_DEBUG", "=== LOCAL CHAT DATA CLEARED SUCCESSFULLY ===");
                    
                } catch (Exception e) {
                    Log.e("CHAT_ERROR", "CRASH in clearLocalChatData", e);
                    e.printStackTrace();
                }
            });
        } else {
            Log.e("CHAT_ERROR", "Cannot clear data - activity not available or fragment not added");
        }
    }

    /**
     * Vibrate on button press (haptic feedback) - WITH PERMISSION CHECK
     */
    private void vibrateIfPossible() {
        if (getContext() != null) {
            // Check VIBRATE permission
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                // Android 12+ requires runtime permission check for VIBRATE
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        getContext(), 
                        android.Manifest.permission.VIBRATE) != 
                        android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "VIBRATE permission not granted, skipping vibration");
                    return;
                }
            }
            
            android.os.Vibrator vibrator = (android.os.Vibrator) getContext()
                .getSystemService(android.content.Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, 
                            android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(50);
                    }
                    Log.d(TAG, "Vibration successful");
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException during vibration", e);
                }
            }
        }
    }

    private void startTyping() {
        if (!isTyping) {
            isTyping = true;
            typingStartTime = System.currentTimeMillis();
            // TODO: Send typing indicator to Firestore
            showTypingIndicator();
        }
    }

    private void stopTyping() {
        if (isTyping && System.currentTimeMillis() - typingStartTime > 1000) {
            isTyping = false;
            // TODO: Clear typing indicator in Firestore
            hideTypingIndicator();
        }
    }

    private void showTypingIndicator() {
        if (typingIndicatorView != null) {
            typingIndicatorView.setVisibility(View.VISIBLE);
            scrollToBottom();
        }
    }

    private void hideTypingIndicator() {
        if (typingIndicatorView != null) {
            typingIndicatorView.setVisibility(View.GONE);
        }
    }

    private void scrollToBottom() {
        if (messageList.isEmpty()) {
            Log.d(TAG, "scrollToBottom skipped - messageList is empty");
            return;
        }
        
        int lastPosition = messageList.size() - 1;
        Log.d(TAG, "scrollToBottom called for position: " + lastPosition + " (total items: " + messageList.size() + ")");
        
        // Ensure we're on UI thread
        if (getActivity() != null && !getActivity().isFinishing()) {
            getActivity().runOnUiThread(() -> {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerViewMessages.getLayoutManager();
                if (layoutManager == null) {
                    Log.e(TAG, "LayoutManager is null!");
                    return;
                }
                
                // Wait for layout before scrolling
                recyclerViewMessages.post(() -> {
                    try {
                        // Method 1: Immediate scroll to position
                        recyclerViewMessages.scrollToPosition(lastPosition);
                        Log.d(TAG, "Called scrollToPosition(" + lastPosition + ")");
                        
                        // Smooth scroll backup
                        recyclerViewMessages.postDelayed(() -> {
                            recyclerViewMessages.smoothScrollToPosition(lastPosition);
                            Log.d(TAG, "Called smoothScrollToPosition(" + lastPosition + ")");
                        }, 100);
                        
                        // Method 3: Final correction check (250ms delay)
                        recyclerViewMessages.postDelayed(() -> {
                            try {
                                int currentLastVisible = layoutManager.findLastVisibleItemPosition();
                                if (currentLastVisible < lastPosition) {
                                    Log.d(TAG, "Final correction: current=" + currentLastVisible + ", target=" + lastPosition);
                                    recyclerViewMessages.scrollToPosition(lastPosition);
                                } else {
                                    Log.d(TAG, "Already at correct position: " + currentLastVisible);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error in final correction", e);
                            }
                        }, 250);
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error during scroll operation", e);
                    }
                });
                
            });
        } else {
            Log.w(TAG, "Cannot scroll - activity not available");
        }
    }

    private boolean isAtBottom() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerViewMessages.getLayoutManager();
        if (layoutManager != null) {
            int lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition();
            return lastVisibleItem >= messageList.size() - 1;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update online status to online
        updateOnlineStatus(true);
        
        // Mark messages as read when entering chat
        markMessagesAsRead();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Don't set offline here - user might just be switching apps temporarily
        // We'll use a more sophisticated approach with timestamps
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        Log.d(TAG, "onDestroyView called - cleaning up resources");
        
        // Remove message listener
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
            Log.d(TAG, "Firebase message listener removed");
        }
        
        // Reset sending flag to prevent stuck state
        isSendingMessage.set(false);
        
        // Set user offline when leaving
        updateOnlineStatus(false);
        
        // Also update last seen timestamp
        updateLastSeenTimestamp();
        
        // Shutdown executor to prevent leaks
        // Must be done AFTER updating online status to avoid RejectedExecutionException
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdown();
            Log.d(TAG, "Background executor shutdown");
        }
        
        // Unregister from MessageSenderService lifecycle observer
        // Cleanup but keep pending messages
        if (getActivity() instanceof LifecycleOwner && messageSenderService != null) {
            ((LifecycleOwner) getActivity()).getLifecycle().removeObserver(messageSenderService);
            Log.d(TAG, "MessageSenderService lifecycle observer removed");
        }
    }

    /**
     * Update user's online status in Firestore - OPTIMIZED WITH THROTTLING
     */
    private void updateOnlineStatus(boolean isOnline) {
        if (currentUserId == null || db == null) {
            Log.w(TAG, "updateOnlineStatus called but currentUserId or db is null");
            return;
        }
        
        // Throttle status updates
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastStatusUpdateTime < STATUS_UPDATE_THROTTLE_MS && 
            isOnline == lastKnownOnlineStatus) {
            Log.d(TAG, "Skipping status update (throttled): " + isOnline);
            return;
        }
        lastStatusUpdateTime = currentTime;
        lastKnownOnlineStatus = isOnline;
        
        // Check if executor is shutting down
        if (backgroundExecutor == null || backgroundExecutor.isShutdown() || backgroundExecutor.isTerminated()) {
            Log.w(TAG, "Cannot update online status - executor is shut down, updating directly on main thread");
            // Fallback: Update directly without executor (safe during fragment destruction)
            performOnlineStatusUpdate(isOnline);
            return;
        }
        
        // Run on background thread
        try {
            backgroundExecutor.execute(() -> {
                performOnlineStatusUpdate(isOnline);
            });
        } catch (RejectedExecutionException e) {
            Log.w(TAG, "Executor rejected task (shutting down), updating status directly: " + e.getMessage());
            // Fallback: Update directly if executor rejects the task
            performOnlineStatusUpdate(isOnline);
        }
    }
    
    /**
     * Perform the actual online status update in Firestore
     */
    private void performOnlineStatusUpdate(boolean isOnline) {
        try {
            Log.d(TAG, "Updating online status to: " + isOnline + " for userId: " + currentUserId);
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("isOnline", isOnline);
            
            // Only update lastSeen when going offline
            if (!isOnline) {
                updates.put("lastSeen", com.google.firebase.Timestamp.now());
                Log.d(TAG, "Adding lastSeen timestamp");
            }
            
            if (db != null && currentUserId != null) {
                db.collection("users").document(currentUserId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Updated online status successfully: " + isOnline);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update online status", e);
                        // Silent fail - don't show error to user
                    });
            }
        } catch (Exception e) {
            Log.e(TAG, "Update status error", e);
        }
    }

    /**
     * Update last seen timestamp
     */
    private void updateLastSeenTimestamp() {
        if (currentUserId == null || db == null) {
            Log.w(TAG, "updateLastSeenTimestamp called but currentUserId or db is null");
            return;
        }
        
        Log.d(TAG, "Updating last seen timestamp for userId: " + currentUserId);
        
        db.collection("users").document(currentUserId)
            .update("lastSeen", com.google.firebase.Timestamp.now())
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Updated last seen timestamp successfully");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to update last seen", e);
            });
    }

    /**
     * Mark all messages from other user as read - OPTIMIZED WITH BACKGROUND THREAD
     */
    private void markMessagesAsRead() {
        if (chatId == null || currentUserId == null) {
            Log.w(TAG, "markMessagesAsRead called but chatId or currentUserId is null");
            return;
        }
        
        // Run on background thread
        backgroundExecutor.execute(() -> {
            try {
                db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .whereEqualTo("receiverId", currentUserId)
                    .whereEqualTo("messageStatus", "sent")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " messages to mark as read");
                            // Batch update all messages to "read"
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                db.collection("chats")
                                    .document(chatId)
                                    .collection("messages")
                                    .document(doc.getId())
                                    .update("messageStatus", "read")
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Message marked as read");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to mark message as read", e);
                                    });
                            }
                        } else {
                            Log.d(TAG, "No messages to mark as read");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get messages to mark as read", e);
                    });
            } catch (Exception e) {
                Log.e(TAG, "Background mark as read error", e);
            }
        });
    }
}
