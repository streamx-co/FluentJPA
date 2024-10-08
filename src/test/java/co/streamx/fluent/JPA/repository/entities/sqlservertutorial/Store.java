package co.streamx.fluent.JPA.repository.entities.sqlservertutorial;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "stores")
public class Store  {
    @Id
    @Column(name = "store_id")
    private Integer id;

    @Column(name = "store_name")
    private String name;
}
