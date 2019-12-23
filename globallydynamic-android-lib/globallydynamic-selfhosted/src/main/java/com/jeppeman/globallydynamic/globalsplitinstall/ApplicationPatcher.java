package com.jeppeman.globallydynamic.globalsplitinstall;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.ArrayMap;

import com.google.android.play.core.splitcompat.SplitCompat;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.RequiresApi;

class ApplicationPatcherFactory {
    static ApplicationPatcher create(Context context, Logger logger) {
        return new ApplicationPatcherImpl(context, logger);
    }
}

interface ApplicationPatcher {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    void patchExistingApplication();
}

class ApplicationPatcherImpl implements ApplicationPatcher {
    private final Context context;
    private final Logger logger;
    private ClassLoader originalClassLoader;

    ApplicationPatcherImpl(Context context, Logger logger) {
        this.context = context;
        this.logger = logger;
        this.originalClassLoader = context.getClassLoader();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private List<String> getSourcePaths() throws PackageManager.NameNotFoundException {
        List<String> ret = new LinkedList<String>();
        Context newContext = context.createPackageContext(context.getPackageName(), 0);
        ApplicationInfo applicationInfo = newContext.getApplicationInfo();
        if (applicationInfo.publicSourceDir != null) {
            ret.add(applicationInfo.publicSourceDir);
        }
        if (applicationInfo.splitPublicSourceDirs != null) {
            ret.addAll(Arrays.asList(applicationInfo.splitPublicSourceDirs));
        }
        return ret;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private List<File> getNativePaths() throws PackageManager.NameNotFoundException {
        Context newContext = context.createPackageContext(context.getPackageName(), 0);
        ApplicationInfo applicationInfo = newContext.getApplicationInfo();List<File> ret = new LinkedList<File>();
        if (applicationInfo.nativeLibraryDir != null) {
            ret.add(new File(applicationInfo.nativeLibraryDir));
        }
        return ret;
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void patchOriginalClassLoader() throws
            NoSuchMethodException,
            InvocationTargetException,
            IllegalAccessException,
            PackageManager.NameNotFoundException {
        try {
            // Clear the outdated dex paths from the original ClassLoader
            Object pathList = ReflectionUtils.getFieldValue(originalClassLoader, "pathList");
            Object dexElements = ReflectionUtils.getFieldValue(pathList, "dexElements");
            Object newDexElements = Array.newInstance(dexElements.getClass().getComponentType(), 0);
            ReflectionUtils.setFieldValue(pathList, "dexElements", newDexElements);
            ReflectionUtils.setFieldValue(pathList, "nativeLibraryDirectories", getNativePaths());
        } catch (Exception exception) {
            logger.e("Failed to clear outdated dex paths from ClassLoader", exception);
        }

        // Update with the new paths
        Method addDexPathMethod = ReflectionUtils.getMethod(originalClassLoader, "addDexPath", String.class);
        boolean wasAccessible = addDexPathMethod.isAccessible();
        addDexPathMethod.setAccessible(true);
        for (String sourcePath : getSourcePaths()) {
            addDexPathMethod.invoke(originalClassLoader, sourcePath);
        }
        addDexPathMethod.setAccessible(wasAccessible);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void updateAppInfo() throws
            ClassNotFoundException,
            NoSuchFieldException,
            IllegalAccessException,
            NoSuchMethodException,
            InvocationTargetException {
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Object currentActivityThread = ReflectionUtils.getFieldValue(activityThreadClass, "sCurrentActivityThread");

        try {
            // Make sure references to outdated resource paths are cleared
            Object resourcesManager = ReflectionUtils.getFieldValue(currentActivityThread, "mResourcesManager");
            ArrayMap<Object, Object> arrayMap = new ArrayMap<Object, Object>();
            ReflectionUtils.setFieldValue(resourcesManager, "mResourceImpls", arrayMap);
        } catch (Exception exception) {
            logger.e("Failed to update resource references");
        }
        // Update application info of running activities, this enables them to access to
        // code / resources from the new module
        Object appThread = ReflectionUtils.getFieldValue(currentActivityThread, "mAppThread");
        Method dispatchMethod = ReflectionUtils.getMethod(appThread, "dispatchPackageBroadcast", int.class, String[].class);
        dispatchMethod.setAccessible(true);
        dispatchMethod.invoke(appThread, 2, new String[]{context.getPackageName()});
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void retainOriginalApplicationInstance() throws
            NoSuchFieldException,
            IllegalAccessException,
            ClassNotFoundException {
        // Make sure that the original application instance and it's accompanying ClassLoader is
        // retained after an installation. If we do not do this the runtime will be polluted with
        // multiple application instances which inevitably leads to undesired behavior
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Object currentActivityThread = ReflectionUtils.getFieldValue(activityThreadClass, "sCurrentActivityThread");
        ArrayMap<String, ?> map = (ArrayMap<String, ?>) ReflectionUtils.getFieldValue(currentActivityThread, "mPackages");
        WeakReference<?> ref = (WeakReference<?>) map.get(context.getPackageName());
        Object loadedApk = ref.get();
        List<Application> currentApplications = (List<Application>) ReflectionUtils.getFieldValue(currentActivityThread, "mAllApplications");
        ArrayList<Application> applications = new ArrayList<Application>();
        Application originalAppInstance = currentApplications.get(0);
        applications.add(originalAppInstance);
        ReflectionUtils.setFieldValue(loadedApk, "mClassLoader", originalClassLoader);
        ReflectionUtils.setFieldValue(loadedApk, "mApplication", originalAppInstance);
        ReflectionUtils.setFieldValue(currentActivityThread, "mAllApplications", applications);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void patchExistingApplication() {
        SplitCompat.a(context);

        // Order matters here
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
            try {
                updateAppInfo();
            } catch (Exception exception) {
                logger.e("Failed to patch update app info", exception);
            }
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            try {
                patchOriginalClassLoader();
            } catch (Exception exception) {
                logger.e("Failed to patch ClassLoader", exception);
            }
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
            try {
                retainOriginalApplicationInstance();
            } catch (Exception exception) {
                logger.e("Failed to retain original application instance", exception);
            }
        }
    }
}

