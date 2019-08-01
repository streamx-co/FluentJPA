package co.streamx.fluent.JPA.repository.entities.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

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

