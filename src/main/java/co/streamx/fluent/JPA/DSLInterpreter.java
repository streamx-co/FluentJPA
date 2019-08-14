package co.streamx.fluent.JPA;

import static co.streamx.fluent.JPA.JPAHelpers.getAssociation;
import static co.streamx.fluent.JPA.JPAHelpers.getAssociationMTM;
import static co.streamx.fluent.JPA.JPAHelpers.getColumnNameFromProperty;
import static co.streamx.fluent.JPA.JPAHelpers.getJoinTableName;
import static co.streamx.fluent.JPA.JPAHelpers.getTableName;
import static co.streamx.fluent.JPA.JPAHelpers.isCollection;
import static co.streamx.fluent.JPA.JPAHelpers.isEmbeddable;
import static co.streamx.fluent.JPA.JPAHelpers.isEntityLike;
import static co.streamx.fluent.JPA.JPAHelpers.isScalar;
import static co.streamx.fluent.JPA.JPAHelpers.wrap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import co.streamx.fluent.JPA.JPAHelpers.Association;
import co.streamx.fluent.JPA.util.ScopedHashMap;
import co.streamx.fluent.extree.expression.BinaryExpression;
import co.streamx.fluent.extree.expression.BlockExpression;
import co.streamx.fluent.extree.expression.ConstantExpression;
import co.streamx.fluent.extree.expression.DelegateExpression;
import co.streamx.fluent.extree.expression.Expression;
import co.streamx.fluent.extree.expression.ExpressionType;
import co.streamx.fluent.extree.expression.ExpressionVisitor;
import co.streamx.fluent.extree.expression.InvocableExpression;
import co.streamx.fluent.extree.expression.InvocationExpression;
import co.streamx.fluent.extree.expression.LambdaExpression;
import co.streamx.fluent.extree.expression.MemberExpression;
import co.streamx.fluent.extree.expression.NewArrayInitExpression;
import co.streamx.fluent.extree.expression.ParameterExpression;
import co.streamx.fluent.extree.expression.UnaryExpression;
import co.streamx.fluent.notation.Alias;
import co.streamx.fluent.notation.Capability;
import co.streamx.fluent.notation.CommonTableExpression;
import co.streamx.fluent.notation.CommonTableExpressionType;
import co.streamx.fluent.notation.Context;
import co.streamx.fluent.notation.Literal;
import co.streamx.fluent.notation.Operator;
import co.streamx.fluent.notation.Parameter;
import co.streamx.fluent.notation.ParameterContext;
import co.streamx.fluent.notation.SubQuery;
import co.streamx.fluent.notation.TableJoin;
import co.streamx.fluent.notation.TableJoin.Property;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class DSLInterpreter
        implements ExpressionVisitor<Function<List<Expression>, Function<List<CharSequence>, CharSequence>>> {

    private static final Optional<Boolean> OPTIONAL_FALSE = Optional.of(false);
    private static final char UNDERSCORE_CHAR = '_';
    private static final char COMMA_CHAR = ',';
    private static final String AS = "AS";
    private static final String NEW_LINE = "\n";
    private static final String STAR = "*";
    private static final char QUESTION_MARK_CHAR = '?';
    private static final char DOT_CHAR = IdentifierPath.DOT;
    private static final String DOT = "" + DOT_CHAR;
    private static final String AND = "AND";
    private static final String EQUAL_SIGN = "=";
    private static final String KEYWORD_DELIMITER = " ";
    private static final String SEP_AS_SEP = KEYWORD_DELIMITER + AS + KEYWORD_DELIMITER;
    private static final char KEYWORD_DELIMITER_CHAR = ' ';
    private static final String TABLE_ALIAS_PREFIX = "t";
    private static final String SUB_QUERY_ALIAS_PREFIX = "q";
    private static final String LEFT_PARAN = "(";
    private static final String RIGHT_PARAN = ")";
    private static final char SINGLE_QUOTE_CHAR = '\'';

    @NonNull
    private final Set<Capability> capabilities;

    private SubQueryManager subQueries_ = new SubQueryManager(Collections.emptyList());
    private Set<CharSequence> parameterRefs = new HashSet<>();
    private Map<CharSequence, CharSequence> aliases_ = Collections.emptyMap();
    private CharSequence subQueryAlias;
    private List<CharSequence> undeclaredAliases = Collections.emptyList();
    private Expression argumentsTarget; // used to differentiate invocation vs. reference

    // join table
    private Map<ParameterExpression, Member> joinTables = Collections.emptyMap();
    private Map<CharSequence, Member> joinTablesForFROM = Collections.emptyMap();
    private Map<ParameterExpression, ParameterExpression> parameterBackwardMap = new HashMap<>();

    @Getter
    private List<Object> indexedParameters = new ArrayList<>();

    private ParameterContext renderingContext = ParameterContext.EXPRESSION;
    private Optional<Boolean> collectingParameters = OPTIONAL_FALSE;

    private int parameterCounter;
    private int subQueriesCounter;
    private boolean renderingAssociation;
    private boolean renderAliases; // TODO: use context per clause

    private void addUndeclaredAlias(CharSequence alias) {
        if (undeclaredAliases.isEmpty())
            undeclaredAliases = new ArrayList<>();

        undeclaredAliases.add(alias);
    }

    private ParameterContext getParameterContext(co.streamx.fluent.notation.Function f) {
        for (Capability cap : f.parameterContextCapabilities()) {
            if (capabilities.contains(cap))
                return (ParameterContext) cap.getHint();
        }
        return f.parameterContext();
    }

    protected SubQueryManager getSubQueries() {
        return subQueries_;
    }

    protected Map<CharSequence, CharSequence> getAliases() {
        return aliases_;
    }

    private static Expression bind(List<Expression> args,
                                   Expression e) {
        if (e instanceof ParameterExpression) {
            int index = ((ParameterExpression) e).getIndex();
            if (index < args.size()) {
                Expression bound = args.get(index);

                int boundExpressionType = bound.getExpressionType();
                if (boundExpressionType == ExpressionType.Parameter || boundExpressionType == ExpressionType.Constant) {
                    if ((bound.getResultType() != Object.class)
                            && (wrap(bound.getResultType()) != wrap(e.getResultType())))
                        return boundExpressionType == ExpressionType.Constant
                                ? Expression.parameter(bound.getResultType(), index)
                                : bound;
                } else {
                    e = bound;
                }
            }
        }

        return e;
    }

    @Override
    public Function<List<Expression>, Function<List<CharSequence>, CharSequence>> visit(BinaryExpression e) {

        return eargs -> {

            Expression first = bind(eargs, e.getFirst());
            Expression second = bind(eargs, e.getSecond());

            Function<List<Expression>, Function<List<CharSequence>, CharSequence>> efirst = first.accept(this);
            Function<List<Expression>, Function<List<CharSequence>, CharSequence>> esecond = second.accept(this);

            Function<List<CharSequence>, CharSequence> left = efirst.apply(eargs);
            Function<List<CharSequence>, CharSequence> right = esecond.apply(eargs);

            Association assoc = (e.getExpressionType() == ExpressionType.Equal)
                    && (isEntityLike(first.getResultType()) || isCollection(first.getResultType()))
                    && (isEntityLike(second.getResultType()) || isCollection(second.getResultType()))
                            ? getAssociation(first, second)
                            : null;

            switch (e.getExpressionType()) {
            case ExpressionType.Equal:
                Map<CharSequence, CharSequence> aliases = getAliases();
                return (args) -> {
                    if (assoc != null)
                        renderingAssociation = true;
                    CharSequence lseq = left.apply(args);
                    CharSequence rseq = right.apply(args);
                    if (assoc != null) {
                        renderingAssociation = false;
                        return renderAssociation(new StringBuilder(), assoc, aliases, lseq, rseq);
                    }
                    return renderBinaryOperator(lseq, EQUAL_SIGN, rseq);
                };

            case ExpressionType.Add:
            case ExpressionType.BitwiseAnd:
            case ExpressionType.BitwiseOr:
            case ExpressionType.Divide:
            case ExpressionType.ExclusiveOr:
            case ExpressionType.GreaterThan:
            case ExpressionType.GreaterThanOrEqual:
            case ExpressionType.LeftShift:
            case ExpressionType.LessThan:
            case ExpressionType.LessThanOrEqual:
            case ExpressionType.LogicalAnd:
            case ExpressionType.LogicalOr:
            case ExpressionType.Modulo:
            case ExpressionType.Multiply:
            case ExpressionType.NotEqual:
            case ExpressionType.RightShift:
            case ExpressionType.Subtract:
                return (args) -> {
                    CharSequence lseq = left.apply(args);
                    CharSequence rseq = right.apply(args);
                    String op = getOperatorSign(e.getExpressionType());
                    return renderBinaryOperator(lseq, op, rseq);
                };
            default:
                throw new IllegalArgumentException(
                        TranslationError.UNSUPPORTED_EXPRESSION_TYPE
                                .getError(getOperatorSign(e.getExpressionType())));
            }
        };
    }

    private StringBuilder renderAssociation(StringBuilder out,
                                   Association assoc,
                                   Map<CharSequence, CharSequence> aliases,
                                   CharSequence lseq,
                                   CharSequence rseq) {
        out.append(LEFT_PARAN);

        for (int i = 0; i < assoc.getCardinality(); i++) {
            if (out.length() > 1)
                out.append(KEYWORD_DELIMITER + AND + KEYWORD_DELIMITER);

            if (IdentifierPath.isResolved(lseq))
                out.append(lseq);
            else
                out.append(resolveLabel(aliases, lseq)).append(DOT).append(assoc.getLeft().get(i));

            out.append(KEYWORD_DELIMITER + EQUAL_SIGN + KEYWORD_DELIMITER);

            if (IdentifierPath.isResolved(rseq))
                out.append(rseq);
            else
                out.append(resolveLabel(aliases, rseq)).append(DOT).append(assoc.getRight().get(i));
        }

        return out.append(RIGHT_PARAN);
    }

    private static String getOperatorSign(int expressionType) {
        switch (expressionType) {
        case ExpressionType.LogicalAnd:
            return "AND";
        case ExpressionType.LogicalOr:
            return "OR";
        case ExpressionType.NotEqual:
            return "<>";
        default:
            return ExpressionType.toString(expressionType);
        }
    }

    private static CharSequence renderBinaryOperator(CharSequence lseq,
                                               String op,
                                               CharSequence rseq) {
        return new StringBuilder(LEFT_PARAN).append(verifyParentheses(lseq))
                .append(KEYWORD_DELIMITER_CHAR)
                .append(op)
                .append(KEYWORD_DELIMITER_CHAR)
                .append(verifyParentheses(rseq))
                .append(RIGHT_PARAN);
    }

    @Override
    public Function<List<Expression>, Function<List<CharSequence>, CharSequence>> visit(ConstantExpression e) {
        Object value = e.getValue();
        if (value instanceof Expression) {
            return eargs -> {
                Expression ex = (Expression) value;
                argumentsTarget = ex;
                return ex.accept(this).apply(eargs);
            };
        }
        return eargs -> {
            if (collectingParameters.orElse(false))
                return args -> registerParameter(value);

            return args -> new DynamicConstant(value);
        };
    }

    @RequiredArgsConstructor
    private class DynamicConstant implements CharSequence {

        private CharSequence string;
        private final Object value;

        private void lazyInit() {
            if (string == null) {
                if (value instanceof CharSequence || value instanceof Character) {
                    // wrap with quotes and escape existing quotes
                    StringBuilder out = new StringBuilder().append(SINGLE_QUOTE_CHAR);
                    if (value instanceof CharSequence)
                        out.append((CharSequence) value);
                    else
                        out.append((char) value);
                    for (int i = out.length() - 1; i > 0; i--) {
                        if (out.charAt(i) == SINGLE_QUOTE_CHAR)
                            out.insert(i, SINGLE_QUOTE_CHAR);
                    }
                    string = out.append(SINGLE_QUOTE_CHAR);
                } else
                    string = String.valueOf(value);
            }
        }

        public CharSequence registerAsParameter() {
            return new StringBuilder().append(QUESTION_MARK_CHAR).append(registerParameter(value));
        }

        @Override
        public int length() {
            lazyInit();
            return string.length();
        }

        @Override
        public char charAt(int index) {
            lazyInit();
            return string.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start,
                                        int end) {
            lazyInit();
            return string.subSequence(start, end);
        }

        @Override
        public String toString() {
            lazyInit();
            return string.toString();
        }
    }

    private StringBuilder registerParameter(Object value) {
        indexedParameters.add(value);
        int index = indexedParameters.size();
        StringBuilder name = new StringBuilder().append(index);
        parameterRefs.add(name);
        return name;
    }

    private static boolean isLambda(Object e) {
        if (e instanceof LambdaExpression)
            return true;

        return e instanceof ConstantExpression && isLambda(((ConstantExpression) e).getValue());
    }

    @Override
    public Function<List<Expression>, Function<List<CharSequence>, CharSequence>> visit(InvocationExpression e) {
        InvocableExpression target = e.getTarget();
        Function<List<Expression>, Function<List<CharSequence>, CharSequence>> ftarget = target.accept(this);

        List<Expression> allArgs = e.getArguments();

        Stream<Function<List<Expression>, Function<List<CharSequence>, CharSequence>>> args = allArgs.stream()
                .map(p -> isLambda(p) ? x -> y -> null : p.accept(this));

        return eargs -> {

            boolean isSubQuery = false;
            CharSequence previousSubQueryAlias = null;

            if (target.getExpressionType() == ExpressionType.MethodAccess) {
                Method method = (Method) ((MemberExpression) target).getMember();
                isSubQuery = method.isAnnotationPresent(SubQuery.class);

                if (isSubQuery) {
                    previousSubQueryAlias = subQueryAlias;
                    subQueryAlias = new StringBuilder(SUB_QUERY_ALIAS_PREFIX).append(subQueriesCounter++);
                }
            }

            // only for the first time
            if (collectingParameters.isPresent())
                collectingParameters = Optional.of(true);
            List<Function<List<CharSequence>, CharSequence>> fargs = args.map(arg -> arg.apply(eargs))
                    .collect(Collectors.toList());
            collectingParameters = Optional.empty();

            Function<List<CharSequence>, List<CharSequence>> params = pp -> fargs.stream()
                    .map(arg -> arg.apply(pp))
                    .collect(Collectors.toList());

            List<Expression> curArgs = bind(allArgs, eargs);

            argumentsTarget = target;
            Function<List<CharSequence>, CharSequence> fmember = ftarget.apply(curArgs);

            if (target.getExpressionType() == ExpressionType.Lambda) {
                return fmember.compose(params);
            }

            @SuppressWarnings("unchecked")
            Function<List<Expression>, Function<List<CharSequence>, Function<List<CharSequence>, CharSequence>>> m1 = (Function<List<Expression>, Function<List<CharSequence>, Function<List<CharSequence>, CharSequence>>>) (Object) fmember;

            Function<List<CharSequence>, Function<List<CharSequence>, CharSequence>> m = m1.apply(eargs);

            if (isSubQuery) {
                subQueryAlias = previousSubQueryAlias;
            }

            return pp -> m.apply(pp).apply(params.apply(pp));
        };
    }

    private List<Expression> bind(List<Expression> allArgs,
                                  List<Expression> eargs) {
        List<Expression> curArgs = allArgs;
        if (!eargs.isEmpty() && !curArgs.isEmpty()) {
            curArgs = new ArrayList<>(curArgs);
            for (int i = 0; i < curArgs.size(); i++) {
                Expression a = curArgs.get(i);
                if (a instanceof ParameterExpression) {
                    int index = ((ParameterExpression) a).getIndex();
                    if (index >= eargs.size())
                        continue;
                    Expression bound = eargs.get(index);
                    int boundExpressionType = bound.getExpressionType();
                    if (boundExpressionType == ExpressionType.Parameter
                            || boundExpressionType == ExpressionType.Constant) {
                        if ((bound.getResultType() == Object.class)
                                || (wrap(bound.getResultType()) == wrap(a.getResultType())))
                            continue;

                        if (boundExpressionType == ExpressionType.Constant)
                            bound = Expression.parameter(bound.getResultType(), index);
                    }

                    curArgs.set(i, bound);
                }
            }
        }
        return curArgs;
    }


    private List<Expression> prepareLambdaParameters(List<ParameterExpression> declared,
                                                            List<Expression> arguments) {
        List<Expression> result = new ArrayList<>(declared);
        for (int i = 0; i < arguments.size(); i++) {
            if (i >= declared.size())
                break;
            Expression original = arguments.get(i);
            Expression arg = original;
            while (arg instanceof UnaryExpression)
                arg = ((UnaryExpression) arg).getFirst();

            if (arg.getResultType() == Object.class)
                continue; // better leave parameter

            // don't forward anything except LambdaExpression
            if (arg instanceof ConstantExpression) {
                if (!(((ConstantExpression) arg).getValue() instanceof LambdaExpression))
                    arg = Expression.parameter(original.getResultType(), i);

            } else if (!(arg instanceof LambdaExpression)) {
                ParameterExpression newParam = Expression.parameter(original.getResultType(), i);
                if (arg instanceof ParameterExpression)
                    parameterBackwardMap.put(newParam, (ParameterExpression) arg);
                arg = newParam;
            }

            result.set(i, arg);
        }

        return result;
    }

    @Override
    public Function<List<Expression>, Function<List<CharSequence>, CharSequence>> visit(LambdaExpression<?> e) {

        boolean skipScopeAllocation = e.getResultType() != Void.TYPE;

        Function<List<Expression>, Function<List<CharSequence>, CharSequence>> ff = e.getBody().accept(this);
        Stream<Function<List<Expression>, Function<List<CharSequence>, CharSequence>>> flocals = e.getLocals()
                .stream()
                .map(l -> l != null ? l.accept(this) : null);

        Stream<Function<List<Expression>, Function<List<CharSequence>, CharSequence>>> fparams = e.getParameters()
                .stream()
                .map(p -> p.accept(this));

        return eargs -> {
            Map<CharSequence, CharSequence> currentAliases;
            SubQueryManager currentSubQueries, capturedSubQueries;

            if (!skipScopeAllocation) {
                currentAliases = aliases_;
                aliases_ = new ScopedHashMap<>(currentAliases);
                currentSubQueries = subQueries_;
                capturedSubQueries = subQueries_ = new SubQueryManager(currentSubQueries);
            } else {
                currentAliases = null;
                currentSubQueries = capturedSubQueries = null;
            }

            try {

                List<Expression> eargsFinal = argumentsTarget == e ? eargs : Collections.emptyList();
                argumentsTarget = e.getBody();

                List<Expression> eargsPrepared = prepareLambdaParameters(e.getParameters(), eargsFinal);

                Function<List<CharSequence>, CharSequence> f = ff.apply(eargsPrepared);

                List<Function<List<CharSequence>, CharSequence>> ple = flocals
                        .map(p -> p != null ? p.apply(eargsPrepared) : null)
                        .collect(Collectors.toList());

                if (!ple.isEmpty()) {
                    f = f.compose(pp -> {
                        List<CharSequence> npe = new ArrayList<>(pp);
                        ple.forEach(le -> npe.add(le != null ? le.apply(npe) : null));
                        return npe;
                    });
                }
                return f.compose(visitParameters(e.getParameters(), fparams, eargsFinal))
                        .andThen(seq -> {
                            try {
                                return seq;
                            } finally {
                                if (!skipScopeAllocation)
                                    capturedSubQueries.close();
                            }
                        });
            } finally {
                if (!skipScopeAllocation) {
                    aliases_ = currentAliases;
                    subQueries_ = currentSubQueries;
                }
            }
        };
    }

    private Function<List<CharSequence>, List<CharSequence>> visitParameters(List<ParameterExpression> original,
                                                                             Stream<Function<List<Expression>, Function<List<CharSequence>, CharSequence>>> parameters,
                                                                             List<Expression> eargs) {

        List<Function<List<CharSequence>, CharSequence>> ppe = parameters.map(p -> p.apply(eargs))
                .collect(Collectors.toList());

        Function<List<CharSequence>, List<CharSequence>> params = pp -> {
            pp = pp.subList(0, eargs.size());
            CharSequence[] r = new CharSequence[ppe.size()];

            for (int index = 0; index < r.length; index++) {
                Function<List<CharSequence>, CharSequence> pe = ppe.get(index);
                r[original.get(index).getIndex()] = pe.apply(pp);
            }
            return Arrays.asList(r);
        };

        return params;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Function<List<Expression>, Function<List<CharSequence>, CharSequence>> visit(DelegateExpression e) {
        return (Function<List<Expression>, Function<List<CharSequence>, CharSequence>>) (Object) visitDelegateExpression(
                e);
    }

    private Function<List<Expression>, Function<List<Expression>, Function<List<CharSequence>, Function<List<CharSequence>, CharSequence>>>> visitDelegateExpression(DelegateExpression e) {

        Function<Object[], ?> fdelegate = LambdaExpression.compile(e.getDelegate());

        return invocationArguments -> instanceArguments -> {
            Expression delegate = (Expression) fdelegate.apply(instanceArguments.toArray());
            argumentsTarget = delegate;
            Function<List<CharSequence>, CharSequence> x = delegate.accept(this).apply(invocationArguments);
            return ipp -> x;
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public Function<List<Expression>, Function<List<CharSequence>, CharSequence>> visit(MemberExpression e) {
        return (Function<List<Expression>, Function<List<CharSequence>, CharSequence>>) (Object) visitMemberExpression(
                e);
    }

    private static ParameterContext calculateContext(Context context,
                                                     ParameterContext functionContext) {
        if (context != null)
            return context.value();

        return functionContext == ParameterContext.INHERIT ? null : functionContext;
    }

    private Function<CharSequence, CharSequence> setupParameterRenderingContext(ParameterContext newContext,
                                                                                Function<CharSequence, CharSequence> parameterRenderer) {

        if (newContext == null)
            return parameterRenderer;

        return seq -> {
            ParameterContext current = renderingContext;
            renderingContext = newContext;
            try {
                return parameterRenderer.apply(seq);
            } finally {
                renderingContext = current;
            }
        };
    }

    private static <T extends Annotation> T getAnnotation(Annotation[] annotations,
                                                          Class<T> toFind) {
        for (int i = 0; i < annotations.length; i++) {
            Annotation a = annotations[i];
            if (toFind.isAssignableFrom(a.getClass()))
                return toFind.cast(a);
        }
        return null;
    }

    private CharSequence resolveLabel(Map<CharSequence, CharSequence> aliases,
                                      CharSequence seq) {
        CharSequence label = aliases.get(seq);
        if (label != null)
            return label;

        return SubQueryManager.isSubQuery(seq) ? SubQueryManager.getName(seq) : seq;
    }

    private static CharSequence verifyParentheses(CharSequence seq) {

        return SubQueryManager.isSubQueryExpression(seq)
                ? new StringBuilder(LEFT_PARAN).append(seq).append(RIGHT_PARAN)
                : seq;
    }

    private Function<List<Expression>, Function<List<Expression>, Function<List<CharSequence>, Function<List<CharSequence>, CharSequence>>>> visitMemberExpression(MemberExpression e) {

        // TODO: field support ?!
        final Method m = (Method) e.getMember();

        Expression ei = e.getInstance();
        final Function<List<Expression>, Function<List<CharSequence>, CharSequence>> finstance = ei != null
                ? ei.accept(this)
                : null;

        return invocationArguments -> instanceArguments -> {

            Function<List<CharSequence>, CharSequence> instance = finstance != null ? finstance.apply(instanceArguments)
                    : null;

            Alias alias = m.getAnnotation(Alias.class);
            if (alias != null) {

                if (alias.value()) {

                    if (invocationArguments.size() > 2)
                        throw new UnsupportedOperationException();

                    Map<CharSequence, CharSequence> aliases = getAliases();
                    return ipp -> {
                        CharSequence inst = instance != null ? instance.apply(ipp) : null;
                        return pp -> {
                            CharSequence aliased = inst != null ? inst : pp.get(0);
                            int numOfArgs = pp.size();
                            if (numOfArgs == 0)
                                return aliased;
                            int labelIndex = numOfArgs - 1;
                            CharSequence label = pp.get(labelIndex);
                            label = extractColumnName(label);
                            aliases.put(aliased, label);
                            return aliased;
                        };
                    };
                } else {
                    return ipp -> pp -> {
                        if (!pp.isEmpty())
                            expandVarArgs(pp).forEach(this::addUndeclaredAlias);
                        return "";
                    };
                }
            }

            if (m.isAnnotationPresent(Parameter.class)) {
                return ipp -> pp -> {
                    CharSequence seq = pp.get(0);
                    if (!Strings.isNullOrEmpty(seq) && seq.charAt(0) == QUESTION_MARK_CHAR)
                        return seq;
                    if (!(seq instanceof DynamicConstant))
                        throw TranslationError.REQUIRES_EXTERNAL_PARAMETER.getError(seq);

                    return ((DynamicConstant) seq).registerAsParameter();
                };
            }

            Member tableJoinMember;
            TableJoin tableJoin = m.getAnnotation(TableJoin.class);
            if (tableJoin != null) {

                if (!(ei instanceof ParameterExpression)) {
                    throw TranslationError.INSTANCE_NOT_JOINTABLE.getError(ei);
                }

                Object x = instanceArguments;
                Expression actual = instanceArguments.get(((ParameterExpression) ei).getIndex());

                if (!(actual instanceof ParameterExpression)) {
                    throw TranslationError.INSTANCE_NOT_JOINTABLE.getError(ei);
                }

                ParameterExpression actualParam = (ParameterExpression) actual;

                if (joinTables.isEmpty()) {
                    joinTables = new HashMap<>();
                    joinTablesForFROM = new HashMap<>();
                }

                Expression arg = invocationArguments.get(0);
                if (!(arg instanceof InvocationExpression)) {
                    throw TranslationError.NOT_PROPERTY_CALL.getError(arg);
                }

                InvocableExpression target = ((InvocationExpression) arg).getTarget();
                if (!(target instanceof MemberExpression)) {
                    throw TranslationError.NOT_PROPERTY_CALL.getError(target);
                }

                tableJoinMember = ((MemberExpression) target).getMember();

                while (actualParam != null) {
                    joinTables.put(actualParam, tableJoinMember);
                    actualParam = parameterBackwardMap.get(actualParam);
                }
            }
            else {
                tableJoinMember = null;
            }

            SubQueryManager subQueries = getSubQueries();
            Map<CharSequence, CharSequence> aliases = getAliases();

            CommonTableExpression cte = m.getAnnotation(CommonTableExpression.class);
            if (cte != null) {

                if (cte.value() == CommonTableExpressionType.SELF) {
                    CharSequence subQueryAlias = this.subQueryAlias;
                    return ipp -> pp -> {
                        return subQueries.put(pp.get(0), subQueryAlias, false);
                    };
                }

                if (cte.value() == CommonTableExpressionType.DECLARATION) {
                    return ipp -> pp -> {
                        StringBuilder with = new StringBuilder(m.getName()).append(KEYWORD_DELIMITER_CHAR);
                        int startLength = with.length();
                        for (CharSequence subQueryAlias : expandVarArgs(pp)) {
                            if (with.length() > startLength)
                                with.append(COMMA_CHAR);
                            CharSequence subQuery = subQueries.sealName(subQueryAlias);
                            if (subQuery == null)
                                throw new IllegalArgumentException("Parameter must be subQuery: " + subQueryAlias);
                            with.append(subQueryAlias)
                                    .append(SEP_AS_SEP + NEW_LINE + LEFT_PARAN)
                                    .append(subQuery)
                                    .append(RIGHT_PARAN + NEW_LINE);
                        }
                        return with;
                    };
                }

                if (cte.value() == CommonTableExpressionType.REFERENCE) {
                    return ipp -> pp -> {

                        CharSequence seq = pp.get(0);
                        return SubQueryManager.isSubQuery(seq) ? SubQueryManager.getName(seq) : seq;

                    };
                }
            }

//        if (m instanceof Field || m instanceof Constructor<?>)
//            throw new IllegalStateException(e.toString());
            co.streamx.fluent.notation.Function function = m.getAnnotation(co.streamx.fluent.notation.Function.class);
            ParameterContext functionContext = function == null ? ParameterContext.INHERIT
                    : getParameterContext(function);
            Annotation[][] parametersAnnotations = m.getParameterAnnotations();

            List<Function<Expression, Function<CharSequence, CharSequence>>> argsBuilder = new ArrayList<>(
                    invocationArguments.size());
            for (int i = 0; i < invocationArguments.size(); i++) {
                Expression arg = invocationArguments.get(i);

                Annotation[] parameterAnnotations = parametersAnnotations[i];
                Context contextAnnotation = getAnnotation(parameterAnnotations, Context.class);
                ParameterContext context = calculateContext(contextAnnotation, functionContext);

                if (context == ParameterContext.FROM || context == ParameterContext.FROM_WITHOUT_ALIAS)
                    argsBuilder.add(ex -> tableReference(ex != null ? ex : arg, subQueries, aliases));
                else {
                    Literal literal = getAnnotation(parameterAnnotations, Literal.class);

                    argsBuilder.add(ex -> {
                        Function<CharSequence, CharSequence> renderer = setupParameterRenderingContext(context,
                                expression(ex != null ? ex : arg, subQueries, aliases));

                        return literal != null
                                ? renderer.andThen(seq -> new StringBuilder(seq.length() + 2).append(SINGLE_QUOTE_CHAR)
                                        .append(seq)
                                        .append(SINGLE_QUOTE_CHAR))
                                : renderer;
                    });
                }
            }

            boolean isSubQuery = m.isAnnotationPresent(SubQuery.class);
            CharSequence subQueryAlias = isSubQuery ? this.subQueryAlias : null;
            Alias.Allowed useAlias = m.getAnnotation(Alias.Allowed.class);

            return ipp -> {

                StringBuilder out = new StringBuilder();
                CharSequence instMutating = null;
                if (instance != null) {
                    // the table definition must come from JOIN clause
                    CharSequence inst = instance.apply(ipp);

                    if (tableJoin != null) {
                        CharSequence lseq = inst;
                        return pp -> {
                            Association association = getAssociationMTM(tableJoinMember, tableJoin.inverse());
                            return renderAssociation(out, association, aliases, lseq,
                                    tableJoin.inverse() ? pp.get(1) : pp.get(0));
                        };
                    }

                    Property tableJoinProperty = m.getAnnotation(TableJoin.Property.class);
                    if (tableJoinProperty != null) {
                        CharSequence lseq = inst;
                        return pp -> {
                            Member member = joinTablesForFROM.get(lseq);
                            Association association = getAssociationMTM(member, tableJoinProperty.inverse());
                            return new IdentifierPath.MultiColumnIdentifierPath(m.getName(), association)
                                    .resolveInstance(lseq);
                        };
                    }

                    inst = resolveLabel(aliases, inst);

                    if (!undeclaredAliases.contains(inst)) {
                        out.append(IdentifierPath.current(inst));
                        instMutating = inst;
                    }
                }

                CharSequence instFinal = instMutating;

                if (function != null) {

                    renderAliases |= function.aliasesVisible();

                    ParameterContext parameterContext = getParameterContext(function);
                    if (instance == null && parameterContext != ParameterContext.INHERIT) {
                        renderingContext = parameterContext; // starting new clause
                    }

                    CharSequence fn = function.name().equals(co.streamx.fluent.notation.Function.USE_METHOD_NAME)
                            ? function.underscoresAsBlanks()
                                    ? m.getName().replace(UNDERSCORE_CHAR, KEYWORD_DELIMITER_CHAR)
                                    : m.getName()
                            : function.name();

                    CharSequence functionName = fn;

                    Operator operator = m.getAnnotation(Operator.class);

                    return pp -> {

                        CharSequence currentSubQuery = (cte != null
                                && cte.value() == CommonTableExpressionType.DECORATOR) ? subQueries.sealName(pp.get(0))
                                : null;

                        if (instance != null) {
                            out.append(KEYWORD_DELIMITER_CHAR);
                        }

                        List<Function<CharSequence, CharSequence>> argsBuilderBound = new ArrayList<>();
                        pp = expandVarArgs(pp, argsBuilder, argsBuilderBound);

                        Stream<CharSequence> args = Streams.zip(pp, argsBuilderBound, (arg,
                                                                                       builder) -> builder.apply(arg));

                        if (useAlias != null && renderAliases) {
                            args = args.map(seq -> {
                                CharSequence label = aliases.get(seq);
                                return label != null ? label : seq;
                            });
                        }

                        String delimiter = function.omitArgumentsDelimiter() ? KEYWORD_DELIMITER
                                : function.argumentsDelimiter() + KEYWORD_DELIMITER;

                        boolean omitParentheses;
                        if (operator == null) {

                            omitParentheses = function.omitParentheses()
                                    || (function.omitParenthesesIfArgumentess() && argsBuilderBound.isEmpty());
                            out.append(functionName);
                            out.append(omitParentheses ? KEYWORD_DELIMITER : LEFT_PARAN);

                            String collectedArgs = args.collect(Collectors.joining(delimiter));
                            out.append(collectedArgs);

                        } else {

                            omitParentheses = function.omitParentheses();
                            out.append(omitParentheses ? KEYWORD_DELIMITER : LEFT_PARAN);

                            Iterator<CharSequence> it = args.iterator();
                            CharSequence next = it.next();

                            if (operator.right()) {
                                out.append(next).append(KEYWORD_DELIMITER).append(functionName);
                            } else {
                                out.append(functionName).append(KEYWORD_DELIMITER).append(next);
                            }

                            if (it.hasNext()) {
                                out.append(operator.omitParentheses() ? KEYWORD_DELIMITER : LEFT_PARAN);

                                do {
                                    next = it.next();
                                    out.append(next).append(delimiter);
                                } while (it.hasNext());

                                out.setLength(out.length() - delimiter.length());

                                out.append(operator.omitParentheses() ? KEYWORD_DELIMITER : RIGHT_PARAN);
                            }
                        }

                        out.append(omitParentheses ? KEYWORD_DELIMITER : RIGHT_PARAN);

                        args.close();

                        if (currentSubQuery != null) // decorator is optional
                            return subQueries.put(out, currentSubQuery);

                        if (function.requiresAlias()) {
                            StringBuilder implicitAlias = new StringBuilder(TABLE_ALIAS_PREFIX)
                                    .append(parameterCounter++);
                            if (function.omitParentheses()) {
                                RequiresParenthesesInAS specialCase = new RequiresParenthesesInAS(out);
                                aliases.put(specialCase, implicitAlias);
                                return specialCase;
                            }

                            aliases.put(out, implicitAlias);
                        }

                        return out;
                    };

                }

                if (isSubQuery) {
                    return pp -> subQueries.put(subQueryAlias, pp.get(0));
                }

                return pp -> {

                    if (renderingAssociation) {
                        // we cannot resolve other side of the association without this side
                        // so let's handle them together
                        return out;
                    }

                    if (isEmbeddable(m.getReturnType()) || isCollection(m.getReturnType()))
                        // embedded
                        return out;

                    IdentifierPath columnName = getColumnNameFromProperty(m);

                    if (m.getParameterCount() > 0) // assignment
                        return new StringBuilder(columnName).append(KEYWORD_DELIMITER + EQUAL_SIGN + KEYWORD_DELIMITER)
                                .append(pp.get(0));

                    if (instFinal != null) {
                        return columnName.resolveInstance(instFinal);
                    }

                    out.append(columnName);

                    return out;
                };
            };
        };
    }

    private static List<CharSequence> expandVarArgs(List<CharSequence> pp) {
        int lastIndex = pp.size() - 1;
        if (lastIndex < 0)
            return pp;

        CharSequence last = pp.get(lastIndex);
        if (!(last instanceof PackedInitializers))
            return pp;

        List<CharSequence> initializers = ((PackedInitializers) last).getInitializers();
        if (lastIndex == 0)
            return initializers;
        return Streams.join(pp.subList(0, lastIndex), initializers);
    }

    private static List<CharSequence> expandVarArgs(List<CharSequence> pp,
                                                    List<Function<Expression, Function<CharSequence, CharSequence>>> argsBuilder,
                                                    List<Function<CharSequence, CharSequence>> argsBuilderBound) {
        int lastIndex = pp.size() - 1;

        CharSequence lastSeq = lastIndex < 0 ? null : pp.get(lastIndex);
        if (!(lastSeq instanceof PackedInitializers)) {
            argsBuilder.forEach(b -> argsBuilderBound.add(b.apply(null)));
            return pp;
        }
        PackedInitializers packed = (PackedInitializers) lastSeq;
        List<CharSequence> initializers = packed.getInitializers();
        pp = new ArrayList<>(pp);
        pp.remove(lastIndex);
        pp.addAll(initializers);

        for (int i = 0; i < lastIndex; i++)
            argsBuilderBound.add(argsBuilder.get(i).apply(null));

        Function<Expression, Function<CharSequence, CharSequence>> varargsBuilder = argsBuilder.get(lastIndex);
        for (int i = 0; i < initializers.size(); i++)
            argsBuilderBound.add(varargsBuilder.apply(packed.getExpressions().get(i)));

        return pp;
    }

    private static CharSequence extractColumnName(CharSequence expression) {
        int dotIndex = Strings.lastIndexOf(expression, DOT_CHAR);
        if (dotIndex >= 0)
            expression = expression.subSequence(dotIndex + 1, expression.length());
        if (expression.charAt(0) == SINGLE_QUOTE_CHAR)
            expression = expression.subSequence(1, expression.length() - 1); // remove quotes coming from constant
        return expression;
    }

    // terminal function
    private Function<CharSequence, CharSequence> tableReference(Expression e,
                                                                SubQueryManager subQueries,
                                                                Map<CharSequence, CharSequence> aliases) {

        return seq -> {

            return handleFromClause(seq, e, aliases, subQueries);
        };
    }

    private CharSequence resolveTableName(CharSequence seq,
                                       Class<?> resultType) {
        Member joinTable = joinTablesForFROM.get(seq);
        return joinTable != null ? getJoinTableName(joinTable) : getTableName(resultType);
    }

    private CharSequence handleFromClause(CharSequence seq,
                                          Expression e,
                                          Map<CharSequence, CharSequence> aliases,
                                          SubQueryManager subQueries) {
        Class<?> resultType = e.getResultType();
        if (!isCollection(resultType) && !isScalar(resultType) && (Object.class != resultType)
                && !isEntityLike(resultType))
            throw TranslationError.INVALID_FROM_PARAM.getError(resultType);

        CharSequence label = aliases.get(seq);
        boolean hasLabel = label != null;
        if (!hasLabel)
            label = seq;

        if (SubQueryManager.isSubQuery(seq)) {

            CharSequence name = SubQueryManager.getName(seq);
            if (name != seq) {
                if (!hasLabel)
                    label = name;
                StringBuilder fromBuilder = SubQueryManager.isRequiresParentheses(seq)
                        ? new StringBuilder(LEFT_PARAN).append(seq).append(RIGHT_PARAN)
                        : new StringBuilder(seq);
                fromBuilder.append(capabilities.contains(Capability.TABLE_AS_ALIAS) ? SEP_AS_SEP : KEYWORD_DELIMITER);
                return fromBuilder.append(label);
            }

            return label;
        }

        CharSequence tableName = resolveTableName(seq, resultType);
        if (hasLabel && tableName == null)
            tableName = seq instanceof RequiresParenthesesInAS
                    ? new StringBuilder(LEFT_PARAN).append(((RequiresParenthesesInAS) seq).getWrapped())
                            .append(RIGHT_PARAN)
                    : seq;

        if (tableName != null) {
            if (renderingContext == ParameterContext.FROM) {
                StringBuilder fromBuilder = new StringBuilder(tableName);
                fromBuilder.append(capabilities.contains(Capability.TABLE_AS_ALIAS) ? SEP_AS_SEP : KEYWORD_DELIMITER);
                return fromBuilder.append(label);
            } else {
                addUndeclaredAlias(label);
                return tableName;
            }
        }

        return seq;
    }

    // terminal function
    private Function<CharSequence, CharSequence> expression(Expression e,
                                                            SubQueryManager subQueries,
                                                            Map<CharSequence, CharSequence> aliases) {
        boolean isEntity = isEntityLike(e.getResultType());

        return seq -> {

            if (renderingContext == ParameterContext.ALIAS)
                return extractColumnName(seq);

            if (e instanceof ParameterExpression) {
            	if (isEntity) {

            		switch (renderingContext) {
                    case SELECT:
                        if (IdentifierPath.isResolved(seq))
                            break;
                        seq = resolveLabel(aliases, seq);
                        return new StringBuilder(seq).append(DOT + STAR);
						
                    case FROM:
                    case FROM_WITHOUT_ALIAS:
                        return handleFromClause(seq, e, aliases, subQueries);

					default:
					}
                }
            }

            if (renderingContext == ParameterContext.SELECT) {

                CharSequence label = aliases.get(seq);

                if (label != null) {
                    return new StringBuilder(verifyParentheses(seq)).append(SEP_AS_SEP).append(label);
                }
            }

            return verifyParentheses(seq);
        };
    }

    @Override
    public Function<List<Expression>, Function<List<CharSequence>, CharSequence>> visit(ParameterExpression e) {

        return eargs -> {

            return t -> {

                final int index = e.getIndex();

                if (t.isEmpty() || index >= t.size()) {
                    Class<?> resultType = e.getResultType();
                    if (!isEntityLike(resultType))
                        throw TranslationError.CANNOT_CALCULATE_TABLE_REFERENCE.getError(resultType);
                    return registerJoinTable(new StringBuilder(TABLE_ALIAS_PREFIX).append(parameterCounter++), e);
                }

                CharSequence value = t.get(index);

                if (parameterRefs.contains(value))
                    return new StringBuilder().append(QUESTION_MARK_CHAR).append(value);

                return registerJoinTable(value, e);
            };
        };
    }

    private CharSequence registerJoinTable(CharSequence seq,
                                           ParameterExpression e) {
        Member joinTable = joinTables.get(e);
        if (joinTable != null)
            joinTablesForFROM.put(seq, joinTable);
        return seq;
    }

    @Override
    public Function<List<Expression>, Function<List<CharSequence>, CharSequence>> visit(UnaryExpression e) {

        Function<List<Expression>, Function<List<CharSequence>, CharSequence>> ffirst = e.getFirst().accept(this);

        return eargs -> {

            Function<List<CharSequence>, CharSequence> first = ffirst.apply(eargs);
            switch (e.getExpressionType()) {
            case ExpressionType.IsNull:
                return (args) -> UnaryOperator.IsNull.eval(first.apply(args));
            case ExpressionType.IsNonNull:
                return (args) -> UnaryOperator.IsNonNull.eval(first.apply(args));
            case ExpressionType.Convert:
                return first::apply;
            case ExpressionType.LogicalNot:
                return (args) -> UnaryOperator.LogicalNot.eval(first.apply(args));
            case ExpressionType.Negate:
                return (args) -> UnaryOperator.Negate.eval(first.apply(args));
            case ExpressionType.BitwiseNot:
                return (args) -> UnaryOperator.BitwiseNot.eval(first.apply(args));
            default:
                throw new IllegalArgumentException(
                        TranslationError.UNSUPPORTED_EXPRESSION_TYPE.getError(getOperatorSign(e.getExpressionType())));
            }
        };
    }

    @Override
    public Function<List<Expression>, Function<List<CharSequence>, CharSequence>> visit(BlockExpression e) {

        Stream<Function<List<Expression>, Function<List<CharSequence>, CharSequence>>> fexprs = e.getExpressions()
                .stream()
                .map(p -> p.accept(this));

        return eargs -> {
            collectingParameters = Optional.empty();
            List<Function<List<CharSequence>, CharSequence>> ppe = fexprs.map(p -> p.apply(eargs))
                    .collect(Collectors.toList());

            return t -> ppe.stream().map(pe -> {
                ParameterContext previousRenderingContext = this.renderingContext;
                renderingContext = ParameterContext.EXPRESSION;
                try {
                    return pe.apply(t);
                } finally {
                    renderingContext = previousRenderingContext;
                }
            }).collect(Collectors.joining(NEW_LINE));
        };
    }

    @RequiredArgsConstructor
    @Getter
    private final static class PackedInitializers implements CharSequence {

        private final List<Expression> expressions;
        private final List<CharSequence> initializers;

        @Override
        public int length() {
            throw new IllegalStateException();
        }

        @Override
        public char charAt(int index) {
            throw new IllegalStateException();
        }

        @Override
        public CharSequence subSequence(int start,
                                        int end) {
            throw new IllegalStateException();
        }

        @Override
        public String toString() {
            throw new IllegalStateException();
        }
    }

    @Override
    public Function<List<Expression>, Function<List<CharSequence>, CharSequence>> visit(NewArrayInitExpression e) {

        List<Expression> allArgs = e.getInitializers();
        Stream<Function<List<Expression>, Function<List<CharSequence>, CharSequence>>> fexprs = allArgs
                .stream()
                .map(p -> p.accept(this));

        return eargs -> {

            List<Function<List<CharSequence>, CharSequence>> ppe = fexprs.map(p -> p.apply(eargs))
                    .collect(Collectors.toList());

            List<Expression> curArgs = bind(allArgs, eargs);

            return t -> {
                List<CharSequence> collected = ppe.stream().map(pe -> pe.apply(t)).collect(Collectors.toList());

                return new PackedInitializers(curArgs, collected);
            };
        };
    }

    @Getter
    @RequiredArgsConstructor
    private static final class RequiresParenthesesInAS implements CharSequence {
        private final CharSequence wrapped;

        @Override
        public int length() {
            return wrapped.length();
        }

        @Override
        public char charAt(int index) {
            return wrapped.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start,
                                        int end) {
            return wrapped.subSequence(start, end);
        }

        @Override
        public String toString() {
            return wrapped.toString();
        }
    }
}
