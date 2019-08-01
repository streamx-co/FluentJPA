package co.streamx.fluent.JPA.vendor;

import java.util.List;

import javax.persistence.Query;

import org.hibernate.query.NativeQuery;
import org.hibernate.transform.ResultTransformer;

public class Hibernate5ResultTransformerInstaller implements ResultTransformerInstaller {

    @SuppressWarnings({ "deprecation", "serial" })
    @Override
    public void install(Query query,
                        TupleResultTransformer<?> transformer) {
        query.unwrap(NativeQuery.class).setResultTransformer(new ResultTransformer() {

            @Override
            public Object transformTuple(Object[] tuple,
                                         String[] aliases) {
                return transformer.transformTuple(tuple, aliases);
            }

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public List transformList(List collection) {
                return transformer.transformList(collection);
            }

        });
    }

}
