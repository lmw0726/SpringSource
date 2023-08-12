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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Simple template superclass for {@link FactoryBean} implementations that
 * creates a singleton or a prototype object, depending on a flag.
 *
 * <p>If the "singleton" flag is {@code true} (the default),
 * this class will create the object that it creates exactly once
 * on initialization and subsequently return said singleton instance
 * on all calls to the {@link #getObject()} method.
 *
 * <p>Else, this class will create a new instance every time the
 * {@link #getObject()} method is invoked. Subclasses are responsible
 * for implementing the abstract {@link #createInstance()} template
 * method to actually create the object(s) to expose.
 *
 * @param <T> the bean type
 * @author Juergen Hoeller
 * @author Keith Donald
 * @see #setSingleton
 * @see #createInstance()
 * @since 1.0.2
 */
public abstract class AbstractFactoryBean<T>
		implements FactoryBean<T>, BeanClassLoaderAware, BeanFactoryAware, InitializingBean, DisposableBean {

	/**
	 * 记录器可用于子类。
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 是否是单例
	 */
	private boolean singleton = true;

	/**
	 * bean的类加载器
	 */
	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	/**
	 * bean工厂
	 */
	@Nullable
	private BeanFactory beanFactory;

	/**
	 * 是否初始化过该bean
	 */
	private boolean initialized = false;

	/**
	 * 单例对象
	 */
	@Nullable
	private T singletonInstance;

	/**
	 * 早期单例对象
	 */
	@Nullable
	private T earlySingletonInstance;


	/**
	 * 设置是否应创建单例，否则应在每个请求上创建新对象。默认值为 {@code true} (单例)。
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	@Override
	public boolean isSingleton() {
		return this.singleton;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void setBeanFactory(@Nullable BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * 返回此bean运行的BeanFactory。
	 */
	@Nullable
	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * 从该bean运行的BeanFactory获取bean类型转换器。
	 * 这通常是每个调用的新实例，因为类型转换器通常是 <i> 不是 </i> 线程安全的。
	 * <p> 当不在BeanFactory中运行时，会退回到SimpleTypeConverter。
	 *
	 * @see ConfigurableBeanFactory#getTypeConverter()
	 * @see org.springframework.beans.SimpleTypeConverter
	 */
	protected TypeConverter getBeanTypeConverter() {
		//获取bean工厂
		BeanFactory beanFactory = getBeanFactory();
		if (beanFactory instanceof ConfigurableBeanFactory) {
			//如果是在可配置的bean工厂中运行，通过bean工厂获取他的类型转换器
			return ((ConfigurableBeanFactory) beanFactory).getTypeConverter();
		} else {
			//否则返回简单的类型转换器
			return new SimpleTypeConverter();
		}
	}

	/**
	 * 如有必要，急切地创建单例实例。
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		if (isSingleton()) {
			//如果是单例，将已初始化设置为true
			this.initialized = true;
			//创建单例
			this.singletonInstance = createInstance();
			//早期单例对象设置为空
			this.earlySingletonInstance = null;
		}
	}


	/**
	 * 暴露单例实例或创建新的原型实例。
	 *
	 * @see #createInstance()
	 * @see #getEarlySingletonInterfaces()
	 */
	@Override
	public final T getObject() throws Exception {
		if (isSingleton()) {
			//如果是单例，如果该bean已经初始化过，直接返回单例对象，否则获取早期单例实例。
			return (this.initialized ? this.singletonInstance : getEarlySingletonInstance());
		} else {
			//否则，创建一个实例
			return createInstance();
		}
	}

	/**
	 * 确定一个 “早期单例” 实例，在循环引用的情况下暴露。在非循环场景中不调用。
	 */
	@SuppressWarnings("unchecked")
	private T getEarlySingletonInstance() throws Exception {
		Class<?>[] ifcs = getEarlySingletonInterfaces();
		if (ifcs == null) {
			//没有实现的接口，抛出异常
			throw new FactoryBeanNotInitializedException(
					getClass().getName() + " does not support circular references");
		}
		if (this.earlySingletonInstance == null) {
			//早期单例对象为空， 通过 JDK 的动态代理生成代理对象。
			this.earlySingletonInstance = (T) Proxy.newProxyInstance(
					this.beanClassLoader, ifcs, new EarlySingletonInvocationHandler());
		}
		return this.earlySingletonInstance;
	}

	/**
	 * 公开单例实例 (用于通过 “早期单例” 代理访问)。
	 *
	 * @return 此FactoryBean持有的单例实例
	 * @throws IllegalStateException 如果单例实例未初始化
	 */
	@Nullable
	private T getSingletonInstance() throws IllegalStateException {
		Assert.state(this.initialized, "Singleton instance not initialized yet");
		return this.singletonInstance;
	}

	/**
	 * 销毁单例实例 (如果有)。
	 *
	 * @see #destroyInstance(Object)
	 */
	@Override
	public void destroy() throws Exception {
		if (isSingleton()) {
			destroyInstance(this.singletonInstance);
		}
	}


	/**
	 * 此抽象方法声明在FactoryBean接口中复制该方法，以提供一致的抽象模板方法。
	 *
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	@Override
	@Nullable
	public abstract Class<?> getObjectType();

	/**
	 * 子类必须覆盖的模板方法，以构造此工厂返回的对象。
	 * <p> 在单例的情况下，在初始化此FactoryBean时调用; 否则，在每个 {@link #getObject()} 调用中。
	 *
	 * @return 这家工厂返回的对象
	 * @throws Exception 如果在创建对象期间发生异常
	 * @see #getObject()
	 */
	protected abstract T createInstance() throws Exception;

	/**
	 * 返回由该FactoryBean公开的单例对象应该实现的接口数组，以与在循环引用的情况下将公开的 “早期单例代理” 一起使用。
	 * <p> 默认实现返回此FactoryBean的对象类型，前提是它是一个接口，否则返回 {@code null}。
	 * 后者表示此FactoryBean不支持早期单例访问。这将导致抛出一个FactoryBeanNotInitializedException。
	 *
	 * @return 用于 “早期单例” 的接口，
	 * 或 {@code null} 来指示一个FactoryBeanNotInitializedException
	 * @see org.springframework.beans.factory.FactoryBeanNotInitializedException
	 */
	@Nullable
	protected Class<?>[] getEarlySingletonInterfaces() {
		//获取他的所属类型
		Class<?> type = getObjectType();
		//如果所属类型不为空，且该类型是一个接口，则将该类型构建成Class数组返回，否则返回null
		return (type != null && type.isInterface() ? new Class<?>[]{type} : null);
	}

	/**
	 * 用于销毁单例实例的回调。子类可能会覆盖此以销毁先前创建的实例。
	 * <p> 默认实现为空。
	 *
	 * @param instance {@link #createInstance()} 返回的单例实例
	 * @throws Exception 在关机错误的情况下
	 * @see #createInstance()
	 */
	protected void destroyInstance(@Nullable T instance) throws Exception {
	}


	/**
	 * 反射式的 InvocationHandler用于对实际单例对象的惰性访问。
	 */
	private class EarlySingletonInvocationHandler implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (ReflectionUtils.isEqualsMethod(method)) {
				//如果是equals方法，确定它的参数，是否与代理对象相同。
				return (proxy == args[0]);
			} else if (ReflectionUtils.isHashCodeMethod(method)) {
				//如果是hashcode方法，获取代理对象的hashcode。
				return System.identityHashCode(proxy);
			} else if (!initialized && ReflectionUtils.isToStringMethod(method)) {
				//如果还未初始化，且方法是toString方法，返回一段字符串描述。
				return "Early singleton proxy for interfaces " +
						ObjectUtils.nullSafeToString(getEarlySingletonInterfaces());
			}
			try {
				//反射调用该实例的方法。
				return method.invoke(getSingletonInstance(), args);
			} catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
