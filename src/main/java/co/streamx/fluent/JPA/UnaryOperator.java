package co.streamx.fluent.JPA;

import java.util.Formatter;
import java.util.Locale;

enum UnaryOperator {

    IsNull {

        @Override
        public CharSequence eval(Object operand) {
            return format("(%s IS NULL)", operand);
        }

    },
    IsNonNull {

        @Override
        public CharSequence eval(Object operand) {
            return format("(%s IS NOT NULL)", operand);
        }

    },
    LogicalNot {

        @Override
        public CharSequence eval(Object operand) {
            return format("NOT(%s)", operand);
        }

    },

    Negate {

        @Override
        public CharSequence eval(Object operand) {
            return format("-%s", operand);
        }

    },

    BitwiseNot {

        @Override
        public CharSequence eval(Object operand) {
            return format("~%s", operand);
        }

    },

    ;

    public abstract CharSequence eval(Object operand);

    protected final CharSequence format(String format,
                                        Object... args) {
        StringBuilder out = new StringBuilder();
        try (Formatter f = new Formatter(out, Locale.ROOT)) {
            f.format(format, args);
        }
        return out;
    }
}
