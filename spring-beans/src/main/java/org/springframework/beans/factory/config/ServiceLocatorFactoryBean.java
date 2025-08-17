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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Properties;
/**
 * 一个{@link FactoryBean}实现，接受一个接口，该接口必须具有一个或多个
 * 带有{@code MyType xxx()}或{@code MyType xxx(MyIdType id)}签名的方法
 * （通常是{@code MyService getService()}或{@code MyService getService(String id)}），
 * 并创建实现该接口的动态代理，委托给底层的
 * {@link org.springframework.beans.factory.BeanFactory}。
 *
 * <p>这样的服务定位器允许调用代码与
 * {@link org.springframework.beans.factory.BeanFactory} API解耦，
 * 通过使用适当的自定义定位器接口。它们通常用于
 * <b>原型bean</b>，即应该为每次调用返回新实例的工厂方法。
 * 客户端通过setter或构造函数注入接收服务定位器的引用，
 * 以便能够按需调用定位器的工厂方法。<b>对于单例bean，
 * 直接setter或构造函数注入目标bean更可取。</b>
 *
 * <p>在调用无参工厂方法时，或使用{@code null}或空字符串的String id
 * 调用单参工厂方法时，如果工厂中恰好有<b>一个</b>bean匹配工厂方法的返回类型，
 * 则返回该bean，否则抛出
 * {@link org.springframework.beans.factory.NoSuchBeanDefinitionException}。
 *
 * <p>在使用非null（且非空）参数调用单参工厂方法时，
 * 代理返回{@link org.springframework.beans.factory.BeanFactory#getBean(String)}
 * 调用的结果，使用传入id的字符串化版本作为bean名称。
 *
 * <p>工厂方法参数通常是String，但也可以是int或自定义枚举类型，
 * 例如，通过{@code toString}字符串化。结果字符串可以直接用作bean名称，
 * 前提是在bean工厂中定义了相应的bean。或者，可以定义服务ID和bean名称之间的
 * {@linkplain #setServiceMappings(java.util.Properties) 自定义映射}。
 *
 * <p>举例来说，考虑以下服务定位器接口。
 * 注意此接口不依赖任何Spring API。
 *
 * <pre class="code">package a.b.c;
 *
 *public interface ServiceFactory {
 *
 *    public MyService getService();
 *}</pre>
 *
 * <p>在基于XML的{@link org.springframework.beans.factory.BeanFactory}中的
 * 示例配置可能如下所示：
 *
 * <pre class="code">&lt;beans&gt;
 *
 *   &lt;!-- 原型bean，因为我们有状态 --&gt;
 *   &lt;bean id="myService" class="a.b.c.MyService" singleton="false"/&gt;
 *
 *   &lt;!-- 将通过*类型*查找上面的'myService' bean --&gt;
 *   &lt;bean id="myServiceFactory"
 *            class="org.springframework.beans.factory.config.ServiceLocatorFactoryBean"&gt;
 *     &lt;property name="serviceLocatorInterface" value="a.b.c.ServiceFactory"/&gt;
 *   &lt;/bean&gt;
 *
 *   &lt;bean id="clientBean" class="a.b.c.MyClientBean"&gt;
 *     &lt;property name="myServiceFactory" ref="myServiceFactory"/&gt;
 *   &lt;/bean&gt;
 *
 *&lt;/beans&gt;</pre>
 *
 * <p>相应的{@code MyClientBean}类实现可能如下所示：
 *
 * <pre class="code">package a.b.c;
 *
 *public class MyClientBean {
 *
 *    private ServiceFactory myServiceFactory;
 *
 *    // 实际实现由Spring容器提供
 *    public void setServiceFactory(ServiceFactory myServiceFactory) {
 *        this.myServiceFactory = myServiceFactory;
 *    }
 *
 *    public void someBusinessMethod() {
 *        // 获取一个'全新'的MyService实例
 *        MyService service = this.myServiceFactory.getService();
 *        // 使用服务对象执行业务逻辑...
 *    }
 *}</pre>
 *
 * <p>举例来说，考虑<b>按名称</b>查找bean的以下服务定位器接口。
 * 同样，注意此接口不依赖任何Spring API。
 *
 * <pre class="code">package a.b.c;
 *
 *public interface ServiceFactory {
 *
 *    public MyService getService (String serviceName);
 *}</pre>
 *
 * <p>在基于XML的{@link org.springframework.beans.factory.BeanFactory}中的
 * 示例配置可能如下所示：
 *
 * <pre class="code">&lt;beans&gt;
 *
 *   &lt;!-- 原型bean，因为我们有状态（都扩展MyService） --&gt;
 *   &lt;bean id="specialService" class="a.b.c.SpecialService" singleton="false"/&gt;
 *   &lt;bean id="anotherService" class="a.b.c.AnotherService" singleton="false"/&gt;
 *
 *   &lt;bean id="myServiceFactory"
 *            class="org.springframework.beans.factory.config.ServiceLocatorFactoryBean"&gt;
 *     &lt;property name="serviceLocatorInterface" value="a.b.c.ServiceFactory"/&gt;
 *   &lt;/bean&gt;
 *
 *   &lt;bean id="clientBean" class="a.b.c.MyClientBean"&gt;
 *     &lt;property name="myServiceFactory" ref="myServiceFactory"/&gt;
 *   &lt;/bean&gt;
 *
 *&lt;/beans&gt;</pre>
 *
 * <p>相应的{@code MyClientBean}类实现可能如下所示：
 *
 * <pre class="code">package a.b.c;
 *
 *public class MyClientBean {
 *
 *    private ServiceFactory myServiceFactory;
 *
 *    // 实际实现由Spring容器提供
 *    public void setServiceFactory(ServiceFactory myServiceFactory) {
 *        this.myServiceFactory = myServiceFactory;
 *    }
 *
 *    public void someBusinessMethod() {
 *        // 获取一个'全新'的MyService实例
 *        MyService service = this.myServiceFactory.getService("specialService");
 *        // 使用服务对象执行业务逻辑...
 *    }
 *
 *    public void anotherBusinessMethod() {
 *        // 获取一个'全新'的MyService实例
 *        MyService service = this.myServiceFactory.getService("anotherService");
 *        // 使用服务对象执行业务逻辑...
 *    }
 *}</pre>
 *
 * <p>有关替代方法，请参见{@link ObjectFactoryCreatingFactoryBean}。
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 1.1.4
 * @see #setServiceLocatorInterface
 * @see #setServiceMappings
 * @see ObjectFactoryCreatingFactoryBean
 */
public class ServiceLocatorFactoryBean implements FactoryBean<Object>, BeanFactoryAware, InitializingBean {

	@Nullable
	private Class<?> serviceLocatorInterface;

	@Nullable
	private Constructor<Exception> serviceLocatorExceptionConstructor;

	@Nullable
	private Properties serviceMappings;

	@Nullable
	private ListableBeanFactory beanFactory;

	@Nullable
	private Object proxy;


	/**
	 * 设置要使用的服务定位器接口，该接口必须具有一个或多个带有
	 * {@code MyType xxx()}或{@code MyType xxx(MyIdType id)}签名的方法
	 * （通常是{@code MyService getService()}或{@code MyService getService(String id)}）。
	 * 有关此类方法的语义信息，请参见{@link ServiceLocatorFactoryBean 类级别Javadoc}。
	 */
	public void setServiceLocatorInterface(Class<?> interfaceType) {
		this.serviceLocatorInterface = interfaceType;
	}

	/**
	 * 设置服务定位器在服务查找失败时应抛出的异常类。
	 * 指定的异常类必须具有以下参数类型之一的构造函数：
	 * {@code (String, Throwable)}或{@code (Throwable)}或{@code (String)}。
	 * <p>如果未指定，将抛出Spring的BeansException的子类，
	 * 例如NoSuchBeanDefinitionException。由于这些是非检查异常，
	 * 调用方不需要处理它们，因此只要以通用方式处理Spring异常，
	 * 抛出Spring异常可能是可以接受的。
	 * @see #determineServiceLocatorExceptionConstructor
	 * @see #createServiceLocatorException
	 */
	public void setServiceLocatorExceptionClass(Class<? extends Exception> serviceLocatorExceptionClass) {
		this.serviceLocatorExceptionConstructor =
				determineServiceLocatorExceptionConstructor(serviceLocatorExceptionClass);
	}

	/**
	 * 设置服务ID（传入服务定位器）和bean名称（在bean工厂中）之间的映射。
	 * 未在此处定义的服务ID将按原样视为bean名称。
	 * <p>空字符串作为服务ID键定义了{@code null}和空字符串的映射，
	 * 以及没有参数的工厂方法的映射。如果未定义，
	 * 将从bean工厂检索单个匹配的bean。
	 * @param serviceMappings 服务ID和bean名称之间的映射，
	 * 以服务ID作为键，bean名称作为值
	 */
	public void setServiceMappings(Properties serviceMappings) {
		this.serviceMappings = serviceMappings;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (!(beanFactory instanceof ListableBeanFactory)) {
			throw new FatalBeanException(
					"ServiceLocatorFactoryBean needs to run in a BeanFactory that is a ListableBeanFactory");
		}
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.serviceLocatorInterface == null) {
			throw new IllegalArgumentException("Property 'serviceLocatorInterface' is required");
		}

		// 创建服务定位器代理。
		this.proxy = Proxy.newProxyInstance(
				this.serviceLocatorInterface.getClassLoader(),
				new Class<?>[] {this.serviceLocatorInterface},
				new ServiceLocatorInvocationHandler());
	}


	/**
	 * 确定用于给定服务定位器异常类的构造函数。
	 * 仅在自定义服务定位器异常的情况下调用。
	 * <p>默认实现查找具有以下参数类型之一的构造函数：
	 * {@code (String, Throwable)}或{@code (Throwable)}或{@code (String)}。
	 * @param exceptionClass 异常类
	 * @return 要使用的构造函数
	 * @see #setServiceLocatorExceptionClass
	 */
	@SuppressWarnings("unchecked")
	protected Constructor<Exception> determineServiceLocatorExceptionConstructor(Class<? extends Exception> exceptionClass) {
		try {
			return (Constructor<Exception>) exceptionClass.getConstructor(String.class, Throwable.class);
		}
		catch (NoSuchMethodException ex) {
			try {
				return (Constructor<Exception>) exceptionClass.getConstructor(Throwable.class);
			}
			catch (NoSuchMethodException ex2) {
				try {
					return (Constructor<Exception>) exceptionClass.getConstructor(String.class);
				}
				catch (NoSuchMethodException ex3) {
					throw new IllegalArgumentException(
							"Service locator exception [" + exceptionClass.getName() +
							"] neither has a (String, Throwable) constructor nor a (String) constructor");
				}
			}
		}
	}

	/**
	 * 为给定原因创建服务定位器异常。
	 * 仅在自定义服务定位器异常的情况下调用。
	 * <p>默认实现可以处理消息和异常参数的所有变体。
	 * @param exceptionConstructor 要使用的构造函数
	 * @param cause 服务查找失败的原因
	 * @return 要抛出的服务定位器异常
	 * @see #setServiceLocatorExceptionClass
	 */
	protected Exception createServiceLocatorException(Constructor<Exception> exceptionConstructor, BeansException cause) {
		Class<?>[] paramTypes = exceptionConstructor.getParameterTypes();
		Object[] args = new Object[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			if (String.class == paramTypes[i]) {
				args[i] = cause.getMessage();
			}
			else if (paramTypes[i].isInstance(cause)) {
				args[i] = cause;
			}
		}
		return BeanUtils.instantiateClass(exceptionConstructor, args);
	}


	@Override
	@Nullable
	public Object getObject() {
		return this.proxy;
	}

	@Override
	public Class<?> getObjectType() {
		return this.serviceLocatorInterface;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * 将服务定位器调用委托给bean工厂的调用处理器。
	 */
	private class ServiceLocatorInvocationHandler implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (ReflectionUtils.isEqualsMethod(method)) {
				// 只有当代理相同时才认为相等。
				return (proxy == args[0]);
			}
			else if (ReflectionUtils.isHashCodeMethod(method)) {
				// 使用服务定位器代理的hashCode。
				return System.identityHashCode(proxy);
			}
			else if (ReflectionUtils.isToStringMethod(method)) {
				return "Service locator: " + serviceLocatorInterface;
			}
			else {
				return invokeServiceLocatorMethod(method, args);
			}
		}

		private Object invokeServiceLocatorMethod(Method method, Object[] args) throws Exception {
			Class<?> serviceLocatorMethodReturnType = getServiceLocatorMethodReturnType(method);
			try {
				String beanName = tryGetBeanName(args);
				Assert.state(beanFactory != null, "No BeanFactory available");
				if (StringUtils.hasLength(beanName)) {
					// 特定bean名称的服务定位器
					return beanFactory.getBean(beanName, serviceLocatorMethodReturnType);
				}
				else {
					// bean类型的服务定位器
					return beanFactory.getBean(serviceLocatorMethodReturnType);
				}
			}
			catch (BeansException ex) {
				if (serviceLocatorExceptionConstructor != null) {
					throw createServiceLocatorException(serviceLocatorExceptionConstructor, ex);
				}
				throw ex;
			}
		}

		/**
		 * 检查是否传入了服务ID。
		 */
		private String tryGetBeanName(@Nullable Object[] args) {
			String beanName = "";
			if (args != null && args.length == 1 && args[0] != null) {
				beanName = args[0].toString();
			}
			// 查找明确的serviceId到beanName的映射。
			if (serviceMappings != null) {
				String mappedName = serviceMappings.getProperty(beanName);
				if (mappedName != null) {
					beanName = mappedName;
				}
			}
			return beanName;
		}

		private Class<?> getServiceLocatorMethodReturnType(Method method) throws NoSuchMethodException {
			Assert.state(serviceLocatorInterface != null, "No service locator interface specified");
			Class<?>[] paramTypes = method.getParameterTypes();
			Method interfaceMethod = serviceLocatorInterface.getMethod(method.getName(), paramTypes);
			Class<?> serviceLocatorReturnType = interfaceMethod.getReturnType();

			// 检查方法是否是有效的服务定位器。
			if (paramTypes.length > 1 || void.class == serviceLocatorReturnType) {
				throw new UnsupportedOperationException(
						"May only call methods with signature '<type> xxx()' or '<type> xxx(<idtype> id)' " +
						"on factory interface, but tried to call: " + interfaceMethod);
			}
			return serviceLocatorReturnType;
		}
	}

}
