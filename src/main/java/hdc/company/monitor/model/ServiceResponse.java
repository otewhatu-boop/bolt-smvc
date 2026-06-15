package hdc.company.monitor.model;

import java.util.Collections;
import java.util.List;

public class ServiceResponse<T> {
    private final List<T> data;
    private final String errorMessage;

    public ServiceResponse(List<T> data, String errorMessage) {
        this.data = data != null ? data : Collections.emptyList();
        this.errorMessage = errorMessage;
    }

    public List<T> getData() {
        return data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean hasError() {
        return errorMessage != null;
    }

    public static <T> ServiceResponse<T> success(List<T> data) {
        return new ServiceResponse<>(data, null);
    }

    public static <T> ServiceResponse<T> error(String errorMessage) {
        return new ServiceResponse<>(null, errorMessage);
    }
}
