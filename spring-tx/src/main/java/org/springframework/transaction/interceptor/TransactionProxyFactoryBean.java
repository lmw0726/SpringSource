/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.transaction.interceptor;

import java.util.Properties;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.AbstractSingletonProxyFactoryBean;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 用于简化声明式事务处理的代理 factory bean。
 * 这是标准 AOP {@link org.springframework.aop.framework.ProxyFactoryBean}
 * 加单独 {@link TransactionInterceptor} 定义的便捷替代方案。
 *
 * <p><strong>历史说明：</strong>此类最初设计用于覆盖声明式事务边界划分的
 * 典型场景：即使用事务代理包装 singleton 目标对象，代理目标实现的所有接口。
 * 然而，在 Spring 2.0 及更高版本中，此处提供的功能已被更便捷的
 * {@code tx:} XML 命名空间取代。请参阅 Spring 参考文档中的
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/data-access.html#transaction-declarative">声明式事务管理</a>
 * 章节，以了解在 Spring 应用程序中管理事务的现代选项。基于这些原因，
 * <strong>用户应优先使用 {@code tx:} XML 命名空间，以及
 * @{@link org.springframework.transaction.annotation.Transactional Transactional}
 * 和 @{@link org.springframework.transaction.annotation.EnableTransactionManagement
 * EnableTransactionManagement} 注解。</strong>
 *
 * <p>需要指定三个主要属性：
 * <ul>
 * <li>"transactionManager"：要使用的 {@link PlatformTransactionManager} 实现
 * （例如 {@link org.springframework.transaction.jta.JtaTransactionManager} 实例）
 * <li>"target"：应为其创建事务代理的目标对象
 * <li>"transactionAttributes"：按目标方法名称（或方法名称模式）指定的事务属性
 * （例如传播行为和 "readOnly" 标志）
 * </ul>
 *
 * <p>如果未显式设置 "transactionManager" 属性，且此 {@link FactoryBean}
 * 正在 {@link ListableBeanFactory} 中运行，则会从 {@link BeanFactory}
 * 获取单个匹配的 {@link PlatformTransactionManager} 类型 bean。
 *
 * <p>与 {@link TransactionInterceptor} 相比，事务属性以 properties 指定，
 * 方法名称作为键，事务属性描述符作为值。方法名称始终应用于目标类。
 *
 * <p>内部使用 {@link TransactionInterceptor} 实例，但此类用户无需关心。
 * 可选地，可以指定方法 pointcut，以引起底层 {@link TransactionInterceptor}
 * 的条件调用。
 *
 * <p>可以设置 "preInterceptors" 和 "postInterceptors" 属性，
 * 以向组合中添加附加拦截器，例如
 * {@link org.springframework.aop.interceptor.PerformanceMonitorInterceptor}。
 *
 * <p><b>提示：</b>此类常与父/子 bean 定义一起使用。
 * 通常，你会在抽象父 bean 定义中定义事务管理器和默认事务属性
 * （用于方法名称模式），并为特定目标对象派生具体子 bean 定义。
 * 这将每个 bean 的定义工作量降到最低。
 *
 * <pre class="code">
 * &lt;bean id="baseTransactionProxy" class="org.springframework.transaction.interceptor.TransactionProxyFactoryBean"
 *     abstract="true"&gt;
 *   &lt;property name="transactionManager" ref="transactionManager"/&gt;
 *   &lt;property name="transactionAttributes"&gt;
 *     &lt;props&gt;
 *       &lt;prop key="insert*"&gt;PROPAGATION_REQUIRED&lt;/prop&gt;
 *       &lt;prop key="update*"&gt;PROPAGATION_REQUIRED&lt;/prop&gt;
 *       &lt;prop key="*"&gt;PROPAGATION_REQUIRED,readOnly&lt;/prop&gt;
 *     &lt;/props&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="myProxy" parent="baseTransactionProxy"&gt;
 *   &lt;property name="target" ref="myTarget"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="yourProxy" parent="baseTransactionProxy"&gt;
 *   &lt;property name="target" ref="yourTarget"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Juergen Hoeller
 * @author Dmitriy Kopylenko
 * @author Rod Johnson
 * @author Chris Beams
 * @since 21.08.2003
 * @see #setTransactionManager
 * @see #setTarget
 * @see #setTransactionAttributes
 * @see TransactionInterceptor
 * @see org.springframework.aop.framework.ProxyFactoryBean
 */
@SuppressWarnings("serial")
public class TransactionProxyFactoryBean extends AbstractSingletonProxyFactoryBean
		implements BeanFactoryAware {

	private final TransactionInterceptor transactionInterceptor = new TransactionInterceptor();

	@Nullable
	private Pointcut pointcut;


	/**
	 * 设置默认事务管理器。这将执行实际的事务管理：
	 * 此类只是一种调用它的方式。
	 * @see TransactionInterceptor#setTransactionManager
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionInterceptor.setTransactionManager(transactionManager);
	}

	/**
	 * 设置以方法名称为键、事务属性描述符（通过 TransactionAttributeEditor 解析）
	 * 为值的 properties：例如 key = "myMethod"，value = "PROPAGATION_REQUIRED,readOnly"。
	 * <p>注意：无论定义在接口中还是类本身中，方法名称始终应用于目标类。
	 * <p>内部会根据给定 properties 创建 NameMatchTransactionAttributeSource。
	 * @see #setTransactionAttributeSource
	 * @see TransactionInterceptor#setTransactionAttributes
	 * @see TransactionAttributeEditor
	 * @see NameMatchTransactionAttributeSource
	 */
	public void setTransactionAttributes(Properties transactionAttributes) {
		this.transactionInterceptor.setTransactionAttributes(transactionAttributes);
	}

	/**
	 * 设置用于查找事务属性的事务属性源。
	 * 如果指定 String 属性值，则 PropertyEditor
	 * 将根据该值创建 MethodMapTransactionAttributeSource。
	 * @see #setTransactionAttributes
	 * @see TransactionInterceptor#setTransactionAttributeSource
	 * @see TransactionAttributeSourceEditor
	 * @see MethodMapTransactionAttributeSource
	 * @see NameMatchTransactionAttributeSource
	 * @see org.springframework.transaction.annotation.AnnotationTransactionAttributeSource
	 */
	public void setTransactionAttributeSource(TransactionAttributeSource transactionAttributeSource) {
		this.transactionInterceptor.setTransactionAttributeSource(transactionAttributeSource);
	}

	/**
	 * 设置 pointcut，即可以根据传入的方法和属性引起
	 * TransactionInterceptor 条件调用的 bean。
	 * 注意：附加拦截器始终会被调用。
	 * @see #setPreInterceptors
	 * @see #setPostInterceptors
	 */
	public void setPointcut(Pointcut pointcut) {
		this.pointcut = pointcut;
	}

	/**
	 * 此回调是可选的：如果在 BeanFactory 中运行且未显式设置事务管理器，
	 * 则会从 BeanFactory 获取单个匹配的 {@link PlatformTransactionManager} 类型 bean。
	 * @see org.springframework.beans.factory.BeanFactory#getBean(Class)
	 * @see org.springframework.transaction.PlatformTransactionManager
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.transactionInterceptor.setBeanFactory(beanFactory);
	}


	/**
	 * 为此 FactoryBean 的 TransactionInterceptor 创建 advisor。
	 */
	@Override
	protected Object createMainInterceptor() {
		this.transactionInterceptor.afterPropertiesSet();
		if (this.pointcut != null) {
			return new DefaultPointcutAdvisor(this.pointcut, this.transactionInterceptor);
		}
		else {
			// 依赖默认 pointcut。
			return new TransactionAttributeSourceAdvisor(this.transactionInterceptor);
		}
	}

	/**
	 * 自 4.2 起，此方法将 {@link TransactionalProxy} 添加到代理接口集合中，
	 * 以避免重新处理事务元数据。
	 */
	@Override
	protected void postProcessProxyFactory(ProxyFactory proxyFactory) {
		proxyFactory.addInterface(TransactionalProxy.class);
	}

}
