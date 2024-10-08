package co.streamx.fluent.JPA;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import lombok.RequiredArgsConstructor;
//import lombok.SneakyThrows;
import lombok.ToString;

interface TupleResultTransformerImplHelpers {
    interface Setter {
        Class<?> getType();

        void set(Object target,
                 Object value)
                throws InvocationTargetException, IllegalAccessException, InstantiationException;

        Getter getter();
    }

    @FunctionalInterface
    interface Getter {
        Object get(Object target) throws InvocationTargetException, IllegalAccessException, InstantiationException;
    }

    @ToString
    final class MethodSetter implements Setter {

        private final Method set;
        private final Method get;

        public MethodSetter(Method getter) {
            this.get = getter;
            try {
                this.set = setterFromGetter(getter);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Class<?> getType() {
            return set.getReturnType();
        }

        @Override
        public void set(Object target,
                        Object value)
                throws InvocationTargetException, IllegalAccessException {
            set.invoke(target, value);
        }

        @Override
        public Getter getter() {
            return new MethodGetter(get);
        }

//        @SneakyThrows
        private static Method setterFromGetter(Method getter) throws NoSuchMethodException {
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

    @ToString
    final class FieldSetter implements Setter {

        private final Field field;

        public FieldSetter(Field field) {
            this.field = field;

            if (!field.isAccessible())
                field.setAccessible(true);
        }

        @Override
        public Class<?> getType() {
            return field.getType();
        }

        @Override
        public void set(Object target,
                        Object value)
                throws InvocationTargetException, IllegalAccessException {
            field.set(target, value);
        }

        @Override
        public Getter getter() {
            return new FieldGetter(field);
        }
    }

    static Setter getSetter(Member getter,
                            Setter baseSetter) {
        Setter innerSetter = (getter instanceof Method) ? new MethodSetter((Method) getter)
                : new FieldSetter((Field) getter);
        if (baseSetter == null)
            return innerSetter;

        return new NestedSetter(baseSetter.getter(), baseSetter, innerSetter);
    }

    @RequiredArgsConstructor
    @ToString
    final class MethodGetter implements Getter {

        private final Method get;

        @Override
        public Object get(Object target) throws InvocationTargetException, IllegalAccessException {
            return get.invoke(target, (Object[]) null);
        }
    }

    @ToString
    final class FieldGetter implements Getter {

        private final Field field;

        public FieldGetter(Field field) {
            this.field = field;

            if (!field.isAccessible())
                field.setAccessible(true);
        }

        @Override
        public Object get(Object target) throws IllegalAccessException {
            return field.get(target);
        }
    }

    @RequiredArgsConstructor
    @ToString
    final class NestedSetter implements Setter {
        private final Getter getter;
        private final Setter setter;
        private final Setter nestedSetter;

        @Override
        public Class<?> getType() {
            return nestedSetter.getType();
        }

        @Override
        public void set(Object target,
                        Object value)
                throws InvocationTargetException, IllegalAccessException, InstantiationException {

            Object nested = getNested(target);
            nestedSetter.set(nested, value);
        }

        private Object getNested(Object target)
                throws InvocationTargetException, IllegalAccessException, InstantiationException {
            Object nested = getter.get(target);
            if (nested == null) {
                nested = setter.getType().newInstance();
                setter.set(target, nested);
            }
            return nested;
        }

        @Override
        public Getter getter() {

            Getter nestedGetter = nestedSetter.getter();

            return target -> nestedGetter.get(getNested(target));
        }
    }
}
