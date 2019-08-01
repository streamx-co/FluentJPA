package co.streamx.fluent.JPA.spi;

import java.lang.reflect.Executable;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import co.streamx.fluent.extree.expression.Expression;
import co.streamx.fluent.extree.expression.InvocationExpression;
import co.streamx.fluent.extree.expression.LambdaExpression;
import co.streamx.fluent.extree.expression.MemberExpression;
import co.streamx.fluent.extree.expression.UnaryExpression;
import co.streamx.fluent.functions.Function0;
import co.streamx.fluent.functions.Function1;
import co.streamx.fluent.functions.Function2;
import co.streamx.fluent.functions.Function3;
import lombok.Getter;

public final class SQLConfiguratorImpl implements SQLConfigurator {

    private final static Map<String, Map<Executable, LambdaExpression<?>>> substitutions_ = new ConcurrentHashMap<>();

    @Getter
    private final static Map<String, Map<Executable, LambdaExpression<?>>> substitutions = Collections
            .unmodifiableMap(substitutions_);

    @Override
    public <T1, T2, T3, R> void registerMethodSubstitution(Function3<T1, T2, T3, R> from,
                                                           Function3<? super T1, ? super T2, ? super T3, ? extends R> to) {

        registerMethodSubstitution0(from, to);

    }

    @Override
    public <T, U, R> void registerMethodSubstitution(Function2<T, U, R> from,
                                                     Function2<? super T, ? super U, ? extends R> to) {

        registerMethodSubstitution0(from, to);

    }

    @Override
    public <T, R> void registerMethodSubstitution(Function1<T, R> from,
                                                  Function1<? super T, ? extends R> to) {

        registerMethodSubstitution0(from, to);

    }

    @Override
    public <R> void registerMethodSubstitution(Function0<R> from,
                                               Function0<? extends R> to) {

        registerMethodSubstitution0(from, to);

    }

    @Override
    public <T1, T2, T3, R> boolean unregisterMethodSubstitution(Function3<T1, T2, T3, R> from) {
        return unregisterMethodSubstitution0(from);
    }

    @Override
    public <T, U, R> boolean unregisterMethodSubstitution(Function2<T, U, R> from) {
        return unregisterMethodSubstitution0(from);
    }

    @Override
    public <T, R> boolean unregisterMethodSubstitution(Function1<T, R> from) {
        return unregisterMethodSubstitution0(from);
    }

    @Override
    public <R> boolean unregisterMethodSubstitution(Function0<R> from) {
        return unregisterMethodSubstitution0(from);
    }

    private static <T> void registerMethodSubstitution0(T from,
                                                        T to) {

        LambdaExpression<?> parsedFrom = LambdaExpression.parse(from);
        LambdaExpression<?> parsedTo = LambdaExpression.parse(to);

        Expression arg = parsedFrom.getBody();
        while (arg instanceof UnaryExpression)
            arg = ((UnaryExpression) arg).getFirst();
        InvocationExpression ie = (InvocationExpression) arg;
        MemberExpression me = (MemberExpression) ie.getTarget();
        Executable m = (Executable) me.getMember();

        Map<Executable, LambdaExpression<?>> subByName = substitutions_.computeIfAbsent(m.getName(),
                key -> new ConcurrentHashMap<>());

        subByName.put(m, parsedTo);
    }

    private static <T> boolean unregisterMethodSubstitution0(T from) {

        LambdaExpression<?> parsedFrom = LambdaExpression.parse(from);

        InvocationExpression ie = (InvocationExpression) parsedFrom.getBody();
        MemberExpression me = (MemberExpression) ie.getTarget();
        Executable m = (Executable) me.getMember();

        Map<Executable, LambdaExpression<?>> subByName = substitutions_.get(m.getName());

        if (subByName == null)
            return false;

        return subByName.remove(m) != null;
    }

}
