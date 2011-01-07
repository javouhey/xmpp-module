package com.raverun.im.infrastructure.system.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class ExecutorServiceProvider implements Provider<ExecutorService>
{

    @Override
    public ExecutorService get()
    {
        return Executors.newCachedThreadPool();
    }

}
