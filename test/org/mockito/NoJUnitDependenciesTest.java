package org.mockito;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.mockito.cglib.proxy.Enhancer;
import org.mockitoutil.ClassLoaders;
import org.objenesis.Objenesis;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class NoJUnitDependenciesTest {
    @Test
    public void pure_mockito_should_not_depend_JUnit() throws Exception {
        ClassLoader classLoader_without_JUnit = ClassLoaders.excludingClassLoader()
                .withCodeSourceUrlOf(Mockito.class)
                .withCodeSourceUrlOf(Matcher.class)
                .withCodeSourceUrlOf(Enhancer.class)
                .withCodeSourceUrlOf(Objenesis.class)
                .without("junit", "org.junit")
                .build();

        Set<String> pureMockitoAPIClasses = classesIn(classLoader_without_JUnit, "runners", "junit", "JUnit");

        for (String pureMockitoAPIClass : pureMockitoAPIClasses) {
            checkDependency(classLoader_without_JUnit, pureMockitoAPIClass);
        }
    }

    private void checkDependency(ClassLoader classLoader_without_JUnit, String pureMockitoAPIClass) throws ClassNotFoundException {
        try {
            Class.forName(pureMockitoAPIClass, true, classLoader_without_JUnit);
        } catch (Throwable e) {
            throw new AssertionError(String.format("'%s' has some dependency to JUnit", pureMockitoAPIClass), e);
        }
    }

    private Set<String> classesIn(ClassLoader classLoader_without_JUnit, String... packageFilter) throws IOException, URISyntaxException {
        Enumeration<URL> roots = classLoader_without_JUnit.getResources("");

        Set<String> classes = new HashSet<String>();
        while(roots.hasMoreElements()) {
            File root = new File(roots.nextElement().toURI());
            classes.addAll(findClasses(root, root, packageFilter));
        }
        return classes;
    }

    private Set<String> findClasses(File root, File file, String... packageFilters) {
        if(file.isDirectory()) {
            File[] files = file.listFiles();
            Set<String> classes = new HashSet<String>();
            for (File children : files) {
                classes.addAll(findClasses(root, children, packageFilters));
            }
            return classes;
        } else {
            String qualifiedName = classNameFor(root, file);
            if (file.getName().endsWith(".class") && excludes(qualifiedName, packageFilters)) {
                return Collections.singleton(qualifiedName);
            }
        }
        return Collections.emptySet();
    }

    private boolean excludes(String qualifiedName, String... packageFilters) {
        for (String filter : packageFilters) {
            if(qualifiedName.contains(filter)) return false;
        }
        return true;
    }

    private String classNameFor(File root, File file) {
        String temp = file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1).replace('/', '.');
        return temp.subSequence(0, temp.indexOf(".class")).toString();
    }
}
