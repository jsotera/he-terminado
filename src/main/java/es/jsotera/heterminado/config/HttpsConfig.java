package es.jsotera.heterminado.config;

import io.javalin.config.JavalinConfig;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/** HTTPS directo y opcional: conserva la IP real del alumnado. */
public final class HttpsConfig {
    private HttpsConfig() {}

    public static boolean enabled() {
        return !System.getProperty("tls.keystore", "").isBlank();
    }

    public static int port() {
        return Integer.parseInt(System.getProperty("tls.port", "8443"));
    }

    public static void configure(JavalinConfig config) {
        if (!enabled()) return;
        String password = System.getenv("HE_TERMINADO_TLS_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Define HE_TERMINADO_TLS_PASSWORD para abrir el certificado HTTPS.");
        }
        SslContextFactory.Server ssl = new SslContextFactory.Server();
        ssl.setKeyStorePath(System.getProperty("tls.keystore"));
        ssl.setKeyStoreType("PKCS12");
        ssl.setKeyStorePassword(password);
        config.jetty.addConnector(
                (server, httpConfig) -> {
                    HttpConfiguration https = new HttpConfiguration(httpConfig);
                    https.addCustomizer(new SecureRequestCustomizer());
                    ServerConnector connector =
                            new ServerConnector(
                                    server,
                                    new SslConnectionFactory(ssl, "http/1.1"),
                                    new HttpConnectionFactory(https));
                    connector.setHost("0.0.0.0");
                    connector.setPort(port());
                    return connector;
                });
    }
}
