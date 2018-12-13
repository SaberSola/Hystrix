/**
 * Copyright 2012 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.hystrix.examples.basic;

import static org.junit.Assert.*;

import org.junit.Test;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;

/**
 * Sample {@link HystrixCommand} showing how implementing the {@link #getCacheKey()} method
 * enables request caching for eliminating duplicate calls within the same request context.
 *
 * 请求启用缓存 当上下文是同一调用的时候
 */
public class CommandUsingRequestCache extends HystrixCommand<Boolean> {

    private final int value;

    protected CommandUsingRequestCache(int value) {
        super(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"));
        this.value = value;
    }

    @Override
    protected Boolean run() {
        return value == 0 || value % 2 == 0;
    }

    /**
     * 需要启用降级的话就重写这个方法
     * @return
     */
    @Override
    protected Boolean getFallback() {
        return super.getFallback();
    }

    @Override
    protected String getCacheKey() {
        return String.valueOf(value);
    }

    public static class UnitTest {

        /**
         *HystrixRequestContext 目的是维护一个状态量 且被多线程共享
         *使用cache的话 需要HystrixRequestContext 维护声明周期
         */
        @Test
        public void testWithoutCacheHits() {
            HystrixRequestContext context = HystrixRequestContext.initializeContext();
            try {
                System.out.println(new CommandUsingRequestCache(2).execute());
                System.out.println(new CommandUsingRequestCache(1).execute());
                System.out.println(new CommandUsingRequestCache(0).execute());
                System.out.println(new CommandUsingRequestCache(58672).execute());
            } finally {
                context.shutdown();
            }
        }

        @Test
        public void testWithCacheHits() {
            HystrixRequestContext context = HystrixRequestContext.initializeContext();
            try {
                CommandUsingRequestCache command2a = new CommandUsingRequestCache(2);
                CommandUsingRequestCache command2b = new CommandUsingRequestCache(2);

                System.out.println(command2a.execute());
                // this is the first time we've executed this command with the value of "2" so it should not be from cache
                System.out.println(command2a.isResponseFromCache());

                System.out.println(command2b.execute());
                // this is the second time we've executed this command with the same value so it should return from cache
                System.out.println(command2b.isResponseFromCache());
            } finally {
                context.shutdown();
            }

            // start a new request context
            context = HystrixRequestContext.initializeContext();
            try {
                CommandUsingRequestCache command3b = new CommandUsingRequestCache(2);
                System.out.println(command3b.execute());
                // this is a new request context so this should not come from cache
                System.out.println(command3b.isResponseFromCache());
            } finally {
                context.shutdown();
            }
        }
    }

}
