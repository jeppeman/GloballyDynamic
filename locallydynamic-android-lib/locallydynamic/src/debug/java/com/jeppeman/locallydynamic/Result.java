package com.jeppeman.locallydynamic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@SuppressWarnings("unchecked")
abstract class Result<T> {
    interface ResultCallback {
        void onResult();
    }

    interface Mapper<T, TTo> {
        TTo map(T from);
    }

    interface Action<T> {
        T run();
    }

    static class Success<T> extends Result<T> {
        final T data;

        Success(T data) {
            this.data = data;
        }

        interface Callback<T> {
            void success(T data);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof Success
                    && Objects.equals(((Success) obj).data, data);
        }
    }

    static class Failure<T> extends Result<T> {
        @Nullable
        final String message;
        @Nullable
        final Exception exception;

        Failure() {
            this(null, null);
        }

        Failure(@NonNull String message) {
            this(message, null);
        }

        Failure(@NonNull Exception exception) {
            this(null, exception);
        }

        Failure(@Nullable String message, @Nullable Exception exception) {
            this.message = message;
            this.exception = exception;
        }

        interface Callback {
            void failure(@Nullable Exception exception);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof Failure
                    && Objects.equals(((Failure) obj).exception, exception)
                    && Objects.equals(((Failure) obj).message, message);
        }
    }

    Result<T> doOnFailure(@NonNull Failure.Callback callback) {
        try {
            if (this instanceof Success) {
                return this;
            } else {
                callback.failure(((Failure<T>) this).exception);
                return this;
            }
        } catch (Exception exception) {
            return new Failure<T>(exception);
        }
    }

    Result<T> doOnResult(@NonNull ResultCallback resultCallback) {
        try {
            resultCallback.onResult();
        } catch (Exception exception) {
            return new Failure<T>(exception);
        }
        return this;
    }

    Result<T> doOnSuccess(@NonNull Success.Callback<T> callback) {
        try {
            if (this instanceof Success) {
                callback.success(((Success<T>) this).data);
                return this;
            } else {
                return this;
            }
        } catch (Exception exception) {
            return new Failure<T>(exception);
        }
    }

    <TTo> Result<TTo> flatMap(@NonNull Mapper<T, Result<TTo>> mapper) {
        try {
            if (this instanceof Success) {
                return mapper.map(((Success<T>) this).data);
            } else {
                return (Result<TTo>) this;
            }
        } catch (Exception exception) {
            return new Failure<TTo>(exception);
        }
    }

    <TTo> Result<TTo> map(@NonNull Mapper<T, TTo> mapper) {
        try {
            if (this instanceof Success) {
                return new Result.Success(mapper.map(((Success<T>) this).data));
            } else {
                return (Result<TTo>) this;
            }
        } catch (Exception exception) {
            return new Failure<TTo>(exception);
        }
    }


    static <R> Result<R> from(@NonNull Action<R> action) {
        try {
            return new Success(action.run());
        } catch (Exception exception) {
            return new Failure<R>(exception);
        }
    }

    static <R> Result<R> of(R value) {
        return new Result.Success<R>(value);
    }

    static Result of(Exception exception) {
        return new Result.Failure(exception);
    }
}