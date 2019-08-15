package co.streamx.fluent.JPA.spi;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.util.Date;
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

        // Time conversion
        SQLConfig.registerMethodSubstitution(Date::toInstant, JavaConfiguration::toTemporal);
        SQLConfig.registerMethodSubstitution(Date::from, JavaConfiguration::toDate);
        SQLConfig.registerMethodSubstitution(java.sql.Date::toLocalDate, JavaConfiguration::toTemporal);
        SQLConfig.registerMethodSubstitution((Function1<LocalDate, java.sql.Date>) java.sql.Date::valueOf,
                JavaConfiguration::toDate);
        SQLConfig.registerMethodSubstitution(Time::toLocalTime, JavaConfiguration::toTemporal);
        SQLConfig.registerMethodSubstitution((Function1<LocalTime, Time>) Time::valueOf, JavaConfiguration::toDate);
        SQLConfig.registerMethodSubstitution(Timestamp::toLocalDateTime, JavaConfiguration::toTemporal);
        SQLConfig.registerMethodSubstitution((Function1<LocalDateTime, Timestamp>) Timestamp::valueOf,
                JavaConfiguration::toDate);

    }

    @Function(name = "", omitParentheses = true)
    public static String valueOf(Object o) {
        throw new UnsupportedOperationException();
    }

    @Function(name = "", omitParentheses = true)
    public static <T extends Temporal> T toTemporal(Date o) {
        throw new UnsupportedOperationException();
    }

    @Function(name = "", omitParentheses = true)
    public static <T extends Date> T toDate(Object o) {
        throw new UnsupportedOperationException();
    }
}
