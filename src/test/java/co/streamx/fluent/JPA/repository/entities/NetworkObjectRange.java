package co.streamx.fluent.JPA.repository.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "NETWORK_OBJECT_RANGE")
public class NetworkObjectRange extends EntityBase {
    private Long first;
    private Long last;
    @ManyToOne
    @JoinColumn(name = "NETWORK_OBJ")
    private NetworkObject networkObject;
}
