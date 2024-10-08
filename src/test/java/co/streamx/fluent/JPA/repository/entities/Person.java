package co.streamx.fluent.JPA.repository.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "PERSON_TABLE")
public class Person extends EntityBase {
    private String name;

    @Column(name = "aging")
    private Integer age;
    private Integer height = 1;
    @Column(name = "balancer")
    private boolean isLoadBalancer;

    public boolean isAdult() {
        return age >= 18;
    }
}
