/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.scheduling.support;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.support.ArgumentConvertingMethodInvoker;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * 适配器，将 {@link Runnable} 接口实现为基于 Spring MethodInvoker 的可配置方法调用。
 *
 * <p>继承 {@link org.springframework.util.MethodInvoker} 的通用配置属性。
 *
 * @author Juergen Hoeller
 * @since 1.2.4
 * @see java.util.concurrent.Executor#execute(Runnable)
 */
public class MethodInvokingRunnable extends ArgumentConvertingMethodInvoker
		implements Runnable, BeanClassLoaderAware, InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	protected Class<?> resolveClassName(String className) throws ClassNotFoundException {
		return ClassUtils.forName(className, this.beanClassLoader);
	}

	@Override
	public void afterPropertiesSet() throws ClassNotFoundException, NoSuchMethodException {
		prepare();
	}


	@Override
	public void run() {
		try {
			invoke();
		}
		catch (InvocationTargetException ex) {
			logger.error(getInvocationFailureMessage(), ex.getTargetException());
			// 不要抛出异常，否则调度器的主循环可能会停止！
		}
		catch (Throwable ex) {
			logger.error(getInvocationFailureMessage(), ex);
			// 不要抛出异常，否则调度器的主循环可能会停止！
		}
	}

	/**
	 * 构建调用失败异常的错误消息。
	 * @return 错误消息，包含目标方法名称等信息
	 */
	protected String getInvocationFailureMessage() {
		return "Invocation of method '" + getTargetMethod() +
				"' on target class [" + getTargetClass() + "] failed";
	}

}
