package fr.arolla.ddd9.entity;

import javax.persistence.*;

@Entity
public class OrderLine {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    String productSku;
    String productName;

    @Embedded
    Quantity quantity;

    public OrderLine() { //for JPA
    }

    public OrderLine(String productSku, String productName, Quantity quantity) {
        this.productSku = productSku;
        this.productName = productName;
        this.quantity = quantity;
    }

    public String getProductSku() {
        return productSku;
    }

    public String getProductName() {
        return productName;
    }

    public Quantity getQuantity() {
        return quantity;
    }
}