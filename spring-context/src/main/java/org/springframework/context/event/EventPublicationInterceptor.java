/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.context.event;

import java.lang.reflect.Constructor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link MethodInterceptor 拦截器}，在每次<i>成功</i>的方法调用后，
 * 将 {@code ApplicationEvent} 发布到所有通过
 * {@code ApplicationEventPublisher} 注册的 {@code ApplicationListeners}。
 *
 * <p>请注意，此拦截器只能发布通过
 * {@link #setApplicationEventClass "applicationEventClass"} 属性配置的<i>无状态</i>事件。
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @author Rick Evans
 * @see #setApplicationEventClass
 * @see org.springframework.context.ApplicationEvent
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.ApplicationEventPublisher
 * @see org.springframework.context.ApplicationContext
 */
public class EventPublicationInterceptor
		implements MethodInterceptor, ApplicationEventPublisherAware, InitializingBean {

	@Nullable
	private Constructor<?> applicationEventClassConstructor;

	@Nullable
	private ApplicationEventPublisher applicationEventPublisher;


	/**
	 * 设置要发布的应用程序事件类。
	 * <p>事件类<b>必须</b>有一个接受单个 {@code Object} 参数的构造函数，用于指定事件源。拦截器将传入被调用的对象。
	 * @throws IllegalArgumentException 如果提供的 {@code Class} 为 {@code null}，或者不是 {@code ApplicationEvent} 的子类，或者没有接受单个 {@code Object} 参数的构造函数
	 */
	public void setApplicationEventClass(Class<?> applicationEventClass) {
		if (ApplicationEvent.class == applicationEventClass ||
				!ApplicationEvent.class.isAssignableFrom(applicationEventClass)) {
			throw new IllegalArgumentException("'applicationEventClass' needs to extend ApplicationEvent");
		}
		try {
			this.applicationEventClassConstructor = applicationEventClass.getConstructor(Object.class);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalArgumentException("ApplicationEvent class [" +
					applicationEventClass.getName() + "] does not have the required Object constructor: " + ex);
		}
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.applicationEventClassConstructor == null) {
			throw new IllegalArgumentException("Property 'applicationEventClass' is required");
		}
	}


	@Override
	@Nullable
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Object retVal = invocation.proceed();

		Assert.state(this.applicationEventClassConstructor != null, "No ApplicationEvent class set");
		ApplicationEvent event = (ApplicationEvent)
				this.applicationEventClassConstructor.newInstance(invocation.getThis());

		Assert.state(this.applicationEventPublisher != null, "No ApplicationEventPublisher available");
		this.applicationEventPublisher.publishEvent(event);

		return retVal;
	}

}
