package co.streamx.fluent.JPA;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
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
}
