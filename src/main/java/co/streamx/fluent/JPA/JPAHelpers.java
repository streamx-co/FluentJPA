package co.streamx.fluent.JPA;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.AccessType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;
import jakarta.persistence.Table;

import co.streamx.fluent.extree.expression.Expression;
import co.streamx.fluent.extree.expression.InvocationExpression;
import co.streamx.fluent.extree.expression.MemberExpression;
import co.streamx.fluent.notation.Tuple;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class JPAHelpers {

    private static final char DOT = IdentifierPath.DOT;
    private static final char UNDERSCORE = '_';
    private static final Map<Member, Field> membersToFields = new ConcurrentHashMap<>();

    // https://stackoverflow.com/questions/3473756/java-convert-primitive-class/17836370
    private static final Class<?>[] wrappers = {Integer.class, Double.class, Byte.class, Boolean.class,
            Character.class, Void.class, Short.class, Float.class, Long.class};

    @SuppressWarnings("unchecked")
    public static <T> Class<T> wrap(final Class<T> clazz) {
        if (!clazz.isPrimitive())
            return clazz;
        final String name = clazz.getName();
        final int c0 = name.charAt(0);
        final int c2 = name.charAt(2);
        final int mapper = (c0 + c0 + c0 + 5) & (118 - c2);
        return (Class<T>) wrappers[mapper];
    }

    @FunctionalInterface
    private interface SneakyBiFunction<T, U, R> {
        R apply(T t,
                U u) throws Exception;
    }

    ;

    @Getter
    @RequiredArgsConstructor
    @ToString
    public static final class ID {

        private final CharSequence path;
        private final CharSequence column;
        private final Member member;
    }

    @Getter
    @ToString
    public static final class ClassMeta {

        public ClassMeta(AccessType accessType) {
            this.accessType = accessType;
        }

        private final AccessType accessType;
        private final List<ID> ids = new ArrayList<>();
    }

    @Getter
    @ToString
    public static final class Association {
        private final List<CharSequence> left;
        private final List<CharSequence> right;

        public Association(List<CharSequence> that, List<CharSequence> other, boolean left) {
            if (that.size() != other.size())
                throw new IllegalArgumentException("keys of different sizes: " + that.size() + "-" + other.size());
            if (left) {
                this.left = that;
                this.right = other;
            } else {
                this.left = other;
                this.right = that;
            }
        }

        public int getCardinality() {
            return left.size();
        }
    }

    private static final Map<Class<?>, ClassMeta> ids = new ConcurrentHashMap<>();

    public static Association getAssociation(Class<?> entity,
                                             SecondaryTable table) {

        PrimaryKeyJoinColumn[] pkCols = table.pkJoinColumns();

        List<ID> typeIds = getClassMeta(entity).getIds();
        if (pkCols.length == 0) {
            List<CharSequence> columns = Streams.map(typeIds, ID::getColumn);
            return new Association(columns, columns, true);
        }

        List<CharSequence> other = new ArrayList<>();
        List<CharSequence> that = new ArrayList<>();

        for (int i = 0; i < pkCols.length; i++) {
            PrimaryKeyJoinColumn join = pkCols[i];

            if (!Strings.isNullOrEmpty(join.referencedColumnName()))
                other.add(join.referencedColumnName());
            else {
                // extension to Standard: HN seems to go positional here
                other.add(typeIds.get(i).getColumn());
            }

            if (!Strings.isNullOrEmpty(join.name()))
                that.add(join.name());
            else {
                that.add(typeIds.get(i).getColumn());
            }

        }

        return new Association(that, other, true);
    }

    public static Association getAssociation(Expression left,
                                             Expression right) {

        if (left instanceof InvocationExpression) {
            return getAssociation((InvocationExpression) left, true);
        }

        if (right instanceof InvocationExpression) {
            return getAssociation((InvocationExpression) right, false);
        }

        if (left.getResultType() == right.getResultType()) {// self
            List<ID> typeIds = getClassMeta(left.getResultType()).getIds();

            List<CharSequence> columns = Streams.map(typeIds, ID::getColumn);
            return new Association(columns, columns, false);
        }

        throw new IllegalStateException(
                String.format("Cannot bind association for [(%s)%s = (%s)%s]. Ensure both sides are entities.",
                        left.getResultType().getSimpleName(), left, right.getResultType().getSimpleName(), right));
    }

    private static Association getAssociation(InvocationExpression e,
                                              boolean left) {
        Member member = ((MemberExpression) e.getTarget()).getMember();
        member = (member instanceof Method) ? getAnnotatedField((Method) member) : member;
        return getAssociation(member, left);
    }

    private static Class<?> getType(Member m) {
        if (m instanceof Field)
            return ((Field) m).getType();

        return ((Method) m).getReturnType();
    }

    private static void calcEmbeddedId(Class<?> embeddable,
                                       List<ID> ids,
                                       StringBuilder path,
                                       Map<String, String> overrides) {
        int length = path.append(DOT).length();
        for (Field f : embeddable.getDeclaredFields()) {
            path.append(f.getName());
            if (f.getAnnotation(Embeddable.class) != null)
                calcEmbeddedId(f.getType(), ids, path, overrides);
            else {
                String cur = path.toString();
                String override = overrides.get(cur);
                ids.add(new ID(cur, override != null ? override : getColumnName(f), f));
            }
            path.setLength(length);
        }
    }

    public static CharSequence calcOverrides(CharSequence instance,
                                             Member field,
                                             Map<String, CharSequence> secondaryResolver) {
        field = getAnnotatedField(field);
        AnnotatedElement annotated = (AnnotatedElement) field;

        return calcOverrides(instance, annotated, secondaryResolver);
    }

    public static CharSequence calcOverrides(CharSequence instance,
                                             AnnotatedElement annotated,
                                             Map<String, CharSequence> secondaryResolver) {
        Map<String, String> overrides = calcOverrides(annotated);
        if (overrides.isEmpty())
            return instance;

        return new IdentifierPath.ColumnOverridingIdentifierPath(overrides, null).resolveInstance(instance,
                secondaryResolver);
    }

    // TODO: overrides for MappedClass objects
    private static Map<String, String> calcOverrides(AnnotatedElement member) {
        AttributeOverride override = member.getAnnotation(AttributeOverride.class);
        if (override != null)
            return Collections.singletonMap(override.name(), override.column().name());

        AttributeOverrides overrides = member.getAnnotation(AttributeOverrides.class);
        if (overrides != null)
            return Arrays.stream(overrides.value())
                    .collect(Collectors.toMap(AttributeOverride::name, o -> o.column().name()));

        return Collections.emptyMap();
    }

    private static ClassMeta findId(Class<?> type) {
        ClassMeta meta = null;
        while (type != Object.class && type != null) {
            if (!type.isInterface()) {
                for (Field f : type.getDeclaredFields()) {
                    if (f.isAnnotationPresent(Id.class)) {
                        if (meta == null)
                            meta = new ClassMeta(AccessType.FIELD);
                        meta.getIds().add(new ID(f.getName(), getColumnName(f), f));
                    } else if (f.isAnnotationPresent(EmbeddedId.class)) {
                        if (meta == null)
                            meta = new ClassMeta(AccessType.FIELD);

                        calcEmbeddedId(f.getType(), meta.getIds(), new StringBuilder(f.getName()), calcOverrides(f));
                    }
                }

                if (meta != null)
                    return meta;
            }

            for (Method m : type.getDeclaredMethods()) {
                if (m.isAnnotationPresent(Id.class)) {
                    if (meta == null)
                        meta = new ClassMeta(AccessType.PROPERTY);
                    meta.getIds().add(new ID(m.getName(), getColumnName(m), m));
                } else if (m.isAnnotationPresent(EmbeddedId.class)) {
                    if (meta == null)
                        meta = new ClassMeta(AccessType.PROPERTY);

                    calcEmbeddedId(m.getReturnType(), meta.getIds(), new StringBuilder(m.getName()), calcOverrides(m));
                }
            }

            if (meta != null)
                return meta;

            type = type.getSuperclass();
        }
        return new ClassMeta(AccessType.FIELD);
    }

    private static AccessType getAccessType(Class<?> type) {
        if (type.isInterface())
            return AccessType.PROPERTY;
        return getClassMeta(type).getAccessType();
    }

    /**
     * member must be annotated!
     */
    public static Association getAssociation(Member member) {
        return getAssociation(member, true);
    }

    private static Association getAssociation(Member field,
                                              boolean left) {

        AnnotatedElement leftField = (AnnotatedElement) field;
        OneToOne oneToOne = leftField.getAnnotation(OneToOne.class);
        if (oneToOne != null) {

            Class<?> targetEntity = oneToOne.targetEntity();
            String mappedBy = oneToOne.mappedBy();
            return getAssociation(field, targetEntity, mappedBy, left);
        }

        ManyToOne manyToOne = leftField.getAnnotation(ManyToOne.class);
        if (manyToOne != null) {
            return getAssociation(field, manyToOne.targetEntity(), null, left);
        }

        OneToMany oneToMany = leftField.getAnnotation(OneToMany.class);
        if (oneToMany != null) {

            Class<?> targetEntity = oneToMany.targetEntity();
            String mappedBy = oneToMany.mappedBy();
            return getAssociation(field, targetEntity, mappedBy, left);
        }

        throw new IllegalStateException("association was not resolved for " + field + ".");
    }

    public static Association getAssociationElementCollection(Member field) {

        field = getAnnotatedField(field);
        AnnotatedElement leftField = (AnnotatedElement) field;

        ElementCollection elemCollection = leftField.getAnnotation(ElementCollection.class);
        if (elemCollection != null) {

            Class<?> declaringClass = field.getDeclaringClass();

            List<CharSequence> entity = null;
            List<CharSequence> join = null;

            CollectionTable colTable = leftField.getAnnotation(CollectionTable.class);
            String declaringTableName = getTableName(declaringClass);
            if (colTable != null) {
                JoinColumn[] columns = colTable.joinColumns();
                if (columns != null) {

                    join = new ArrayList<>();
                    entity = buildVTableJoins(field, join, columns, declaringTableName, declaringClass);
                }
            }

            if (entity == null) {
                entity = Streams.map(getClassMeta(declaringClass).getIds(), ID::getColumn);
                join = entity.stream()
                        .map(col -> concatWithUnderscore(declaringTableName, col))
                        .collect(Collectors.toList());
            }

            return new Association(join, entity, true);
        }

        throw new IllegalStateException("association was not resolved for " + field + ".");
    }

    public static Association getAssociationMTM(Member field,
                                                boolean inverse) {
        field = getAnnotatedField(field);
        AnnotatedElement leftField = (AnnotatedElement) field;

        ManyToMany manyToMany = leftField.getAnnotation(ManyToMany.class);
        if (manyToMany != null) {

            String mappedBy = manyToMany.mappedBy();

            Class<?> declaringClass = field.getDeclaringClass();
            if (mappedBy.length() > 0) {
                field = resolveMappedBy(getTargetForMTM(field), mappedBy);
                leftField = (AnnotatedElement) field;
                inverse = !inverse;
            } else if (inverse) {
                declaringClass = manyToMany.targetEntity();
                if (declaringClass == void.class)
                    declaringClass = getTargetByParameterizedType(field);
            }

            List<CharSequence> entity = null;
            List<CharSequence> join = null;

            JoinTable joinTable = leftField.getAnnotation(JoinTable.class);
            String declaringTableName = getTableName(declaringClass);
            if (joinTable != null) {
                JoinColumn[] columns = inverse ? joinTable.inverseJoinColumns() : joinTable.joinColumns();
                if (columns != null) {

                    join = new ArrayList<>();
                    entity = buildVTableJoins(field, join, columns, declaringTableName, declaringClass);
                }
            }

            if (entity == null) {
                entity = Streams.map(getClassMeta(declaringClass).getIds(), ID::getColumn);
                join = entity.stream()
                        .map(col -> concatWithUnderscore(declaringTableName, col))
                        .collect(Collectors.toList());
            }

            return new Association(join, entity, true);
        }

        throw new IllegalStateException("association was not resolved for " + field + ".");
    }

    private static List<CharSequence> buildVTableJoins(Member field,
                                                       List<CharSequence> join,
                                                       JoinColumn[] columns,
                                                       String declaringTableName,
                                                       Class<?> declaringClass) {
        List<CharSequence> entity = null;
        for (int i = 0; i < columns.length; i++) {
            JoinColumn column = columns[i];
            String columnName = column.name();
            String referencedColumnName = column.referencedColumnName();

            if (Strings.isNullOrEmpty(referencedColumnName)) {
                if (entity == null)
                    entity = Streams.map(getClassMeta(declaringClass).getIds(), ID::getColumn);

                if (entity.get(i) == null) {
                    throw new IllegalStateException(
                            "referencedColumnName not specified on field: " + field);
                }
            } else {
                if (entity == null)
                    entity = new ArrayList<>();
                entity.add(referencedColumnName);
            }

            join.add(Strings.isNullOrEmpty(columnName)
                    ? concatWithUnderscore(declaringTableName, entity.get(i))
                    : columnName);
        }
        return entity;
    }

    private static Association getAssociation(Member field,
                                              Class<?> targetEntity,
                                              String mappedBy,
                                              boolean left) {

        AnnotatedElement leftField = (AnnotatedElement) field;
        Class<?> type = targetEntity == void.class ? getType(field) : targetEntity;

        if (Strings.isNullOrEmpty(mappedBy)) {

            // should have JoinColumn or defaults
            JoinColumn[] joins;
            JoinColumn joinColumn = leftField.getAnnotation(JoinColumn.class);
            if (joinColumn != null)
                joins = new JoinColumn[]{joinColumn};
            else {
                JoinColumns joinColumns = leftField.getAnnotation(JoinColumns.class);
                joins = joinColumns != null ? joinColumns.value() : null;
            }

            List<CharSequence> that;
            List<CharSequence> other;
            if (joins != null) {
                other = new ArrayList<>();
                that = new ArrayList<>();

                for (int i = 0; i < joins.length; i++) {
                    JoinColumn join = joins[i];

                    if (!Strings.isNullOrEmpty(join.referencedColumnName()))
                        other.add(join.referencedColumnName());
                    else {
                        // extension to Standard: HN seems to go positional here
                        List<ID> typeIds = getClassMeta(type).getIds();
                        other.add(typeIds.get(i).getColumn());
                    }

                    if (!Strings.isNullOrEmpty(join.name()))
                        that.add(join.name());
                    else {
                        that.add(concatWithUnderscore(getColumnName(field), other.get(other.size() - 1)));
                    }

                }
            } else {

                MapsId mapsId = leftField.getAnnotation(MapsId.class);
                if (mapsId != null) {
                    List<ID> leftId = getClassMeta(field.getDeclaringClass()).getIds();
                    String idPath = mapsId.value();
                    if (!Strings.isNullOrEmpty(idPath)) {
                        leftId = Collections
                                .singletonList(
                                        leftId.stream().filter(id -> isIDMapped(id, idPath)).findFirst().get());
                    }
                    List<ID> id = getClassMeta(type).getIds();
                    return new Association(Streams.map(leftId, ID::getColumn), Streams.map(id, ID::getColumn), left);
                }

                other = Streams.map(getClassMeta(type).getIds(), ID::getColumn);
                that = Collections.singletonList(isEmbeddable(field.getDeclaringClass()) ?
                        getColumnName(field) :
                        concatWithUnderscore(getColumnName(field), other.get(0)));
            }

            return new Association(that, other, left);

        } else {
            return getAssociation(resolveMappedBy(type, mappedBy), !left);
        }
    }

    private static boolean isIDMapped(ID id,
                                      String mappedIdPath) {
        CharSequence path = id.getPath();
        int firstDot = Strings.indexOf(path, DOT);
        int secondDot = path.length();
        if (firstDot >= 0) {
            firstDot++;
            secondDot = Strings.indexOf(path, DOT, firstDot);
            if (secondDot < 0)
                secondDot = path.length();
        } else {
            firstDot = 0;
        }

        int len = secondDot - firstDot;
        if (len != mappedIdPath.length())
            return false;

        return Strings.compare(mappedIdPath, 0, path, firstDot, len) == 0;
    }

    private static String buildFullTableName(String catalog,
                                             String schema,
                                             String name) {
        if (schema.length() > 0 || catalog.length() > 0) {
            StringBuilder b = new StringBuilder();
            if (catalog.length() > 0)
                b.append(catalog).append(DOT);
            if (schema.length() > 0)
                b.append(schema).append(DOT);

            name = b.append(name).toString();
        }

        return name;
    }

    private static <T extends Annotation> T getAnnotationRecursive(Class<?> entity,
                                                                   Class<T> annotation) {
        T tableA = entity.getAnnotation(annotation);
        if (tableA != null)
            return tableA;

        entity = entity.getSuperclass();
        if (!isEntityLike(entity))
            return null;
        return getAnnotationRecursive(entity, annotation);
    }

    private static <T extends Annotation> Class<?> getAnnotatedTypeRecursive(Class<?> entity,
                                                                             Class<T> annotation) {
        T tableA = entity.getAnnotation(annotation);
        if (tableA != null)
            return entity;

        entity = entity.getSuperclass();
        if (!isEntityLike(entity))
            return null;
        return getAnnotatedTypeRecursive(entity, annotation);
    }

    private static SecondaryTable getSecondaryTableAnnotation(Class<?> entity,
                                                              String secondary) {
        if (!isEntityLike(entity))
            throw TranslationError.SECONDARY_TABLE_NOT_FOUND.getError(entity, secondary);
        SecondaryTable tableA = entity.getAnnotation(SecondaryTable.class);
        if (tableA != null) {
            if (Strings.isNullOrEmpty(secondary) || secondary.equalsIgnoreCase(tableA.name()))
                return tableA;

            throw TranslationError.SECONDARY_TABLE_NOT_FOUND.getError(entity, secondary);
        }

        SecondaryTables secTables = entity.getAnnotation(SecondaryTables.class);
        if (secTables != null) {
            if (secondary.isEmpty()) {
                if (secTables.value().length == 1)
                    return secTables.value()[0];

                throw TranslationError.SECONDARY_TABLE_NOT_FOUND.getError(entity, secondary);
            } else {
                Optional<SecondaryTable> found = Stream.of(secTables.value())
                        .filter(st -> secondary.equalsIgnoreCase(st.name()))
                        .findFirst();
                return found.orElseThrow(() -> TranslationError.SECONDARY_TABLE_NOT_FOUND.getError(entity, secondary));
            }
        }

        return getSecondaryTableAnnotation(entity.getSuperclass(), secondary);
    }

    public static Class<?> getInheritanceBaseType(Class<?> entity) {
        Class<?> type = getAnnotatedTypeRecursive(entity, Inheritance.class);
        if (type == null)
            throw TranslationError.INHERITANCE_STRATEGY_NOT_FOUND.getError(entity);
        return type;
    }

    public static SecondaryTable getSecondaryTable(Class<?> entity,
                                                   String secondary) {
        return getSecondaryTableAnnotation(entity, secondary);
    }

    public static String getTableName(SecondaryTable secondaryTable) {
        return buildFullTableName(secondaryTable.catalog(), secondaryTable.schema(), secondaryTable.name());
    }

    public static String getTableName(Class<?> entity) {
        if (!isEntityLike(entity))
            return null;
        Table tableA = getAnnotationRecursive(entity, Table.class);
        String name;

        if (tableA != null) {

            name = tableA.name();
            if (!Strings.isNullOrEmpty(name))
                return buildFullTableName(tableA.catalog(), tableA.schema(), name);
        }

        Entity ent = entity.getAnnotation(Entity.class);
        if (ent != null)
            name = ent.name();
        else {
            Tuple tuple = entity.getAnnotation(Tuple.class);
            if (tuple != null) {
                String tupleName = tuple.value();
                return tupleName.length() > 0 ? tupleName : null;
            }

            name = null;
        }

        if (!Strings.isNullOrEmpty(name))
            return name;

        return entity.getSimpleName();
    }

    public static boolean isCollection(Class<?> entity) {
        return Collection.class.isAssignableFrom(entity);
    }

    public static boolean isScalar(Class<?> entity) {
        // assuming all scalars must be Comparable
        return entity.isPrimitive() || Comparable.class.isAssignableFrom(entity) || Number.class.isAssignableFrom(entity);
    }

    public static boolean isEntity(Class<?> entity) {
        return entity.isAnnotationPresent(Entity.class);
    }

    public static boolean isEntityLike(Class<?> entity) {
        return isEntity(entity) || entity.isAnnotationPresent(Tuple.class);
    }

    public static boolean isEmbeddable(Class<?> entity) {
        return entity.isAnnotationPresent(Embeddable.class);
    }

    public static boolean isEmbedded(Member field) {
        AnnotatedElement annotated = (AnnotatedElement) getAnnotatedField(field);
        return annotated.isAnnotationPresent(Embedded.class) || annotated.isAnnotationPresent(EmbeddedId.class);
    }

    private static IdentifierPath getColumnName(Member field) {
        Column column = ((AnnotatedElement) field).getAnnotation(Column.class);
        if (column != null) {
            CharSequence cname = column.name();
            if (Strings.isNullOrEmpty(cname))
                cname = toDBNotation(getFieldName(field));

            return new IdentifierPath.Resolved(cname, field.getDeclaringClass(), field.getName(), column.table());
        }

        JoinColumn join = ((AnnotatedElement) field).getAnnotation(JoinColumn.class);
        if (join != null) {
            CharSequence cname = join.name();
            if (Strings.isNullOrEmpty(cname))
                cname = toDBNotation(getFieldName(field));

            return new IdentifierPath.Resolved(cname, field.getDeclaringClass(), field.getName(), join.table());
        }

        JoinColumns joins = ((AnnotatedElement) field).getAnnotation(JoinColumns.class);
        if (joins != null)
            return new IdentifierPath.MultiColumnIdentifierPath(field.getName(), c -> getAssociation(field, true),
                    joins.value()[0].table());

        MapsId mapsId = ((AnnotatedElement) field).getAnnotation(MapsId.class);
        if (mapsId != null) {

            Class<?> declaringClass = field.getDeclaringClass();
            List<ID> entityIds = getClassMeta(declaringClass).getIds();
            String idPath = mapsId.value();
            if (Strings.isNullOrEmpty(idPath)) {
                // should be the sole PK column
                return entityIds.size() == 1
                        ? new IdentifierPath.Resolved(entityIds.get(0).getColumn(), field.getDeclaringClass(),
                        field.getName(), null)
                        : // if this case is possible, we are covered
                        new IdentifierPath.MultiColumnIdentifierPath(field.getName(), c -> getAssociation(field, true),
                                null);
            }

            return new IdentifierPath.Resolved(
                    entityIds.stream().filter(id -> isIDMapped(id, idPath)).findFirst().get().getColumn(),
                    field.getDeclaringClass(), field.getName(), null);
        }

        return new IdentifierPath.Resolved(toDBNotation(getFieldName(field)), field.getDeclaringClass(),
                field.getName(), null);
    }

    public static ClassMeta getClassMeta(Class<?> declaringClass) {
        return ids.computeIfAbsent(declaringClass, JPAHelpers::findId);
    }

    public static Field getField(Member m1) {

        if (membersToFields.size() > 10000)
            membersToFields.clear();

        return membersToFields.computeIfAbsent(m1, m -> {

            String original = m.getName();

            String name = getFieldName(m);
            String decapitalized = decapitalize(name);
            Class<?> clazz = m.getDeclaringClass();
            for (; ; ) {
                try {
                    return clazz.getDeclaredField(decapitalized);
                } catch (NoSuchFieldException e) {
                    try {
                        return clazz.getDeclaredField(name);
                    } catch (NoSuchFieldException e1) {
                        try {
                            return clazz.getDeclaredField(original);
                        } catch (NoSuchFieldException e2) {
                            clazz = clazz.getSuperclass();
                            if (clazz == Object.class || clazz == null)
                                throw TranslationError.UNMAPPED_FIELD.getError(e2, original);
                        }
                    }
                }
            }
        });
    }

    private static String getFieldName(Member m) {
        String name = m.getName();
        if (m instanceof Method) {
            if (name.startsWith("is"))
                name = name.substring(2);
            else if (name.startsWith("get") || name.startsWith("set"))
                name = name.substring(3);
        }
        return name;
    }

    public static IdentifierPath getColumnNameFromProperty(Member member) {
        return getColumnName(getAnnotatedField(member));
    }

    public static <T extends Annotation> T getAnnotationFromProperty(Member member,
                                                                     Class<T> annotationClass) {
        AnnotatedElement annotated = (AnnotatedElement) getAnnotatedField(member);
        return annotated.getAnnotation(annotationClass);
    }

    public static String getJoinTableName(Member member) {

        Member field = getAnnotatedField(member);
        AnnotatedElement annotated = (AnnotatedElement) field;
        ManyToMany mtm = annotated.getAnnotation(ManyToMany.class);

        String mappedBy = mtm.mappedBy();

        if (mappedBy.length() > 0) {
            field = resolveMappedBy(getTargetForMTM(field), mappedBy);
            annotated = (AnnotatedElement) field;
        }

        String name = "", catalog = "", schema = "";

        JoinTable joinTable = annotated.getAnnotation(JoinTable.class);

        if (joinTable != null) {
            name = joinTable.name();
            catalog = joinTable.catalog();
            schema = joinTable.schema();
        }

        Member f = field;
        Supplier<Class<?>> targetSupplier = () -> getTargetForMTM(f);
        return getJoinTableName(field, name, catalog, schema, targetSupplier);
    }

    public static String getECTableName(Member member) {

        Member field = getAnnotatedField(member);
        AnnotatedElement annotated = (AnnotatedElement) field;

        String name = "", catalog = "", schema = "";

        CollectionTable joinTable = annotated.getAnnotation(CollectionTable.class);

        if (joinTable != null) {
            name = joinTable.name();
            catalog = joinTable.catalog();
            schema = joinTable.schema();
        }

        Member f = field;
        Supplier<Class<?>> targetSupplier = () -> getTargetForECAnnotated(f);
        return getJoinTableName(field, name, catalog, schema, targetSupplier);
    }

    private static Class<?> getTargetForMTM(Member field) {

        AnnotatedElement annotated = (AnnotatedElement) field;
        ManyToMany mtm = annotated.getAnnotation(ManyToMany.class);

        Class<?> target = mtm.targetEntity();
        if (target == void.class)
            target = getTargetByParameterizedType(field);

        return target;
    }

    public static Class<?> getTargetForEC(Member field) {
        return getTargetForECAnnotated(getAnnotatedField(field));
    }

    private static Class<?> getTargetForECAnnotated(Member field) {

        AnnotatedElement annotated = (AnnotatedElement) field;
        ElementCollection mtm = annotated.getAnnotation(ElementCollection.class);

        Class<?> target = mtm.targetClass();
        if (target == void.class)
            target = getTargetByParameterizedType(field);

        return target;
    }

    private static String getJoinTableName(Member field,
                                           String name,
                                           String catalog,
                                           String schema,
                                           Supplier<Class<?>> target) {
        if (Strings.isNullOrEmpty(name)) {
            String first = getTableName(field.getDeclaringClass());
            name = concatWithUnderscore(first, getTableName(target.get()));
        }

        return buildFullTableName(catalog, schema, name);
    }

    private static Class<?> getTargetByParameterizedType(Member member) {
        ParameterizedType genericType = (ParameterizedType) (member instanceof Field ? ((Field) member).getGenericType()
                : ((Method) member).getGenericReturnType());
        Type[] types = genericType.getActualTypeArguments();
        return (Class<?>) types[0];
    }

    private static String concatWithUnderscore(CharSequence first,
                                               CharSequence second) {
        return first + "_" + second;
    }

    public static Member getAnnotatedField(Member member) {
        return getAccessType(member.getDeclaringClass()) == AccessType.PROPERTY ? member : getField(member);
    }

    private static Member resolveMappedBy(Class<?> type,
                                          String mappedBy) {
        SneakyBiFunction<Class<?>, String, ? extends Member> memberAccessor = getAccessType(type) == AccessType.PROPERTY
                ? (clazz,
                   name) -> clazz.getDeclaredMethod(name, (Class<?>[]) null)
                : Class::getDeclaredField;

        for (; ; ) {
            try {
                return memberAccessor.apply(type, mappedBy);
            } catch (Exception e) {
                type = type.getSuperclass();
                if (type == Object.class || type == null)
                    throw new RuntimeException(e);
            }
        }

    }

    /**
     * Utility method to take a string and convert it to normal Java variable name capitalization. This normally means
     * converting the first character from upper case to lower case, but in the (unusual) special case when there is
     * more than one character and both the first and second characters are upper case, we leave it alone.
     * <p>
     * Thus "FooBah" becomes "fooBah" and "X" becomes "x", but "URL" stays as "URL".
     *
     * @param name The string to be decapitalized.
     * @return The decapitalized version of the string.
     */
    private static String decapitalize(String name) {
        if (Strings.isNullOrEmpty(name)) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) && Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        char chars[] = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    private static CharSequence toDBNotation(String name) {
        if (Strings.isNullOrEmpty(name)) {
            return name;
        }

        StringBuilder out = null;
        boolean foundUppercase = true;
        for (int i = 0; i < name.length(); i++) {
            char charAtI = name.charAt(i);
            if (Character.isUpperCase(charAtI)) {
                if (out == null) {
                    out = new StringBuilder(name.length() + 6).append(name, 0, i);
                }

                if (!foundUppercase) {
                    out.append(UNDERSCORE);
                    foundUppercase = true;
                }

                out.append(Character.toLowerCase(charAtI));
            } else {
                foundUppercase = false;

                if (out != null)
                    out.append(charAtI);
            }
        }

        return out != null ? out : name;
    }
}
