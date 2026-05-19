package com.example.kitchenbrain.adapter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kitchenbrain.R;
import com.example.kitchenbrain.model.ChatMessage;

/**
 * 🔥 MODERN SWIPE ACTIONS - Telegram Style
 * Right Swipe -> Edit (Green)
 * Left Swipe -> Delete (Red)
 */
public class SwipeChatCallback extends ItemTouchHelper.SimpleCallback {

    public interface SwipeListener {
        void onEditSwipe(int position);
        void onDeleteSwipe(int position);
    }

    private final SwipeListener listener;
    private final String currentUserId;
    private final ZeroCrashChatAdapter adapter;

    private final Paint mPaint;
    private final int editColor;
    private final int deleteColor;
    private final Drawable editIcon;
    private final Drawable deleteIcon;
    
    private final float cornerRadius;

    public SwipeChatCallback(Context context, ZeroCrashChatAdapter adapter, String currentUserId, SwipeListener listener) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.adapter = adapter;
        this.currentUserId = currentUserId;
        this.listener = listener;

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        editColor = Color.parseColor("#34C759"); // Apple/Telegram Green
        deleteColor = Color.parseColor("#FF3B30"); // Apple/Telegram Red
        
        editIcon = ContextCompat.getDrawable(context, R.drawable.ic_edit);
        deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete_outline);
        
        if (editIcon != null) editIcon.setTint(Color.WHITE);
        if (deleteIcon != null) deleteIcon.setTint(Color.WHITE);
        
        cornerRadius = 16 * context.getResources().getDisplayMetrics().density;
    }

    @Override
    public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getAdapterPosition();
        ChatMessage message = adapter.getMessage(position);
        
        if (message != null && message.getSenderId().equals(currentUserId)) {
            boolean isText = "text".equals(message.getMessageType()) || message.getMessageType() == null;
            if (isText) {
                return ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
            } else {
                return ItemTouchHelper.LEFT; // Only allow deletion for media
            }
        }
        return 0;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        if (direction == ItemTouchHelper.RIGHT) {
            listener.onEditSwipe(position);
        } else {
            listener.onDeleteSwipe(position);
        }
        adapter.notifyItemChanged(position);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, 
                           float dX, float dY, int actionState, boolean isCurrentlyActive) {
        
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            View itemView = viewHolder.itemView;
            float height = (float) itemView.getBottom() - (float) itemView.getTop();
            float width = height;

            if (dX > 0) { // Swiping Right -> Edit
                mPaint.setColor(editColor);
                // Draw rounded background
                RectF background = new RectF((float) itemView.getLeft(), (float) itemView.getTop(), dX, (float) itemView.getBottom());
                c.drawRoundRect(background, cornerRadius, cornerRadius, mPaint);

                // Draw icon
                if (editIcon != null) {
                    int iconMargin = (int) (height - editIcon.getIntrinsicHeight()) / 2;
                    int iconTop = itemView.getTop() + iconMargin;
                    int iconBottom = iconTop + editIcon.getIntrinsicHeight();
                    int iconLeft = itemView.getLeft() + (int)(dX / 3) - editIcon.getIntrinsicWidth() / 2;
                    if (iconLeft < itemView.getLeft() + 40) iconLeft = itemView.getLeft() + 40;
                    int iconRight = iconLeft + editIcon.getIntrinsicWidth();
                    
                    editIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    editIcon.setAlpha((int) Math.min(255, dX * 2));
                    editIcon.draw(c);
                }
            } else if (dX < 0) { // Swiping Left -> Delete
                mPaint.setColor(deleteColor);
                RectF background = new RectF((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
                c.drawRoundRect(background, cornerRadius, cornerRadius, mPaint);

                if (deleteIcon != null) {
                    int iconMargin = (int) (height - deleteIcon.getIntrinsicHeight()) / 2;
                    int iconTop = itemView.getTop() + iconMargin;
                    int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
                    int iconRight = itemView.getRight() + (int)(dX / 3) + deleteIcon.getIntrinsicWidth() / 2;
                    if (iconRight > itemView.getRight() - 40) iconRight = itemView.getRight() - 40;
                    int iconLeft = iconRight - deleteIcon.getIntrinsicWidth();
                    
                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    deleteIcon.setAlpha((int) Math.min(255, Math.abs(dX) * 2));
                    deleteIcon.draw(c);
                }
            }
        }
        
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
