package com.example.kitchenbrain.adapter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kitchenbrain.R;
import com.example.kitchenbrain.model.ChatMessage;

/**
 * 🔥 SWIPE ACTIONS FOR CHAT
 * Left Swipe -> Delete (Red)
 * Right Swipe -> Edit (Green)
 */
public class SwipeChatCallback extends ItemTouchHelper.SimpleCallback {

    public interface SwipeListener {
        void onEditSwipe(int position);
        void onDeleteSwipe(int position);
    }

    private final SwipeListener listener;
    private final String currentUserId;
    private final ZeroCrashChatAdapter adapter;

    private final Paint mClearPaint;
    private final ColorDrawable mBackground;
    private final int editBackgroundColor;
    private final int deleteBackgroundColor;
    private final Drawable editIcon;
    private final Drawable deleteIcon;

    public SwipeChatCallback(Context context, ZeroCrashChatAdapter adapter, String currentUserId, SwipeListener listener) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.adapter = adapter;
        this.currentUserId = currentUserId;
        this.listener = listener;

        mBackground = new ColorDrawable();
        editBackgroundColor = Color.parseColor("#34C759");
        deleteBackgroundColor = Color.parseColor("#FF3B30");
        mClearPaint = new Paint();
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        editIcon = ContextCompat.getDrawable(context, R.drawable.ic_edit);
        deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete_outline);
        
        if (editIcon != null) editIcon.setTint(Color.WHITE);
        if (deleteIcon != null) deleteIcon.setTint(Color.WHITE);
    }

    @Override
    public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getAdapterPosition();
        ChatMessage message = adapter.getMessage(position);
        
        // Only allow swipe on own messages
        if (message != null && message.getSenderId().equals(currentUserId)) {
            // Text messages can be edited, voice messages only deleted (usually)
            boolean isText = "text".equals(message.getMessageType()) || message.getMessageType() == null;
            if (isText) {
                return ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
            } else {
                return ItemTouchHelper.LEFT; // Voice/Image only delete
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
        // Notify adapter to restore item state (don't actually remove yet)
        adapter.notifyItemChanged(position);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getBottom() - itemView.getTop();
        boolean isCanceled = dX == 0f && !isCurrentlyActive;

        if (isCanceled) {
            clearCanvas(c, (float) itemView.getLeft(), (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            return;
        }

        if (dX > 0) { // Swipe Right -> Edit
            mBackground.setColor(editBackgroundColor);
            mBackground.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + (int) dX, itemView.getBottom());
            mBackground.draw(c);
            drawIcon(c, editIcon, itemView, (int) dX, true);
        } else if (dX < 0) { // Swipe Left -> Delete
            mBackground.setColor(deleteBackgroundColor);
            mBackground.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            mBackground.draw(c);
            drawIcon(c, deleteIcon, itemView, (int) dX, false);
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void drawIcon(Canvas c, Drawable icon, View itemView, int dX, boolean isEdit) {
        if (icon == null) return;
        int itemHeight = itemView.getBottom() - itemView.getTop();
        int iconTop = itemView.getTop() + (itemHeight - icon.getIntrinsicHeight()) / 2;
        int iconBottom = iconTop + icon.getIntrinsicHeight();
        
        if (isEdit) {
            int iconLeft = itemView.getLeft() + 40;
            int iconRight = iconLeft + icon.getIntrinsicWidth();
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
        } else {
            int iconRight = itemView.getRight() - 40;
            int iconLeft = iconRight - icon.getIntrinsicWidth();
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
        }
        icon.draw(c);
    }

    private void clearCanvas(Canvas c, Float left, Float top, Float right, Float bottom) {
        c.drawRect(left, top, right, bottom, mClearPaint);
    }
}
