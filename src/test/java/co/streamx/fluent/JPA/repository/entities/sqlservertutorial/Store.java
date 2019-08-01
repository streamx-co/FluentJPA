package co.streamx.fluent.JPA.repository.entities.sqlservertutorial;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
