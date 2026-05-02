/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.aop.support;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.DynamicIntroductionAdvice;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.aop.IntroductionInterceptor} 接口的便捷实现。
 *
 * <p>子类只需扩展此类并自行实现要引介的接口。
 * 在这种情况下，委托就是子类实例本身。或者，可以由单独的委托实现该接口，
 * 并通过 delegate bean 属性进行设置。
 *
 * <p>委托或子类可以实现任意数量的接口。
 * 默认情况下，除 IntroductionInterceptor 之外的所有接口都会从子类或委托中获取。
 *
 * <p>{@code suppressInterface} 方法可用于抑制委托实现但不应引介到所属
 * AOP 代理的接口。
 *
 * <p>如果委托可序列化，则此类的实例也可序列化。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 16.11.2003
 * @see #suppressInterface
 * @see DelegatePerTargetObjectIntroductionInterceptor
 */
@SuppressWarnings("serial")
public class DelegatingIntroductionInterceptor extends IntroductionInfoSupport
		implements IntroductionInterceptor {

	/**
	 * 实际实现接口的对象。
	 * 如果子类实现了被引介的接口，则可能是 "this"。
	 */
	@Nullable
	private Object delegate;


	/**
	 * 构造新的 DelegatingIntroductionInterceptor，
	 * 提供一个实现要引介接口的委托。
	 * @param delegate 实现被引介接口的委托
	 */
	public DelegatingIntroductionInterceptor(Object delegate) {
		init(delegate);
	}

	/**
	 * 构造新的 DelegatingIntroductionInterceptor。
	 * 委托将是子类，该子类必须实现附加接口。
	 */
	protected DelegatingIntroductionInterceptor() {
		init(this);
	}


	/**
	 * 两个构造函数都使用此 init 方法，
	 * 因为无法从一个构造函数向另一个构造函数传递 "this" 引用。
	 * @param delegate 委托对象
	 */
	private void init(Object delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
		implementInterfacesOnObject(delegate);

		// 我们不希望暴露控制接口
		suppressInterface(IntroductionInterceptor.class);
		suppressInterface(DynamicIntroductionAdvice.class);
	}


	/**
	 * 如果子类希望在环绕通知中执行自定义行为，可能需要重写此方法。
	 * 但是，子类应调用此方法，
	 * 该方法会处理被引介的接口并转发到目标。
	 */
	@Override
	@Nullable
	public Object invoke(MethodInvocation mi) throws Throwable {
		if (isMethodOnIntroducedInterface(mi)) {
			// 使用以下方法而不是直接反射，
			// 可以在被引介方法抛出异常时，
			// 正确处理 InvocationTargetException。
			Object retVal = AopUtils.invokeJoinpointUsingReflection(this.delegate, mi.getMethod(), mi.getArguments());

			// 如果可能，调整返回值：如果委托返回了自身，
			// 我们真正想返回的是代理。
			if (retVal == this.delegate && mi instanceof ProxyMethodInvocation) {
				Object proxy = ((ProxyMethodInvocation) mi).getProxy();
				if (mi.getMethod().getReturnType().isInstance(proxy)) {
					retVal = proxy;
				}
			}
			return retVal;
		}

		return doProceed(mi);
	}

	/**
	 * 使用提供的 {@link org.aopalliance.intercept.MethodInterceptor} 继续执行。
	 * 子类可以重写此方法，以拦截目标对象上的方法调用；
	 * 当引介需要监控被引介到的对象时，这很有用。
	 * 对于被引介接口上的 {@link MethodInvocation MethodInvocations}，
	 * 此方法<strong>永远不会</strong>被调用。
	 */
	@Nullable
	protected Object doProceed(MethodInvocation mi) throws Throwable {
		// 如果执行到这里，只需继续传递该调用。
		return mi.proceed();
	}

}
