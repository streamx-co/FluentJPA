package co.streamx.fluent.JPA.repository.entities.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "Employee_enum", schema = "JPA")
public class EmployeeEnumerated {

    public enum EmployeeStatus {
        FULL_TIME, PART_TIME, CONTRACT
    }

    public enum SalaryRate {
        JUNIOR, SENIOR, MANAGER, EXECUTIVE
    }

    @Id
    private Integer id;

    private EmployeeStatus status;

    @Column(name = "salary_rate")
    @Enumerated(EnumType.STRING)
    private SalaryRate payScale;

}

