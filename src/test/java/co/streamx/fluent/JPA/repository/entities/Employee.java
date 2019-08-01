package co.streamx.fluent.JPA.repository.entities;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

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
