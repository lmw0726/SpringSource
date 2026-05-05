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

package org.springframework.aop.support;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.DynamicIntroductionAdvice;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * {@link org.springframework.aop.IntroductionInterceptor} 接口的便捷实现。
 *
 * <p>此类与 {@link DelegatingIntroductionInterceptor} 的不同之处在于，
 * 此类的单个实例可用于通知多个目标对象，并且每个目标对象都会拥有其<i>自己的</i>委托
 * （而 DelegatingIntroductionInterceptor 在所有目标之间共享同一个委托，
 * 因而也共享同一状态）。
 *
 * <p>{@code suppressInterface} 方法可用于抑制委托类实现但不应引介到所属
 * AOP 代理的接口。
 *
 * <p>如果委托可序列化，则此类的实例也可序列化。
 *
 * <p><i>注意：此类与 {@link DelegatingIntroductionInterceptor} 之间存在一些实现相似性，
 * 这表明将来可能会重构以提取公共祖先类。</i>
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 2.0
 * @see #suppressInterface
 * @see DelegatingIntroductionInterceptor
 */
@SuppressWarnings("serial")
public class DelegatePerTargetObjectIntroductionInterceptor extends IntroductionInfoSupport
		implements IntroductionInterceptor {

	/**
	 * 持有键的弱引用，因为我们不希望干扰垃圾回收。
	 */
	private final Map<Object, Object> delegateMap = new WeakHashMap<>();

	private Class<?> defaultImplType;

	private Class<?> interfaceType;


	public DelegatePerTargetObjectIntroductionInterceptor(Class<?> defaultImplType, Class<?> interfaceType) {
		this.defaultImplType = defaultImplType;
		this.interfaceType = interfaceType;
		// 现在创建一个新委托（但不将其存储在映射中）。
		// 我们这样做有两个原因：
		// 1) 如果实例化委托存在问题，则尽早失败
		// 2) 只填充一次接口映射
		Object delegate = createNewDelegate();
		implementInterfacesOnObject(delegate);
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
		// 判断当前调用的方法是否属于“引介接口”（Introduction 接口）
		if (isMethodOnIntroducedInterface(mi)) {
			// 根据当前目标对象，获取对应的引介实现对象
			Object delegate = getIntroductionDelegateFor(mi.getThis());

			// 使用以下方法而不是直接反射，
			// 可以在被引介方法抛出异常时，
			// 正确处理 InvocationTargetException。
			Object retVal = AopUtils.invokeJoinpointUsingReflection(delegate, mi.getMethod(), mi.getArguments());

			// 如果可能，调整返回值：如果委托返回了自身，
			// 我们真正想返回的是代理。
			// 如果返回值就是 delegate 本身，并且当前调用是代理调用
			if (retVal == delegate && mi instanceof ProxyMethodInvocation) {
				// 获取代理对象，并替换返回值
				retVal = ((ProxyMethodInvocation) mi).getProxy();
			}
			// 返回最终结果
			return retVal;
		}
		// 如果不是引介接口方法，则走正常的拦截器链流程
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

	private Object getIntroductionDelegateFor(@Nullable Object targetObject) {
		synchronized (this.delegateMap) {
			if (this.delegateMap.containsKey(targetObject)) {
				return this.delegateMap.get(targetObject);
			}
			else {
				Object delegate = createNewDelegate();
				this.delegateMap.put(targetObject, delegate);
				return delegate;
			}
		}
	}

	private Object createNewDelegate() {
		try {
			return ReflectionUtils.accessibleConstructor(this.defaultImplType).newInstance();
		}
		catch (Throwable ex) {
			throw new IllegalArgumentException("Cannot create default implementation for '" +
					this.interfaceType.getName() + "' mixin (" + this.defaultImplType.getName() + "): " + ex);
		}
	}

}
