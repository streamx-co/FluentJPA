package co.streamx.fluent.JPA.repository.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "Sales")
public class Sales extends EntityBase {
    private String country;
    private String region;
    private int sales;
}
