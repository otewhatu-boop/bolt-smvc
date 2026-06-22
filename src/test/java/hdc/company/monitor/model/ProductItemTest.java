package hdc.company.monitor.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductItemTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void noArgConstructor_andSetters_roundTripValues() {
        ProductItem item = new ProductItem();
        item.setProductName("prod1");
        item.setProductDescription("desc1");
        item.setTestCase("tc1");

        assertEquals("prod1", item.getProductName());
        assertEquals("desc1", item.getProductDescription());
        assertEquals("tc1", item.getTestCase());
    }

    @Test
    void allArgConstructor_setsAllFields() {
        ProductItem item = new ProductItem("name", "desc", "tc");

        assertEquals("name", item.getProductName());
        assertEquals("desc", item.getProductDescription());
        assertEquals("tc", item.getTestCase());
    }

    @Test
    void jacksonSerialization_usesSnakeCaseFieldNames() throws Exception {
        ProductItem item = new ProductItem("myprod", "mydesc", "mytc");

        String json = objectMapper.writeValueAsString(item);

        assertTrue(json.contains("\"product_name\":\"myprod\""));
        assertTrue(json.contains("\"product_description\":\"mydesc\""));
        assertTrue(json.contains("\"test_case\":\"mytc\""));
    }

    @Test
    void jacksonDeserialization_mapsSnakeCaseJsonToObject() throws Exception {
        String json = "{\"product_name\":\"p\",\"product_description\":\"d\",\"test_case\":\"t\"}";

        ProductItem item = objectMapper.readValue(json, ProductItem.class);

        assertEquals("p", item.getProductName());
        assertEquals("d", item.getProductDescription());
        assertEquals("t", item.getTestCase());
    }
}
