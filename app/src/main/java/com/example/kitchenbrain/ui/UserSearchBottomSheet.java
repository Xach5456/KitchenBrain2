package com.example.kitchenbrain.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

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
 * Fragment for searching users.
 * Supports "Selection Mode" for sharing content.
 */
public class UserSearchBottomSheet extends Fragment {

    private SearchViewModel viewModel;
    private UserSearchAdapter adapter;
    private TextInputEditText editTextSearch;
    private RecyclerView recyclerView;
    private TextView textTitle;
    
    private OnUserSelectedListener selectionListener;
    private boolean isSelectionMode = false;

    public interface OnUserSelectedListener {
        void onUserSelected(User user);
    }

    public void setOnUserSelectedListener(OnUserSelectedListener listener) {
        this.selectionListener = listener;
        this.isSelectionMode = true;
    }

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
        setupAdapter();
        setupListeners();
        observeViewModel();
        
        if (isSelectionMode && textTitle != null) {
            textTitle.setText("Send to...");
        }
    }

    private void initViews(View view) {
        editTextSearch = view.findViewById(R.id.editTextUserSearch);
        recyclerView = view.findViewById(R.id.recyclerViewUserResults);
        textTitle = view.findViewById(R.id.textTitle); // Assuming this ID exists
        ImageButton buttonBack = view.findViewById(R.id.buttonBack);
        
        if (buttonBack != null) {
            buttonBack.setOnClickListener(v -> requireActivity().onBackPressed());
        }
    }

    private void setupAdapter() {
        adapter = new UserSearchAdapter(getContext(), new UserSearchAdapter.OnUserInteractionListener() {
            @Override
            public void onUserClick(User user) {
                if (isSelectionMode && selectionListener != null) {
                    selectionListener.onUserSelected(user);
                    requireActivity().onBackPressed();
                } else if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToFragment(
                            OtherUserProfileFragment.newInstance(user.getUserId()), true);
                }
            }

            @Override
            public void onFollowToggle(User user, boolean isFollowing) {}

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

    private void setupListeners() {
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.searchUsers(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModel() {
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), items -> {
            List<User> users = new ArrayList<>();
            for (com.example.kitchenbrain.model.SearchItem item : items) {
                if (item.getType() == com.example.kitchenbrain.model.SearchItem.Type.USER) {
                    users.add(item.getUser());
                }
            }
            adapter.setUsers(users);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (adapter != null) adapter.cleanup();
    }
}
