package com.example.kitchenbrain;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kitchenbrain.adapter.UserSearchAdapter;
import com.example.kitchenbrain.viewmodel.SearchViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        
        viewModel = new ViewModelProvider(requireActivity()).get(SearchViewModel.class);
        
        initViews(view);
        setupRecyclerView();
        setupSearchLogic();
        observeViewModel();
    }

    private void initViews(View view) {
        editTextSearch = view.findViewById(R.id.editTextUserSearch);
        recyclerView = view.findViewById(R.id.recyclerViewUserResults);
        buttonBack = view.findViewById(R.id.buttonBack);
        
        if (buttonBack != null) {
            buttonBack.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }
    }

    private void setupRecyclerView() {
        adapter = new UserSearchAdapter(getContext(), new UserSearchAdapter.OnUserInteractionListener() {
            @Override
            public void onUserClick(User user) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToFragment(
                            OtherUserProfileFragment.newInstance(user.getUserId()), true);
                }
            }

            @Override
            public void onFollowToggle(User user, boolean isFollowing) {
                // Logic handled inside adapter via FollowRepository
                if (getContext() != null) {
                    String message = isFollowing ? "Following " + user.getUsername() : "Unfollowed " + user.getUsername();
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onMessageClick(User user) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openChat(user.getUserId(), user.getUsername());
                }
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearchLogic() {
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
            List<User> users = items.stream()
                    .filter(item -> item.getType() == com.example.kitchenbrain.model.SearchItem.Type.USER)
                    .map(com.example.kitchenbrain.model.SearchItem::getUser)
                    .collect(Collectors.toList());

            adapter.setUsers(users);
        });
    }
}
