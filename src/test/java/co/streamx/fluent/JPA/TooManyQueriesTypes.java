package co.streamx.fluent.JPA;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

public interface TooManyQueriesTypes {

    @Entity
    @Table(name = "cd_person", schema = "cd")
    @Data
    @NoArgsConstructor
    public class Person {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
        @JoinColumn(name = "password_id")
//        @Fetch(FetchMode.JOIN)
        private Password password;

        @ManyToMany(fetch = FetchType.EAGER, cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
        @JoinTable(name = "cd_person_role", schema = "cd", joinColumns = @JoinColumn(name = "person_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
//        @Fetch(FetchMode.JOIN)
        private Set<Role> roles = new HashSet<>();
    }

    @Entity
    @Table(name = "cd_password", schema = "cd")
    @Data
    @NoArgsConstructor
    public class Password {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id", updatable = false, nullable = false)
        private Long id;

        @Column(name = "password_hash", nullable = false)
        private String passwordHash;
    }

    @Entity
    @Table(name = "cd_role", schema = "cd")
    @Data
    @NoArgsConstructor
    public class Role {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "role_type")
        @Enumerated(EnumType.STRING)
        private RoleType roleType;
    }

    enum RoleType {
        TYPE1, TYPE2
    }
}
