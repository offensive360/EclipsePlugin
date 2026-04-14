package com.o360.sastplugin.util;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class SslHelper {

    /**
     * Creates an SSLContext that trusts all certificates (for self-signed cert support).
     */
    public static SSLContext createTrustAllContext() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            return sslContext;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create trust-all SSL context", e);
        }
    }

    /**
     * Creates a HostnameVerifier that accepts all hostnames.
     */
    public static HostnameVerifier createTrustAllHostnameVerifier() {
        return (hostname, session) -> true;
    }
}
