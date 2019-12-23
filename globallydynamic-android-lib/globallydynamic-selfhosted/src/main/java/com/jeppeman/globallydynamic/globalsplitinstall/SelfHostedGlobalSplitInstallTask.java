package com.jeppeman.globallydynamic.globalsplitinstall;

import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.OnFailureListener;
import com.google.android.play.core.tasks.OnSuccessListener;
import com.google.android.play.core.tasks.Task;
import com.jeppeman.globallydynamic.tasks.GlobalSplitInstallTask;
import com.jeppeman.globallydynamic.tasks.OnGlobalSplitInstallCompleteListener;
import com.jeppeman.globallydynamic.tasks.OnGlobalSplitInstallFailureListener;
import com.jeppeman.globallydynamic.tasks.OnGlobalSplitInstallSuccessListener;

import java.util.concurrent.Executor;

interface SelfHostedGlobalSplitInstallResultMapper<GPlayResult, TGlobalSplitResult> {
    TGlobalSplitResult map(GPlayResult from);
}

class SelfHostedGlobalSplitInstallTask<SelfHostedResult, TGlobalSplitResult> implements GlobalSplitInstallTask<TGlobalSplitResult> {
    private final Task<SelfHostedResult> delegate;
    private final SelfHostedGlobalSplitInstallResultMapper<SelfHostedResult, TGlobalSplitResult> resultMapper;

    SelfHostedGlobalSplitInstallTask(Task<SelfHostedResult> delegate) {
        this(delegate, new SelfHostedGlobalSplitInstallResultMapper<SelfHostedResult, TGlobalSplitResult>() {
            @Override
            public TGlobalSplitResult map(SelfHostedResult from) {
                return (TGlobalSplitResult) from;
            }
        });
    }

    SelfHostedGlobalSplitInstallTask(
            Task<SelfHostedResult> delegate,
            SelfHostedGlobalSplitInstallResultMapper<SelfHostedResult, TGlobalSplitResult> resultMapper) {
        this.delegate = delegate;
        this.resultMapper = resultMapper;
    }

    @Override
    public boolean isComplete() {
        return delegate.isComplete();
    }

    @Override
    public boolean isSuccessful() {
        return delegate.isSuccessful();
    }

    @Override
    public TGlobalSplitResult getResult() {
        return resultMapper.map(delegate.getResult());
    }

    @Override
    public Exception getException() {
        return GlobalSplitInstallExceptionFactory.create(delegate.getException());
    }

    @Override
    public GlobalSplitInstallTask<TGlobalSplitResult> addOnSuccessListener(final OnGlobalSplitInstallSuccessListener<TGlobalSplitResult> onSuccessListener) {
        delegate.addOnSuccessListener(new OnSuccessListener<SelfHostedResult>() {
            @Override
            public void onSuccess(SelfHostedResult result) {
                onSuccessListener.onSuccess(resultMapper.map(result));
            }
        });
        return this;
    }

    @Override
    public GlobalSplitInstallTask<TGlobalSplitResult> addOnSuccessListener(Executor executor, final OnGlobalSplitInstallSuccessListener<TGlobalSplitResult> onSuccessListener) {
        delegate.addOnSuccessListener(executor, new OnSuccessListener<SelfHostedResult>() {
            @Override
            public void onSuccess(SelfHostedResult result) {
                onSuccessListener.onSuccess(resultMapper.map(result));
            }
        });
        return this;
    }

    @Override
    public GlobalSplitInstallTask<TGlobalSplitResult> addOnFailureListener(final OnGlobalSplitInstallFailureListener onFailureListener) {
        delegate.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                GlobalSplitInstallException globalSplitInstallException =
                        GlobalSplitInstallExceptionFactory.create(e);
                onFailureListener.onFailure(globalSplitInstallException);
            }
        });
        return this;
    }

    @Override
    public GlobalSplitInstallTask<TGlobalSplitResult> addOnFailureListener(Executor executor, final OnGlobalSplitInstallFailureListener onFailureListener) {
        delegate.addOnFailureListener(executor, new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                GlobalSplitInstallException globalSplitInstallException =
                        GlobalSplitInstallExceptionFactory.create(e);
                onFailureListener.onFailure(globalSplitInstallException);
            }
        });
        return this;
    }

    @Override
    public GlobalSplitInstallTask<TGlobalSplitResult> addOnCompleteListener(final OnGlobalSplitInstallCompleteListener<TGlobalSplitResult> onCompleteListener) {
        delegate.addOnCompleteListener(new OnCompleteListener<SelfHostedResult>() {
            @Override
            public void onComplete(Task<SelfHostedResult> featureTask) {
                onCompleteListener.onComplete(SelfHostedGlobalSplitInstallTask.this);
            }
        });
        return this;
    }

    @Override
    public GlobalSplitInstallTask<TGlobalSplitResult> addOnCompleteListener(Executor executor, final OnGlobalSplitInstallCompleteListener<TGlobalSplitResult> onCompleteListener) {
        delegate.addOnCompleteListener(executor, new OnCompleteListener<SelfHostedResult>() {
            @Override
            public void onComplete(Task<SelfHostedResult> featureTask) {
                onCompleteListener.onComplete(SelfHostedGlobalSplitInstallTask.this);
            }
        });
        return this;
    }
}
