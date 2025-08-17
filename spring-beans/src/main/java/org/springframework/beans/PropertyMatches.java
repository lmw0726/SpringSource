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

package org.springframework.beans;

import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 根据可配置的距离计算属性匹配的辅助类。提供潜在匹配项列表
 * 和生成错误消息的简便方法。适用于Java Bean属性和字段。
 *
 * <p>主要供框架内部使用，特别是绑定功能。
 *
 * @author Alef Arendsen
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.0
 * @see #forProperty(String, Class)
 * @see #forField(String, Class)
 */
public abstract class PropertyMatches {

	/** 默认最大属性距离：2。 */
	public static final int DEFAULT_MAX_DISTANCE = 2;


	// 静态工厂方法

	/**
	 * 为给定的Bean属性创建PropertyMatches。
	 * @param propertyName 要查找可能匹配项的属性名称
	 * @param beanClass 要搜索匹配项的Bean类
	 */
	public static PropertyMatches forProperty(String propertyName, Class<?> beanClass) {
		return forProperty(propertyName, beanClass, DEFAULT_MAX_DISTANCE);
	}

	/**
	 * 为给定的Bean属性创建PropertyMatches。
	 * @param propertyName 要查找可能匹配项的属性名称
	 * @param beanClass 要搜索匹配项的Bean类
	 * @param maxDistance 匹配项允许的最大属性距离
	 */
	public static PropertyMatches forProperty(String propertyName, Class<?> beanClass, int maxDistance) {
		return new BeanPropertyMatches(propertyName, beanClass, maxDistance);
	}

	/**
	 * 为给定的字段属性创建PropertyMatches。
	 * @param propertyName 要查找可能匹配项的字段名称
	 * @param beanClass 要搜索匹配项的Bean类
	 */
	public static PropertyMatches forField(String propertyName, Class<?> beanClass) {
		return forField(propertyName, beanClass, DEFAULT_MAX_DISTANCE);
	}

	/**
	 * 为给定的字段属性创建PropertyMatches。
	 * @param propertyName 要查找可能匹配项的字段名称
	 * @param beanClass 要搜索匹配项的Bean类
	 * @param maxDistance 匹配项允许的最大属性距离
	 */
	public static PropertyMatches forField(String propertyName, Class<?> beanClass, int maxDistance) {
		return new FieldPropertyMatches(propertyName, beanClass, maxDistance);
	}


	// 实例状态

	private final String propertyName;

	private final String[] possibleMatches;


	/**
	 * 为给定的属性和可能匹配项创建新的PropertyMatches实例。
	 */
	private PropertyMatches(String propertyName, String[] possibleMatches) {
		this.propertyName = propertyName;
		this.possibleMatches = possibleMatches;
	}


	/**
	 * 返回请求的属性名称。
	 */
	public String getPropertyName() {
		return this.propertyName;
	}

	/**
	 * 返回计算出的可能匹配项。
	 */
	public String[] getPossibleMatches() {
		return this.possibleMatches;
	}

	/**
	 * 为给定的无效属性名构建错误消息，
	 * 指示可能的属性匹配项。
	 */
	public abstract String buildErrorMessage();


	// 为子类提供的实现支持

	protected void appendHintMessage(StringBuilder msg) {
		msg.append("Did you mean ");
		for (int i = 0; i < this.possibleMatches.length; i++) {
			msg.append('\'');
			msg.append(this.possibleMatches[i]);
			if (i < this.possibleMatches.length - 2) {
				msg.append("', ");
			}
			else if (i == this.possibleMatches.length - 2) {
				msg.append("', or ");
			}
		}
		msg.append("'?");
	}

	/**
	 * 根据Levenshtein算法计算给定两个字符串之间的距离。
	 * @param s1 第一个字符串
	 * @param s2 第二个字符串
	 * @return 距离值
	 */
	private static int calculateStringDistance(String s1, String s2) {
		if (s1.isEmpty()) {
			return s2.length();
		}
		if (s2.isEmpty()) {
			return s1.length();
		}

		int[][] d = new int[s1.length() + 1][s2.length() + 1];
		for (int i = 0; i <= s1.length(); i++) {
			d[i][0] = i;
		}
		for (int j = 0; j <= s2.length(); j++) {
			d[0][j] = j;
		}

		for (int i = 1; i <= s1.length(); i++) {
			char c1 = s1.charAt(i - 1);
			for (int j = 1; j <= s2.length(); j++) {
				int cost;
				char c2 = s2.charAt(j - 1);
				if (c1 == c2) {
					cost = 0;
				}
				else {
					cost = 1;
				}
				d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
			}
		}

		return d[s1.length()][s2.length()];
	}


	// 具体子类

	private static class BeanPropertyMatches extends PropertyMatches {

		public BeanPropertyMatches(String propertyName, Class<?> beanClass, int maxDistance) {
			super(propertyName,
					calculateMatches(propertyName, BeanUtils.getPropertyDescriptors(beanClass), maxDistance));
		}

		/**
		 * 为给定的属性和类生成可能的属性替代项。
		 * 内部使用 {@code getStringDistance} 方法，该方法使用
		 * Levenshtein算法来确定两个字符串之间的距离。
		 * @param descriptors 要搜索的JavaBeans属性描述符
		 * @param maxDistance 可接受的最大距离
		 */
		private static String[] calculateMatches(String name, PropertyDescriptor[] descriptors, int maxDistance) {
			List<String> candidates = new ArrayList<>();
			for (PropertyDescriptor pd : descriptors) {
				if (pd.getWriteMethod() != null) {
					String possibleAlternative = pd.getName();
					if (calculateStringDistance(name, possibleAlternative) <= maxDistance) {
						candidates.add(possibleAlternative);
					}
				}
			}
			Collections.sort(candidates);
			return StringUtils.toStringArray(candidates);
		}

		@Override
		public String buildErrorMessage() {
			StringBuilder msg = new StringBuilder(160);
			msg.append("Bean property '").append(getPropertyName()).append(
					"' is not writable or has an invalid setter method. ");
			if (!ObjectUtils.isEmpty(getPossibleMatches())) {
				appendHintMessage(msg);
			}
			else {
				msg.append("Does the parameter type of the setter match the return type of the getter?");
			}
			return msg.toString();
		}
	}


	private static class FieldPropertyMatches extends PropertyMatches {

		public FieldPropertyMatches(String propertyName, Class<?> beanClass, int maxDistance) {
			super(propertyName, calculateMatches(propertyName, beanClass, maxDistance));
		}

		private static String[] calculateMatches(final String name, Class<?> clazz, final int maxDistance) {
			final List<String> candidates = new ArrayList<>();
			ReflectionUtils.doWithFields(clazz, field -> {
				String possibleAlternative = field.getName();
				if (calculateStringDistance(name, possibleAlternative) <= maxDistance) {
					candidates.add(possibleAlternative);
				}
			});
			Collections.sort(candidates);
			return StringUtils.toStringArray(candidates);
		}

		@Override
		public String buildErrorMessage() {
			StringBuilder msg = new StringBuilder(80);
			msg.append("Bean property '").append(getPropertyName()).append("' has no matching field.");
			if (!ObjectUtils.isEmpty(getPossibleMatches())) {
				msg.append(' ');
				appendHintMessage(msg);
			}
			return msg.toString();
		}
	}

}
