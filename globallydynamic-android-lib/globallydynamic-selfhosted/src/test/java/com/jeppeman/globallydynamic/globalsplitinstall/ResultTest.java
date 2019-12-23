package com.jeppeman.globallydynamic.globalsplitinstall;

import org.junit.Test;

import androidx.annotation.Nullable;

import static com.google.common.truth.Truth.assertThat;

public class ResultTest {
    @Test
    public void whenFailing_doOnFailure_shouldInvokeCallback() {
        Result failure = new Result.Failure();
        final boolean[] calledDoOnFailure = new boolean[]{false};

        Result result = failure.doOnFailure(new Result.Failure.Callback() {
            @Override
            public void failure(@Nullable Exception exception) {
                calledDoOnFailure[0] = true;
            }
        });

        assertThat(calledDoOnFailure[0]).isTrue();
        assertThat(result).isEqualTo(failure);
    }

    @Test
    public void whenSucceeding_doOnFailure_shouldNotInvokeCallback() {
        Result<Integer> success = new Result.Success<Integer>(1);
        final boolean[] calledDoOnFailure = new boolean[]{false};

        Result result = success.doOnFailure(new Result.Failure.Callback() {
            @Override
            public void failure(@Nullable Exception exception) {
                calledDoOnFailure[0] = true;
            }
        });

        assertThat(calledDoOnFailure[0]).isFalse();
        assertThat(result).isEqualTo(success);
    }

    @Test
    public void whenCallbackIsThrowing_doOnFailure_shouldPropagateNewFailure() {
        Result failure = new Result.Failure();
        final RuntimeException runtimeException = new RuntimeException("illegalState");

        Result result = failure.doOnFailure(new Result.Failure.Callback() {
            @Override
            public void failure(@Nullable Exception exception) {
                throw runtimeException;
            }
        });

        assertThat(result).isEqualTo(new Result.Failure(runtimeException));
    }

    @Test
    public void whenSucceeding_doOnSuccess_shouldInvokeCallback() {
        Result<Integer> success = new Result.Success<Integer>(1);
        final boolean[] calledDoOnSuccess = new boolean[]{false};

        Result result = success.doOnSuccess(new Result.Success.Callback<Integer>() {
            @Override
            public void success(Integer data) {
                calledDoOnSuccess[0] = true;
            }
        });

        assertThat(calledDoOnSuccess[0]).isTrue();
        assertThat(result).isEqualTo(success);
    }

    @Test
    public void whenFailing_doOnSuccess_shouldNotInvokeCallback() {
        Result failure = new Result.Failure();
        final boolean[] calledDoOnSuccess = new boolean[]{false};

        Result result = failure.doOnSuccess(new Result.Success.Callback() {
            @Override
            public void success(Object data) {
                calledDoOnSuccess[0] = true;
            }
        });

        assertThat(calledDoOnSuccess[0]).isFalse();
        assertThat(result).isEqualTo(failure);
    }

    @Test
    public void whenCallbackIsThrowing_doOnSuccess_shouldPropagateFailure() {
        Result<Integer> success = new Result.Success<Integer>(1);
        final RuntimeException exception = new IllegalStateException("illegalState");

        Result result = success.doOnSuccess(new Result.Success.Callback<Integer>() {
            @Override
            public void success(Integer data) {
                throw exception;
            }
        });

        assertThat(result).isEqualTo(new Result.Failure(exception));
    }

    @Test
    public void doOnResult_shouldAlwaysInvokeCallback() {
        Result<Integer> success = new Result.Success<Integer>(1);
        Result failure = new Result.Failure();
        final boolean[] calledSuccess = new boolean[]{false};
        final boolean[] calledFailure = new boolean[]{false};

        Result successResult = success.doOnResult(new Result.ResultCallback() {
            @Override
            public void onResult() {
                calledSuccess[0] = true;
            }
        });
        Result failureResult = failure.doOnResult(new Result.ResultCallback() {
            @Override
            public void onResult() {
                calledFailure[0] = true;
            }
        });

        assertThat(calledSuccess[0]).isTrue();
        assertThat(successResult).isEqualTo(success);
        assertThat(calledFailure[0]).isTrue();
        assertThat(failureResult).isEqualTo(failure);
    }

    @Test
    public void whenCallbackIsThrowing_doOnResult_shouldPropagateFailure() {
        Result<Integer> success = new Result.Success<Integer>(1);
        final RuntimeException successException = new IllegalStateException("illegalState");
        Result failure = new Result.Failure();
        final RuntimeException failureException = new IllegalStateException("illegalStateFail");

        Result successResult = success.doOnResult(new Result.ResultCallback() {
            @Override
            public void onResult() {
                throw successException;
            }
        });
        Result failureResult = failure.doOnResult(new Result.ResultCallback() {
            @Override
            public void onResult() {
                throw failureException;
            }
        });

        assertThat(successResult).isEqualTo(new Result.Failure(successException));
        assertThat(failureResult).isEqualTo(new Result.Failure(failureException));
    }

    @Test
    public void whenResultIsFailure_flatMap_shouldNotInvokeCallback() {
        Result<Object> failure = new Result.Failure();

        Result<Integer> result = failure.flatMap(new Result.Mapper<Object, Result<Integer>>() {
            @Override
            public Result<Integer> map(Object from) {
                return new Result.Success<Integer>(1);
            }
        });

        assertThat(result).isEqualTo(failure);
    }

    @Test
    public void whenResultIsSuccess_flatMap_shouldReturnResultOfCallback() {
        Result<Integer> success = new Result.Success<Integer>(1);

        Result<String> result = success.flatMap(new Result.Mapper<Integer, Result<String>>() {
            @Override
            public Result<String> map(Integer from) {
                return new Result.Success<String>("Success!");
            }
        });

        assertThat(result).isEqualTo(new Result.Success<String>("Success!"));
    }

    @Test
    public void whenCallbackIsThrowing_flatMap_shouldPropagateFailure() {
        Result<Integer> success = new Result.Success<Integer>(1);
        final RuntimeException exception = new IllegalStateException("illegalState");

        Result<Integer> result = success.flatMap(new Result.Mapper<Integer, Result<Integer>>() {
            @Override
            public Result<Integer> map(Integer from) {
                throw exception;
            }
        });

        assertThat(result).isEqualTo(new Result.Failure(exception));
    }

    @Test
    public void whenResultIsFailure_map_shouldNotInvokeCallback() {
        Result<Object> failure = new Result.Failure();

        Result<Integer> result = failure.map(new Result.Mapper<Object, Integer>() {
            @Override
            public Integer map(Object from) {
                return 1;
            }
        });

        assertThat(result).isEqualTo(failure);
    }

    @Test
    public void whenResultIsSuccess_map_shouldReturnResultOfCallback() {
        Result<Integer> success = new Result.Success<Integer>(1);

        Result<String> result = success.map(new Result.Mapper<Integer, String>() {
            @Override
            public String map(Integer from) {
                return "Success!";
            }
        });

        assertThat(result).isEqualTo(new Result.Success<String>("Success!"));
    }

    @Test
    public void whenCallbackIsThrowing_map_shouldPropagateFailure() {
        Result<Integer> success = new Result.Success<Integer>(1);
        final RuntimeException exception = new IllegalStateException("illegalState");

        Result<Integer> result = success.map(new Result.Mapper<Integer, Integer>() {
            @Override
            public Integer map(Integer from) {
                throw exception;
            }
        });

        assertThat(result).isEqualTo(new Result.Failure(exception));
    }

    @Test
    public void whenCallbackIsThrowing_from_shouldPropagateFailure() {
        final RuntimeException exception = new IllegalStateException("illegalState");

        Result<Object> result = Result.from(new Result.Action<Object>() {
            @Override
            public Object run() {
                throw exception;
            }
        });

        assertThat(result).isEqualTo(new Result.Failure(exception));
    }

    @Test
    public void from_shouldPropagateValueOfCallbackAsResult() {
        final int value = 123;

        Result<Integer> result = Result.from(new Result.Action<Integer>() {
            @Override
            public Integer run() {
                return value;
            }
        });

        assertThat(result).isEqualTo(new Result.Success<Integer>(value));
    }

    @Test
    public void whenValueIsNotThrowable_of_shouldReturnItselfAsSuccess() {
        final int value = 1;

        Result<Integer> result = Result.of(value);

        assertThat(result).isEqualTo(new Result.Success<Integer>(1));
    }

    @Test
    public void whenValueIsThrowable_asResult_shouldReturnItselfAsFailure() {
        final RuntimeException throwable = new IllegalStateException("illegalState");

        Result result = Result.of(throwable);

        assertThat(result).isEqualTo(new Result.Failure(throwable));
    }
}