package com.jeppeman.globallydynamic.net;

import com.jeppeman.globallydynamic.globalsplitinstall.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class MultiPart {
    private static final String LINE_FEED = "\r\n";
    private final InputStream inputStream;
    private final String boundary;
    private final String mediaType;

    private MultiPart(InputStream inputStream, String boundary) {
        this.inputStream = inputStream;
        this.boundary = boundary;
        this.mediaType = "multipart/form-data; boundary=" + boundary;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public String getBoundary() {
        return boundary;
    }

    public String getMediaType() {
        return mediaType;
    }

    public static class Builder {
        private final String boundary = "===" + System.currentTimeMillis() + "===";
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        private final String charset = "UTF-8";
        private final PrintWriter writer;

        {
            try {
                writer = new PrintWriter(new OutputStreamWriter(outputStream, charset));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        public Builder setField(String name, String value) {
            writer.append("--$boundary")
                    .append(LINE_FEED)
                    .append("Content-Disposition: form-data; name=\"" + name + "\"")
                    .append(LINE_FEED)
                    .append("Content-Type: text/plain; charset=\"" + charset + "\"")
                    .append(LINE_FEED)
                    .append(LINE_FEED)
                    .append(value)
                    .append(LINE_FEED)
                    .flush();
            return this;
        }

        public Builder setFile(String name, File file) throws IOException {
            String fileName = file.getName();
            String mimeType = URLConnection.guessContentTypeFromName(fileName);
            writer.append("--$boundary")
                    .append(LINE_FEED)
                    .append("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"")
                    .append(LINE_FEED)
                    .append("Content-Type: " + mimeType)
                    .append(LINE_FEED)
                    .append("Content-Transfer-Encoding: binary")
                    .append(LINE_FEED)
                    .append(LINE_FEED)
                    .flush();

            outputStream.write(FileUtils.readAllBytes(file));
            return this;
        }

        public MultiPart build() {
            return new MultiPart(
                    new ByteArrayInputStream(outputStream.toByteArray()),
                    boundary
            );
        }
    }
}