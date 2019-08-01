package co.streamx.fluent.JPA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import co.streamx.fluent.JPA.util.ScopedArrayList;
import lombok.Getter;

@SuppressWarnings("serial")
final class SubQueryManager extends ScopedArrayList<SubQueryManager.SubQuery> {

    @Getter
    public static final class SubQuery implements CharSequence {

        private CharSequence value;

        private final CharSequence name;
        private final CharSequence expression;
        private final boolean requiresParentheses;

        public SubQuery(CharSequence name, CharSequence value, boolean requiresParentheses) {
            this.name = name;
            this.expression = value;
            this.value = value;
            this.requiresParentheses = requiresParentheses;
        }

        public boolean isName() {
            return value == name;
        }

        public boolean flip(boolean toName) {
            CharSequence prev = value;
            value = toName ? name : expression;
            return prev != value;
        }

        @Override
        public int length() {
            return value.length();
        }

        @Override
        public char charAt(int index) {
            return value.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start,
                                        int end) {
            return value.subSequence(start, end);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    private List<SubQuery> sealedNames = Collections.emptyList();

    public SubQueryManager(List<SubQueryManager.SubQuery> upper) {
        super(upper);
    }

    CharSequence put(CharSequence name,
                     CharSequence value) {
        return put(name, value, true);
    }

    CharSequence put(CharSequence name,
                     CharSequence value,
                     boolean requiresParentheses) {
        SubQuery subQuery = new SubQuery(name, value, requiresParentheses);
        add(subQuery);

        return subQuery;
    }

    CharSequence sealName(CharSequence seq) {

        if (!isSubQuery(seq))
            return null;
        SubQuery subQuery = (SubQuery) seq;
        if (!subQuery.flip(true))
            return null;

        if (sealedNames.isEmpty())
            sealedNames = new ArrayList<>();

        sealedNames.add(subQuery);

        return subQuery.getExpression();
    }

    static boolean isSubQuery(CharSequence seq) {
        return seq instanceof SubQuery;
	}

    static boolean isSubQueryName(CharSequence seq) {
        return isSubQuery(seq) && ((SubQuery) seq).isName();
    }

    static boolean isSubQueryExpression(CharSequence seq) {
        return isSubQuery(seq) && !((SubQuery) seq).isName();
    }

    static CharSequence getName(CharSequence seq) {
        SubQuery subQuery = (SubQuery) seq;
        return subQuery.isName() ? subQuery : subQuery.getName();
    }

    static boolean isRequiresParentheses(CharSequence seq) {
        SubQuery subQuery = (SubQuery) seq;
        return subQuery.isRequiresParentheses();
    }

    void close() {
        for (SubQuery subQuery : sealedNames)
            subQuery.flip(false);
    }
}
