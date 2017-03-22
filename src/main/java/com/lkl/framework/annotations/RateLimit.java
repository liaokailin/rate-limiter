package com.lkl.framework.annotations;


import com.lkl.framework.limiter.handler.RejectedExecutionHandler;
import com.lkl.framework.type.Type;

import java.lang.annotation.*;

/**
 * Created by liaokailin on 16/8/29.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    String name() default ""; //default is (class name + method name)

    Strategy strategy() default Strategy.reject;  //default is reject strategy,if can't acquire permit will reject request

    long timeOut() default 0l; // timeUnit is millisecond ,it work when strategy is reject,if can't acquire permit will wait {timeOut}ms,still can't allowed and reject request.

    long warmupPeriod() default 0l;  // timeUnit is millisecond  the duration of the period where the {@code RateLimiter} ramps up its rate, before reaching its stable (maximum) rate

    /**
     * it work when strategy is reject,implements reject logic.
     */
    Class<? extends RejectedExecutionHandler> rejectedExecutionHandler() default RejectedExecutionHandler.class;


    Class<? extends Type>  type() default  Type.DefaultMethodType.class;


    /**
     * the strategy of resource is limited
     */
    enum Strategy {

        /**
         * if can't acquire permit will wait for accepted
         */
        await,

        /**
         * if can't acquire permit will reject request
         */
        reject
    }




}
