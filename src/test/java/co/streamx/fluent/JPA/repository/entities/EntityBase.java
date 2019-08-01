package co.streamx.fluent.JPA.repository.entities;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import lombok.Data;

@MappedSuperclass
@Data
public abstract class EntityBase {
    @Id
    private Long id;
}
