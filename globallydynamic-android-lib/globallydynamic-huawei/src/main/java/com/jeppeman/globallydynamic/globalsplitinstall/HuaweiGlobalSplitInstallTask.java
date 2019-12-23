package com.jeppeman.globallydynamic.globalsplitinstall;

import com.huawei.hms.feature.tasks.FeatureTask;
import com.huawei.hms.feature.tasks.listener.OnFeatureCompleteListener;
import com.huawei.hms.feature.tasks.listener.OnFeatureFailureListener;
import com.huawei.hms.feature.tasks.listener.OnFeatureSuccessListener;
import com.jeppeman.globallydynamic.tasks.GlobalSplitInstallTask;
import com.jeppeman.globallydynamic.tasks.OnGlobalSplitInstallCompleteListener;
import com.jeppeman.globallydynamic.tasks.OnGlobalSplitInstallFailureListener;
import com.jeppeman.globallydynamic.tasks.OnGlobalSplitInstallSuccessListener;

import java.util.concurrent.Executor;

interface HuaweiGlobalSplitInstallResultMapper<THuaweiResult, TGlobalSplitResult> {
    TGlobalSplitResult map(THuaweiResult from);
}

class HuaweiGlobalSplitInstallTask<THuaweiResult, TGlobalSplitResult> implements GlobalSplitInstallTask<TGlobalSplitResult> {
    private final FeatureTask<THuaweiResult> delegate;
    private final HuaweiGlobalSplitInstallResultMapper<THuaweiResult, TGlobalSplitResult> resultMapper;

    HuaweiGlobalSplitInstallTask(FeatureTask<THuaweiResult> delegate) {
        this(delegate, new HuaweiGlobalSplitInstallResultMapper<THuaweiResult, TGlobalSplitResult>() {
            @Override
            public TGlobalSplitResult map(THuaweiResult from) {
                return (TGlobalSplitResult) from;
            }
        });
    }

    HuaweiGlobalSplitInstallTask(
            FeatureTask<THuaweiResult> delegate,
            HuaweiGlobalSplitInstallResultMapper<THuaweiResult, TGlobalSplitResult> resultMapper) {
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
        return HuaweiGlobalSplitInstallExceptionFactory.create(delegate.getException());
    }

    @Override
    public GlobalSplitInstallTask<TGlobalSplitResult> addOnSuccessListener(final OnGlobalSplitInstallSuccessListener<TGlobalSplitResult> onSuccessListener) {
        delegate.addOnListener(new OnFeatureSuccessListener<THuaweiResult>() {
            @Override
            public void onSuccess(THuaweiResult result) {
                onSuccessListener.onSuccess(resultMapper.map(result));
            }
        });
        return this;
    }

    @Override
    public GlobalSplitInstallTask<TGlobalSplitResult> addOnSuccessListener(Executor executor, final OnGlobalSplitInstallSuccessListener<TGlobalSplitResult> onSuccessListener) {
        delegate.addOnListener(executor, new OnFeatureSuccessListener<THuaweiResult>() {
            @Override
            public void onSuccess(THuaweiResult result) {
                onSuccessListener.onSuccess(resultMapper.map(result));
            }
        });
        return this;
    }

    @Override
    public GlobalSplitInstallTask<TGlobalSplitResult> addOnFailureListener(final OnGlobalSplitInstallFailureListener onFailureListener) {
        delegate.addOnListener(new OnFeatureFailureListener<THuaweiResult>() {
            @Override
            public void onFailure(Exception e) {
                GlobalSplitInstallException globalSplitInstallException =
                        HuaweiGlobalSplitInstallExceptionFactory.create(e);
                onFailureListener.onFailure(globalSplitInstallException);
            }
        });
        return this;
    }

    @Override
    public GlobalSplitInstallTask<TGlobalSplitResult> addOnFailureListener(Executor executor, final OnGlobalSplitInstallFailureListener onFailureListener) {
        delegate.addOnListener(executor, new OnFeatureFailureListener<THuaweiResult>() {
            @Override
            public void onFailure(Exception e) {
                GlobalSplitInstallException globalSplitInstallException =
                        HuaweiGlobalSplitInstallExceptionFactory.create(e);
                onFailureListener.onFailure(globalSplitInstallException);
            }
        });
        return this;
    }

    @Override
    public GlobalSplitInstallTask<TGlobalSplitResult> addOnCompleteListener(final OnGlobalSplitInstallCompleteListener<TGlobalSplitResult> onCompleteListener) {
        delegate.addOnListener(new OnFeatureCompleteListener<THuaweiResult>() {
            @Override
            public void onComplete(FeatureTask<THuaweiResult> featureTask) {
                onCompleteListener.onComplete(HuaweiGlobalSplitInstallTask.this);
            }
        });
        return this;
    }

    @Override
    public GlobalSplitInstallTask<TGlobalSplitResult> addOnCompleteListener(Executor executor, final OnGlobalSplitInstallCompleteListener<TGlobalSplitResult> onCompleteListener) {
        delegate.addOnListener(executor, new OnFeatureCompleteListener<THuaweiResult>() {
            @Override
            public void onComplete(FeatureTask<THuaweiResult> featureTask) {
                onCompleteListener.onComplete(HuaweiGlobalSplitInstallTask.this);
            }
        });
        return this;
    }
}
