package co.streamx.fluent.JPA.repository.entities.sqlservertutorial;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("serial")
@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemId implements Serializable {

    @Column(name = "order_id")
    private Integer orderId;
    
    @Column(name = "item_id")
    private Integer itemId;
}
