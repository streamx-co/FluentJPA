package co.streamx.fluent.JPA;

import java.util.List;

import lombok.RequiredArgsConstructor;

interface IdentifierPath extends UnboundCharSequence {
    CharSequence resolveInstance(CharSequence inst);

    CharSequence resolve(IdentifierPath path);

    CharSequence current();

    public static final char DOT = '.';

    public static CharSequence current(CharSequence seq) {
        return seq instanceof IdentifierPath ? ((IdentifierPath) seq).current() : seq;
    }

    public static boolean isResolved(CharSequence seq) {
        return seq instanceof Resolved;
    }

    @Override
    default boolean isEmpty() {
        return false;
    }

    @RequiredArgsConstructor
    public class Resolved implements IdentifierPath {
        private final CharSequence resolution;

        @Override
        public int length() {
            return resolution.length();
        }

        @Override
        public char charAt(int index) {
            return resolution.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start,
                                        int end) {
            return resolution.subSequence(start, end);
        }

        @Override
        public CharSequence resolveInstance(CharSequence inst) {
            if (Strings.isNullOrEmpty(inst))
                return this;
            if (inst instanceof IdentifierPath)
                return ((IdentifierPath) inst).resolve(this);
            return new Resolved(new StringBuilder(inst).append(DOT).append(resolution).toString());
        }

        @Override
        public CharSequence resolve(IdentifierPath path) {
            return this;
        }

        @Override
        public CharSequence current() {
            return resolution;
        }

        @Override
        public String toString() {
            return resolution.toString();
        }
    }

    @RequiredArgsConstructor
    public class MultiColumnIdentifierPath implements IdentifierPath {

        private final String originalField;
        private final JPAHelpers.Association association;
        private CharSequence inst;

        private RuntimeException error() {
            return new IllegalStateException("'" + originalField
                    + "' has multi-column mapping. You must call the appropriate property to resolve it");
        }

        @Override
        public int length() {
            throw error();
        }

        @Override
        public char charAt(int index) {
            throw error();
        }

        @Override
        public CharSequence subSequence(int start,
                                        int end) {
            throw error();
        }

        @Override
        public String toString() {
            throw error();
        }

        @Override
        public CharSequence current() {
            return inst;
        }

        @Override
        public CharSequence resolveInstance(CharSequence inst) {
            if (this.inst != null)
                new IllegalStateException("Already initialized with '" + this.inst + "' instance. Passing a new '"
                        + inst + "' is illegal");
            this.inst = inst;
            return this;
        }

        @Override
        public CharSequence resolve(IdentifierPath path) {
            if (!(path instanceof Resolved))
                throw new IllegalArgumentException(path.getClass() + ":" + path.current());
            CharSequence key = path.current();
            List<CharSequence> referenced = association.getRight();
            for (int i = 0; i < referenced.size(); i++) {
                CharSequence seq = referenced.get(i);
                if (Strings.equals(seq, key)) {
                    return new Resolved(new StringBuilder(inst).append(DOT).append(association.getLeft().get(i)));
                }
            }
            throw new IllegalArgumentException("Column '" + key + "' not found in PK: " + referenced);
        }

    }
}
