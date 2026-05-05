package com.lmw.springaop.aspect;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 日志切面
 *
 * @author LaiMingWei
 * @version 1.1.0
 * @since 2026-05-02 22:30
 */
@Aspect
@Component
public class LogAspect {

	@Pointcut("execution(* com.lmw.springaop.service.*.*(..))")
	public void pointcut() {
	}

	@Before("pointcut()")
	public void before() {
		System.out.println("AOP before");
	}

	@After("pointcut()")
	public void after() {
		System.out.println("AOP after");
	}
}
