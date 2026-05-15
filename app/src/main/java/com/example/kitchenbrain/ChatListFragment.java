package com.example.kitchenbrain;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.kitchenbrain.manager.FollowGraphRepository;
import com.example.kitchenbrain.model.ChatRoom;
import com.example.kitchenbrain.repository.ChatRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 🔥 STRICT MUTUAL-ONLY CHAT LIST
 * Displays ONLY users where a mutual follow relationship exists.
 * Filters out any non-mutual contacts, even if a chat history exists.
 */
public class ChatListFragment extends Fragment {

    private static final String TAG = "ChatListFragment";
    private RecyclerView recyclerViewChatList;
    private EditText editTextSearch;
    private ChatListAdapter chatListAdapter;
    private List<ChatConversation> conversationList = new ArrayList<>();
    private List<ChatConversation> allConversations = new ArrayList<>();
    
    private ChatRepository chatRepository;
    private FirebaseFirestore db;
    private String currentUserId;
    
    private ListenerRegistration chatRoomsListener;
    private ListenerRegistration friendsListener;
    private ListenerRegistration userDocListener;

    private List<ChatRoom> latestRooms = new ArrayList<>();
    
    // Social IDs
    private final Set<String> socialFollowingIds = new HashSet<>();
    private final Set<String> socialFollowerIds = new HashSet<>();
    private final Set<String> friendsCollIds = new HashSet<>();
    private final Set<String> docFollowingIds = new HashSet<>();
    private final Set<String> docFollowerIds = new HashSet<>();
    
    private final Map<String, User> profileCache = new ConcurrentHashMap<>();
    private final Set<String> fetchingIds = Collections.synchronizedSet(new HashSet<>());

    public static class ChatConversation {
        public ChatRoom room;
        public User otherUser;
        public String otherUserId;

        public ChatConversation(ChatRoom room, User otherUser, String currentUserId) {
            this.room = room;
            this.otherUser = otherUser;
            if (otherUser != null) {
                this.otherUserId = otherUser.getUserId();
            } else if (room != null) {
                this.otherUserId = room.getOtherUserId(currentUserId);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);
        initViews(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), LoginActivity.class));
                getActivity().finish();
            }
            return;
        }
        
        currentUserId = currentUser.getUid();
        chatRepository = new ChatRepository();
        db = FirebaseFirestore.getInstance();
        
        setupRecyclerView();
        setupSearch();
        
        // 1. Social Graph Sync
        FollowGraphRepository graph = FollowGraphRepository.getInstance();
        graph.initialize(currentUserId);
        
        graph.getFollowingLiveData().observe(getViewLifecycleOwner(), users -> { 
            if (users != null) {
                socialFollowingIds.clear();
                for (User u : users) { if (u.getUserId() != null) socialFollowingIds.add(u.getUserId()); profileCache.put(u.getUserId(), u); }
                sync();
            }
        });
        graph.getFollowersLiveData().observe(getViewLifecycleOwner(), users -> { 
            if (users != null) {
                socialFollowerIds.clear();
                for (User u : users) { if (u.getUserId() != null) socialFollowerIds.add(u.getUserId()); profileCache.put(u.getUserId(), u); }
                sync();
            }
        });

        // 2. legacy Syncs
        startFriendsSync();
        startUserDocSync();
        startRoomsSync();
    }

    private void initViews(View view) {
        recyclerViewChatList = view.findViewById(R.id.recyclerViewChatList);
        editTextSearch = view.findViewById(R.id.editTextSearch);
        View buttonBack = view.findViewById(R.id.buttonBack);
        if (buttonBack != null) buttonBack.setOnClickListener(v -> { if (getActivity() != null) getActivity().onBackPressed(); });
    }

    private void setupRecyclerView() {
        chatListAdapter = new ChatListAdapter(conversationList, conv -> {
            if (getActivity() instanceof MainActivity && conv.otherUserId != null) {
                ((MainActivity) getActivity()).openChat(conv.otherUserId, conv.otherUser != null ? conv.otherUser.getUsername() : null);
            }
        });
        recyclerViewChatList.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewChatList.setAdapter(chatListAdapter);
    }

    private void setupSearch() {
        if (editTextSearch != null) {
            editTextSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void filter(String query) {
        if (query.isEmpty()) {
            conversationList.clear(); conversationList.addAll(allConversations);
        } else {
            String q = query.toLowerCase().trim();
            conversationList.clear();
            conversationList.addAll(allConversations.stream().filter(c -> c.otherUser != null && (
                (c.otherUser.getUsername() != null && c.otherUser.getUsername().toLowerCase().contains(q)) ||
                (c.otherUser.getNickname() != null && c.otherUser.getNickname().toLowerCase().contains(q))
            )).collect(Collectors.toList()));
        }
        chatListAdapter.notifyDataSetChanged();
    }

    private void startFriendsSync() {
        if (friendsListener != null) friendsListener.remove();
        friendsListener = db.collection("friends").whereEqualTo("userId", currentUserId).addSnapshotListener((snap, e) -> {
            if (snap != null) {
                friendsCollIds.clear();
                for (var doc : snap.getDocuments()) {
                    Friend f = doc.toObject(Friend.class);
                    if (f != null && f.getFriendId() != null) {
                        friendsCollIds.add(f.getFriendId());
                        if (!profileCache.containsKey(f.getFriendId())) {
                            User u = new User(); u.setUserId(f.getFriendId()); u.setUsername(f.getFriendUsername());
                            u.setAvatarUrl(f.getFriendAvatarUrl()); profileCache.put(u.getUserId(), u);
                        }
                    }
                }
                sync();
            }
        });
    }

    private void startUserDocSync() {
        if (userDocListener != null) userDocListener.remove();
        userDocListener = db.collection("users").document(currentUserId).addSnapshotListener((snap, e) -> {
            if (snap != null && snap.exists()) {
                List<String> fing = (List<String>) snap.get("following");
                List<String> fers = (List<String>) snap.get("followers");
                docFollowingIds.clear(); if (fing != null) docFollowingIds.addAll(fing);
                docFollowerIds.clear(); if (fers != null) docFollowerIds.addAll(fers);
                sync();
            }
        });
    }

    private void startRoomsSync() {
        if (chatRoomsListener != null) chatRoomsListener.remove();
        chatRoomsListener = chatRepository.listenChatRooms(currentUserId, new ChatRepository.ChatRoomsListener() {
            @Override public void onChatRooms(List<ChatRoom> rooms) { if (isAdded()) { latestRooms = rooms; sync(); } }
            @Override public void onError(Exception e) { Log.e(TAG, "Sync error", e); }
        });
    }

    private synchronized void sync() {
        if (currentUserId == null) return;
        
        Map<String, ChatRoom> roomMap = new HashMap<>();
        for (ChatRoom r : latestRooms) {
            String oid = r.getOtherUserId(currentUserId);
            if (oid != null) roomMap.put(oid, r);
        }

        // --- STRICT MUTUAL INTERSECTION ---
        Set<String> followingSet = new HashSet<>();
        followingSet.addAll(socialFollowingIds);
        followingSet.addAll(docFollowingIds);
        followingSet.addAll(friendsCollIds);

        Set<String> followersSet = new HashSet<>();
        followersSet.addAll(socialFollowerIds);
        followersSet.addAll(docFollowerIds);

        Set<String> mutualIds = new HashSet<>(followingSet);
        mutualIds.retainAll(followersSet);
        mutualIds.remove(currentUserId);

        List<ChatConversation> merged = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String id : mutualIds) {
            User u = profileCache.get(id);
            ChatRoom r = roomMap.get(id);
            if (r == null) { r = new ChatRoom(); r.setLastMessageText("Start a conversation!"); r.setLastMessageAt(0); }
            if (u == null || u.getUsername() == null || u.getUsername().isEmpty()) missing.add(id);
            merged.add(new ChatConversation(r, u, currentUserId));
        }

        merged.sort((c1, c2) -> Long.compare(c2.room.getLastMessageAt(), c1.room.getLastMessageAt()));
        updateUI(merged);
        for (String id : missing) if (!fetchingIds.contains(id)) fetch(id);
    }

    private void fetch(String id) {
        fetchingIds.add(id);
        db.collection("users").document(id).get().addOnSuccessListener(doc -> {
            fetchingIds.remove(id);
            if (doc.exists()) {
                User u = doc.toObject(User.class);
                if (u != null) { u.setUserId(id); profileCache.put(id, u); if (isAdded()) sync(); }
            }
        }).addOnFailureListener(e -> fetchingIds.remove(id));
    }

    private void updateUI(List<ChatConversation> list) {
        if (getActivity() == null || !isAdded()) return;
        getActivity().runOnUiThread(() -> {
            allConversations.clear(); allConversations.addAll(list);
            filter(editTextSearch != null ? editTextSearch.getText().toString() : "");
        });
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (chatRoomsListener != null) chatRoomsListener.remove();
        if (friendsListener != null) friendsListener.remove();
        if (userDocListener != null) userDocListener.remove();
    }

    public static class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder> {
        private List<ChatConversation> list;
        private OnConversationSelectedListener listener;
        private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());

        public interface OnConversationSelectedListener { void onConversationSelected(ChatConversation conversation); }
        public ChatListAdapter(List<ChatConversation> list, OnConversationSelectedListener listener) { this.list = list; this.listener = listener; }

        @NonNull @Override public ChatListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ChatListViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_list, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull ChatListViewHolder holder, int position) {
            ChatConversation c = list.get(position); User u = c.otherUser; ChatRoom r = c.room;
            if (u == null) {
                holder.name.setText("Loading..."); holder.msg.setText("..."); holder.time.setVisibility(View.GONE);
                holder.avatar.setImageResource(R.drawable.ic_default_avatar);
            } else {
                String n = u.getNickname(); if (n == null || n.isEmpty()) n = u.getUsername();
                if (n == null || n.isEmpty()) n = "User " + u.getUserId().substring(0, Math.min(4, u.getUserId().length()));
                holder.name.setText(n);
                holder.msg.setText(r.getLastMessageText());
                if (r.getLastMessageAt() > 0) {
                    holder.time.setVisibility(View.VISIBLE);
                    Date d = new Date(r.getLastMessageAt());
                    if (new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(d).equals(new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date())))
                        holder.time.setText(timeFormat.format(d)); else holder.time.setText(dateFormat.format(d));
                } else holder.time.setVisibility(View.GONE);
                if (u.getAvatarUrl() != null && !u.getAvatarUrl().isEmpty()) Glide.with(holder.itemView.getContext()).load(u.getAvatarUrl()).placeholder(R.drawable.ic_default_avatar).circleCrop().into(holder.avatar);
                else holder.avatar.setImageResource(R.drawable.ic_default_avatar);
            }
            holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onConversationSelected(c); });
        }

        @Override public int getItemCount() { return list.size(); }
        static class ChatListViewHolder extends RecyclerView.ViewHolder {
            TextView name, msg, time; ImageView avatar;
            public ChatListViewHolder(@NonNull View v) { super(v); name = v.findViewById(R.id.textViewUsername); msg = v.findViewById(R.id.textViewLastMessage); time = v.findViewById(R.id.textViewTimestamp); avatar = v.findViewById(R.id.imageViewAvatar); }
        }
    }
}
