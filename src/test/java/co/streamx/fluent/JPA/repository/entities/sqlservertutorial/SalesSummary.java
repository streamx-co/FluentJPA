package co.streamx.fluent.JPA.repository.entities.sqlservertutorial;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "sales_summary")
public class SalesSummary  {
    @Id
    private Integer id;

    private String brand;

    private String category;

    @Column(name = "model_year")
    private short modelYear;

    private float sales;
}
