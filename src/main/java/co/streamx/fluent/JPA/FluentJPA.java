package co.streamx.fluent.JPA;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import co.streamx.fluent.JPA.spi.JPAConfiguration;
import co.streamx.fluent.JPA.spi.SQLConfigurator;
import co.streamx.fluent.extree.expression.LambdaExpression;
import co.streamx.fluent.functions.Consumer0;
import co.streamx.fluent.functions.Consumer1;
import co.streamx.fluent.functions.Consumer2;
import co.streamx.fluent.functions.Consumer3;
import co.streamx.fluent.functions.Consumer4;
import co.streamx.fluent.functions.Consumer5;
import co.streamx.fluent.functions.Consumer6;
import co.streamx.fluent.notation.Capability;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Fluent JPA entry point. Use its static <code>SQL</code> methods to create native SQL queries.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class FluentJPA {

    private static final SQLConfigurator SQLConfig;
    @Getter
    @Setter
    @NonNull
    private static Set<Capability> capabilities = Collections.emptySet();

    private static final AtomicBoolean licenseChecked = new AtomicBoolean();
    private static volatile boolean isLicensed = true;

    private static final String DEBUG_MODE = "Debug mode";

    static {
        ServiceLoader<SQLConfigurator> loader = ServiceLoader.load(SQLConfigurator.class);
        SQLConfig = loader.iterator().next();

        ServiceLoader<JPAConfiguration> loader1 = ServiceLoader.load(JPAConfiguration.class);
        for (@SuppressWarnings("unused")
        JPAConfiguration conf : loader1)
            ;
    }

    /**
     * checks FluentJPA license
     * 
     * @param licStream      if null, will try to load fluent-jpa.lic file from root
     * @param suppressBanner suppresses license banner if license is valid
     * @return true if license is valid
     */
    @SneakyThrows
    public static boolean checkLicense(InputStream licStream,
                                       boolean suppressBanner) {
        if (licenseChecked.compareAndSet(false, true)) {

            String versionString = System.getProperty("java.version");
            Pattern p = Pattern.compile("\\D");
            Matcher matcher = p.matcher(versionString);
            if (matcher.find()) {
                int dot = matcher.start();
                versionString = versionString.substring(0, dot);
            }

            int version = Integer.parseInt(versionString);
            if (version > 14) {
                isLicensed = false;
                throw new UnsupportedClassVersionError(
                        "Java " + versionString + " is not supported by this FluentJPA version.");
            }

            boolean needLicense = false;
            List<String> arguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
            for (String arg : arguments) {
                needLicense = arg.startsWith("-Xrunjdwp") || arg.startsWith("-agentlib:jdwp");
                if (needLicense)
                    break;
            }

            if (!needLicense)
                return isLicensed;

            boolean closeStream = licStream == null;
            try {
                if (closeStream)
                    licStream = FluentJPA.class.getClassLoader().getResourceAsStream("fluent-jpa.lic");

                String key;
                if (licStream == null) {
                    key = null;
                } else {
                    Reader r = new InputStreamReader(licStream, StandardCharsets.US_ASCII);
                    BufferedReader bufferedReader = new BufferedReader(r);
                    key = bufferedReader.lines().collect(Collectors.joining());
                }

                License.validate(key);
                if (key != null)
                    License.reportLicenseOk();
                else
                    License.reportNoLicense();
                isLicensed = true;

            } catch (Exception e) {
                log.warn("Licence check failed", e);
                isLicensed = false;
            } finally {
                if (closeStream && licStream != null)
                    licStream.close();
            }
        }

        return isLicensed;
    }

    /**
     * A shortcut for {@code checkLicense(null, false)}
     */
    public static boolean checkLicense() {
        return checkLicense(null, false);
    }

    public static SQLConfigurator SQLConfig() {
        return SQLConfig;
    }

    private static FluentQuery SQL(Object fluentQuery) {

//        if (!checkLicense())
//            throw TranslationError.REQUIRES_LICENSE.getError(DEBUG_MODE);

        LambdaExpression<?> parsed = LambdaExpression.parse(fluentQuery);

        return new FluentQueryImpl(parsed, true);
    }

    /**
     * Creates native SQL {@link FluentQuery}
     */
    public static FluentQuery SQL(Consumer0 fluentQuery) {
        return SQL((Object) fluentQuery);
    }

    /**
     * Creates native SQL {@link FluentQuery}
     */
    public static FluentQuery SQL(Consumer1<?> fluentQuery) {
        return SQL((Object) fluentQuery);
    }

    /**
     * Creates native SQL {@link FluentQuery}
     */
    public static FluentQuery SQL(Consumer2<?, ?> fluentQuery) {
        return SQL((Object) fluentQuery);
    }

    /**
     * Creates native SQL {@link FluentQuery}
     */
    public static FluentQuery SQL(Consumer3<?, ?, ?> fluentQuery) {
        return SQL((Object) fluentQuery);
    }

    /**
     * Creates native SQL {@link FluentQuery}
     */
    public static FluentQuery SQL(Consumer4<?, ?, ?, ?> fluentQuery) {
        return SQL((Object) fluentQuery);
    }

    /**
     * Creates native SQL {@link FluentQuery}
     */
    public static FluentQuery SQL(Consumer5<?, ?, ?, ?, ?> fluentQuery) {
        return SQL((Object) fluentQuery);
    }

    /**
     * Creates native SQL {@link FluentQuery}
     */
    public static FluentQuery SQL(Consumer6<?, ?, ?, ?, ?, ?> fluentQuery) {
        return SQL((Object) fluentQuery);
    }
}
