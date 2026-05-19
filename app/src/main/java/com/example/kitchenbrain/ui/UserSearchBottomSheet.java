package com.example.kitchenbrain.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kitchenbrain.MainActivity;
import com.example.kitchenbrain.OtherUserProfileFragment;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.User;
import com.example.kitchenbrain.adapter.UserSearchAdapter;
import com.example.kitchenbrain.viewmodel.SearchViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * BottomSheet for searching users.
 * Supports "Selection Mode" for sharing content.
 */
public class UserSearchBottomSheet extends BottomSheetDialogFragment {

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
        textTitle = view.findViewById(R.id.textTitle);
        
        // Handle Back button in Toolbar
        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                if (isAdded()) {
                    if (getShowsDialog()) {
                        dismiss();
                    } else {
                        getParentFragmentManager().popBackStack();
                    }
                }
            });
        }

        // Support for old/legacy back button ID if present
        View buttonBack = view.findViewById(R.id.buttonBack);
        if (buttonBack != null) {
            buttonBack.setOnClickListener(v -> {
                if (isAdded()) {
                    if (getShowsDialog()) {
                        dismiss();
                    } else {
                        getParentFragmentManager().popBackStack();
                    }
                }
            });
        }
    }

    private void setupAdapter() {
        adapter = new UserSearchAdapter(getContext(), new UserSearchAdapter.OnUserInteractionListener() {
            @Override
            public void onUserClick(User user) {
                if (isSelectionMode && selectionListener != null) {
                    selectionListener.onUserSelected(user);
                    dismiss();
                } else if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToFragment(
                            OtherUserProfileFragment.newInstance(user.getUserId()), true);
                    if (getShowsDialog()) dismiss();
                }
            }

            @Override
            public void onFollowToggle(User user, boolean isFollowing) {}

            @Override
            public void onMessageClick(User user) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openChat(user.getUserId(), user.getUsername());
                    if (getShowsDialog()) dismiss();
                }
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        if (editTextSearch != null) {
            editTextSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    viewModel.searchUsers(s.toString().trim());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
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
