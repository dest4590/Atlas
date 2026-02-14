package org.collapseloader.atlas.dto;

public record ApiResponse<T>(
        boolean success,
        T data,
        String error,
        long timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, System.currentTimeMillis());
    }
}