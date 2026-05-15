package com.example.kitchenbrain;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;

import com.example.kitchenbrain.adapter.FriendsAdapter;
import com.example.kitchenbrain.repository.FollowRepository;
import com.example.kitchenbrain.manager.FollowGraphRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * 🔥 FRIENDS FRAGMENT - Reactive Social Lists
 * 
 * Displays Followers, Following, and Mutual Friends using FollowGraphRepository.
 */
public class FriendsFragment extends Fragment {
    private static final String TAG = "FriendsFragment";
    
    // Tab Order: Followers (0), Following (1), Mutual (2) - Consistent with common social apps
    public static final int TAB_FOLLOWERS = 0;
    public static final int TAB_FOLLOWING = 1;
    public static final int TAB_MUTUAL = 2;
    
    private RecyclerView recyclerView;
    private View layoutLoading;
    private ProgressBar progressBar;
    private TextView textEmpty;
    private TextView textFriendsCount;
    private TabLayout tabLayout;
    private FriendsAdapter adapter;
    private final List<User> userList = new ArrayList<>();
    private FollowRepository followRepository;
    private String currentUserId;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        currentUserId = FirebaseAuth.getInstance().getUid();
        followRepository = new FollowRepository();
        
        initViews(view);
        setupRecyclerView();
        setupTabLayout();
        setupObservers();
        
        // Ensure FollowGraph is initialized for the current user
        if (currentUserId != null) {
            FollowGraphRepository.getInstance().initialize(currentUserId);
        }

        // Restore or set initial tab
        int initialTab = TAB_FOLLOWERS;
        if (getArguments() != null) {
            initialTab = getArguments().getInt("current_tab", TAB_FOLLOWERS);
        } else if (savedInstanceState != null) {
            initialTab = savedInstanceState.getInt("current_tab", TAB_FOLLOWERS);
        }
        
        if (tabLayout != null) {
            TabLayout.Tab tab = tabLayout.getTabAt(initialTab);
            if (tab != null) {
                tab.select();
            } else {
                loadUsers(initialTab);
            }
        }
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tabLayout);
        recyclerView = view.findViewById(R.id.recyclerViewFriends);
        layoutLoading = view.findViewById(R.id.layoutLoading);
        progressBar = view.findViewById(R.id.progressBar);
        textEmpty = view.findViewById(R.id.textEmpty);
        textFriendsCount = view.findViewById(R.id.textFriendsCount);
        
        if (tabLayout != null) {
            // Re-create tabs to ensure correct order and labels
            tabLayout.removeAllTabs();
            tabLayout.addTab(tabLayout.newTab().setText("Followers"));
            tabLayout.addTab(tabLayout.newTab().setText("Following"));
            tabLayout.addTab(tabLayout.newTab().setText("Mutual"));
        }
    }

    private void setupRecyclerView() {
        if (recyclerView == null) return;
        
        adapter = new FriendsAdapter(userList, currentUserId, new FriendsAdapter.OnFriendActionListener() {
            @Override
            public void onMessageClick(User user) {
                openChat(user);
            }

            @Override
            public void onFollowClick(User user) {
                followUser(user);
            }

            @Override
            public void onUnfollowClick(User user) {
                unfollowUser(user);
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupTabLayout() {
        if (tabLayout == null) return;
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (adapter != null) adapter.setTabType(position);
                loadUsers(position);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                loadUsers(tab.getPosition());
            }
        });
    }

    private void setupObservers() {
        FollowGraphRepository graph = FollowGraphRepository.getInstance();
        
        // Reactive updates when the follow graph changes (e.g. following/followers are fetched)
        graph.getFollowersLiveData().observe(getViewLifecycleOwner(), users -> {
            if (tabLayout != null && tabLayout.getSelectedTabPosition() == TAB_FOLLOWERS) {
                updateUI(users, "followers");
            }
        });
        
        graph.getFollowingLiveData().observe(getViewLifecycleOwner(), users -> {
            if (tabLayout != null && tabLayout.getSelectedTabPosition() == TAB_FOLLOWING) {
                updateUI(users, "following");
            }
        });
        
        graph.getMutualLiveData().observe(getViewLifecycleOwner(), users -> {
            if (tabLayout != null && tabLayout.getSelectedTabPosition() == TAB_MUTUAL) {
                updateUI(users, "mutual");
            }
        });
    }

    private void loadUsers(int tabType) {
        if (currentUserId == null || !isAdded()) return;
        
        if (layoutLoading != null) layoutLoading.setVisibility(View.VISIBLE);
        else if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        if (textEmpty != null) textEmpty.setVisibility(View.GONE);

        FollowGraphRepository graph = FollowGraphRepository.getInstance();
        List<User> users;
        String type;
        
        switch (tabType) {
            case TAB_FOLLOWERS:
                users = graph.getFollowersUsers();
                type = "followers";
                break;
            case TAB_FOLLOWING:
                users = graph.getFollowingUsers();
                type = "following";
                break;
            case TAB_MUTUAL:
            default:
                users = graph.getMutualUsers();
                type = "mutual friends";
                break;
        }
        
        updateUI(users, type);
    }

    private void updateUI(List<User> users, String type) {
        if (!isAdded()) return;
        
        if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        
        if (users != null) {
            userList.clear();
            userList.addAll(users);
            if (adapter != null) adapter.notifyDataSetChanged();
            
            if (textEmpty != null) {
                textEmpty.setVisibility(userList.isEmpty() ? View.VISIBLE : View.GONE);
                textEmpty.setText("No " + type + " found");
            }
            
            if (textFriendsCount != null) {
                textFriendsCount.setText(users.size() + " " + type);
            }
        }
    }

    private void openChat(User user) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openChat(user.getUserId(), user.getUsername());
        }
    }

    private void followUser(User user) {
        if (currentUserId == null) return;
        followRepository.followUser(currentUserId, user.getUserId(), (success, error) -> {
            if (!success && isAdded()) {
                Log.e(TAG, "Failed to follow: " + error);
            }
        });
    }

    private void unfollowUser(User user) {
        if (currentUserId == null) return;
        followRepository.unfollowUser(currentUserId, user.getUserId(), (success, error) -> {
            if (!success && isAdded()) {
                Log.e(TAG, "Failed to unfollow: " + error);
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (tabLayout != null) {
            outState.putInt("current_tab", tabLayout.getSelectedTabPosition());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView = null;
        layoutLoading = null;
        progressBar = null;
        textEmpty = null;
        textFriendsCount = null;
        tabLayout = null;
    }
}
