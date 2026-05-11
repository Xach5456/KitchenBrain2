package com.example.kitchenbrain;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;
import com.example.kitchenbrain.provider.ChatIdProvider;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ChatListFragment extends Fragment {

    private static final String TAG = "ChatListFragment";
    private RecyclerView recyclerViewChatList;
    private ChatListAdapter chatListAdapter;
    private List<User> userList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private String currentUserId;
    private FriendManager friendManager;
    private Context context;

    // 🔥 CRITICAL: Track Fragment lifecycle for async operations
    private volatile boolean isViewDestroyed = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d("CHAT_DEBUG", "=== ChatListFragment opened ===");
        // 🔥 RESET FLAG: When returning from backstack, the fragment instance is reused
        isViewDestroyed = false;

        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        initViews(view);
        // Only continue if initialization was successful
        if (currentUserId != null && friendManager != null) {
            setupRecyclerView();
            loadUsers();
        }

        return view;
    }

    private void initViews(View view) {
        recyclerViewChatList = view.findViewById(R.id.recyclerViewChatList);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        FirebaseUser currentUser = auth.getCurrentUser();
        
        // Check if we're in guest mode by checking MainActivity
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity.isGuestMode()) {
                // In guest mode - show message and handle accordingly
                Toast.makeText(getContext(), "Chat feature is not available in guest mode", Toast.LENGTH_LONG).show();
                // Redirect back or handle appropriately
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
                return;
            }
        }
        
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            friendManager = new FriendManager(db, currentUserId);
        } else {
            // Handle unauthenticated user
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            // Redirect to login
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), LoginActivity.class));
                getActivity().finish();
            }
            return;
        }
    }

    private void setupRecyclerView() {
        userList = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(userList, user -> {
            // 🔥 Navigate to chat with centralized chatId
            Log.d("CHAT_DEBUG", "=== ChatListFragment: User clicked - " + user.getUserId() + " ===");
            
            try {
                // Use ChatIdProvider - single source of truth
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                String chatId = ChatIdProvider.getChatId(currentUserId, user.getUserId());
                
                ChatFragment chatFragment = new ChatFragment();
                Bundle args = new Bundle();
                args.putString("other_user_id", user.getUserId());
                args.putString("chat_room_id", chatId);
                chatFragment.setArguments(args);
                
                Log.d("CHAT_DEBUG", "🔑 Bundle created with ChatIdProvider:");
                Log.d("CHAT_DEBUG", "  - other_user_id: " + user.getUserId());
                Log.d("CHAT_DEBUG", "  - chat_room_id: " + chatId);
                
                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, chatFragment)
                            .addToBackStack(null)
                            .commit();
                } else {
                    Log.e("CHAT_DEBUG", "❌ ParentFragmentManager is null!");
                }
                
            } catch (IllegalArgumentException e) {
                Log.e("CHAT_DEBUG", "❌ Failed to generate chatId: " + e.getMessage());
                Toast.makeText(getContext(), "Error: Cannot create chat", Toast.LENGTH_SHORT).show();
            }
        });

        recyclerViewChatList.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewChatList.setAdapter(chatListAdapter);
    }

    private void loadUsers() {
        // First, get the current user's following list
        if (db == null || currentUserId == null) {
            Toast.makeText(getContext(), "Database or user not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(currentUserDoc -> {
                    if (currentUserDoc.exists()) {
                        User currentUser = currentUserDoc.toObject(User.class);
                        List<String> followingList = currentUser != null ? currentUser.getFollowing() : new ArrayList<>();
                        
                        if (followingList != null && !followingList.isEmpty()) {
                            // Now, for each person the current user is following, check if they follow back
                            List<User> tempUserList = new ArrayList<>();
                            int[] processedCount = {0}; // Use array to allow modification in lambda
                            
                            for (String followedUserId : followingList) {
                                // Check if the followed user also follows the current user back
                                db.collection("users").document(followedUserId)
                                        .get()
                                        .addOnSuccessListener(followedUserDoc -> {
                                            if (followedUserDoc.exists()) {
                                                User followedUser = followedUserDoc.toObject(User.class);
                                                if (followedUser != null && followedUser.getFollowing() != null) {
                                                    // Check if the followed user follows the current user back
                                                    if (followedUser.getFollowing().contains(currentUserId)) {
                                                        // This is a mutual follow relationship
                                                        followedUser.setUserId(followedUserId);
                                                        synchronized (tempUserList) {
                                                            tempUserList.add(followedUser);
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            // Check if all requests have been processed
                                            processedCount[0]++;
                                            if (processedCount[0] >= followingList.size()) {
                                                // 🔥 CRITICAL FIX: Check Fragment lifecycle before UI update
                                                if (isViewDestroyed || !isAdded() || getActivity() == null) {
                                                    Log.w("ChatListFragment", "❌ Fragment not attached or view destroyed, skipping UI update");
                                                    return;
                                                }
                                                
                                                // Update UI after all requests complete
                                                getActivity().runOnUiThread(() -> {
                                                    // 🔥 DOUBLE CHECK: Fragment still attached?
                                                    if (!isAdded()) {
                                                        Log.w("ChatListFragment", "❌ Fragment detached during UI update");
                                                        return;
                                                    }
                                                    
                                                    userList.clear();
                                                    userList.addAll(tempUserList);
                                                    chatListAdapter.notifyDataSetChanged();
                                                });
                                            }
                                        })
                                        .addOnFailureListener(error -> {
                                            processedCount[0]++;
                                            if (processedCount[0] >= followingList.size()) {
                                                // 🔥 CRITICAL FIX: Check Fragment lifecycle before UI update
                                                if (isViewDestroyed || !isAdded() || getActivity() == null) {
                                                    Log.w("ChatListFragment", "❌ Fragment not attached or view destroyed in failure callback, skipping UI update");
                                                    return;
                                                }
                                                
                                                // Update UI after all requests complete
                                                getActivity().runOnUiThread(() -> {
                                                    // 🔥 DOUBLE CHECK: Fragment still attached?
                                                    if (!isAdded()) {
                                                        Log.w("ChatListFragment", "❌ Fragment detached during failure UI update");
                                                        return;
                                                    }
                                                    
                                                    userList.clear();
                                                    userList.addAll(tempUserList);
                                                    chatListAdapter.notifyDataSetChanged();
                                                });
                                            }
                                        });
                            }
                        } else {
                            // No users being followed, so no mutual friends
                            userList.clear();
                            chatListAdapter.notifyDataSetChanged();
                        }
                    } else {
                        // User document doesn't exist
                        userList.clear();
                        chatListAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(error -> {
                    Toast.makeText(getContext(), "Error loading users: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    // Still need to update UI to show empty state
                    userList.clear();
                    chatListAdapter.notifyDataSetChanged();
                });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d("ChatListFragment", "🧹 ChatListFragment cleaned up");
        
        // 🔥 CRITICAL: Mark view as destroyed for async operations
        isViewDestroyed = true;
        
        // Clean up FriendManager resources
        if (friendManager != null) {
            friendManager.cleanup();
        }
    }

    // Adapter for the chat list
    public static class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder> {
        private List<User> userList;
        private OnUserSelectedListener listener;

        public interface OnUserSelectedListener {
            void onUserSelected(User user);
        }

        public ChatListAdapter(List<User> userList, OnUserSelectedListener listener) {
            this.userList = userList;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ChatListAdapter.ChatListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_list, parent, false);
            return new ChatListViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatListViewHolder holder, int position) {
            User user = userList.get(position);
            holder.textViewUsername.setText(user.getUsername() != null ? user.getUsername() : "Unknown User");
            
            holder.itemView.setOnClickListener(v -> {
                Log.d("CHAT_DEBUG", "CLICK ON CHAT ITEM - User: " + user.getUsername());
                if (listener != null) {
                    listener.onUserSelected(user);
                }
            });
        }

        @Override
        public int getItemCount() {
            return userList != null ? userList.size() : 0;
        }

        static class ChatListViewHolder extends RecyclerView.ViewHolder {
            TextView textViewUsername;

            public ChatListViewHolder(@NonNull View itemView) {
                super(itemView);
                textViewUsername = itemView.findViewById(R.id.textViewUsername);
            }
        }
    }
}