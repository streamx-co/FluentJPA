package co.streamx.fluent.JPA.repository.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "OBJECT_CON")
public class ObjectContainer extends EntityBase {
    private String name;
}
