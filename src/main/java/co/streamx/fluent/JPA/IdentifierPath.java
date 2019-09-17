package co.streamx.fluent.JPA;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import co.streamx.fluent.JPA.DSLInterpreterHelpers.ParameterRef;
import co.streamx.fluent.JPA.JPAHelpers.Association;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

interface IdentifierPath extends UnboundCharSequence {
    default CharSequence resolveInstance(CharSequence inst,
                                         Map<String, CharSequence> secondaryResolver) {
        return resolveInstance(inst, false, secondaryResolver);
    }

    CharSequence resolveInstance(CharSequence inst,
                                 boolean withoutInstance,
                                 Map<String, CharSequence> secondaryResolver);

    default CharSequence resolve(IdentifierPath path) {
        return resolve(path, false);
    }

    CharSequence resolve(IdentifierPath path,
                         boolean withoutInstance);

    CharSequence current();

    Class<?> getDeclaringClass();

    String getFieldName();

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

    static CharSequence resolveInstance(CharSequence inst,
                                        String table,
                                        Map<String, CharSequence> secondaryResolver) {
        if (!Strings.isNullOrEmpty(table) && secondaryResolver != null) {
            CharSequence inst1 = secondaryResolver.get(table);
            if (inst1 == null)
                inst1 = secondaryResolver.get("");

            if (inst1 != null)
                inst = inst1;
        }

        return inst;
    }

    @RequiredArgsConstructor
    final class Resolved implements IdentifierPath {
        private final CharSequence resolution;

        @Getter
        private final Class<?> declaringClass;

        @Getter
        private final String fieldName;

        private final String table;

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
        public CharSequence resolveInstance(CharSequence inst,
                                            boolean withoutInstance,
                                            Map<String, CharSequence> secondaryResolver) {
            if (inst instanceof ParameterRef)
                throw TranslationError.CANNOT_DEREFERENCE_PARAMETERS.getError(((ParameterRef) inst).getValue(),
                        resolution);
            if (Strings.isNullOrEmpty(inst))
                return this;
            if (inst instanceof IdentifierPath)
                return ((IdentifierPath) inst).resolve(this, withoutInstance);

            inst = IdentifierPath.resolveInstance(inst, table, secondaryResolver);

            StringBuilder seq = withoutInstance ? new StringBuilder() : new StringBuilder(inst).append(DOT);
            return new Resolved(seq.append(resolution).toString(), declaringClass, fieldName, table);
        }

        @Override
        public CharSequence resolve(IdentifierPath path,
                                    boolean withoutInstance) {
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
    @Getter
    abstract class AssociativeIdentifierPath implements IdentifierPath {

        private final String fieldName;
        private final String table;
        private CharSequence instance;

        private RuntimeException error() {
            return new IllegalStateException("'" + fieldName
                    + "' has multi-column mapping. You must call the appropriate property to resolve it");
        }

        @Override
        public Class<?> getDeclaringClass() {
            throw new UnsupportedOperationException();
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
            return instance;
        }

        @Override
        public CharSequence resolveInstance(CharSequence inst,
                                            boolean withoutInstance,
                                            Map<String, CharSequence> secondaryResolver) {
            if (this.instance != null)
                new IllegalStateException("Already initialized with '" + this.instance + "' instance. Passing a new '"
                        + inst + "' is illegal");
            inst = IdentifierPath.resolveInstance(inst, table, secondaryResolver);
            this.instance = inst;
            return this;
        }
    }

    final class MultiColumnIdentifierPath extends AssociativeIdentifierPath {

        private final Function<Class<?>, JPAHelpers.Association> associationSupplier;

        public MultiColumnIdentifierPath(String originalField,
                Function<Class<?>, JPAHelpers.Association> associationSupplier, String table) {
            super(originalField, table);
            this.associationSupplier = associationSupplier;
        }

        @Override
        public CharSequence resolve(IdentifierPath path,
                                    boolean withoutInstance) {
            if (!(path instanceof Resolved))
                throw new IllegalArgumentException(path.getClass() + ":" + path.current());
            CharSequence key = path.current();
            Association association = associationSupplier.apply(path.getDeclaringClass());
            List<CharSequence> referenced = association.getRight();
            for (int i = 0; i < referenced.size(); i++) {
                CharSequence seq = referenced.get(i);
                if (Strings.equals(seq, key)) {
                    StringBuilder inst = withoutInstance ? new StringBuilder()
                            : new StringBuilder(getInstance()).append(DOT);
                    return new Resolved(inst.append(association.getLeft().get(i)), path.getDeclaringClass(),
                            path.getFieldName(), getTable());
                }
            }
            throw new IllegalArgumentException("Column '" + key + "' not found in PK: " + referenced);
        }
    }

    final class ColumnOverridingIdentifierPath extends AssociativeIdentifierPath {

        private final Map<String, String> overrides;

        public ColumnOverridingIdentifierPath(Map<String, String> overrides, String table) {
            super(null, table);
            this.overrides = overrides;
        }

        @Override
        public int length() {
            return current().length();
        }

        @Override
        public char charAt(int index) {
            return current().charAt(index);
        }

        @Override
        public CharSequence subSequence(int start,
                                        int end) {
            return current().subSequence(start, end);
        }

        @Override
        public String toString() {
            return current().toString();
        }

        @Override
        public CharSequence resolve(IdentifierPath path,
                                    boolean withoutInstance) {
            if (!(path instanceof Resolved)) // TODO: can be MultiColumnIdentifierPath
                throw new IllegalArgumentException(path.getClass() + ":" + path.current());
            String key = path.getFieldName();
            String override = overrides.get(key);
            CharSequence column = override != null ? override : path.current();

            StringBuilder inst = withoutInstance ? new StringBuilder() : new StringBuilder(getInstance()).append(DOT);
            return new Resolved(inst.append(column), path.getDeclaringClass(), key, getTable());
        }
    }
}
