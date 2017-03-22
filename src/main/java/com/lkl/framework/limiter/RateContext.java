package com.lkl.framework.limiter;

import com.lkl.framework.annotations.RateLimit;
import com.google.common.util.concurrent.RateLimiter;
import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.reflect.Method;

/**
 * Created by liaokailin on 16/8/29.
 */
public class RateContext {
    private String name ; // rate name ;
    private Object target;
    private Method method ;
    private Object[] args ;
    private RateLimiter rateLimiter;
    private RateLimit rateLimit ;
    private ProceedingJoinPoint joinPoint;


    public static final class Builder{
        private RateContext context = null;
        public Builder(){
            context= new RateContext();
        }

        public RateContext getContext(){
            return this.context;
        }

        public Builder name(String name){
            this.context.name = name ;
            return this ;
        }

        public Builder rateLimit(RateLimit rate){
            this.context.rateLimit = rate ;
            return this ;
        }

        public Builder target(Object target){
            this.context.target = target;
            return this;
        }

        public Builder method(Method method){
            this.context.method = method;
            return this;
        }

        public Builder args(Object[] args){
            this.context.args = args;
            return this;
        }

        public Builder rateLimiter(RateLimiter rateLimiter){
            this.context.rateLimiter = rateLimiter;
            return this;
        }

        public Builder joinPoint(ProceedingJoinPoint joinPoint){
            this.context.joinPoint = joinPoint;
            return this;
        }

    }

    public String getName() {
        return name;
    }

    public Object getTarget() {
        return target;
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArgs() {
        return args;
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public ProceedingJoinPoint getJoinPoint() {
        return joinPoint;
    }
}
