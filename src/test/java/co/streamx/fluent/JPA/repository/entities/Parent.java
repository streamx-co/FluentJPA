package co.streamx.fluent.JPA.repository.entities;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@IdClass(Parent.Key.class)
@Table(name = "Parent")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class Parent {
    @OneToMany(targetEntity = Child.class, mappedBy = "parent", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    private Set<Child> childs = new HashSet<>();

    @Id
    @Column(name = "\"co de\"", length = 4)
    @EqualsAndHashCode.Include
    private String code;

    @Id
    @EqualsAndHashCode.Include
    private int id;

    @SuppressWarnings("serial")
    @NoArgsConstructor
    @AllArgsConstructor
    public final static class Key implements Serializable {
        private String code;
        private int id;
    }
}
