package top.ybq87;

import java.util.Arrays;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;

/**
 *
 * @author smlz
 * @date 2019/6/10
 */
@Aspect
@Order
public class LogAspect {
    
    @Pointcut("execution(* top.ybq87.LybqCalculate.*(..))")
    public void pointCut() {
    }
    
    @Before(value = "pointCut()")
    public void methodBefore(JoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        System.out.println("执行目标方法【" + methodName + "】的<前置通知>,入参" + Arrays.asList(joinPoint.getArgs()));
    }
    
    @After(value = "pointCut()")
    public void methodAfter(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        System.out.println("执行目标方法【" + methodName + "】的<后置通知>,入参" + Arrays.asList(joinPoint.getArgs()));
    }
    
    @AfterReturning(value = "pointCut()", returning = "result")
    public void methodReturning(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        System.out.println("执行目标方法【" + methodName + "】的<返回通知>,入参" + Arrays.asList(joinPoint.getArgs()) + ";返回值：" + result);
    }
    
    @AfterThrowing(value = "pointCut()")
    public void methodAfterThrowing(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        System.out.println("执行目标方法【" + methodName + "】的<异常通知>,入参" + Arrays.asList(joinPoint.getArgs()));
    }
    
}
