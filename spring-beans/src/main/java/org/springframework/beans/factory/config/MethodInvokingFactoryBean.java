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

package org.springframework.beans.factory.config;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.lang.Nullable;

/**
 * {@link FactoryBean} 返回静态或实例方法调用结果的值。
 * 对于大多数用例，最好只使用容器内置的工厂方法支持来达到相同目的，
 * 因为它在转换参数方面更智能。不过，当您需要调用不返回任何值的方法时
 * （例如，静态类方法强制进行某种初始化），这个工厂bean仍然很有用。
 * 工厂方法不支持这种用例，因为需要返回值才能获得bean实例。
 *
 * <p>请注意，由于它预期主要用于访问工厂方法，此工厂默认以<b>单例</b>
 * 方式运行。拥有bean工厂对 {@link #getObject} 的第一次请求将导致方法调用，
 * 其返回值将被缓存以供后续请求使用。可以将内部的
 * {@link #setSingleton singleton} 属性设置为"false"，
 * 使此工厂在每次被请求对象时调用目标方法。
 *
 * <p><b>注意：如果您的目标方法不产生要公开的结果，请考虑使用
 * {@link MethodInvokingBean}，它避免了此 {@link MethodInvokingFactoryBean}
 * 带来的类型确定和生命周期限制。</b>
 *
 * <p>此调用器支持任何类型的目标方法。可以通过将 {@link #setTargetMethod targetMethod}
 * 属性设置为表示静态方法名称的字符串来指定静态方法，
 * 并使用 {@link #setTargetClass targetClass} 指定定义静态方法的类。
 * 或者，可以通过将 {@link #setTargetObject targetObject} 属性设置为目标对象，
 * 并将 {@link #setTargetMethod targetMethod} 属性设置为要在该目标对象上调用的方法名称
 * 来指定目标实例方法。可以通过设置 {@link #setArguments arguments} 属性
 * 来指定方法调用的参数。
 *
 * <p>此类依赖于在设置所有属性后调用 {@link #afterPropertiesSet()}，
 * 根据InitializingBean约定。
 *
 * <p>使用此类调用静态工厂方法的bean定义示例（在基于XML的bean工厂定义中）：
 *
 * <pre class="code">
 * &lt;bean id="myObject" class="org.springframework.beans.factory.config.MethodInvokingFactoryBean"&gt;
 *   &lt;property name="staticMethod" value="com.whatever.MyClassFactory.getInstance"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>调用静态方法然后调用实例方法来获取Java系统属性的示例。
 * 有些冗长，但它可以工作。
 *
 * <pre class="code">
 * &lt;bean id="sysProps" class="org.springframework.beans.factory.config.MethodInvokingFactoryBean"&gt;
 *   &lt;property name="targetClass" value="java.lang.System"/&gt;
 *   &lt;property name="targetMethod" value="getProperties"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="javaVersion" class="org.springframework.beans.factory.config.MethodInvokingFactoryBean"&gt;
 *   &lt;property name="targetObject" ref="sysProps"/&gt;
 *   &lt;property name="targetMethod" value="getProperty"/&gt;
 *   &lt;property name="arguments" value="java.version"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 21.11.2003
 * @see MethodInvokingBean
 * @see org.springframework.util.MethodInvoker
 */
public class MethodInvokingFactoryBean extends MethodInvokingBean implements FactoryBean<Object> {

	private boolean singleton = true;

	private boolean initialized = false;

	/** 单例情况下的方法调用结果。 */
	@Nullable
	private Object singletonObject;


	/**
	 * 设置是否应该创建单例，否则在每次
	 * {@link #getObject()} 请求时创建新对象。默认为"true"。
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		prepare();
		if (this.singleton) {
			this.initialized = true;
			this.singletonObject = invokeWithTargetException();
		}
	}


	/**
	 * 如果singleton属性设置为"true"，则每次返回相同的值，
	 * 否则返回即时调用指定方法返回的值。
	 */
	@Override
	@Nullable
	public Object getObject() throws Exception {
		if (this.singleton) {
			if (!this.initialized) {
				throw new FactoryBeanNotInitializedException();
			}
			// 单例：返回共享对象。
			return this.singletonObject;
		}
		else {
			// 原型：每次调用都创建新对象。
			return invokeWithTargetException();
		}
	}

	/**
	 * 返回此FactoryBean创建的对象类型，
	 * 如果无法提前知道则返回{@code null}。
	 */
	@Override
	public Class<?> getObjectType() {
		if (!isPrepared()) {
			// 尚未完全初始化 -> 返回null表示"尚未知道"。
			return null;
		}
		return getPreparedMethod().getReturnType();
	}

	@Override
	public boolean isSingleton() {
		return this.singleton;
	}

}
