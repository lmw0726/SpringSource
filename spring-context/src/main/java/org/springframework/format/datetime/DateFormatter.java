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

package org.springframework.format.datetime;

import org.springframework.format.Formatter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * {@link java.util.Date} 类型的格式化器。
 * <p>支持配置显式日期时间模式、时区、语言环境以及宽松解析的回退日期时间模式。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @see SimpleDateFormat
 * @since 3.0
 */
public class DateFormatter implements Formatter<Date> {
	/**
	 * 默认时区为标准时区
	 */
	private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
	/**
	 * ISO枚举和时间格式映射
	 */
	private static final Map<ISO, String> ISO_PATTERNS;

	static {
		// 添加ISO枚举和时间格式映射
		Map<ISO, String> formats = new EnumMap<>(ISO.class);
		formats.put(ISO.DATE, "yyyy-MM-dd");
		formats.put(ISO.TIME, "HH:mm:ss.SSSXXX");
		formats.put(ISO.DATE_TIME, "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		ISO_PATTERNS = Collections.unmodifiableMap(formats);
	}

	/**
	 * 源对象。
	 */
	@Nullable
	private Object source;

	/**
	 * 格式模式。
	 */
	@Nullable
	private String pattern;

	/**
	 * 回退模式数组。
	 */
	@Nullable
	private String[] fallbackPatterns;

	/**
	 * 样式，使用 {@link DateFormat} 中的常量，默认为 {@link DateFormat#DEFAULT}。
	 */
	private int style = DateFormat.DEFAULT;

	/**
	 * 样式模式。
	 */
	@Nullable
	private String stylePattern;

	/**
	 * ISO 格式类型。
	 */
	@Nullable
	private ISO iso;

	/**
	 * 时区。
	 */
	@Nullable
	private TimeZone timeZone;

	/**
	 * 是否宽松模式，默认为 false。
	 */
	private boolean lenient = false;


	/**
	 * 创建一个新的默认 {@code DateFormatter}。
	 */
	public DateFormatter() {
	}

	/**
	 * 使用给定的日期时间模式创建一个新的 {@code DateFormatter}。
	 */
	public DateFormatter(String pattern) {
		this.pattern = pattern;
	}


	/**
	 * 设置配置此 {@code DateFormatter} 的源 &mdash;
	 * 例如，如果使用了 {@link DateTimeFormat @DateTimeFormat} 注解来配置此 {@code DateFormatter}，
	 * 则源可以是该注解的实例。
	 * <p>提供的源对象仅用于描述性目的，通过调用其 {@code toString()} 方法使用 &mdash;
	 * 例如，在生成异常消息以提供进一步上下文时。
	 *
	 * @param source 配置的源
	 * @since 5.3.5
	 */
	public void setSource(Object source) {
		this.source = source;
	}

	/**
	 * 设置用于格式化日期值的模式。
	 * <p>如果未指定，则将使用 DateFormat 的默认样式。
	 *
	 * @param pattern 要使用的模式
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * 设置作为备用的附加模式，用于在解析配置的{@linkplain #setPattern 模式}、{@linkplain #setIso ISO格式}、
	 * {@linkplain #setStyle 样式}或{@linkplain #setStylePattern 样式模式}失败时使用。
	 *
	 * @param fallbackPatterns 备用解析模式
	 * @see DateTimeFormat#fallbackPatterns()
	 * @since 5.3.5
	 */
	public void setFallbackPatterns(String... fallbackPatterns) {
		this.fallbackPatterns = fallbackPatterns;
	}

	/**
	 * 设置用于格式化日期值的 ISO 格式。
	 *
	 * @param iso {@link ISO} 格式
	 * @since 3.2
	 */
	public void setIso(ISO iso) {
		this.iso = iso;
	}

	/**
	 * 设置用于格式化日期值的 {@link DateFormat} 样式。
	 * <p>如果未指定，则将使用 DateFormat 的默认样式。
	 *
	 * @see DateFormat#DEFAULT
	 * @see DateFormat#SHORT
	 * @see DateFormat#MEDIUM
	 * @see DateFormat#LONG
	 * @see DateFormat#FULL
	 */
	public void setStyle(int style) {
		this.style = style;
	}

	/**
	 * 设置用于格式化日期值的两个字符。
	 * <p>第一个字符用于日期样式；第二个字符用于时间样式。
	 * <p>支持的字符：
	 * <ul>
	 * <li>'S' = 小</li>
	 * <li>'M' = 中</li>
	 * <li>'L' = 长</li>
	 * <li>'F' = 完整</li>
	 * <li>'-' = 省略</li>
	 * </ul>
	 * 该方法模仿了 Joda-Time 支持的样式。
	 *
	 * @param stylePattern 来自集合 {"S", "M", "L", "F", "-"} 的两个字符
	 * @since 3.2
	 */
	public void setStylePattern(String stylePattern) {
		this.stylePattern = stylePattern;
	}

	/**
	 * 设置用于将日期值规范化的 {@link TimeZone}（如果有）。
	 */
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * 指定是否解析为宽松模式。默认值为 false。
	 * <p>使用宽松解析，解析器可以允许与格式不完全匹配的输入。
	 * 使用严格解析，输入必须与格式完全匹配。
	 */
	public void setLenient(boolean lenient) {
		this.lenient = lenient;
	}


	@Override
	public String print(Date date, Locale locale) {
		return getDateFormat(locale).format(date);
	}

	@Override
	public Date parse(String text, Locale locale) throws ParseException {
		// 尝试使用指定的语言环境解析文本为日期对象
		try {
			return getDateFormat(locale).parse(text);
		} catch (ParseException ex) {
			// 如果存在备用模式，则尝试使用备用模式进行解析
			if (!ObjectUtils.isEmpty(this.fallbackPatterns)) {
				for (String pattern : this.fallbackPatterns) {
					try {
						// 配置日期格式
						DateFormat dateFormat = configureDateFormat(new SimpleDateFormat(pattern, locale));
						// 如果设置了ISO，则将时区设置为UTC以与打印格式对齐
						if (this.iso != null && this.iso != ISO.NONE) {
							dateFormat.setTimeZone(UTC);
						}
						return dateFormat.parse(text);
					} catch (ParseException ignoredException) {
						// 忽略备用解析异常，因为下面抛出的异常将包含来自“source”的信息（如果可用）-- 例如，@DateTimeFormat注解的toString()。
					}
				}
			}
			// 如果存在源对象，则构造解析异常并抛出
			if (this.source != null) {
				ParseException parseException = new ParseException(
						String.format("Unable to parse date time value \"%s\" using configuration from %s", text, this.source),
						ex.getErrorOffset());
				parseException.initCause(ex);
				throw parseException;
			}
			// 否则重新抛出原始异常
			throw ex;
		}
	}


	/**
	 * 获取给定区域设置的 {@link DateFormat}。
	 *
	 * @param locale 区域设置
	 * @return 配置后的 {@link DateFormat}
	 */
	protected DateFormat getDateFormat(Locale locale) {
		return configureDateFormat(createDateFormat(locale));
	}

	/**
	 * 配置给定的 {@link DateFormat}。
	 *
	 * @param dateFormat 要配置的 {@link DateFormat} 实例
	 * @return 配置后的 {@link DateFormat} 实例
	 */
	private DateFormat configureDateFormat(DateFormat dateFormat) {
		// 如果设置了时区，则设置日期格式的时区
		if (this.timeZone != null) {
			dateFormat.setTimeZone(this.timeZone);
		}
		// 设置日期格式的严格模式
		dateFormat.setLenient(this.lenient);
		return dateFormat;
	}

	/**
	 * 创建给定区域设置的 {@link DateFormat}。
	 *
	 * @param locale 区域设置
	 * @return {@link DateFormat} 实例
	 */
	private DateFormat createDateFormat(Locale locale) {
		// 如果指定了日期格式化模式，则使用指定的日期格式化模式
		if (StringUtils.hasLength(this.pattern)) {
			return new SimpleDateFormat(this.pattern, locale);
		}
		// 如果指定了 ISO 格式，并且 ISO 不是 NONE，则使用 ISO 格式
		if (this.iso != null && this.iso != ISO.NONE) {
			// 获取指定 ISO 格式对应的模式
			String pattern = ISO_PATTERNS.get(this.iso);
			if (pattern == null) {
				throw new IllegalStateException("Unsupported ISO format " + this.iso);
			}
			// 使用 标准时区创建 SimpleDateFormat
			SimpleDateFormat format = new SimpleDateFormat(pattern);
			format.setTimeZone(UTC);
			return format;
		}
		// 如果指定了样式格式化模式，则根据样式创建格式化对象
		if (StringUtils.hasLength(this.stylePattern)) {
			// 获取日期样式和时间样式
			int dateStyle = getStylePatternForChar(0);
			int timeStyle = getStylePatternForChar(1);
			// 如果日期样式和时间样式都有效，则创建日期时间格式化对象
			if (dateStyle != -1 && timeStyle != -1) {
				return DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
			}
			// 如果只有日期样式有效，则创建日期格式化对象
			if (dateStyle != -1) {
				return DateFormat.getDateInstance(dateStyle, locale);
			}
			// 如果只有时间样式有效，则创建时间格式化对象
			if (timeStyle != -1) {
				return DateFormat.getTimeInstance(timeStyle, locale);
			}
			// 如果样式模式不受支持，则抛出异常
			throw new IllegalStateException("Unsupported style pattern '" + this.stylePattern + "'");
		}
		// 根据指定样式创建日期格式化对象
		return DateFormat.getDateInstance(this.style, locale);
	}

	/**
	 * 获取给定索引处的样式字符。
	 *
	 * @param index 要检索的样式字符的索引
	 * @return 索引处的样式字符
	 */
	private int getStylePatternForChar(int index) {
		// 如果样式模式不为空且索引有效
		if (this.stylePattern != null && this.stylePattern.length() > index) {
			// 根据样式模式中的字符返回相应的样式
			switch (this.stylePattern.charAt(index)) {
				case 'S':
					return DateFormat.SHORT;
				case 'M':
					return DateFormat.MEDIUM;
				case 'L':
					return DateFormat.LONG;
				case 'F':
					return DateFormat.FULL;
				case '-':
					// 如果是'-'，表示样式无效
					return -1;
			}
		}
		// 如果样式模式不受支持，则抛出异常
		throw new IllegalStateException("Unsupported style pattern '" + this.stylePattern + "'");
	}

}
