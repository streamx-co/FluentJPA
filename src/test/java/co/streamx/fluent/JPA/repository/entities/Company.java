package co.streamx.fluent.JPA.repository.entities;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "Company")
@Table(name = "company")
@Data
@NoArgsConstructor
public class Company {

    @Id
    private Long id;

    private String name;

}
