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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * 一个 {@link org.springframework.beans.factory.FactoryBean} 实现，
 * 返回一个值，该值是 {@link org.springframework.beans.factory.ObjectFactory}，
 * 而后者又返回来源于 {@link org.springframework.beans.factory.BeanFactory} 的bean。
 *
 * <p>因此，这可用于避免客户端对象直接调用
 * {@link org.springframework.beans.factory.BeanFactory#getBean(String)}
 * 从 {@link org.springframework.beans.factory.BeanFactory} 获取
 * （通常是原型）bean，这会违反控制反转原则。相反，通过使用此类，
 * 客户端对象可以被馈送一个 {@link org.springframework.beans.factory.ObjectFactory}
 * 实例作为属性，该实例直接返回唯一的目标bean（同样，通常是原型bean）。
 *
 * <p>在基于XML的 {@link org.springframework.beans.factory.BeanFactory}
 * 中的示例配置可能如下所示：
 *
 * <pre class="code">&lt;beans&gt;
 *
 *   &lt;!-- 原型bean，因为我们有状态 --&gt;
 *   &lt;bean id="myService" class="a.b.c.MyService" scope="prototype"/&gt;
 *
 *   &lt;bean id="myServiceFactory"
 *       class="org.springframework.beans.factory.config.ObjectFactoryCreatingFactoryBean"&gt;
 *     &lt;property name="targetBeanName"&gt;&lt;idref local="myService"/&gt;&lt;/property&gt;
 *   &lt;/bean&gt;
 *
 *   &lt;bean id="clientBean" class="a.b.c.MyClientBean"&gt;
 *     &lt;property name="myServiceFactory" ref="myServiceFactory"/&gt;
 *   &lt;/bean&gt;
 *
 *&lt;/beans&gt;</pre>
 *
 * <p>相应的 {@code MyClientBean} 类实现可能如下所示：
 *
 * <pre class="code">package a.b.c;
 *
 * import org.springframework.beans.factory.ObjectFactory;
 *
 * public class MyClientBean {
 *
 *   private ObjectFactory&lt;MyService&gt; myServiceFactory;
 *
 *   public void setMyServiceFactory(ObjectFactory&lt;MyService&gt; myServiceFactory) {
 *     this.myServiceFactory = myServiceFactory;
 *   }
 *
 *   public void someBusinessMethod() {
 *     // 获取一个"新鲜的"、全新的MyService实例
 *     MyService service = this.myServiceFactory.getObject();
 *     // 使用service对象来执行业务逻辑...
 *   }
 * }</pre>
 *
 * <p>对象创建模式的替代方法是使用 {@link ServiceLocatorFactoryBean}
 * 来获取（原型）bean。{@link ServiceLocatorFactoryBean} 方法的优点是
 * 不需要依赖任何Spring特定的接口，如 {@link org.springframework.beans.factory.ObjectFactory}，
 * 但缺点是需要运行时类生成。请参考
 * {@link ServiceLocatorFactoryBean ServiceLocatorFactoryBean JavaDoc}
 * 以更全面地讨论此问题。
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 1.0.2
 * @see org.springframework.beans.factory.ObjectFactory
 * @see ServiceLocatorFactoryBean
 */
public class ObjectFactoryCreatingFactoryBean extends AbstractFactoryBean<ObjectFactory<Object>> {

	@Nullable
	private String targetBeanName;


	/**
	 * 设置目标bean的名称。
	 * <p>目标<i>不必</i>是非单例bean，但实际上通常都是
	 * （因为如果目标bean是单例，那么该单例bean可以直接注入到依赖对象中，
	 * 从而避免了这种工厂方法提供的额外间接层的需要）。
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(this.targetBeanName, "Property 'targetBeanName' is required");
		super.afterPropertiesSet();
	}


	@Override
	public Class<?> getObjectType() {
		return ObjectFactory.class;
	}

	@Override
	protected ObjectFactory<Object> createInstance() {
		BeanFactory beanFactory = getBeanFactory();
		Assert.state(beanFactory != null, "No BeanFactory available");
		Assert.state(this.targetBeanName != null, "No target bean name specified");
		return new TargetBeanObjectFactory(beanFactory, this.targetBeanName);
	}


	/**
	 * 独立的内部类-用于序列化目的。
	 */
	@SuppressWarnings("serial")
	private static class TargetBeanObjectFactory implements ObjectFactory<Object>, Serializable {

		private final BeanFactory beanFactory;

		private final String targetBeanName;

		public TargetBeanObjectFactory(BeanFactory beanFactory, String targetBeanName) {
			this.beanFactory = beanFactory;
			this.targetBeanName = targetBeanName;
		}

		@Override
		public Object getObject() throws BeansException {
			return this.beanFactory.getBean(this.targetBeanName);
		}
	}

}
