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
 * 被 @Aspect 注解的 bean 就是一个切面，就是一个 Aspect
 * @author smlz
 * @date 2019/6/10
 */
@Aspect
@Order
public class LogAspect {
	
	/**
	 * 被 @Pointcut 注解的，就是一个切点，这个决定了我们要增强哪个方法。
	 * 【springaop 只能增强 springioc 容器管理下的 bean 中的方法。它和 aspectj 还是有区别的，springaop 只是实现了 aspectj 的部分思想】
	 */
	@Pointcut("execution(* top.ybq87.LybqCalculate.*(..))")
    public void pointCut() {
    }
	
	/**
	 * 各种通知，Advise，我们前面定义了切点，就拦截了 bean 的方法，那么要对这些方法做什么呢？就在通知方法这里进行
	 * @param joinPoint
	 * @throws Throwable
	 */
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
    
    // 还有个 Around 方法，这里没有列出
    
}
