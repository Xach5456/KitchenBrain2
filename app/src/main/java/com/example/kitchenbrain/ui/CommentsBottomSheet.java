package com.example.kitchenbrain.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kitchenbrain.R;
import com.example.kitchenbrain.model.Comment;
import com.example.kitchenbrain.social.CommentsManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

/**
 * CommentsBottomSheet - Instagram-style Comments UI
 * 
 * ✅ INSTAGRAM ARCHITECTURE:
 * - Bottom sheet with comments list
 * - Real-time comment updates
 * - Send comment functionality
 */
public class CommentsBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "CommentsBottomSheet";
    private static final String ARG_POST_ID = "postId";
    private static final String ARG_POST_TITLE = "postTitle";

    private RecyclerView recyclerViewComments;
    private EditText editTextComment;
    private ImageButton btnSend;
    private ImageButton btnClose;

    private CommentsManager commentsManager;
    private com.google.firebase.firestore.ListenerRegistration commentsListener;
    private CommentsAdapter commentsAdapter;
    private FirebaseAuth auth;
    private String postId;
    private String postTitle;

    public static CommentsBottomSheet newInstance(String postId, String postTitle) {
        CommentsBottomSheet fragment = new CommentsBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_POST_ID, postId);
        args.putString(ARG_POST_TITLE, postTitle);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getArguments() != null) {
            postId = getArguments().getString(ARG_POST_ID);
            postTitle = getArguments().getString(ARG_POST_TITLE);
        }
        
        commentsManager = new CommentsManager();
        auth = FirebaseAuth.getInstance();
        
        Log.d(TAG, "🔥 CommentsBottomSheet created for post: " + postId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_comments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupRecyclerView();
        setupClickListeners();
        startCommentsListener();
    }

    private void initViews(View view) {
        recyclerViewComments = view.findViewById(R.id.recyclerViewComments);
        editTextComment = view.findViewById(R.id.editTextComment);
        btnSend = view.findViewById(R.id.btnSend);
        btnClose = view.findViewById(R.id.btnClose);
        
        Log.d(TAG, "✅ Views initialized");
    }

    private void setupRecyclerView() {
        commentsAdapter = new CommentsAdapter();
        recyclerViewComments.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewComments.setAdapter(commentsAdapter);
        
        Log.d(TAG, "✅ RecyclerView setup complete");
    }

    private void setupClickListeners() {
        btnClose.setOnClickListener(v -> {
            Log.d(TAG, "🔽 Close button clicked");
            dismiss();
        });

        btnSend.setOnClickListener(v -> {
            Log.d(TAG, "📤 Send button clicked");
            sendComment();
        });
        
        Log.d(TAG, "✅ Click listeners setup complete");
    }

    private void startCommentsListener() {
        Log.d(TAG, "👂 Starting comments listener for post: " + postId);
        
        if (postId == null) {
            Log.e(TAG, "❌ Post ID is null, cannot start listener");
            return;
        }
        
        commentsListener = commentsManager.listenToComments(postId, new CommentsManager.CommentsUpdateCallback() {
            @Override
            public void onCommentsUpdated(List<Comment> comments) {
                Log.d(TAG, "📥 Comments updated: " + comments.size() + " comments");
                commentsAdapter.setComments(comments);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Comments listener error: " + error);
                Toast.makeText(getContext(), "Error loading comments: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendComment() {
        String commentText = editTextComment.getText().toString().trim();
        
        if (commentText.isEmpty()) {
            Log.d(TAG, "⚠️ Comment text is empty");
            return;
        }
        
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "❌ User not authenticated");
            Toast.makeText(getContext(), "Please login to comment", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = user.getUid();
        String userName = user.getDisplayName() != null ? user.getDisplayName() : "Anonymous";
        String userAvatar = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";
        
        Log.d(TAG, "📤 Sending comment: " + commentText);
        
        // Disable send button while sending
        btnSend.setEnabled(false);
        
        commentsManager.addComment(postId, userId, userName, userAvatar, commentText, new CommentsManager.CommentCallback() {
            @Override
            public void onSuccess(Comment comment) {
                Log.d(TAG, "✅ Comment sent successfully");
                editTextComment.setText(""); // Clear input
                btnSend.setEnabled(true); // Re-enable send button
                
                // Scroll to bottom to show new comment
                recyclerViewComments.scrollToPosition(commentsAdapter.getItemCount() - 1);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Failed to send comment: " + error);
                btnSend.setEnabled(true); // Re-enable send button
                Toast.makeText(getContext(), "Failed to send comment: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Stop comments listener
        if (commentsListener != null) {
            commentsListener.remove();
            Log.d(TAG, "👋 Comments listener stopped");
        }
        
        commentsManager = null;
        commentsAdapter = null;
        
        Log.d(TAG, "💀 CommentsBottomSheet destroyed");
    }
    
    // Simple CommentsAdapter (can be expanded later)
    private static class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {
        
        private List<Comment> comments = new java.util.ArrayList<>();
        
        public void setComments(List<Comment> comments) {
            this.comments = comments;
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // TODO: Create comment item layout
            View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new CommentViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
            Comment comment = comments.get(position);
            holder.bind(comment);
        }
        
        @Override
        public int getItemCount() {
            return comments.size();
        }
        
        static class CommentViewHolder extends RecyclerView.ViewHolder {
            public CommentViewHolder(@NonNull View itemView) {
                super(itemView);
            }
            
            public void bind(Comment comment) {
                // TODO: Bind comment data to views
                // For now, use simple text display
                if (itemView instanceof android.widget.TwoLineListItem) {
                    android.widget.TwoLineListItem twoLine = (android.widget.TwoLineListItem) itemView;
                    twoLine.getText1().setText(comment.getUserName());
                    twoLine.getText2().setText(comment.getText());
                }
            }
        }
    }
}
