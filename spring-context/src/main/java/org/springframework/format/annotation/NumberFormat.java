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

package org.springframework.format.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明字段或方法参数应该格式化为数字。
 *
 * <p>支持按样式或自定义模式字符串格式化。可应用于任何 JDK {@code Number} 类型，如 {@code Double} 和 {@code Long}。
 *
 * <p>对于基于样式的格式化，请将 {@link #style} 属性设置为所需的 {@link Style}。对于自定义格式，请将 {@link #pattern}
 * 属性设置为数字模式，例如 {@code #,###.##}。
 *
 * <p>每个属性都是互斥的，因此每个注解实例只能设置一个属性（选择对格式化需求最方便的属性）。
 * 当指定 {@link #pattern} 属性时，它将覆盖 {@link #style} 属性。
 * 当未指定任何注解属性时，默认应用的格式是基于样式的数字或货币格式，具体取决于所注释的字段或方法参数类型。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 * @see java.text.NumberFormat
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface NumberFormat {

	/**
	 * 用于格式化字段的样式模式。
	 * <p>默认为 {@link Style#DEFAULT}，用于大多数注释类型的通用数字格式化，除了默认为货币格式的货币类型。
	 * 当您希望根据与默认样式不同的常见样式格式化字段时，请设置此属性。
	 */
	Style style() default Style.DEFAULT;

	/**
	 * 用于格式化字段的自定义模式。
	 * <p>默认为空字符串，表示未指定自定义模式字符串。
	 * 当您希望根据不由样式表示的自定义数字模式格式化字段时，请设置此属性。
	 */
	String pattern() default "";


	/**
	 * 常见的数字格式样式。
	 */
	enum Style {

		/**
		 * 注释类型的默认格式：通常为'number'，但对于货币类型（例如 {@code javax.money.MonetaryAmount}）可能为'currency'。
		 * @since 4.2
		 */
		DEFAULT,

		/**
		 * 当前区域设置的通用数字格式。
		 */
		NUMBER,

		/**
		 * 当前区域设置的百分比格式。
		 */
		PERCENT,

		/**
		 * 当前区域设置的货币格式。
		 */
		CURRENCY
	}

}
