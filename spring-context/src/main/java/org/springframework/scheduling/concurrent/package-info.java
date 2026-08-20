/**
 * 针对 {@code java.util.concurrent} 和 {@code javax.enterprise.concurrent} 包的调度便捷类，
 * 允许在 Spring 上下文中将 ThreadPoolExecutor 或 ScheduledThreadPoolExecutor 作为 Bean 进行配置。
 * 既支持原生 {@code java.util.concurrent} 接口，也支持 Spring {@code TaskExecutor} 机制。
 */
@NonNullApi
@NonNullFields
package org.springframework.scheduling.concurrent;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
