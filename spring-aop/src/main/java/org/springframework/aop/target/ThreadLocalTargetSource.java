/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.HashSet;
import java.util.Set;

import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.NamedThreadLocal;

/**
 * 对象池的替代方案。此 {@link org.springframework.aop.TargetSource}
 * 使用线程模型，其中每个线程都有自己独立的目标对象副本。
 * 不存在对目标对象的争用。目标对象的创建保持在最低限度，
 * 适用于正在运行的服务器。
 *
 * <p>应用程序代码按普通池的方式编写；调用者不能假设
 * 在不同线程的调用中会处理相同的实例。
 * 但是，在单个线程的操作期间，状态是可以依赖的：
 * 例如，如果一个调用者对 AOP 代理进行多次重复调用。
 *
 * <p>线程绑定对象的清理在 BeanFactory 销毁时执行，
 * 如果可用，会调用它们的 {@code DisposableBean.destroy()} 方法。
 * 请注意，许多线程绑定对象可能会一直存在，直到应用程序实际关闭。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see ThreadLocalTargetSourceStats
 * @see org.springframework.beans.factory.DisposableBean#destroy()
 */
@SuppressWarnings("serial")
public class ThreadLocalTargetSource extends AbstractPrototypeBasedTargetSource
		implements ThreadLocalTargetSourceStats, DisposableBean {

	/**
	 * 持有与当前线程关联的目标对象的 ThreadLocal。
	 * 与大多数 ThreadLocal（通常是静态的）不同，此变量
	 * 意味着每个线程、每个 ThreadLocalTargetSource 类实例各自独立。
	 */
	private final ThreadLocal<Object> targetInThread =
			new NamedThreadLocal<>("Thread-local instance of bean '" + getTargetBeanName() + "'");

	/**
	 * 托管目标的集合，使我们能够跟踪已创建的目标对象。
	 */
	private final Set<Object> targetSet = new HashSet<>();

	private int invocationCount;

	private int hitCount;


	/**
	 * 抽象 getTarget() 方法的实现。
	 * 我们查找保存在 ThreadLocal 中的目标对象。如果没有找到，
	 * 则创建一个并将其绑定到线程。无需同步。
	 */
	@Override
	public Object getTarget() throws BeansException {
		++this.invocationCount;
		Object target = this.targetInThread.get();
		if (target == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("No target for prototype '" + getTargetBeanName() + "' bound to thread: " +
						"creating one and binding it to thread '" + Thread.currentThread().getName() + "'");
			}
			// 将目标对象与 ThreadLocal 关联。
			target = newPrototypeInstance();
			this.targetInThread.set(target);
			synchronized (this.targetSet) {
				this.targetSet.add(target);
			}
		}
		else {
			++this.hitCount;
		}
		return target;
	}

	/**
	 * 必要时销毁目标对象；清除 ThreadLocal。
	 * @see #destroyPrototypeInstance
	 */
	@Override
	public void destroy() {
		logger.debug("Destroying ThreadLocalTargetSource bindings");
		synchronized (this.targetSet) {
			for (Object target : this.targetSet) {
				destroyPrototypeInstance(target);
			}
			this.targetSet.clear();
		}
		// 清除 ThreadLocal，以防万一。
		this.targetInThread.remove();
	}


	@Override
	public int getInvocationCount() {
		return this.invocationCount;
	}

	@Override
	public int getHitCount() {
		return this.hitCount;
	}

	@Override
	public int getObjectCount() {
		synchronized (this.targetSet) {
			return this.targetSet.size();
		}
	}


	/**
	 * 返回一个引入通知 Advisor mixin，允许 AOP 代理被转换为
	 * ThreadLocalInvokerStats。
	 */
	public IntroductionAdvisor getStatsMixin() {
		DelegatingIntroductionInterceptor dii = new DelegatingIntroductionInterceptor(this);
		return new DefaultIntroductionAdvisor(dii, ThreadLocalTargetSourceStats.class);
	}

}
