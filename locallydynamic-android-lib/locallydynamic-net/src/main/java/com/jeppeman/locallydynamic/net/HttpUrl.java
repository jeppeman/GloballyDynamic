package com.jeppeman.locallydynamic.net;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpUrl {
    private static final Pattern PORT_REGEX = Pattern.compile(":[0-9]+");
    private static final Pattern QUERY_PARAM_REGEX = Pattern.compile("[^=]+=[^=]+");
    private final String scheme;
    private final String host;
    private final int port;
    private final List<String> pathSegments;
    private final Map<String, String> queryParams;

    private HttpUrl(
            String scheme,
            String host,
            int port,
            List<String> pathSegments,
            Map<String, String> queryParams) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.pathSegments = pathSegments;
        this.queryParams = queryParams;
    }

    private String queryString() {
        StringBuilder stringBuilder = new StringBuilder(
                queryParams.isEmpty()
                        ? ""
                        : "?");

        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            stringBuilder.append(StringUtils.urlEncode(entry.getKey()));
            stringBuilder.append("=");
            stringBuilder.append(StringUtils.urlEncode(entry.getValue()));
            stringBuilder.append("&");
        }

        return stringBuilder.length() > 0
                ? stringBuilder.substring(0, stringBuilder.length() - 1)
                : stringBuilder.toString();
    }

    private String pathString() {
        StringBuilder stringBuilder = new StringBuilder("/");

        for (String segment : pathSegments) {
            stringBuilder.append(segment);
            stringBuilder.append("/");
        }

        return stringBuilder.length() > 1
                ? stringBuilder.substring(0, stringBuilder.length() - 1)
                : stringBuilder.toString();
    }

    private String portString() {
        return port == 80
                ? ""
                : ":" + port;
    }

    public String url() {
        return scheme + "://" + host + portString() + pathString() + queryString();
    }

    public String scheme() {
        return scheme;
    }

    public int port() {
        return port;
    }

    public String host() {
        return host;
    }

    public List<String> pathSegments() {
        return pathSegments;
    }

    public Map<String, String> queryParams() {
        return queryParams;
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    public static class Builder {
        private final List<String> pathSegments = new LinkedList<String>();
        private final Map<String, String> queryParams = new LinkedHashMap<String, String>();
        private String scheme;
        private Integer port;
        private String host;

        Builder() {

        }

        Builder(HttpUrl httpUrl) {
            scheme = httpUrl.scheme;
            port = httpUrl.port;
            host = httpUrl.host;
            queryParams.putAll(httpUrl.queryParams);
            pathSegments.addAll(httpUrl.pathSegments);
        }

        private Integer parsePortFromHost(String host) {
            Matcher matcher = PORT_REGEX.matcher(host);
            Integer port = null;
            while (matcher.find()) {
                port = Integer.valueOf(host.substring(matcher.start() + 1, matcher.end()));
            }

            return port;
        }

        Builder parse(String url) {
            String trimmedUrl = url.trim();

            if (trimmedUrl.regionMatches(true, 0, "http:", 0, 5)) {
                scheme = "http";
            } else if (trimmedUrl.regionMatches(true, 0, "https:", 0, 6)) {
                scheme = "https";
            } else {
                throw new IllegalArgumentException("Expected url to contain http: or https:");
            }

            String current = trimmedUrl.substring(scheme.length() + 1);

            if (!current.regionMatches(true, 0, "//", 0, 2)) {
                throw new IllegalArgumentException("Expected // after scheme");
            }

            current = current.substring(2);

            if (current.startsWith("/")) {
                throw new IllegalArgumentException("Unexpected / encountered");
            }

            int firstIndexOfSlash = current.indexOf('/');
            int actualIndex = firstIndexOfSlash == -1
                    ? current.length()
                    : firstIndexOfSlash;

            host = current.substring(0, actualIndex);

            port = parsePortFromHost(host);

            if (host != null && port != null) {
                Matcher portMatcher = PORT_REGEX.matcher(host);
                while (portMatcher.find()) {
                    host = host.substring(0, portMatcher.start());
                }
            }

            current = current.substring(actualIndex);

            String[] splitAtQuestionMark = current.split("\\?");
            if (splitAtQuestionMark.length > 1) {
                current = splitAtQuestionMark[0];
                String[] queryParams = splitAtQuestionMark[1].split("&");
                for (String queryParam : queryParams) {
                    if (!QUERY_PARAM_REGEX.matcher(queryParam).matches()) {
                        throw new IllegalArgumentException("Invalid query parameter $queryParam");
                    }

                    String[] nameValuePair = queryParam.split("=");

                    queryParam(nameValuePair[0], nameValuePair[1]);
                }
            }

            String[] nonTrimmedSegments = current.split("/");
            List<String> segments = new LinkedList<String>();
            for (String segment : nonTrimmedSegments) {
                String trimmed = segment.replaceAll("//", "/");
                if (!segment.isEmpty()) {
                    segments.add(trimmed);
                }
            }

            pathSegments.addAll(segments);

            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder scheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        public Builder pathSegments(String... pathSegments) {
            this.pathSegments.addAll(Arrays.asList(pathSegments));
            return this;
        }

        public Builder queryParam(String name, String value) {
            this.queryParams.put(name, value);
            return this;
        }

        public HttpUrl build() {
            String scheme = this.scheme;
            String host = this.host;
            int port = this.port != null ? this.port : 80;

            return new HttpUrl(
                    scheme,
                    host,
                    port,
                    pathSegments,
                    queryParams
            );
        }
    }

    public static HttpUrl parse(String url) {
        return new Builder().parse(url).build();
    }

    public static Builder builder() {
        return new Builder();
    }
}