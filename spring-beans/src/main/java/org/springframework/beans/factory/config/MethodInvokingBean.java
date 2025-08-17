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

package org.springframework.beans.factory.config;

import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.support.ArgumentConvertingMethodInvoker;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * 简单的方法调用 Bean：仅调用目标方法，而不期望将结果暴露给容器
 * （与 {@link MethodInvokingFactoryBean} 相对）。
 *
 * <p>此调用器支持任意类型的目标方法。可以通过将 {@link #setTargetMethod targetMethod}
 * 属性设置为静态方法名（字符串），并通过 {@link #setTargetClass targetClass} 指定定义该静态方法的类，
 * 来调用静态方法。或者，也可以调用实例方法：通过将 {@link #setTargetObject targetObject} 属性
 * 设置为目标对象，并将 {@link #setTargetMethod targetMethod} 属性设置为要调用的方法名。
 * 方法调用所需的参数可以通过 {@link #setArguments arguments} 属性进行指定。
 *
 * <p>该类依赖 {@link #afterPropertiesSet()} 在所有属性设置完成后被调用，
 * 遵循 InitializingBean 契约。
 *
 * <p>以下是一个示例（在基于 XML 的 BeanFactory 定义中），使用此类来调用静态初始化方法：
 *
 * <pre class="code">
 * &lt;bean id="myObject" class="org.springframework.beans.factory.config.MethodInvokingBean"&gt;
 *   &lt;property name="staticMethod" value="com.whatever.MyClass.init"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>以下是一个调用实例方法来启动某个服务器 Bean 的示例：
 *
 * <pre class="code">
 * &lt;bean id="myStarter" class="org.springframework.beans.factory.config.MethodInvokingBean"&gt;
 *   &lt;property name="targetObject" ref="myServer"/&gt;
 *   &lt;property name="targetMethod" value="start"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Juergen Hoeller
 * @since 4.0.3
 * @see MethodInvokingFactoryBean
 * @see org.springframework.util.MethodInvoker
 */
public class MethodInvokingBean extends ArgumentConvertingMethodInvoker
		implements BeanClassLoaderAware, BeanFactoryAware, InitializingBean {

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	@Nullable
	private ConfigurableBeanFactory beanFactory;


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	protected Class<?> resolveClassName(String className) throws ClassNotFoundException {
		return ClassUtils.forName(className, this.beanClassLoader);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableBeanFactory) {
			this.beanFactory = (ConfigurableBeanFactory) beanFactory;
		}
	}

	/**
	 * 如果可能的话，从此bean运行所在的BeanFactory中获取TypeConverter。
	 * @see ConfigurableBeanFactory#getTypeConverter()
	 */
	@Override
	protected TypeConverter getDefaultTypeConverter() {
		if (this.beanFactory != null) {
			return this.beanFactory.getTypeConverter();
		}
		else {
			return super.getDefaultTypeConverter();
		}
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		prepare();
		invokeWithTargetException();
	}

	/**
	 * 执行调用并将 InvocationTargetException 转换为
	 * 底层目标异常。
	 */
	@Nullable
	protected Object invokeWithTargetException() throws Exception {
		try {
			return invoke();
		}
		catch (InvocationTargetException ex) {
			if (ex.getTargetException() instanceof Exception) {
				throw (Exception) ex.getTargetException();
			}
			if (ex.getTargetException() instanceof Error) {
				throw (Error) ex.getTargetException();
			}
			throw ex;
		}
	}

}
