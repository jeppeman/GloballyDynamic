package com.jeppeman.globallydynamic.globalsplitinstall;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils {
    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: ${zipEntry.name}");
        }

        if (zipEntry.isDirectory()) {
            destFile.mkdirs();
        } else {
            createNewFileAndParentDirectory(destFile);
        }

        return destFile;
    }

    private static void createNewFileAndParentDirectory(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        if (file.exists()) {
            file.delete();
        }

        file.createNewFile();
    }

    static List<File> unzip(File file, String destinationDir) {
        try {
            List<File> ret = new LinkedList<File>();
            byte[] buffer = new byte[1024];
            File destinationDirFile = new File(destinationDir);
            ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(destinationDirFile, zipEntry);
                FileOutputStream fos = new FileOutputStream(newFile);
                int len = zis.read(buffer);
                while (len > 0) {
                    fos.write(buffer, 0, len);
                    len = zis.read(buffer);
                }

                zipEntry = zis.getNextEntry();
                ret.add(newFile);
            }
            zis.closeEntry();

            return ret;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

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
