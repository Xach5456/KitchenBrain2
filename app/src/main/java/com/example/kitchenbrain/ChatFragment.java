package com.example.kitchenbrain;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.OnBackPressedCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.kitchenbrain.state.ChatStateManager;
import com.example.kitchenbrain.engine.MessageLifecycleEngine;
import com.example.kitchenbrain.engine.ReadReceiptEngine;
import com.example.kitchenbrain.engine.TypingIndicatorEngine;
import com.example.kitchenbrain.engine.OfflineMessageQueue;
import com.example.kitchenbrain.coordinator.ChatRealtimeCoordinator;
import com.example.kitchenbrain.provider.ChatIdProvider;
import com.example.kitchenbrain.repository.ChatRepository;
import com.example.kitchenbrain.model.ChatMessage;
import com.example.kitchenbrain.adapter.ZeroCrashChatAdapter;
import com.example.kitchenbrain.model.DeliveryState;
import android.os.Handler;
import android.os.Looper;
import com.google.firebase.firestore.ListenerRegistration;

public class ChatFragment extends Fragment {
    
    private static final String TAG = "ChatFragment";
    
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private View buttonSend;
    private ImageButton buttonBack;
    private TextView typingIndicator;
    private TextView textUserName;
    private TextView textUserStatus;
    
    private ChatRepository chatRepository;
    private MessageLifecycleEngine messageEngine;
    private ReadReceiptEngine readReceiptEngine;
    private TypingIndicatorEngine typingEngine;
    private OfflineMessageQueue offlineQueue;
    private ChatRealtimeCoordinator coordinator;
    private ChatStateManager stateManager;
    private ZeroCrashChatAdapter zeroCrashAdapter;
    
    private ChatStateManager.StateListener stateListener;
    
    private String currentUserId;
    private String otherUserId;
    private String chatId;
    
    private String editingMessageId = null;
    private volatile boolean isActive = false;

    public static ChatFragment newInstance(String otherUserId) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString("other_user_id", otherUserId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * 🔥 EXPOSED FOR NAVIGATOR: To identify which chat is currently open
     */
    public String getCurrentChatId() {
        return chatId;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isActive = true;
        
        Bundle args = getArguments();
        if (args == null) {
            handleChatError("Chat arguments not found");
            return;
        }
        
        String otherUserIdFromArgs = args.getString("other_user_id");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (otherUserIdFromArgs == null || currentUser == null) {
            handleChatError("User information not available");
            return;
        }
        
        currentUserId = currentUser.getUid();
        otherUserId = otherUserIdFromArgs;
        
        // Use provided roomId if available, otherwise generate it
        String providedRoomId = args.getString("chat_room_id");
        if (providedRoomId != null && !providedRoomId.isEmpty()) {
            chatId = providedRoomId;
        } else {
            chatId = ChatIdProvider.getChatId(currentUserId, otherUserId);
        }
        
        initRepository();
        initUI(view);              
        initEngines();             
        initReadReceipts();
        
        if (stateManager != null && stateListener != null) {
            stateManager.addStateListener(stateListener);
        }
        
        setupBackPressedDispatcher();
    }

    @Override
    public void onStart() {
        super.onStart();
        attachFirebaseListener();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (stateManager != null) {
            stateManager.stopListening();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isActive = false;
        if (stateManager != null && stateListener != null) {
            stateManager.removeStateListener(stateListener);
        }
        zeroCrashAdapter = null;
        recyclerViewMessages = null;
    }

    private void attachFirebaseListener() {
        if (chatId != null && chatRepository != null && stateManager != null) {
            stateManager.startListening(chatId, chatRepository);
        }
    }
    
    private void initRepository() {
        chatRepository = new ChatRepository();
    }
    
    private void initEngines() {
        stateManager = ChatStateManager.getInstance();
        stateListener = messages -> {
            if (!isAdded() || getView() == null || !isActive) return;
            if (zeroCrashAdapter != null) {
                zeroCrashAdapter.renderMessages(messages);
                scrollToBottom(false);
            }
        };

        coordinator = new ChatRealtimeCoordinator();
        coordinator.setChatRepository(chatRepository);
        coordinator.addListener(new ChatRealtimeCoordinator.CoordinatorListener() {
            @Override public void onMessageAdded(ChatMessage m) { if (stateManager != null) stateManager.addMessage(m); }
            @Override public void onMessageUpdated(ChatMessage m) { if (stateManager != null) stateManager.updateMessage(m); }
            @Override public void onMessageRemoved(String id) { if (stateManager != null) stateManager.removeMessage(id); }
            @Override public void onStateChanged(String id, ChatRealtimeCoordinator.ChatState s) {}
            @Override public void onConflictResolved(String id, ChatMessage r) { if (stateManager != null) stateManager.updateMessageStatus(id, r.getStatus()); }
        });
        
        messageEngine = new MessageLifecycleEngine(chatRepository, new MessageLifecycleEngine.MessageLifecycleListener() {
            @Override public void onMessageAdded(ChatMessage m) { if (coordinator != null) coordinator.processIncomingMessage(m, chatId); }
            @Override public void onMessageUpdated(ChatMessage m) { if (coordinator != null) coordinator.processIncomingMessage(m, chatId); }
            @Override public void onMessageFailed(ChatMessage m) { if (coordinator != null) coordinator.processIncomingMessage(m, chatId); }
        });
        
        typingEngine = new TypingIndicatorEngine(chatRepository, new TypingIndicatorEngine.TypingListener() {
            @Override public void onTypingStarted(String id, String name) {
                if (isActive && typingIndicator != null) {
                    typingIndicator.setText(name + " is typing...");
                    typingIndicator.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onTypingStopped(String id) {
                if (isActive && typingIndicator != null) typingIndicator.setVisibility(View.GONE);
            }
        });
        
        readReceiptEngine = new ReadReceiptEngine(chatRepository, new ReadReceiptEngine.ReadReceiptListener() {
            @Override public void onMessageRead(String id) {}
            @Override public void onReadReceiptError(Exception e) {}
        });
        
        offlineQueue = new OfflineMessageQueue(requireContext(), new OfflineMessageQueue.QueueListener() {
            @Override public void onMessageQueued(OfflineMessageQueue.PendingMessage m) {}
            @Override public void onMessageSent(OfflineMessageQueue.PendingMessage m) {}
            @Override public void onSyncStarted(int count) {}
            @Override public void onSyncCompleted(int s, int f) {}
        });
    }
    
    private void initUI(View view) {
        recyclerViewMessages = view.findViewById(R.id.recyclerViewMessages);
        editTextMessage = view.findViewById(R.id.editTextMessage);
        buttonSend = view.findViewById(R.id.buttonSend);
        buttonBack = view.findViewById(R.id.buttonBack);
        typingIndicator = view.findViewById(R.id.typingIndicator);
        textUserName = view.findViewById(R.id.textUserName);
        textUserStatus = view.findViewById(R.id.textUserStatus);
        
        zeroCrashAdapter = new ZeroCrashChatAdapter(currentUserId,
            this::onMessageLongClick,
            message -> Log.d(TAG, "Retry: " + message.getMessageId()));
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true); // 🔥 Push messages from bottom
        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setAdapter(zeroCrashAdapter);

        // 🔥 KEYBOARD FIX: Scroll to bottom when keyboard appears
        recyclerViewMessages.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) { // If view height shrunk (keyboard popped up)
                scrollToBottom(false);
            }
        });

        // 🔥 SCROLL ON FOCUS: Manual scroll when input is touched
        editTextMessage.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollToBottom(true);
            }
        });
        
        if (buttonSend != null) buttonSend.setOnClickListener(v -> sendMessage());
        if (buttonBack != null) buttonBack.setOnClickListener(v -> requireActivity().onBackPressed());
        
        editTextMessage.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (buttonSend != null) buttonSend.setEnabled(s.toString().trim().length() > 0);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });
        
        if (otherUserId != null) loadUserInfo(otherUserId);
    }

    private void scrollToBottom(boolean smooth) {
        if (recyclerViewMessages != null && zeroCrashAdapter != null && zeroCrashAdapter.getItemCount() > 0) {
            recyclerViewMessages.postDelayed(() -> {
                if (recyclerViewMessages != null) {
                    if (smooth) {
                        recyclerViewMessages.smoothScrollToPosition(zeroCrashAdapter.getItemCount() - 1);
                    } else {
                        recyclerViewMessages.scrollToPosition(zeroCrashAdapter.getItemCount() - 1);
                    }
                }
            }, 100);
        }
    }
    
    private void onMessageLongClick(ChatMessage message, int position) {
        if (!message.getSenderId().equals(currentUserId)) return;
        String[] options = {"Edit", "Delete", "Cancel"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Message options")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) startEditMessage(message);
                    else if (which == 1) deleteMessage(message);
                })
                .show();
    }

    private void startEditMessage(ChatMessage message) {
        editTextMessage.setText(message.getText());
        editTextMessage.setSelection(message.getText().length());
        editTextMessage.requestFocus();
        editingMessageId = message.getMessageId();
    }

    private void deleteMessage(ChatMessage message) {
        chatRepository.deleteMessage(chatId, message.getMessageId(), new ChatRepository.FirebaseCallback() {
            @Override public void onSuccess() {}
            @Override public void onFailure(Exception e) {}
        });
    }

    private void initReadReceipts() {
        if (readReceiptEngine != null && chatId != null) {
            readReceiptEngine.listenToReadReceipts(chatId, currentUserId);
        }
    }
    
    private void sendMessage() {
        String text = editTextMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        
        if (editingMessageId != null) {
            chatRepository.editMessage(chatId, editingMessageId, text, new ChatRepository.FirebaseCallback() {
                @Override public void onSuccess() {}
                @Override public void onFailure(Exception e) {}
            });
            editingMessageId = null;
        } else {
            ChatMessage optimisticMessage = new ChatMessage();
            optimisticMessage.setMessageId(java.util.UUID.randomUUID().toString());
            optimisticMessage.setSenderId(currentUserId);
            optimisticMessage.setReceiverId(otherUserId);
            optimisticMessage.setText(text);
            optimisticMessage.setTimestamp(com.google.firebase.Timestamp.now());
            optimisticMessage.setStatus(ChatMessage.MessageStatus.SENDING);
            optimisticMessage.setDeliveryState(DeliveryState.SENDING);
            
            if (coordinator != null) coordinator.processOptimisticMessage(optimisticMessage, chatId);
            if (offlineQueue != null) offlineQueue.sendMessage(text, chatId, currentUserId);
            else if (messageEngine != null) messageEngine.sendMessage(text, chatId, currentUserId);
        }
        editTextMessage.setText("");
        scrollToBottom(true);
    }

    private void setupBackPressedDispatcher() {
        requireActivity().getOnBackPressedDispatcher()
            .addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                @Override public void handleOnBackPressed() {
                    if (getParentFragmentManager().getBackStackEntryCount() > 0) getParentFragmentManager().popBackStack();
                    else { setEnabled(false); requireActivity().onBackPressed(); }
                }
            });
    }
    
    private void handleChatError(String errorMessage) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!isAdded() || getContext() == null) return;
            android.widget.Toast.makeText(getContext(), "⚠️ " + errorMessage, android.widget.Toast.LENGTH_LONG).show();
        });
    }
    
    private void loadUserInfo(String userId) {
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || getView() == null || !isActive) return;
                    if (doc.exists()) {
                        String username = doc.getString("username");
                        if (textUserName != null) textUserName.setText(username != null ? username : "User_" + userId.substring(0, 8));
                    }
                });
    }
}
