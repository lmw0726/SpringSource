/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.aop.target;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.aop.TargetSource} 的实现，
 * 延迟从 {@link org.springframework.beans.factory.BeanFactory} 中获取单例 Bean。
 *
 * <p>适用于在初始化时需要代理引用，但实际目标对象应在首次使用时才初始化的场景。
 * 当目标 Bean 定义在 {@link org.springframework.context.ApplicationContext}
 * （或一个会提前预实例化单例 Bean 的 {@code BeanFactory}）中时，
 * 它也必须标记为 "lazy-init"，否则会在 {@code ApplicationContext}
 * （或 {@code BeanFactory}）启动时被实例化。
 * <p>例如：
 *
 * <pre class="code">
 * &lt;bean id="serviceTarget" class="example.MyService" lazy-init="true"&gt;
 *   ...
 * &lt;/bean&gt;
 *
 * &lt;bean id="service" class="org.springframework.aop.framework.ProxyFactoryBean"&gt;
 *   &lt;property name="targetSource"&gt;
 *     &lt;bean class="org.springframework.aop.target.LazyInitTargetSource"&gt;
 *       &lt;property name="targetBeanName"&gt;&lt;idref local="serviceTarget"/&gt;&lt;/property&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * 直到 "service" 代理上的方法被调用之前，"serviceTarget" Bean 都不会被初始化。
 *
 * <p>子类可以扩展此类并重写 {@link #postProcessTargetObject(Object)} 方法，
 * 以便在目标对象首次加载时对其执行一些额外的处理。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 1.1.4
 * @see org.springframework.beans.factory.BeanFactory#getBean
 * @see #postProcessTargetObject
 */
@SuppressWarnings("serial")
public class LazyInitTargetSource extends AbstractBeanFactoryBasedTargetSource {

	@Nullable
	private Object target;


	@Override
	@Nullable
	public synchronized Object getTarget() throws BeansException {
		if (this.target == null) {
			this.target = getBeanFactory().getBean(getTargetBeanName());
			postProcessTargetObject(this.target);
		}
		return this.target;
	}

	/**
	 * 子类可以重写此方法，以便在目标对象首次加载时对其执行额外的处理。
	 * @param targetObject 刚刚被实例化（和配置）的目标对象
	 */
	protected void postProcessTargetObject(Object targetObject) {
	}

}
