package com.example.kitchenbrain.model;

/**
 * Generic Result wrapper for clean architecture
 * Separates business logic results from errors
 * 
 * Usage:
 * - Success: new Result<>(data, null)
 * - Error: new Result<>(null, "Error message")
 * 
 * Check with: result.isSuccess()
 */
public class Result<T> {
    private final T data;
    private final String error;

    public Result(T data, String error) {
        this.data = data;
        this.error = error;
    }

    /**
     * Factory method for success result
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(data, null);
    }

    /**
     * Factory method for error result
     */
    public static <T> Result<T> error(String error) {
        return new Result<>(null, error);
    }

    /**
     * Returns true if operation was successful (no error)
     */
    public boolean isSuccess() {
        return error == null;
    }

    /**
     * Returns true if operation failed
     */
    public boolean isError() {
        return error != null;
    }

    /**
     * Get data (may be null if error occurred)
     */
    public T getData() {
        return data;
    }

    /**
     * Get error message (null if success)
     */
    public String getError() {
        return error;
    }

    /**
     * Get data or throw if error
     */
    public T getOrThrow() {
        if (error != null) {
            throw new IllegalStateException("Result contains error: " + error);
        }
        return data;
    }

    @Override
    public String toString() {
        if (isSuccess()) {
            return "Result[success: " + data + "]";
        } else {
            return "Result[error: " + error + "]";
        }
    }
}
