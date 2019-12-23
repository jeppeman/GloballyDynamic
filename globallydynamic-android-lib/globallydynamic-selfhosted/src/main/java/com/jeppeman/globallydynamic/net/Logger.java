package com.jeppeman.globallydynamic.net;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface Logger {
    void d(String message);

    void i(String message);

    void e(String message);

    void e(Exception exception);

    void e(String message, Exception exception);

    void v(String message);
}

class LoggerFactory {
    static Logger create() {
        return new Logger() {
            @Override
            public void d(String message) {

            }

            @Override
            public void i(String message) {

            }

            @Override
            public void e(String message) {

            }

            @Override
            public void e(Exception exception) {

            }

            @Override
            public void e(String message, Exception exception) {

            }

            @Override
            public void v(String message) {

            }
        };
    }
}
