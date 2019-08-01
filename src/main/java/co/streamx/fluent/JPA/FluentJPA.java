package co.streamx.fluent.JPA;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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
import javax0.license3j.License;
import javax0.license3j.io.IOFormat;
import javax0.license3j.io.LicenseReader;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
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
     * @return true if license is valid and not previously checked
     * @throws IOException
     */
    public static boolean checkLicense(InputStream licStream,
                                       boolean suppressBanner)
            throws IOException {
        if (licenseChecked.compareAndSet(false, true)) {
            boolean closeStream = licStream == null;
            try {
                if (closeStream)
                    licStream = FluentJPA.class.getClassLoader().getResourceAsStream("fluent-jpa.lic");
                if (licStream == null) {
                    reportNoLicense();
                } else {
                    return checkLicense0(licStream, suppressBanner);
                }
            } catch (IOException e) {
                log.warn("Licence check failed", e);
                reportNoLicense();
                throw e;
            } finally {
                if (closeStream && licStream != null)
                    licStream.close();
            }
        }

        return false;
    }

    private static boolean checkLicense0(InputStream licStream,
                                        boolean suppressBanner)
            throws IOException {
        try (InputStream keyStream = FluentJPA.class.getClassLoader().getResourceAsStream("public.lic");
                LicenseReader licenseReader = new LicenseReader(licStream)) {
            License lic = licenseReader.read(IOFormat.STRING);

            byte[] key = new byte[298]; // size of the public.lic file
            keyStream.read(key);

            boolean ok = lic.isOK(key);
            if (!ok) {
                reportNoLicense();
                return false;
            }

            Date expiration = lic.get("expiration").getDate();
            if (expiration.getTime() < System.currentTimeMillis()) {
                reportLicenseExpired();
                return false;
            }

            if (!suppressBanner) {
                String project = lic.get("project").getString();
                reportLicenseOk(project);
            }

            return true;
        }
    }

    private static void reportLicenseOk(String project) {
        printBanner("Thank you for using FluentJPA in the awesome '" + project + "' project!");
    }

    private static void reportNoLicense() {
        printBanner(
                "Thank you for using FluentJPA!\nNo valid FluentJPA commercial license file was found.\nFluentJPA is licensed under AGPLv3.\nPlease verify your product license is compliant.");
    }

    private static void reportLicenseExpired() {
        printBanner(
                "Thank you for using FluentJPA!\nFluentJPA commercial license has expired, please renew.\nFluentJPA is licensed under AGPLv3.\nPlease verify your product license is compliant.");
    }


    private static void printBanner(String message) {
        char[] dashes = new char[message.length()];
        Arrays.fill(dashes, '#');
        System.out.println(dashes);
        System.out.println();
        System.out.println(message);
        System.out.println();
        System.out.println(dashes);
        System.out.println();
    }

    public static SQLConfigurator SQLConfig() {
        return SQLConfig;
    }

    private static FluentQuery SQL(Object fluentQuery) {

        try {
            checkLicense(null, false);
        } catch (Exception e) {
            // suppress
        }

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
