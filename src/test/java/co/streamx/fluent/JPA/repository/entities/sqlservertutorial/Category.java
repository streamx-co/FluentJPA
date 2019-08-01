package co.streamx.fluent.JPA.repository.entities.sqlservertutorial;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "categories")
public class Category  {
    @Id
    @Column(name = "category_id")
    private Integer id;

    @Column(name = "category_name")
    private String name;
}
