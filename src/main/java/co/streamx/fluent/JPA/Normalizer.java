package co.streamx.fluent.JPA;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import co.streamx.fluent.JPA.spi.SQLConfiguratorImpl;
import co.streamx.fluent.extree.expression.BinaryExpression;
import co.streamx.fluent.extree.expression.ConstantExpression;
import co.streamx.fluent.extree.expression.Expression;
import co.streamx.fluent.extree.expression.ExpressionType;
import co.streamx.fluent.extree.expression.InvocableExpression;
import co.streamx.fluent.extree.expression.InvocationExpression;
import co.streamx.fluent.extree.expression.LambdaExpression;
import co.streamx.fluent.extree.expression.MemberExpression;
import co.streamx.fluent.extree.expression.NewArrayInitExpression;
import co.streamx.fluent.extree.expression.ParameterExpression;
import co.streamx.fluent.extree.expression.SimpleExpressionVisitor;
import co.streamx.fluent.extree.expression.UnaryExpression;
import co.streamx.fluent.notation.Literal;
import co.streamx.fluent.notation.Local;
import co.streamx.fluent.notation.Notation;
import co.streamx.fluent.notation.ViewDeclaration;
import lombok.SneakyThrows;


class Normalizer extends SimpleExpressionVisitor {
    public static final String DYNAMIC_QUERIES_FUNCTIONALITY = "Dynamic Queries functionality";
    private static final String METHODS_PARSING_FEATURE = "Methods parsing feature";
    private static final String COMPARE_TO = "compareTo";
    private final Method compareToMethod;

    public static Normalizer get() {
        return new Normalizer();
    }

    @SneakyThrows
    private Normalizer() {
            compareToMethod = Comparable.class.getMethod(COMPARE_TO, Object.class);
    }

    private static boolean isFunctionalInterfaceCall(Member member) {

        Class<?> declaringClass = member.getDeclaringClass();
        return declaringClass.isInterface() && declaringClass.isAnnotationPresent(FunctionalInterface.class);
    }

    @Override
    @SneakyThrows
    public Expression visit(MemberExpression e) {
        Member member = e.getMember();

        if (!(member instanceof Method)) {

            if (member instanceof Field && Modifier.isStatic(member.getModifiers())) {
                Field f = (Field) member;

                Object value;

                try {
                    value = f.get(null);
                } catch (IllegalAccessException iae) {
                    throw new RuntimeException("Cannot access: " + f.getName() + ". " + iae.getMessage(), iae);
                }
                Class<?> clazz = value.getClass();
                if (clazz.isAnnotationPresent(Literal.class))
                    value = value.toString();
                ConstantExpression c = Expression.constant(value, clazz);
                return c;
            }

            return super.visit(e);
        }

        Method method = (Method) member;

        if (isNotation(method))
            return super.visit(e);

        if (Modifier.isStatic(method.getModifiers()) || method.isDefault()) {
            if (!FluentJPA.checkLicense()) {
                String name = method.getDeclaringClass().getName();
                if (!name.startsWith("co.streamx.fluent.SQL."))
                    throw TranslationError.REQUIRES_LICENSE.getError(METHODS_PARSING_FEATURE);
            }
            LambdaExpression<?> parsed = LambdaExpression.parseMethod(method, e.getInstance());
            return visit(parsed);
        }

        if (isFunctionalInterfaceCall(method)) {
            if (!FluentJPA.checkLicense())
                throw TranslationError.REQUIRES_LICENSE.getError(DYNAMIC_QUERIES_FUNCTIONALITY);
            Expression instance = e.getInstance();
            if (instance != null && instance.getExpressionType() == ExpressionType.Parameter) {
                int index = ((ParameterExpression) instance).getIndex();
                return super.visit(Expression.delegate(e.getResultType(),
                        Expression.parameter(LambdaExpression.class, index), e.getParameters()));
            }
        }

        return super.visit(e);
    }

    @Override
    protected Expression invoke(InvocableExpression target,
                                List<Expression> args,
                                InvocationExpression original) {
        InvocableExpression originalTarget = original.getTarget();
        if (target instanceof LambdaExpression && originalTarget instanceof MemberExpression) {
            // was parsed. Potentially requires passing instance as 1st parameter
            if (target.getParameters().size() > originalTarget.getParameters().size()) {
                MemberExpression member = (MemberExpression) originalTarget;
                Expression instance = member.getInstance();
                if (instance != null) {
                    List<Expression> newArgs = new ArrayList<>(args.size() + 1);
                    newArgs.add(instance.accept(this));
                    newArgs.addAll(args);
                    args = newArgs;
                }
            }
        }
        return super.invoke(target, args, original);
    }

    @Override
    public Expression visit(InvocationExpression e) {
        InvocableExpression target = e.getTarget();
        if (!(target instanceof MemberExpression))
            return super.visit(e);

        MemberExpression memberExpression = (MemberExpression) target;
        Member member = memberExpression.getMember();
        if (!(member instanceof Executable))
            return super.visit(e);

        Executable method = (Executable) member;

        if (method.isAnnotationPresent(Local.class)) {
            if (!FluentJPA.checkLicense())
                throw TranslationError.REQUIRES_LICENSE.getError(DYNAMIC_QUERIES_FUNCTIONALITY);
            Object result = LambdaExpression.compile(e).apply(contextArgumentsArray(e.getArguments()));
            boolean isSynthetic = result != null && isFunctional(result.getClass());
            if (isSynthetic) {
                LambdaExpression<?> parsed = LambdaExpression.parse(result);
                return visit(parsed);
            }

            return Expression.constant(result);
        }

        Map<String, Map<Executable, LambdaExpression<?>>> substitutions = SQLConfiguratorImpl.getSubstitutions();
        Map<Executable, LambdaExpression<?>> subByName = substitutions.get(method.getName());
        if (subByName != null) {
            for (Executable m : subByName.keySet()) {
                if (isSameOrDerived(m, method)) {
                    LambdaExpression<?> substition = subByName.get(m);
                    Expression instance = memberExpression.getInstance();
                    List<Expression> args = e.getArguments();
                    if (instance != null) {
                        List<Expression> newArgs = new ArrayList<>();
                        newArgs.add(instance);
                        newArgs.addAll(args);
                        args = newArgs;
                    }

                    e = Expression.invoke(substition, args);
                    break;
                }
            }
        }

        Expression visited = super.visit(e);
        if (method.isAnnotationPresent(ViewDeclaration.class)) {

            Expression entity = e.getArguments().get(0);
            List<Expression> args = Collections.singletonList(entity);
            NewArrayInitExpression aliases = (NewArrayInitExpression) e.getArguments().get(1);
            List<Expression> resolvedAliases = aliases.getInitializers()
                    .stream()
                    .map(ex -> Expression.invoke((LambdaExpression<?>) ex, args))
                    .collect(Collectors.toList());

            aliases = Expression.newArrayInit(aliases.getComponentType(), resolvedAliases);
            visited = Expression.invoke(target, entity, aliases);

        }
        return visited;
    }

    private static boolean isFunctional(Class<?> clazz) {
        if (clazz.isSynthetic())
            return true;

        for (Class<?> i : clazz.getInterfaces())
            if (i.isAnnotationPresent(FunctionalInterface.class))
                return true;

        return false;
    }

    private Object[] contextArgumentsArray(List<Expression> args) {
        if (args.isEmpty())
            return null;
        List<Expression> contextArguments = getContextArguments();
        return args.stream()
                .map(e -> {
                    Expression x = removeCast(e);
                    if (x instanceof ParameterExpression)
                        x = contextArguments.get(((ParameterExpression) x).getIndex());

                    if (!(x instanceof ConstantExpression))
                        throw TranslationError.REQUIRES_EXTERNAL_PARAMETER.getError(x);

                    return ((ConstantExpression) x).getValue();
                })
                .toArray();
    }

    private static Expression removeCast(Expression x) {
        return (x != null && x.getExpressionType() == ExpressionType.Convert)
                ? removeCast(((UnaryExpression) x).getFirst())
                : x;
    }

    private static boolean isNotation(AnnotatedElement annotated) {
        for (Annotation a : annotated.getDeclaredAnnotations())
            if (a.annotationType().isAnnotationPresent(Notation.class))
                return true;

        return false;
    }

    @Override
    public Expression visit(BinaryExpression e) {
        int type = e.getExpressionType();
        switch (type) {
        case ExpressionType.LessThan:
        case ExpressionType.LessThanOrEqual:
        case ExpressionType.Equal:
        case ExpressionType.NotEqual:
        case ExpressionType.GreaterThan:
        case ExpressionType.GreaterThanOrEqual:
            if (isZero(e.getFirst()) && isCompareTo(e.getSecond())) {
                Expression comparison = createComparison(e.getExpressionType(), e.getSecond());
                return type == ExpressionType.Equal || type == ExpressionType.NotEqual ? comparison
                        : Expression.logicalNot(comparison);
            }

            if (isZero(e.getSecond()) && isCompareTo(e.getFirst()))
                return createComparison(type, e.getFirst());
        }
        return super.visit(e);
    }

    private Expression createComparison(int expressionType,
                                               Expression e) {
        InvocationExpression ie = (InvocationExpression) e;
        MemberExpression m = (MemberExpression) ie.getTarget();
        return Expression.binary(expressionType, m.getInstance(), ie.getArguments().get(0));
    }

    private boolean isCompareTo(Expression e) {
        if (!(e instanceof InvocationExpression))
            return false;

        InvocableExpression target = ((InvocationExpression) e).getTarget();
        if (!(target instanceof MemberExpression))
            return false;

        Member member = ((MemberExpression) target).getMember();
        return member instanceof Method && isSameOrDerived(compareToMethod, (Method) member);
    }

    private static boolean isZero(Expression e) {
        return e instanceof ConstantExpression && Objects.equals(0, ((ConstantExpression) e).getValue());
    }

    private static boolean isSameOrDerived(Executable base,
                                           Executable derived) {
        return Modifier.isStatic(base.getModifiers()) == Modifier.isStatic(derived.getModifiers())
                && base.getDeclaringClass().isAssignableFrom(derived.getDeclaringClass())
                && base.getName().equals(derived.getName()) && base.getParameterCount() == derived.getParameterCount();
    }
}
