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

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * {@link ObjectFactory}的一个变体，专为注入点而设计，
 * 允许程序化的可选性和宽松的非唯一处理。
 *
 * <p>从5.1版本开始，此接口扩展了{@link Iterable}并提供{@link Stream}
 * 支持。因此可以在{@code for}循环中使用，提供{@link #forEach}
 * 迭代并允许集合式的{@link #stream}访问。
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @param <T> 对象类型
 * @see BeanFactory#getBeanProvider
 * @see org.springframework.beans.factory.annotation.Autowired
 */
public interface ObjectProvider<T> extends ObjectFactory<T>, Iterable<T> {

	/**
	 * 返回此工厂管理的对象的一个实例（可能是共享的或独立的）。
	 * <p>允许指定明确的构造参数，类似于
	 * {@link BeanFactory#getBean(String, Object...)}。
	 * @param args 创建相应实例时使用的参数
	 * @return bean的实例
	 * @throws BeansException 创建过程中出现错误时抛出
	 * @see #getObject()
	 */
	T getObject(Object... args) throws BeansException;

	/**
	 * 返回此工厂管理的对象的一个实例（可能是共享的或独立的）。
	 * @return bean的实例，如果不可用则返回{@code null}
	 * @throws BeansException 创建过程中出现错误时抛出
	 * @see #getObject()
	 */
	@Nullable
	T getIfAvailable() throws BeansException;

	/**
	 * 返回此工厂管理的对象的一个实例（可能是共享的或独立的）。
	 * @param defaultSupplier 当工厂中不存在对象时，用于提供默认对象的回调
	 * @return bean的实例，如果没有这样的bean可用，则返回提供的默认对象
	 * @throws BeansException 创建过程中出现错误时抛出
	 * @since 5.0
	 * @see #getIfAvailable()
	 */
	default T getIfAvailable(Supplier<T> defaultSupplier) throws BeansException {
		T dependency = getIfAvailable();
		return (dependency != null ? dependency : defaultSupplier.get());
	}

	/**
	 * 如果可用，则使用此工厂管理的对象的一个实例（可能是共享的或独立的）。
	 * @param dependencyConsumer 如果对象可用则用于处理目标对象的回调
	 * （否则不会被调用）
	 * @throws BeansException 创建过程中出现错误时抛出
	 * @since 5.0
	 * @see #getIfAvailable()
	 */
	default void ifAvailable(Consumer<T> dependencyConsumer) throws BeansException {
		T dependency = getIfAvailable();
		if (dependency != null) {
			dependencyConsumer.accept(dependency);
		}
	}

	/**
	 * 返回此工厂管理的对象的一个实例（可能是共享的或独立的）。
	 * @return bean的实例，如果不可用或不唯一则返回{@code null}
	 * （即找到多个候选者但没有标记为主要的）
	 * @throws BeansException 创建过程中出现错误时抛出
	 * @see #getObject()
	 */
	@Nullable
	T getIfUnique() throws BeansException;

	/**
	 * 返回此工厂管理的对象的一个实例（可能是共享的或独立的）。
	 * @param defaultSupplier 当工厂中不存在唯一候选者时，用于提供默认对象的回调
	 * @return bean的实例，如果没有这样的bean可用或在工厂中不唯一，则返回提供的默认对象
	 * （即找到多个候选者但没有标记为主要的）
	 * @throws BeansException 创建过程中出现错误时抛出
	 * @since 5.0
	 * @see #getIfUnique()
	 */
	default T getIfUnique(Supplier<T> defaultSupplier) throws BeansException {
		T dependency = getIfUnique();
		return (dependency != null ? dependency : defaultSupplier.get());
	}

	/**
	 * 如果唯一，则使用此工厂管理的对象的一个实例（可能是共享的或独立的）。
	 * @param dependencyConsumer 如果对象唯一则用于处理目标对象的回调
	 * （否则不会被调用）
	 * @throws BeansException 创建过程中出现错误时抛出
	 * @since 5.0
	 * @see #getIfAvailable()
	 */
	default void ifUnique(Consumer<T> dependencyConsumer) throws BeansException {
		T dependency = getIfUnique();
		if (dependency != null) {
			dependencyConsumer.accept(dependency);
		}
	}

	/**
	 * 返回所有匹配对象实例的{@link Iterator}，
	 * 不保证特定的排序（但通常按注册顺序）。
	 * @since 5.1
	 * @see #stream()
	 */
	@Override
	default Iterator<T> iterator() {
		return stream().iterator();
	}

	/**
	 * 返回所有匹配对象实例的顺序{@link Stream}，
	 * 不保证特定的排序（但通常按注册顺序）。
	 * @since 5.1
	 * @see #iterator()
	 * @see #orderedStream()
	 */
	default Stream<T> stream() {
		throw new UnsupportedOperationException("Multi element access not supported");
	}

	/**
	 * 返回所有匹配对象实例的顺序{@link Stream}，
	 * 根据工厂的通用顺序比较器进行预排序。
	 * <p>在标准的Spring应用程序上下文中，这将根据
	 * {@link org.springframework.core.Ordered}约定进行排序，
	 * 在基于注解的配置情况下，还会考虑
	 * {@link org.springframework.core.annotation.Order}注解，
	 * 类似于列表/数组类型的多元素注入点。
	 * @since 5.1
	 * @see #stream()
	 * @see org.springframework.core.OrderComparator
	 */
	default Stream<T> orderedStream() {
		throw new UnsupportedOperationException("Ordered element access not supported");
	}

}
