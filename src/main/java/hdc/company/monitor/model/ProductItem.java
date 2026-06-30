package hdc.company.monitor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProductItem {

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("product_description")
    private String productDescription;

    @JsonProperty("test_case")
    private String testCase;

    @JsonProperty("http_op")
    private String httpOp;

    public ProductItem() {
    }

    public ProductItem(String productName, String productDescription, String testCase, String httpOp) {
        this.productName = productName;
        this.productDescription = productDescription;
        this.testCase = testCase;
        this.httpOp = httpOp;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public String getTestCase() {
        return testCase;
    }

    public void setTestCase(String testCase) {
        this.testCase = testCase;
    }

    public String getHttpOp() {
        return httpOp;
    }

    public void setHttpOp(String httpOp) {
        this.httpOp = httpOp;
    }
}
