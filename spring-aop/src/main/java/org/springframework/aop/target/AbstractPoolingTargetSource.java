/*
 * Copyright 2002-2021 the original author or authors.
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

import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.lang.Nullable;

/**
 * 池化 {@link org.springframework.aop.TargetSource} 实现的抽象基类，
 * 维护一个目标实例池，为每次方法调用从池中获取并释放目标对象。
 * 此抽象基类独立于具体的池化技术；
 * 参见子类 {@link CommonsPool2TargetSource} 以了解具体示例。
 *
 * <p>子类必须基于其选择的对象池实现 {@link #getTarget} 和
 * {@link #releaseTarget} 方法。
 * 从 {@link AbstractPrototypeBasedTargetSource} 继承的
 * {@link #newPrototypeInstance()} 方法可以用于创建对象以放入池中。
 *
 * <p>子类还必须实现 {@link PoolingConfig} 接口中的一些监控方法。
 * {@link #getPoolingConfigMixin()} 方法通过引入通知 Advisors 使这些统计信息
 * 在代理对象上可用。
 *
 * <p>此类实现了 {@link org.springframework.beans.factory.DisposableBean} 接口，
 * 以强制子类实现 {@link #destroy()} 方法，关闭其对象池。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getTarget
 * @see #releaseTarget
 * @see #destroy
 */
@SuppressWarnings("serial")
public abstract class AbstractPoolingTargetSource extends AbstractPrototypeBasedTargetSource
		implements PoolingConfig, DisposableBean {

	/** 池的最大大小。 */
	private int maxSize = -1;


	/**
	 * 设置池的最大大小。
	 * 默认值为 -1，表示无大小限制。
	 */
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	/**
	 * 返回池的最大大小。
	 */
	@Override
	public int getMaxSize() {
		return this.maxSize;
	}


	@Override
	public final void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		super.setBeanFactory(beanFactory);
		try {
			createPool();
		}
		catch (Throwable ex) {
			throw new BeanInitializationException("Could not create instance pool for TargetSource", ex);
		}
	}


	/**
	 * 创建池。
	 * @throws Exception 以避免对池化 API 施加约束
	 */
	protected abstract void createPool() throws Exception;

	/**
	 * 从池中获取一个对象。
	 * @return 来自池的对象
	 * @throws Exception 我们可能需要处理池 API 中的受检异常，
	 * 因此我们在异常签名方面保持宽松
	 */
	@Override
	@Nullable
	public abstract Object getTarget() throws Exception;

	/**
	 * 将给定的对象返回到池中。
	 * @param target 必须是通过 {@code getTarget()} 从池中获取的对象
	 * @throws Exception 允许池化 API 抛出异常
	 * @see #getTarget
	 */
	@Override
	public abstract void releaseTarget(Object target) throws Exception;


	/**
	 * 返回一个引入通知 Advisor，它提供一个 mixin，
	 * 用于暴露此对象维护的池的统计信息。
	 */
	public DefaultIntroductionAdvisor getPoolingConfigMixin() {
		DelegatingIntroductionInterceptor dii = new DelegatingIntroductionInterceptor(this);
		return new DefaultIntroductionAdvisor(dii, PoolingConfig.class);
	}

}
