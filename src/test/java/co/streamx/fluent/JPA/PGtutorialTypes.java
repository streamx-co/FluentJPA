package co.streamx.fluent.JPA;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.Data;

public interface PGtutorialTypes {
    @Entity
    @Data
    @Table(name = "film")
    class Film {
        @Id
        @Column(name = "film_id")
        private Integer id;
        private String title;
        private float rentalRate;
        private int length;
    }

    @Entity
    @Data
    @Table(name = "inventory")
    class Inventory {
        @Id
        @Column(name = "inventory_id")
        private Integer id;
        @OneToOne
        @JoinColumn(name = "film_id")
        private Film film;

    }

    @Entity
    @Data
    @Table(name = "rental")
    public class Rental {
        @Id
        @Column(name = "rental_id")
        private Integer id;

        @ManyToOne
        @JoinColumn(name = "customer_id")
        private Customer customer;

        @ManyToOne
        @JoinColumn(name = "inventory_id")
        private Inventory inventory;

        private Date returnDate;

    }

    @Entity
    @Data
    @Table(name = "customer")
    class Customer {
        @Id
        @Column(name = "customer_id")
        private Integer id;
        private String name;
        private String firstName;
        private String lastName;
        private String email;

        @ManyToOne
        @JoinColumn(name = "store_id")
        private Store store;
    }

    @Entity
    @Data
    @Table(name = "staff")
    class Staff {
        @Id
        @Column(name = "staff_id")
        private Integer id;
        private String firstName;
        private String lastName;
    }

    @Entity
    @Data
    @Table(name = "payment")
    class Payment {
        @Id
        @Column(name = "payment_id")
        private Integer id;

        @ManyToOne
        @JoinColumn(name = "customer_id")
        private Customer customer;

        @ManyToOne
        @JoinColumn(name = "staff_id")
        private Staff staff;

        private float amount;
        private Date paymentDate;
    }

    @Entity
    @Data
    @Table(name = "store")
    class Store {
        @Id
        @Column(name = "store_id")
        private Integer id;
    }

    @Entity
    @Data
    @Table(name = "link")
    class Link {
        @Id
        @Column(name = "ID")
        private Integer id;

        private String url;
        private String name;
        private String description;
        private String rel;
        private Date lastUpdate;
    }
}
