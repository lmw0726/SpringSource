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

package org.springframework.web.bind;

import org.springframework.lang.Nullable;

import javax.servlet.ServletRequest;

/**
 * 参数提取方法，用于与数据绑定不同的方法，其中需要特定类型的参数。
 *
 * <p>这种方法非常适用于简单的提交，其中将请求参数绑定到命令对象会过于繁琐。
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @since 2.0
 */
public abstract class ServletRequestUtils {

	/**
	 * 解析整数的工具类实例。
	 */
	private static final IntParser INT_PARSER = new IntParser();

	/**
	 * 解析长整数的工具类实例。
	 */
	private static final LongParser LONG_PARSER = new LongParser();

	/**
	 * 解析浮点数的工具类实例。
	 */
	private static final FloatParser FLOAT_PARSER = new FloatParser();

	/**
	 * 解析双精度浮点数的工具类实例。
	 */
	private static final DoubleParser DOUBLE_PARSER = new DoubleParser();

	/**
	 * 解析布尔值的工具类实例。
	 */
	private static final BooleanParser BOOLEAN_PARSER = new BooleanParser();

	/**
	 * 解析字符串的工具类实例。
	 */
	private static final StringParser STRING_PARSER = new StringParser();


	/**
	 * 获取一个Integer参数，如果不存在则返回{@code null}。
	 * 如果参数值不是数字，则抛出异常。
	 *
	 * @param request 当前HTTP请求
	 * @param name    参数的名称
	 * @return Integer值，如果不存在则返回{@code null}
	 * @throws ServletRequestBindingException ServletException的子类，因此不需要捕获
	 */
	@Nullable
	public static Integer getIntParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		// 如果请求参数中指定名称的值为空，则返回 null
		if (request.getParameter(name) == null) {
			return null;
		}

		// 否则调用 getRequiredIntParameter 方法获取必需的整型参数并返回
		return getRequiredIntParameter(request, name);
	}

	/**
	 * 获取一个int参数，并提供回退值。永远不会抛出异常。
	 * 可以传递一个特殊的默认值来启用检查是否已提供。
	 *
	 * @param request    当前HTTP请求
	 * @param name       参数的名称
	 * @param defaultVal 回退的默认值
	 */
	public static int getIntParameter(ServletRequest request, String name, int defaultVal) {
		// 如果请求参数为空，则返回默认值
		if (request.getParameter(name) == null) {
			return defaultVal;
		}
		try {
			// 尝试获取必需的整型参数
			return getRequiredIntParameter(request, name);
		} catch (ServletRequestBindingException ex) {
			// 如果捕获到ServletRequestBindingException异常，则返回默认值
			return defaultVal;
		}
	}

	/**
	 * 获取int参数的数组，如果未找到则返回空数组。
	 *
	 * @param request 当前HTTP请求
	 * @param name    具有多个可能值的参数的名称
	 */
	public static int[] getIntParameters(ServletRequest request, String name) {
		try {
			// 尝试获取必需的整型参数
			return getRequiredIntParameters(request, name);
		} catch (ServletRequestBindingException ex) {
			// 如果发生 ServletRequestBindingException 异常，则返回空整型数组
			return new int[0];
		}
	}

	/**
	 * 获取一个int参数，如果未找到或不是数字，则抛出异常。
	 *
	 * @param request 当前HTTP请求
	 * @param name    参数的名称
	 * @throws ServletRequestBindingException ServletException的子类，
	 *                                        因此不需要捕获
	 */
	public static int getRequiredIntParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return INT_PARSER.parseInt(name, request.getParameter(name));
	}

	/**
	 * 获取int参数的数组，如果未找到或有一个不是数字，则抛出异常。
	 *
	 * @param request 当前HTTP请求
	 * @param name    具有多个可能值的参数的名称
	 * @throws ServletRequestBindingException ServletException的子类，
	 *                                        因此不需要捕获
	 */
	public static int[] getRequiredIntParameters(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return INT_PARSER.parseInts(name, request.getParameterValues(name));
	}


	/**
	 * 获取一个Long参数，如果不存在则返回{@code null}。
	 * 如果参数值不是数字，则抛出异常。
	 *
	 * @param request 当前HTTP请求
	 * @param name    参数的名称
	 * @return Long值，如果不存在则返回{@code null}
	 * @throws ServletRequestBindingException ServletException的子类，
	 *                                        因此不需要捕获
	 */
	@Nullable
	public static Long getLongParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		// 如果请求参数中指定名称的值为空，则返回 null
		if (request.getParameter(name) == null) {
			return null;
		}

		// 否则调用 getRequiredLongParameter 方法获取必需的长整型参数并返回
		return getRequiredLongParameter(request, name);
	}

	/**
	 * 获取一个long参数，并提供回退值。永远不会抛出异常。
	 * 可以传递一个特殊的默认值来启用检查是否已提供。
	 *
	 * @param request    当前HTTP请求
	 * @param name       参数的名称
	 * @param defaultVal 用作回退的默认值
	 */
	public static long getLongParameter(ServletRequest request, String name, long defaultVal) {
		// 如果请求参数为空，则返回默认值
		if (request.getParameter(name) == null) {
			return defaultVal;
		}
		try {
			// 尝试获取必需的长整型参数
			return getRequiredLongParameter(request, name);
		} catch (ServletRequestBindingException ex) {
			// 如果捕获到ServletRequestBindingException异常，则返回默认值
			return defaultVal;
		}
	}

	/**
	 * 获取long参数的数组，如果未找到则返回空数组。
	 *
	 * @param request 当前HTTP请求
	 * @param name    具有多个可能值的参数的名称
	 */
	public static long[] getLongParameters(ServletRequest request, String name) {
		try {
			// 尝试获取必需的长整型参数
			return getRequiredLongParameters(request, name);
		} catch (ServletRequestBindingException ex) {
			// 如果发生 ServletRequestBindingException 异常，则返回空长整型数组
			return new long[0];
		}
	}

	/**
	 * 获取一个long参数，如果未找到或不是数字，则抛出异常。
	 *
	 * @param request 当前HTTP请求
	 * @param name    参数的名称
	 * @throws ServletRequestBindingException ServletException的子类，
	 *                                        因此不需要捕获
	 */
	public static long getRequiredLongParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return LONG_PARSER.parseLong(name, request.getParameter(name));
	}

	/**
	 * 获取long参数的数组，如果未找到或其中一个不是数字，则抛出异常。
	 *
	 * @param request 当前HTTP请求
	 * @param name    具有多个可能值的参数的名称
	 * @throws ServletRequestBindingException ServletException的子类，
	 *                                        因此不需要捕获
	 */
	public static long[] getRequiredLongParameters(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return LONG_PARSER.parseLongs(name, request.getParameterValues(name));
	}


	/**
	 * 获取一个Float参数，如果不存在则返回{@code null}。
	 * 如果参数值不是数字，则抛出异常。
	 *
	 * @param request 当前HTTP请求
	 * @param name    参数的名称
	 * @return Float值，如果不存在则返回{@code null}
	 * @throws ServletRequestBindingException ServletException的子类，
	 *                                        因此不需要捕获
	 */
	@Nullable
	public static Float getFloatParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		// 如果请求参数中指定名称的值为空，则返回 null
		if (request.getParameter(name) == null) {
			return null;
		}

		// 否则调用 getRequiredFloatParameter 方法获取必需的浮点数参数并返回
		return getRequiredFloatParameter(request, name);
	}

	/**
	 * 获取一个float参数，并提供回退值。永远不会抛出异常。
	 * 可以传递一个特殊的默认值来启用检查是否已提供。
	 *
	 * @param request    当前HTTP请求
	 * @param name       参数的名称
	 * @param defaultVal 用作回退的默认值
	 */
	public static float getFloatParameter(ServletRequest request, String name, float defaultVal) {
		// 如果请求参数为空，则返回默认值
		if (request.getParameter(name) == null) {
			return defaultVal;
		}
		try {
			// 尝试获取必需的浮点型参数
			return getRequiredFloatParameter(request, name);
		} catch (ServletRequestBindingException ex) {
			// 如果捕获到ServletRequestBindingException异常，则返回默认值
			return defaultVal;
		}
	}

	/**
	 * 获取float参数的数组，如果未找到则返回空数组。
	 *
	 * @param request 当前HTTP请求
	 * @param name    具有多个可能值的参数的名称
	 */
	public static float[] getFloatParameters(ServletRequest request, String name) {
		try {
			// 尝试获取必需的浮点型参数
			return getRequiredFloatParameters(request, name);
		} catch (ServletRequestBindingException ex) {
			// 如果捕获到ServletRequestBindingException异常，则返回空的浮点型数组
			return new float[0];
		}
	}

	/**
	 * 获取一个float参数，如果未找到或不是数字，则抛出异常。
	 *
	 * @param request 当前HTTP请求
	 * @param name    参数的名称
	 * @throws ServletRequestBindingException ServletException的子类，
	 *                                        因此不需要捕获
	 */
	public static float getRequiredFloatParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return FLOAT_PARSER.parseFloat(name, request.getParameter(name));
	}

	/**
	 * 获取float参数的数组，如果未找到或其中一个不是数字，则抛出异常。
	 *
	 * @param request 当前HTTP请求
	 * @param name    具有多个可能值的参数的名称
	 * @throws ServletRequestBindingException ServletException的子类，
	 *                                        因此不需要捕获
	 */
	public static float[] getRequiredFloatParameters(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return FLOAT_PARSER.parseFloats(name, request.getParameterValues(name));
	}


	/**
	 * 获取一个Double参数，如果不存在则返回{@code null}。
	 * 如果参数值不是数字，则抛出异常。
	 *
	 * @param request 当前HTTP请求
	 * @param name    参数的名称
	 * @return Double值，如果不存在则返回{@code null}
	 * @throws ServletRequestBindingException ServletException的子类，
	 *                                        因此不需要捕获
	 */
	@Nullable
	public static Double getDoubleParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		// 如果请求参数为空，则返回null
		if (request.getParameter(name) == null) {
			return null;
		}
		// 返回必需的双精度浮点型参数
		return getRequiredDoubleParameter(request, name);
	}

	/**
	 * 获取一个double参数，并提供回退值。永远不会抛出异常。
	 * 可以传递一个特殊的默认值来启用检查是否已提供。
	 *
	 * @param request    当前HTTP请求
	 * @param name       参数的名称
	 * @param defaultVal 用作回退的默认值
	 */
	public static double getDoubleParameter(ServletRequest request, String name, double defaultVal) {
		// 如果请求参数为空，则返回默认值
		if (request.getParameter(name) == null) {
			return defaultVal;
		}
		try {
			// 尝试获取必需的双精度浮点型参数
			return getRequiredDoubleParameter(request, name);
		} catch (ServletRequestBindingException ex) {
			// 如果捕获到ServletRequestBindingException异常，则返回默认值
			return defaultVal;
		}
	}

	/**
	 * 获取double参数的数组，如果未找到则返回空数组。
	 *
	 * @param request 当前HTTP请求
	 * @param name    具有多个可能值的参数的名称
	 */
	public static double[] getDoubleParameters(ServletRequest request, String name) {
		try {
			// 尝试获取必需的双精度浮点型参数
			return getRequiredDoubleParameters(request, name);
		} catch (ServletRequestBindingException ex) {
			// 如果捕获到ServletRequestBindingException异常，则返回空的双精度浮点型数组
			return new double[0];
		}
	}

	/**
	 * 获取一个double参数，如果未找到或不是数字，则抛出异常。
	 *
	 * @param request 当前HTTP请求
	 * @param name    参数的名称
	 * @throws ServletRequestBindingException ServletException的子类，因此不需要捕获
	 */
	public static double getRequiredDoubleParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return DOUBLE_PARSER.parseDouble(name, request.getParameter(name));
	}

	/**
	 * 获取double参数的数组，如果未找到或其中一个不是数字，则抛出异常。
	 *
	 * @param request 当前HTTP请求
	 * @param name    具有多个可能值的参数的名称
	 * @throws ServletRequestBindingException ServletException的子类，因此不需要捕获
	 */
	public static double[] getRequiredDoubleParameters(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return DOUBLE_PARSER.parseDoubles(name, request.getParameterValues(name));
	}


	/**
	 * 获取一个Boolean参数，如果不存在则返回{@code null}。
	 * 如果参数值不是布尔值，则抛出异常。
	 * <p>接受"true"、"on"、"yes"（任何大小写）和"1"作为true的值；
	 * 将每个其他非空值都视为false（即宽松解析）。
	 *
	 * @param request 当前HTTP请求
	 * @param name    参数的名称
	 * @return Boolean值，如果不存在则返回{@code null}
	 * @throws ServletRequestBindingException ServletException的子类，因此不需要捕获
	 */
	@Nullable
	public static Boolean getBooleanParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		// 如果请求参数为空，则返回null
		if (request.getParameter(name) == null) {
			return null;
		}
		// 返回必需的布尔型参数
		return getRequiredBooleanParameter(request, name);
	}

	/**
	 * 获取一个boolean参数，并提供回退值。永远不会抛出异常。
	 * 可以传递一个特殊的默认值来启用检查是否已提供。
	 * <p>接受"true"、"on"、"yes"（任何大小写）和"1"作为true的值；
	 * 将每个其他非空值都视为false（即宽松解析）。
	 *
	 * @param request    current HTTP request
	 * @param name       参数的名称
	 * @param defaultVal 用作回退的默认值
	 */
	public static boolean getBooleanParameter(ServletRequest request, String name, boolean defaultVal) {
		if (request.getParameter(name) == null) {
			// 如果请求参数为空，则返回默认值
			return defaultVal;
		}
		try {
			// 尝试获取必需的布尔型参数
			return getRequiredBooleanParameter(request, name);
		} catch (ServletRequestBindingException ex) {
			// 如果捕获到ServletRequestBindingException异常，则返回默认值
			return defaultVal;
		}
	}

	/**
	 * 获取一个布尔参数数组，如果未找到则返回空数组。
	 * <p>接受 "true", "on", "yes" （任何大小写）和 "1" 作为 true 的值；
	 * 将其他非空值视为 false（即宽松解析）。
	 *
	 * @param request 当前HTTP请求
	 * @param name    具有多个可能值的参数的名称
	 * @return 包含布尔值的数组
	 */
	public static boolean[] getBooleanParameters(ServletRequest request, String name) {
		try {
			// 尝试获取必需的布尔参数
			return getRequiredBooleanParameters(request, name);
		} catch (ServletRequestBindingException ex) {
			// 如果发生 ServletRequestBindingException 异常，则返回空布尔数组
			return new boolean[0];
		}
	}

	/**
	 * 获取一个布尔参数，如果未找到或不是布尔值则抛出异常。
	 * <p>接受 "true", "on", "yes" （任何大小写）和 "1" 作为 true 的值；
	 * 将其他非空值视为 false（即宽松解析）。
	 *
	 * @param request 当前HTTP请求
	 * @param name    参数的名称
	 * @return 布尔值
	 * @throws ServletRequestBindingException 继承自 ServletException，因此无需捕获
	 */
	public static boolean getRequiredBooleanParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return BOOLEAN_PARSER.parseBoolean(name, request.getParameter(name));
	}

	/**
	 * 获取一个布尔参数数组，如果未找到或有一个不是布尔值则抛出异常。
	 * <p>接受 "true", "on", "yes" （任何大小写）和 "1" 作为 true 的值；
	 * 将其他非空值视为 false（即宽松解析）。
	 *
	 * @param request 当前HTTP请求
	 * @param name    参数的名称
	 * @return 包含布尔值的数组
	 * @throws ServletRequestBindingException 继承自 ServletException，因此无需捕获
	 */
	public static boolean[] getRequiredBooleanParameters(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return BOOLEAN_PARSER.parseBooleans(name, request.getParameterValues(name));
	}


	/**
	 * 获取一个字符串参数，如果不存在则返回 {@code null}。
	 *
	 * @param request 当前HTTP请求
	 * @param name    参数的名称
	 * @return 字符串值，如果不存在则为 {@code null}
	 * @throws ServletRequestBindingException 继承自 ServletException，因此无需捕获
	 */
	@Nullable
	public static String getStringParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		// 如果请求参数中指定名称的值为空，则返回 null
		if (request.getParameter(name) == null) {
			return null;
		}

		// 否则调用 getRequiredStringParameter 方法获取必需的字符串参数并返回
		return getRequiredStringParameter(request, name);
	}

	/**
	 * 获取一个字符串参数，如果不存在则返回指定的默认值。永远不会抛出异常。
	 *
	 * @param request    当前HTTP请求
	 * @param name       参数的名称
	 * @param defaultVal 默认值，用作后备
	 * @return 参数值或默认值
	 */
	public static String getStringParameter(ServletRequest request, String name, String defaultVal) {
		// 获取请求参数的值
		String val = request.getParameter(name);

		// 如果值不为空，则返回该值；否则返回默认值
		return (val != null ? val : defaultVal);
	}

	/**
	 * 获取一个字符串参数数组，如果未找到则返回空数组。
	 *
	 * @param request 当前HTTP请求
	 * @param name    具有多个可能值的参数的名称
	 * @return 包含参数值的数组
	 */
	public static String[] getStringParameters(ServletRequest request, String name) {
		try {
			// 尝试获取必需的字符串参数
			return getRequiredStringParameters(request, name);
		} catch (ServletRequestBindingException ex) {
			// 如果发生 ServletRequestBindingException 异常，则返回空数组
			return new String[0];
		}
	}

	/**
	 * 获取一个字符串参数，如果未找到则抛出异常。
	 *
	 * @param request 当前HTTP请求
	 * @param name    参数的名称
	 * @return 参数值
	 * @throws ServletRequestBindingException 继承自 ServletException，因此无需捕获
	 */
	public static String getRequiredStringParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return STRING_PARSER.validateRequiredString(name, request.getParameter(name));
	}

	/**
	 * 获取一个字符串参数数组，如果未找到则抛出异常。
	 *
	 * @param request 当前HTTP请求
	 * @param name    参数的名称
	 * @return 包含参数值的数组
	 * @throws ServletRequestBindingException 继承自 ServletException，因此无需捕获
	 */
	public static String[] getRequiredStringParameters(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return STRING_PARSER.validateRequiredStrings(name, request.getParameterValues(name));
	}


	private abstract static class ParameterParser<T> {

		protected final T parse(String name, String parameter) throws ServletRequestBindingException {
			// 验证必需的参数
			validateRequiredParameter(name, parameter);

			try {
				// 尝试解析参数
				return doParse(parameter);
			} catch (NumberFormatException ex) {
				// 如果解析失败，则抛出 ServletRequestBindingException 异常
				throw new ServletRequestBindingException(
						"Required " + getType() + " parameter '" + name + "' with value of '" +
								parameter + "' is not a valid number", ex);
			}
		}

		protected final void validateRequiredParameter(String name, @Nullable Object parameter)
				throws ServletRequestBindingException {

			if (parameter == null) {
				// 如果参数为空，则抛出异常
				throw new MissingServletRequestParameterException(name, getType());
			}
		}

		protected abstract String getType();

		protected abstract T doParse(String parameter) throws NumberFormatException;
	}


	private static class IntParser extends ParameterParser<Integer> {

		@Override
		protected String getType() {
			return "int";
		}

		@Override
		protected Integer doParse(String s) throws NumberFormatException {
			return Integer.valueOf(s);
		}

		public int parseInt(String name, String parameter) throws ServletRequestBindingException {
			return parse(name, parameter);
		}

		public int[] parseInts(String name, String[] values) throws ServletRequestBindingException {
			// 验证必需参数
			validateRequiredParameter(name, values);
			// 创建整型数组
			int[] parameters = new int[values.length];
			// 遍历值列表
			for (int i = 0; i < values.length; i++) {
				// 逐个解析为整型并存储在数组中
				parameters[i] = parseInt(name, values[i]);
			}
			// 返回整型数组
			return parameters;
		}
	}


	private static class LongParser extends ParameterParser<Long> {

		@Override
		protected String getType() {
			return "long";
		}

		@Override
		protected Long doParse(String parameter) throws NumberFormatException {
			return Long.valueOf(parameter);
		}

		public long parseLong(String name, String parameter) throws ServletRequestBindingException {
			return parse(name, parameter);
		}

		public long[] parseLongs(String name, String[] values) throws ServletRequestBindingException {
			// 验证必需参数
			validateRequiredParameter(name, values);
			// 创建长整型数组
			long[] parameters = new long[values.length];
			// 遍历值列表
			for (int i = 0; i < values.length; i++) {
				// 逐个解析为长整型并存储在数组中
				parameters[i] = parseLong(name, values[i]);
			}
			// 返回长整型数组
			return parameters;
		}
	}


	private static class FloatParser extends ParameterParser<Float> {

		@Override
		protected String getType() {
			return "float";
		}

		@Override
		protected Float doParse(String parameter) throws NumberFormatException {
			return Float.valueOf(parameter);
		}

		public float parseFloat(String name, String parameter) throws ServletRequestBindingException {
			return parse(name, parameter);
		}

		public float[] parseFloats(String name, String[] values) throws ServletRequestBindingException {
			// 验证必需参数
			validateRequiredParameter(name, values);
			// 创建浮点型数组
			float[] parameters = new float[values.length];
			// 遍历值列表
			for (int i = 0; i < values.length; i++) {
				// 逐个解析为浮点型并存储在数组中
				parameters[i] = parseFloat(name, values[i]);
			}
			// 返回浮点型数组
			return parameters;
		}
	}


	private static class DoubleParser extends ParameterParser<Double> {

		@Override
		protected String getType() {
			return "double";
		}

		@Override
		protected Double doParse(String parameter) throws NumberFormatException {
			return Double.valueOf(parameter);
		}

		public double parseDouble(String name, String parameter) throws ServletRequestBindingException {
			return parse(name, parameter);
		}

		public double[] parseDoubles(String name, String[] values) throws ServletRequestBindingException {
			// 验证必需参数
			validateRequiredParameter(name, values);
			// 创建双精度浮点型数组
			double[] parameters = new double[values.length];
			// 遍历值列表
			for (int i = 0; i < values.length; i++) {
				// 逐个解析为双精度浮点型并存储在数组中
				parameters[i] = parseDouble(name, values[i]);
			}
			// 返回双精度浮点型数组
			return parameters;
		}
	}


	private static class BooleanParser extends ParameterParser<Boolean> {

		@Override
		protected String getType() {
			return "boolean";
		}

		@Override
		protected Boolean doParse(String parameter) throws NumberFormatException {
			return (parameter.equalsIgnoreCase("true") || parameter.equalsIgnoreCase("on") ||
					parameter.equalsIgnoreCase("yes") || parameter.equals("1"));
		}

		public boolean parseBoolean(String name, String parameter) throws ServletRequestBindingException {
			return parse(name, parameter);
		}

		public boolean[] parseBooleans(String name, String[] values) throws ServletRequestBindingException {
			// 验证必需参数
			validateRequiredParameter(name, values);
			// 创建布尔型数组
			boolean[] parameters = new boolean[values.length];
			// 遍历值列表
			for (int i = 0; i < values.length; i++) {
				// 逐个解析为布尔型并存储在数组中
				parameters[i] = parseBoolean(name, values[i]);
			}
			// 返回布尔型数组
			return parameters;
		}
	}


	private static class StringParser extends ParameterParser<String> {

		@Override
		protected String getType() {
			return "string";
		}

		@Override
		protected String doParse(String parameter) throws NumberFormatException {
			return parameter;
		}

		public String validateRequiredString(String name, String value) throws ServletRequestBindingException {
			// 验证必需参数
			validateRequiredParameter(name, value);
			// 返回值
			return value;
		}

		public String[] validateRequiredStrings(String name, String[] values) throws ServletRequestBindingException {
			// 验证必需参数
			validateRequiredParameter(name, values);
			// 遍历值列表
			for (String value : values) {
				// 逐个验证必需参数
				validateRequiredParameter(name, value);
			}
			// 返回值列表
			return values;
		}
	}

}
