package co.streamx.fluent.JPA.repository.entities;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "Employee")
@Table(name = "employee")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Employee {
    @EmbeddedId
    private EmployeeId id;

    private String name;

    @ManyToOne
    @MapsId(value = "companyId")
    private Company company;
}
