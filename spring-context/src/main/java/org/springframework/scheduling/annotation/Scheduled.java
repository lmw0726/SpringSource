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

package org.springframework.scheduling.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 标记方法为定时调度的注解。必须指定 {@link #cron}、{@link #fixedDelay} 或
 * {@link #fixedRate} 属性中的恰好一个。
 *
 * <p>被注解的方法不能有参数。通常返回类型为 {@code void}；如果不是，
 * 则通过调度器调用时返回值将被忽略。
 *
 * <p>{@code @Scheduled} 注解的处理通过注册
 * {@link ScheduledAnnotationBeanPostProcessor} 来执行。可以手动完成，
 * 或者更方便地通过 {@code <task:annotation-driven/>} XML 元素
 * 或 {@link EnableScheduling @EnableScheduling} 注解来启用。
 *
 * <p>此注解可以用作<em>元注解</em>，通过属性覆盖来创建自定义的
 * <em>组合注解</em>。
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Dave Syer
 * @author Chris Beams
 * @author Victor Brown
 * @author Sam Brannen
 * @since 3.0
 * @see EnableScheduling
 * @see ScheduledAnnotationBeanPostProcessor
 * @see Schedules
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(Schedules.class)
public @interface Scheduled {

	/**
	 * 表示禁用触发器的特殊 cron 表达式值：{@value}。
	 * <p>主要用于配合 <code>${...}</code> 占位符使用，
	 * 允许从外部禁用对应的定时调度方法。
	 * @since 5.1
	 * @see ScheduledTaskRegistrar#CRON_DISABLED
	 */
	String CRON_DISABLED = ScheduledTaskRegistrar.CRON_DISABLED;


	/**
	 * 类似 cron 的表达式，扩展了标准的 UNIX 定义，增加了对秒、分钟、小时、日、月和星期的触发支持。
	 * <p>例如，{@code "0 * * * * MON-FRI"} 表示工作日每分钟执行一次
	 * （在每分钟的顶部——第 0 秒）。
	 * <p>从左到右的字段依次解释如下。
	 * <ul>
	 * <li>秒</li>
	 * <li>分钟</li>
	 * <li>小时</li>
	 * <li>日</li>
	 * <li>月</li>
	 * <li>星期</li>
	 * </ul>
	 * <p>特殊值 {@link #CRON_DISABLED "-"} 表示禁用的 cron 触发器，
	 * 主要用于通过 <code>${...}</code> 占位符解析的外部指定值。
	 * @return 可解析为 cron 调度计划的表达式
	 * @see org.springframework.scheduling.support.CronExpression#parse(String)
	 */
	String cron() default "";

	/**
	 * cron 表达式解析所使用的时区。默认情况下，该属性为空字符串
	 * （即使用服务器的本地时区）。
	 * @return {@link java.util.TimeZone#getTimeZone(String)} 接受的时区 ID，
	 * 或空字符串表示使用服务器默认时区
	 * @since 4.0
	 * @see org.springframework.scheduling.support.CronTrigger#CronTrigger(String, java.util.TimeZone)
	 * @see java.util.TimeZone
	 */
	String zone() default "";

	/**
	 * 以固定周期执行被注解的方法，周期为上次调用结束到下次调用开始之间的间隔。
	 * <p>默认时间单位为毫秒，但可通过 {@link #timeUnit} 覆盖。
	 * @return 延迟时间
	 */
	long fixedDelay() default -1;

	/**
	 * 以固定周期执行被注解的方法，周期为上次调用结束到下次调用开始之间的间隔。
	 * <p>默认时间单位为毫秒，但可通过 {@link #timeUnit} 覆盖。
	 * @return 延迟时间的字符串值——例如占位符或符合
	 * {@link java.time.Duration#parse java.time.Duration} 格式的值
	 * @since 3.2.2
	 */
	String fixedDelayString() default "";

	/**
	 * 以固定周期执行被注解的方法，周期为两次调用之间的间隔。
	 * <p>默认时间单位为毫秒，但可通过 {@link #timeUnit} 覆盖。
	 * @return 周期时间
	 */
	long fixedRate() default -1;

	/**
	 * 以固定周期执行被注解的方法，周期为两次调用之间的间隔。
	 * <p>默认时间单位为毫秒，但可通过 {@link #timeUnit} 覆盖。
	 * @return 周期时间的字符串值——例如占位符或符合
	 * {@link java.time.Duration#parse java.time.Duration} 格式的值
	 * @since 3.2.2
	 */
	String fixedRateString() default "";

	/**
	 * {@link #fixedRate} 或 {@link #fixedDelay} 任务首次执行前的延迟时间单位数。
	 * <p>默认时间单位为毫秒，但可通过 {@link #timeUnit} 覆盖。
	 * @return 初始延迟时间
	 * @since 3.2
	 */
	long initialDelay() default -1;

	/**
	 * {@link #fixedRate} 或 {@link #fixedDelay} 任务首次执行前的延迟时间单位数。
	 * <p>默认时间单位为毫秒，但可通过 {@link #timeUnit} 覆盖。
	 * @return 初始延迟时间的字符串值——例如占位符或符合
	 * {@link java.time.Duration#parse java.time.Duration} 格式的值
	 * @since 3.2.2
	 */
	String initialDelayString() default "";

	/**
	 * 用于 {@link #fixedDelay}、{@link #fixedDelayString}、
	 * {@link #fixedRate}、{@link #fixedRateString}、{@link #initialDelay} 和
	 * {@link #initialDelayString} 的 {@link TimeUnit}。
	 * <p>默认为 {@link TimeUnit#MILLISECONDS}。
	 * <p>此属性对 {@linkplain #cron() cron 表达式} 以及通过
	 * {@link #fixedDelayString}、{@link #fixedRateString} 或
	 * {@link #initialDelayString} 提供的 {@link java.time.Duration} 值无效。
	 * @return 要使用的 {@code TimeUnit}
	 * @since 5.3.10
	 */
	TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

}
