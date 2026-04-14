package com.example.translator;

import com.example.translator.auth.AuthenticationFilter;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api")
public class TranslatorApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(TranslatorResource.class);
        classes.add(AuthenticationFilter.class);
        return classes;
    }
}
