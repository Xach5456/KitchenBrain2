package com.example.kitchenbrain;

import com.example.kitchenbrain.model.FeedItem;

import java.util.List;

/**
 * UiState - SINGLE SOURCE OF TRUTH for HomeFragment UI
 */
public class HomeUiState {

    public enum Status {
        LOADING,
        SUCCESS,
        EMPTY,
        ERROR
    }

    // UI State
    private final Status status;
    private final List<FeedItem> data;
    private final String errorMessage;
    private final boolean isFromCache;

    // Private constructor - use factory methods
    private HomeUiState(Status status, List<FeedItem> data, String errorMessage, boolean isFromCache) {
        this.status = status;
        this.data = data;
        this.errorMessage = errorMessage;
        this.isFromCache = isFromCache;
    }

    // ===== FACTORY METHODS (Builder pattern) =====

    public static HomeUiState loading() {
        return new HomeUiState(Status.LOADING, null, null, false);
    }

    public static HomeUiState success(List<FeedItem> data) {
        return success(data, false);
    }

    public static HomeUiState success(List<FeedItem> data, boolean isFromCache) {
        if (data == null || data.isEmpty()) {
            return empty();
        }
        return new HomeUiState(Status.SUCCESS, data, null, isFromCache);
    }

    public static HomeUiState empty() {
        return new HomeUiState(Status.EMPTY, null, null, false);
    }

    public static HomeUiState error(String message) {
        return new HomeUiState(Status.ERROR, null, message, false);
    }

    // ===== GETTERS =====

    public Status getStatus() {
        return status;
    }

    public List<FeedItem> getData() {
        return data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isFromCache() {
        return isFromCache;
    }

    public boolean isLoading() {
        return status == Status.LOADING;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isEmpty() {
        return status == Status.EMPTY;
    }

    public boolean hasError() {
        return status == Status.ERROR;
    }

    @Override
    public String toString() {
        return "HomeUiState{" +
                "status=" + status +
                ", data=" + (data != null ? data.size() : "null") +
                ", error='" + errorMessage + '\'' +
                ", isFromCache=" + isFromCache +
                '}';
    }
}
