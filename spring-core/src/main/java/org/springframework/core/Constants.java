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

package org.springframework.core;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 该类用于解析包含常量定义的其他类，这些常量以 public static final 成员存在。
 * 本类的 {@code asXXXX} 方法允许通过字符串名称访问这些常量值。
 *
 * <p>例如，类 Foo 包含 {@code public final static int CONSTANT1 = 66;}，
 * 包装 {@code Foo.class} 的本类实例，调用 {@code asNumber("CONSTANT1")} 将返回 66。
 *
 * <p>本类非常适合用于 PropertyEditors，使其能够识别与常量本身相同的名称，
 * 并且无需自行维护映射关系。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 16.03.2003
 */
public class Constants {

	/** 被内省的类的名称。 */
	private final String className;

	/** 从字段名到字段值的缓存映射。 */
	private final Map<String, Object> fieldCache = new HashMap<>();


	/**
	 * 创建一个包装给定类的 Constants 转换器。
	 * <p>所有 <b>public</b> static final 变量都会被暴露，无论其类型为何。
	 * @param clazz 需要解析的类
	 * @throws IllegalArgumentException 如果传入的 {@code clazz} 为 {@code null}
	 */
	public Constants(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		this.className = clazz.getName();
		Field[] fields = clazz.getFields();
		for (Field field : fields) {
			if (ReflectionUtils.isPublicStaticFinal(field)) {
				String name = field.getName();
				try {
					Object value = field.get(null);
					this.fieldCache.put(name, value);
				}
				catch (IllegalAccessException ex) {
					// 忽略该字段并继续
				}
			}
		}
	}


	/**
	 * 返回被分析类的名称。
	 */
	public final String getClassName() {
		return this.className;
	}

	/**
	 * 返回暴露的常量数量。
	 */
	public final int getSize() {
		return this.fieldCache.size();
	}

	/**
	 * 供子类访问的字段缓存：
	 * 从字段名到对象值的映射。
	 */
	protected final Map<String, Object> getFieldCache() {
		return this.fieldCache;
	}


	/**
	 * 返回对应名称的常量值，转换为 Number 类型。
	 * @param code 字段名（不允许为 {@code null}）
	 * @return Number 类型的值
	 * @throws ConstantException 如果字段名不存在或类型不兼容
	 * @see #asObject
	 */
	public Number asNumber(String code) throws ConstantException {
		Object obj = asObject(code);
		if (!(obj instanceof Number)) {
			throw new ConstantException(this.className, code, "not a Number");
		}
		return (Number) obj;
	}

	/**
	 * 返回对应名称的常量值，转换为字符串。
	 * @param code 字段名（不允许为 {@code null}）
	 * @return 字符串值（如果不是字符串，则调用 {@code toString()}）
	 * @throws ConstantException 如果字段名不存在
	 * @see #asObject
	 */
	public String asString(String code) throws ConstantException {
		return asObject(code).toString();
	}

	/**
	 * 解析给定的字符串（大小写均可），
	 * 并返回对应的常量值，如果它是被分析类中常量字段的名称。
	 * @param code 字段名（不允许为 {@code null}）
	 * @return 常量值对象
	 * @throws ConstantException 如果字段不存在
	 */
	public Object asObject(String code) throws ConstantException {
		Assert.notNull(code, "Code must not be null");
		String codeToUse = code.toUpperCase(Locale.ENGLISH);
		Object val = this.fieldCache.get(codeToUse);
		if (val == null) {
			throw new ConstantException(this.className, codeToUse, "not found");
		}
		return val;
	}


	/**
	 * 返回指定常量组的所有名称。
	 * <p>该方法假设常量名称符合标准 Java 常量命名规范（全部大写）。
	 * 传入的 {@code namePrefix} 会被转成大写（不区分区域设置）后再处理。
	 * @param namePrefix 常量名称的前缀（可为 {@code null}）
	 * @return 常量名称集合
	 */
	public Set<String> getNames(@Nullable String namePrefix) {
		String prefixToUse = (namePrefix != null ? namePrefix.trim().toUpperCase(Locale.ENGLISH) : "");
		Set<String> names = new HashSet<>();
		for (String code : this.fieldCache.keySet()) {
			if (code.startsWith(prefixToUse)) {
				names.add(code);
			}
		}
		return names;
	}

	/**
	 * 返回给定 bean 属性名对应的常量名称集合。
	 * @param propertyName bean 属性名
	 * @return 常量名称集合
	 * @see #propertyToConstantNamePrefix
	 */
	public Set<String> getNamesForProperty(String propertyName) {
		return getNames(propertyToConstantNamePrefix(propertyName));
	}

	/**
	 * 返回指定常量组的所有名称。
	 * <p>该方法假设常量名称符合标准 Java 常量命名规范（全部大写）。
	 * 传入的 {@code nameSuffix} 会被转成大写（不区分区域设置）后再处理。
	 * @param nameSuffix 常量名称的后缀（可为 {@code null}）
	 * @return 常量名称集合
	 */
	public Set<String> getNamesForSuffix(@Nullable String nameSuffix) {
		String suffixToUse = (nameSuffix != null ? nameSuffix.trim().toUpperCase(Locale.ENGLISH) : "");
		Set<String> names = new HashSet<>();
		for (String code : this.fieldCache.keySet()) {
			if (code.endsWith(suffixToUse)) {
				names.add(code);
			}
		}
		return names;
	}


	/**
	 * 返回指定常量组的所有值。
	 * <p>该方法假设常量名称符合标准 Java 常量命名规范（全部大写）。
	 * 传入的 {@code namePrefix} 会被转成大写（不区分区域设置）后再处理。
	 * @param namePrefix 常量名称的前缀（可为 {@code null}）
	 * @return 常量值集合
	 */
	public Set<Object> getValues(@Nullable String namePrefix) {
		String prefixToUse = (namePrefix != null ? namePrefix.trim().toUpperCase(Locale.ENGLISH) : "");
		Set<Object> values = new HashSet<>();
		this.fieldCache.forEach((code, value) -> {
			if (code.startsWith(prefixToUse)) {
				values.add(value);
			}
		});
		return values;
	}

	/**
	 * 返回给定 bean 属性名对应的常量值集合。
	 * @param propertyName bean 属性名
	 * @return 常量值集合
	 * @see #propertyToConstantNamePrefix
	 */
	public Set<Object> getValuesForProperty(String propertyName) {
		return getValues(propertyToConstantNamePrefix(propertyName));
	}

	/**
	 * 返回指定常量组的所有值。
	 * <p>该方法假设常量名称符合标准 Java 常量命名规范（全部大写）。
	 * 传入的 {@code nameSuffix} 会被转成大写（不区分区域设置）后再处理。
	 * @param nameSuffix 常量名称的后缀（可为 {@code null}）
	 * @return 常量值集合
	 */
	public Set<Object> getValuesForSuffix(@Nullable String nameSuffix) {
		String suffixToUse = (nameSuffix != null ? nameSuffix.trim().toUpperCase(Locale.ENGLISH) : "");
		Set<Object> values = new HashSet<>();
		this.fieldCache.forEach((code, value) -> {
			if (code.endsWith(suffixToUse)) {
				values.add(value);
			}
		});
		return values;
	}


	/**
	 * 在指定的常量组中查找给定的值。
	 * <p>返回第一个匹配项。
	 * @param value 要查找的常量值
	 * @param namePrefix 常量名称的前缀（可为 {@code null}）
	 * @return 常量字段的名称
	 * @throws ConstantException 如果未找到对应值
	 */
	public String toCode(Object value, @Nullable String namePrefix) throws ConstantException {
		String prefixToUse = (namePrefix != null ? namePrefix.trim().toUpperCase(Locale.ENGLISH) : "");
		for (Map.Entry<String, Object> entry : this.fieldCache.entrySet()) {
			if (entry.getKey().startsWith(prefixToUse) && entry.getValue().equals(value)) {
				return entry.getKey();
			}
		}
		throw new ConstantException(this.className, prefixToUse, value);
	}

	/**
	 * 在指定 bean 属性名对应的常量组中查找给定的值。
	 * <p>返回第一个匹配项。
	 * @param value 要查找的常量值
	 * @param propertyName bean 属性名
	 * @return 常量字段的名称
	 * @throws ConstantException 如果未找到对应值
	 * @see #propertyToConstantNamePrefix
	 */
	public String toCodeForProperty(Object value, String propertyName) throws ConstantException {
		return toCode(value, propertyToConstantNamePrefix(propertyName));
	}

	/**
	 * 在指定的常量组中查找给定的值。
	 * <p>返回第一个匹配项。
	 * @param value 要查找的常量值
	 * @param nameSuffix 常量名称的后缀（可为 {@code null}）
	 * @return 常量字段的名称
	 * @throws ConstantException 如果未找到对应值
	 */
	public String toCodeForSuffix(Object value, @Nullable String nameSuffix) throws ConstantException {
		String suffixToUse = (nameSuffix != null ? nameSuffix.trim().toUpperCase(Locale.ENGLISH) : "");
		for (Map.Entry<String, Object> entry : this.fieldCache.entrySet()) {
			if (entry.getKey().endsWith(suffixToUse) && entry.getValue().equals(value)) {
				return entry.getKey();
			}
		}
		throw new ConstantException(this.className, suffixToUse, value);
	}


	/**
	 * 将给定的 bean 属性名转换为常量名称前缀。
	 * <p>采用常见命名惯例：将所有小写字符转换为大写，且在大写字符前添加下划线。
	 * <p>示例： "imageSize" → "IMAGE_SIZE"<br>
	 *       "imagesize" → "IMAGESIZE"<br>
	 *       "ImageSize" → "_IMAGE_SIZE"<br>
	 *       "IMAGESIZE" → "_I_M_A_G_E_S_I_Z_E"
	 * @param propertyName bean 属性名
	 * @return 对应的常量名称前缀
	 * @see #getValuesForProperty
	 * @see #toCodeForProperty
	 */
	public String propertyToConstantNamePrefix(String propertyName) {
		StringBuilder parsedPrefix = new StringBuilder();
		for (int i = 0; i < propertyName.length(); i++) {
			char c = propertyName.charAt(i);
			if (Character.isUpperCase(c)) {
				parsedPrefix.append('_');
				parsedPrefix.append(c);
			}
			else {
				parsedPrefix.append(Character.toUpperCase(c));
			}
		}
		return parsedPrefix.toString();
	}


	/**
	 * 当请求无效常量名时抛出该异常。
	 */
	@SuppressWarnings("serial")
	public static class ConstantException extends IllegalArgumentException {

		/**
		 * 当请求无效常量名时抛出。
		 * @param className 含常量定义的类名
		 * @param field 无效的常量名
		 * @param message 问题描述
		 */
		public ConstantException(String className, String field, String message) {
			super("Field '" + field + "' " + message + " in class [" + className + "]");
		}

		/**
		 * 当查找无效常量值时抛出。
		 * @param className 含常量定义的类名
		 * @param namePrefix 查找的常量名前缀
		 * @param value 查找的常量值
		 */
		public ConstantException(String className, String namePrefix, Object value) {
			super("No '" + namePrefix + "' field with value '" + value + "' found in class [" + className + "]");
		}
	}

}
