package co.streamx.fluent.JPA;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.management.openmbean.InvalidKeyException;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

class License {

    private static boolean validateToken(String token,
                                         String fingerprint) {
        JWTVerifier verifier = getVerifier();

        try {

            DecodedJWT jwt = verifier.verify(token);

            Map<String, Claim> claims = jwt.getClaims();

            String fp = claims.get("fp").asString();
            if (!fingerprint.equals(fp))
                return false;

            Claim leat = claims.get("leat");
            if (leat == null)
                leat = claims.get("eat");
            Date asDate = leat.asDate();
            long oneHour = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);
            boolean expired = System.currentTimeMillis() >= asDate.getTime() + oneHour;

//      System.out.println(asDate + " expired: " + expired);

            return !expired;
        } catch (JWTVerificationException e) {
            return false;
        }

    }

    private static final String pem = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAteuLzI/1WNVjJYZ2GjvB"
            + "s7PZSfaWaFHSzYC2I7tdK37RaKmF4C7Vy31gZbwrsOvs3PuBgXsCeJVRcX76staN"
            + "1yqA0DvOjS+GX44LTc/YR+Z83g4ZGmY7i08k9D8crVDIh5BtHoPGRi60Pzm7F/GS"
            + "Dj9tPRpgYIajK3gjk+L5x9oq87AkiMPFOf1nxTTUho/w8qM083+7l6aAZUowixDn"
            + "j2bvrKj63+LofUt2/dNNsLKuWajk+Y+FxAxIIS0y3j1kAW1+6YupDBRzqHH/SC5y"
            + "WGx2vsWVOewWhQhkLXFvjtMax1mvo0mZwiTAv4QvY6gLAlO5xARRyyZl/zePgj+A" + "PQIDAQAB";

    @SneakyThrows
    private static JWTVerifier getVerifier() {
        // publicKeyPEM = publicKeyPEM.replace("-----BEGIN PUBLIC KEY-----", "");
        // publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");
        byte[] encoded = Base64.getDecoder().decode(pem);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(encoded));

        Algorithm algorithm = Algorithm.RSA256(publicKey, null);
        return JWT.require(algorithm).build(); // Reusable verifier instance
    }

    @SneakyThrows
    public static void validate(String key) {

        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path tokenFile = tempDir.resolve("streamx.session");
        String fingerprint = getFingerprint();

        try {
            String exestingToken = Files.lines(tokenFile).collect(Collectors.joining());
            if (validateToken(exestingToken, fingerprint))
                return;
        } catch (IOException io) {
            // continue validating
        }

        CharSequence payload = getPayload(key, fingerprint);

        String service = key != null ? "activations" : "trial-activations";

        HttpURLConnection con;

        try {
            URL url = new URL("https://api.cryptlex.com/v3/" + service);
            con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.connect();
        } catch (IOException io) {
            return;
        }

        try (OutputStream out = con.getOutputStream()) {
            OutputStreamWriter writer = new OutputStreamWriter(out);

            writer.append(payload);
            writer.flush();
        }

        int retCode = con.getResponseCode();

        if (retCode < 400) {

            try (InputStream in = con.getInputStream()) {

                String all = readAll(in);

                String field = key != null ? "activationToken" : "trialActivationToken";

                Pattern pattern = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"");
                String token = getMatch(pattern, all);

                if (!validateToken(token, fingerprint))
                    return; // Hmmm

                try {
                    Path tempFile = Files.createTempFile(tempDir, null, null);
                    try (Writer w = Files.newBufferedWriter(tempFile)) {
                        w.append(token);
                    }

                    Files.move(tempFile, tokenFile, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    // ignore
                }

            }
        } else {

            try (InputStream in = con.getErrorStream()) {

                String all = readAll(in);

                Pattern pattern = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");
                String message = getMatch(pattern, all);

                pattern = Pattern.compile("\"code\"\\s*:\\s*\"([^\"]+)\"");
                String code = getMatch(pattern, all);

                throw new InvalidKeyException("Error loading FluentJPA: " + message + " Reason: " + code);
            }
        }
    }

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 3];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = HEX_ARRAY[v >>> 4];
            hexChars[j * 3 + 1] = HEX_ARRAY[v & 0x0F];
            hexChars[j * 3 + 2] = ':';
        }
        return new String(hexChars, 0, hexChars.length - 1);
    }

    private static boolean isRunningInsideDocker() {

        try (Stream<String> stream = Files.lines(Paths.get("/proc/self/cgroup"))) {
            return stream.anyMatch(line -> line.contains("/docker"));
        } catch (IOException e) {
            return false;
        }
    }

    @SneakyThrows
    private static CharSequence getPayload(String key,
                                           String fingerprint) {

        InetAddress localHost = InetAddress.getLocalHost();

        String os = System.getProperty("os.name").toLowerCase();
        if (os.startsWith("mac"))
            os = "macos";
        else if (!"linux".equals(os) && !"windows".equals(os) && !"android".equals(os))
            os = "ios";

        JSONBuilder payload = JSONBuilder.start();

        if (key != null)
            payload = payload.string("key", key);

        boolean docker = isRunningInsideDocker();
        if (docker)
            payload.string("vmName", localHost.getHostName());

        return payload.string("hostname", localHost.getHostName())
                .string("os", os)
                .string("fingerprint", fingerprint)
                .string("appVersion", "NA")
                .string("userHash", System.getProperty("user.name"))
                .string("productId", "ad65ea0b-45af-43e1-b87c-4bd48f01f78a")
                .end();
    }

    @SneakyThrows
    private static String getFingerprint() {
//        InetAddress localHost = InetAddress.getLocalHost();

        List<String> addresses = new ArrayList<>();

        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface ni = networkInterfaces.nextElement();
            if (ni.isLoopback() || !ni.isUp() || ni.isVirtual() || ni.getName().startsWith("docker"))
                continue;

            byte[] mac = ni.getHardwareAddress();
            if (mac == null)
                continue;

            Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress address = inetAddresses.nextElement();
                if (address.isSiteLocalAddress()) {
                    addresses.add(bytesToHex(mac));
                    break;
                }
            }
        }

        addresses.sort(null);
//        addresses.add(localHost.getHostName());
        String fingerprint = addresses.toString();

        if (fingerprint.length() < 64) {
            char[] filler = new char[64 - fingerprint.length()];
            Arrays.fill(filler, '=');
            fingerprint += new String(filler);
        }
        return fingerprint;
    }

    private static String getMatch(Pattern pattern,
                                   String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String readAll(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String all = null;
        String line;
        while ((line = reader.readLine()) != null) {
            all = all != null ? all + line : line;
        }
        return all;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static class JSONBuilder {

        private final StringBuilder builder = new StringBuilder("{");

        public static JSONBuilder start() {
            return new JSONBuilder();
        }

        public CharSequence end() {
            int lastChar = builder.length() - 1;
            if (builder.charAt(lastChar) == ',')
                builder.setCharAt(lastChar, '}');
            else
                builder.append('}');
            return builder;
        }

        public JSONBuilder string(String key,
                                  String value) {
            builder.append('"').append(key).append('"').append(':');
            builder.append('"').append(value).append('"').append(',');
            return this;
        }
    }

    public static void reportLicenseOk() {

        printBanner("Thank you for using FluentJPA!");
    }

    public static void reportNoLicense() {
        printBanner(
                "Thank you for using FluentJPA!\nNo valid FluentJPA license file was found.\nFluentJPA is in evaluation period.");
    }

    private static void printBanner(String message) {

        OptionalInt length = Arrays.stream(message.split("\n")).mapToInt(String::length).max();

        char[] dashes = new char[length.orElse(50)];
        Arrays.fill(dashes, '#');
        System.out.println(dashes);
        System.out.println();
        System.out.println(message);
        System.out.println();
        System.out.println(dashes);
        System.out.println();
    }
}
