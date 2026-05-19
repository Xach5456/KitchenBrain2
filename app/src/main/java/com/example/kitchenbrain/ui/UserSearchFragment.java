package com.example.kitchenbrain.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
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
 * Premium Fullscreen Fragment for user search.
 * Uses SearchViewModel (Activity Scope) to access search results.
 */
public class UserSearchFragment extends Fragment {

    private SearchViewModel viewModel;
    private UserSearchAdapter adapter;
    private TextInputEditText editTextSearch;
    private RecyclerView recyclerView;
    private ImageButton buttonClearSearch;
    private View searchBarContainer;
    private View shimmerView;
    private View layoutEmpty;

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
        
        showLoading(false);
    }

    private void initViews(View view) {
        editTextSearch = view.findViewById(R.id.editTextUserSearch);
        recyclerView = view.findViewById(R.id.recyclerViewUserResults);
        buttonClearSearch = view.findViewById(R.id.buttonClearSearch);
        searchBarContainer = view.findViewById(R.id.searchBarContainer);
        shimmerView = view.findViewById(R.id.shimmerUserSearch);
        layoutEmpty = view.findViewById(R.id.layoutEmptyUser);

        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                if (getActivity() != null) getActivity().onBackPressed();
            });
        }
    }

    private void setupAdapter() {
        adapter = new UserSearchAdapter(getContext(), new UserSearchAdapter.OnUserInteractionListener() {
            @Override
            public void onUserClick(User user) {
                if (getActivity() instanceof MainActivity) {
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
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (buttonClearSearch != null) {
                    buttonClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                }
                
                if (!query.isEmpty()) {
                    showLoading(true);
                }
                viewModel.searchUsers(query);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        if (buttonClearSearch != null) {
            buttonClearSearch.setOnClickListener(v -> editTextSearch.setText(""));
        }

        editTextSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (searchBarContainer != null) {
                searchBarContainer.setBackgroundResource(hasFocus ? 
                        R.drawable.bg_input_field_focused : R.drawable.bg_input_field);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), items -> {
            showLoading(false);
            List<User> users = new ArrayList<>();
            for (com.example.kitchenbrain.model.SearchItem item : items) {
                if (item.getType() == com.example.kitchenbrain.model.SearchItem.Type.USER) {
                    users.add(item.getUser());
                }
            }
            adapter.setUsers(users);
            
            if (layoutEmpty != null) {
                String query = editTextSearch.getText().toString().trim();
                layoutEmpty.setVisibility(!query.isEmpty() && users.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (shimmerView != null) shimmerView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        if (isLoading && layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hideKeyboard();
    }

    private void hideKeyboard() {
        if (getView() != null) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }
}
