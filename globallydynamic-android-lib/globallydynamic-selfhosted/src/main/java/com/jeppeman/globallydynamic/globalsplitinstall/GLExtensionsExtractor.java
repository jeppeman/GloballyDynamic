package com.jeppeman.globallydynamic.globalsplitinstall;

import android.content.Context;
import android.content.pm.FeatureInfo;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES10;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class GLExtensionsExtractorFactory {
    static GLExtensionsExtractor create(@NonNull Context context) {
        return new GLExtensionsExtractorImpl(context);
    }
}

interface GLExtensionsExtractor {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    List<String> extract();
}

class GLExtensionsExtractorImpl implements GLExtensionsExtractor {
    private final Pair<Integer, Integer> supportedGlEsVersion;

    GLExtensionsExtractorImpl(@NonNull Context context) {
        supportedGlEsVersion = getSupportedGlEsVersion(context);
    }

    private Pair<Integer, Integer> getSupportedGlEsVersion(@NonNull Context context) {
        FeatureInfo[] features = context.getPackageManager().getSystemAvailableFeatures();

        if (features != null) {
            for (FeatureInfo featureInfo : features) {
                if (featureInfo.name == null) {
                    if (featureInfo.reqGlEsVersion != FeatureInfo.GL_ES_VERSION_UNDEFINED) {
                        return Pair.create(
                                (featureInfo.reqGlEsVersion & 0xffff0000) >> 16,
                                featureInfo.reqGlEsVersion & 0x0000ffff
                        );
                    } else {
                        return Pair.create(1, 0);
                    }
                }
            }
        }

        return Pair.create(1, 0);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public List<String> extract() {
        EGLDisplay eglDisplay = EGL14.eglGetDisplay(0);
        EGL14.eglInitialize(
                eglDisplay,
                new int[]{supportedGlEsVersion.first},
                0,
                new int[]{supportedGlEsVersion.second},
                0
        );
        EGL14.eglBindAPI(EGL14.EGL_OPENGL_ES_API);

        int[] pi32ConfigAttribs = new int[]{
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 0,
                EGL14.EGL_STENCIL_SIZE, 0,
                EGL14.EGL_NONE
        };

        int[] iConfigs = new int[]{0};
        EGLConfig[] eglConfig = new EGLConfig[]{null};
        EGL14.eglChooseConfig(
                eglDisplay,
                pi32ConfigAttribs,
                0,
                eglConfig,
                0,
                1,
                iConfigs,
                0
        );

        EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(
                eglDisplay,
                eglConfig[0],
                new int[]{EGL14.EGL_NONE},
                0
        );

        int[] attribList = new int[]{
                EGL14.EGL_CONTEXT_CLIENT_VERSION, supportedGlEsVersion.first,
                EGL14.EGL_NONE
        };

        EGLContext eglContext = EGL14.eglCreateContext(
                eglDisplay,
                eglConfig[0],
                EGL14.EGL_NO_CONTEXT,
                attribList,
                0
        );

        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);

        String extensions = GLES10.glGetString(GLES10.GL_EXTENSIONS);

        EGL14.eglDestroyContext(eglDisplay, eglContext);
        EGL14.eglDestroySurface(eglDisplay, eglSurface);

        return extensions != null
                ? Arrays.asList(extensions.split(" "))
                : new ArrayList<String>();
    }
}