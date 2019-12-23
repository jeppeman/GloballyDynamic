package com.jeppeman.globallydynamic.globalsplitinstall;

import androidx.annotation.Nullable;

class HttpException extends RuntimeException {
    final int code;
    @Nullable
    final String message;

    HttpException(int code, @Nullable String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}