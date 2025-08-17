/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.beans.factory;

import org.springframework.beans.BeansException;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 针对 BeanFactory 的便捷工具方法，特别是
 * 用于 {@link ListableBeanFactory} 接口。
 *
 * <p>这些方法会返回 Bean 的数量、Bean 的名称或 Bean 的实例，
 * 并会考虑 BeanFactory 的层级嵌套关系
 * （与 {@code BeanFactory} 接口定义的方法不同，
 * {@code ListableBeanFactory} 接口的方法本身不会考虑层级结构）。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 04.07.2003
 */
public abstract class BeanFactoryUtils {

	/**
	 * 用于生成 Bean 名称的分隔符。
	 * 如果类名或父类名不唯一，会追加 "#1"、"#2" 等，
	 * 直到名称变得唯一为止。
	 */
	public static final String GENERATED_BEAN_NAME_SEPARATOR = "#";

	/**
	 * 从带有工厂bean前缀的名称缓存到已经转换好的名称。
	 * 缓存 {@link #transformedBeanName(String)} 已经转换好的结果。
	 *
	 * @see BeanFactory#FACTORY_BEAN_PREFIX
	 * @since 5.1
	 */
	private static final Map<String, String> transformedBeanNameCache = new ConcurrentHashMap<>();


	/**
	 * 返回给定的名称是否是工厂取消引用 (以工厂取消引用前缀开头)。
	 *
	 * @param name bean名称
	 * @return 给定的名称是否是工厂取消引用
	 * @see BeanFactory#FACTORY_BEAN_PREFIX
	 */
	public static boolean isFactoryDereference(@Nullable String name) {
		return (name != null && name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX));
	}

	/**
	 * 返回实际的bean名称，剥离工厂取消引用前缀 (如果有的话，如果找到，也去掉重复的工厂前缀)。
	 * 去除 FactoryBean 的修饰符 &
	 * 如果 name 以 “&” 为前缀，那么会去掉该 "&" 。
	 * 例如：name = "&&studentService" ，则会是 name = "studentService"。
	 *
	 * @param name bean名称
	 * @return 转换后的名称
	 * @see BeanFactory#FACTORY_BEAN_PREFIX
	 */
	public static String transformedBeanName(String name) {
		Assert.notNull(name, "'name' must not be null");
		if (!name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX)) {
			//如果该名称不是以$符号开头，则返回该名称
			return name;
		}
		return transformedBeanNameCache.computeIfAbsent(name, beanName -> {
			do {
				//如果该名称以$符号开头，循环获取$符号后面的字符串作为bean名称
				beanName = beanName.substring(BeanFactory.FACTORY_BEAN_PREFIX.length());
			} while (beanName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX));
			return beanName;
		});
	}

	/**
	 * 判断给定的名称是否是由默认命名策略生成的 Bean 名称
	 * （包含一个 "#..." 部分）。
	 *
	 * @param name Bean 的名称
	 * @return 给定的名称是否是生成的 Bean 名称
	 * @see #GENERATED_BEAN_NAME_SEPARATOR
	 * @see org.springframework.beans.factory.support.BeanDefinitionReaderUtils#generateBeanName
	 * @see org.springframework.beans.factory.support.DefaultBeanNameGenerator
	 */
	public static boolean isGeneratedBeanName(@Nullable String name) {
		return (name != null && name.contains(GENERATED_BEAN_NAME_SEPARATOR));
	}

	/**
	 * 从给定的（可能是生成的）Bean 名称中提取“原始”名称，
	 * 去掉可能为了唯一性而添加的 "#..." 后缀。
	 *
	 * @param name 可能是生成的 Bean 名称
	 * @return 原始的 Bean 名称
	 * @see #GENERATED_BEAN_NAME_SEPARATOR
	 */
	public static String originalBeanName(String name) {
		Assert.notNull(name, "'name' must not be null");
		int separatorIndex = name.indexOf(GENERATED_BEAN_NAME_SEPARATOR);
		return (separatorIndex != -1 ? name.substring(0, separatorIndex) : name);
	}


	// Bean 名称的获取

	/**
	 * 统计该工厂及其参与的所有层级中的所有 Bean。
	 * 包括父级 BeanFactory 中的数量。
	 * <p>对于“被覆盖”的 Bean（在子工厂中使用相同名称重新定义的），只计算一次。
	 *
	 * @param lbf BeanFactory
	 * @return 包括父级工厂定义的 Bean 数量
	 * @see #beanNamesIncludingAncestors
	 */
	public static int countBeansIncludingAncestors(ListableBeanFactory lbf) {
		return beanNamesIncludingAncestors(lbf).length;
	}

	/**
	 * 返回工厂中的所有 Bean 名称，包括父级工厂中的。
	 *
	 * @param lbf BeanFactory
	 * @return 匹配的 Bean 名称数组，如果没有则返回空数组
	 * @see #beanNamesForTypeIncludingAncestors
	 */
	public static String[] beanNamesIncludingAncestors(ListableBeanFactory lbf) {
		return beanNamesForTypeIncludingAncestors(lbf, Object.class);
	}

	/**
	 * 获取指定类型的所有 Bean 名称，包括父级工厂中定义的。
	 * 如果 Bean 定义被覆盖，将只返回唯一的名称。
	 * <p>会考虑由 FactoryBean 创建的对象，这意味着 FactoryBean 会被初始化。
	 * 如果 FactoryBean 创建的对象不匹配，则会将 FactoryBean 本身作为原始对象与类型匹配。
	 * <p>该版本的 {@code beanNamesForTypeIncludingAncestors} 自动包含原型和 FactoryBean。
	 *
	 * @param lbf  BeanFactory
	 * @param type Bean 必须匹配的类型（作为 {@code ResolvableType}）
	 * @return 匹配的 Bean 名称数组，如果没有则返回空数组
	 * @see ListableBeanFactory#getBeanNamesForType(ResolvableType)
	 * @since 4.2
	 */
	public static String[] beanNamesForTypeIncludingAncestors(ListableBeanFactory lbf, ResolvableType type) {
		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		String[] result = lbf.getBeanNamesForType(type);
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				String[] parentResult = beanNamesForTypeIncludingAncestors((ListableBeanFactory) hbf.getParentBeanFactory(), type);
				result = mergeNamesWithParent(result, parentResult, hbf);
			}
		}
		return result;
	}

	/**
	 * 获取给定类型的所有bean名称，包括在祖先工厂中定义的bean。
	 * 在bean定义被重写的情况下将返回唯一名称。
	 * <p>如果设置了"allowEagerInit"标志，则考虑由FactoryBean创建的对象，
	 * 这意味着FactoryBean将被初始化。如果FactoryBean创建的对象不匹配，
	 * 原始的FactoryBean本身将与类型进行匹配。如果未设置"allowEagerInit"，
	 * 则只检查原始FactoryBean（不需要初始化每个FactoryBean）。
	 *
	 * @param lbf                  bean工厂
	 * @param type                 bean必须匹配的类型（作为{@code ResolvableType}）
	 * @param includeNonSingletons 是否也包括原型或作用域bean，
	 *                             还是只包括单例（也适用于FactoryBean）
	 * @param allowEagerInit       是否为类型检查初始化<i>延迟初始化单例</i>和
	 *                             <i>由FactoryBean创建的对象</i>（或通过带有"factory-bean"引用的工厂方法）。
	 *                             注意FactoryBean需要被预先初始化以确定其类型：
	 *                             因此请注意，为此标志传入"true"将初始化FactoryBean和"factory-bean"引用。
	 * @return 匹配的bean名称数组，如果没有则返回空数组
	 * @see ListableBeanFactory#getBeanNamesForType(ResolvableType, boolean, boolean)
	 * @since 5.2
	 */
	public static String[] beanNamesForTypeIncludingAncestors(ListableBeanFactory lbf, ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {

		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		String[] result = lbf.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				String[] parentResult = beanNamesForTypeIncludingAncestors((ListableBeanFactory) hbf.getParentBeanFactory(), type, includeNonSingletons, allowEagerInit);
				result = mergeNamesWithParent(result, parentResult, hbf);
			}
		}
		return result;
	}

	/**
	 * 获取给定类型的所有bean名称，包括在祖先工厂中定义的bean。
	 * 在bean定义被重写的情况下将返回唯一名称。
	 * <p>考虑由FactoryBean创建的对象，这意味着FactoryBean将被初始化。
	 * 如果FactoryBean创建的对象不匹配，原始的FactoryBean本身将与类型进行匹配。
	 * <p>此版本的{@code beanNamesForTypeIncludingAncestors}自动
	 * 包括原型和FactoryBean。
	 *
	 * @param lbf  bean工厂
	 * @param type bean必须匹配的类型（作为{@code Class}）
	 * @return 匹配的bean名称数组，如果没有则返回空数组
	 * @see ListableBeanFactory#getBeanNamesForType(Class)
	 */
	public static String[] beanNamesForTypeIncludingAncestors(ListableBeanFactory lbf, Class<?> type) {
		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		String[] result = lbf.getBeanNamesForType(type);
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				String[] parentResult = beanNamesForTypeIncludingAncestors((ListableBeanFactory) hbf.getParentBeanFactory(), type);
				result = mergeNamesWithParent(result, parentResult, hbf);
			}
		}
		return result;
	}

	/**
	 * 获取给定类型的所有bean名称，包括在祖先工厂中定义的bean。
	 * 在bean定义被重写的情况下将返回唯一名称。
	 * <p>如果设置了"allowEagerInit"标志，则考虑由FactoryBean创建的对象，
	 * 这意味着FactoryBean将被初始化。如果FactoryBean创建的对象不匹配，
	 * 原始的FactoryBean本身将与类型进行匹配。如果未设置"allowEagerInit"，
	 * 则只检查原始FactoryBean（不需要初始化每个FactoryBean）。
	 *
	 * @param lbf                  bean工厂
	 * @param includeNonSingletons 是否也包括原型或作用域bean，
	 *                             还是只包括单例（也适用于FactoryBean）
	 * @param allowEagerInit       是否为类型检查初始化<i>延迟初始化单例</i>和
	 *                             <i>由FactoryBean创建的对象</i>（或通过带有"factory-bean"引用的工厂方法）。
	 *                             注意FactoryBean需要被预先初始化以确定其类型：
	 *                             因此请注意，为此标志传入"true"将初始化FactoryBean和"factory-bean"引用。
	 * @param type                 bean必须匹配的类型
	 * @return 匹配的bean名称数组，如果没有则返回空数组
	 * @see ListableBeanFactory#getBeanNamesForType(Class, boolean, boolean)
	 */
	public static String[] beanNamesForTypeIncludingAncestors(ListableBeanFactory lbf, Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {

		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		String[] result = lbf.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				String[] parentResult = beanNamesForTypeIncludingAncestors((ListableBeanFactory) hbf.getParentBeanFactory(), type, includeNonSingletons, allowEagerInit);
				result = mergeNamesWithParent(result, parentResult, hbf);
			}
		}
		return result;
	}

	/**
	 * 获取其{@code Class}具有提供的{@link Annotation}类型的所有bean名称，
	 * 包括在祖先工厂中定义的bean，但不创建任何bean实例。
	 * 在bean定义被重写的情况下将返回唯一名称。
	 *
	 * @param lbf            bean工厂
	 * @param annotationType 要查找的注解类型
	 * @return 匹配的bean名称数组，如果没有则返回空数组
	 * @see ListableBeanFactory#getBeanNamesForAnnotation(Class)
	 * @since 5.0
	 */
	public static String[] beanNamesForAnnotationIncludingAncestors(ListableBeanFactory lbf, Class<? extends Annotation> annotationType) {

		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		String[] result = lbf.getBeanNamesForAnnotation(annotationType);
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				String[] parentResult = beanNamesForAnnotationIncludingAncestors((ListableBeanFactory) hbf.getParentBeanFactory(), annotationType);
				result = mergeNamesWithParent(result, parentResult, hbf);
			}
		}
		return result;
	}


	// bean实例的检索

	/**
	 * 返回给定类型或子类型的所有bean，如果当前bean工厂是HierarchicalBeanFactory，
	 * 也会获取在祖先bean工厂中定义的bean。返回的Map将只包含此类型的bean。
	 * <p>考虑由FactoryBean创建的对象，这意味着FactoryBean将被初始化。
	 * 如果FactoryBean创建的对象不匹配，原始的FactoryBean本身将与类型进行匹配。
	 * <p><b>注意：相同名称的bean在"最低"工厂级别具有优先权，
	 * 即这些bean将从找到它们的最低工厂返回，隐藏祖先工厂中相应的bean。</b>
	 * 此功能允许通过在子工厂中明确选择相同的bean名称来"替换"bean；
	 * 祖先工厂中的bean就不会可见，甚至按类型查找也不会。
	 *
	 * @param lbf  bean工厂
	 * @param type 要匹配的bean类型
	 * @return 匹配bean实例的Map，如果没有则返回空Map
	 * @throws BeansException 如果无法创建bean
	 * @see ListableBeanFactory#getBeansOfType(Class)
	 */
	public static <T> Map<String, T> beansOfTypeIncludingAncestors(ListableBeanFactory lbf, Class<T> type) throws BeansException {

		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		Map<String, T> result = new LinkedHashMap<>(4);
		result.putAll(lbf.getBeansOfType(type));
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				Map<String, T> parentResult = beansOfTypeIncludingAncestors((ListableBeanFactory) hbf.getParentBeanFactory(), type);
				parentResult.forEach((beanName, beanInstance) -> {
					if (!result.containsKey(beanName) && !hbf.containsLocalBean(beanName)) {
						result.put(beanName, beanInstance);
					}
				});
			}
		}
		return result;
	}

	/**
	 * 返回给定类型或子类型的所有bean，如果当前bean工厂是HierarchicalBeanFactory，
	 * 也会获取在祖先bean工厂中定义的bean。返回的Map将只包含此类型的bean。
	 * <p>如果设置了"allowEagerInit"标志，则考虑由FactoryBean创建的对象，
	 * 这意味着FactoryBean将被初始化。如果FactoryBean创建的对象不匹配，
	 * 原始的FactoryBean本身将与类型进行匹配。如果未设置"allowEagerInit"，
	 * 则只检查原始FactoryBean（不需要初始化每个FactoryBean）。
	 * <p><b>注意：相同名称的bean在"最低"工厂级别具有优先权，
	 * 即这些bean将从找到它们的最低工厂返回，隐藏祖先工厂中相应的bean。</b>
	 * 此功能允许通过在子工厂中明确选择相同的bean名称来"替换"bean；
	 * 祖先工厂中的bean就不会可见，甚至按类型查找也不会。
	 *
	 * @param lbf                  bean工厂
	 * @param type                 要匹配的bean类型
	 * @param includeNonSingletons 是否也包括原型或作用域bean，
	 *                             还是只包括单例（也适用于FactoryBean）
	 * @param allowEagerInit       是否为类型检查初始化<i>延迟初始化单例</i>和
	 *                             <i>由FactoryBean创建的对象</i>（或通过带有"factory-bean"引用的工厂方法）。
	 *                             注意FactoryBean需要被预先初始化以确定其类型：
	 *                             因此请注意，为此标志传入"true"将初始化FactoryBean和"factory-bean"引用。
	 * @return 匹配bean实例的Map，如果没有则返回空Map
	 * @throws BeansException 如果无法创建bean
	 * @see ListableBeanFactory#getBeansOfType(Class, boolean, boolean)
	 */
	public static <T> Map<String, T> beansOfTypeIncludingAncestors(ListableBeanFactory lbf, Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {

		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		Map<String, T> result = new LinkedHashMap<>(4);
		result.putAll(lbf.getBeansOfType(type, includeNonSingletons, allowEagerInit));
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				Map<String, T> parentResult = beansOfTypeIncludingAncestors((ListableBeanFactory) hbf.getParentBeanFactory(), type, includeNonSingletons, allowEagerInit);
				parentResult.forEach((beanName, beanInstance) -> {
					if (!result.containsKey(beanName) && !hbf.containsLocalBean(beanName)) {
						result.put(beanName, beanInstance);
					}
				});
			}
		}
		return result;
	}

	/**
	 * 返回给定类型或子类型的单个bean，如果当前bean工厂是
	 * HierarchicalBeanFactory，也会获取在祖先bean工厂中定义的bean。
	 * 当我们期望单个bean且不关心bean名称时的有用便利方法。
	 * <p>考虑由FactoryBean创建的对象，这意味着FactoryBean将被初始化。
	 * 如果FactoryBean创建的对象不匹配，原始的FactoryBean本身将与类型进行匹配。
	 * <p>此版本的{@code beanOfTypeIncludingAncestors}自动包括
	 * 原型和FactoryBean。
	 * <p><b>注意：相同名称的bean在"最低"工厂级别具有优先权，
	 * 即这些bean将从找到它们的最低工厂返回，隐藏祖先工厂中相应的bean。</b>
	 * 此功能允许通过在子工厂中明确选择相同的bean名称来"替换"bean；
	 * 祖先工厂中的bean就不会可见，甚至按类型查找也不会。
	 *
	 * @param lbf  bean工厂
	 * @param type 要匹配的bean类型
	 * @return 匹配的bean实例
	 * @throws NoSuchBeanDefinitionException   如果未找到给定类型的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个给定类型的bean
	 * @throws BeansException                  如果无法创建bean
	 * @see #beansOfTypeIncludingAncestors(ListableBeanFactory, Class)
	 */
	public static <T> T beanOfTypeIncludingAncestors(ListableBeanFactory lbf, Class<T> type) throws BeansException {

		Map<String, T> beansOfType = beansOfTypeIncludingAncestors(lbf, type);
		return uniqueBean(type, beansOfType);
	}

	/**
	 * 返回给定类型或子类型的单个bean，如果当前bean工厂是
	 * HierarchicalBeanFactory，也会获取在祖先bean工厂中定义的bean。
	 * 当我们期望单个bean且不关心bean名称时的有用便利方法。
	 * <p>如果设置了"allowEagerInit"标志，则考虑由FactoryBean创建的对象，
	 * 这意味着FactoryBean将被初始化。如果FactoryBean创建的对象不匹配，
	 * 原始的FactoryBean本身将与类型进行匹配。如果未设置"allowEagerInit"，
	 * 则只检查原始FactoryBean（不需要初始化每个FactoryBean）。
	 * <p><b>注意：相同名称的bean在"最低"工厂级别具有优先权，
	 * 即这些bean将从找到它们的最低工厂返回，隐藏祖先工厂中相应的bean。</b>
	 * 此功能允许通过在子工厂中明确选择相同的bean名称来"替换"bean；
	 * 祖先工厂中的bean就不会可见，甚至按类型查找也不会。
	 *
	 * @param lbf                  bean工厂
	 * @param type                 要匹配的bean类型
	 * @param includeNonSingletons 是否也包括原型或作用域bean，
	 *                             还是只包括单例（也适用于FactoryBean）
	 * @param allowEagerInit       是否为类型检查初始化<i>延迟初始化单例</i>和
	 *                             <i>由FactoryBean创建的对象</i>（或通过带有"factory-bean"引用的工厂方法）。
	 *                             注意FactoryBean需要被预先初始化以确定其类型：
	 *                             因此请注意，为此标志传入"true"将初始化FactoryBean和"factory-bean"引用。
	 * @return 匹配的bean实例
	 * @throws NoSuchBeanDefinitionException   如果未找到给定类型的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个给定类型的bean
	 * @throws BeansException                  如果无法创建bean
	 * @see #beansOfTypeIncludingAncestors(ListableBeanFactory, Class, boolean, boolean)
	 */
	public static <T> T beanOfTypeIncludingAncestors(ListableBeanFactory lbf, Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {

		Map<String, T> beansOfType = beansOfTypeIncludingAncestors(lbf, type, includeNonSingletons, allowEagerInit);
		return uniqueBean(type, beansOfType);
	}

	/**
	 * 返回给定类型或子类型的单个bean，不在祖先工厂中查找。
	 * 当我们期望单个bean且不关心bean名称时的有用便利方法。
	 * <p>考虑由FactoryBean创建的对象，这意味着FactoryBean将被初始化。
	 * 如果FactoryBean创建的对象不匹配，原始的FactoryBean本身将与类型进行匹配。
	 * <p>此版本的{@code beanOfType}自动包括原型和FactoryBean。
	 *
	 * @param lbf  bean工厂
	 * @param type 要匹配的bean类型
	 * @return 匹配的bean实例
	 * @throws NoSuchBeanDefinitionException   如果未找到给定类型的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个给定类型的bean
	 * @throws BeansException                  如果无法创建bean
	 * @see ListableBeanFactory#getBeansOfType(Class)
	 */
	public static <T> T beanOfType(ListableBeanFactory lbf, Class<T> type) throws BeansException {
		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		Map<String, T> beansOfType = lbf.getBeansOfType(type);
		return uniqueBean(type, beansOfType);
	}

	/**
	 * 返回给定类型或子类型的单个bean，不在祖先工厂中查找。
	 * 当我们期望单个bean且不关心bean名称时的有用便利方法。
	 * <p>如果设置了"allowEagerInit"标志，则考虑由FactoryBean创建的对象，
	 * 这意味着FactoryBean将被初始化。如果FactoryBean创建的对象不匹配，
	 * 原始的FactoryBean本身将与类型进行匹配。如果未设置"allowEagerInit"，
	 * 则只检查原始FactoryBean（不需要初始化每个FactoryBean）。
	 *
	 * @param lbf                  bean工厂
	 * @param type                 要匹配的bean类型
	 * @param includeNonSingletons 是否也包括原型或作用域bean，
	 *                             还是只包括单例（也适用于FactoryBean）
	 * @param allowEagerInit       是否为类型检查初始化<i>延迟初始化单例</i>和
	 *                             <i>由FactoryBean创建的对象</i>（或通过带有"factory-bean"引用的工厂方法）。
	 *                             注意FactoryBean需要被预先初始化以确定其类型：
	 *                             因此请注意，为此标志传入"true"将初始化FactoryBean和"factory-bean"引用。
	 * @return 匹配的bean实例
	 * @throws NoSuchBeanDefinitionException   如果未找到给定类型的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个给定类型的bean
	 * @throws BeansException                  如果无法创建bean
	 * @see ListableBeanFactory#getBeansOfType(Class, boolean, boolean)
	 */
	public static <T> T beanOfType(ListableBeanFactory lbf, Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {

		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		Map<String, T> beansOfType = lbf.getBeansOfType(type, includeNonSingletons, allowEagerInit);
		return uniqueBean(type, beansOfType);
	}


	/**
	 * 将给定的bean名称结果与给定的父结果合并。
	 *
	 * @param result       本地bean名称结果
	 * @param parentResult 父bean名称结果（可能为空）
	 * @param hbf          本地bean工厂
	 * @return 合并后的结果（可能是原样的本地结果）
	 * @since 4.3.15
	 */
	private static String[] mergeNamesWithParent(String[] result, String[] parentResult, HierarchicalBeanFactory hbf) {
		if (parentResult.length == 0) {
			return result;
		}
		List<String> merged = new ArrayList<>(result.length + parentResult.length);
		merged.addAll(Arrays.asList(result));
		for (String beanName : parentResult) {
			if (!merged.contains(beanName) && !hbf.containsLocalBean(beanName)) {
				merged.add(beanName);
			}
		}
		return StringUtils.toStringArray(merged);
	}

	/**
	 * 从给定的匹配bean映射中提取给定类型的唯一bean。
	 *
	 * @param type          要匹配的bean类型
	 * @param matchingBeans 找到的所有匹配bean
	 * @return 唯一的bean实例
	 * @throws NoSuchBeanDefinitionException   如果未找到给定类型的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个给定类型的bean
	 */
	private static <T> T uniqueBean(Class<T> type, Map<String, T> matchingBeans) {
		int count = matchingBeans.size();
		if (count == 1) {
			return matchingBeans.values().iterator().next();
		} else if (count > 1) {
			throw new NoUniqueBeanDefinitionException(type, matchingBeans.keySet());
		} else {
			throw new NoSuchBeanDefinitionException(type);
		}
	}

}
