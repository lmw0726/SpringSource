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

package org.springframework.beans.factory;

/**
 * {@link FactoryBean}接口的扩展。实现类可以指示它们是否总是返回独立实例，
 * 用于处理其{@link #isSingleton()}实现返回{@code false}但不能清楚表明
 * 是否为独立实例的情况。
 *
 * <p>未实现此扩展接口的普通{@link FactoryBean}实现，如果其{@link #isSingleton()}
 * 实现返回{@code false}，则简单地假定总是返回独立实例；暴露的对象只在需要时访问。
 *
 * <p><b>注意：</b>此接口是一个特殊用途的接口，主要用于框架内部使用以及
 * 协作框架内使用。通常情况下，应用程序提供的FactoryBean应该简单地实现
 * 普通的{@link FactoryBean}接口。即使在修订版本中，也可能会向此扩展接口
 * 添加新方法。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @param <T> bean类型
 * @see #isPrototype()
 * @see #isSingleton()
 */
public interface SmartFactoryBean<T> extends FactoryBean<T> {

	/**
	 * 此工厂管理的对象是否为原型？也就是说，{@link #getObject()}是否总是
	 * 返回一个独立的实例？
	 * <p>FactoryBean本身的原型状态通常由拥有它的{@link BeanFactory}提供；
	 * 通常，它必须在那里定义为单例。
	 * <p>此方法应该严格检查独立实例；对于作用域对象或其他类型的非单例、
	 * 非独立对象，它不应该返回{@code true}。出于这个原因，这不是
	 * {@link #isSingleton()}的简单反向形式。
	 * <p>默认实现返回{@code false}。
	 * @return 暴露的对象是否为原型
	 * @see #getObject()
	 * @see #isSingleton()
	 */
	default boolean isPrototype() {
		return false;
	}

	/**
	 * 此FactoryBean是否期望预先初始化，也就是说，预先初始化自身以及
	 * 期望预先初始化其单例对象（如果有的话）？
	 * <p>标准的FactoryBean不期望预先初始化：其{@link #getObject()}只会在
	 * 实际访问时被调用，即使是单例对象的情况下也是如此。从此方法返回{@code true}
	 * 表示应该预先调用{@link #getObject()}，同时也预先应用后处理器。
	 * 在{@link #isSingleton() 单例}对象的情况下这可能是有意义的，特别是
	 * 如果后处理器期望在启动时被应用。
	 * <p>默认实现返回{@code false}。
	 * @return 是否应用预先初始化
	 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#preInstantiateSingletons()
	 */
	default boolean isEagerInit() {
		return false;
	}

}
