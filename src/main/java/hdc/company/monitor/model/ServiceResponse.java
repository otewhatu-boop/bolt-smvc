package hdc.company.monitor.model;

import java.util.Collections;
import java.util.List;

public class ServiceResponse<T> {
    private final List<T> data;
    private final String errorMessage;
    private final String message;

    public ServiceResponse(List<T> data, String errorMessage, String message) {
        this.data = data != null ? data : Collections.emptyList();
        this.errorMessage = errorMessage;
        this.message = message;
    }

    public ServiceResponse(List<T> data, String errorMessage) {
        this(data, errorMessage, null);
    }

    public List<T> getData() {
        return data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getMessage() {
        return message;
    }

    public boolean hasError() {
        return errorMessage != null;
    }

    public static <T> ServiceResponse<T> success(List<T> data) {
        return new ServiceResponse<>(data, null);
    }

    public static <T> ServiceResponse<T> successMessage(String message) {
        return new ServiceResponse<>(null, null, message);
    }

    public static <T> ServiceResponse<T> error(String errorMessage) {
        return new ServiceResponse<>(null, errorMessage);
    }
}
