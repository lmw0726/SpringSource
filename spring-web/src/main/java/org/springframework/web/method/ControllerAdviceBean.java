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

package org.springframework.web.method;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 封装有关{@link ControllerAdvice @ControllerAdvice} Spring管理的bean的信息，而不一定需要实例化它。
 *
 * <p>{@link #findAnnotatedBeans(ApplicationContext)}方法可用于发现这些bean。
 * 然而，可以从任何对象创建{@code ControllerAdviceBean}，包括那些没有{@code @ControllerAdvice}注解的对象。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.2
 */
public class ControllerAdviceBean implements Ordered {

	/**
	 * 对实际bean实例的引用或表示bean名称的{@code String}。
	 */
	private final Object beanOrName;

	/**
	 * 是否是单例
	 */
	private final boolean isSingleton;

	/**
	 * 对解析后的bean实例的引用，可能通过{@code BeanFactory}延迟检索。
	 */
	@Nullable
	private Object resolvedBean;

	/**
	 * bean类型
	 */
	@Nullable
	private final Class<?> beanType;

	/**
	 * bean类型断言
	 */
	private final HandlerTypePredicate beanTypePredicate;

	/**
	 * bean工厂
	 */
	@Nullable
	private final BeanFactory beanFactory;

	/**
	 * 排序值
	 */
	@Nullable
	private Integer order;


	/**
	 * 使用给定的bean实例创建一个{@code ControllerAdviceBean}。
	 *
	 * @param bean bean实例
	 */
	public ControllerAdviceBean(Object bean) {
		Assert.notNull(bean, "Bean must not be null");
		this.beanOrName = bean;
		this.isSingleton = true;
		this.resolvedBean = bean;
		// 获取 bean 的实际类
		this.beanType = ClassUtils.getUserClass(bean.getClass());
		// 创建用于匹配 bean 类型的断言
		this.beanTypePredicate = createBeanTypePredicate(this.beanType);
		this.beanFactory = null;
	}

	/**
	 * 使用给定的bean名称和{@code BeanFactory}创建一个{@code ControllerAdviceBean}。
	 *
	 * @param beanName    bean的名称
	 * @param beanFactory {@code BeanFactory}用于初步检索bean类型，后来用于解析实际bean
	 */
	public ControllerAdviceBean(String beanName, BeanFactory beanFactory) {
		this(beanName, beanFactory, null);
	}

	/**
	 * 使用给定的bean名称、{@code BeanFactory}和{@link ControllerAdvice @ControllerAdvice}注解创建一个{@code ControllerAdviceBean}。
	 *
	 * @param beanName         bean的名称
	 * @param beanFactory      {@code BeanFactory}用于初步检索bean类型，后来用于解析实际bean
	 * @param controllerAdvice bean的{@code @ControllerAdvice}注解，如果尚未检索则为{@code null}
	 * @since 5.2
	 */
	public ControllerAdviceBean(String beanName, BeanFactory beanFactory, @Nullable ControllerAdvice controllerAdvice) {
		Assert.hasText(beanName, "Bean name must contain text");
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		Assert.isTrue(beanFactory.containsBean(beanName), () -> "BeanFactory [" + beanFactory +
				"] does not contain specified controller advice bean '" + beanName + "'");

		this.beanOrName = beanName;
		// 检查 bean 是否为单例
		this.isSingleton = beanFactory.isSingleton(beanName);
		// 获取 bean 的类型
		this.beanType = getBeanType(beanName, beanFactory);
		// 根据传入的 controllerAdvice 或 beanType 创建相应的类型谓词
		this.beanTypePredicate = (controllerAdvice != null ? createBeanTypePredicate(controllerAdvice) :
				createBeanTypePredicate(this.beanType));
		this.beanFactory = beanFactory;
	}


	/**
	 * 获取包含bean的排序值。
	 * <p>从Spring Framework 5.3开始，排序值使用以下算法懒惰地检索并缓存。
	 * 但是，需要注意的是，配置为作用域bean的{@link ControllerAdvice @ControllerAdvice} bean
	 * ——例如，作为请求作用域或会话作用域bean——不会被急切解析。因此，对于作用域的
	 * {@code @ControllerAdvice} beans，不会遵循{@link Ordered}。
	 * <ul>
	 * <li>如果{@linkplain #resolveBean 解析的bean}实现了{@link Ordered}，则使用
	 * {@link Ordered#getOrder()}返回的值。</li>
	 * <li>如果已知{@linkplain org.springframework.context.annotation.Bean 工厂方法}，
	 * 则使用{@link OrderUtils#getOrder(AnnotatedElement)}返回的值。
	 * <li>如果已知{@linkplain #getBeanType() bean类型}，则使用{@link OrderUtils#getOrder(Class, int)}
	 * 返回的值，{@link Ordered#LOWEST_PRECEDENCE}用作默认排序值。</li>
	 * <li>否则，使用{@link Ordered#LOWEST_PRECEDENCE}作为默认的后备排序值。</li>
	 * </ul>
	 *
	 * @see #resolveBean()
	 */
	@Override
	public int getOrder() {
		if (this.order == null) {
			// 如果排序值为空
			String beanName = null;
			Object resolvedBean = null;
			if (this.beanFactory != null && this.beanOrName instanceof String) {
				// 如果bean工厂不为空，并且bean实例或者bean名称是字符串类型
				beanName = (String) this.beanOrName;
				// 获取目标bean名称
				String targetBeanName = ScopedProxyUtils.getTargetBeanName(beanName);
				// 判断目标bean名称是否已被注册
				boolean isScopedProxy = this.beanFactory.containsBean(targetBeanName);
				// 避免对作用域代理的@ControllerAdvice bean进行急切解析，
				// 因为在上下文初始化期间尝试这样做会由于当前缺少作用域而导致异常。
				// 例如，在初始化期间HTTP请求或会话作用域未激活。
				if (!isScopedProxy && !ScopedProxyUtils.isScopedTarget(beanName)) {
					// 如果目标bean名称未被注册，并且不是作用域目标，解析bean
					resolvedBean = resolveBean();
				}
			} else {
				// bean工厂不存在，或者beanOrName不是字符串类型，解析bean
				resolvedBean = resolveBean();
			}

			if (resolvedBean instanceof Ordered) {
				// 如果解析好的bean 实现了 Ordered 接口，则获取排序值
				this.order = ((Ordered) resolvedBean).getOrder();
			} else {
				if (beanName != null && this.beanFactory instanceof ConfigurableBeanFactory) {
					// 如果bean名称不为空，并且bean工厂是可配置bean工厂
					ConfigurableBeanFactory cbf = (ConfigurableBeanFactory) this.beanFactory;
					try {
						// 获取该bean名称的合并bean定义
						BeanDefinition bd = cbf.getMergedBeanDefinition(beanName);
						if (bd instanceof RootBeanDefinition) {
							// 如果 bean定义 是根bean定义，获取解析好的工厂方法
							Method factoryMethod = ((RootBeanDefinition) bd).getResolvedFactoryMethod();
							if (factoryMethod != null) {
								// 如果解析好的工厂方法存在，则通过OrderUtils获取排序值
								this.order = OrderUtils.getOrder(factoryMethod);
							}
						}
					} catch (NoSuchBeanDefinitionException ex) {
						// 忽略 -> 可能是手动注册的单例
					}
				}
				if (this.order == null) {
					// 如果排序值仍为空
					if (this.beanType != null) {
						// 如果bean类型存在，通过OrderUtils获取bean类型的排序值
						this.order = OrderUtils.getOrder(this.beanType, Ordered.LOWEST_PRECEDENCE);
					} else {
						// 否则将排序值设置为最低排序值
						this.order = Ordered.LOWEST_PRECEDENCE;
					}
				}
			}
		}
		return this.order;
	}

	/**
	 * 返回包含bean的类型。
	 * <p>如果bean类型是CGLIB生成的类，则返回原始的用户定义类。
	 */
	@Nullable
	public Class<?> getBeanType() {
		return this.beanType;
	}

	/**
	 * 获取此{@code ControllerAdviceBean}的bean实例，如果需要，通过{@link BeanFactory}解析bean名称。
	 * <p>从Spring Framework 5.2开始，一旦解析了bean实例，如果它是单例，将会缓存它，
	 * 从而避免在{@code BeanFactory}中的重复查找。
	 */
	public Object resolveBean() {
		if (this.resolvedBean == null) {
			// 如果this.resolvedBean为null，则this.beanOrName必须是表示bean名称的字符串。
			Object resolvedBean = obtainBeanFactory().getBean((String) this.beanOrName);
			// 不缓存非单例（例如，原型）。
			if (!this.isSingleton) {
				// 不是单例，则返回解析好的bean
				return resolvedBean;
			}
			this.resolvedBean = resolvedBean;
		}
		return this.resolvedBean;
	}

	private BeanFactory obtainBeanFactory() {
		Assert.state(this.beanFactory != null, "No BeanFactory set");
		return this.beanFactory;
	}

	/**
	 * 检查给定的bean类型是否应由此{@code ControllerAdviceBean}建议。
	 *
	 * @param beanType 要检查的bean类型
	 * @see ControllerAdvice
	 * @since 4.0
	 */
	public boolean isApplicableToBeanType(@Nullable Class<?> beanType) {
		return this.beanTypePredicate.test(beanType);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ControllerAdviceBean)) {
			return false;
		}
		ControllerAdviceBean otherAdvice = (ControllerAdviceBean) other;
		return (this.beanOrName.equals(otherAdvice.beanOrName) && this.beanFactory == otherAdvice.beanFactory);
	}

	@Override
	public int hashCode() {
		return this.beanOrName.hashCode();
	}

	@Override
	public String toString() {
		return this.beanOrName.toString();
	}


	/**
	 * 查找给定{@link ApplicationContext}中带有{@link ControllerAdvice @ControllerAdvice}注解的bean，
	 * 并将它们包装为{@code ControllerAdviceBean}实例。
	 * <p>自Spring Framework 5.2以来，返回列表中的{@code ControllerAdviceBean}实例
	 * 使用{@link OrderComparator#sort(List)}进行排序。
	 *
	 * @see #getOrder()
	 * @see OrderComparator
	 * @see Ordered
	 */
	public static List<ControllerAdviceBean> findAnnotatedBeans(ApplicationContext context) {
		// 如果上下文是 ConfigurableApplicationContext 类型，则使用其内部 BeanFactory
		ListableBeanFactory beanFactory = context;
		if (context instanceof ConfigurableApplicationContext) {
			// 使用内部BeanFactory以便上面潜在地向下转换为ConfigurableBeanFactory
			beanFactory = ((ConfigurableApplicationContext) context).getBeanFactory();
		}

		// 初始化 ControllerAdviceBean 列表
		List<ControllerAdviceBean> adviceBeans = new ArrayList<>();

		// 遍历上下文中所有的 bean 名称
		for (String name : BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Object.class)) {
			// 如果不是作用域目标
			if (!ScopedProxyUtils.isScopedTarget(name)) {
				// 在 bean 上查找 ControllerAdvice 注解
				ControllerAdvice controllerAdvice = beanFactory.findAnnotationOnBean(name, ControllerAdvice.class);
				if (controllerAdvice != null) {
					// 使用findAnnotationOnBean()找到的@ControllerAdvice注解
					// 以避免后续对相同注解的查找。
					// 如果找到了 ControllerAdvice 注解，则将其封装成 ControllerAdviceBean 并添加到列表中
					adviceBeans.add(new ControllerAdviceBean(name, beanFactory, controllerAdvice));
				}
			}
		}

		// 按照 OrderComparator 进行排序
		OrderComparator.sort(adviceBeans);

		// 返回排序后的 ControllerAdviceBean 列表
		return adviceBeans;
	}

	@Nullable
	private static Class<?> getBeanType(String beanName, BeanFactory beanFactory) {
		// 获取 bean 的类型
		Class<?> beanType = beanFactory.getType(beanName);

		// 返回用户定义的类，如果无法获取则返回 null
		return (beanType != null ? ClassUtils.getUserClass(beanType) : null);
	}

	private static HandlerTypePredicate createBeanTypePredicate(@Nullable Class<?> beanType) {
		// 获取 ControllerAdvice 注解
		ControllerAdvice controllerAdvice = (beanType != null ?
				AnnotatedElementUtils.findMergedAnnotation(beanType, ControllerAdvice.class) : null);

		// 创建 Bean 类型谓词
		return createBeanTypePredicate(controllerAdvice);
	}

	private static HandlerTypePredicate createBeanTypePredicate(@Nullable ControllerAdvice controllerAdvice) {
		// 如果存在 ControllerAdvice 注解
		if (controllerAdvice != null) {
			// 构建 HandlerTypePredicate
			return HandlerTypePredicate.builder()
					.basePackage(controllerAdvice.basePackages())
					.basePackageClass(controllerAdvice.basePackageClasses())
					.assignableType(controllerAdvice.assignableTypes())
					.annotation(controllerAdvice.annotations())
					.build();
		}
		// 否则返回任意 HandlerTypePredicate
		return HandlerTypePredicate.forAnyHandlerType();
	}

}
