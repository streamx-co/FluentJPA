package co.streamx.fluent.JPA.repository.entities.sqlservertutorial;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
