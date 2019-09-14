package co.streamx.fluent.JPA;

import static co.streamx.fluent.JPA.JPAHelpers.wrap;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import co.streamx.fluent.extree.expression.ConstantExpression;
import co.streamx.fluent.extree.expression.Expression;
import co.streamx.fluent.extree.expression.ExpressionType;
import co.streamx.fluent.extree.expression.InvocationExpression;
import co.streamx.fluent.extree.expression.LambdaExpression;
import co.streamx.fluent.extree.expression.ParameterExpression;
import co.streamx.fluent.notation.Context;
import co.streamx.fluent.notation.ParameterContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

interface DSLInterpreterHelpers {
    @RequiredArgsConstructor
    final class DynamicConstant implements UnboundCharSequence {

        private CharSequence string;
        @Getter
        private final Object value;
        private final DSLInterpreter dsl;

        private void lazyInit() {
            if (string == null) {
                if (value instanceof CharSequence || value instanceof Character) {
                    // wrap with quotes and escape existing quotes
                    StringBuilder out = new StringBuilder().append(DSLInterpreter.SINGLE_QUOTE_CHAR);
                    if (value instanceof CharSequence)
                        out.append((CharSequence) value);
                    else
                        out.append((char) value);
                    for (int i = out.length() - 1; i > 0; i--) {
                        if (out.charAt(i) == DSLInterpreter.SINGLE_QUOTE_CHAR)
                            out.insert(i, DSLInterpreter.SINGLE_QUOTE_CHAR);
                    }
                    string = out.append(DSLInterpreter.SINGLE_QUOTE_CHAR);
                } else
                    string = String.valueOf(value);
            }
        }

        public CharSequence registerAsParameter() {
            return dsl.registerParameter(value);
        }

        @Override
        public boolean isEmpty() {
            return false;
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

    @RequiredArgsConstructor
    @Getter
    final class PackedInitializers implements UnboundCharSequence {

        private final List<Expression> expressions;
        List<Function<Object[], ?>> compiled;
        private final List<Function<List<CharSequence>, CharSequence>> producers;
        private final List<CharSequence> it;
        private List<CharSequence> itDecoded;
        private final DSLInterpreter dsl;

        public List<CharSequence> getInitializers() {
            if (itDecoded != null)
                return itDecoded;
            return itDecoded = getInitializers(it, 0);
        }

        public List<CharSequence> getInitializers(CharSequence seq,
                                                  int limit) {
            CharSequence[] seqs = new CharSequence[it.size()];
            Arrays.fill(seqs, seq);
            return getInitializers(Arrays.asList(seqs), limit);
        }

        public List<CharSequence> getInitializers(Object value,
                                                  int limit) {
            if (limit <= 0)
                limit = producers.size() + limit;
            Object[] t = { value };
            return compiled().stream()
                    .limit(limit)
                    .map(f -> f.apply(t))
                    .map(p -> dsl.registerParameter(p))
                    .collect(Collectors.toList());
        }

        private List<CharSequence> getInitializers(List<CharSequence> it,
                                                   int limit) {
            if (limit <= 0)
                limit = producers.size() + limit;
            return producers.stream().limit(limit).map(pe -> pe.apply(it)).collect(Collectors.toList());
        }

        private List<Function<Object[], ?>> compiled() {
            if (compiled == null)
                compiled = expressions.stream()
                        .map(e -> ((InvocationExpression) e).getTarget())
                        .map(ie -> ie instanceof LambdaExpression ? ((LambdaExpression<?>) ie).compile()
                                : LambdaExpression.compile(ie))
                        .collect(Collectors.toList());

            return compiled;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

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

    @Getter
    @RequiredArgsConstructor
    final class RequiresParenthesesInAS implements Wrapped {
        private final CharSequence wrapped;

        @Override
        public String toString() {
            return getWrapped().toString();
        }
    }

    @Getter
    @RequiredArgsConstructor
    final class View {

        private final PackedInitializers packed;
        private CharSequence columns;
        private List<CharSequence> allColumns;
        private CharSequence selfSelect;

        public CharSequence getColumn(CharSequence p,
                                      int i) {
            getColumns();
            try {
                if (i < 0)
                    i += allColumns.size();
                return new StringBuilder(p).append(DSLInterpreter.SEP_AS_SEP).append(allColumns.get(i));
            } catch (IndexOutOfBoundsException ioobe) {
                throw TranslationError.ALIAS_NOT_SPECIFIED.getError(ioobe, i);
            }
        }

        public CharSequence getColumns() {
            if (this.columns != null)
                return this.columns;
            allColumns = packed.getInitializers("", 0);
            return this.columns = join(allColumns, false);
        }

        private String join(List<CharSequence> columns, boolean aliased) {
            Iterable<CharSequence> it = aliased
                    ? () -> IntStream.range(0, columns.size()).mapToObj(i -> getColumn(columns.get(i), i)).iterator()
                    : columns;
            return String.join(DSLInterpreter.COMMA_CHAR + DSLInterpreter.KEYWORD_DELIMITER, it);
        }

        public CharSequence getSelect() {
            if (selfSelect != null)
                return selfSelect;
            return selfSelect = join(packed.getInitializers(), false);
        }

        public CharSequence getSelect(CharSequence seq,
                                      int limit,
                                      boolean aliased) {
            return join(packed.getInitializers(seq, limit), aliased);
        }

        public CharSequence getSelect(Object value,
                                      int limit,
                                      boolean aliased) {
            return join(packed.getInitializers(value, limit), aliased);
        }

    }

    interface Wrapped extends UnboundCharSequence {

        CharSequence getWrapped();

        @Override
        default boolean isEmpty() {
            return length() != 0;
        }

        @Override
        default int length() {
            return getWrapped().length();
        }

        @Override
        default char charAt(int index) {
            return getWrapped().charAt(index);
        }

        @Override
        default CharSequence subSequence(int start,
                                        int end) {
            return getWrapped().subSequence(start, end);
        }
    }

    @RequiredArgsConstructor
    @Getter
    class ParameterRef implements Wrapped {

        private CharSequence seq;
        private final Object value;
        private final List<Object> indexedParameters;
        
        @Override
        public CharSequence getWrapped() {
            if (seq == null) {
                indexedParameters.add(value);
                int index = indexedParameters.size();
                seq = new StringBuilder().append(DSLInterpreter.QUESTION_MARK_CHAR).append(index);
            }
            return seq;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public String toString() {
            return getWrapped().toString();
        }

    }

    default String getOperatorSign(int expressionType) {
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

    default CharSequence renderBinaryOperator(CharSequence lseq,
                                             String op,
                                             CharSequence rseq) {
        return new StringBuilder(DSLInterpreter.LEFT_PARAN).append(verifyParentheses(lseq))
                .append(DSLInterpreter.KEYWORD_DELIMITER_CHAR)
                .append(op)
                .append(DSLInterpreter.KEYWORD_DELIMITER_CHAR)
                .append(verifyParentheses(rseq))
                .append(DSLInterpreter.RIGHT_PARAN);
    }

    default CharSequence verifyParentheses(CharSequence seq) {

        return SubQueryManager.isSubQueryExpression(seq)
                ? new StringBuilder(DSLInterpreter.LEFT_PARAN).append(seq).append(DSLInterpreter.RIGHT_PARAN)
                : seq;
    }

    default boolean isLambda(Object e) {
        if (e instanceof LambdaExpression)
            return true;

        return e instanceof ConstantExpression && isLambda(((ConstantExpression) e).getValue());
    }

    default <T extends Annotation> T getAnnotation(Annotation[] annotations,
                                                   Class<T> toFind) {
        for (int i = 0; i < annotations.length; i++) {
            Annotation a = annotations[i];
            if (toFind.isAssignableFrom(a.getClass()))
                return toFind.cast(a);
        }
        return null;
    }

    default ParameterContext calculateContext(Context context,
                                              ParameterContext functionContext) {
        if (context != null)
            return context.value();

        return functionContext == ParameterContext.INHERIT ? null : functionContext;
    }

    default List<CharSequence> expandVarArgs(List<CharSequence> pp) {
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

    default List<CharSequence> expandVarArgs(List<CharSequence> pp,
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

    default CharSequence extractColumnName(CharSequence expression) {
        int dotIndex = Strings.lastIndexOf(expression, DSLInterpreter.DOT_CHAR);
        if (dotIndex >= 0)
            expression = expression.subSequence(dotIndex + 1, expression.length());
        if (expression.charAt(0) == DSLInterpreter.SINGLE_QUOTE_CHAR)
            expression = expression.subSequence(1, expression.length() - 1); // remove quotes coming from constant
        return expression;
    }

    default Expression bind(List<Expression> args,
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
}
