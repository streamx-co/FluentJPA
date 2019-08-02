package co.streamx.fluent.JPA;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import lombok.ToString;

/**
 * Interface used to create {@link Query} or {@link TypedQuery} instances
 */
public interface FluentQuery {

    /**
     * @see ToString
     * @return Parameters ordered as passed to the resulting expression
     */
    List<Object> getOrdinalParameters();

    /**
     * Creates an instance of {@link Query}
     * 
     * @see EntityManager#createNativeQuery(String)
     * @param em {@link EntityManager}
     * @return the new query instance
     */
    Query createQuery(EntityManager em);

    /**
     * Create an instance of {@link TypedQuery}.
     * 
     * @see EntityManager#createNativeQuery(String, Class)
     * @param em          {@link EntityManager}
     * @param resultClass the class of the resulting instance(s)
     * @return the new query instance
     */
    <X> TypedQuery<X> createQuery(EntityManager em,
                                  Class<X> resultClass);

    /**
     * Set {@link TemporalType} for the parameter. Must be set before calling {@link #createQuery(EntityManager)}
     * <p>
     * By default {@link Timestamp} {@code param} will be assigned as {@link TemporalType#TIMESTAMP},<br>
     * {@link Time} {@code param} as {@link TemporalType#TIME}, otherwise as {@link TemporalType#DATE}
     */
    void setParameterTemporalType(Date param,
                                  TemporalType type);

    /**
     * Retrieve previously set {@link TemporalType}
     */
    TemporalType getParameterTemporalType(Date param);

    /**
     * Set {@link TemporalType} for the parameter. Must be set before calling {@link #createQuery(EntityManager)}
     * <p>
     * By default {@code param} will be assigned as {@link TemporalType#DATE}
     */
    void setParameterTemporalType(Calendar param,
                                  TemporalType type);

    /**
     * Retrieve previously set {@link TemporalType}
     */
    TemporalType getParameterTemporalType(Calendar param);

    /**
     * @return The resulting SQL or JPQL expression
     */
    @Override
    String toString();
}
