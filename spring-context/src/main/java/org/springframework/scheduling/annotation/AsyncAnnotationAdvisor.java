/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.scheduling.annotation;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.SingletonSupplier;

/**
 * 通过 {@link Async} 注解激活异步方法执行的顾问（Advisor）。
 * 此注解可在实现类以及代理接口的方法和类型级别上使用。
 *
 * <p>此顾问还会检测 EJB 3.1 {@code javax.ejb.Asynchronous} 注解，
 * 并将其与 Spring 自身的 {@code Async} 注解同等对待。
 * 此外，可以通过 {@link #setAsyncAnnotationType "asyncAnnotationType"} 属性指定自定义的异步注解类型。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see Async
 * @see AnnotationAsyncExecutionInterceptor
 */
@SuppressWarnings("serial")
public class AsyncAnnotationAdvisor extends AbstractPointcutAdvisor implements BeanFactoryAware {

	private Advice advice;

	private Pointcut pointcut;


	/**
	 * 创建一个新的 {@code AsyncAnnotationAdvisor}，用于 bean 风格的配置。
	 */
	public AsyncAnnotationAdvisor() {
		this((Supplier<Executor>) null, (Supplier<AsyncUncaughtExceptionHandler>) null);
	}

	/**
	 * 创建一个新的 {@code AsyncAnnotationAdvisor}，使用给定的任务执行器。
	 * @param executor 用于异步方法的任务执行器
	 * （可以为 {@code null} 以触发默认执行器解析）
	 * @param exceptionHandler 用于处理异步方法执行所抛出的意外异常的 {@link AsyncUncaughtExceptionHandler}
	 * @see AnnotationAsyncExecutionInterceptor#getDefaultExecutor(BeanFactory)
	 */
	public AsyncAnnotationAdvisor(
			@Nullable Executor executor, @Nullable AsyncUncaughtExceptionHandler exceptionHandler) {

		this(SingletonSupplier.ofNullable(executor), SingletonSupplier.ofNullable(exceptionHandler));
	}

	/**
	 * 创建一个新的 {@code AsyncAnnotationAdvisor}，使用给定的任务执行器。
	 * @param executor 用于异步方法的任务执行器
	 * （可以为 {@code null} 以触发默认执行器解析）
	 * @param exceptionHandler 用于处理异步方法执行所抛出的意外异常的 {@link AsyncUncaughtExceptionHandler}
	 * @since 5.1
	 * @see AnnotationAsyncExecutionInterceptor#getDefaultExecutor(BeanFactory)
	 */
	@SuppressWarnings("unchecked")
	public AsyncAnnotationAdvisor(
			@Nullable Supplier<Executor> executor, @Nullable Supplier<AsyncUncaughtExceptionHandler> exceptionHandler) {

		Set<Class<? extends Annotation>> asyncAnnotationTypes = new LinkedHashSet<>(2);
		asyncAnnotationTypes.add(Async.class);
		try {
			asyncAnnotationTypes.add((Class<? extends Annotation>)
					ClassUtils.forName("javax.ejb.Asynchronous", AsyncAnnotationAdvisor.class.getClassLoader()));
		}
		catch (ClassNotFoundException ex) {
			// 如果 EJB 3.1 API 不存在，则简单忽略。
		}
		this.advice = buildAdvice(executor, exceptionHandler);
		this.pointcut = buildPointcut(asyncAnnotationTypes);
	}


	/**
	 * 设置 'async' 注解类型。
	 * <p>默认的异步注解类型是 {@link Async} 注解，
	 * 以及 EJB 3.1 {@code javax.ejb.Asynchronous} 注解（如果存在）。
	 * <p>此 setter 属性的存在是为了让开发者可以提供自己的
	 * （非 Spring 特定的）注解类型来指示方法应异步执行。
	 * @param asyncAnnotationType 期望的注解类型
	 */
	public void setAsyncAnnotationType(Class<? extends Annotation> asyncAnnotationType) {
		Assert.notNull(asyncAnnotationType, "'asyncAnnotationType' must not be null");
		Set<Class<? extends Annotation>> asyncAnnotationTypes = new HashSet<>();
		asyncAnnotationTypes.add(asyncAnnotationType);
		this.pointcut = buildPointcut(asyncAnnotationTypes);
	}

	/**
	 * 设置在通过限定符（qualifier）查找执行器时使用的 {@code BeanFactory}。
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (this.advice instanceof BeanFactoryAware) {
			((BeanFactoryAware) this.advice).setBeanFactory(beanFactory);
		}
	}


	@Override
	public Advice getAdvice() {
		return this.advice;
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}


	protected Advice buildAdvice(
			@Nullable Supplier<Executor> executor, @Nullable Supplier<AsyncUncaughtExceptionHandler> exceptionHandler) {

		AnnotationAsyncExecutionInterceptor interceptor = new AnnotationAsyncExecutionInterceptor(null);
		interceptor.configure(executor, exceptionHandler);
		return interceptor;
	}

	/**
	 * 为给定的异步注解类型计算切入点（Pointcut）（如果有的话）。
	 * @param asyncAnnotationTypes 需要内省的异步注解类型
	 * @return 适用的 Pointcut 对象，如果没有则返回 {@code null}
	 */
	protected Pointcut buildPointcut(Set<Class<? extends Annotation>> asyncAnnotationTypes) {
		ComposablePointcut result = null;
		for (Class<? extends Annotation> asyncAnnotationType : asyncAnnotationTypes) {
			Pointcut cpc = new AnnotationMatchingPointcut(asyncAnnotationType, true);
			Pointcut mpc = new AnnotationMatchingPointcut(null, asyncAnnotationType, true);
			if (result == null) {
				result = new ComposablePointcut(cpc);
			}
			else {
				result.union(cpc);
			}
			result = result.union(mpc);
		}
		return (result != null ? result : Pointcut.TRUE);
	}

}
