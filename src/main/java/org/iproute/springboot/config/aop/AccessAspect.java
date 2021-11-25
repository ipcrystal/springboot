package org.iproute.springboot.config.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * ControllerAop
 *
 * @author winterfell
 * @since 2021/11/25
 */
@Component
@Aspect
@Slf4j
public class AccessAspect {

    @Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping)")
    public void getMappingPointcut() {
    }

    @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping)")
    public void postMappingPoint() {
    }

    @Around("getMappingPointcut()")
    public Object beforeGetMapping(ProceedingJoinPoint joinPoint) throws Throwable {
        return statistic(joinPoint);
    }

    @Around("postMappingPoint()")
    public Object beforePostMapping(ProceedingJoinPoint joinPoint) throws Throwable {
        return statistic(joinPoint);
    }

    private Object statistic(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        String name = joinPoint.getTarget().getClass().getSimpleName();
        String functionName = joinPoint.getSignature().getName();

        Object[] args = joinPoint.getArgs();
        Object res = null;
        try {
            res = joinPoint.proceed();
        } finally {
            long take = System.currentTimeMillis() - start;
            log.info("{}|{}|{}ms", name, functionName, take);
        }
        return res;
    }

}
