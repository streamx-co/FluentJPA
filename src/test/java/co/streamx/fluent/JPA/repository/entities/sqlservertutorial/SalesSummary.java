package co.streamx.fluent.JPA.repository.entities.sqlservertutorial;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
