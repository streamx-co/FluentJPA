package co.streamx.fluent.JPA;

import java.lang.reflect.AnnotatedElement;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.persistence.MapsId;
import javax.persistence.Transient;

import co.streamx.fluent.JPA.JPAHelpers.Association;
import co.streamx.fluent.JPA.JPAHelpers.ClassMeta;
import co.streamx.fluent.JPA.JPAHelpers.ID;
import co.streamx.fluent.JPA.vendor.TupleResultTransformer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
class TupleResultTransformerImpl<T> implements TupleResultTransformer<T>, TupleResultTransformerImplHelpers {

    @RequiredArgsConstructor
    @lombok.Getter
    private static class PropertyInfo {
        private final Setter setter;
        private final String column;
    }

    private static final Map<Class<?>, Map<String, PropertyInfo>> transformMetaDataCache = new ConcurrentHashMap<>();

    private final Class<T> targetType;
    private final Map<String, PropertyInfo> aliasesToMethods;

    public TupleResultTransformerImpl(Class<T> targetType) {
        this.targetType = targetType;

        this.aliasesToMethods = transformMetaDataCache.computeIfAbsent(targetType, type -> {

            return getProperties(type, null).collect(Collectors.toMap(PropertyInfo::getColumn, Function.identity()));
        });

    }

    private Stream<PropertyInfo> getProperties(Class<?> type,
                                               Setter baseSetter) {
        return Stream.of(type.getMethods()).filter(m -> {

            if (m.getDeclaringClass() == Object.class)
                return false;

            if (m.getParameterCount() > 0)
                return false;

            if (m.getReturnType().isAssignableFrom(Collection.class))
                return false;

            String name = m.getName();
            return name.startsWith("is") || name.startsWith("get");
        }).map(JPAHelpers::getAnnotatedField).filter(m -> {
            AnnotatedElement ae = (AnnotatedElement) m;
            if (ae.isAnnotationPresent(Transient.class))
                return false;

            return !ae.isAnnotationPresent(MapsId.class);
        }).flatMap(m -> {

            Setter setter = TupleResultTransformerImplHelpers.getSetter(m, baseSetter);

            Class<?> propertyType = setter.getType();
            if (JPAHelpers.isEntityLike(propertyType)) {

                Association assoc = JPAHelpers.getAssociation(m);
                ClassMeta propMeta = JPAHelpers.getClassMeta(propertyType);

                return IntStream.range(0, assoc.getCardinality()).mapToObj(i -> {
                    String column = assoc.getLeft().get(i).toString().toUpperCase(Locale.ROOT);
                    CharSequence other = assoc.getRight().get(i);

                    ID foundId = Streams.find(propMeta.getIds(), id -> Strings.equals(other, id.getColumn()));

                    Setter nestedSetter = TupleResultTransformerImplHelpers.getSetter(foundId.getMember(), setter);

                    return new PropertyInfo(nestedSetter, column);
                });
            }

            if (JPAHelpers.isEmbedded(m)) {
                return getProperties(propertyType, setter);
            }

            PropertyInfo propertyInfo = new PropertyInfo(setter,
                    JPAHelpers.getColumnNameFromProperty(m).current().toString().toUpperCase(Locale.ROOT));
            return Stream.of(propertyInfo);
        });
    }

    /**
     * Tuples are the elements making up each "row" of the query result. The contract here is to transform these
     * elements into the final row.
     *
     * @param tuple   The result elements
     * @param aliases The result aliases ("parallel" array to tuple)
     * @return The transformed row.
     */
    @SneakyThrows
    public T transformTuple(Object[] tuple,
                            String[] aliases) {
        T x = targetType.newInstance();

        for (int i = 0; i < tuple.length; i++) {

            Object value = tuple[i];
            if (value == null)
                continue;

            String alias = aliases[i];
            if (Strings.isNullOrEmpty(alias))
                throw new IllegalArgumentException("No alias for column " + i);

            PropertyInfo info = aliasesToMethods.get(alias.toUpperCase(Locale.ROOT));
            if (info == null)
                throw new IndexOutOfBoundsException("Alias '" + alias + "' for column " + i + " not found");
            info.getSetter().set(x, defaultConvert(value, info.getSetter().getType()));
        }

        return x;
    }

    /**
     * Here we have an opportunity to perform transformation on the query result as a whole. This might be useful to
     * convert from one collection type to another or to remove duplicates from the result, etc.
     *
     * @param collection The result.
     * @return The transformed result.
     */
    public List<T> transformList(List<T> collection) {
        return collection;
    }

    private static Object defaultConvert(Object value,
                                         Class<?> target) {
        target = JPAHelpers.wrap(target);
        if (!target.isAssignableFrom(value.getClass())) {
            if (Number.class.isAssignableFrom(target) && Number.class.isAssignableFrom(value.getClass())) {
                Number n = (Number) value;
                if (target == Byte.class)
                    return n.byteValue();
                if (target == Short.class)
                    return n.shortValue();
                if (target == Integer.class)
                    return n.intValue();
                if (target == Long.class)
                    return n.longValue();
                if (target == Float.class)
                    return n.floatValue();
                if (target == Double.class)
                    return n.doubleValue();
                if (target == BigInteger.class)
                    return BigInteger.valueOf(n.longValue());
                if (target == BigDecimal.class)
                    return BigDecimal.valueOf(n.doubleValue());
            } else if (target == String.class && value.getClass() == Character.class) {
                return String.valueOf((char) value);
            } else if (target == Character.class && value.getClass() == String.class) {
                String string = (String) value;
                return string.length() > 0 ? string.charAt(0) : null;
            }
        }
        return value;
    }
}
