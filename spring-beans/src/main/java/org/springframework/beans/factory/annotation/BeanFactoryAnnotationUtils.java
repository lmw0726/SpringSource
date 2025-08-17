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

package org.springframework.beans.factory.annotation;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 便捷方法，用于执行与 Spring 特定注解相关的 Bean 查找，
 * 例如 Spring 的 {@link Qualifier @Qualifier} 注解。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.1.2
 * @see BeanFactoryUtils
 */
public abstract class BeanFactoryAnnotationUtils {

	/**
	 * 从给定的 {@code BeanFactory} 中检索所有类型为 {@code T} 的 Bean，
	 * 这些 Bean 声明了与给定限定符匹配的限定符（例如通过 {@code <qualifier>} 或 {@code @Qualifier}），
	 * 或者 Bean 名称与给定限定符匹配。
	 * @param beanFactory 用于获取目标 Bean 的工厂（也会搜索父工厂）
	 * @param beanType 要检索的 Bean 类型
	 * @param qualifier 用于在所有类型匹配中选择的限定符
	 * @return 匹配的类型为 {@code T} 的 Bean
	 * @throws BeansException 如果任何匹配的 Bean 无法创建
	 * @since 5.1.1
	 * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(ListableBeanFactory, Class)
	 */
	public static <T> Map<String, T> qualifiedBeansOfType(
			ListableBeanFactory beanFactory, Class<T> beanType, String qualifier) throws BeansException {

		String[] candidateBeans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, beanType);
		Map<String, T> result = new LinkedHashMap<>(4);
		for (String beanName : candidateBeans) {
			if (isQualifierMatch(qualifier::equals, beanName, beanFactory)) {
				result.put(beanName, beanFactory.getBean(beanName, beanType));
			}
		}
		return result;
	}

	/**
	 * 从给定的 {@code BeanFactory} 中获取类型为 {@code T} 的 Bean，
	 * 这些 Bean 声明了与给定限定符匹配的限定符（例如通过 {@code <qualifier>} 或 {@code @Qualifier}），
	 * 或者 Bean 名称与给定限定符匹配。
	 * @param beanFactory 用于获取目标 Bean 的工厂（也会搜索父工厂）
	 * @param beanType 要检索的 Bean 类型
	 * @param qualifier 用于在多个 Bean 匹配中选择的限定符
	 * @return 匹配的类型为 {@code T} 的 Bean（永不为 {@code null}）
	 * @throws NoUniqueBeanDefinitionException 如果找到多个匹配的 {@code T} 类型 Bean
	 * @throws NoSuchBeanDefinitionException 如果未找到匹配的 {@code T} 类型 Bean
	 * @throws BeansException 如果 Bean 无法创建
	 * @see BeanFactoryUtils#beanOfTypeIncludingAncestors(ListableBeanFactory, Class)
	 */
	public static <T> T qualifiedBeanOfType(BeanFactory beanFactory, Class<T> beanType, String qualifier)
			throws BeansException {

		Assert.notNull(beanFactory, "BeanFactory must not be null");

		if (beanFactory instanceof ListableBeanFactory) {
			// 支持完整限定符匹配
			return qualifiedBeanOfType((ListableBeanFactory) beanFactory, beanType, qualifier);
		}
		else if (beanFactory.containsBean(qualifier)) {
			// 回退：至少通过 Bean 名称找到目标 Bean
			return beanFactory.getBean(qualifier, beanType);
		}
		else {
			throw new NoSuchBeanDefinitionException(qualifier, "No matching " + beanType.getSimpleName() +
					" bean found for bean name '" + qualifier +
					"'! (Note: Qualifier matching not supported because given " +
					"BeanFactory does not implement ConfigurableListableBeanFactory.)");
		}
	}

	/**
	 * 从给定的 {@code BeanFactory} 中获取类型为 {@code T} 的 Bean，
	 * 这些 Bean 声明了与给定限定符匹配的限定符（例如 {@code <qualifier>} 或 {@code @Qualifier}）。
	 * @param bf 用于获取目标 Bean 的工厂
	 * @param beanType 要检索的 Bean 类型
	 * @param qualifier 用于在多个 Bean 匹配中选择的限定符
	 * @return 匹配的类型为 {@code T} 的 Bean（永不为 {@code null}）
	 */
	private static <T> T qualifiedBeanOfType(ListableBeanFactory bf, Class<T> beanType, String qualifier) {
		String[] candidateBeans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(bf, beanType);
		String matchingBean = null;
		for (String beanName : candidateBeans) {
			if (isQualifierMatch(qualifier::equals, beanName, bf)) {
				if (matchingBean != null) {
					throw new NoUniqueBeanDefinitionException(beanType, matchingBean, beanName);
				}
				matchingBean = beanName;
			}
		}
		if (matchingBean != null) {
			return bf.getBean(matchingBean, beanType);
		}
		else if (bf.containsBean(qualifier)) {
			// 回退：至少通过 Bean 名称找到目标 Bean - 可能是手动注册的单例
			return bf.getBean(qualifier, beanType);
		}
		else {
			throw new NoSuchBeanDefinitionException(qualifier, "No matching " + beanType.getSimpleName() +
					" bean found for qualifier '" + qualifier + "' - neither qualifier match nor bean name match!");
		}
	}

	/**
	 * 检查指定名称的 Bean 是否声明了给定名称的限定符。
	 * @param qualifier 要匹配的限定符
	 * @param beanName 候选 Bean 的名称
	 * @param beanFactory 用于获取指定 Bean 的工厂
	 * @return 如果 Bean 定义（XML 情况下）或 Bean 的工厂方法（{@code @Bean} 情况下）定义了匹配的限定符值
	 *         （通过 {@code <qualifier>} 或 {@code @Qualifier}），则返回 {@code true}
	 * @since 5.0
	 */
	public static boolean isQualifierMatch(
			Predicate<String> qualifier, String beanName, @Nullable BeanFactory beanFactory) {

		// 先尝试快速匹配 Bean 名称或别名
		if (qualifier.test(beanName)) {
			return true;
		}
		if (beanFactory != null) {
			for (String alias : beanFactory.getAliases(beanName)) {
				if (qualifier.test(alias)) {
					return true;
				}
			}
			try {
				Class<?> beanType = beanFactory.getType(beanName);
				if (beanFactory instanceof ConfigurableBeanFactory) {
					BeanDefinition bd = ((ConfigurableBeanFactory) beanFactory).getMergedBeanDefinition(beanName);
					// Bean 定义上是否有显式限定符元数据？（通常在 XML 定义中）
					if (bd instanceof AbstractBeanDefinition) {
						AbstractBeanDefinition abd = (AbstractBeanDefinition) bd;
						AutowireCandidateQualifier candidate = abd.getQualifier(Qualifier.class.getName());
						if (candidate != null) {
							Object value = candidate.getAttribute(AutowireCandidateQualifier.VALUE_KEY);
							if (value != null && qualifier.test(value.toString())) {
								return true;
							}
						}
					}
					// 工厂方法上对应的限定符？（通常在配置类中）
					if (bd instanceof RootBeanDefinition) {
						Method factoryMethod = ((RootBeanDefinition) bd).getResolvedFactoryMethod();
						if (factoryMethod != null) {
							Qualifier targetAnnotation = AnnotationUtils.getAnnotation(factoryMethod, Qualifier.class);
							if (targetAnnotation != null) {
								return qualifier.test(targetAnnotation.value());
							}
						}
					}
				}
				// Bean 实现类上对应的限定符？（针对自定义用户类型）
				if (beanType != null) {
					Qualifier targetAnnotation = AnnotationUtils.getAnnotation(beanType, Qualifier.class);
					if (targetAnnotation != null) {
						return qualifier.test(targetAnnotation.value());
					}
				}
			}
			catch (NoSuchBeanDefinitionException ex) {
				// 忽略 - 无法比较手动注册的单例对象的限定符
			}
		}
		return false;
	}

}
