package com.jeppeman.globallydynamic.globalsplitinstall;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import androidx.annotation.NonNull;

class SignatureProviderFactory {
    static SignatureProvider create(@NonNull Context context) {
        return new SignatureProviderImpl(context);
    }
}

interface SignatureProvider {
    String getCertificateFingerprint();
}

class SignatureProviderImpl implements SignatureProvider {
    private final Context context;

    SignatureProviderImpl(@NonNull Context context) {
        this.context = context;
    }

    private String getFingerPrintFromSignature(Signature[] signatures)
            throws NoSuchAlgorithmException {
        String hashKey = null;
            StringBuilder stringBuilder = new StringBuilder();

            for (Signature signature : signatures) {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
                messageDigest.update(signature.toByteArray());

                for (byte b : messageDigest.digest()) {
                    stringBuilder.append(String.format("%02x", b & 0xff)).append(':');
                }
                hashKey = stringBuilder.toString().toUpperCase(Locale.getDefault());
                hashKey = hashKey.substring(0, hashKey.length() - 1);
            }
        return hashKey;
    }

    @Override
    public String getCertificateFingerprint() {
        int flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                ? PackageManager.GET_SIGNING_CERTIFICATES
                : PackageManager.GET_SIGNATURES;

        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), flag);

            Signature[] signature;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (packageInfo.signingInfo.hasMultipleSigners()) {
                    signature = packageInfo.signingInfo.getApkContentsSigners();
                } else {
                    signature = packageInfo.signingInfo.getSigningCertificateHistory();
                }
            } else {
                signature = packageInfo.signatures;
            }

            return getFingerPrintFromSignature(signature);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }
}
