package hdc.company.monitor.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServiceResponseTest {

    @Test
    void success_setsDataAndNullError() {
        List<String> data = Arrays.asList("a", "b");
        ServiceResponse<String> response = ServiceResponse.success(data);

        assertEquals(data, response.getData());
        assertFalse(response.hasError());
        assertNull(response.getErrorMessage());
    }

    @Test
    void success_withNullDataList_returnsEmptyListNotNull() {
        ServiceResponse<String> response = ServiceResponse.success(null);

        assertNotNull(response.getData());
        assertTrue(response.getData().isEmpty());
        assertFalse(response.hasError());
    }

    @Test
    void successMessage_setsMessageAndEmptyData() {
        ServiceResponse<String> response = ServiceResponse.successMessage("done");

        assertEquals("done", response.getMessage());
        assertTrue(response.getData().isEmpty());
        assertFalse(response.hasError());
    }

    @Test
    void error_setsErrorMessageAndEmptyData() {
        ServiceResponse<String> response = ServiceResponse.error("boom");

        assertTrue(response.hasError());
        assertEquals("boom", response.getErrorMessage());
        assertTrue(response.getData().isEmpty());
    }

    @Test
    void twoArgConstructor_setsMessageToNull() {
        List<Integer> data = Collections.singletonList(1);
        ServiceResponse<Integer> response = new ServiceResponse<>(data, "err");

        assertEquals("err", response.getErrorMessage());
        assertTrue(response.hasError());
        assertNull(response.getMessage());
        assertEquals(data, response.getData());
    }

    @Test
    void successMessage_withNullMessage_doesNotMarkAsError() {
        ServiceResponse<String> response = new ServiceResponse<>(null, null, null);

        assertFalse(response.hasError());
        assertNull(response.getMessage());
        assertTrue(response.getData().isEmpty());
    }
}
