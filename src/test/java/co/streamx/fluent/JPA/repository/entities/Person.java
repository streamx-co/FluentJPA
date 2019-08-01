package co.streamx.fluent.JPA.repository.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "PERSON_TABLE")
public class Person extends EntityBase {
    private String name;
    @Column(name = "aging")
    private int age;
    private Integer height = 1;
    @Column(name = "balancer")
    private boolean isLoadBalancer;

    public boolean isAdult() {
        return age >= 18;
    }
}
