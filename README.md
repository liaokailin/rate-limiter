# 应用级限流

## 快速开始

引入jar
```
<dependency>
  <groupId>com.lkl.framework</groupId>
  <artifactId>rate-limiter</artifactId>
  <version>0.0.3-SNAPSHOT</version>
</dependency>
```

在需要限流的方法上加注解`@RateLimit`,通过`类的全限定名称`+`.方法名`+`.rate`配置速率，单位为`qps`,例如

```java
package com.lkl.framework.services.controller;
@RestController 
public class HelloWorldController {
		@RateLimit()
		public void sayHello(){
			System.out.println("hello rate limiter");

		}
	}

```

在配置文件中配置 `com.lkl.framework.services.controller.HelloWorldController.sayHello.rate=10` 表示该`sayHello()`方法限流为10qps


ps: `com.lkl.framework.limiter.default.rate` 可以设置全局默认速率



## 配置

`@RateLimit`注解定义为

``` java

/**
 * Created by liaokailin on 16/8/29.
 */
watchdog@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    public String name()  default "" ; //default is (class name + method name)

    public Strategy strategy() default Strategy.reject;  //default is reject strategy,if can't acquire permit will reject request

    public long timeOut() default 0l; // timeUnit is millisecond ,it work when strategy is reject,if can't acquire permit will wait {timeOut}ms,still can't allowed and reject request.

    public long warmupPeriod() default 0l ;  // timeUnit is millisecond  the duration of the period where the {@code RateLimiter} ramps up its rate, before reaching its stable (maximum) rate

    /**
     * it work when strategy is reject,implements reject logic.
     */
    public Class<? extends RejectedExecutionHandler> rejectedExecutionHandler() default RejectedExecutionHandler.DefaultRejectedExecutionHandler.class;

    /**
     * the strategy of resource is limited
     */
    public enum Strategy{

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
```


配置项             | 默认             |含义                                           |
-------------------|------------------|-----------------------------------------------|
name               |类名+"."+方法名   |指定该注解名称，通过`name`+`.rate`配置限流速率 |
strategy	   |Strategy.reject   |当速率过快时处理请求的策略;   `Strategy.reject`表示直接拒绝请求, `Strategy.await`表示等待接受请求 |
timeOut            |0                 |当策略为`Strategy.reject`生效，等待timeOut时间后拒绝请求，单位毫秒 |
rejectedExecutionHandler | RejectedExecutionHandler.DefaultRejectedExecutionHandler.class  | 当策略为`Strategy.reject`生效,请求被拒绝后处理策略，默认抛出`RateDefaultRejectException`异常，可通过实现`RejectedExecutionHandler`自定义拒绝的处理逻辑 |
warmupPeriod       |0                 |指定达到稳定速率时的预热时间,单位毫秒                 |


## 注意事项

`@RateLimit`注解基于`spring aop`实现，因此在使用该注解会受到`spring aop`的约束，例如：

1.`spring aop`不能拦截非`public`的方法，因此在如下代码上使用`@RateLimit`**无效**

``` java

@RateLimit()
private void sayHello(){
   // some logic...
}

```

2.`spring aop`不能拦截同一类中方法调用的**另外一个方法**上

``` java

public class Sample{

   public void foo(){
     this.bar(); // call inside another method  
   }

   @RateLimit()
   public void bar(){
       // some logic... 
   }

}

```
在上面代码中`bar()`上的注解无效，由于`spring aop`是围绕`bean`的，因此可以将`bar()`定义到另外一个`bean`中。

以上两种情况希望能规避.





# 0.0.3 版本新增功能说明


在注解`RateLimit`中新增属性`type` 用依标注该限流类型

* type=Type.DefaultMethodType.class(默认值)时表示对整个方法进行限流

* type=CustomType.class或其子类时表示对该方法进行自定义的限流


`type=Type.DefaultMethodType.class`用以兼容老版本的功能,自定义类型进行拓展

其中`CustomType.java`实现为:

```java

/**
 * 自定义限流类型
 * Created by liaokailin on 16/12/13.
 */
public interface CustomType extends Type {

    /**
     *
     * @return 返回值用来区分自定义的限流策略,例如:按service token限流时 可返回clientId
     */
    String name();
}
```

假设在0.0.3以前版本配置速度方式为`old.pattern.rate=xxx`(该内容在前面的文档已说明)，则新版本`配置速率`方式为

* type=Type.DefaultMethodType.class,保持老的方式不变

* type=CustomType.class或其子类时,配置方式为`old.pattern.{0}.rate=xxx`  其中`{0}`占位参数用`CustomType`中的`name()`方法返回结果填充

`com.lkl.framework.limiter.default.rate` 仍可设置全局默认速率


## 来个例子比较直观

对方法A按照不同的clientId进行限流，则可利用`service token`去获取`clientId` 然后针对不同的`clientId`去配置速率即可

增加`TokenType.java`  该类`name()`返回`clientId`

``` java

public class TokenType implements CustomType {

    @Override
    public String name() {
        return getClientId();
    }

    public static String getClientId() {
        return JSON.parseObject(MDC.get("jwtClaims").toString(), Map.class).get("clientId").toString();
    }
}

```

在方法上使用

``` java

package com.lkl.framework.services.controller;

public class ToolsTestController {
  @RateLimit( type = TokenType.class)
    public void testRate() {
        //do something
    }
}
```

在consul使用如下配置:

```
com.lkl.framework.service.limiter.default.rate=20

com.lkl.framework.services.controller.ToolsTestController.testRate.security-client.rate=4

com.lkl.framework.services.controller.ToolsTestController.testRate.message-manager.rate=10

```

该配置表示全局速率为20qps, clientId为`security-client`限流4qps,`message-manager`限流10qps .