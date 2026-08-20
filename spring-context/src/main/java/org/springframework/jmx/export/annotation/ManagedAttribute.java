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

package org.springframework.jmx.export.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级别的注解，表示将给定的 bean 属性暴露为
 * JMX 属性，对应于
 * {@link org.springframework.jmx.export.metadata.ManagedAttribute}。
 *
 * <p>仅在 JavaBean 的 getter 或 setter 上使用时有效。
 *
 * @author Rob Harrop
 * @since 1.2
 * @see org.springframework.jmx.export.metadata.ManagedAttribute
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ManagedAttribute {

	/**
	 * 在 {@link javax.management.Descriptor} 中设置属性的默认值。
	 */
	String defaultValue() default "";

	/**
	 * 在 {@link javax.management.Descriptor} 中设置属性的描述。
	 */
	String description() default "";

	/**
	 * 在 {@link javax.management.Descriptor} 中设置 currencyTimeLimit 字段。
	 */
	int currencyTimeLimit() default -1;

	/**
	 * 在 {@link javax.management.Descriptor} 中设置 persistPolicy 字段。
	 */
	String persistPolicy() default "";

	/**
	 * 在 {@link javax.management.Descriptor} 中设置 persistPeriod 字段。
	 */
	int persistPeriod() default -1;

}
