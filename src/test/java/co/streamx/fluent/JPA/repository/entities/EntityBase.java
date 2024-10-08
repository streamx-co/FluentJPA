package co.streamx.fluent.JPA.repository.entities;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import lombok.Data;

@MappedSuperclass
@Data
public abstract class EntityBase {
    @Id
    private Long id;
}
