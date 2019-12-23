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

interface GPlayGlobalSplitInstallResultMapper<GPlayResult, TGlobalSplitResult> {
    TGlobalSplitResult map(GPlayResult from);
}

class GPlayGlobalSplitInstallTask<GPlayResult, TGlobalSplitResult> implements GlobalSplitInstallTask<TGlobalSplitResult> {
    private final Task<GPlayResult> delegate;
    private final GPlayGlobalSplitInstallResultMapper<GPlayResult, TGlobalSplitResult> resultMapper;

    GPlayGlobalSplitInstallTask(Task<GPlayResult> delegate) {
        this(delegate, new GPlayGlobalSplitInstallResultMapper<GPlayResult, TGlobalSplitResult>() {
            @Override
            public TGlobalSplitResult map(GPlayResult from) {
                return (TGlobalSplitResult) from;
            }
        });
    }

    GPlayGlobalSplitInstallTask(
            Task<GPlayResult> delegate,
            GPlayGlobalSplitInstallResultMapper<GPlayResult, TGlobalSplitResult> resultMapper) {
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
        return GPlayGlobalSplitInstallExceptionFactory.create(delegate.getException());
    }

    @Override
    public GlobalSplitInstallTask<TGlobalSplitResult> addOnSuccessListener(final OnGlobalSplitInstallSuccessListener<TGlobalSplitResult> onSuccessListener) {
        delegate.addOnSuccessListener(new OnSuccessListener<GPlayResult>() {
            @Override
            public void onSuccess(GPlayResult result) {
                onSuccessListener.onSuccess(resultMapper.map(result));
            }
        });
        return this;
    }

    @Override
    public GlobalSplitInstallTask<TGlobalSplitResult> addOnSuccessListener(Executor executor, final OnGlobalSplitInstallSuccessListener<TGlobalSplitResult> onSuccessListener) {
        delegate.addOnSuccessListener(executor, new OnSuccessListener<GPlayResult>() {
            @Override
            public void onSuccess(GPlayResult result) {
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
                        GPlayGlobalSplitInstallExceptionFactory.create(e);
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
                        GPlayGlobalSplitInstallExceptionFactory.create(e);
                onFailureListener.onFailure(globalSplitInstallException);
            }
        });
        return this;
    }

    @Override
    public GlobalSplitInstallTask<TGlobalSplitResult> addOnCompleteListener(final OnGlobalSplitInstallCompleteListener<TGlobalSplitResult> onCompleteListener) {
        delegate.addOnCompleteListener(new OnCompleteListener<GPlayResult>() {
            @Override
            public void onComplete(Task<GPlayResult> featureTask) {
                onCompleteListener.onComplete(GPlayGlobalSplitInstallTask.this);
            }
        });
        return this;
    }

    @Override
    public GlobalSplitInstallTask<TGlobalSplitResult> addOnCompleteListener(Executor executor, final OnGlobalSplitInstallCompleteListener<TGlobalSplitResult> onCompleteListener) {
        delegate.addOnCompleteListener(executor, new OnCompleteListener<GPlayResult>() {
            @Override
            public void onComplete(Task<GPlayResult> featureTask) {
                onCompleteListener.onComplete(GPlayGlobalSplitInstallTask.this);
            }
        });
        return this;
    }
}
