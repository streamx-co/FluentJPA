package co.streamx.fluent.JPA.repository.entities;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("serial")
@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeId implements Serializable {

    @ManyToOne()
//    @JoinColumn(name = "company_id")
    private Company companyId;

    @Column(name = "employee_number")
    private Long employeeNumber;

}
