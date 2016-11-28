package com.android.quickmultidex;

import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexFile;

/**
 * @version V1.0
 * @author: lizhangqu
 * @date: 2016-11-28 16:39
 */
public class IncrementalClassLoader extends ClassLoader {
    /**
     * When false, compiled out of runtime library
     */
    public static final boolean DEBUG_CLASS_LOADING = false;
    private final DelegateClassLoader delegateClassLoader;

    public IncrementalClassLoader(
            List<Integer> cookies, ClassLoader original, String nativeLibraryPath, File codeCacheDir, List<String> dexes) {
        super(original.getParent());
        // TODO(bazel-team): For some mysterious reason, we need to use two class loaders so that
        // everything works correctly. Investigate why that is the case so that the code can be
        // simplified.
        delegateClassLoader = createDelegateClassLoader(cookies, nativeLibraryPath, codeCacheDir, dexes,
                original);
    }

    @Override
    public Class<?> findClass(String className) throws ClassNotFoundException {
        try {
            Class<?> aClass = delegateClassLoader.findClass(className);

            return aClass;
        } catch (ClassNotFoundException e) {

            throw e;
        }
    }

    /**
     * A class loader whose only purpose is to make {@code findClass()} public.
     */
    private static class DelegateClassLoader extends BaseDexClassLoader {
        private Method defineClass;
        private List<Integer> cookies;

        private DelegateClassLoader(
                List<Integer> cookies, String dexPath, File optimizedDirectory, String libraryPath, ClassLoader parent) {
            super(dexPath, optimizedDirectory, libraryPath, parent);
            this.cookies = cookies;
            try {
                defineClass = DexFile.class.getDeclaredMethod("defineClassNative", String.class, ClassLoader.class, int.class);
                defineClass.setAccessible(true);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            Log.e("TAG", "findClass:" + name);

            Class<?> aClass = null;
            try {
                aClass = super.findClass(name);
            } catch (ClassNotFoundException e) {
                for (int i = 0; i < cookies.size(); i++) {
                    try {
                        aClass = (Class<?>) defineClass.invoke(null, name.replace('.', '/'), this, cookies.get(i));
                        if (aClass != null) {
                            break;
                        }
                    } catch (IllegalAccessException e1) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e2) {
                        e.printStackTrace();
                    }
                }
            }
            return aClass;

        }
    }

    private static DelegateClassLoader createDelegateClassLoader(
            List<Integer> cookies, String nativeLibraryPath, File codeCacheDir, List<String> dexes,
            ClassLoader original) {
        String pathBuilder = createDexPath(dexes);
        return new DelegateClassLoader(cookies, pathBuilder, codeCacheDir,
                nativeLibraryPath, original);
    }

    private static String createDexPath(List<String> dexes) {
        StringBuilder pathBuilder = new StringBuilder();
        boolean first = true;
        if (dexes != null) {
            for (String dex : dexes) {
                if (first) {
                    first = false;
                } else {
                    pathBuilder.append(File.pathSeparator);
                }
                pathBuilder.append(dex);
            }
        }
        return pathBuilder.toString();
    }

    private static void setParent(ClassLoader classLoader, ClassLoader newParent) {
        try {
            Field parent = ClassLoader.class.getDeclaredField("parent");
            parent.setAccessible(true);
            parent.set(classLoader, newParent);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static ClassLoader inject(
            List<Integer> cookies, ClassLoader classLoader, String nativeLibraryPath, File codeCacheDir,
            List<String> dexes) {
        IncrementalClassLoader incrementalClassLoader =
                new IncrementalClassLoader(cookies, classLoader, nativeLibraryPath, codeCacheDir, dexes);
        setParent(classLoader, incrementalClassLoader);
        // This works as follows:
        // We're given the current class loader that's used to load the bootstrap application.
        // We have a new class loader which reads patches/overrides from the data directory
        // instead. We want *that* class loader to have the bootstrap class loader's parent
        // as its parent, and then we make the bootstrap class loader parented by our
        // class loader.
        //
        // In other words, we have this:
        //      BootstrapApplication.classLoader = ClassLoader1, parent=ClassLoader2
        // We create ClassLoader3 from the .dex files in the data directory, and arrange for
        // the hierarchy to be like this:
        //      BootstrapApplication.classLoader = ClassLoader1, parent=ClassLoader3, parent=ClassLoader2
        // With this approach, a class find (which should always look at the parents first) should
        // find anything from ClassLoader3 before they get them from ClassLoader1.
        // (Note that ClassLoader2 in the above is generally the BootClassLoader, not containing
        // any classes we care about.)
        return incrementalClassLoader;
    }
}