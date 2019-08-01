package co.streamx.fluent.JPA.repository.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity(name = "Phone")
@Table(name = "phone")
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Phone {
    @Id
    @Column(name = "num")
    @EqualsAndHashCode.Include
    private String number;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({ @JoinColumn(name = "company_id", referencedColumnName = "company_id"),
            @JoinColumn(name = "employee_number", referencedColumnName = "employee_number") })
    private Employee employee;
}
