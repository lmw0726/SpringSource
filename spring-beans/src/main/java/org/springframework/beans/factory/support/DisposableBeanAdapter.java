/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 一个适配器，实现了 {@link DisposableBean} 和 {@link Runnable} 接口，
 * 用于对给定的 Bean 实例执行一系列销毁步骤：
 * <ul>
 * <li>调用 DestructionAwareBeanPostProcessors 的销毁前处理方法；
 * <li>调用 Bean 自身实现的 {@code DisposableBean.destroy()} 方法；
 * <li>调用 Bean 定义中指定的自定义销毁方法。
 * </ul>
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @see AbstractBeanFactory
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor
 * @see AbstractBeanDefinition#getDestroyMethodName()
 * @since 2.0
 */
@SuppressWarnings("serial")
class DisposableBeanAdapter implements DisposableBean, Runnable, Serializable {

	private static final String DESTROY_METHOD_NAME = "destroy";

	private static final String CLOSE_METHOD_NAME = "close";

	private static final String SHUTDOWN_METHOD_NAME = "shutdown";

	private static final Log logger = LogFactory.getLog(DisposableBeanAdapter.class);


	private final Object bean;

	private final String beanName;

	private final boolean nonPublicAccessAllowed;

	private final boolean invokeDisposableBean;

	private boolean invokeAutoCloseable;

	@Nullable
	private String destroyMethodName;

	@Nullable
	private transient Method destroyMethod;

	@Nullable
	private final List<DestructionAwareBeanPostProcessor> beanPostProcessors;

	@Nullable
	private final AccessControlContext acc;


	/**
	 * 为指定的 bean 创建一个新的 DisposableBeanAdapter。
	 *
	 * @param bean           bean 实例（不允许为 {@code null}）
	 * @param beanName       bean 的名称
	 * @param beanDefinition 合并后的 bean 定义
	 * @param postProcessors BeanPostProcessor 列表（可能包含 DestructionAwareBeanPostProcessor，可以为空）
	 */
	public DisposableBeanAdapter(Object bean, String beanName, RootBeanDefinition beanDefinition,
								 List<DestructionAwareBeanPostProcessor> postProcessors, @Nullable AccessControlContext acc) {

		Assert.notNull(bean, "Disposable bean must not be null");
		this.bean = bean;
		this.beanName = beanName;
		this.nonPublicAccessAllowed = beanDefinition.isNonPublicAccessAllowed();
		this.invokeDisposableBean = (bean instanceof DisposableBean &&
				!beanDefinition.hasAnyExternallyManagedDestroyMethod(DESTROY_METHOD_NAME));

		String destroyMethodName = inferDestroyMethodIfNecessary(bean, beanDefinition);
		if (destroyMethodName != null &&
				!(this.invokeDisposableBean && DESTROY_METHOD_NAME.equals(destroyMethodName)) &&
				!beanDefinition.hasAnyExternallyManagedDestroyMethod(destroyMethodName)) {

			this.invokeAutoCloseable = (bean instanceof AutoCloseable && CLOSE_METHOD_NAME.equals(destroyMethodName));
			if (!this.invokeAutoCloseable) {
				this.destroyMethodName = destroyMethodName;
				Method destroyMethod = determineDestroyMethod(destroyMethodName);
				if (destroyMethod == null) {
					if (beanDefinition.isEnforceDestroyMethod()) {
						throw new BeanDefinitionValidationException("Could not find a destroy method named '" +
								destroyMethodName + "' on bean with name '" + beanName + "'");
					}
				} else {
					if (destroyMethod.getParameterCount() > 0) {
						Class<?>[] paramTypes = destroyMethod.getParameterTypes();
						if (paramTypes.length > 1) {
							throw new BeanDefinitionValidationException("Method '" + destroyMethodName + "' of bean '" +
									beanName + "' has more than one parameter - not supported as destroy method");
						} else if (paramTypes.length == 1 && boolean.class != paramTypes[0]) {
							throw new BeanDefinitionValidationException("Method '" + destroyMethodName + "' of bean '" +
									beanName + "' has a non-boolean parameter - not supported as destroy method");
						}
					}
					destroyMethod = ClassUtils.getInterfaceMethodIfPossible(destroyMethod, bean.getClass());
				}
				this.destroyMethod = destroyMethod;
			}
		}

		this.beanPostProcessors = filterPostProcessors(postProcessors, bean);
		this.acc = acc;
	}

	/**
	 * 为指定的 bean 创建一个新的 DisposableBeanAdapter。
	 *
	 * @param bean           bean 实例（不允许为 {@code null}）
	 * @param postProcessors BeanPostProcessor 列表（可能包含 DestructionAwareBeanPostProcessor，可以为空）
	 */
	public DisposableBeanAdapter(
			Object bean, List<DestructionAwareBeanPostProcessor> postProcessors, AccessControlContext acc) {

		Assert.notNull(bean, "Disposable bean must not be null");
		this.bean = bean;
		this.beanName = bean.getClass().getName();
		this.nonPublicAccessAllowed = true;
		this.invokeDisposableBean = (this.bean instanceof DisposableBean);
		this.beanPostProcessors = filterPostProcessors(postProcessors, bean);
		this.acc = acc;
	}

	/**
	 * 为指定的 bean 创建一个新的 DisposableBeanAdapter。
	 */
	private DisposableBeanAdapter(Object bean, String beanName, boolean nonPublicAccessAllowed,
								  boolean invokeDisposableBean, boolean invokeAutoCloseable, @Nullable String destroyMethodName,
								  @Nullable List<DestructionAwareBeanPostProcessor> postProcessors) {

		this.bean = bean;
		this.beanName = beanName;
		this.nonPublicAccessAllowed = nonPublicAccessAllowed;
		this.invokeDisposableBean = invokeDisposableBean;
		this.invokeAutoCloseable = invokeAutoCloseable;
		this.destroyMethodName = destroyMethodName;
		this.beanPostProcessors = postProcessors;
		this.acc = null;
	}


	@Override
	public void run() {
		destroy();
	}

	@Override
	public void destroy() {
		if (!CollectionUtils.isEmpty(this.beanPostProcessors)) {
			for (DestructionAwareBeanPostProcessor processor : this.beanPostProcessors) {
				processor.postProcessBeforeDestruction(this.bean, this.beanName);
			}
		}

		if (this.invokeDisposableBean) {
			if (logger.isTraceEnabled()) {
				logger.trace("Invoking destroy() on bean with name '" + this.beanName + "'");
			}
			try {
				if (System.getSecurityManager() != null) {
					AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
						((DisposableBean) this.bean).destroy();
						return null;
					}, this.acc);
				} else {
					((DisposableBean) this.bean).destroy();
				}
			} catch (Throwable ex) {
				String msg = "Invocation of destroy method failed on bean with name '" + this.beanName + "'";
				if (logger.isDebugEnabled()) {
					logger.warn(msg, ex);
				} else {
					logger.warn(msg + ": " + ex);
				}
			}
		}

		if (this.invokeAutoCloseable) {
			if (logger.isTraceEnabled()) {
				logger.trace("Invoking close() on bean with name '" + this.beanName + "'");
			}
			try {
				if (System.getSecurityManager() != null) {
					AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
						((AutoCloseable) this.bean).close();
						return null;
					}, this.acc);
				} else {
					((AutoCloseable) this.bean).close();
				}
			} catch (Throwable ex) {
				String msg = "Invocation of close method failed on bean with name '" + this.beanName + "'";
				if (logger.isDebugEnabled()) {
					logger.warn(msg, ex);
				} else {
					logger.warn(msg + ": " + ex);
				}
			}
		} else if (this.destroyMethod != null) {
			invokeCustomDestroyMethod(this.destroyMethod);
		} else if (this.destroyMethodName != null) {
			Method destroyMethod = determineDestroyMethod(this.destroyMethodName);
			if (destroyMethod != null) {
				invokeCustomDestroyMethod(ClassUtils.getInterfaceMethodIfPossible(destroyMethod, this.bean.getClass()));
			}
		}
	}


	@Nullable
	private Method determineDestroyMethod(String name) {
		try {
			if (System.getSecurityManager() != null) {
				return AccessController.doPrivileged((PrivilegedAction<Method>) () -> findDestroyMethod(name));
			} else {
				return findDestroyMethod(name);
			}
		} catch (IllegalArgumentException ex) {
			throw new BeanDefinitionValidationException("Could not find unique destroy method on bean with name '" +
					this.beanName + ": " + ex.getMessage());
		}
	}

	@Nullable
	private Method findDestroyMethod(String name) {
		return (this.nonPublicAccessAllowed ?
				BeanUtils.findMethodWithMinimalParameters(this.bean.getClass(), name) :
				BeanUtils.findMethodWithMinimalParameters(this.bean.getClass().getMethods(), name));
	}

	/**
	 * 在指定的 bean 上调用自定义的销毁方法。
	 * <p>本实现会优先查找无参方法并调用；若未找到，则检查是否存在一个参数类型为 boolean 的方法（传入 "true"，假设为“强制”参数）；
	 * 若均不存在，则记录错误日志。
	 */
	private void invokeCustomDestroyMethod(final Method destroyMethod) {
		int paramCount = destroyMethod.getParameterCount();
		final Object[] args = new Object[paramCount];
		if (paramCount == 1) {
			args[0] = Boolean.TRUE;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Invoking custom destroy method '" + this.destroyMethodName +
					"' on bean with name '" + this.beanName + "'");
		}
		try {
			if (System.getSecurityManager() != null) {
				AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
					ReflectionUtils.makeAccessible(destroyMethod);
					return null;
				});
				try {
					AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () ->
							destroyMethod.invoke(this.bean, args), this.acc);
				} catch (PrivilegedActionException pax) {
					throw (InvocationTargetException) pax.getException();
				}
			} else {
				ReflectionUtils.makeAccessible(destroyMethod);
				destroyMethod.invoke(this.bean, args);
			}
		} catch (InvocationTargetException ex) {
			String msg = "Custom destroy method '" + this.destroyMethodName + "' on bean with name '" +
					this.beanName + "' threw an exception";
			if (logger.isDebugEnabled()) {
				logger.warn(msg, ex.getTargetException());
			} else {
				logger.warn(msg + ": " + ex.getTargetException());
			}
		} catch (Throwable ex) {
			logger.warn("Failed to invoke custom destroy method '" + this.destroyMethodName +
					"' on bean with name '" + this.beanName + "'", ex);
		}
	}


	/**
	 * 序列化此类状态的一个副本，并过滤掉不可序列化的 BeanPostProcessor。
	 */
	protected Object writeReplace() {
		List<DestructionAwareBeanPostProcessor> serializablePostProcessors = null;
		if (this.beanPostProcessors != null) {
			serializablePostProcessors = new ArrayList<>();
			for (DestructionAwareBeanPostProcessor postProcessor : this.beanPostProcessors) {
				if (postProcessor instanceof Serializable) {
					serializablePostProcessors.add(postProcessor);
				}
			}
		}
		return new DisposableBeanAdapter(
				this.bean, this.beanName, this.nonPublicAccessAllowed, this.invokeDisposableBean,
				this.invokeAutoCloseable, this.destroyMethodName, serializablePostProcessors);
	}


	/**
	 * 检查给定的 bean 是否具有任何类型的销毁方法需要调用。
	 *
	 * @param bean           bean 实例
	 * @param beanDefinition 对应的 bean 定义
	 */
	public static boolean hasDestroyMethod(Object bean, RootBeanDefinition beanDefinition) {
		return (bean instanceof DisposableBean || inferDestroyMethodIfNecessary(bean, beanDefinition) != null);
	}


	/**
	 * 如果给定 beanDefinition 的 "destroyMethodName" 属性值为 {@link AbstractBeanDefinition#INFER_METHOD}，
	 * 则尝试推断一个销毁方法。候选方法目前仅限于名为 "close" 或 "shutdown" 的公共无参方法（无论是在本地声明还是继承而来）。
	 * 如果未找到此类方法，则将该 BeanDefinition 的 "destroyMethodName" 设为 null；否则设置为推断出的方法名。
	 * 此常量作为 {@code @Bean#destroyMethod} 注解属性的默认值，也可在 XML 配置中的
	 * {@code <bean destroy-method="">} 或 {@code <beans default-destroy-method="">} 属性中使用。
	 * <p>同时处理 {@link java.io.Closeable} 和 {@link java.lang.AutoCloseable} 接口，
	 * 对实现这些接口的 bean 反射调用其 "close" 方法。
	 */
	@Nullable
	private static String inferDestroyMethodIfNecessary(Object bean, RootBeanDefinition beanDefinition) {
		String destroyMethodName = beanDefinition.resolvedDestroyMethodName;
		if (destroyMethodName == null) {
			destroyMethodName = beanDefinition.getDestroyMethodName();
			boolean autoCloseable = (bean instanceof AutoCloseable);
			if (AbstractBeanDefinition.INFER_METHOD.equals(destroyMethodName) ||
					(destroyMethodName == null && autoCloseable)) {
				// 仅在 bean 未显式实现 DisposableBean 接口时进行销毁方法推断
				destroyMethodName = null;
				if (!(bean instanceof DisposableBean)) {
					if (autoCloseable) {
						destroyMethodName = CLOSE_METHOD_NAME;
					} else {
						try {
							destroyMethodName = bean.getClass().getMethod(CLOSE_METHOD_NAME).getName();
						} catch (NoSuchMethodException ex) {
							try {
								destroyMethodName = bean.getClass().getMethod(SHUTDOWN_METHOD_NAME).getName();
							} catch (NoSuchMethodException ex2) {
								// 未找到候选的销毁方法
							}
						}
					}
				}
			}
			beanDefinition.resolvedDestroyMethodName = (destroyMethodName != null ? destroyMethodName : "");
		}
		return (StringUtils.hasLength(destroyMethodName) ? destroyMethodName : null);
	}

	/**
	 * 检查给定的 bean 是否有适用的、支持销毁操作的后置处理器。
	 *
	 * @param bean           bean 实例
	 * @param postProcessors 后置处理器候选列表
	 */
	public static boolean hasApplicableProcessors(Object bean, List<DestructionAwareBeanPostProcessor> postProcessors) {
		if (!CollectionUtils.isEmpty(postProcessors)) {
			for (DestructionAwareBeanPostProcessor processor : postProcessors) {
				if (processor.requiresDestruction(bean)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 在给定列表中查找所有 DestructionAwareBeanPostProcessor 实例。
	 *
	 * @param processors 要搜索的列表
	 * @return 过滤后的 DestructionAwareBeanPostProcessor 列表
	 */
	@Nullable
	private static List<DestructionAwareBeanPostProcessor> filterPostProcessors(
			List<DestructionAwareBeanPostProcessor> processors, Object bean) {

		List<DestructionAwareBeanPostProcessor> filteredPostProcessors = null;
		if (!CollectionUtils.isEmpty(processors)) {
			filteredPostProcessors = new ArrayList<>(processors.size());
			for (DestructionAwareBeanPostProcessor processor : processors) {
				if (processor.requiresDestruction(bean)) {
					filteredPostProcessors.add(processor);
				}
			}
		}
		return filteredPostProcessors;
	}

}
