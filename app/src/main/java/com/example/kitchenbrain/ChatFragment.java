package com.example.kitchenbrain;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.Timestamp;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private TextView textViewOnlineStatus;
    private ChatMessageAdapter messageAdapter;
    private List<ChatMessage> messageList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;
    private String otherUserId;
    private String chatId;
    private ListenerRegistration messagesListener;
    private FriendManager friendManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        // Get other user ID from arguments first before initializing views
        Bundle args = getArguments();
        if (args != null) {
            otherUserId = args.getString("other_user_id");
            if (otherUserId != null && !otherUserId.isEmpty()) {
                Log.d(TAG, "Other user ID received: " + otherUserId);
            } else {
                Log.e(TAG, "No recipient specified in arguments");
                if (getContext() != null && isAdded()) {
                    Toast.makeText(getContext(), "Error: No recipient specified", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Log.e(TAG, "Chat arguments not provided");
            if (getContext() != null && isAdded()) {
                Toast.makeText(getContext(), "Error: Chat arguments not provided", Toast.LENGTH_SHORT).show();
            }
        }

        initViews(view);
        setupRecyclerView();
        setupClickListeners();

        // Check mutual follow status after initialization if we have a valid otherUserId
        if (otherUserId != null && !otherUserId.isEmpty()) {
            checkMutualFollowStatus();
        }

        return view;
    }

    private void initViews(View view) {
        Log.d(TAG, "initViews called");
        try {
            recyclerViewMessages = view.findViewById(R.id.recyclerViewMessages);
            editTextMessage = view.findViewById(R.id.editTextMessage);
            buttonSend = view.findViewById(R.id.buttonSend);
            textViewOnlineStatus = view.findViewById(R.id.textViewOnlineStatus);

            // Add a menu button for delete options (you'll need to add this button to your layout)
            ImageButton buttonMenu = view.findViewById(R.id.buttonMenu);
            if (buttonMenu != null) {
                buttonMenu.setOnClickListener(v -> showDeleteOptionsDialog());
            }

            db = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();
            
            // Check if user is authenticated first
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser != null) {
                currentUserId = currentUser.getUid();
                if (getContext() != null) {
                    friendManager = new FriendManager(db, currentUserId);
                }
            } else {
                // Handle unauthenticated user - show error but don't exit the fragment
                Log.e(TAG, "User not authenticated");
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
                }
                // Don't return here - let the fragment continue but disable functionality
            }
            
            // Check if we're in guest mode by checking MainActivity
            if (getActivity() != null && getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity.isGuestMode()) {
                    // In guest mode - show message but don't exit the fragment
                    Log.w(TAG, "Chat feature not available in guest mode");
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Chat feature is not available in guest mode", Toast.LENGTH_LONG).show();
                    }
                    // Don't return here - let the fragment continue but disable functionality
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in initViews", e);
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Error initializing chat: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupRecyclerView() {
        try {
            if (getContext() == null || !isAdded() || getActivity() == null || getActivity().isFinishing()) {
                Log.e(TAG, "Context is null, fragment not added, or activity is finishing in setupRecyclerView");
                return;
            }
            
            messageList = new ArrayList<>();
            messageAdapter = new ChatMessageAdapter(messageList, currentUserId);
            if (recyclerViewMessages != null) {
                recyclerViewMessages.setLayoutManager(new LinearLayoutManager(getContext()));
                recyclerViewMessages.setAdapter(messageAdapter);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in setupRecyclerView", e);
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Error setting up chat display: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupClickListeners() {
        if (buttonSend != null) {
            buttonSend.setOnClickListener(v -> sendMessage());
        }
        
        if (editTextMessage != null) {
            editTextMessage.setOnEditorActionListener((v, actionId, event) -> {
                sendMessage();
                return true;
            });
        }
    }

    private void checkMutualFollowStatus() {
        Log.d(TAG, "checkMutualFollowStatus called");
        try {
            if (getContext() == null || !isAdded() || getActivity() == null || getActivity().isFinishing()) {
                Log.e(TAG, "Context is null, fragment not added, or activity is finishing in checkMutualFollowStatus");
                return;
            }
            
            // Check if user is authenticated before proceeding
            if (auth == null || auth.getCurrentUser() == null) {
                Log.e(TAG, "User not authenticated, cannot check mutual follow status");
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Please log in to use chat", Toast.LENGTH_LONG).show();
                }
                return;
            }
            
            // Check if in guest mode
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity.isGuestMode()) {
                    Log.w(TAG, "Chat not available in guest mode");
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Chat feature is not available in guest mode", Toast.LENGTH_LONG).show();
                    }
                    return;
                }
            }
            
            if (friendManager != null && otherUserId != null && currentUserId != null) {
                friendManager.areMutualFriends(currentUserId, otherUserId)
                        .addOnSuccessListener(areMutual -> {
                            Log.d(TAG, "Mutual follow check result: " + areMutual);
                            if (getActivity() != null && !getActivity().isFinishing() && areMutual) {
                                // Both users follow each other, allow chat
                                setupChatSession();
                            } else if (getActivity() != null && !getActivity().isFinishing()){
                                // Users don't have mutual follow, show error
                                if (isAdded() && getContext() != null) {
                                    Toast.makeText(getContext(), 
                                        "Cannot start chat: You must follow each other to chat", 
                                        Toast.LENGTH_LONG).show();
                                }
                                // Don't pop back stack - just disable functionality
                                disableChatFunctionality();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error checking follow status", e);
                            if (getActivity() != null && !getActivity().isFinishing()) {
                                if (isAdded() && getContext() != null) {
                                    Toast.makeText(getContext(), 
                                        "Error checking follow status: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            } else {
                Log.e(TAG, "Missing required data for mutual follow check: friendManager=" + (friendManager != null) + 
                     ", otherUserId=" + otherUserId + ", currentUserId=" + currentUserId);
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Cannot verify chat permissions", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in checkMutualFollowStatus", e);
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Error checking chat permissions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupChatSession() {
        // Check if otherUserId is available
        if (otherUserId == null || otherUserId.isEmpty() || currentUserId == null || currentUserId.isEmpty()) {
            if (getContext() != null && isAdded()) {
                Toast.makeText(getContext(), "Cannot start chat: recipient not specified", Toast.LENGTH_LONG).show();
            }
            return;
        }
        
        // Prevent chat with self
        if (currentUserId.equals(otherUserId)) {
            if (getContext() != null && isAdded()) {
                Toast.makeText(getContext(), "Cannot chat with yourself", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        
        // Create chat ID based on user IDs (ensure consistent ordering)
        if (currentUserId.compareTo(otherUserId) < 0) {
            chatId = currentUserId + "_" + otherUserId;
        } else {
            chatId = otherUserId + "_" + currentUserId;
        }

        loadExistingMessages();
        startListeningForNewMessages();
        updateOnlineStatus();
    }

    private void loadExistingMessages() {
        try {
            if (chatId == null || chatId.isEmpty() || db == null) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Cannot load messages: chat ID not available", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            
            db.collection("chats").document(chatId).collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .limit(50) // Limit to last 50 messages initially
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        try {
                            if (getActivity() != null && !getActivity().isFinishing() && 
                                queryDocumentSnapshots != null && messageList != null && messageAdapter != null && isAdded()) {
                                
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        try {
                                            if (messageList != null && getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                                                messageList.clear();
                                                for (DocumentSnapshot document : queryDocumentSnapshots) {
                                                    ChatMessage message = document.toObject(ChatMessage.class);
                                                    if (message != null) {
                                                        message.setMessageId(document.getId());
                                                        // Ensure all required fields are present
                                                        if (message.getText() == null) message.setText("");
                                                        if (message.getSenderId() == null) message.setSenderId("");
                                                        if (message.getReceiverId() == null) message.setReceiverId("");
                                                        if (message.getTimestamp() == null) message.setTimestamp(Timestamp.now());
                                                        if (message.getMessageStatus() == null) message.setMessageStatus("sent");
                                                        
                                                        messageList.add(message);
                                                    }
                                                }
                                                if (messageAdapter != null && getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                                                    messageAdapter.notifyDataSetChanged();
                                                }
                                                if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                                                    scrollToBottom();
                                                }
                                            }
                                        } catch (Exception uiException) {
                                            Log.e(TAG, "Error updating UI in loadExistingMessages", uiException);
                                        }
                                    });
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing message snapshots", e);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load messages", e);
                        if (getContext() != null && getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                            getActivity().runOnUiThread(() -> {
                                try {
                                    if (getContext() != null && !getActivity().isFinishing() && isAdded()) {
                                        Toast.makeText(getContext(), "Failed to load messages: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception toastException) {
                                    Log.e(TAG, "Error showing toast for message load failure", toastException);
                                }
                            });
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in loadExistingMessages", e);
        }
    }

    private void startListeningForNewMessages() {
        try {
            if (chatId == null || chatId.isEmpty() || db == null) {
                return;
            }
            
            messagesListener = db.collection("chats").document(chatId).collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            Log.e(TAG, "Error listening for messages: " + error.getMessage());
                            return;
                        }

                        try {
                            if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                                getActivity().runOnUiThread(() -> {
                                    try {
                                        if (!getActivity().isFinishing() && value != null && messageList != null && messageAdapter != null && isAdded()) {
                                            for (DocumentChange dc : value.getDocumentChanges()) {
                                                switch (dc.getType()) {
                                                    case ADDED:
                                                        ChatMessage newMessage = dc.getDocument().toObject(ChatMessage.class);
                                                        if (newMessage != null) {
                                                            newMessage.setMessageId(dc.getDocument().getId());
                                                            // Ensure all required fields are present
                                                            if (newMessage.getText() == null) newMessage.setText("");
                                                            if (newMessage.getSenderId() == null) newMessage.setSenderId("");
                                                            if (newMessage.getReceiverId() == null) newMessage.setReceiverId("");
                                                            if (newMessage.getTimestamp() == null) newMessage.setTimestamp(Timestamp.now());
                                                            if (newMessage.getMessageStatus() == null) newMessage.setMessageStatus("sent");
                                                            
                                                            // Check if message already exists to avoid duplicates
                                                            boolean exists = false;
                                                            if (messageList != null) {
                                                                for (ChatMessage msg : messageList) {
                                                                    if (msg.getMessageId() != null && newMessage.getMessageId() != null && 
                                                                        msg.getMessageId().equals(newMessage.getMessageId())) {
                                                                        exists = true;
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                            if (!exists) {
                                                                if (messageList != null) {
                                                                    messageList.add(newMessage);
                                                                    if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                                                                        messageAdapter.notifyItemInserted(messageList.size() - 1);
                                                                        scrollToBottom();
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        break;
                                                    case MODIFIED:
                                                        // Handle message status updates (delivered/read)
                                                        ChatMessage updatedMessage = dc.getDocument().toObject(ChatMessage.class);
                                                        if (updatedMessage != null) {
                                                            updatedMessage.setMessageId(dc.getDocument().getId());
                                                            if (getActivity() != null && !getActivity().isFinishing() && messageAdapter != null && isAdded()) {
                                                                messageAdapter.updateMessageStatus(updatedMessage.getMessageId(), updatedMessage.getMessageStatus());
                                                            }
                                                        }
                                                        break;
                                                    case REMOVED:
                                                        // Handle message deletion if needed
                                                        if (messageList != null) {
                                                            String docId = dc.getDocument().getId();
                                                            for (int i = 0; i < messageList.size(); i++) {
                                                                ChatMessage msg = messageList.get(i);
                                                                if (msg != null && msg.getMessageId() != null && msg.getMessageId().equals(docId)) {
                                                                    messageList.remove(i);
                                                                    if (messageAdapter != null) {
                                                                        messageAdapter.notifyItemRemoved(i);
                                                                    }
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                        break;
                                                }
                                            }
                                        }
                                    } catch (Exception uiException) {
                                        Log.e(TAG, "Error updating UI in message listener", uiException);
                                    }
                                });
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error in message listener thread execution", e);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in startListeningForNewMessages", e);
        }
    }

    private void sendMessage() {
        try {
            // Verify all required fields are available
            if (editTextMessage == null || editTextMessage.getText() == null || db == null || getContext() == null) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Cannot send message: input field not ready", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            
            String messageText = editTextMessage.getText().toString().trim();
            if (messageText.isEmpty()) {
                return;
            }
            
            // Check if user is still authenticated
            if (auth == null || auth.getCurrentUser() == null || currentUserId == null || chatId == null) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "User not authenticated. Please log in again.", Toast.LENGTH_LONG).show();
                }
                // Navigate to login
                if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                    getActivity().startActivity(new Intent(getActivity(), LoginActivity.class));
                    getActivity().finish();
                }
                return;
            }
            
            if (otherUserId == null || otherUserId.isEmpty()) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Cannot send message: recipient not specified", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            String messageId = UUID.randomUUID().toString();
            Timestamp timestamp = Timestamp.now();

            ChatMessage message = new ChatMessage();
            message.setMessageId(messageId);
            message.setSenderId(currentUserId);
            message.setReceiverId(otherUserId);
            message.setText(messageText);
            message.setTimestamp(timestamp);
            message.setMessageStatus("sending"); // Initial status

            // Add to local list immediately
            if (messageList != null && getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                messageList.add(message);
                if (messageAdapter != null && getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                    messageAdapter.notifyItemInserted(messageList.size() - 1);
                    scrollToBottom();
                }
            }

            // Prepare message data
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("senderId", currentUserId);
            messageData.put("receiverId", otherUserId);
            messageData.put("text", messageText);
            messageData.put("timestamp", timestamp);
            messageData.put("messageStatus", "sent");

            // First, ensure the chat exists
            db.collection("chats").document(chatId).set(createChatMetadata())
                    .addOnSuccessListener(aVoid -> {
                        if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                            // Then send the message
                            db.collection("chats").document(chatId).collection("messages")
                                    .document(messageId)
                                    .set(messageData)
                                    .addOnSuccessListener(aVoid1 -> {
                                        if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                                            // Update message status to sent
                                            if (messageList != null && messageAdapter != null) {
                                                // Find the message in the list and update its status
                                                for (int i = 0; i < messageList.size(); i++) {
                                                    ChatMessage msg = messageList.get(i);
                                                    if (msg != null && msg.getMessageId() != null && messageId.equals(msg.getMessageId())) {
                                                        msg.setMessageStatus("sent");
                                                        messageAdapter.updateMessageStatus(messageId, "sent");
                                                        break;
                                                    }
                                                }
                                            }
                                            if (editTextMessage != null && getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                                                editTextMessage.setText("");
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                                            // Update message status to failed
                                            if (messageList != null && messageAdapter != null) {
                                                for (int i = 0; i < messageList.size(); i++) {
                                                    ChatMessage msg = messageList.get(i);
                                                    if (msg != null && msg.getMessageId() != null && messageId.equals(msg.getMessageId())) {
                                                        msg.setMessageStatus("failed");
                                                        messageAdapter.updateMessageStatus(messageId, "failed");
                                                        break;
                                                    }
                                                }
                                            }
                                            if (isAdded() && getContext() != null) {
                                                Toast.makeText(getContext(), "Failed to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to create chat session", e);
                        if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                            // Update message status to failed
                            if (messageList != null && messageAdapter != null) {
                                for (int i = 0; i < messageList.size(); i++) {
                                    ChatMessage msg = messageList.get(i);
                                    if (msg != null && msg.getMessageId() != null && msg.getMessageId().equals(messageId)) {
                                        msg.setMessageStatus("failed");
                                        messageAdapter.updateMessageStatus(messageId, "failed");
                                        break;
                                    }
                                }
                            }
                            if (isAdded() && getContext() != null) {
                                Toast.makeText(getContext(), "Failed to create chat session: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in sendMessage", e);
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Error sending message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

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
        chatData.put("lastUpdated", FieldValue.serverTimestamp());
        return chatData;
    }

    private void scrollToBottom() {
        if (messageList != null && messageList.size() > 0 && recyclerViewMessages != null) {
            recyclerViewMessages.smoothScrollToPosition(messageList.size() - 1);
        }
    }
    
    private void disableChatFunctionality() {
        Log.d(TAG, "Disabling chat functionality");
        if (editTextMessage != null) {
            editTextMessage.setEnabled(false);
            editTextMessage.setHint("Cannot chat: Follow required");
        }
        if (buttonSend != null) {
            buttonSend.setEnabled(false);
        }
        if (textViewOnlineStatus != null) {
            textViewOnlineStatus.setText("Chat unavailable");
        }
    }

    private void updateOnlineStatus() {
        try {
            // Check if otherUserId is available
            if (otherUserId == null || otherUserId.isEmpty() || db == null || textViewOnlineStatus == null) {
                if (textViewOnlineStatus != null && isAdded() && getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        if (textViewOnlineStatus != null) {
                            textViewOnlineStatus.setText("Recipient not specified");
                        }
                    });
                }
                return;
            }
            
            // Listen for other user's online status
            db.collection("users").document(otherUserId)
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (error != null) {
                            // Log the error for debugging
                            Log.e(TAG, "Error getting user status: " + error.getMessage());
                            if (textViewOnlineStatus != null && isAdded() && getActivity() != null && !getActivity().isFinishing()) {
                                getActivity().runOnUiThread(() -> {
                                    try {
                                        textViewOnlineStatus.setText("Error retrieving status");
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error updating text view", e);
                                    }
                                });
                            }
                            return;
                        }
                        
                        try {
                            if (getActivity() != null && !getActivity().isFinishing() && isAdded() &&
                                documentSnapshot != null && documentSnapshot.exists()) {
                                Boolean isOnline = documentSnapshot.getBoolean("isOnline");
                                Object lastSeenObj = documentSnapshot.get("lastSeen");
                                Long lastSeen = null;
                                
                                if (lastSeenObj instanceof Long) {
                                    lastSeen = (Long) lastSeenObj;
                                } else if (lastSeenObj instanceof com.google.firebase.Timestamp) {
                                    com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) lastSeenObj;
                                    lastSeen = timestamp.getSeconds() * 1000; // Convert to milliseconds
                                } else if (lastSeenObj instanceof java.util.Date) {
                                    java.util.Date date = (java.util.Date) lastSeenObj;
                                    lastSeen = date.getTime();
                                }
                                
                                // Handle potential null values
                                if (isOnline != null && isOnline) {
                                    if (textViewOnlineStatus != null && isAdded() && getActivity() != null && !getActivity().isFinishing()) {
                                        getActivity().runOnUiThread(() -> {
                                            try {
                                                if (textViewOnlineStatus != null && !getActivity().isFinishing() && isAdded()) {
                                                    textViewOnlineStatus.setText("Online");
                                                }
                                            } catch (Exception e) {
                                                Log.e(TAG, "Error updating online status text", e);
                                            }
                                        });
                                    }
                                } else if (lastSeen != null) {
                                    // Convert timestamp to readable format
                                    try {
                                        java.util.Date lastSeenDate = new java.util.Date(lastSeen);
                                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault());
                                        if (textViewOnlineStatus != null && isAdded() && getActivity() != null && !getActivity().isFinishing()) {
                                            getActivity().runOnUiThread(() -> {
                                                try {
                                                    if (textViewOnlineStatus != null && !getActivity().isFinishing() && isAdded()) {
                                                        textViewOnlineStatus.setText("Last seen " + sdf.format(lastSeenDate));
                                                    }
                                                } catch (Exception e) {
                                                    Log.e(TAG, "Error updating last seen text", e);
                                                }
                                            });
                                        }
                                    } catch (Exception e) {
                                        // Handle date formatting error
                                        Log.e(TAG, "Error formatting date", e);
                                        if (textViewOnlineStatus != null && isAdded() && getActivity() != null && !getActivity().isFinishing()) {
                                            getActivity().runOnUiThread(() -> {
                                                try {
                                                    if (textViewOnlineStatus != null && !getActivity().isFinishing() && isAdded()) {
                                                        textViewOnlineStatus.setText("Offline");
                                                    }
                                                } catch (Exception innerException) {
                                                    Log.e(TAG, "Error updating offline text", innerException);
                                                }
                                            });
                                        }
                                    }
                                } else {
                                    if (textViewOnlineStatus != null && isAdded() && getActivity() != null && !getActivity().isFinishing()) {
                                        getActivity().runOnUiThread(() -> {
                                            try {
                                                if (textViewOnlineStatus != null && !getActivity().isFinishing() && isAdded()) {
                                                    textViewOnlineStatus.setText("Offline");
                                                }
                                            } catch (Exception e) {
                                                Log.e(TAG, "Error updating offline status text", e);
                                            }
                                        });
                                    }
                                }
                            } else {
                                if (textViewOnlineStatus != null && isAdded() && getActivity() != null && !getActivity().isFinishing()) {
                                    getActivity().runOnUiThread(() -> {
                                        try {
                                            if (textViewOnlineStatus != null && !getActivity().isFinishing() && isAdded()) {
                                                textViewOnlineStatus.setText("Offline");
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error updating offline status fallback text", e);
                                        }
                                    });
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing snapshot in updateOnlineStatus", e);
                        }
                    });
            
            // Update current user's online status
            updateUserOnlineStatus(true);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in updateOnlineStatus", e);
        }
    }
    
    private void updateUserOnlineStatus(boolean isOnline) {
        if (currentUserId == null || currentUserId.isEmpty() || db == null) {
            return;
        }
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("isOnline", isOnline);
        updates.put("lastSeen", Timestamp.now()); // Always update last seen when status changes
        updates.put("online", isOnline); // Also update the 'online' field for compatibility
        
        db.collection("users").document(currentUserId)
                .update(updates)
                .addOnFailureListener(e -> {
                    // Handle error silently
                    Log.e(TAG, "Error updating user online status: " + e.getMessage());
                });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        
        try {
            // Check if user is still authenticated
            if (auth != null && auth.getCurrentUser() == null) {
                // User has been logged out, redirect to login
                Log.e(TAG, "User not authenticated in onResume");
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
                }
                if (getActivity() != null && isAdded()) {
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    // Only finish if this is not part of a fragment transaction
                    if (getActivity() != null && getActivity().getClass().getSimpleName().equals("MainActivity")) {
                        getActivity().finish();
                    }
                }
                return;
            }
            
            if (auth != null && auth.getCurrentUser() != null && db != null && currentUserId != null) {
                updateUserOnlineStatus(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
    }
    
    @Override
    public void onPause() {
        Log.d(TAG, "onPause called");
        super.onPause();
        try {
            if (auth != null && auth.getCurrentUser() != null && db != null && currentUserId != null) {
                updateUserOnlineStatus(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onPause", e);
        }
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView called");
        try {
            super.onDestroyView();
            if (messagesListener != null) {
                messagesListener.remove();
            }
            // Update current user's online status to offline when leaving chat
            if (auth != null && auth.getCurrentUser() != null && db != null && currentUserId != null) {
                updateUserOnlineStatus(false);
            }
            // Clean up FriendManager resources
            if (friendManager != null) {
                friendManager.cleanup();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroyView", e);
        }
    }

    private void showDeleteOptionsDialog() {
        if (getContext() == null || !isAdded()) return;
        
        String[] options = {"Delete Chat", "Delete Account", "Cancel"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Choose Action")
               .setItems(options, (dialog, which) -> {
                   if (getActivity() == null || getActivity().isFinishing() || !isAdded()) {
                       return;
                   }
                   
                   switch (which) {
                       case 0:
                           confirmDeleteChat();
                           break;
                       case 1:
                           confirmDeleteAccount();
                           break;
                       case 2:
                           dialog.dismiss();
                           break;
                   }
               })
               .show();
    }

    private void confirmDeleteChat() {
        if (getContext() == null || !isAdded()) return;
        
        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Delete")
                .setMessage("Delete this chat? All messages will be removed.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                        deleteChatConversation();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteChatConversation() {
        if (chatId == null || chatId.isEmpty()) {
            if (getContext() != null && isAdded()) {
                Toast.makeText(getContext(), "Cannot delete chat: invalid chat ID", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Delete all messages in the chat first
        db.collection("chats").document(chatId).collection("messages")
                .get()
                .addOnSuccessListener(messagesSnapshot -> {
                    List<Task<Void>> deleteTasks = new ArrayList<>();
                    
                    // Create delete tasks for all messages
                    for (DocumentSnapshot messageDoc : messagesSnapshot) {
                        deleteTasks.add(messageDoc.getReference().delete());
                    }
                    
                    // Execute all delete tasks
                    Tasks.whenAll(deleteTasks)
                            .addOnSuccessListener(aVoid -> {
                                // Now delete the chat metadata document
                                db.collection("chats").document(chatId).delete()
                                        .addOnSuccessListener(aVoid2 -> {
                                            if (getContext() != null && isAdded()) {
                                                Toast.makeText(getContext(), "Chat deleted", Toast.LENGTH_SHORT).show();
                                            }
                                            
                                            // Navigate back to chat list or home
                                            if (getParentFragmentManager() != null && isAdded()) {
                                                getParentFragmentManager().popBackStack();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            if (getContext() != null && isAdded()) {
                                                Toast.makeText(getContext(), "Chat partially deleted", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            })
                            .addOnFailureListener(e -> {
                                if (getContext() != null && isAdded()) {
                                    Toast.makeText(getContext(), "Failed to delete messages: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null && isAdded()) {
                        Toast.makeText(getContext(), "Failed to delete chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmDeleteAccount() {
        if (getContext() == null || !isAdded()) return;
        
        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Delete")
                .setMessage("Delete your account? All data will be lost permanently.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                        deleteAccount();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            if (getContext() != null && isAdded()) {
                Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Confirm this is the current user's account being deleted
        String currentUserId = currentUser.getUid();
        if (this.currentUserId == null || !this.currentUserId.equals(currentUserId)) {
            if (getContext() != null && isAdded()) {
                Toast.makeText(getContext(), "Cannot delete account: authentication mismatch", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // First, delete user data from Firestore
        deleteUserFirestoreData(currentUserId)
                .addOnSuccessListener(aVoid -> {
                    // Then delete the Firebase Auth account
                    currentUser.delete()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    if (getContext() != null && isAdded()) {
                                        Toast.makeText(getContext(), "Account deleted", Toast.LENGTH_SHORT).show();
                                    }
                                    
                                    // Navigate to login screen
                                    if (getActivity() != null && isAdded()) {
                                        startActivity(new Intent(getActivity(), LoginActivity.class));
                                        getActivity().finish();
                                    }
                                } else {
                                    if (getContext() != null && isAdded()) {
                                        Toast.makeText(getContext(), "Account deleted locally", Toast.LENGTH_SHORT).show();
                                    }
                                    // Still navigate to login since data is gone
                                    if (getActivity() != null && isAdded()) {
                                        startActivity(new Intent(getActivity(), LoginActivity.class));
                                        getActivity().finish();
                                    }
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null && isAdded()) {
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private Task<Void> deleteUserFirestoreData(String userId) {
        // First, clean up friend relationships
        Task<Void> cleanupTask = friendManager != null ? 
            friendManager.cleanupUserRelationships(userId) : 
            Tasks.forResult(null);

        return cleanupTask.continueWithTask(task -> {
            // Then delete user document
            Task<Void> userDeleteTask = db.collection("users").document(userId).delete();

            // Delete all chats involving this user
            return userDeleteTask.continueWithTask(userTask -> {
                return db.collection("chats")
                        .whereArrayContains("participants", userId)
                        .get()
                        .continueWithTask(chatsTask -> {
                            if (chatsTask.isSuccessful()) {
                                List<Task<Void>> deleteTasks = new ArrayList<>();
                                
                                for (QueryDocumentSnapshot document : chatsTask.getResult()) {
                                    deleteTasks.add(document.getReference().delete());
                                }
                                
                                return Tasks.whenAll(deleteTasks);
                            } else {
                                throw chatsTask.getException();
                            }
                        });
            });
        });
    }
}