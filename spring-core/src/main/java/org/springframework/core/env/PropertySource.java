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

package org.springframework.core.env;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 抽象基类，表示名称/值属性对的源。底层的 {@linkplain #getSource() 源对象} 可以是任何类型 {@code T}，
 * 它封装了属性。示例包括 {@link java.util.Properties} 对象、{@link java.util.Map} 对象、
 * {@code ServletContext} 和 {@code ServletConfig} 对象（用于访问初始化参数）。
 * 探索 {@code PropertySource} 类型层次结构以查看提供的实现。
 *
 * <p>{@code PropertySource} 对象通常不会单独使用，而是通过一个 {@link PropertySources} 对象使用，
 * 该对象聚合了属性源，并与能够在一组 {@code PropertySources} 中执行基于优先级的搜索的 {@link PropertyResolver} 实现配合使用。
 *
 * <p>{@code PropertySource} 的标识不是根据封装属性的内容确定的，而是仅根据 {@code PropertySource} 的
 * {@link #getName() 名称} 确定的。在集合上下文中操作 {@code PropertySource} 对象时，这很有用。
 * 有关详细信息，请参阅 {@link MutablePropertySources} 中的操作以及 {@link #named(String)} 和 {@link #toString()} 方法。
 * <p>
 * <p请注意，在使用 @{@link org.springframework.context.annotation.PropertySource PropertySource} 注解的
 *
 * @param <T> 源类型
 * @author Chris Beams
 * @{@link org.springframework.context.annotation.Configuration Configuration} 类时，
 * 它提供了一种便捷和声明性的方式将属性源添加到封闭的 {@code Environment} 中。
 * @see PropertySources
 * @see PropertyResolver
 * @see PropertySourcesPropertyResolver
 * @see MutablePropertySources
 * @see org.springframework.context.annotation.PropertySource
 * @since 3.1
 */
public abstract class PropertySource<T> {

	protected final Log logger = LogFactory.getLog(getClass());
	/**
	 * 名称
	 */
	protected final String name;
	/**
	 * 属性源
	 */
	protected final T source;


	/**
	 * 使用给定的名称和源对象创建一个新的 {@code PropertySource}。
	 *
	 * @param name   关联的名称
	 * @param source 源对象
	 */
	public PropertySource(String name, T source) {
		Assert.hasText(name, "Property source name must contain at least one character");
		Assert.notNull(source, "Property source must not be null");
		this.name = name;
		this.source = source;
	}

	/**
	 * 使用给定的名称创建一个新的 {@code PropertySource}，并使用新的 {@code Object} 实例作为底层源。
	 * <p>在测试场景中经常有用，用于创建永远不会查询实际源但返回硬编码值的匿名实现。
	 */
	@SuppressWarnings("unchecked")
	public PropertySource(String name) {
		this(name, (T) new Object());
	}


	/**
	 * 返回此 {@code PropertySource} 的名称。
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 返回此 {@code PropertySource} 的基础源对象。
	 */
	public T getSource() {
		return this.source;
	}

	/**
	 * 返回此 {@code PropertySource} 是否包含给定名称。
	 * <p>此实现仅检查 {@link #getProperty(String)} 的返回值是否为 {@code null}。
	 * 如果可能的话，子类可能希望实现一个更高效的算法。
	 *
	 * @param name 要查找的属性名称
	 */
	public boolean containsProperty(String name) {
		return (getProperty(name) != null);
	}

	/**
	 * 返回与给定名称关联的值，如果找不到，则返回 {@code null}。
	 *
	 * @param name 找到的属性
	 * @see PropertyResolver#getRequiredProperty(String)
	 */
	@Nullable
	public abstract Object getProperty(String name);


	/**
	 * 如果：
	 * <ul>
	 * <li>它们是相同的实例
	 * <li>两个对象的 {@code name} 属性相等
	 * </ul>
	 * 则此 {@code PropertySource} 对象等于给定的对象。
	 * <p>除 {@code name} 以外的属性不会被评估。
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof PropertySource &&
				ObjectUtils.nullSafeEquals(getName(), ((PropertySource<?>) other).getName())));
	}

	/**
	 * 返回从此 {@code PropertySource} 对象的 {@code name} 属性派生的哈希码。
	 */
	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(getName());
	}

	/**
	 * 如果当前日志级别不包括调试，则生成简洁的输出（类型和名称）。
	 * 如果启用了调试，则生成详细输出，包括 PropertySource 实例的哈希码和每个名称/值属性对。
	 * <p>此变量详细信息对于诸如系统属性或环境变量之类的属性源可能包含任意数量的属性对很有用，
	 * 这可能导致难以阅读的异常和日志消息。
	 *
	 * @see Log#isDebugEnabled()
	 */
	@Override
	public String toString() {
		if (logger.isDebugEnabled()) {
			return getClass().getSimpleName() + "@" + System.identityHashCode(this) +
					" {name='" + getName() + "', properties=" + getSource() + "}";
		} else {
			return getClass().getSimpleName() + " {name='" + getName() + "'}";
		}
	}


	/**
	 * 返回一个仅用于集合比较目的的 {@code PropertySource} 实现。
	 * <p>主要供内部使用，但给定一组 {@code PropertySource} 对象，可能会如下使用：
	 * <pre class="code">
	 * {@code List<PropertySource<?>> sources = new ArrayList<PropertySource<?>>();
	 * sources.add(new MapPropertySource("sourceA", mapA));
	 * sources.add(new MapPropertySource("sourceB", mapB));
	 * assert sources.contains(PropertySource.named("sourceA"));
	 * assert sources.contains(PropertySource.named("sourceB"));
	 * assert !sources.contains(PropertySource.named("sourceC"));
	 * }</pre>
	 * 如果调用了除 {@code equals(Object)}、{@code hashCode()} 和 {@code toString()} 之外的任何方法，
	 * 返回的 {@code PropertySource} 将抛出 {@code UnsupportedOperationException}。
	 *
	 * @param name 要创建和返回的比较 {@code PropertySource} 的名称。
	 */
	public static PropertySource<?> named(String name) {
		return new ComparisonPropertySource(name);
	}


	/**
	 * {@code PropertySource} 用于在实际属性源不能在应用程序上下文创建时急切地初始化的情况下使用占位符。
	 * 例如，基于 {@code ServletContext} 的属性源必须等到其封闭 {@code ApplicationContext} 中的 {@code ServletContext} 对象可用。
	 * 在这种情况下，应使用存根来保存属性源的预期默认位置/顺序，然后在上下文刷新期间替换。
	 *
	 * @see org.springframework.context.support.AbstractApplicationContext#initPropertySources()
	 * @see org.springframework.web.context.support.StandardServletEnvironment
	 * @see org.springframework.web.context.support.ServletContextPropertySource
	 */
	public static class StubPropertySource extends PropertySource<Object> {

		public StubPropertySource(String name) {
			super(name, new Object());
		}

		/**
		 * 总是返回 {@code null}。
		 */
		@Override
		@Nullable
		public String getProperty(String name) {
			return null;
		}
	}


	/**
	 * 用于集合比较目的的 {@code PropertySource} 实现。
	 *
	 * @see PropertySource#named(String)
	 */
	static class ComparisonPropertySource extends StubPropertySource {

		private static final String USAGE_ERROR =
				"ComparisonPropertySource instances are for use with collection comparison only";

		public ComparisonPropertySource(String name) {
			super(name);
		}

		@Override
		public Object getSource() {
			throw new UnsupportedOperationException(USAGE_ERROR);
		}

		@Override
		public boolean containsProperty(String name) {
			throw new UnsupportedOperationException(USAGE_ERROR);
		}

		@Override
		@Nullable
		public String getProperty(String name) {
			throw new UnsupportedOperationException(USAGE_ERROR);
		}
	}

}
