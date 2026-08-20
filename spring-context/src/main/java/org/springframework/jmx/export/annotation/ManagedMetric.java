/*
 * Copyright 2002-2015 the original author or authors.
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

import org.springframework.jmx.support.MetricType;

/**
 * 方法级别的注解，用于将指定的 Bean 属性作为 JMX 属性暴露，并添加描述符属性以表明该属性是一个度量指标（metric）。
 *
 * <p>仅在用于 JavaBean 的 getter 方法时有效。
 *
 * @author Jennifer Hickey
 * @since 3.0
 * @see org.springframework.jmx.export.metadata.ManagedMetric
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ManagedMetric {

	String category() default "";

	int currencyTimeLimit() default -1;

	String description() default "";

	String displayName() default "";

	MetricType metricType() default MetricType.GAUGE;

	int persistPeriod() default -1;

	String persistPolicy() default "";

	String unit() default "";

}
