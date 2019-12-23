package com.jeppeman.globallydynamic.net;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class StreamUtils {
    public static byte[] readAllBytes(InputStream inputStream) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.max(8 * 1024, inputStream.available()));
            byte[] buffer = new byte[8 * 1024];
            int bytes = inputStream.read(buffer);
            while (bytes >= 0) {
                baos.write(buffer, 0, bytes);
                bytes = inputStream.read(buffer);
            }
            return baos.toByteArray();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public static String readString(InputStream inputStream) {
        return new String(readAllBytes(inputStream));
    }
}
