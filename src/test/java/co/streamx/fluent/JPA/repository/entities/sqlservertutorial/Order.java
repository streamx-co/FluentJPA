package co.streamx.fluent.JPA.repository.entities.sqlservertutorial;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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
