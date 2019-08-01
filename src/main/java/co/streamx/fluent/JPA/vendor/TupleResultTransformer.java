package co.streamx.fluent.JPA.vendor;

import java.util.List;

public interface TupleResultTransformer<T> {
    T transformTuple(Object[] tuple,
                     String[] aliases);

    List<T> transformList(List<T> collection);
}
