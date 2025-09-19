package co.streamx.fluent.JPA;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

final class TypedQueryWrapper<X> implements InvocationHandler {

    private Query query;

    private TypedQueryWrapper(Query query) {
        this.query = query;
    }

    public static <X> TypedQuery<X> wrap(Query query) {
        return new TypedQueryWrapper<X>(query).createProxy();
    }

    @SuppressWarnings("unchecked")
    private TypedQuery<X> createProxy() {
        return (TypedQuery<X>) Proxy.newProxyInstance(
                query.getClass().getClassLoader(),
                new Class<?>[]{TypedQuery.class},
                this
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        try {
            String methodName = method.getName();
            Method queryMethod = Query.class.getMethod(methodName, method.getParameterTypes());

            // Delegate to Query interface
            Object result = queryMethod.invoke(query, args);

            // Handle setter methods - they should return this proxy
            if (methodName.startsWith("set")) {
                query = (Query) result;
                return proxy;
            }

            // If the result is the query itself, return our proxy instead
            if (result == query) {
                return proxy;
            }

            return result;
        } catch (InvocationTargetException ite) {
            throw ite.getCause();
        }
    }

}
