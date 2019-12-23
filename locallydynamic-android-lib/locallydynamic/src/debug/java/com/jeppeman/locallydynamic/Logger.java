package com.jeppeman.locallydynamic;

import android.util.Log;

import androidx.annotation.NonNull;

class LoggerFactory {
    static Logger create() {
        return new LoggerImpl();
    }

    static com.jeppeman.locallydynamic.net.Logger createHttpLogger(@NonNull final Logger logger) {
        return new com.jeppeman.locallydynamic.net.Logger() {
            @Override
            public void d(String message) {
                logger.d(message);
            }

            @Override
            public void i(String message) {
                logger.i(message);
            }

            @Override
            public void e(String message) {
                logger.e(message);
            }

            @Override
            public void e(Exception exception) {
                logger.e(exception);
            }

            @Override
            public void e(String message, Exception exception) {
                logger.e(message, exception);
            }

            @Override
            public void v(String message) {
                logger.v(message);
            }
        };
    }
}

interface Logger {
    void d(String message);

    void i(String message);

    void e(String message);

    void e(Exception exception);

    void e(String message, Exception exception);

    void v(String message);
}

class LoggerImpl implements Logger {
    private static final String TAG = "LocallyDynamic";

    @Override
    public void d(String message) {
        Log.d(TAG, message);
    }

    @Override
    public void i(String message) {
        Log.i(TAG, message);
    }

    @Override
    public void e(String message) {
        Log.e(TAG, message);
    }

    @Override
    public void e(Exception exception) {
        Log.e(TAG, "", exception);
    }

    @Override
    public void e(String message, Exception exception) {
        Log.e(TAG, message, exception);
    }

    @Override
    public void v(String message) {
        Log.v(TAG, message);
    }
}