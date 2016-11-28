package com.android.quickmultidex;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;


import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * @version V1.0
 * @author: lizhangqu
 * @date: 2016-11-25 21:06
 */
public class Multidex {
    private static final String TAG = "Multidex";

    public static boolean install(Context context) {
        try {
            long start = System.nanoTime();
            boolean ret = false;
            long startLoadDexData = System.nanoTime();
            List<Integer> cookies = loadDex(context);
            long endLoadDexData = System.nanoTime();
            Log.e(TAG, "loadDexData time：" + (endLoadDexData - startLoadDexData) + " ns");
            Log.e(TAG, "loadDexData time：" + (endLoadDexData - startLoadDexData) / 1000000 + " ms");

            if (cookies != null && cookies.size() > 0) {
                long startInject = System.nanoTime();
                boolean result = inject(context, cookies);
                long endInject = System.nanoTime();
                Log.e(TAG, "inject time：" + (endInject - startInject) + " ns");
                Log.e(TAG, "inject time：" + (endInject - startInject) / 1000000 + " ms");
                ret = result;
            } else {
                ret = false;
            }
            Log.e(TAG, "install result:" + ret);
            long end = System.nanoTime();
            Log.e(TAG, "install time：" + (end - start) + " ns");
            Log.e(TAG, "install time：" + (end - start) / 1000000 + " ms");
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    public static boolean injectClassLoader(Context context, List<Integer> cookies) {
        ClassLoader inject = IncrementalClassLoader.inject(cookies, context.getClassLoader(), null, null, null);
        return inject != null;
    }


    public static boolean inject(Context base, List<Integer> cookies) {
        try {
            ApplicationInfo applicationInfo = base.getApplicationInfo();
            String sourceDir = applicationInfo.sourceDir;

            Field pathListField = findField(base.getClassLoader(), "pathList");
            Object pathList = pathListField.get(base.getClassLoader());

            Method makeDexElements = null;
            if (Build.VERSION.SDK_INT < 19) {
                makeDexElements =
                        findMethod(pathList, "makeDexElements", ArrayList.class, File.class);

            } else {
                makeDexElements =
                        findMethod(pathList, "makeDexElements", ArrayList.class, File.class,
                                ArrayList.class);

            }
            Object[] invokeElements = null;
            ArrayList<File> files = new ArrayList<>();
            for (int i = 0; i < cookies.size(); i++) {
                files.add(new File(sourceDir));
            }
            if (Build.VERSION.SDK_INT < 19) {
                invokeElements = (Object[]) makeDexElements.invoke(pathList, files, null);
            } else {
                invokeElements = (Object[]) makeDexElements.invoke(pathList, files, null, null);
            }

            Field dexElementsFiled = Multidex.findField(pathList, "dexElements");
            Object[] originalDexElements = (Object[]) dexElementsFiled.get(pathList);
            Object[] resultDexElements = (Object[]) Array.newInstance(originalDexElements.getClass().getComponentType(), originalDexElements.length + invokeElements.length);

            System.arraycopy(originalDexElements, 0, resultDexElements, 0, originalDexElements.length);
            System.arraycopy(invokeElements, 0, resultDexElements, originalDexElements.length, invokeElements.length);

            int length = originalDexElements.length;
            for (int i = 0; i < cookies.size(); i++) {
                Object dexElements = resultDexElements[length + i];
                Field fileField = Multidex.findField(dexElements, "file");
                fileField.set(dexElements, null);
                Field zipField = Multidex.findField(dexElements, "zip");
                zipField.set(dexElements, null);
                Field zipFileField = Multidex.findField(dexElements, "zipFile");
                zipFileField.set(dexElements, null);
                Field dexFileField = Multidex.findField(dexElements, "dexFile");
                Object o = dexFileField.get(dexElements);
                Field mCookieField = Multidex.findField(o, "mCookie");
                mCookieField.set(o, cookies.get(i));
                Field mFileNameFiled = Multidex.findField(o, "mFileName");
                mFileNameFiled.set(o, null);
            }

            dexElementsFiled.set(pathList, resultDexElements);
            return true;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static List<Integer> loadDex(Context context) throws Exception {

        ArrayList<Integer> list = new ArrayList<>();

        ApplicationInfo applicationInfo = context.getApplicationInfo();
        String sourceDir = applicationInfo.sourceDir;

        List<byte[]> dexByteslist = performExtractions(sourceDir);
        if (dexByteslist != null && dexByteslist.size() > 0) {
            for (byte[] dexBytes : dexByteslist) {
                int i = openDexFile(dexBytes);
                Log.e(TAG, "loadDex openDexFile cookie:" + i);
                list.add(i);
            }
        } else {
            Log.e(TAG, "loadDex performExtractions null");

        }
        return list;
    }

    public static Field findField(Object instance, String name) throws NoSuchFieldException {
        Class clazz = instance.getClass();
        while (clazz != null) {
            try {
                Field e = clazz.getDeclaredField(name);
                if (!e.isAccessible()) {
                    e.setAccessible(true);
                }

                return e;
            } catch (NoSuchFieldException var4) {
                clazz = clazz.getSuperclass();
            }
        }

        throw new NoSuchFieldException("Field " + name + " not found in " + instance.getClass());
    }

    public static Method findMethod(Object instance, String name, Class... parameterTypes) throws NoSuchMethodException {
        Class clazz = instance.getClass();

        while (clazz != null) {
            try {
                Method e = clazz.getDeclaredMethod(name, parameterTypes);
                if (!e.isAccessible()) {
                    e.setAccessible(true);
                }

                return e;
            } catch (NoSuchMethodException var5) {
                clazz = clazz.getSuperclass();
            }
        }

        throw new NoSuchMethodException("Method " + name + " with parameters " + Arrays.asList(parameterTypes) + " not found in " + instance.getClass());
    }


    private static final String DEX_PREFIX = "classes";
    private static final String DEX_SUFFIX = ".dex";
    private static final int MAX_EXTRACT_ATTEMPTS = 3;

    private static List<byte[]> performExtractions(String sourceApk)
            throws IOException {
        List<byte[]> dexDatas = new ArrayList<byte[]>();

        final ZipFile apk = new ZipFile(sourceApk);
        try {
            int secondaryNumber = 2;
            ZipEntry dexFile = apk.getEntry(DEX_PREFIX + secondaryNumber + DEX_SUFFIX);
            while (dexFile != null) {
                int numAttempts = 0;
                boolean isExtractionSuccessful = false;
                while (numAttempts < MAX_EXTRACT_ATTEMPTS && !isExtractionSuccessful) {
                    numAttempts++;
                    byte[] extract = extract(apk, dexFile);
                    if (extract == null) {
                        isExtractionSuccessful = false;
                    } else {
                        dexDatas.add(extract);
                        isExtractionSuccessful = true;
                    }
                }
                if (!isExtractionSuccessful) {
                    throw new IOException("Could not create extra file " +
                            " for secondary dex (" +
                            secondaryNumber + ")");
                }
                secondaryNumber++;
                dexFile = apk.getEntry(DEX_PREFIX + secondaryNumber + DEX_SUFFIX);
            }
            return dexDatas;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                apk.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Failed to close resource", e);
            }
        }
        return null;
    }

    private static byte[] extract(ZipFile apk, ZipEntry dexFile) throws IOException {
        InputStream input = apk.getInputStream(dexFile);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        closeQuietly(output);
        closeQuietly(input);
        return output.toByteArray();
    }

    private static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            Log.w(TAG, "Failed to close resource", e);
        }
    }

    static {
        System.loadLibrary("multidex");
    }

    public static int openDexFile(byte[] dexBytes) throws Exception {
        return openDexFile(dexBytes, dexBytes.length);
    }

    /*
     * Open a DEX file based on a {@code byte[]}. The value returned
     * is a magic VM cookie. On failure, a RuntimeException is thrown.
     */
    private native static int openDexFile(byte[] fileContents, long length);


}
