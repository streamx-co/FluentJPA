package co.streamx.fluent.JPA.repository.entities;

import java.io.Serializable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@IdClass(Child.Key.class)
@Table(name = "Child")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class Child {
    @Id
    @Column(length = 4)
    @EqualsAndHashCode.Include
    private String code;

    @Id
    @EqualsAndHashCode.Include
    private int id;

    @Id
    @EqualsAndHashCode.Include
    private int index;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumns({ @JoinColumn(name = "code", insertable = false, updatable = false, referencedColumnName="\"co de\""),
            @JoinColumn(name = "id", insertable = false, updatable = false, referencedColumnName="id") })
    private Parent parent;

    @SuppressWarnings("serial")
    @NoArgsConstructor
    @AllArgsConstructor
    public final static class Key implements Serializable {
        private String code;
        private int id;
        private int index;
    }
}
