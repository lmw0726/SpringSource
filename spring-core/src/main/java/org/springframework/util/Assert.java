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

package org.springframework.util;

import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 断言工具类，用于辅助验证参数有效性。
 *
 * <p>可用于在运行时清晰、及早地识别编程错误。
 *
 * <p>例如，如果公共方法的契约规定不允许{@code null}参数，
 * 可以使用{@code Assert}来验证该契约。这能在违反契约时清晰地指出问题，
 * 并保护类的不变性条件。
 *
 * <p>通常用于验证方法参数而非配置属性，检查通常是程序员错误而非配置错误的情况。
 * 与配置初始化代码不同，此类方法通常不需要回退到默认值。
 *
 * <p>此类类似于JUnit的断言库。如果参数值被视为无效，通常会抛出
 * {@link IllegalArgumentException}。例如：
 *
 * <pre class="code">
 * Assert.notNull(clazz, "类不能为null");
 * Assert.isTrue(i > 0, "值必须大于零");</pre>
 *
 * <p>主要供框架内部使用；如需更全面的断言工具集，可考虑使用
 * <a href="https://commons.apache.org/proper/commons-lang/">Apache Commons Lang</a>的
 * {@code org.apache.commons.lang3.Validate}、
 * Google Guava的
 * <a href="https://github.com/google/guava/wiki/PreconditionsExplained">Preconditions</a>、
 * 或类似的第三方库。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Colin Sampaleanu
 * @author Rob Harrop
 * @since 1.1.2
 */
public abstract class Assert {

	/**
	 * 断言布尔表达式为true，如果表达式结果为{@code false}则抛出{@code IllegalStateException}。
	 * <p>如需在断言失败时抛出{@code IllegalArgumentException}，请调用{@link #isTrue}。
	 * <pre class="code">Assert.state(id == null, "id属性必须尚未初始化");</pre>
	 * @param expression 布尔表达式
	 * @param message 断言失败时的异常消息
	 * @throws IllegalStateException 如果{@code expression}为{@code false}
	 */
	public static void state(boolean expression, String message) {
		if (!expression) {
			throw new IllegalStateException(message);
		}
	}

	/**
	 * 断言布尔表达式为true，如果表达式结果为{@code false}则抛出{@code IllegalStateException}。
	 * <p>如需在断言失败时抛出{@code IllegalArgumentException}，请调用{@link #isTrue}。
	 * <pre class="code">
	 * Assert.state(entity.getId() == null,
	 *     () -> "实体" + entity.getName() + "的ID必须尚未初始化");
	 * </pre>
	 * @param expression 布尔表达式
	 * @param messageSupplier 断言失败时的异常消息提供者
	 * @throws IllegalStateException 如果{@code expression}为{@code false}
	 * @since 5.0
	 */
	public static void state(boolean expression, Supplier<String> messageSupplier) {
		if (!expression) {
			throw new IllegalStateException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言布尔表达式为true，如果表达式结果为{@code false}则抛出{@code IllegalStateException}。
	 * @deprecated 自4.3.7起，推荐使用{@link #state(boolean, String)}
	 */
	@Deprecated
	public static void state(boolean expression) {
		state(expression, "[Assertion failed] - this state invariant must be true");
	}

	/**
	 * 断言布尔表达式为true，如果表达式结果为{@code false}则抛出{@code IllegalArgumentException}。
	 * <pre class="code">Assert.isTrue(i > 0, "值必须大于零");</pre>
	 * @param expression 布尔表达式
	 * @param message 断言失败时的异常消息
	 * @throws IllegalArgumentException 如果{@code expression}为{@code false}
	 */
	public static void isTrue(boolean expression, String message) {
		if (!expression) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言布尔表达式为true，如果表达式结果为{@code false}则抛出{@code IllegalArgumentException}。
	 * <pre class="code">
	 * Assert.isTrue(i > 0, () -> "值'" + i + "'必须大于零");
	 * </pre>
	 * @param expression 布尔表达式
	 * @param messageSupplier 断言失败时的异常消息提供者
	 * @throws IllegalArgumentException 如果{@code expression}为{@code false}
	 * @since 5.0
	 */
	public static void isTrue(boolean expression, Supplier<String> messageSupplier) {
		if (!expression) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言布尔表达式为true，如果表达式结果为{@code false}则抛出{@code IllegalArgumentException}。
	 * @deprecated 自4.3.7起，推荐使用{@link #isTrue(boolean, String)}
	 */
	@Deprecated
	public static void isTrue(boolean expression) {
		isTrue(expression, "[Assertion failed] - this expression must be true");
	}

	/**
	 * 断言对象为{@code null}。
	 * <pre class="code">Assert.isNull(value, "值必须为null");</pre>
	 * @param object 要检查的对象
	 * @param message 断言失败时的异常消息
	 * @throws IllegalArgumentException 如果对象不为{@code null}
	 */
	public static void isNull(@Nullable Object object, String message) {
		if (object != null) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言对象为{@code null}。
	 * <pre class="code">
	 * Assert.isNull(value, () -> "值'" + value + "'必须为null");
	 * </pre>
	 * @param object 要检查的对象
	 * @param messageSupplier 断言失败时的异常消息提供者
	 * @throws IllegalArgumentException 如果对象不为{@code null}
	 * @since 5.0
	 */
	public static void isNull(@Nullable Object object, Supplier<String> messageSupplier) {
		if (object != null) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言对象为{@code null}。
	 * @deprecated 自4.3.7起，推荐使用{@link #isNull(Object, String)}
	 */
	@Deprecated
	public static void isNull(@Nullable Object object) {
		isNull(object, "[Assertion failed] - the object argument must be null");
	}

	/**
	 * 断言对象不为{@code null}。
	 * <pre class="code">Assert.notNull(clazz, "类不能为null");</pre>
	 * @param object 要检查的对象
	 * @param message 断言失败时的异常消息
	 * @throws IllegalArgumentException 如果对象为{@code null}
	 */
	public static void notNull(@Nullable Object object, String message) {
		if (object == null) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言对象不为{@code null}。
	 * <pre class="code">
	 * Assert.notNull(entity.getId(),
	 *     () -> "实体" + entity.getName() + "的ID不能为null");
	 * </pre>
	 * @param object 要检查的对象
	 * @param messageSupplier 断言失败时的异常消息提供者
	 * @throws IllegalArgumentException 如果对象为{@code null}
	 * @since 5.0
	 */
	public static void notNull(@Nullable Object object, Supplier<String> messageSupplier) {
		if (object == null) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言对象不为{@code null}。
	 * @deprecated 自4.3.7起，推荐使用{@link #notNull(Object, String)}
	 */
	@Deprecated
	public static void notNull(@Nullable Object object) {
		notNull(object, "[Assertion failed] - this argument is required; it must not be null");
	}

	/**
	 * 断言给定字符串不为空，即不能为{@code null}且不能为空字符串。
	 * <pre class="code">Assert.hasLength(name, "名称不能为空");</pre>
	 * @param text 要检查的字符串
	 * @param message 断言失败时的异常消息
	 * @throws IllegalArgumentException 如果字符串为空
	 * @see StringUtils#hasLength
	 */
	public static void hasLength(@Nullable String text, String message) {
		if (!StringUtils.hasLength(text)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言给定字符串不为空，即不能为{@code null}且不能为空字符串。
	 * <pre class="code">
	 * Assert.hasLength(account.getName(),
	 *     () -> "账户'" + account.getId() + "'的名称不能为空");
	 * </pre>
	 * @param text 要检查的字符串
	 * @param messageSupplier 断言失败时的异常消息提供者
	 * @throws IllegalArgumentException 如果字符串为空
	 * @since 5.0
	 * @see StringUtils#hasLength
	 */
	public static void hasLength(@Nullable String text, Supplier<String> messageSupplier) {
		if (!StringUtils.hasLength(text)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言给定字符串不为空，即不能为{@code null}且不能为空字符串。
	 * @deprecated 自4.3.7起，推荐使用{@link #hasLength(String, String)}
	 */
	@Deprecated
	public static void hasLength(@Nullable String text) {
		hasLength(text,
				"[Assertion failed] - this String argument must have length; it must not be null or empty");
	}

	/**
	 * 断言给定字符串包含有效文本内容，即不能为{@code null}且必须包含至少一个非空白字符。
	 * <pre class="code">Assert.hasText(name, "'name'不能为空");</pre>
	 * @param text 要检查的字符串
	 * @param message 断言失败时的异常消息
	 * @throws IllegalArgumentException 如果字符串不包含有效文本内容
	 * @see StringUtils#hasText
	 */
	public static void hasText(@Nullable String text, String message) {
		if (!StringUtils.hasText(text)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言给定字符串包含有效文本内容，即不能为{@code null}且必须包含至少一个非空白字符。
	 * <pre class="code">
	 * Assert.hasText(account.getName(),
	 *     () -> "账户'" + account.getId() + "'的名称不能为空");
	 * </pre>
	 * @param text 要检查的字符串
	 * @param messageSupplier 断言失败时的异常消息提供者
	 * @throws IllegalArgumentException 如果字符串不包含有效文本内容
	 * @since 5.0
	 * @see StringUtils#hasText
	 */
	public static void hasText(@Nullable String text, Supplier<String> messageSupplier) {
		if (!StringUtils.hasText(text)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言给定字符串包含有效文本内容，即不能为{@code null}且必须包含至少一个非空白字符。
	 * @deprecated 自4.3.7起，推荐使用{@link #hasText(String, String)}
	 */
	@Deprecated
	public static void hasText(@Nullable String text) {
		hasText(text,
				"[Assertion failed] - this String argument must have text; it must not be null, empty, or blank");
	}

	/**
	 * 断言给定文本不包含指定子字符串。
	 * <pre class="code">Assert.doesNotContain(name, "rod", "名称不能包含'rod'");</pre>
	 * @param textToSearch 要搜索的文本
	 * @param substring 要在文本中查找的子字符串
	 * @param message 断言失败时的异常消息
	 * @throws IllegalArgumentException 如果文本包含子字符串
	 */
	public static void doesNotContain(@Nullable String textToSearch, String substring, String message) {
		if (StringUtils.hasLength(textToSearch) && StringUtils.hasLength(substring) &&
				textToSearch.contains(substring)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言给定文本不包含指定子字符串。
	 * <pre class="code">
	 * Assert.doesNotContain(name, forbidden, () -> "名称不能包含'" + forbidden + "'");
	 * </pre>
	 * @param textToSearch 要搜索的文本
	 * @param substring 要在文本中查找的子字符串
	 * @param messageSupplier 断言失败时的异常消息提供者
	 * @throws IllegalArgumentException 如果文本包含子字符串
	 * @since 5.0
	 */
	public static void doesNotContain(@Nullable String textToSearch, String substring, Supplier<String> messageSupplier) {
		if (StringUtils.hasLength(textToSearch) && StringUtils.hasLength(substring) &&
				textToSearch.contains(substring)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言给定文本不包含指定子字符串。
	 * @deprecated 自4.3.7起，推荐使用{@link #doesNotContain(String, String, String)}
	 */
	@Deprecated
	public static void doesNotContain(@Nullable String textToSearch, String substring) {
		doesNotContain(textToSearch, substring,
				() -> "[Assertion failed] - this String argument must not contain the substring [" + substring + "]");
	}

	/**
	 * 断言数组包含元素，即不能为{@code null}且必须至少包含一个元素。
	 * <pre class="code">Assert.notEmpty(array, "数组必须包含元素");</pre>
	 * @param array 要检查的数组
	 * @param message 断言失败时的异常消息
	 * @throws IllegalArgumentException 如果数组为{@code null}或不包含元素
	 */
	public static void notEmpty(@Nullable Object[] array, String message) {
		if (ObjectUtils.isEmpty(array)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言数组包含元素，即不能为{@code null}且必须至少包含一个元素。
	 * <pre class="code">
	 * Assert.notEmpty(array, () -> arrayType + "数组必须包含元素");
	 * </pre>
	 * @param array 要检查的数组
	 * @param messageSupplier 断言失败时的异常消息提供者
	 * @throws IllegalArgumentException 如果数组为{@code null}或不包含元素
	 * @since 5.0
	 */
	public static void notEmpty(@Nullable Object[] array, Supplier<String> messageSupplier) {
		if (ObjectUtils.isEmpty(array)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言数组包含元素，即不能为{@code null}且必须至少包含一个元素。
	 * @deprecated 自4.3.7起，推荐使用{@link #notEmpty(Object[], String)}
	 */
	@Deprecated
	public static void notEmpty(@Nullable Object[] array) {
		notEmpty(array, "[Assertion failed] - this array must not be empty: it must contain at least 1 element");
	}

	/**
	 * 断言数组不包含{@code null}元素。
	 * <p>注意：不会对空数组报错！
	 * <pre class="code">Assert.noNullElements(array, "数组必须包含非null元素");</pre>
	 * @param array 要检查的数组
	 * @param message 断言失败时的异常消息
	 * @throws IllegalArgumentException 如果数组包含{@code null}元素
	 */
	public static void noNullElements(@Nullable Object[] array, String message) {
		if (array != null) {
			for (Object element : array) {
				if (element == null) {
					throw new IllegalArgumentException(message);
				}
			}
		}
	}

	/**
	 * 断言数组不包含{@code null}元素。
	 * <p>注意：不会对空数组报错！
	 * <pre class="code">
	 * Assert.noNullElements(array, () -> arrayType + "数组必须包含非null元素");
	 * </pre>
	 * @param array 要检查的数组
	 * @param messageSupplier 断言失败时的异常消息提供者
	 * @throws IllegalArgumentException 如果数组包含{@code null}元素
	 * @since 5.0
	 */
	public static void noNullElements(@Nullable Object[] array, Supplier<String> messageSupplier) {
		if (array != null) {
			for (Object element : array) {
				if (element == null) {
					throw new IllegalArgumentException(nullSafeGet(messageSupplier));
				}
			}
		}
	}

	/**
	 * 断言数组不包含{@code null}元素。
	 * @deprecated 自4.3.7起，推荐使用{@link #noNullElements(Object[], String)}
	 */
	@Deprecated
	public static void noNullElements(@Nullable Object[] array) {
		noNullElements(array, "[Assertion failed] - this array must not contain any null elements");
	}

	/**
	 * 断言集合包含元素，即不能为{@code null}且必须至少包含一个元素。
	 * <pre class="code">Assert.notEmpty(collection, "集合必须包含元素");</pre>
	 * @param collection 要检查的集合
	 * @param message 断言失败时的异常消息
	 * @throws IllegalArgumentException 如果集合为{@code null}或不包含元素
	 */
	public static void notEmpty(@Nullable Collection<?> collection, String message) {
		if (CollectionUtils.isEmpty(collection)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言集合包含元素，即不能为{@code null}且必须至少包含一个元素。
	 * <pre class="code">
	 * Assert.notEmpty(collection, () -> collectionType + "集合必须包含元素");
	 * </pre>
	 * @param collection 要检查的集合
	 * @param messageSupplier 断言失败时的异常消息提供者
	 * @throws IllegalArgumentException 如果集合为{@code null}或不包含元素
	 * @since 5.0
	 */
	public static void notEmpty(@Nullable Collection<?> collection, Supplier<String> messageSupplier) {
		if (CollectionUtils.isEmpty(collection)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言集合包含元素，即不能为{@code null}且必须至少包含一个元素。
	 * @deprecated 自4.3.7起，推荐使用{@link #notEmpty(Collection, String)}
	 */
	@Deprecated
	public static void notEmpty(@Nullable Collection<?> collection) {
		notEmpty(collection,
				"[Assertion failed] - this collection must not be empty: it must contain at least 1 element");
	}

	/**
	 * 断言集合不包含{@code null}元素。
	 * <p>注意：不会对空集合报错！
	 * <pre class="code">Assert.noNullElements(collection, "集合必须包含非null元素");</pre>
	 * @param collection 要检查的集合
	 * @param message 断言失败时的异常消息
	 * @throws IllegalArgumentException 如果集合包含{@code null}元素
	 * @since 5.2
	 */
	public static void noNullElements(@Nullable Collection<?> collection, String message) {
		if (collection != null) {
			for (Object element : collection) {
				if (element == null) {
					throw new IllegalArgumentException(message);
				}
			}
		}
	}

	/**
	 * 断言集合不包含{@code null}元素。
	 * <p>注意：不会对空集合报错！
	 * <pre class="code">
	 * Assert.noNullElements(collection, () -> "集合" + collectionName + "必须包含非null元素");
	 * </pre>
	 * @param collection 要检查的集合
	 * @param messageSupplier 断言失败时的异常消息提供者
	 * @throws IllegalArgumentException 如果集合包含{@code null}元素
	 * @since 5.2
	 */
	public static void noNullElements(@Nullable Collection<?> collection, Supplier<String> messageSupplier) {
		if (collection != null) {
			for (Object element : collection) {
				if (element == null) {
					throw new IllegalArgumentException(nullSafeGet(messageSupplier));
				}
			}
		}
	}

	/**
	 * 断言Map包含条目，即不能为{@code null}且必须至少包含一个条目。
	 * <pre class="code">Assert.notEmpty(map, "Map必须包含条目");</pre>
	 * @param map 要检查的Map
	 * @param message 断言失败时的异常消息
	 * @throws IllegalArgumentException 如果Map为{@code null}或不包含条目
	 */
	public static void notEmpty(@Nullable Map<?, ?> map, String message) {
		if (CollectionUtils.isEmpty(map)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言Map包含条目，即不能为{@code null}且必须至少包含一个条目。
	 * <pre class="code">
	 * Assert.notEmpty(map, () -> "The " + mapType + " map must contain entries");
	 * </pre>
	 * @param map 要检查的Map
	 * @param messageSupplier 断言失败时使用的异常消息提供者
	 * @throws IllegalArgumentException 如果Map为{@code null}或不包含条目
	 * @since 5.0
	 */
	public static void notEmpty(@Nullable Map<?, ?> map, Supplier<String> messageSupplier) {
		if (CollectionUtils.isEmpty(map)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言Map包含条目，即不能为{@code null}且必须至少包含一个条目。
	 * @deprecated 自4.3.7起，推荐使用{@link #notEmpty(Map, String)}
	 */
	@Deprecated
	public static void notEmpty(@Nullable Map<?, ?> map) {
		notEmpty(map, "[Assertion failed] - this map must not be empty; it must contain at least one entry");
	}

	/**
	 * 断言给定对象是指定类型的实例。
	 * <pre class="code">Assert.instanceOf(Foo.class, foo, "Foo expected");</pre>
	 * @param type 要检查的类型
	 * @param obj 要检查的对象
	 * @param message 前置的上下文消息。如果消息为空或以":"、";"、","或"."结尾，
	 * 将追加完整的异常消息。如果以空格结尾，将追加违规对象的类型名称。其他情况下，
	 * 将追加":"加空格和违规对象的类型名称。
	 * @throws IllegalArgumentException 如果对象不是指定类型的实例
	 */
	public static void isInstanceOf(Class<?> type, @Nullable Object obj, String message) {
		notNull(type, "Type to check against must not be null");
		if (!type.isInstance(obj)) {
			instanceCheckFailed(type, obj, message);
		}
	}

	/**
	 * 断言给定对象是指定类型的实例。
	 * <pre class="code">
	 * Assert.instanceOf(Foo.class, foo, () -> "Processing " + Foo.class.getSimpleName() + ":");
	 * </pre>
	 * @param type 要检查的类型
	 * @param obj 要检查的对象
	 * @param messageSupplier 断言失败时使用的异常消息提供者。详情参见
	 * {@link #isInstanceOf(Class, Object, String)}。
	 * @throws IllegalArgumentException 如果对象不是指定类型的实例
	 * @since 5.0
	 */
	public static void isInstanceOf(Class<?> type, @Nullable Object obj, Supplier<String> messageSupplier) {
		notNull(type, "Type to check against must not be null");
		if (!type.isInstance(obj)) {
			instanceCheckFailed(type, obj, nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言给定对象是指定类型的实例。
	 * <pre class="code">Assert.instanceOf(Foo.class, foo);</pre>
	 * @param type 要检查的类型
	 * @param obj 要检查的对象
	 * @throws IllegalArgumentException 如果对象不是指定类型的实例
	 */
	public static void isInstanceOf(Class<?> type, @Nullable Object obj) {
		isInstanceOf(type, obj, "");
	}

	/**
	 * 断言 {@code superType.isAssignableFrom(subType)} 为 {@code true}。
	 * <pre class="code">Assert.isAssignable(Number.class, myClass, "Number expected");</pre>
	 * @param superType 要检查的父类型
	 * @param subType 要检查的子类型
	 * @param message 前置的上下文消息。如果消息为空或以":"、";"、","或"."结尾，
	 * 将追加完整的异常消息。如果以空格结尾，将追加违规的子类型名称。其他情况下，
	 * 将追加":"加空格和违规的子类型名称。
	 * @throws IllegalArgumentException 如果类型之间不可赋值
	 */
	public static void isAssignable(Class<?> superType, @Nullable Class<?> subType, String message) {
		notNull(superType, "Super type to check against must not be null");
		if (subType == null || !superType.isAssignableFrom(subType)) {
			assignableCheckFailed(superType, subType, message);
		}
	}

	/**
	 * 断言 {@code superType.isAssignableFrom(subType)} 为 {@code true}。
	 * <pre class="code">
	 * Assert.isAssignable(Number.class, myClass, () -> "Processing " + myAttributeName + ":");
	 * </pre>
	 * @param superType 要检查的父类型
	 * @param subType 要检查的子类型
	 * @param messageSupplier 断言失败时使用的异常消息提供者。详情参见
	 * {@link #isAssignable(Class, Class, String)}。
	 * @throws IllegalArgumentException 如果类型之间不可赋值
	 * @since 5.0
	 */
	public static void isAssignable(Class<?> superType, @Nullable Class<?> subType, Supplier<String> messageSupplier) {
		notNull(superType, "Super type to check against must not be null");
		if (subType == null || !superType.isAssignableFrom(subType)) {
			assignableCheckFailed(superType, subType, nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言 {@code superType.isAssignableFrom(subType)} 为 {@code true}。
	 * <pre class="code">Assert.isAssignable(Number.class, myClass);</pre>
	 * @param superType 要检查的父类型
	 * @param subType 要检查的子类型
	 * @throws IllegalArgumentException 如果类型之间不可赋值
	 */
	public static void isAssignable(Class<?> superType, Class<?> subType) {
		isAssignable(superType, subType, "");
	}


	private static void instanceCheckFailed(Class<?> type, @Nullable Object obj, @Nullable String msg) {
		String className = (obj != null ? obj.getClass().getName() : "null");
		String result = "";
		boolean defaultMessage = true;
		if (StringUtils.hasLength(msg)) {
			if (endsWithSeparator(msg)) {
				result = msg + " ";
			}
			else {
				result = messageWithTypeName(msg, className);
				defaultMessage = false;
			}
		}
		if (defaultMessage) {
			result = result + ("Object of class [" + className + "] must be an instance of " + type);
		}
		throw new IllegalArgumentException(result);
	}

	private static void assignableCheckFailed(Class<?> superType, @Nullable Class<?> subType, @Nullable String msg) {
		String result = "";
		boolean defaultMessage = true;
		if (StringUtils.hasLength(msg)) {
			if (endsWithSeparator(msg)) {
				result = msg + " ";
			}
			else {
				result = messageWithTypeName(msg, subType);
				defaultMessage = false;
			}
		}
		if (defaultMessage) {
			result = result + (subType + " is not assignable to " + superType);
		}
		throw new IllegalArgumentException(result);
	}

	private static boolean endsWithSeparator(String msg) {
		return (msg.endsWith(":") || msg.endsWith(";") || msg.endsWith(",") || msg.endsWith("."));
	}

	private static String messageWithTypeName(String msg, @Nullable Object typeName) {
		return msg + (msg.endsWith(" ") ? "" : ": ") + typeName;
	}

	@Nullable
	private static String nullSafeGet(@Nullable Supplier<String> messageSupplier) {
		return (messageSupplier != null ? messageSupplier.get() : null);
	}

}
