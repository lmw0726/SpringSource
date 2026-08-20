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

package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 需要感知应用上下文的应用对象的便捷超类，例如用于自定义查找协作 Bean
 * 或访问特定上下文的资源。它保存了应用上下文引用并提供了一个初始化回调方法。
 * 此外，它还提供了众多便捷的消息查找方法。
 *
 * <p>不要求必须继承此类：它只是在需要访问上下文时让事情变得稍微简单一些，
 * 例如访问文件资源或消息源。请注意，许多应用对象根本不需要感知应用上下文，
 * 因为它们可以通过 Bean 引用来接收协作的 Bean。
 *
 * <p>许多框架类都派生自此类，尤其是在 Web 支持中。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.web.context.support.WebApplicationObjectSupport
 */
public abstract class ApplicationObjectSupport implements ApplicationContextAware {

	/** 供子类使用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 此对象运行所在的 ApplicationContext。 */
	@Nullable
	private ApplicationContext applicationContext;

	/** 用于便捷消息访问的 MessageSourceAccessor。 */
	@Nullable
	private MessageSourceAccessor messageSourceAccessor;


	@Override
	public final void setApplicationContext(@Nullable ApplicationContext context) throws BeansException {
		if (context == null && !isContextRequired()) {
			// 重置内部上下文状态。
			this.applicationContext = null;
			this.messageSourceAccessor = null;
		}
		else if (this.applicationContext == null) {
			// 使用传入的上下文进行初始化。
			if (!requiredContextClass().isInstance(context)) {
				throw new ApplicationContextException(
						"Invalid application context: needs to be of type [" + requiredContextClass().getName() + "]");
			}
			this.applicationContext = context;
			this.messageSourceAccessor = new MessageSourceAccessor(context);
			initApplicationContext(context);
		}
		else {
			// 如果传入的是同一个上下文，则忽略重新初始化。
			if (this.applicationContext != context) {
				throw new ApplicationContextException(
						"Cannot reinitialize with different application context: current one is [" +
						this.applicationContext + "], passed-in one is [" + context + "]");
			}
		}
	}

	/**
	 * 确定此应用对象是否需要在 ApplicationContext 中运行。
	 * <p>默认值为 "false"。可以重写以强制在上下文中运行
	 * （即在访问器中，如果不在上下文内则抛出 IllegalStateException）。
	 * @see #getApplicationContext
	 * @see #getMessageSourceAccessor
	 */
	protected boolean isContextRequired() {
		return false;
	}

	/**
	 * 确定传递给 {@code setApplicationContext} 的任何上下文必须是其
	 * 实例的上下文类型。可在子类中重写。
	 * @see #setApplicationContext
	 */
	protected Class<?> requiredContextClass() {
		return ApplicationContext.class;
	}

	/**
	 * 子类可以重写此方法以实现自定义初始化行为。
	 * 在 {@code setApplicationContext} 设置上下文实例后调用。
	 * <p>注意：<i>不会</i>在上下文重新初始化时调用，
	 * 而是仅在此对象的上下文引用首次初始化时调用。
	 * <p>默认实现调用了不带 ApplicationContext 参数的重载方法
	 * {@link #initApplicationContext()}。
	 * @param context 所属的 ApplicationContext
	 * @throws ApplicationContextException 在初始化出错时抛出
	 * @throws BeansException 由 ApplicationContext 方法抛出时
	 * @see #setApplicationContext
	 */
	protected void initApplicationContext(ApplicationContext context) throws BeansException {
		initApplicationContext();
	}

	/**
	 * 子类可以重写此方法以实现自定义初始化行为。
	 * <p>默认实现为空。由
	 * {@link #initApplicationContext(org.springframework.context.ApplicationContext)} 调用。
	 * @throws ApplicationContextException 在初始化出错时抛出
	 * @throws BeansException 由 ApplicationContext 方法抛出时
	 * @see #setApplicationContext
	 */
	protected void initApplicationContext() throws BeansException {
	}


	/**
	 * 返回此对象关联的 ApplicationContext。
	 * @throws IllegalStateException 如果未在 ApplicationContext 中运行
	 */
	@Nullable
	public final ApplicationContext getApplicationContext() throws IllegalStateException {
		if (this.applicationContext == null && isContextRequired()) {
			throw new IllegalStateException(
					"ApplicationObjectSupport instance [" + this + "] does not run in an ApplicationContext");
		}
		return this.applicationContext;
	}

	/**
	 * 获取用于实际使用的 ApplicationContext。
	 * @return ApplicationContext（永远不为 {@code null}）
	 * @throws IllegalStateException 在未设置 ApplicationContext 时抛出
	 * @since 5.0
	 */
	protected final ApplicationContext obtainApplicationContext() {
		ApplicationContext applicationContext = getApplicationContext();
		Assert.state(applicationContext != null, "No ApplicationContext");
		return applicationContext;
	}

	/**
	 * 返回此对象使用的应用上下文对应的 MessageSourceAccessor，
	 * 用于便捷的消息访问。
	 * @throws IllegalStateException 如果未在 ApplicationContext 中运行
	 */
	@Nullable
	protected final MessageSourceAccessor getMessageSourceAccessor() throws IllegalStateException {
		if (this.messageSourceAccessor == null && isContextRequired()) {
			throw new IllegalStateException(
					"ApplicationObjectSupport instance [" + this + "] does not run in an ApplicationContext");
		}
		return this.messageSourceAccessor;
	}

}
