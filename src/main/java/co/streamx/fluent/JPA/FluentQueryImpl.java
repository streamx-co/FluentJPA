package co.streamx.fluent.JPA;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;

import co.streamx.fluent.JPA.vendor.ResultTransformerInstaller;
import co.streamx.fluent.JPA.vendor.TupleResultTransformer;
import co.streamx.fluent.extree.expression.Expression;
import co.streamx.fluent.extree.expression.LambdaExpression;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class FluentQueryImpl implements FluentQuery {

private static final String HIBERNATE5_RESULT_TRANSFORMER_INSTALLER = "co.streamx.fluent.JPA.vendor.Hibernate5ResultTransformerInstaller";
    //    private final LambdaExpression<?> expression;
//    private final boolean sql;
    private final String qlString;
    private final List<Object> parameters;
    private final static ResultTransformerInstaller resultTransformerInstaller;
    private Map<Object, TemporalType> temporalTypes = Collections.emptyMap();

    static {

        ResultTransformerInstaller x;

        try {
            Class<?> HN5ResultTransformer = Class.forName(HIBERNATE5_RESULT_TRANSFORMER_INSTALLER);
            x = ResultTransformerInstaller.class.cast(HN5ResultTransformer.newInstance());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            x = null;
        }

        resultTransformerInstaller = x;

    }

    public FluentQueryImpl(LambdaExpression<?> expression, boolean sql) {
        expression = (LambdaExpression<?>) Normalizer.get().visit(expression);
        log.debug("query: {}", expression);
        DSLInterpreter dsl = new DSLInterpreter(FluentJPA.getCapabilities());
        Function<List<Expression>, Function<List<CharSequence>, CharSequence>> fvisited = dsl.visit(expression);
        Function<List<CharSequence>, CharSequence> visited = fvisited.apply(Collections.emptyList());
        qlString = visited.apply(Collections.emptyList()).toString().trim();
        parameters = Collections.unmodifiableList(dsl.getIndexedParameters());
    }

    public String getJPQL() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return qlString;
    }

    @Override
    public List<Object> getOrdinalParameters() {
        return parameters;
    }

    @Override
    public Query createQuery(EntityManager em) {
        return bindParameters(em.createNativeQuery(toString()));
    }

    @Override
    public <X> TypedQuery<X> createQuery(EntityManager em,
                                         Class<X> resultClass) {
        boolean isEntity = resultClass.isAnnotationPresent(Entity.class) || resultClass == Tuple.class;
        TupleResultTransformer<X> transformer = null;

        if (!isEntity && resultTransformerInstaller != null) {
            transformer = new TupleResultTransformerImpl<>(resultClass);

            @SuppressWarnings("unchecked")
            Class<X> tempClass = (Class<X>) Tuple.class;
            resultClass = tempClass;
        } else {
            isEntity = true;
        }

        Query query = em.createNativeQuery(toString(), resultClass);
        if (!isEntity) {
            resultTransformerInstaller.install(query, transformer);
        }

        @SuppressWarnings("unchecked")
        TypedQuery<X> xquery = query instanceof TypedQuery ? (TypedQuery<X>) query : new TypedQueryWrapper<>(query);
        return bindParameters(xquery);
    }

    private <T extends Query> T bindParameters(T nativeQuery) {
        for (int i = 0; i < parameters.size(); i++) {
            Object param = parameters.get(i);
            if (param instanceof Date) {
                Date date = (Date) param;
                nativeQuery.setParameter(i + 1, date, getTemporalType(date));
                continue;
            }

            if (param instanceof Calendar) {
                Calendar cal = (Calendar) param;
                nativeQuery.setParameter(i + 1, cal, getTemporalType(cal));
                continue;
            }

            nativeQuery.setParameter(i + 1, param);
        }
        return nativeQuery;
    }

    private TemporalType getTemporalType(Date param) {
        TemporalType type = temporalTypes.get(param);
        if (type != null)
            return type;

        if (param instanceof Time)
            return TemporalType.TIME;

        if (param instanceof Timestamp)
            return TemporalType.TIMESTAMP;

        return TemporalType.DATE;
    }

    private TemporalType getTemporalType(Calendar param) {
        return coalesce(temporalTypes.get(param), TemporalType.DATE);
    }

    private static <T> T coalesce(T t1,
                                  T t2) {
        return t1 != null ? t1 : t2;
    }

    @Override
    public void setParameterTemporalType(Date param,
                                         TemporalType type) {
        temporalTypes.put(param, type);
    }

    @Override
    public TemporalType getParameterTemporalType(Date param) {
        return temporalTypes.get(param);
    }

    @Override
    public void setParameterTemporalType(Calendar param,
                                         TemporalType type) {
        temporalTypes.put(param, type);
    }

    @Override
    public TemporalType getParameterTemporalType(Calendar param) {
        return temporalTypes.get(param);
    }
}
