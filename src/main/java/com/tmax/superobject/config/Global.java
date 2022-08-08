package com.tmax.superobject.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tmax.superobject.object.TaskObject;

import java.net.SocketAddress;
import java.net.URLClassLoader;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Global {
    private static Global instance = new Global();
    private URLClassLoader serviceClassLoader;
    public static ThreadLocal<TaskObject> currentTask = new ThreadLocal<>();
    public static ThreadLocal<List<TaskObject>> afterService = new ThreadLocal<>();
    public static ThreadLocal<Gson> gson = new ThreadLocal<Gson>() {
        @Override
        protected Gson initialValue() {
            return new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        };
    };
    private Long unsafeSeq = 0L;
    private Long taskIdSeq = 0L;
    private AtomicLong safeSeq = new AtomicLong(0);
    private SocketAddress serviceEm;

    public SocketAddress getServiceEm() {
        return serviceEm;
    }

    public void setServiceEm(SocketAddress serviceEm) {
        this.serviceEm = serviceEm;
    }

    public static Global getInstance() {
        return instance;
    }

    public Long unsafeSeq() {
        return unsafeSeq++;
    }

    public Long taskIdSeq() {
        return taskIdSeq++;
    }

    public Long safeSeq() {
        return safeSeq.incrementAndGet();
    }

    public Object config() {
        return null;
    }

    public void setServiceClassLoader(URLClassLoader classLoader) {
        this.serviceClassLoader = classLoader;
    }

    public URLClassLoader getServiceClassLoader() {
        return this.serviceClassLoader;
    }
}
