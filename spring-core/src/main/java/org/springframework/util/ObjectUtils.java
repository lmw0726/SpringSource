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

package org.springframework.util;

import org.springframework.lang.Nullable;

import java.lang.reflect.Array;
import java.util.*;

/**
 * 对象操作工具类，提供各种杂项对象工具方法。
 *
 * <p>主要用于框架内部使用。
 *
 * <p>感谢Alex Ruiz对本类做出的多项改进贡献！
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Chris Beams
 * @author Sam Brannen
 * @see ClassUtils
 * @see CollectionUtils
 * @see StringUtils
 * @since 19.03.2004
 */
public abstract class ObjectUtils {

	private static final int INITIAL_HASH = 7;
	private static final int MULTIPLIER = 31;

	private static final String EMPTY_STRING = "";
	private static final String NULL_STRING = "null";
	private static final String ARRAY_START = "{";
	private static final String ARRAY_END = "}";
	private static final String EMPTY_ARRAY = ARRAY_START + ARRAY_END;
	private static final String ARRAY_ELEMENT_SEPARATOR = ", ";
	private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];


	/**
	 * 判断给定的异常是否为受检异常：
	 * 即既不是RuntimeException也不是Error。
	 *
	 * @param ex 要检查的异常
	 * @return 该异常是否为受检异常
	 * @see java.lang.Exception
	 * @see java.lang.RuntimeException
	 * @see java.lang.Error
	 */
	public static boolean isCheckedException(Throwable ex) {
		return !(ex instanceof RuntimeException || ex instanceof Error);
	}

	/**
	 * 检查给定异常是否与throws子句中声明的指定异常类型兼容。
	 *
	 * @param ex                 要检查的异常
	 * @param declaredExceptions throws子句中声明的异常类型
	 * @return 给定异常是否兼容
	 */
	public static boolean isCompatibleWithThrowsClause(Throwable ex, @Nullable Class<?>... declaredExceptions) {
		if (!isCheckedException(ex)) {
			return true;
		}
		if (declaredExceptions != null) {
			for (Class<?> declaredException : declaredExceptions) {
				if (declaredException.isInstance(ex)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 判断给定对象是否为数组：
	 * 可以是对象数组或基本类型数组。
	 *
	 * @param obj 要检查的对象
	 */
	public static boolean isArray(@Nullable Object obj) {
		return (obj != null && obj.getClass().isArray());
	}

	/**
	 * 确定给定数组是否为空: 即 {@code null} 或长度为零。
	 *
	 * @param array 要检查的数组
	 * @see #isEmpty(Object)
	 */
	public static boolean isEmpty(@Nullable Object[] array) {
		return (array == null || array.length == 0);
	}

	/**
	 * 判断给定对象是否为空。
	 * <p>本方法支持以下对象类型：
	 * <ul>
	 * <li>{@code Optional}：如果未{@link Optional#isPresent()}则视为空</li>
	 * <li>{@code Array}：如果长度为0则视为空</li>
	 * <li>{@link CharSequence}：如果长度为0则视为空</li>
	 * <li>{@link Collection}：委托给{@link Collection#isEmpty()}</li>
	 * <li>{@link Map}：委托给{@link Map#isEmpty()}</li>
	 * </ul>
	 * <p>如果给定对象非null且不属于上述支持的类型，则返回{@code false}。
	 *
	 * @param obj 要检查的对象
	 * @return 如果对象为{@code null}或<em>空</em>则返回{@code true}
	 * @see Optional#isPresent()
	 * @see ObjectUtils#isEmpty(Object[])
	 * @see StringUtils#hasLength(CharSequence)
	 * @see CollectionUtils#isEmpty(java.util.Collection)
	 * @see CollectionUtils#isEmpty(java.util.Map)
	 * @since 4.2
	 */
	public static boolean isEmpty(@Nullable Object obj) {
		if (obj == null) {
			return true;
		}

		if (obj instanceof Optional) {
			return !((Optional<?>) obj).isPresent();
		}
		if (obj instanceof CharSequence) {
			return ((CharSequence) obj).length() == 0;
		}
		if (obj.getClass().isArray()) {
			return Array.getLength(obj) == 0;
		}
		if (obj instanceof Collection) {
			return ((Collection<?>) obj).isEmpty();
		}
		if (obj instanceof Map) {
			return ((Map<?, ?>) obj).isEmpty();
		}

		// 其他情况
		return false;
	}

	/**
	 * 解包可能是 {@link java.util.Optional} 的给定对象。
	 *
	 * @param obj 候选对象
	 * @return {@code Optional} 中保存的值，如果 {@code Optional} 为空，则为 {@code null}；或者直接返回给定对象
	 * @since 5.0
	 */
	@Nullable
	public static Object unwrapOptional(@Nullable Object obj) {
		// 检查对象是否为Optional类型
		if (obj instanceof Optional) {
			// 将对象转换为Optional
			Optional<?> optional = (Optional<?>) obj;
			// 如果Optional为空，则返回null
			if (!optional.isPresent()) {
				return null;
			}
			// 获取Optional中的值
			Object result = optional.get();
			// 断言：结果不应该是Optional类型，避免多级Optional使用
			Assert.isTrue(!(result instanceof Optional), "Multi-level Optional usage not supported");
			return result;
		}
		// 如果不是Optional类型，则直接返回对象
		return obj;
	}

	/**
	 * 检查给定数组中是否包含指定元素。
	 *
	 * @param array   要检查的数组（可能为{@code null}，此时始终返回{@code false}）
	 * @param element 要查找的元素
	 * @return 是否在数组中找到该元素
	 */
	public static boolean containsElement(@Nullable Object[] array, Object element) {
		if (array == null) {
			return false;
		}
		for (Object arrayEle : array) {
			if (nullSafeEquals(arrayEle, element)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 检查给定的枚举常量数组是否包含指定名称的常量，比较时忽略大小写。
	 *
	 * @param enumValues 要检查的枚举值，通常通过{@code MyEnum.values()}获取
	 * @param constant   要查找的常量名称（不能为null或空字符串）
	 * @return 是否在枚举数组中找到该常量
	 */
	public static boolean containsConstant(Enum<?>[] enumValues, String constant) {
		return containsConstant(enumValues, constant, false);
	}

	/**
	 * 检查给定的枚举常量数组是否包含指定名称的常量。
	 *
	 * @param enumValues    要检查的枚举值，通常通过{@code MyEnum.values()}获取
	 * @param constant      要查找的常量名称（不能为null或空字符串）
	 * @param caseSensitive 比较时是否区分大小写
	 * @return 是否在枚举数组中找到该常量
	 */
	public static boolean containsConstant(Enum<?>[] enumValues, String constant, boolean caseSensitive) {
		for (Enum<?> candidate : enumValues) {
			if (caseSensitive ? candidate.toString().equals(constant) :
					candidate.toString().equalsIgnoreCase(constant)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * {@link Enum#valueOf(Class, String)} 的不区分大小写替代方法。
	 *
	 * @param <E>        具体的枚举类型
	 * @param enumValues  相关枚举常量的数组，通常通过{@code Enum.values()}获取
	 * @param constant    要获取的枚举常量值
	 * @throws IllegalArgumentException 如果在给定的枚举值数组中找不到指定常量。
	 *                                  可以使用{@link #containsConstant(Enum[], String)}作为防护避免此异常。
	 */
	public static <E extends Enum<?>> E caseInsensitiveValueOf(E[] enumValues, String constant) {
		for (E candidate : enumValues) {
			if (candidate.toString().equalsIgnoreCase(constant)) {
				return candidate;
			}
		}
		throw new IllegalArgumentException("Constant [" + constant + "] does not exist in enum type " +
				enumValues.getClass().getComponentType().getName());
	}

	/**
	 * 将给定对象添加到给定数组中，返回由原数组内容加上给定对象组成的新数组。
	 *
	 * @param array 要添加到的数组（可为{@code null}）
	 * @param obj   要添加的对象
	 * @return 新数组（相同组件类型，永不返回{@code null}）
	 */
	public static <A, O extends A> A[] addObjectToArray(@Nullable A[] array, @Nullable O obj) {
		Class<?> compType = Object.class;
		if (array != null) {
			compType = array.getClass().getComponentType();
		} else if (obj != null) {
			compType = obj.getClass();
		}
		int newArrLength = (array != null ? array.length + 1 : 1);
		@SuppressWarnings("unchecked")
		A[] newArr = (A[]) Array.newInstance(compType, newArrLength);
		if (array != null) {
			System.arraycopy(array, 0, newArr, 0, array.length);
		}
		newArr[newArr.length - 1] = obj;
		return newArr;
	}

	/**
	 * 将给定数组（可能是基本类型数组）转换为对象数组（必要时使用基本类型的包装类对象）。
	 * <p>{@code null}源值将被转换为空对象数组。
	 *
	 * @param source 可能为基本类型的数组
	 * @return 对应的对象数组（永不返回{@code null}）
	 * @throws IllegalArgumentException 如果参数不是数组
	 */
	public static Object[] toObjectArray(@Nullable Object source) {
		if (source instanceof Object[]) {
			return (Object[]) source;
		}
		if (source == null) {
			return EMPTY_OBJECT_ARRAY;
		}
		if (!source.getClass().isArray()) {
			throw new IllegalArgumentException("Source is not an array: " + source);
		}
		int length = Array.getLength(source);
		if (length == 0) {
			return EMPTY_OBJECT_ARRAY;
		}
		Class<?> wrapperType = Array.get(source, 0).getClass();
		Object[] newArray = (Object[]) Array.newInstance(wrapperType, length);
		for (int i = 0; i < length; i++) {
			newArray[i] = Array.get(source, i);
		}
		return newArray;
	}


	//---------------------------------------------------------------------
	// 基于内容的相等性/哈希码处理的便捷方法
	//---------------------------------------------------------------------

	/**
	 * 判断给定对象是否相等，如果两者都为{@code null}则返回{@code true}，
	 * 如果只有一个为{@code null}则返回{@code false}。
	 * <p>使用{@code Arrays.equals}比较数组，基于数组元素而非数组引用进行相等性检查。
	 *
	 * @param o1 要比较的第一个对象
	 * @param o2 要比较的第二个对象
	 * @return 给定对象是否相等
	 * @see Object#equals(Object)
	 * @see java.util.Arrays#equals
	 */
	public static boolean nullSafeEquals(@Nullable Object o1, @Nullable Object o2) {
		if (o1 == o2) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		if (o1.equals(o2)) {
			return true;
		}
		if (o1.getClass().isArray() && o2.getClass().isArray()) {
			return arrayEquals(o1, o2);
		}
		return false;
	}

	/**
	 * 使用{@code Arrays.equals}比较给定数组，基于数组元素而非数组引用进行相等性检查。
	 *
	 * @param o1 要比较的第一个数组
	 * @param o2 要比较的第二个数组
	 * @return 给定数组是否相等
	 * @see #nullSafeEquals(Object, Object)
	 * @see java.util.Arrays#equals
	 */
	private static boolean arrayEquals(Object o1, Object o2) {
		if (o1 instanceof Object[] && o2 instanceof Object[]) {
			return Arrays.equals((Object[]) o1, (Object[]) o2);
		}
		if (o1 instanceof boolean[] && o2 instanceof boolean[]) {
			return Arrays.equals((boolean[]) o1, (boolean[]) o2);
		}
		if (o1 instanceof byte[] && o2 instanceof byte[]) {
			return Arrays.equals((byte[]) o1, (byte[]) o2);
		}
		if (o1 instanceof char[] && o2 instanceof char[]) {
			return Arrays.equals((char[]) o1, (char[]) o2);
		}
		if (o1 instanceof double[] && o2 instanceof double[]) {
			return Arrays.equals((double[]) o1, (double[]) o2);
		}
		if (o1 instanceof float[] && o2 instanceof float[]) {
			return Arrays.equals((float[]) o1, (float[]) o2);
		}
		if (o1 instanceof int[] && o2 instanceof int[]) {
			return Arrays.equals((int[]) o1, (int[]) o2);
		}
		if (o1 instanceof long[] && o2 instanceof long[]) {
			return Arrays.equals((long[]) o1, (long[]) o2);
		}
		if (o1 instanceof short[] && o2 instanceof short[]) {
			return Arrays.equals((short[]) o1, (short[]) o2);
		}
		return false;
	}

	/**
	 * 返回给定对象的哈希码；通常是{@code Object#hashCode()}的值。
	 * 如果对象是数组，则委托给本类中对应的{@code nullSafeHashCode}数组方法。
	 * 如果对象为{@code null}，则返回0。
	 *
	 * @see Object#hashCode()
	 * @see #nullSafeHashCode(Object[])
	 * @see #nullSafeHashCode(boolean[])
	 * @see #nullSafeHashCode(byte[])
	 * @see #nullSafeHashCode(char[])
	 * @see #nullSafeHashCode(double[])
	 * @see #nullSafeHashCode(float[])
	 * @see #nullSafeHashCode(int[])
	 * @see #nullSafeHashCode(long[])
	 * @see #nullSafeHashCode(short[])
	 */
	public static int nullSafeHashCode(@Nullable Object obj) {
		if (obj == null) {
			return 0;
		}
		if (obj.getClass().isArray()) {
			if (obj instanceof Object[]) {
				return nullSafeHashCode((Object[]) obj);
			}
			if (obj instanceof boolean[]) {
				return nullSafeHashCode((boolean[]) obj);
			}
			if (obj instanceof byte[]) {
				return nullSafeHashCode((byte[]) obj);
			}
			if (obj instanceof char[]) {
				return nullSafeHashCode((char[]) obj);
			}
			if (obj instanceof double[]) {
				return nullSafeHashCode((double[]) obj);
			}
			if (obj instanceof float[]) {
				return nullSafeHashCode((float[]) obj);
			}
			if (obj instanceof int[]) {
				return nullSafeHashCode((int[]) obj);
			}
			if (obj instanceof long[]) {
				return nullSafeHashCode((long[]) obj);
			}
			if (obj instanceof short[]) {
				return nullSafeHashCode((short[]) obj);
			}
		}
		return obj.hashCode();
	}

	/**
	 * 基于指定对象数组的内容返回哈希码。
	 * 如果{@code array}为{@code null}，则返回0。
	 */
	public static int nullSafeHashCode(@Nullable Object[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (Object element : array) {
			hash = MULTIPLIER * hash + nullSafeHashCode(element);
		}
		return hash;
	}

	/**
	 * 基于指定布尔数组的内容返回哈希码。
	 * 如果{@code array}为{@code null}，则返回0。
	 */
	public static int nullSafeHashCode(@Nullable boolean[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (boolean element : array) {
			hash = MULTIPLIER * hash + Boolean.hashCode(element);
		}
		return hash;
	}

	/**
	 * 基于指定字节数组的内容返回哈希码。
	 * 如果{@code array}为{@code null}，则返回0。
	 */
	public static int nullSafeHashCode(@Nullable byte[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (byte element : array) {
			hash = MULTIPLIER * hash + element;
		}
		return hash;
	}

	/**
	 * 基于指定字符数组的内容返回哈希码。
	 * 如果{@code array}为{@code null}，则返回0。
	 */
	public static int nullSafeHashCode(@Nullable char[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (char element : array) {
			hash = MULTIPLIER * hash + element;
		}
		return hash;
	}

	/**
	 * 基于指定双精度浮点数组的内容返回哈希码。
	 * 如果{@code array}为{@code null}，则返回0。
	 */
	public static int nullSafeHashCode(@Nullable double[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (double element : array) {
			hash = MULTIPLIER * hash + Double.hashCode(element);
		}
		return hash;
	}

	/**
	 * 基于指定单精度浮点数组的内容返回哈希码。
	 * 如果{@code array}为{@code null}，则返回0。
	 */
	public static int nullSafeHashCode(@Nullable float[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (float element : array) {
			hash = MULTIPLIER * hash + Float.hashCode(element);
		}
		return hash;
	}

	/**
	 * 基于指定整型数组的内容返回哈希码。
	 * 如果{@code array}为{@code null}，则返回0。
	 */
	public static int nullSafeHashCode(@Nullable int[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (int element : array) {
			hash = MULTIPLIER * hash + element;
		}
		return hash;
	}

	/**
	 * 基于指定数组的内容返回哈希码。
	 * 如果{@code array}为{@code null}，则返回0。
	 */
	public static int nullSafeHashCode(@Nullable long[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (long element : array) {
			hash = MULTIPLIER * hash + Long.hashCode(element);
		}
		return hash;
	}

	/**
	 * 基于指定数组的内容返回哈希码。
	 * 如果{@code array}为{@code null}，则返回0。
	 */
	public static int nullSafeHashCode(@Nullable short[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (short element : array) {
			hash = MULTIPLIER * hash + element;
		}
		return hash;
	}

	/**
	 * 返回与{@link Boolean#hashCode(boolean)}相同的值。
	 *
	 * @deprecated 自Spring Framework 5.0起，推荐使用JDK 8原生方法
	 */
	@Deprecated
	public static int hashCode(boolean bool) {
		return Boolean.hashCode(bool);
	}

	/**
	 * 返回与{@link Double#hashCode(double)}相同的值。
	 *
	 * @deprecated 自Spring Framework 5.0起，推荐使用JDK 8原生方法
	 */
	@Deprecated
	public static int hashCode(double dbl) {
		return Double.hashCode(dbl);
	}

	/**
	 * 返回与{@link Float#hashCode(float)}相同的值。
	 *
	 * @deprecated 自Spring Framework 5.0起，推荐使用JDK 8原生方法
	 */
	@Deprecated
	public static int hashCode(float flt) {
		return Float.hashCode(flt);
	}

	/**
	 * 返回与{@link Long#hashCode(long)}相同的值。
	 *
	 * @deprecated 自Spring Framework 5.0起，推荐使用JDK 8原生方法
	 */
	@Deprecated
	public static int hashCode(long lng) {
		return Long.hashCode(lng);
	}


	//---------------------------------------------------------------------
	// toString输出的便捷方法
	//---------------------------------------------------------------------

	/**
	 * 返回对象整体标识的字符串表示形式。
	 *
	 * @param obj 对象（可能为{@code null}）
	 * @return 对象的标识字符串表示，若对象为{@code null}则返回空字符串
	 */
	public static String identityToString(@Nullable Object obj) {
		if (obj == null) {
			return EMPTY_STRING;
		}
		return obj.getClass().getName() + "@" + getIdentityHexString(obj);
	}

	/**
	 * 返回对象的标识哈希代码的十六进制字符串形式。
	 *
	 * @param obj 目标对象
	 * @return 十六进制符号中的对象标识代码
	 */
	public static String getIdentityHexString(Object obj) {
		return Integer.toHexString(System.identityHashCode(obj));
	}

	/**
	 * 返回基于对象内容的字符串表示形式（若对象不为null），否则返回空字符串。
	 * <p>与{@link #nullSafeToString(Object)}的区别在于：对于null值，
	 * 本方法返回空字符串而非"null"字符串。
	 *
	 * @param obj 要构建显示字符串的对象
	 * @return 对象的显示字符串表示
	 * @see #nullSafeToString(Object)
	 */
	public static String getDisplayString(@Nullable Object obj) {
		if (obj == null) {
			return EMPTY_STRING;
		}
		return nullSafeToString(obj);
	}

	/**
	 * 获取给定对象的类名。
	 * <p>如果对象为{@code null}，则返回{@code "null"}字符串。
	 *
	 * @param obj 要检查的对象（可能为{@code null}）
	 * @return 对应的类名
	 */
	public static String nullSafeClassName(@Nullable Object obj) {
		return (obj != null ? obj.getClass().getName() : NULL_STRING);
	}

	/**
	 * 返回指定对象的字符串表示形式。
	 * <p>对于数组类型，构建其内容的字符串表示。
	 * 如果对象为{@code null}，则返回{@code "null"}字符串。
	 *
	 * @param obj 要构建字符串表示的对象
	 * @return 对象的字符串表示
	 */
	public static String nullSafeToString(@Nullable Object obj) {
		if (obj == null) {
			return NULL_STRING;
		}
		if (obj instanceof String) {
			return (String) obj;
		}
		if (obj instanceof Object[]) {
			return nullSafeToString((Object[]) obj);
		}
		if (obj instanceof boolean[]) {
			return nullSafeToString((boolean[]) obj);
		}
		if (obj instanceof byte[]) {
			return nullSafeToString((byte[]) obj);
		}
		if (obj instanceof char[]) {
			return nullSafeToString((char[]) obj);
		}
		if (obj instanceof double[]) {
			return nullSafeToString((double[]) obj);
		}
		if (obj instanceof float[]) {
			return nullSafeToString((float[]) obj);
		}
		if (obj instanceof int[]) {
			return nullSafeToString((int[]) obj);
		}
		if (obj instanceof long[]) {
			return nullSafeToString((long[]) obj);
		}
		if (obj instanceof short[]) {
			return nullSafeToString((short[]) obj);
		}
		String str = obj.toString();
		return (str != null ? str : EMPTY_STRING);
	}

	/**
	 * 返回指定对象数组内容的字符串表示形式。
	 * <p>字符串表示形式由数组元素列表组成，包含在大括号({@code "{}"})中。
	 * 相邻元素由字符{@code ", "}(逗号后跟空格)分隔。
	 * 如果{@code array}为{@code null}，则返回{@code "null"}字符串。
	 *
	 * @param array 要构建字符串表示的对象数组
	 * @return 数组的字符串表示形式
	 */
	public static String nullSafeToString(@Nullable Object[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (Object o : array) {
			stringJoiner.add(String.valueOf(o));
		}
		return stringJoiner.toString();
	}

	/**
	 * 返回指定布尔数组内容的字符串表示形式。
	 * <p>字符串表示形式由数组元素列表组成，包含在大括号({@code "{}"})中。
	 * 相邻元素由字符{@code ", "}(逗号后跟空格)分隔。
	 * 如果{@code array}为{@code null}，则返回{@code "null"}字符串。
	 *
	 * @param array 要构建字符串表示的布尔数组
	 * @return 数组的字符串表示形式
	 */
	public static String nullSafeToString(@Nullable boolean[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (boolean b : array) {
			stringJoiner.add(String.valueOf(b));
		}
		return stringJoiner.toString();
	}

	/**
	 * 返回指定字节数组内容的字符串表示形式。
	 * <p>字符串表示形式由数组元素列表组成，包含在大括号({@code "{}"})中。
	 * 相邻元素由字符{@code ", "}(逗号后跟空格)分隔。
	 * 如果{@code array}为{@code null}，则返回{@code "null"}字符串。
	 *
	 * @param array 要构建字符串表示的字节数组
	 * @return 数组的字符串表示形式
	 */
	public static String nullSafeToString(@Nullable byte[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (byte b : array) {
			stringJoiner.add(String.valueOf(b));
		}
		return stringJoiner.toString();
	}

	/**
	 * 返回指定字符数组内容的字符串表示形式。
	 * <p>字符串表示形式由数组元素列表组成，包含在大括号({@code "{}"})中。
	 * 相邻元素由字符{@code ", "}(逗号后跟空格)分隔。
	 * 如果{@code array}为{@code null}，则返回{@code "null"}字符串。
	 * <p>字符元素会使用单引号包裹。
	 *
	 * @param array 要构建字符串表示的字符数组
	 * @return 数组的字符串表示形式
	 */
	public static String nullSafeToString(@Nullable char[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (char c : array) {
			stringJoiner.add('\'' + String.valueOf(c) + '\'');
		}
		return stringJoiner.toString();
	}

	/**
	 * 返回指定双精度浮点数组内容的字符串表示形式。
	 * <p>字符串表示形式由数组元素列表组成，包含在大括号({@code "{}"})中。
	 * 相邻元素由字符{@code ", "}(逗号后跟空格)分隔。
	 * 如果{@code array}为{@code null}，则返回{@code "null"}字符串。
	 *
	 * @param array 要构建字符串表示的双精度浮点数组
	 * @return 数组的字符串表示形式
	 */
	public static String nullSafeToString(@Nullable double[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (double d : array) {
			stringJoiner.add(String.valueOf(d));
		}
		return stringJoiner.toString();
	}

	/**
	 * 返回指定单精度浮点数组内容的字符串表示形式。
	 * <p>字符串表示形式由数组元素列表组成，包含在大括号({@code "{}"})中。
	 * 相邻元素由字符{@code ", "}(逗号后跟空格)分隔。
	 * 如果{@code array}为{@code null}，则返回{@code "null"}字符串。
	 *
	 * @param array 要构建字符串表示的单精度浮点数组
	 * @return 数组的字符串表示形式
	 */
	public static String nullSafeToString(@Nullable float[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (float f : array) {
			stringJoiner.add(String.valueOf(f));
		}
		return stringJoiner.toString();
	}

	/**
	 * 返回指定数组内容的字符串表示形式。
	 * <p>字符串表示形式由数组元素列表组成，包含在大括号({@code "{}"})中。
	 * 相邻元素由字符{@code ", "}(逗号后跟空格)分隔。
	 * 如果{@code array}为{@code null}，则返回{@code "null"}字符串。
	 *
	 * @param array 要构建字符串表示的数组
	 * @return 数组的字符串表示形式
	 */
	public static String nullSafeToString(@Nullable int[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (int i : array) {
			stringJoiner.add(String.valueOf(i));
		}
		return stringJoiner.toString();
	}

	/**
	 * 返回指定数组内容的字符串表示形式。
	 * <p>字符串表示形式由数组元素列表组成，包含在大括号({@code "{}"})中。
	 * 相邻元素由字符{@code ", "}(逗号后跟空格)分隔。
	 * 如果{@code array}为{@code null}，则返回{@code "null"}字符串。
	 *
	 * @param array 要构建字符串表示的数组
	 * @return 数组的字符串表示形式
	 */
	public static String nullSafeToString(@Nullable long[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (long l : array) {
			stringJoiner.add(String.valueOf(l));
		}
		return stringJoiner.toString();
	}

	/**
	 * 返回指定数组内容的字符串表示形式。
	 * <p>字符串表示形式由数组元素列表组成，包含在大括号({@code "{}"})中。
	 * 相邻元素由字符{@code ", "}(逗号后跟空格)分隔。
	 * 如果{@code array}为{@code null}，则返回{@code "null"}字符串。
	 *
	 * @param array 要构建字符串表示的数组
	 * @return 数组的字符串表示形式
	 */
	public static String nullSafeToString(@Nullable short[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (short s : array) {
			stringJoiner.add(String.valueOf(s));
		}
		return stringJoiner.toString();
	}

}
