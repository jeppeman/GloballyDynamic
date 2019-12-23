package com.jeppeman.locallydynamic.net;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

public class FileUtils {
    public static byte[] readAllBytes(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            int offset = 0;
            long fileSize = file.length();
            if (fileSize > Integer.MAX_VALUE) {
                throw new OutOfMemoryError("File $this is too big ($length bytes) to fit in memory.");
            }
            int remaining = (int) fileSize;
            byte[] result = new byte[remaining];
            while (remaining > 0) {
                int read = fileInputStream.read(result, offset, remaining);
                if (read < 0) {
                    break;
                }
                remaining -= read;
                offset += read;
            }
            fileInputStream.close();
            return remaining == 0
                    ? result
                    : Arrays.copyOf(result, offset);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
