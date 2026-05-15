package com.example.kitchenbrain;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.kitchenbrain.state.ChatStateManager;
import com.example.kitchenbrain.engine.MessageLifecycleEngine;
import com.example.kitchenbrain.engine.ReadReceiptEngine;
import com.example.kitchenbrain.engine.OfflineMessageQueue;
import com.example.kitchenbrain.engine.TypingIndicatorEngine;
import com.example.kitchenbrain.coordinator.ChatRealtimeCoordinator;
import com.example.kitchenbrain.provider.ChatIdProvider;
import com.example.kitchenbrain.repository.ChatRepository;
import com.example.kitchenbrain.model.ChatMessage;
import com.example.kitchenbrain.adapter.ZeroCrashChatAdapter;
import com.example.kitchenbrain.adapter.SwipeChatCallback;
import com.example.kitchenbrain.utils.CloudinaryHelper;

import java.io.File;
import java.util.Locale;
import java.util.UUID;

public class ChatFragment extends Fragment implements SwipeChatCallback.SwipeListener {
    
    private static final String TAG = "ChatFragment";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private View buttonSend;
    private View buttonMic;
    private ImageButton buttonBack;
    private ImageButton buttonAttach;
    private TextView textUserName;
    private TextView typingIndicator;

    private View layoutEditMode;
    private TextView textEditingOriginal;
    private ImageButton buttonCancelEdit;
    
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

    // Voice Recording
    private final Object recorderLock = new Object();
    private MediaRecorder recorder = null;
    private String audioFileName = null;
    private long recordingStartTime = 0;
    private volatile boolean isRecording = false;
    private volatile boolean isRecorderStarted = false;

    private ActivityResultLauncher<String> imagePickerLauncher;
    
    private final Handler recordTimerHandler = new Handler(Looper.getMainLooper());
    private final Runnable recordTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRecording) {
                long elapsed = System.currentTimeMillis() - recordingStartTime;
                int seconds = (int) (elapsed / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                if (typingIndicator != null) {
                    typingIndicator.setText(String.format(Locale.getDefault(), "🎤 Recording %02d:%02d", minutes, seconds));
                    typingIndicator.setVisibility(View.VISIBLE);
                }
                recordTimerHandler.postDelayed(this, 500);
            }
        }
    };

    public static ChatFragment newInstance(String otherUserId) {
        return newInstance(otherUserId, null);
    }

    public static ChatFragment newInstance(String otherUserId, String chatRoomId) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString("other_user_id", otherUserId);
        if (chatRoomId != null) {
            args.putString("chat_room_id", chatRoomId);
        }
        fragment.setArguments(args);
        return fragment;
    }

    public String getCurrentChatId() {
        if (chatId != null) return chatId;
        if (getArguments() != null) return getArguments().getString("chat_room_id");
        return null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                uploadImage(uri);
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isActive = true;
        
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, Math.max(imeInsets.bottom, systemBars.bottom));
            return WindowInsetsCompat.CONSUMED;
        });

        Bundle args = getArguments();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (args == null || currentUser == null) {
            handleChatError("Session initialization failed");
            return;
        }
        
        currentUserId = currentUser.getUid();
        otherUserId = args.getString("other_user_id");
        chatId = args.getString("chat_room_id", ChatIdProvider.getChatId(currentUserId, otherUserId));
        
        initRepository();
        initUI(view);              
        initEngines();             
        initReadReceipts();
        
        if (stateManager != null && stateListener != null) {
            stateManager.addStateListener(stateListener);
        }
        setupBackPressedDispatcher();
    }

    private void initRepository() {
        chatRepository = new ChatRepository();
    }
    
    private void initEngines() {
        stateManager = ChatStateManager.getInstance();
        stateListener = messages -> {
            if (!isAdded() || !isActive) return;
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
            @Override
            public void onTypingStarted(String userId, String username) {
                if (typingIndicator != null && isAdded() && !isRecording) {
                    String name = username != null ? username : getString(R.string.user_placeholder);
                    typingIndicator.setText(getString(R.string.typing_indicator_format, name));
                    typingIndicator.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onTypingStopped(String userId) {
                if (typingIndicator != null && !isRecording) typingIndicator.setVisibility(View.GONE);
            }
        });
        typingEngine.listenToTypingEvents(chatId, currentUserId);
        
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
    
    @SuppressLint("ClickableViewAccessibility")
    private void initUI(View view) {
        recyclerViewMessages = view.findViewById(R.id.recyclerViewMessages);
        editTextMessage = view.findViewById(R.id.editTextMessage);
        buttonSend = view.findViewById(R.id.buttonSend);
        buttonMic = view.findViewById(R.id.buttonMic);
        buttonBack = view.findViewById(R.id.buttonBack);
        buttonAttach = view.findViewById(R.id.buttonAttach);
        textUserName = view.findViewById(R.id.textUserName);
        typingIndicator = view.findViewById(R.id.typingIndicator);

        layoutEditMode = view.findViewById(R.id.layoutEditMode);
        textEditingOriginal = view.findViewById(R.id.textEditingOriginal);
        buttonCancelEdit = view.findViewById(R.id.buttonCancelEdit);
        
        zeroCrashAdapter = new ZeroCrashChatAdapter(currentUserId,
            this::onMessageLongClick,
            message -> Log.d(TAG, "Retry: " + message.getMessageId()));
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true); 
        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setAdapter(zeroCrashAdapter);

        // Setup Swipe Actions
        SwipeChatCallback swipeCallback = new SwipeChatCallback(requireContext(), zeroCrashAdapter, currentUserId, this);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewMessages);

        if (typingEngine != null) {
            editTextMessage.addTextChangedListener(typingEngine.createTypingWatcher(chatId, currentUserId));
        }

        editTextMessage.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.toString().trim().length() > 0;
                buttonSend.setVisibility(hasText ? View.VISIBLE : View.GONE);
                buttonMic.setVisibility(hasText ? View.GONE : View.VISIBLE);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        buttonMic.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startRecording();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    stopRecording(event.getAction() == MotionEvent.ACTION_UP);
                    return true;
            }
            return false;
        });
        
        if (buttonSend != null) buttonSend.setOnClickListener(v -> sendMessage());
        if (buttonBack != null) buttonBack.setOnClickListener(v -> requireActivity().onBackPressed());
        if (buttonCancelEdit != null) buttonCancelEdit.setOnClickListener(v -> cancelEditing());
        if (buttonAttach != null) buttonAttach.setOnClickListener(v -> openFilePicker());
        
        if (otherUserId != null) loadUserInfo(otherUserId);
    }

    private void openFilePicker() {
        imagePickerLauncher.launch("image/*");
    }

    private void uploadImage(Uri uri) {
        if (getContext() == null) return;
        Toast.makeText(getContext(), "Uploading image...", Toast.LENGTH_SHORT).show();
        CloudinaryHelper.uploadImage(requireContext(), uri, 
            url -> {
                if (isAdded()) {
                    new Handler(Looper.getMainLooper()).post(() -> sendImageMessage(url));
                }
            },
            error -> {
                if (isAdded()) {
                    new Handler(Looper.getMainLooper()).post(() -> 
                        Toast.makeText(getContext(), "Upload failed: " + error, Toast.LENGTH_SHORT).show());
                }
            },
            null);
    }

    private void sendImageMessage(String imageUrl) {
        if (otherUserId == null) return;

        ChatMessage message = new ChatMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setSenderId(currentUserId);
        message.setReceiverId(otherUserId);
        message.setText("[Image]");
        message.setMessageType("image");
        message.setMediaUrl(imageUrl);
        message.setTimestamp(com.google.firebase.Timestamp.now());
        message.setStatus(ChatMessage.MessageStatus.SENDING);
        
        if (coordinator != null) coordinator.processOptimisticMessage(message, chatId);
        if (messageEngine != null) messageEngine.sendComplexMessage(chatId, message);
        scrollToBottom(true);
    }

    @Override
    public void onEditSwipe(int position) {
        ChatMessage message = zeroCrashAdapter.getMessage(position);
        if (message != null && "text".equals(message.getMessageType())) {
            enterEditMode(message);
        }
    }

    @Override
    public void onDeleteSwipe(int position) {
        ChatMessage message = zeroCrashAdapter.getMessage(position);
        if (message != null) {
            showDeleteConfirmation(message);
        }
    }

    private void enterEditMode(ChatMessage message) {
        editingMessageId = message.getMessageId();
        layoutEditMode.setVisibility(View.VISIBLE);
        textEditingOriginal.setText(message.getText());
        editTextMessage.setText(message.getText());
        editTextMessage.requestFocus();
        // Move cursor to end
        editTextMessage.setSelection(editTextMessage.getText().length());
        
        // Ensure keyboard is visible
        Vibrator v = (Vibrator) getContext().getSystemService(android.content.Context.VIBRATOR_SERVICE);
        if (v != null) v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    private void cancelEditing() {
        editingMessageId = null;
        layoutEditMode.setVisibility(View.GONE);
        editTextMessage.setText("");
    }

    private void showDeleteConfirmation(ChatMessage message) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete message?")
                .setMessage("Are you sure you want to delete this message?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    chatRepository.deleteMessage(chatId, message.getMessageId(), new ChatRepository.FirebaseCallback() {
                        @Override public void onSuccess() {
                            if (isAdded()) Toast.makeText(getContext(), "Message deleted", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onFailure(Exception e) {
                            if (isAdded()) Toast.makeText(getContext(), "Failed to delete", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startRecording() {
        if (isRecording) return;
        
        if (!checkRecordPermission()) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        isRecording = true;
        isRecorderStarted = false;
        updateMicUI(true);
        vibrate();
        
        new Thread(() -> {
            synchronized (recorderLock) {
                try {
                    audioFileName = requireContext().getCacheDir().getAbsolutePath() + "/voice_" + System.currentTimeMillis() + ".m4a";
                    recorder = new MediaRecorder();
                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                    recorder.setAudioEncodingBitRate(128000);
                    recorder.setAudioSamplingRate(44100);
                    recorder.setOutputFile(audioFileName);
                    recorder.prepare();
                    recorder.start();
                    
                    recordingStartTime = System.currentTimeMillis();
                    isRecorderStarted = true;
                    recordTimerHandler.post(recordTimerRunnable);
                    Log.d(TAG, "Recording started: " + audioFileName);
                } catch (Exception e) {
                    Log.e(TAG, "MediaRecorder start failed", e);
                    isRecording = false;
                    isRecorderStarted = false;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        updateMicUI(false);
                        Toast.makeText(getContext(), "Failed to start recording", Toast.LENGTH_SHORT).show();
                    });
                    releaseRecorder();
                }
            }
        }).start();
    }

    private void stopRecording(boolean shouldSend) {
        if (!isRecording) return;
        isRecording = false;
        updateMicUI(false);
        recordTimerHandler.removeCallbacks(recordTimerRunnable);

        new Thread(() -> {
            synchronized (recorderLock) {
                if (recorder == null) return;
                
                long elapsed = System.currentTimeMillis() - recordingStartTime;
                if (!isRecorderStarted || elapsed < 1000) {
                    isRecorderStarted = false;
                    releaseRecorder();
                    cleanupAudioFile(audioFileName);
                    if (shouldSend && isAdded()) {
                        new Handler(Looper.getMainLooper()).post(() -> 
                            Toast.makeText(getContext(), "Hold to record voice message", Toast.LENGTH_SHORT).show());
                    }
                    return;
                }

                try {
                    recorder.stop();
                    File recordedFile = new File(audioFileName);
                    Log.d("VOICE", "size = " + recordedFile.length());
                    
                    if (shouldSend && recordedFile.exists() && recordedFile.length() > 500) {
                        final String filePath = audioFileName;
                        final long finalDuration = elapsed / 1000;
                        new Handler(Looper.getMainLooper()).post(() -> uploadVoiceMessage(filePath, finalDuration));
                    } else {
                        cleanupAudioFile(audioFileName);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "MediaRecorder.stop() failed", e);
                    cleanupAudioFile(audioFileName);
                } finally {
                    isRecorderStarted = false;
                    releaseRecorder();
                }
            }
        }).start();
    }

    private void updateMicUI(final boolean recording) {
        if (buttonMic instanceof FloatingActionButton) {
            FloatingActionButton fab = (FloatingActionButton) buttonMic;
            if (recording) {
                fab.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                startMicPulseAnimation();
            } else {
                fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
                stopMicPulseAnimation();
                if (typingIndicator != null) {
                    typingIndicator.setVisibility(View.GONE);
                }
            }
        }
    }

    private void startMicPulseAnimation() {
        if (!isRecording) return;
        buttonMic.animate()
                .scaleX(1.4f)
                .scaleY(1.4f)
                .alpha(0.7f)
                .setDuration(400)
                .withEndAction(() -> {
                    if (isRecording) {
                        buttonMic.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .alpha(1.0f)
                                .setDuration(400)
                                .withEndAction(this::startMicPulseAnimation)
                                .start();
                    }
                }).start();
    }

    private void stopMicPulseAnimation() {
        buttonMic.animate().cancel();
        buttonMic.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(200).start();
    }

    private void releaseRecorder() {
        if (recorder != null) {
            try {
                recorder.release();
            } catch (Exception e) {
                Log.e(TAG, "MediaRecorder release failed", e);
            }
            recorder = null;
        }
    }

    private void uploadVoiceMessage(String filePath, long duration) {
        File audioFile = new File(filePath);
        if (!audioFile.exists() || audioFile.length() == 0) return;

        Uri fileUri = Uri.fromFile(audioFile);
        CloudinaryHelper.uploadFile(requireContext(), fileUri, "video",
            url -> {
                sendVoiceMessage(url, duration);
                cleanupAudioFile(filePath);
            },
            error -> {
                cleanupAudioFile(filePath);
                if (isAdded()) Toast.makeText(getContext(), "Voice upload failed", Toast.LENGTH_SHORT).show();
            },
            null);
    }

    private void sendVoiceMessage(String audioUrl, long duration) {
        if (otherUserId == null) return;

        ChatMessage message = new ChatMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setSenderId(currentUserId);
        message.setReceiverId(otherUserId);
        message.setText("[Voice Message]");
        message.setMessageType("voice");
        message.setMediaUrl(audioUrl);
        message.setDuration(duration);
        message.setTimestamp(com.google.firebase.Timestamp.now());
        message.setStatus(ChatMessage.MessageStatus.SENDING);
        
        if (coordinator != null) coordinator.processOptimisticMessage(message, chatId);
        if (messageEngine != null) messageEngine.sendComplexMessage(chatId, message);
    }

    private void cleanupAudioFile(String path) {
        if (path != null) { File f = new File(path); if (f.exists()) f.delete(); }
    }

    private void vibrate() {
        if (getContext() == null) return;
        Vibrator v = (Vibrator) getContext().getSystemService(android.content.Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void scrollToBottom(boolean smooth) {
        if (recyclerViewMessages != null && zeroCrashAdapter != null && zeroCrashAdapter.getItemCount() > 0) {
            recyclerViewMessages.post(() -> {
                int lastPos = zeroCrashAdapter.getItemCount() - 1;
                if (smooth) recyclerViewMessages.smoothScrollToPosition(lastPos);
                else recyclerViewMessages.scrollToPosition(lastPos);
            });
        }
    }
    
    private void sendMessage() {
        String text = editTextMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        
        if (editingMessageId != null) {
            chatRepository.editMessage(chatId, editingMessageId, text, new ChatRepository.FirebaseCallback() {
                @Override public void onSuccess() {
                    if (isAdded()) cancelEditing();
                }
                @Override public void onFailure(Exception e) {
                    if (isAdded()) Toast.makeText(getContext(), "Edit failed", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            ChatMessage msg = new ChatMessage();
            msg.setMessageId(UUID.randomUUID().toString());
            msg.setSenderId(currentUserId);
            msg.setReceiverId(otherUserId);
            msg.setText(text);
            msg.setTimestamp(com.google.firebase.Timestamp.now());
            msg.setStatus(ChatMessage.MessageStatus.SENDING);
            if (coordinator != null) coordinator.processOptimisticMessage(msg, chatId);
            if (offlineQueue != null) offlineQueue.sendMessage(text, chatId, currentUserId, otherUserId);
            else if (messageEngine != null) messageEngine.sendMessage(text, chatId, currentUserId, otherUserId);
            editTextMessage.setText("");
            scrollToBottom(true);
        }
    }

    private void setupBackPressedDispatcher() {
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (editingMessageId != null) cancelEditing();
                else if (getParentFragmentManager().getBackStackEntryCount() > 0) getParentFragmentManager().popBackStack();
                else { setEnabled(false); requireActivity().onBackPressed(); }
            }
        });
    }

    private void loadUserInfo(String userId) {
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (isActive && doc.exists()) {
                        String name = doc.getString("username");
                        if (textUserName != null) textUserName.setText(name != null ? name : "User_" + userId.substring(0, 8));
                    }
                });
    }

    private void initReadReceipts() { if (readReceiptEngine != null && chatId != null) readReceiptEngine.listenToReadReceipts(chatId, currentUserId); }
    private void handleChatError(String m) { if(isAdded()) Toast.makeText(getContext(), "⚠️ " + m, Toast.LENGTH_LONG).show(); }
    private boolean checkRecordPermission() { return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED; }
    private void onMessageLongClick(ChatMessage m, int p) { /* Swipe actions replaced long click for common tasks */ }

    @Override public void onStart() { super.onStart(); if (chatId != null && stateManager != null) stateManager.startListening(chatId, chatRepository); }
    @Override public void onStop() { super.onStop(); if (stateManager != null) stateManager.stopListening(); if (isRecording) stopRecording(false); }
    @Override public void onDestroyView() { super.onDestroyView(); isActive = false; if (typingEngine != null) typingEngine.cleanup(); }
}
