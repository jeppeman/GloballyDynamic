package com.jeppeman.locallydynamic;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class GLExtensionsExtractorImplTest {
    private GLExtensionsExtractorImpl glExtensionsExtractor = new GLExtensionsExtractorImpl(
            ApplicationProvider.getApplicationContext()
    );

    @Test
    public void test() {
        //TODO: add EGL shadow?
        System.out.println(glExtensionsExtractor.extract());
    }
}