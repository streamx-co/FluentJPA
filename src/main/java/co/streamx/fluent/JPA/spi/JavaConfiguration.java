package co.streamx.fluent.JPA.spi;

import java.util.ServiceLoader;

import co.streamx.fluent.functions.Function0;
import co.streamx.fluent.functions.Function1;
import co.streamx.fluent.functions.Function2;
import co.streamx.fluent.notation.Function;

public class JavaConfiguration implements JPAConfiguration {
    public JavaConfiguration() {
        ServiceLoader<SQLConfigurator> loader = ServiceLoader.load(SQLConfigurator.class);
        SQLConfigurator SQLConfig = loader.iterator().next();

        SQLConfig.registerMethodSubstitution(String::valueOf, JavaConfiguration::valueOf);
        SQLConfig.registerMethodSubstitution(Object::toString, JavaConfiguration::valueOf);

        // limitation: interferes with association calculation
        // SQLConfig.registerMethodSubstitution(Object::equals, (a, b) -> a == b);

        SQLConfig.registerMethodSubstitution((Function2<StringBuilder, Object, CharSequence>) StringBuilder::append,
                (CharSequence s,
                 Object x) -> valueOf(s).concat(valueOf(x)));

        SQLConfig.registerMethodSubstitution((Function1<String, CharSequence>) StringBuilder::new, (String s) -> s);

        SQLConfig.registerMethodSubstitution((Function0<CharSequence>) StringBuilder::new, () -> "");

    }

    @Function(name = "", omitParentheses = true)
    public static String valueOf(Object o) {
        throw new UnsupportedOperationException();
    }
}
