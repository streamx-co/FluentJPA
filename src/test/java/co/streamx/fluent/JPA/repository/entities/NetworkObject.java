package co.streamx.fluent.JPA.repository.entities;

import java.math.BigInteger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "NETWORK_OBJECT")
public class NetworkObject extends EntityBase {
    @ManyToOne
    @JoinColumn(name = "OBJECT_CON")
    private ObjectContainer objectContainer;
    @Column(name = "ip_count")
    private BigInteger ipCount;
    @Column(name = "object_internal_type")
    private int objectInternalType;
}
