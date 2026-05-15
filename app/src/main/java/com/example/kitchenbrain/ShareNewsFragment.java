package com.example.kitchenbrain;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kitchenbrain.adapter.ShareUserAdapter;
import com.example.kitchenbrain.manager.FollowGraphRepository;
import com.example.kitchenbrain.model.ChatMessage;
import com.example.kitchenbrain.provider.ChatIdProvider;
import com.example.kitchenbrain.repository.ChatRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 🔥 SHARE NEWS FRAGMENT - Reactive Mutual Friends List
 * 
 * Specifically designed to show ONLY mutual followers for sharing recipes/news.
 */
public class ShareNewsFragment extends Fragment {
    private static final String TAG = "ShareNewsFragment";
    
    private String newsTitle, newsUrl;
    private ChatRepository chatRepository;
    private String currentUserId;
    private ShareUserAdapter adapter;
    private RecyclerView recyclerView;
    private View layoutLoading;

    public static ShareNewsFragment newInstance(String title, String url) {
        ShareNewsFragment fragment = new ShareNewsFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("url", url);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_share_news, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (getArguments() != null) {
            newsTitle = getArguments().getString("title");
            newsUrl = getArguments().getString("url");
        }
        
        chatRepository = new ChatRepository();
        currentUserId = FirebaseAuth.getInstance().getUid();

        recyclerView = view.findViewById(R.id.recyclerViewShareUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new ShareUserAdapter(new ArrayList<>(), user -> {
            sendNewsToUser(user);
        });
        recyclerView.setAdapter(adapter);

        observeMutualFriends();
    }

    private void observeMutualFriends() {
        FollowGraphRepository graph = FollowGraphRepository.getInstance();
        
        // 🔥 Reactive UI: Updates as soon as Firestore data changes or is loaded
        graph.getMutualLiveData().observe(getViewLifecycleOwner(), users -> {
            Log.d(TAG, "📥 Mutual friends updated: " + (users != null ? users.size() : 0));
            if (users != null) {
                adapter.setUsers(users);
                
                if (users.isEmpty()) {
                    Log.d(TAG, "ℹ️ No mutual friends found");
                    // Optionally show empty state UI here
                }
            }
        });
    }

    private void sendNewsToUser(User user) {
        if (currentUserId == null || user == null) return;
        
        String chatId = ChatIdProvider.getChatId(currentUserId, user.getUserId());
        
        ChatMessage message = new ChatMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setSenderId(currentUserId);
        message.setReceiverId(user.getUserId());
        message.setText("Check out this recipe I found on KitchenBrain: " + newsTitle + "\n" + newsUrl);
        message.setTimestamp(com.google.firebase.Timestamp.now());
        message.setStatus(ChatMessage.MessageStatus.SENDING);

        chatRepository.sendMessage(chatId, message, new ChatRepository.FirebaseCallback() {
            @Override
            public void onSuccess() {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Shared with " + user.getUsername() + " ✅", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
