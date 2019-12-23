package com.jeppeman.globallydynamic.net;

import android.os.Handler;
import android.os.SystemClock;

import com.jeppeman.globallydynamic.serialization.JsonDeserializer;
import com.jeppeman.globallydynamic.serialization.JsonDeserializerFactory;
import com.jeppeman.globallydynamic.serialization.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
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

    int downloadFile(
            Request request,
            File downloadLocation,
            DownloadCallbacks<String> callbacks
    );

    void cancelDownload(int downloadId);

    Response<String> executeRequest(Request request);

    int getNextDownloadId();

    void executeRequestAsync(
            Request request,
            Callbacks<String> callbacks
    );

    interface Callbacks<T> {
        void onResponse(Response<T> response);

        void onFailure(Throwable throwable);
    }

    interface DownloadCallbacks<T> {
        Handler getCallbackHandler();

        void onResponse(Response<T> response, long bytesDownloaded, long totalBytesToDownload);

        void onStartDownload(long totalBytes);

        void onProgress(long bytesDownloaded, long totalBytesToDownload);

        void onFailure(Throwable throwable);

        void onCanceled();
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

class DownloadCallbacksWrapper<T> implements HttpClient.DownloadCallbacks<T> {

    private final HttpClient.DownloadCallbacks<T> delegate;

    DownloadCallbacksWrapper(HttpClient.DownloadCallbacks<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Handler getCallbackHandler() {
        return delegate.getCallbackHandler();
    }

    @Override
    public void onResponse(final Response<T> response, final long bytesDownloaded, final long totalBytesToDownload) {
        getCallbackHandler().post(new Runnable() {
            @Override
            public void run() {
                delegate.onResponse(response, bytesDownloaded, totalBytesToDownload);
            }
        });
    }

    @Override
    public void onStartDownload(final long totalBytes) {
        getCallbackHandler().post(new Runnable() {
            @Override
            public void run() {
                delegate.onStartDownload(totalBytes);
            }
        });
    }

    @Override
    public void onProgress(final long bytesDownloaded, final long totalBytesToDownload) {
        getCallbackHandler().post(new Runnable() {
            @Override
            public void run() {
                delegate.onProgress(bytesDownloaded, totalBytesToDownload);
            }
        });
    }

    @Override
    public void onFailure(final Throwable throwable) {
        getCallbackHandler().post(new Runnable() {
            @Override
            public void run() {
                delegate.onFailure(throwable);
            }
        });
    }

    @Override
    public void onCanceled() {
        getCallbackHandler().post(new Runnable() {
            @Override
            public void run() {
                delegate.onCanceled();
            }
        });
    }
}

class HttpClientImpl implements HttpClient {
    private static final Executor executor = new ThreadPoolExecutor(
            Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
            Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>()
    );
    private final JsonDeserializer jsonDeserializer;
    private final long connectTimeout;
    private final long readTimeout;
    private final List<Interceptor> interceptors;
    private final SSLSocketFactory sslSocketFactory;
    private final X509TrustManager trustManager;
    private final Logger logger;
    private final AtomicInteger downloadCounter = new AtomicInteger();
    private final Map<Integer, HttpURLConnection> downloads
            = Collections.synchronizedMap(new HashMap<Integer, HttpURLConnection>());

    private HttpClientImpl(
            long connectTimeout,
            long readTimeout,
            List<Interceptor> interceptors,
            SSLSocketFactory sslSocketFactory,
            X509TrustManager trustManager,
            Logger logger) {
        this(connectTimeout, readTimeout, interceptors,
                JsonDeserializerFactory.create(),
                sslSocketFactory,
                trustManager, logger);
    }

    HttpClientImpl(
            long connectTimeout,
            long readTimeout,
            List<Interceptor> interceptors,
            JsonDeserializer jsonDeserializer,
            SSLSocketFactory sslSocketFactory,
            X509TrustManager trustManager,
            Logger logger) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.interceptors = interceptors;
        this.jsonDeserializer = jsonDeserializer;
        this.sslSocketFactory = sslSocketFactory;
        this.trustManager = trustManager;
        this.logger = logger;
    }

    HttpURLConnection openConnection(Request request) throws
            KeyManagementException,
            NoSuchAlgorithmException,
            IOException {
        if ("https".equals(request.getUrl().scheme())
                && sslSocketFactory != null
                && trustManager != null) {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new X509TrustManager[]{trustManager}, new SecureRandom());
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(request.getUrl().url()).openConnection();
        if (connection instanceof HttpsURLConnection
                && sslSocketFactory != null
                && trustManager != null) {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new X509TrustManager[]{trustManager}, new SecureRandom());
            ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
        }

        return connection;
    }

    HttpURLConnection getConnectionFromRequest(Request request) {
        try {
            Request interceptedRequest = request;
            for (Interceptor interceptor : interceptors) {
                interceptedRequest = interceptor.intercept(interceptedRequest);
            }

            logger.i("--> " + interceptedRequest.getMethod() + " " + interceptedRequest.getUrl().url());

            for (Map.Entry<String, List<String>> header : interceptedRequest.getHeaders().entrySet()) {
                logger.i(header.getKey() + ": " + ListUtils.toString(header.getValue(), false));
            }
            HttpURLConnection connection = openConnection(request);
            connection.setDoInput(true);
            connection.setDoOutput(interceptedRequest.getMethod() == HttpMethod.POST
                    || interceptedRequest.getMethod() == HttpMethod.PUT);
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
    public int getNextDownloadId() {
        return downloadCounter.incrementAndGet();
    }

    @Override
    public Response<String> executeRequest(Request request) {
        try {
            HttpURLConnection connection = getConnectionFromRequest(request);
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
    public int downloadFile(final Request request, final File downloadLocation, DownloadCallbacks<String> callbacks) {
        final DownloadCallbacksWrapper<String> callbacksWrapper = new DownloadCallbacksWrapper<String>(callbacks);
        final int id = downloadCounter.incrementAndGet();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection connection = getConnectionFromRequest(request);
                    final int responseCode = connection.getResponseCode();
                    boolean isSuccessful = responseCode >= 200 && responseCode <= 299;
                    logger.i("<-- " + responseCode + " " + request.getUrl().url());
                    final Headers headers = new Headers();
                    headers.putAll(connection.getHeaderFields());
                    for (Map.Entry<String, List<String>> header : headers.entrySet()) {
                        logger.i(header.getKey() + ": " + ListUtils.toString(header.getValue(), false));
                    }
                    if (!isSuccessful) {
                        final String errorBody = readInputStream(connection.getErrorStream());

                        if (errorBody != null) {
                            logger.i("Error body: " + errorBody);
                        }
                        callbacksWrapper.onResponse(Response.<String>builder()
                                .setCode(responseCode)
                                .setErrorBody(errorBody)
                                .setHeaders(headers)
                                .setRequest(request)
                                .build(), 0, 0);
                    } else {
                        InputStream requestInputStream = connection.getInputStream();
                        FileOutputStream downloadedFileOutputStream = new FileOutputStream(downloadLocation);
                        long contentLength = Long.parseLong(connection.getHeaderField("Content-Length"));
                        downloads.put(id, connection);
                        callbacksWrapper.onStartDownload(contentLength);
                        callbacksWrapper.onProgress(0, contentLength);
                        byte[] buffer = new byte[65536];
                        int totalRead = 0;
                        long lastProgressTick = SystemClock.elapsedRealtime();
                        boolean downloadCanceled = false;
                        int numBytesRead = requestInputStream.read(buffer);
                        while (!downloadCanceled && numBytesRead != -1) {
                                totalRead += numBytesRead;
                                long now = SystemClock.elapsedRealtime();
                                long elapsedSinceLastProgressUpdate = now - lastProgressTick;
                                if (elapsedSinceLastProgressUpdate > 1000) {
                                    callbacksWrapper.onProgress(totalRead, contentLength);
                                    lastProgressTick = now;
                                }
                                downloadedFileOutputStream.write(buffer, 0, numBytesRead);
                            synchronized (downloads) {
                                downloadCanceled = !downloads.containsKey(id);
                                if (!downloadCanceled) {
                                    numBytesRead = requestInputStream.read(buffer);
                                } else {
                                    callbacksWrapper.onCanceled();
                                }
                            }
                        }

                        downloadedFileOutputStream.close();
                        requestInputStream.close();

                        if (!downloadCanceled) {
                            callbacksWrapper.onResponse(Response.<String>builder()
                                    .setCode(responseCode)
                                    .setHeaders(headers)
                                    .setRequest(request)
                                    .build(), totalRead, contentLength);

                            downloads.remove(id);
                        }
                    }
                } catch (Exception exception) {
                    downloads.remove(id);
                    callbacksWrapper.onFailure(exception);
                }
            }
        });

        return id;
    }

    @Override
    public void cancelDownload(final int downloadId) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (downloads) {
                    if (downloads.containsKey(downloadId)) {
                        HttpURLConnection connection = downloads.get(downloadId);
                        downloads.remove(downloadId);
                        connection.disconnect();
                    }
                }
            }
        });
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