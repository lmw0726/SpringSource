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

package org.springframework.format.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * 声明字段或方法参数应格式化为日期或时间。
 *
 * <p>支持按样式模式、ISO日期时间模式或自定义格式模式字符串进行格式化。
 * 可应用于{@link java.util.Date}、{@link java.util.Calendar}、{@link Long}（用于
 * 毫秒时间戳）以及JSR-310 {@code java.time}值类型。
 *
 * <p>对于基于样式的格式化，将{@link #style}属性设置为所需的样式模式代码。
 * 代码的第一个字符是日期样式，第二个字符是时间样式。
 * 通过指定字符'S'表示短样式，'M'表示中等样式，'L'表示长样式，'F'表示完整样式。
 * 可以通过指定样式字符'-'来省略日期或时间，例如，'M-'表示无时间的日期的中等格式。
 *
 * <p>对于基于ISO的格式化，将{@link #iso}属性设置为所需的{@link ISO}格式，
 * 例如{@link ISO#DATE}。
 *
 * <p>对于自定义格式化，将{@link #pattern}属性设置为日期时间模式，例如
 * {@code "yyyy/MM/dd hh:mm:ss a"}。
 *
 * <p>每个属性是互斥的，因此每个注解实例只设置一个属性（选择对您的格式化需求最方便的属性）。
 *
 * <ul>
 * <li>当指定了pattern属性时，它优先于style和ISO属性。</li>
 * <li>当指定了{@link #iso}属性时，它优先于style属性。</li>
 * <li>当未指定注解属性时，默认应用样式为SS（短日期，短时间）。</li>
 * </ul>
 *
 * <h3>时区</h3>
 * <p>每当使用{@link #style}或{@link #pattern}属性进行格式化{@link java.util.Date}值时，
 * 将使用JVM的{@linkplain java.util.TimeZone#getDefault()默认时区}。
 * 每当在格式化{@link java.util.Date}值时使用{@link #iso}属性时，将使用{@code UTC}作为时区。
 * 同样的时区也将应用于任何{@linkplain #fallbackPatterns 回退模式}。
 * 为了强制使用{@code UTC}作为时区，您可以使用{@code -Duser.timezone=UTC}引导JVM。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see java.time.format.DateTimeFormatter
 * @see org.joda.time.format.DateTimeFormat
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface DateTimeFormat {

	/**
	 * 用于格式化字段或方法参数的样式模式。
	 * <p>默认为'SS'，表示短日期，短时间。当您希望根据常见样式格式化字段或方法参数时，请设置此属性，而不是使用默认样式。
	 * @see #fallbackPatterns
	 */
	String style() default "SS";

	/**
	 * 用于格式化字段或方法参数的ISO模式。
	 * <p>支持的ISO模式在{@link ISO}枚举中定义。
	 * <p>默认为{@link ISO#NONE}，表示应忽略此属性。当您希望根据ISO格式格式化字段或方法参数时，请设置此属性。
	 * @see #fallbackPatterns
	 */
	ISO iso() default ISO.NONE;

	/**
	 * 用于格式化字段或方法参数的自定义模式。
	 * <p>默认为空字符串，表示未指定自定义模式字符串。当您希望根据样式或ISO格式之外的自定义日期时间模式格式化字段或方法参数时，请设置此属性。
	 * <p>注意：此模式遵循原始的{@link java.text.SimpleDateFormat}样式，也受到Joda-Time支持，
	 * 对于溢出（例如，在非闰年中拒绝二月29日值）具有严格的解析语义。因此，'yy'字符表示传统样式中的年份，而不是
	 * {@link java.time.format.DateTimeFormatter DateTimeFormatter}规范中的“年代年”（即在严格解析模式下，
	 * 'yy'会在通过{@code DateTimeFormatter}时转换为'uu'）。
	 * @see #fallbackPatterns
	 */
	String pattern() default "";

	/**
	 * 用作主要{@link #pattern}、{@link #iso}或{@link #style}属性解析失败时的回退的一组自定义模式。
	 * <p>例如，如果希望使用ISO日期格式进行解析和打印，但允许对各种日期格式的用户输入进行宽松解析，
	 * 则可以配置类似于以下内容。
	 * <pre style="code">
	 * {@literal @}DateTimeFormat(iso = ISO.DATE, fallbackPatterns = { "M/d/yy", "dd.MM.yyyy" })
	 * </pre>
	 * <p>回退模式仅用于解析。它们不用于将值打印为字符串。始终使用主要{@link #pattern}、{@link #iso}
	 * 或{@link #style}属性进行打印。有关使用哪个时区的详细信息，请参见
	 * {@linkplain DateTimeFormat 类级别文档}。
	 * <p>不支持Joda-Time值类型的回退模式。
	 * @since 5.3.5
	 */
	String[] fallbackPatterns() default {};


	/**
	 * 常见的 ISO 日期时间格式模式。
	 */
	enum ISO {

		/**
		 * 最常见的 ISO 日期格式 {@code yyyy-MM-dd} &mdash; 例如，"2000-10-31"。
		 */
		DATE,

		/**
		 * 最常见的 ISO 时间格式 {@code HH:mm:ss.SSSXXX} &mdash; 例如，"01:30:00.000-05:00"。
		 */
		TIME,

		/**
		 * 最常见的 ISO 日期时间格式 {@code yyyy-MM-dd'T'HH:mm:ss.SSSXXX} &mdash; 例如，"2000-10-31T01:30:00.000-05:00"。
		 */
		DATE_TIME,

		/**
		 * 表示不应用基于 ISO 的格式模式。
		 */
		NONE
	}

}
