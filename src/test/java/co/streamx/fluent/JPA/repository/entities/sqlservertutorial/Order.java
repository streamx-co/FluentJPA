package co.streamx.fluent.JPA.repository.entities.sqlservertutorial;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "orders")
public class Order  {
    @Id
    @Column(name = "order_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @JoinColumn(name = "order_date")
    private Date orderDate;

    @JoinColumn(name = "order_status")
    private byte status;
}
