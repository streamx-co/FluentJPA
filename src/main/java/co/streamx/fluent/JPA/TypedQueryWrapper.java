package co.streamx.fluent.JPA;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.Query;
import jakarta.persistence.TemporalType;
import jakarta.persistence.TypedQuery;

import lombok.AllArgsConstructor;

@AllArgsConstructor
final class TypedQueryWrapper<X> implements TypedQuery<X> {

    private Query query;

    @Override
    public int executeUpdate() {
        return query.executeUpdate();
    }

    @Override
    public int getMaxResults() {
        return query.getMaxResults();
    }

    @Override
    public int getFirstResult() {
        return query.getFirstResult();
    }

    @Override
    public Map<String, Object> getHints() {
        return query.getHints();
    }

    @Override
    public Set<Parameter<?>> getParameters() {
        return query.getParameters();
    }

    @Override
    public Parameter<?> getParameter(String name) {
        return query.getParameter(name);
    }

    @Override
    public <T> Parameter<T> getParameter(String name,
                                         Class<T> type) {
        return query.getParameter(name, type);
    }

    @Override
    public Parameter<?> getParameter(int position) {
        return query.getParameter(position);
    }

    @Override
    public <T> Parameter<T> getParameter(int position,
                                         Class<T> type) {
        return query.getParameter(position, type);
    }

    @Override
    public boolean isBound(Parameter<?> param) {
        return query.isBound(param);
    }

    @Override
    public <T> T getParameterValue(Parameter<T> param) {
        return query.getParameterValue(param);
    }

    @Override
    public Object getParameterValue(String name) {
        return query.getParameterValue(name);
    }

    @Override
    public Object getParameterValue(int position) {
        return query.getParameterValue(position);
    }

    @Override
    public FlushModeType getFlushMode() {
        return query.getFlushMode();
    }

    @Override
    public LockModeType getLockMode() {
        return query.getLockMode();
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        return query.unwrap(cls);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<X> getResultList() {
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public X getSingleResult() {
        return (X) query.getSingleResult();
    }

    @Override
    public TypedQuery<X> setMaxResults(int maxResult) {
        query = query.setMaxResults(maxResult);
        return this;
    }

    @Override
    public TypedQuery<X> setFirstResult(int startPosition) {
        query = query.setFirstResult(startPosition);
        return this;
    }

    @Override
    public TypedQuery<X> setHint(String hintName,
                                 Object value) {
        query = query.setHint(hintName, value);
        return this;
    }

    @Override
    public <T> TypedQuery<X> setParameter(Parameter<T> param,
                                          T value) {
        query = query.setParameter(param, value);
        return this;
    }

    @Override
    public TypedQuery<X> setParameter(Parameter<Calendar> param,
                                      Calendar value,
                                      TemporalType temporalType) {
        query = query.setParameter(param, value, temporalType);
        return this;
    }

    @Override
    public TypedQuery<X> setParameter(Parameter<Date> param,
                                      Date value,
                                      TemporalType temporalType) {
        query = query.setParameter(param, value, temporalType);
        return this;
    }

    @Override
    public TypedQuery<X> setParameter(String name,
                                      Object value) {
        query = query.setParameter(name, value);
        return this;
    }

    @Override
    public TypedQuery<X> setParameter(String name,
                                      Calendar value,
                                      TemporalType temporalType) {
        query = query.setParameter(name, value, temporalType);
        return this;
    }

    @Override
    public TypedQuery<X> setParameter(String name,
                                      Date value,
                                      TemporalType temporalType) {
        query = query.setParameter(name, value, temporalType);
        return this;
    }

    @Override
    public TypedQuery<X> setParameter(int position,
                                      Object value) {
        query = query.setParameter(position, value);
        return this;
    }

    @Override
    public TypedQuery<X> setParameter(int position,
                                      Calendar value,
                                      TemporalType temporalType) {
        query = query.setParameter(position, value, temporalType);
        return this;
    }

    @Override
    public TypedQuery<X> setParameter(int position,
                                      Date value,
                                      TemporalType temporalType) {
        query = query.setParameter(position, value, temporalType);
        return this;
    }

    @Override
    public TypedQuery<X> setFlushMode(FlushModeType flushMode) {
        query = query.setFlushMode(flushMode);
        return this;
    }

    @Override
    public TypedQuery<X> setLockMode(LockModeType lockMode) {
        query = query.setLockMode(lockMode);
        return this;
    }

}
