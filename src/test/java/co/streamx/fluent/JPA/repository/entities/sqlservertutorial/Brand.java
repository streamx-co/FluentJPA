package co.streamx.fluent.JPA.repository.entities.sqlservertutorial;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
