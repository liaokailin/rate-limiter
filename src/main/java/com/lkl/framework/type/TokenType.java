package com.lkl.framework.type;

import com.alibaba.fastjson.JSON;
import org.slf4j.MDC;

import java.util.Map;

/**
 * Created by liaokailin on 16/12/13.
 */
public class TokenType implements CustomType {

    @Override
    public String name() {
        return getClientId();
    }

    public static String getClientId() {
        return JSON.parseObject(MDC.get("jwtClaims").toString(), Map.class).get("clientId").toString();
    }
}
