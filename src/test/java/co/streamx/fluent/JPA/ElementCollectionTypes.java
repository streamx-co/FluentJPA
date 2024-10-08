package co.streamx.fluent.JPA;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;

import co.streamx.fluent.notation.Tuple;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

public interface ElementCollectionTypes {
    @Embeddable
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class Address {

        private String addressLine1;


        private String addressLine2;


        private String city;


        private String state;

        private String country;

        private String zipCode;
    }

    @Entity
    @Table(name = "users", schema = "EC")
    @Data
    @NoArgsConstructor
    class User {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String name;

        @Column(unique = true)
        private String email;

        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(name = "user_phone_numbers", schema = "EC", joinColumns = @JoinColumn(name = "user_id"))
        @Column(name = "phone_number")
        private Set<String> phoneNumbers = new HashSet<>();

        @ElementCollection(fetch = FetchType.LAZY)
        @CollectionTable(name = "user_addresses", schema = "EC", joinColumns = @JoinColumn(name = "user_id"))
        @AttributeOverrides({ @AttributeOverride(name = "addressLine1", column = @Column(name = "house_number")),
                @AttributeOverride(name = "addressLine2", column = @Column(name = "street")) })
        private Set<Address> addresses = new HashSet<>();

        public User(String name, String email, Set<String> phoneNumbers, Set<Address> addresses) {
            this.name = name;
            this.email = email;
            this.phoneNumbers = phoneNumbers;
            this.addresses = addresses;
        }
    }

    @MappedSuperclass
    @Data
    @NoArgsConstructor
    public class Employee {

        @Id
        protected Integer empId;
        @ManyToOne
        @JoinColumn(name = "USER")
        protected User user;
    }

    @Tuple
    @Table(name = "PT_EMP")
    @AssociationOverride(name = "user", joinColumns = @JoinColumn(name = "USER_ID"))
    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public class PartTimeEmployee extends Employee {

        // Inherited empId field mapped to PT_EMP.EMPID
        // Inherited version field mapped to PT_EMP.VERSION
        // address field mapping overridden to PT_EMP.ADDR_ID fk
        @Column(name = "WAGE")
        protected Float hourlyWage;

    }

}
