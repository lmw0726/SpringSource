/**
 * 本包建立在 beans 包的基础上，增加了对消息源（Message Source）和观察者设计模式（Observer Pattern）的支持，
 * 并为应用对象提供了一致的 API 来获取资源。
 *
 * <p>Spring 应用无需显式依赖 ApplicationContext 甚至 BeanFactory 的功能。
 * Spring 架构的优势之一在于，应用对象通常可以在不依赖 Spring 特定 API 的情况下进行配置。
 */
@NonNullApi
@NonNullFields
package org.springframework.context;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
