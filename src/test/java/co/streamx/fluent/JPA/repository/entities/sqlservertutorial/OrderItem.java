package co.streamx.fluent.JPA.repository.entities.sqlservertutorial;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "order_items")
public class OrderItem  {
    @EmbeddedId
    private OrderItemId id;

    @ManyToOne
    @MapsId(value = "orderId")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private int quantity;

    @Column(name = "list_price")
    private float listPrice;

    private float discount;
}
