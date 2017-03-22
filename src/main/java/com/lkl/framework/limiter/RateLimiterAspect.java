package com.lkl.framework.limiter;

import com.lkl.framework.annotations.RateLimit;
import com.lkl.framework.limiter.handler.RejectedExecutionHandler;
import com.lkl.framework.type.CustomType;
import com.lkl.framework.type.Type;
import com.google.common.util.concurrent.RateLimiter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by liaokailin on 16/8/29.
 */

@Component
@Aspect
public class RateLimiterAspect implements EnvironmentAware {

    public static final Logger LOGGER = LoggerFactory.getLogger(RateLimiterAspect.class);
    public static final String DEFAULT_RATE = "com.lkl.framework.limiter.default.rate";

    public static final String DELIMITER = ".";


    private Environment environment;

    private static final ConcurrentHashMap<String, RateLimiter> rateLimiters = new ConcurrentHashMap<String, RateLimiter>();

    @Pointcut("@annotation(rateLimit)")
    private void ratePointCut(RateLimit rateLimit) {
    }

    /**
     * 限流名称
     */
    class Naming {
        private String annotationOrmethodName;  //注解名或者方法名
        private String cusName;  //自定义名称
        private String finalName; //最终名称

        public String get() {
            return this.finalName;
        }

        /**
         * 按照限流名获取配置的速率
         *
         * @return
         */
        public double rate() {
            double rate = 0;

            try {
                String rateStr = environment.getProperty(this.get() + DELIMITER + "rate");
                if (StringUtils.isEmpty(rateStr)) {
                    rateStr = environment.getProperty(DEFAULT_RATE);
                    if (StringUtils.isEmpty(rateStr)) {
                        LOGGER.warn("no config about {} rate.", this.get());
                        rateStr = "0";
                    }
                }

                rate = Double.valueOf(rateStr);
            } catch (Exception e) {
                LOGGER.error("obtain rate value of " + this.get() + " occur exception.", e);
            }
            return rate;
        }

        public Naming(Object target, Method method, RateLimit rateLimit) {
            annotationOrmethodName = rateLimit.name();
            if (StringUtils.isEmpty(annotationOrmethodName)) {
                annotationOrmethodName = target.getClass().getName() + DELIMITER + method.getName();
            }
            this.cusName = calCusName(rateLimit);

            this.finalName = StringUtils.isEmpty(this.cusName) ? this.annotationOrmethodName : this.annotationOrmethodName + DELIMITER + this.cusName;
        }

        /**
         * 解析自定义的名称
         *
         * @param rateLimit
         * @return
         */
        private String calCusName(RateLimit rateLimit) {
            Class<? extends Type> type = rateLimit.type();

            if (Type.DefaultMethodType.class.isAssignableFrom(type)) {
                // do nothing
            } else if (CustomType.class.isAssignableFrom(type)) {
                try {
                    CustomType ct = (CustomType) type.newInstance();
                    return ct.name();
                } catch (Exception e) {
                    LOGGER.error("create instance of " + type.getName() + " occur exception.", e);
                }
            } else {
                throw new IllegalArgumentException("illegal rate limit type be assigned.");
            }

            return "";

        }
    }


    @Around("ratePointCut(rateLimit)")
    public Object rateLimiterAround(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {


        Object result = null;

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        Naming name = new Naming(joinPoint.getTarget(), method, rateLimit);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} invoke aop for rate limiter.", name.get());
        }

        RateLimiter rateLimiter = createAndGetRateLimiter(rateLimit, name);

        if (rateLimiter == null) {
            return joinPoint.proceed();
        }

        if (RateLimit.Strategy.reject == rateLimit.strategy()) {

            if (rateLimiter.tryAcquire(rateLimit.timeOut(), TimeUnit.MILLISECONDS)) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("{} acquire permit ,will invoke target.", name.get());
                }

                result = joinPoint.proceed();
            } else {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("{} can't acquire permit ,will invoke reject handle.", name.get());
                }

                Class<? extends RejectedExecutionHandler> clazz = rateLimit.rejectedExecutionHandler();
                RejectedExecutionHandler handler = null;

                try {
                    handler = clazz.newInstance();
                } catch (Exception e) {
                    LOGGER.error("create instance of " + clazz.getName() + " occur exception.", e);
                }
                if (handler != null) {
                    RateContext.Builder builder = new RateContext.Builder();
                    builder.name(name.get()).target(joinPoint.getTarget()).method(method).args(joinPoint.getArgs()).rateLimit(rateLimit).rateLimiter(rateLimiter).joinPoint(joinPoint);
                    handler.rejected(builder.getContext());
                }
            }
        } else {
            rateLimiter.acquire(); //may wait
            result = joinPoint.proceed();
        }
        return result;
    }

    private RateLimiter createAndGetRateLimiter(RateLimit rateLimit, Naming name) {
        try {

            double rate = name.rate();
            if (rate <= 0) {
                return null;
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{} rate limit {}qps", name.get(), rate);
            }

            RateLimiter rateLimiter = rateLimiters.get(name.get());

            if (rateLimiter == null) {

                if (rateLimit.warmupPeriod() > 0) {
                    rateLimiter = RateLimiter.create(rate, rateLimit.warmupPeriod(), TimeUnit.MILLISECONDS);
                } else {
                    rateLimiter = RateLimiter.create(rate);
                }
                RateLimiter oldRateLimiter = rateLimiters.putIfAbsent(name.get(), rateLimiter);
                if (oldRateLimiter != null) {
                    rateLimiter = oldRateLimiter;
                }
            }

            if (Math.abs(rateLimiter.getRate()) - rate > 0.1) {  // 允许误差 setRate(30)后 getRate()可能为会返回29.99999996
                LOGGER.info("{} rate changed,reset rate {}qps,the old rate is {}qps .", name.get(), rate, rateLimiter.getRate());
                rateLimiter.setRate(rate);
            }
            return rateLimiter;

        } catch (Exception e) {
            LOGGER.error(name.get() + " create or get RateLimiter object occur exception.", e);
            return null;
        }
    }


    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

}