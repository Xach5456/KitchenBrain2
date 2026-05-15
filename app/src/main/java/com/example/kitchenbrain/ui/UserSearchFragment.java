package com.example.kitchenbrain.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kitchenbrain.MainActivity;
import com.example.kitchenbrain.OtherUserProfileFragment;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.User;
import com.example.kitchenbrain.adapter.UserSearchAdapter;
import com.example.kitchenbrain.viewmodel.SearchViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * Fullscreen Fragment for user search.
 * Uses SearchViewModel (Activity Scope) to access search results.
 */
public class UserSearchFragment extends Fragment {

    private static final String TAG = "UserSearchFragment";
    
    private SearchViewModel viewModel;
    private UserSearchAdapter adapter;
    private TextInputEditText editTextSearch;
    private RecyclerView recyclerView;
    private ImageButton buttonBack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Use requireActivity() to access the same ViewModel as in SearchFragment
        viewModel = new ViewModelProvider(requireActivity()).get(SearchViewModel.class);

        initViews(view);
        setupAdapter();
        setupListeners();
        observeViewModel();
    }

    private void initViews(View view) {
        editTextSearch = view.findViewById(R.id.editTextUserSearch);
        recyclerView = view.findViewById(R.id.recyclerViewUserResults);
        buttonBack = view.findViewById(R.id.buttonBack);
    }

    private void setupAdapter() {
        adapter = new UserSearchAdapter(getContext(), new UserSearchAdapter.OnUserInteractionListener() {
            @Override
            public void onUserClick(User user) {
                // On click, open user profile
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToFragment(
                            OtherUserProfileFragment.newInstance(user.getUserId()), true);
                }
            }

            @Override
            public void onFollowToggle(User user, boolean isFollowing) {
                // Follow logic is already in the adapter (via FollowRepository)
            }

            @Override
            public void onMessageClick(User user) {
                // Open chat with user
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openChat(user.getUserId(), user.getUsername());
                }
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        // Back button
        buttonBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Search with debounce
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                viewModel.searchUsers(query);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModel() {
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), items -> {
            // Convert SearchItem back to User for the current adapter
            List<User> users = new ArrayList<>();
            for (com.example.kitchenbrain.model.SearchItem item : items) {
                if (item.getType() == com.example.kitchenbrain.model.SearchItem.Type.USER) {
                    users.add(item.getUser());
                }
            }
            adapter.setUsers(users);
        });
    }
}
