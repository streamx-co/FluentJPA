package co.streamx.fluent.JPA.vendor;

import javax.persistence.Query;

public interface ResultTransformerInstaller {
    void install(Query query,
                 TupleResultTransformer<?> transformer);
}
