package com.lkl.framework.limiter.handler;

import com.lkl.framework.exceptions.RateDefaultRejectException;
import com.lkl.framework.limiter.RateContext;

/**
 * Created by liaokailin on 16/8/29.
 */
public interface RejectedExecutionHandler {


    void rejected(RateContext context);


    /**
     * default handle
     * Created by liaokailin on 16/8/25.
     */
     class DefaultRejectedExecutionHandlerDefaultRejectedExecutionHandler implements RejectedExecutionHandler {

        @Override
        public void rejected(RateContext context) {
            throw new RateDefaultRejectException("current rate limit "+context.getRateLimiter().getRate()+"qps, request method " + context.getMethod().getName()+" was rejected.");
        }
    }
}
