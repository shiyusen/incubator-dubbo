/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.bootstrap;

import org.apache.dubbo.config.DubboShutdownHook;
import org.apache.dubbo.config.ServiceConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * A bootstrap class to easily start and stop Dubbo via programmatic API.
 * The bootstrap class will be responsible to cleanup the resources during stop.
 * dubbo服务启动类
 */
public class DubboBootstrap {

    /**
     * The list of ServiceConfig
     */
    private List<ServiceConfig> serviceConfigList;

    /**
     * Whether register the shutdown hook during start?
     */
    private final boolean registerShutdownHookOnStart;

    /**
     * The shutdown hook used when Dubbo is running under embedded environment
     */
    private DubboShutdownHook shutdownHook;

    public DubboBootstrap() {
        this(true, DubboShutdownHook.getDubboShutdownHook());
    }

    public DubboBootstrap(boolean registerShutdownHookOnStart) {
        this(registerShutdownHookOnStart, DubboShutdownHook.getDubboShutdownHook());
    }

    public DubboBootstrap(boolean registerShutdownHookOnStart, DubboShutdownHook shutdownHook) {
        this.serviceConfigList = new ArrayList<ServiceConfig>();
        this.shutdownHook = shutdownHook;
        this.registerShutdownHookOnStart = registerShutdownHookOnStart;
    }

    /**
     * Register service config to bootstrap, which will be called during {@link DubboBootstrap#stop()}
     * @param serviceConfig the service
     * @return the bootstrap instance
     */
    public DubboBootstrap registerServiceConfig(ServiceConfig serviceConfig) {
        serviceConfigList.add(serviceConfig);
        return this;
    }

    /**
     * 注册钩子和发布服务
     */
    public void start() {
        if (registerShutdownHookOnStart) {
            registerShutdownHook();
        } else {
            // DubboShutdown hook has been registered in AbstractConfig,
            // we need to remove it explicitly
            removeShutdownHook();
        }
        //serviceConfigList 沒有初始化的調用。所以真正的服務發佈并不在此
        //此處可能只启动了一个空服务
        //真正的服务发布：org.apache.dubbo.config.spring.ServiceBean
        for (ServiceConfig serviceConfig: serviceConfigList) {
            serviceConfig.export();
        }
    }

    /**
     * 下线服务
     */
    public void stop() {
        for (ServiceConfig serviceConfig: serviceConfigList) {
            serviceConfig.unexport();
        }
        shutdownHook.destroyAll();
        if (registerShutdownHookOnStart) {
            removeShutdownHook();
        }
    }

    /**
     * Register the shutdown hook
     */
    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * Remove this shutdown hook
     */
    public void removeShutdownHook() {
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }
        catch (IllegalStateException ex) {
            // ignore - VM is already shutting down
        }
    }
}
