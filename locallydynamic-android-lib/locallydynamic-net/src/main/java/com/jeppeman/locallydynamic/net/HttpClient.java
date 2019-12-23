package com.jeppeman.locallydynamic.net;

import com.jeppeman.locallydynamic.serialization.JsonDeserializer;
import com.jeppeman.locallydynamic.serialization.JsonDeserializerFactory;
import com.jeppeman.locallydynamic.serialization.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

public interface HttpClient {
    List<Interceptor> getInterceptors();

    long getReadTimeout();

    long getConnectTimeout();

    SSLSocketFactory getSslSocketFactory();

    X509TrustManager getTrustManager();

    Builder newBuilder();

    <T> Response<T> executeRequest(Request request, Class<T> responseBodyType);

    <T> void executeRequestAsync(
            Request request,
            Class<T> responseBodyType,
            Callbacks<T> callbacks
    );

    Response<String> executeRequest(Request request);

    void executeRequestAsync(
            Request request,
            Callbacks<String> callbacks
    );

    interface Callbacks<T> {
        void onResponse(Response<T> response);

        void onFailure(Throwable throwable);
    }

    interface Builder {
        Builder addInterceptor(Interceptor interceptor);

        Builder setReadTimeout(long readTimeout);

        Builder setConnectTimeout(long connectTimeout);

        Builder setSslSocketFactory(
                SSLSocketFactory sslSocketFactory,
                X509TrustManager trustManager
        );

        Builder setLogger(Logger logger);

        HttpClient build();
    }
}

class HttpClientImpl implements HttpClient {
    private final JsonDeserializer jsonDeserializer;
    private final Executor executor;
    private final long connectTimeout;
    private final long readTimeout;
    private final List<Interceptor> interceptors;
    private final SSLSocketFactory sslSocketFactory;
    private final X509TrustManager trustManager;
    private final Logger logger;

    HttpClientImpl(
            long connectTimeout,
            long readTimeout,
            List<Interceptor> interceptors,
            SSLSocketFactory sslSocketFactory,
            X509TrustManager trustManager,
            Logger logger) {
        this(connectTimeout, readTimeout, interceptors,
                Executors.newFixedThreadPool(2), JsonDeserializerFactory.create(), sslSocketFactory,
                trustManager, logger);
    }

    HttpClientImpl(
            long connectTimeout,
            long readTimeout,
            List<Interceptor> interceptors,
            Executor executor,
            JsonDeserializer jsonDeserializer,
            SSLSocketFactory sslSocketFactory,
            X509TrustManager trustManager,
            Logger logger) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.interceptors = interceptors;
        this.jsonDeserializer = jsonDeserializer;
        this.executor = executor;
        this.sslSocketFactory = sslSocketFactory;
        this.trustManager = trustManager;
        this.logger = logger;
    }

    HttpURLConnection getConnectionFromRequest(Request request) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(request.getUrl().url()).openConnection();
            if (connection instanceof HttpsURLConnection
                    && sslSocketFactory != null
                    && trustManager != null) {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new X509TrustManager[]{trustManager}, new SecureRandom());
                ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
            }
            return connection;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    String readInputStream(InputStream inputStream) {
        try {
            StringBuilder buffer = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = bufferedReader.readLine();
            while (line != null) {
                buffer.append(line);
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
            return buffer.toString();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public List<Interceptor> getInterceptors() {
        return interceptors;
    }

    @Override
    public long getReadTimeout() {
        return readTimeout;
    }

    @Override
    public long getConnectTimeout() {
        return connectTimeout;
    }

    @Override
    public SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }

    @Override
    public X509TrustManager getTrustManager() {
        return trustManager;
    }

    @Override
    public HttpClient.Builder newBuilder() {
        return new BuilderImpl(this);
    }

    @Override
    public Response<String> executeRequest(Request request) {
        try {
            Request interceptedRequest = request;
            for (Interceptor interceptor : interceptors) {
                interceptedRequest = interceptor.intercept(interceptedRequest);
            }

            logger.i("--> " + request.getMethod() + " " + request.getUrl().url());

            for (Map.Entry<String, List<String>> header : request.getHeaders().entrySet()) {
                logger.i(header.getKey() + ": " + ListUtils.toString(header.getValue(), false));
            }

            if ("https".equals(interceptedRequest.getUrl().scheme())
                    && sslSocketFactory != null
                    && trustManager != null) {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new X509TrustManager[]{trustManager}, new SecureRandom());
            }

            HttpURLConnection connection = getConnectionFromRequest(interceptedRequest);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod(interceptedRequest.getMethod().getValue());
            connection.setConnectTimeout((int) connectTimeout);
            connection.setReadTimeout((int) readTimeout);
            connection.setUseCaches(true);
            for (Map.Entry<String, List<String>> entry : interceptedRequest.getHeaders().entrySet()) {
                connection.setRequestProperty(
                        entry.getKey(),
                        StringUtils.joinToString(entry.getValue(), ",")
                );
            }

            if (interceptedRequest.getBody() != null) {
                byte[] bytes = StreamUtils.readAllBytes(interceptedRequest.getBody());
                logger.i("Body: " + new String(bytes, Charset.forName("UTF-8")));
                connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
                connection.getOutputStream().write(bytes);
            }

            int responseCode = connection.getResponseCode();

            boolean isSuccessful = responseCode >= 200 && responseCode <= 299;

            String body = isSuccessful
                    ? readInputStream(connection.getInputStream())
                    : null;

            String errorBody = !isSuccessful
                    ? readInputStream(connection.getErrorStream())
                    : null;

            logger.i("<-- " + responseCode + " " + request.getUrl().url());

            Headers headers = new Headers();
            headers.putAll(connection.getHeaderFields());

            for (Map.Entry<String, List<String>> header : headers.entrySet()) {
                logger.i(header.getKey() + ": " + ListUtils.toString(header.getValue(), false));
            }

            if (body != null) {
                logger.i("Body: " + body);
            }

            if (errorBody != null) {
                logger.i("Error body: " + errorBody);
            }

            return Response.<String>builder()
                    .setCode(responseCode)
                    .setBody(body)
                    .setErrorBody(errorBody)
                    .setHeaders(headers)
                    .setRequest(request)
                    .build();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public <T> Response<T> executeRequest(
            Request request,
            Class<T> responseBodyType) {
        Response<String> response = executeRequest(request);
        return Response.<T>builder()
                .setCode(response.getCode())
                .setHeaders(response.getHeaders())
                .setBody(response.getBody() != null
                        ? jsonDeserializer.deserialize(response.getBody(), responseBodyType)
                        : null)
                .setErrorBody(response.getErrorBody())
                .setRequest(request)
                .build();
    }

    @Override
    public void executeRequestAsync(
            final Request request,
            final Callbacks<String> callbacks) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Response<String> response = executeRequest(request);
                    callbacks.onResponse(response);
                } catch (Throwable throwable) {
                    callbacks.onFailure(throwable);
                }
            }
        });
    }

    @Override
    public <T> void executeRequestAsync(
            final Request request,
            final Class<T> responseBodyType,
            final Callbacks<T> callbacks) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Response<T> response = executeRequest(request, responseBodyType);
                    callbacks.onResponse(response);
                } catch (Throwable throwable) {
                    callbacks.onFailure(throwable);
                }
            }
        });
    }

    static class BuilderImpl implements HttpClient.Builder {
        private final List<Interceptor> interceptors = new LinkedList<Interceptor>();
        private long connectTimeout = 15000;
        private long readTimeout = 15000;
        private SSLSocketFactory sslSocketFactory;
        private X509TrustManager trustManager;
        private Logger logger = LoggerFactory.create();

        BuilderImpl() {

        }

        BuilderImpl(HttpClient httpClient) {
            connectTimeout = httpClient.getConnectTimeout();
            readTimeout = httpClient.getReadTimeout();
            interceptors.addAll(httpClient.getInterceptors());
            sslSocketFactory = httpClient.getSslSocketFactory();
            trustManager = httpClient.getTrustManager();
        }

        @Override
        public Builder addInterceptor(Interceptor interceptor) {
            interceptors.add(interceptor);
            return this;
        }

        @Override
        public Builder setReadTimeout(long readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        @Override
        public Builder setConnectTimeout(long connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        @Override
        public Builder setSslSocketFactory(SSLSocketFactory sslSocketFactory, X509TrustManager trustManager) {
            this.sslSocketFactory = sslSocketFactory;
            this.trustManager = trustManager;
            return this;
        }

        @Override
        public Builder setLogger(Logger logger) {
            this.logger = logger;
            return this;
        }

        @Override
        public HttpClient build() {
            return new HttpClientImpl(
                    connectTimeout,
                    readTimeout,
                    interceptors,
                    sslSocketFactory,
                    trustManager,
                    logger
            );
        }
    }
}