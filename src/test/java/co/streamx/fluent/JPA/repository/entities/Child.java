package co.streamx.fluent.JPA.repository.entities;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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
    @JoinColumns({ @JoinColumn(name = "code", insertable = false, updatable = false),
            @JoinColumn(name = "id", insertable = false, updatable = false) })
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
