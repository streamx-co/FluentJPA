package co.streamx.fluent.JPA;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import co.streamx.fluent.JPA.vendor.TupleResultTransformer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
class TupleResultTransformerImpl<T> implements TupleResultTransformer<T> {

    @RequiredArgsConstructor
    @Getter
    private static class SetterInfo {
        private final Method setter;
        private final Class<?> type;
    }

    private static final Map<Class<?>, Map<String, SetterInfo>> transformMetaDataCache = new ConcurrentHashMap<>();

    private final Class<T> targetType;
    private final Map<String, SetterInfo> aliasesToMethods;

    public TupleResultTransformerImpl(Class<T> targetType) {
        this.targetType = targetType;

        this.aliasesToMethods = transformMetaDataCache.computeIfAbsent(targetType, type -> {
            return Arrays.stream(type.getMethods()).filter(m -> {

                if (m.getDeclaringClass() == Object.class)
                    return false;

                if (m.getParameterCount() > 0)
                    return false;

                String name = m.getName();
                return name.startsWith("is") || name.startsWith("get");
            })
                    .collect(Collectors.toMap(m -> JPAHelpers.getColumnNameFromProperty(m).current().toString(),
                            m -> new SetterInfo(setterFromGetter(m), m.getReturnType())));
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

            SetterInfo info = aliasesToMethods.get(alias.toLowerCase(Locale.ROOT));
            if (info == null)
                throw new IndexOutOfBoundsException("Alias '" + alias + "' for column " + i + " not found");
            info.getSetter().invoke(x, defaultConvert(value, info.getType()));
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

    @SneakyThrows
    private static Method setterFromGetter(Method getter) {
        Class<?> type = getter.getDeclaringClass();
        String setterName;
        String name = getter.getName();
        if (name.charAt(0) == 'g') {
            StringBuilder b = new StringBuilder(name);
            b.setCharAt(0, 's');
            setterName = b.toString();
        } else {
            setterName = new StringBuilder("set").append(name.substring(4)).toString();
        }

        return type.getMethod(setterName, getter.getReturnType());
    }
}
