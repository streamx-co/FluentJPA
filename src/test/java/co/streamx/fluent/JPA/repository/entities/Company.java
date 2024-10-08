package co.streamx.fluent.JPA.repository.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
