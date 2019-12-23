package com.jeppeman.locallydynamic.net;

import com.google.common.collect.Lists;
import com.jeppeman.locallydynamic.serialization.JsonDeserializerFactory;
import com.jeppeman.locallydynamic.serialization.StringUtils;
import com.jeppeman.locallydynamic.serialization.annotations.JsonDeserialize;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.net.ssl.HttpsURLConnection;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class HttpClientImplTest {
    private static final String RESPONSE_BODY = "{a: 10}";
    private static final String ERROR_BODY = "errorBody";
    private static final int CONNECT_TIMEOUT = 1;
    private static final int READ_TIMEOUT = 1;
    @Mock
    private Executor mockExecutor;
    @Mock
    private InputStream mockErrorStream;
    @Mock
    private InputStream mockInputStream;
    @Mock
    private OutputStream mockOutputStream;
    @Mock
    private HttpsURLConnection mockHttpURLConnection;
    @Mock
    private Interceptor mockInterceptor;
    @Mock
    private Logger mockLogger;
    private HttpClientImpl httpClientImpl;

    private Request request = Request.builder()
            .url("http://test.se")
            .setBody("requestBody")
            .addHeader("x", "y")
            .addHeader("x", "z")
            .addHeader("a", "b")
            .build();

    @Before
    public void setUp() throws IOException {
        httpClientImpl = spy(
                new HttpClientImpl(
                        (long) CONNECT_TIMEOUT,
                        (long) READ_TIMEOUT,
                        Lists.newArrayList(mockInterceptor),
                        mockExecutor,
                        JsonDeserializerFactory.create(),
                        null,
                        null,
                        mockLogger
                )
        );

        doReturn(mockHttpURLConnection).when(httpClientImpl).getConnectionFromRequest(request);
        when(mockHttpURLConnection.getOutputStream()).thenReturn(mockOutputStream);
        when(mockHttpURLConnection.getInputStream()).thenReturn(mockInputStream);
        when(mockHttpURLConnection.getErrorStream()).thenReturn(mockErrorStream);
        when(mockInterceptor.intercept(ArgumentCaptor.<Request, Request>forClass(Request.class).capture())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return invocation.getArguments()[0];
            }
        });
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ((Runnable) invocation.getArgument(0)).run();
                return null;
            }
        }).when(mockExecutor).execute(ArgumentCaptor.<Runnable, Runnable>forClass(Runnable.class).capture());
        doReturn(RESPONSE_BODY).when(httpClientImpl).readInputStream(mockInputStream);
        doReturn(ERROR_BODY).when(httpClientImpl).readInputStream(mockErrorStream);
    }

    @Test
    public void executeRequest_shouldSetConnectionAndReadTimeoutFromClient() {
        httpClientImpl.executeRequest(request);

        verify(mockHttpURLConnection).setConnectTimeout(CONNECT_TIMEOUT);
        verify(mockHttpURLConnection).setReadTimeout(READ_TIMEOUT);
    }

    @Test
    public void whenInterceptorsArePresent_executeRequest_shouldCallInterceptors() {
        httpClientImpl.executeRequest(request);

        verify(mockInterceptor).intercept(request);
    }

    @Test
    public void whenHeadersArePresent_executeRequest_shouldAddHeadersFromRequest() {
        httpClientImpl.executeRequest(request);

        for (Map.Entry<String, List<String>> entry : request.getHeaders().entrySet()) {
            verify(mockHttpURLConnection).setRequestProperty(entry.getKey(), StringUtils.joinToString(entry.getValue(), ","));
        }
    }

    @Test
    public void whenBodyIsPresent_executeRequest_shouldSetContentLengthAndAddBodyToRequest() throws IOException {
        httpClientImpl.executeRequest(request);

        verify(mockHttpURLConnection).setRequestProperty("Content-Length", String.valueOf("requestBody".length()));
        verify(mockOutputStream).write("requestBody".getBytes("UTF-8"));
    }

    @Test
    public void whenRequestFails_executeRequest_shouldPopulateErrorBody() throws IOException {
        when(mockHttpURLConnection.getResponseCode()).thenReturn(500);

        Response<?> response = httpClientImpl.executeRequest(request);

        assertThat(response.isSuccessful()).isFalse();
        assertThat(response.getBody()).isNull();
        assertThat(response.getErrorBody()).isEqualTo(ERROR_BODY);
    }

    @Test
    public void whenRequestSucceeds_executeRequest_shouldPopulateBody() throws IOException {
        when(mockHttpURLConnection.getResponseCode()).thenReturn(200);

        Response<?> response = httpClientImpl.executeRequest(request);

        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.getErrorBody()).isNull();
        assertThat(response.getBody()).isEqualTo(RESPONSE_BODY);
    }

    @Test
    public void whenCalledWithTypeArgument_executeRequest_shouldDeserializeBody() throws IOException {
        when(mockHttpURLConnection.getResponseCode()).thenReturn(200);

        Response<?> response = httpClientImpl.executeRequest(request, TestClass.class);

        assertThat(response.getBody()).isEqualTo(new TestClass(10));
    }

    @Test
    public void executeRequest_shouldPopulateResponseHeadersFromConnection() {
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        map.put("x", Lists.newArrayList("y", "z"));
        when(mockHttpURLConnection.getHeaderFields()).thenReturn(map);

        Response<?> response = httpClientImpl.executeRequest(request);

        assertThat(response.getHeaders().get("x")).isEqualTo(Lists.newArrayList("y", "z"));
    }

    @Test
    public void whenSuccessful_executeAsync_shouldCallOnResponseCallback() throws IOException {
        when(mockHttpURLConnection.getResponseCode()).thenReturn(200);

        final boolean[] calledOnResponse = new boolean[]{false};

        httpClientImpl.executeRequestAsync(request, new HttpClient.Callbacks<String>() {
            @Override
            public void onResponse(Response<String> response) {
                calledOnResponse[0] = true;
            }

            @Override
            public void onFailure(Throwable throwable) {

            }
        });

        assertThat(calledOnResponse[0]).isTrue();
    }

    @Test
    public void whenFailing_executeAsync_shouldCallOnFailureCallback() throws IOException {
        when(mockHttpURLConnection.getResponseCode()).thenThrow(new RuntimeException());

        final boolean[] calledFailure = new boolean[]{false};

        httpClientImpl.executeRequestAsync(request, new HttpClient.Callbacks<String>() {
            @Override
            public void onResponse(Response<String> response) {

            }

            @Override
            public void onFailure(Throwable throwable) {
                calledFailure[0] = true;
            }
        });

        assertThat(calledFailure[0]).isTrue();
    }

    static class TestClass {
        int a;

        TestClass(@JsonDeserialize("a") int a) {
            this.a = a;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof TestClass && ((TestClass) o).a == a;
        }
    }

}