package co.streamx.fluent.JPA.repository.entities.sqlservertutorial;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "brands")
public class Brand  {
    @Id
    @Column(name = "brand_id")
    private Integer id;

    @Column(name = "brand_name")
    private String name;
}
