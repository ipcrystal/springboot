package org.iproute.springboot.config.aop;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 方法 入参、出参 记录
 * <p>
 * 注: 根据此AOP的逻辑， 若注解与表达式同时匹配成功，那么 注解的优先级高于表达式的优先级。
 * <p>
 *
 * <b>特别注意:<b/>这里借助了RecordParametersAdvice的logger来记录其它地方的日志。即: 相当于其它地方将记录日志的动
 * 作委托给RecordParametersAdvice的logger来进行, 所以此logger需要能打印所有地方最下的日志级别(一般为debug)。
 * 即:需要在配置文件中配置<code>logging.level.com.szlaozicl.demo.aop.RecordParametersAdvice=debug</code>
 * 以保证此处有“权限”记录所有用到的日志级别的日志。
 *
 * @date 2019/12/4 13:57
 */
@Slf4j
@Order
@Aspect
@Configuration
@RequiredArgsConstructor
public class RecordParametersAdvice {

    /**
     * 栈帧局部变量表参数名侦查器
     */
    private static final LocalVariableTableParameterNameDiscoverer PARAMETER_NAME_DISCOVER = new LocalVariableTableParameterNameDiscoverer();

    /**
     * 无返回值
     */
    private static final String VOID_STR = void.class.getName();

    /**
     * 判断是否是controller类的后缀
     */
    private static final String CONTROLLER_STR = "Controller";

    private final AopSupport aopSupport;

    /**
     * 【@within】: 当将注解加在类上时，等同于 在该类下的所有方法上加上了该注解(即:该类的所有方法都会被aop)。
     * 注意:注解必须写在类上，不能写在接口上。
     * 【@annotation】: 当将注解加在某个方法上时，该方法会被aop。
     * 【execution】: 这里:
     * 第一个*, 匹配所有返回类型
     * 第二个..*，匹配com.szlaozicl.demo.controller包下的，所有的类(含其子孙包下的类)
     * 最后的*(..), 匹配任意方法任意参数。
     */
    @Pointcut(
            "("
                    + "@within(org.iproute.springboot.config.aop.RecordParameters)"
                    + " || "
                    + "@annotation(org.iproute.springboot.config.aop.RecordParameters)"
                    + " || "
                    + "execution(* org.iproute.springboot.controller..*.*(..))"
                    + ")"
                    + " && "
                    + "!@annotation(org.iproute.springboot.config.aop.IgnoreRecordParameters)"
    )
    public void executeAdvice() {
    }

    /**
     * 环绕增强
     */
    @Around("executeAdvice()")
    public Object aroundAdvice(ProceedingJoinPoint thisJoinPoint) throws Throwable {
        // 获取目标Class
        Object targetObj = thisJoinPoint.getTarget();
        Class<?> targetClazz = targetObj.getClass();
        String clazzName = targetClazz.getName();
        // 获取目标method
        MethodSignature methodSignature = (MethodSignature) thisJoinPoint.getSignature();
        Method targetMethod = methodSignature.getMethod();
        // 获取目标annotation
        RecordParameters annotation = targetMethod.getAnnotation(RecordParameters.class);
        if (annotation == null) {
            annotation = targetClazz.getAnnotation(RecordParameters.class);
            // 如果是通过execution触发的，那么annotation可能为null, 那么给其赋予默认值即可
            if (annotation == null && clazzName.endsWith(CONTROLLER_STR)) {
                annotation = (RecordParameters) AnnotationUtils.getDefaultValue(RecordParameters.class);
            }
        }
        // 是否需要记录入参、出参
        boolean shouldRecordInputParams;
        boolean shouldRecordOutputParams;
        RecordParameters.LogLevel logLevel;
        boolean isControllerMethod;
        if (annotation != null) {
            shouldRecordInputParams = annotation.strategy() == RecordParameters.Strategy.INPUT
                    ||
                    annotation.strategy() == RecordParameters.Strategy.INPUT_OUTPUT;
            shouldRecordOutputParams = annotation.strategy() == RecordParameters.Strategy.OUTPUT
                    ||
                    annotation.strategy() == RecordParameters.Strategy.INPUT_OUTPUT;
            logLevel = annotation.logLevel();
            isControllerMethod = clazzName.endsWith(CONTROLLER_STR);
            // 此时，若annotation仍然为null, 那说明是通过execution(* com.szlaozicl.demo.controller.*.*(..)触发切面的
        } else {
            shouldRecordInputParams = shouldRecordOutputParams = true;
            logLevel = RecordParameters.LogLevel.INFO;
            isControllerMethod = true;
        }
        final String classMethodInfo = "Class#Method → " + clazzName + "#" + targetMethod.getName();

        if (shouldRecordInputParams) {
            preHandle(thisJoinPoint, logLevel, targetMethod, classMethodInfo, isControllerMethod);
        }
        Object obj = thisJoinPoint.proceed();
        if (shouldRecordOutputParams) {
            postHandle(logLevel, targetMethod, obj, classMethodInfo, isControllerMethod);

        }
        return obj;
    }

    /**
     * 前处理切面日志
     *
     * @param pjp                目标方法的返回结果
     * @param logLevel           日志级别
     * @param targetMethod       目标方法
     * @param classMethodInfo    目标类#方法
     * @param isControllerMethod 是否是controller类中的方法
     */
    private void preHandle(ProceedingJoinPoint pjp, RecordParameters.LogLevel logLevel,
                           Method targetMethod, String classMethodInfo, boolean isControllerMethod) {
        StringBuilder sb = new StringBuilder(64);
        sb.append("\n【the way in】");
        if (isControllerMethod) {
            sb.append("request-path[").append(aopSupport.getRequestPath()).append("] ");
        }
        sb.append(classMethodInfo);
        Object[] parameterValues = pjp.getArgs();
        if (parameterValues != null && parameterValues.length > 0) {
            String[] parameterNames = PARAMETER_NAME_DISCOVER.getParameterNames(targetMethod);
            if (parameterNames == null) {
                throw new RuntimeException("parameterNames must not be null!");
            }
            sb.append(", with parameters ↓↓");
            int iterationTimes = parameterValues.length;
            for (int i = 0; i < iterationTimes; i++) {
                sb.append("\n\t").append(parameterNames[i]).append(" => ").append(aopSupport.jsonPretty(parameterValues[i]));
                if (i == iterationTimes - 1) {
                    sb.append("\n");
                }
            }
        } else {
            sb.append(", without any parameters");
        }
        aopSupport.log(logLevel, sb.toString());
    }

    /**
     * 后处理切面日志
     *
     * @param logLevel           日志级别
     * @param targetMethod       目标方法
     * @param obj                目标方法的返回结果
     * @param classMethodInfo    目标类#方法
     * @param isControllerMethod 是否是controller类中的方法
     */
    private void postHandle(RecordParameters.LogLevel logLevel, Method targetMethod,
                            Object obj, String classMethodInfo, boolean isControllerMethod) {
        StringBuilder sb = new StringBuilder(64);
        sb.append("\n【the way out】");
        if (isControllerMethod) {
            sb.append("request-path[").append(aopSupport.getRequestPath()).append("] ");
        }
        sb.append(classMethodInfo);
        Class<?> returnClass = targetMethod.getReturnType();
        sb.append("\n\treturn type → ").append(targetMethod.getReturnType());
        if (!VOID_STR.equals(returnClass.getName())) {
            sb.append("\n\treturn result → ").append(aopSupport.jsonPretty(obj));
        }
        sb.append("\n");
        aopSupport.log(logLevel, sb.toString());
    }

    @Component
    static class AopSupport {

        private static Class<?> logClass = log.getClass();

        private static Map<String, Method> methodMap = new ConcurrentHashMap<>(8);

        @PostConstruct
        private void init() throws NoSuchMethodException {
            String debugStr = RecordParameters.LogLevel.DEBUG.name();
            String infoStr = RecordParameters.LogLevel.INFO.name();
            String warnStr = RecordParameters.LogLevel.WARN.name();
            Method debugMethod = logClass.getMethod(debugStr.toLowerCase(), String.class, Object.class);
            Method infoMethod = logClass.getMethod(infoStr.toLowerCase(), String.class, Object.class);
            Method warnMethod = logClass.getMethod(warnStr.toLowerCase(), String.class, Object.class);
            methodMap.put(debugStr, debugMethod);
            methodMap.put(infoStr, infoMethod);
            methodMap.put(warnStr, warnMethod);
        }

        /**
         * 记录日志
         *
         * @param logLevel    要记录的日志的级别
         * @param markerValue formatter中占位符的值
         */
        private void log(RecordParameters.LogLevel logLevel, Object markerValue) {
            try {
                methodMap.get(logLevel.name()).invoke(log, "{}", markerValue);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("RecordParametersAdvice$AopSupport#log occur error!", e);
            }
        }

        /**
         * Returns a string representation of the given object in a pretty JSON format.
         *
         * @param obj the object to be converted to a JSON string
         * @return a JSON string representation of the given object
         */
        String jsonPretty(Object obj) {
            return JSON.toJSONString(obj);
        }

        /**
         * 获取请求path
         *
         * @return 请求的path
         * @date 2020/4/10 17:13:06
         */
        String getRequestPath() {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes == null) {
                log.warn("obtain request-path is empty");
                return "";
            }
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            return request.getRequestURI();
        }
    }

}