/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.*;
import org.springframework.core.OrderComparator;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.log.LogMessage;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import javax.inject.Provider;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Spring 默认实现了 ConfigurableListableBeanFactory 和 BeanDefinitionRegistry 接口的类：
 * 一个基于 bean 定义元数据的功能齐全的 bean 工厂，可通过后处理器进行扩展。
 *
 * <p>典型的用法是首先注册所有的 bean 定义（可能是从 bean 定义文件中读取的），然后再访问 bean。
 * 因此，按名称查找 bean 在一个本地 bean 定义表中是一个廉价的操作，操作对象是预解析的 bean 定义元数据对象。
 *
 * <p>需要注意的是，针对特定的 bean 定义格式的读取器通常是单独实现的，而不是作为 bean 工厂的子类：
 * 例如 {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}。
 *
 * <p>对于 {@link org.springframework.beans.factory.ListableBeanFactory} 接口的另一种实现，
 * 可以查看 {@link StaticListableBeanFactory}，该实现管理现有的 bean 实例，而不是基于 bean 定义创建新的实例。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Costin Leau
 * @author Chris Beams
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @see #registerBeanDefinition
 * @see #addBeanPostProcessor
 * @see #getBean
 * @see #resolveDependency
 * @since 16 April 2001
 */
@SuppressWarnings("serial")
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
		implements ConfigurableListableBeanFactory, BeanDefinitionRegistry, Serializable {

	/**
	 * javax注册程序提供者类
	 */
	@Nullable
	private static Class<?> javaxInjectProviderClass;

	static {
		try {
			javaxInjectProviderClass =
					ClassUtils.forName("javax.inject.Provider", DefaultListableBeanFactory.class.getClassLoader());
		} catch (ClassNotFoundException ex) {
			// JSR-330 API不可用-提供者接口根本不支持。
			javaxInjectProviderClass = null;
		}
	}


	/**
	 * 从序列化id映射到工厂实例。
	 */
	private static final Map<String, Reference<DefaultListableBeanFactory>> serializableFactories =
			new ConcurrentHashMap<>(8);

	/**
	 * 此工厂的可选id，用于序列化目的。
	 */
	@Nullable
	private String serializationId;

	/**
	 * 是否允许重新注册具有相同名称的不同定义。
	 */
	private boolean allowBeanDefinitionOverriding = true;

	/**
	 * 是否允许早期的类加载，即使是懒惰的初始化 bean。
	 */
	private boolean allowEagerClassLoading = true;

	/**
	 * 依赖项列表和数组的可选排序比较器。
	 */
	@Nullable
	private Comparator<Object> dependencyComparator;

	/**
	 * 用于检查bean定义是否为自动装配候选的解析器。
	 */
	private AutowireCandidateResolver autowireCandidateResolver = SimpleAutowireCandidateResolver.INSTANCE;

	/**
	 * 从依赖类型映射到相应的自动装配值。
	 */
	private final Map<Class<?>, Object> resolvableDependencies = new ConcurrentHashMap<>(16);

	/**
	 * bean定义对象的映射，以bean名称为键。
	 */
	private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);

	/**
	 * 从bean名称映射到合并的BeanDefinitionHolder。
	 */
	private final Map<String, BeanDefinitionHolder> mergedBeanDefinitionHolders = new ConcurrentHashMap<>(256);

	/**
	 * 单例和非单例- bean名称构成的Map，按依赖类型键入。
	 */
	private final Map<Class<?>, String[]> allBeanNamesByType = new ConcurrentHashMap<>(64);

	/**
	 * 仅单例-bean名称 构成的Map，按依赖类型键入。
	 */
	private final Map<Class<?>, String[]> singletonBeanNamesByType = new ConcurrentHashMap<>(64);

	/**
	 * bean定义名称列表，按注册顺序排列。
	 */
	private volatile List<String> beanDefinitionNames = new ArrayList<>(256);

	/**
	 * 按注册顺序手动注册的单例名称列表。
	 */
	private volatile Set<String> manualSingletonNames = new LinkedHashSet<>(16);

	/**
	 * 在冻结配置的情况下，缓存bean定义名称的数组。
	 */
	@Nullable
	private volatile String[] frozenBeanDefinitionNames;

	/**
	 * 是否可以缓存所有bean的bean定义元数据。
	 */
	private volatile boolean configurationFrozen;


	/**
	 * 创建一个新的 DefaultListableBeanFactory。
	 */
	public DefaultListableBeanFactory() {
		super();
	}

	/**
	 * 创建一个具有给定父级的新的 DefaultListableBeanFactory。
	 *
	 * @param parentBeanFactory 父 BeanFactory
	 */
	public DefaultListableBeanFactory(@Nullable BeanFactory parentBeanFactory) {
		super(parentBeanFactory);
	}


	/**
	 * 指定一个 id 用于序列化目的，如果需要，允许将此 BeanFactory 从此 id 反序列化回 BeanFactory 对象。
	 *
	 * @param serializationId 序列化 id
	 */
	public void setSerializationId(@Nullable String serializationId) {
		//指定一个 id 用于序列化目的，如果需要，允许将此 BeanFactory 从此 id 反序列化回 BeanFactory 对象。
		if (serializationId != null) {
			serializableFactories.put(serializationId, new WeakReference<>(this));
		} else if (this.serializationId != null) {
			serializableFactories.remove(this.serializationId);
		}
		this.serializationId = serializationId;
	}

	/**
	 * 返回一个用于序列化目的的 id（如果指定），允许将此 BeanFactory 从此 id 反序列化回 BeanFactory 对象（如果需要）。
	 *
	 * @since 4.1.2
	 */
	@Nullable
	public String getSerializationId() {
		return this.serializationId;
	}

	/**
	 * 设置是否允许通过注册具有相同名称的不同定义来覆盖 bean 定义，自动替换前者。如果不允许，则将抛出异常。这也适用于覆盖别名。
	 * <p>默认值为 "true"。
	 *
	 * @param allowBeanDefinitionOverriding 是否允许覆盖 bean 定义
	 * @see #registerBeanDefinition
	 */
	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
	}

	/**
	 * 返回是否允许通过注册具有相同名称的不同定义来覆盖 bean 定义，自动替换前者。
	 *
	 * @return 是否允许覆盖 bean 定义
	 * @since 4.1.2
	 */
	public boolean isAllowBeanDefinitionOverriding() {
		return this.allowBeanDefinitionOverriding;
	}

	/**
	 * 设置是否允许工厂急切加载 bean 类，即使对于被标记为“lazy-init”的 bean 定义也是如此。
	 * <p>默认值为 "true"。关闭此标志以禁止为懒初始化的 bean 加载 bean 类，除非明确请求这样的 bean。
	 * 特别是，按类型查找将简单地忽略未解析类名的 bean 定义，而不是在需要时加载 bean 类来执行类型检查。
	 *
	 * @param allowEagerClassLoading 是否允许急切加载 bean 类
	 * @see AbstractBeanDefinition#setLazyInit
	 */
	public void setAllowEagerClassLoading(boolean allowEagerClassLoading) {
		this.allowEagerClassLoading = allowEagerClassLoading;
	}

	/**
	 * 返回工厂是否允许即使对于被标记为“lazy-init”的 bean 定义也急切加载 bean 类。
	 *
	 * @return 是否允许急切加载 bean 类
	 * @since 4.1.2
	 */
	public boolean isAllowEagerClassLoading() {
		return this.allowEagerClassLoading;
	}

	/**
	 * 设置用于依赖列表和数组的 {@link java.util.Comparator}。
	 *
	 * @param dependencyComparator 依赖比较器
	 * @see org.springframework.core.OrderComparator
	 * @see org.springframework.core.annotation.AnnotationAwareOrderComparator
	 * @since 4.0
	 */
	public void setDependencyComparator(@Nullable Comparator<Object> dependencyComparator) {
		this.dependencyComparator = dependencyComparator;
	}

	/**
	 * 返回此 BeanFactory 的依赖比较器（可能为 {@code null}）。
	 *
	 * @return 依赖比较器
	 * @since 4.0
	 */
	@Nullable
	public Comparator<Object> getDependencyComparator() {
		return this.dependencyComparator;
	}

	/**
	 * 设置此 BeanFactory 使用的自定义自动装配候选解析器，用于在决定是否将 bean 定义视为自动装配候选时使用。
	 *
	 * @param autowireCandidateResolver 自动装配候选解析器
	 * @throws IllegalArgumentException 如果 autowireCandidateResolver 为 null
	 */
	public void setAutowireCandidateResolver(AutowireCandidateResolver autowireCandidateResolver) {
		Assert.notNull(autowireCandidateResolver, "AutowireCandidateResolver must not be null");
		if (autowireCandidateResolver instanceof BeanFactoryAware) {
			// 如果autowireCandidateResolver实现了BeanFactoryAware接口，则设置BeanFactory
			if (System.getSecurityManager() != null) {
				// 如果存在安全管理器，则以特权方式设置 自动装配候选者解析器 的BeanFactory
				AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
					((BeanFactoryAware) autowireCandidateResolver).setBeanFactory(this);
					return null;
				}, getAccessControlContext());
			} else {
				// 否则直接设置 自动装配候选者解析器 的BeanFactory
				((BeanFactoryAware) autowireCandidateResolver).setBeanFactory(this);
			}
		}
		// 设置 自动装配候选者解析器
		this.autowireCandidateResolver = autowireCandidateResolver;
	}

	/**
	 * 返回此BeanFactory的自动装配候选解析器 (从不 {@code null})。
	 */
	public AutowireCandidateResolver getAutowireCandidateResolver() {
		return this.autowireCandidateResolver;
	}


	@Override
	public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
		super.copyConfigurationFrom(otherFactory);
		// 如果otherFactory是DefaultListableBeanFactory类型，则复制其特有配置
		if (otherFactory instanceof DefaultListableBeanFactory) {
			DefaultListableBeanFactory otherListableFactory = (DefaultListableBeanFactory) otherFactory;
			// 复制是否允许覆盖bean定义的标志
			this.allowBeanDefinitionOverriding = otherListableFactory.allowBeanDefinitionOverriding;
			// 复制是否允许急加载类的标志
			this.allowEagerClassLoading = otherListableFactory.allowEagerClassLoading;
			// 复制依赖比较器
			this.dependencyComparator = otherListableFactory.dependencyComparator;
			// 复制自动装配候选者解析器
			setAutowireCandidateResolver(otherListableFactory.getAutowireCandidateResolver().cloneIfNecessary());
			// 将可解析的依赖项（例如ResourceLoader）也放在这里
			this.resolvableDependencies.putAll(otherListableFactory.resolvableDependencies);
		}
	}


	//---------------------------------------------------------------------
	// Implementation of remaining BeanFactory methods
	//---------------------------------------------------------------------

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		return getBean(requiredType, (Object[]) null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getBean(Class<T> requiredType, @Nullable Object... args) throws BeansException {
		Assert.notNull(requiredType, "Required type must not be null");
		// 解析所需类型的Bean
		Object resolved = resolveBean(ResolvableType.forRawClass(requiredType), args, false);
		if (resolved == null) {
			// 如果解析结果为null，则抛出NoSuchBeanDefinitionException异常
			throw new NoSuchBeanDefinitionException(requiredType);
		}
		// 将解析结果转换为所需类型，并返回
		return (T) resolved;
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
		Assert.notNull(requiredType, "Required type must not be null");
		return getBeanProvider(ResolvableType.forRawClass(requiredType), true);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
		return getBeanProvider(requiredType, true);
	}


	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public boolean containsBeanDefinition(String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		return this.beanDefinitionMap.containsKey(beanName);
	}

	@Override
	public int getBeanDefinitionCount() {
		return this.beanDefinitionMap.size();
	}

	@Override
	public String[] getBeanDefinitionNames() {
		// 获取冻结的Bean定义名称数组
		String[] frozenNames = this.frozenBeanDefinitionNames;
		// 如果冻结的Bean定义名称数组不为null，则返回其克隆
		if (frozenNames != null) {
			return frozenNames.clone();
		} else {
			// 否则，将Bean定义名称列表转换为字符串数组并返回
			return StringUtils.toStringArray(this.beanDefinitionNames);
		}
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
		Assert.notNull(requiredType, "Required type must not be null");
		return getBeanProvider(ResolvableType.forRawClass(requiredType), allowEagerInit);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit) {
		return new BeanObjectProvider<T>() {
			@Override
			public T getObject() throws BeansException {
				// 解析指定类型的Bean
				T resolved = resolveBean(requiredType, null, false);
				// 如果解析结果为null，则抛出NoSuchBeanDefinitionException异常
				if (resolved == null) {
					throw new NoSuchBeanDefinitionException(requiredType);
				}
				// 返回解析结果
				return resolved;
			}

			@Override
			public T getObject(Object... args) throws BeansException {
				// 解析指定类型的Bean
				T resolved = resolveBean(requiredType, args, false);
				// 如果解析结果为null，则抛出NoSuchBeanDefinitionException异常
				if (resolved == null) {
					throw new NoSuchBeanDefinitionException(requiredType);
				}
				// 返回解析结果
				return resolved;
			}

			@Override
			@Nullable
			public T getIfAvailable() throws BeansException {
				try {
					// 尝试解析指定类型的Bean
					return resolveBean(requiredType, null, false);
				} catch (ScopeNotActiveException ex) {
					// 如果解析Bean时发生ScopeNotActiveException异常，则忽略，并返回null
					// 这通常表示在非活动范围中解析了Bean
					return null;
				}
			}

			@Override
			public void ifAvailable(Consumer<T> dependencyConsumer) throws BeansException {
				// 获取可用的依赖项
				T dependency = getIfAvailable();
				// 如果依赖项不为空，则尝试将其传递给依赖项消费者
				if (dependency != null) {
					try {
						dependencyConsumer.accept(dependency);
					} catch (ScopeNotActiveException ex) {
						// 如果在非活动的范围中解析了Bean，即使在作用域代理调用时也要忽略解析的Bean
						// 这通常表示在非活动的范围中解析了Bean
					}
				}
			}

			@Override
			@Nullable
			public T getIfUnique() throws BeansException {
				try {
					// 尝试解析指定类型的Bean
					return resolveBean(requiredType, null, true);
				} catch (ScopeNotActiveException ex) {
					// 如果在非活动的范围中解析了Bean，则忽略该异常
					return null;
				}
			}

			@Override
			public void ifUnique(Consumer<T> dependencyConsumer) throws BeansException {
				T dependency = getIfUnique();
				if (dependency != null) {
					try {
						// 尝试使用消费者接受解析的依赖项
						dependencyConsumer.accept(dependency);
					} catch (ScopeNotActiveException ex) {
						// 如果在非活动的范围中解析了依赖项，则忽略该异常，即使是在作用域代理调用中也是如此
					}
				}
			}

			@SuppressWarnings("unchecked")
			@Override
			public Stream<T> stream() {
				return Arrays.stream(getBeanNamesForTypedStream(requiredType, allowEagerInit))
						.map(name -> (T) getBean(name))
						.filter(bean -> !(bean instanceof NullBean));
			}

			@SuppressWarnings("unchecked")
			@Override
			public Stream<T> orderedStream() {
				String[] beanNames = getBeanNamesForTypedStream(requiredType, allowEagerInit);
				if (beanNames.length == 0) {
					// 如果没有找到与指定类型匹配的Bean名称，则返回一个空的Stream
					return Stream.empty();
				}
				// 使用有序的LinkedHashMap来存储匹配的Bean实例
				Map<String, T> matchingBeans = CollectionUtils.newLinkedHashMap(beanNames.length);
				for (String beanName : beanNames) {
					// 获取Bean实例
					Object beanInstance = getBean(beanName);
					// 排除NullBean实例
					if (!(beanInstance instanceof NullBean)) {
						matchingBeans.put(beanName, (T) beanInstance);
					}
				}
				// 使用适应排序比较器来对匹配的Bean实例进行排序
				Stream<T> stream = matchingBeans.values().stream();
				return stream.sorted(adaptOrderComparator(matchingBeans));

			}
		};
	}

	@Nullable
	private <T> T resolveBean(ResolvableType requiredType, @Nullable Object[] args, boolean nonUniqueAsNull) {
		// 解析Bean名称
		NamedBeanHolder<T> namedBean = resolveNamedBean(requiredType, args, nonUniqueAsNull);
		if (namedBean != null) {
			// 如果找到了符合条件的命名Bean，则返回其实例
			return namedBean.getBeanInstance();
		}
		// 获取父级Bean工厂
		BeanFactory parent = getParentBeanFactory();
		if (parent instanceof DefaultListableBeanFactory) {
			// 如果父级Bean工厂是DefaultListableBeanFactory类型，则调用其resolveBean方法解析Bean
			return ((DefaultListableBeanFactory) parent).resolveBean(requiredType, args, nonUniqueAsNull);
		} else if (parent != null) {
			// 如果父级Bean工厂存在，则获取对应的ObjectProvider
			ObjectProvider<T> parentProvider = parent.getBeanProvider(requiredType);
			if (args != null) {
				// 如果有参数，则调用getObject方法获取Bean实例
				return parentProvider.getObject(args);
			} else {
				// 如果没有参数，则根据条件获取Bean实例
				return (nonUniqueAsNull ? parentProvider.getIfUnique() : parentProvider.getIfAvailable());
			}
		}
		// 如果没有找到符合条件的Bean，则返回null
		return null;
	}

	private String[] getBeanNamesForTypedStream(ResolvableType requiredType, boolean allowEagerInit) {
		return BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this, requiredType, true, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type) {
		return getBeanNamesForType(type, true, true);
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
		Class<?> resolved = type.resolve();
		if (resolved != null && !type.hasGenerics()) {
			// 如果类型已解析，并且不包含泛型，则调用getBeanNamesForType方法获取对应类型的Bean名称数组
			return getBeanNamesForType(resolved, includeNonSingletons, allowEagerInit);
		} else {
			// 如果类型未解析或者包含泛型，则调用doGetBeanNamesForType方法进一步获取Bean名称数组
			return doGetBeanNamesForType(type, includeNonSingletons, allowEagerInit);
		}
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type) {
		return getBeanNamesForType(type, true, true);
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		if (!isConfigurationFrozen() || type == null || !allowEagerInit) {
			// 如果配置未冻结，或者类型为null，或者不允许提前初始化，则调用doGetBeanNamesForType方法获取Bean名称数组
			return doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, allowEagerInit);
		}
		// 从缓存中获取类型对应的Bean名称数组
		Map<Class<?>, String[]> cache = (includeNonSingletons ? this.allBeanNamesByType : this.singletonBeanNamesByType);
		String[] resolvedBeanNames = cache.get(type);
		if (resolvedBeanNames != null) {
			// 如果缓存中存在对应的Bean名称数组，则直接返回
			return resolvedBeanNames;
		}
		// 调用doGetBeanNamesForType方法获取Bean名称数组
		resolvedBeanNames = doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, true);
		// 如果类型是缓存安全的，则将Bean名称数组放入缓存中
		if (ClassUtils.isCacheSafe(type, getBeanClassLoader())) {
			cache.put(type, resolvedBeanNames);
		}
		return resolvedBeanNames;
	}

	private String[] doGetBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
		List<String> result = new ArrayList<>();

		// 检查所有的Bean定义。
		for (String beanName : this.beanDefinitionNames) {
			// 仅当Bean名称未定义为其他Bean的别名时才将其视为符合条件的Bean。
			if (!isAlias(beanName)) {
				try {
					// 获取合并后的本地Bean定义。
					RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
					// 仅在Bean定义完整时才检查Bean定义。
					if (!mbd.isAbstract() && (allowEagerInit ||
							(mbd.hasBeanClass() || !mbd.isLazyInit() || isAllowEagerClassLoading()) &&
									!requiresEagerInitForType(mbd.getFactoryBeanName()))) {
						// 判断是否为FactoryBean。
						boolean isFactoryBean = isFactoryBean(beanName, mbd);
						// 获取装饰的Bean定义。
						BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
						boolean matchFound = false;
						// 是否允许FactoryBean初始化。
						boolean allowFactoryBeanInit = (allowEagerInit || containsSingleton(beanName));
						// 是否非延迟装饰。
						boolean isNonLazyDecorated = (dbd != null && !mbd.isLazyInit());
						if (!isFactoryBean) {
							// 非FactoryBean情况下，如果允许包含非单例Bean，或者该Bean是单例的，则进行类型匹配检查。
							if (includeNonSingletons || isSingleton(beanName, mbd, dbd)) {
								matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
							}
						} else {
							// FactoryBean情况下，如果允许包含非单例Bean，或者是非延迟装饰的Bean，或者允许FactoryBean初始化并且是单例的，则进行类型匹配检查。
							if (includeNonSingletons || isNonLazyDecorated ||
									(allowFactoryBeanInit && isSingleton(beanName, mbd, dbd))) {
								matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
							}
							if (!matchFound) {
								// 如果未找到匹配的Bean，并且是FactoryBean，尝试匹配FactoryBean实例本身。
								beanName = FACTORY_BEAN_PREFIX + beanName;
								if (includeNonSingletons || isSingleton(beanName, mbd, dbd)) {
									matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
								}
							}
						}
						if (matchFound) {
							result.add(beanName);
						}
					}
				} catch (CannotLoadBeanClassException | BeanDefinitionStoreException ex) {
					if (allowEagerInit) {
						throw ex;
					}
					// 可能是占位符：让我们忽略它以用于类型匹配目的。
					LogMessage message = (ex instanceof CannotLoadBeanClassException ?
							LogMessage.format("Ignoring bean class loading failure for bean '%s'", beanName) :
							LogMessage.format("Ignoring unresolvable metadata in bean definition '%s'", beanName));
					logger.trace(message, ex);
					// 注册异常，以防Bean意外无法解析。
					onSuppressedException(ex);
				} catch (NoSuchBeanDefinitionException ex) {
					// 在迭代过程中，Bean定义被移除 -> 忽略。
				}
			}
		}

		// 还需要检查手动注册的单例Bean。
		for (String beanName : this.manualSingletonNames) {
			try {
				// 如果是FactoryBean，匹配FactoryBean创建的对象。
				if (isFactoryBean(beanName)) {
					if ((includeNonSingletons || isSingleton(beanName)) && isTypeMatch(beanName, type)) {
						result.add(beanName);
						// 对于此Bean找到了匹配项：不再匹配FactoryBean本身。
						continue;
					}
					// 如果是FactoryBean，尝试匹配FactoryBean本身。
					beanName = FACTORY_BEAN_PREFIX + beanName;
				}
				// 匹配原始Bean实例（可能是原始FactoryBean）。
				if (isTypeMatch(beanName, type)) {
					result.add(beanName);
				}
			} catch (NoSuchBeanDefinitionException ex) {
				// 不应该发生 - 可能是循环引用解析的结果...
				logger.trace(LogMessage.format("Failed to check manually registered singleton with name '%s'", beanName), ex);
			}
		}

		return StringUtils.toStringArray(result);
	}

	/**
	 * 判断给定的 bean 是否为单例。
	 *
	 * @param beanName bean 的名称
	 * @param mbd      根 bean 定义
	 * @param dbd      从父 bean 工厂中获取的 bean 定义
	 * @return 如果是单例则返回 true，否则返回 false
	 */
	private boolean isSingleton(String beanName, RootBeanDefinition mbd, @Nullable BeanDefinitionHolder dbd) {
		return (dbd != null ? mbd.isSingleton() : isSingleton(beanName));
	}

	/**
	 * 检查指定的 bean 是否需要在 eager 初始化以确定其类型。
	 *
	 * @param factoryBeanName bean 定义定义工厂方法的工厂 bean 引用
	 * @return 是否需要 eager 初始化
	 */
	private boolean requiresEagerInitForType(@Nullable String factoryBeanName) {
		return (factoryBeanName != null && isFactoryBean(factoryBeanName) && !containsSingleton(factoryBeanName));
	}

	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
		return getBeansOfType(type, true, true);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Map<String, T> getBeansOfType(
			@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {
		// 根据类型获取bean名称
		String[] beanNames = getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
		Map<String, T> result = CollectionUtils.newLinkedHashMap(beanNames.length);

		// 遍历所有的符合条件的Bean名称。
		for (String beanName : beanNames) {
			try {
				// 获取Bean实例。
				Object beanInstance = getBean(beanName);
				// 如果不是NullBean实例，则添加到结果Map中。
				if (!(beanInstance instanceof NullBean)) {
					result.put(beanName, (T) beanInstance);
				}
			} catch (BeanCreationException ex) {
				// 处理Bean创建异常。
				Throwable rootCause = ex.getMostSpecificCause();
				if (rootCause instanceof BeanCurrentlyInCreationException) {
					BeanCreationException bce = (BeanCreationException) rootCause;
					String exBeanName = bce.getBeanName();
					// 如果是循环引用，则忽略当前正在创建的Bean。
					if (exBeanName != null && isCurrentlyInCreation(exBeanName)) {
						if (logger.isTraceEnabled()) {
							logger.trace("Ignoring match to currently created bean '" + exBeanName + "': " +
									ex.getMessage());
						}
						// 忽略该异常，表示在自动装配构造函数时存在循环引用。
						// 我们希望找到的匹配项不包括当前正在创建的Bean本身。
						onSuppressedException(ex);
						continue;
					}
				}
				// 如果不是循环引用，则抛出异常。
				throw ex;
			}
		}
		return result;
	}

	@Override
	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
		List<String> result = new ArrayList<>();

		// 遍历所有的 BeanDefinition 名称。
		for (String beanName : this.beanDefinitionNames) {
			// 获取 BeanDefinition 对象。
			BeanDefinition bd = this.beanDefinitionMap.get(beanName);
			// 如果 BeanDefinition 存在且不是抽象的，并且具有指定的注解类型，则将其添加到结果列表中。
			if (bd != null && !bd.isAbstract() && findAnnotationOnBean(beanName, annotationType) != null) {
				result.add(beanName);
			}
		}

		// 再次遍历手动注册的单例 Bean 名称。
		for (String beanName : this.manualSingletonNames) {
			// 如果结果列表不包含该 Bean 名称，并且具有指定的注解类型，则将其添加到结果列表中。
			if (!result.contains(beanName) && findAnnotationOnBean(beanName, annotationType) != null) {
				result.add(beanName);
			}
		}
		return StringUtils.toStringArray(result);
	}

	/**
	 * 获取带有指定注解的所有 Bean 实例。
	 *
	 * @param annotationType 要搜索的注解类型
	 * @return 包含 Bean 名称和对应实例的映射
	 */
	@Override
	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
		// 获取带有指定注解的所有 Bean 名称
		String[] beanNames = getBeanNamesForAnnotation(annotationType);
		// 创建结果映射
		Map<String, Object> result = CollectionUtils.newLinkedHashMap(beanNames.length);
		// 遍历每个 Bean 名称
		for (String beanName : beanNames) {
			// 获取对应的 Bean 实例
			Object beanInstance = getBean(beanName);
			// 如果 Bean 实例不是 NullBean 类型，则加入到结果映射中
			if (!(beanInstance instanceof NullBean)) {
				result.put(beanName, beanInstance);
			}
		}
		return result;
	}

	@Override
	@Nullable
	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException {

		return findAnnotationOnBean(beanName, annotationType, true);
	}

	/**
	 * 在指定的 Bean 上查找指定类型的注解。
	 *
	 * @param beanName             要搜索的 Bean 名称
	 * @param annotationType       要查找的注解类型
	 * @param allowFactoryBeanInit 是否允许初始化 FactoryBean
	 * @param <A>                  注解类型
	 * @return 如果找到注解，则返回注解实例；否则返回 null
	 * @throws NoSuchBeanDefinitionException 如果找不到指定名称的 Bean
	 */
	@Override
	@Nullable
	public <A extends Annotation> A findAnnotationOnBean(
			String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException {

		// 查找指定 Bean 上合并的注解，并进行合成处理
		return findMergedAnnotationOnBean(beanName, annotationType, allowFactoryBeanInit)
				.synthesize(MergedAnnotation::isPresent).orElse(null);
	}

	/**
	 * 在指定的 Bean 上查找合并的注解。
	 *
	 * @param beanName             要搜索的 Bean 名称
	 * @param annotationType       要查找的注解类型
	 * @param allowFactoryBeanInit 是否允许初始化 FactoryBean
	 * @param <A>                  注解类型
	 * @return 合并的注解
	 */
	private <A extends Annotation> MergedAnnotation<A> findMergedAnnotationOnBean(
			String beanName, Class<A> annotationType, boolean allowFactoryBeanInit) {

		Class<?> beanType = getType(beanName, allowFactoryBeanInit);

		// 如果找到了 Bean 的类型，则尝试从其类型上获取指定注解的 MergedAnnotation。
		if (beanType != null) {
			MergedAnnotation<A> annotation =
					MergedAnnotations.from(beanType, SearchStrategy.TYPE_HIERARCHY).get(annotationType);
			// 如果注解存在，则返回该注解。
			if (annotation.isPresent()) {
				return annotation;
			}
		}

		if (containsBeanDefinition(beanName)) {
			// 如果 Bean 定义存在，获取合并本地bean定义
			RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
			// 检查原始 Bean 类型，例如代理情况下。
			if (bd.hasBeanClass()) {
				// 获取 Bean 类型
				Class<?> beanClass = bd.getBeanClass();
				// 如果 Bean 类型与之前不同，则尝试从 Bean 类型上获取指定注解的 MergedAnnotation。
				if (beanClass != beanType) {
					MergedAnnotation<A> annotation =
							MergedAnnotations.from(beanClass, SearchStrategy.TYPE_HIERARCHY).get(annotationType);
					// 如果注解存在，则返回该注解。
					if (annotation.isPresent()) {
						return annotation;
					}
				}
			}
			// 检查工厂方法上的声明的注解，如果有的话。
			Method factoryMethod = bd.getResolvedFactoryMethod();
			if (factoryMethod != null) {
				// 如果存在工厂方法，获取合并注解
				MergedAnnotation<A> annotation =
						MergedAnnotations.from(factoryMethod, SearchStrategy.TYPE_HIERARCHY).get(annotationType);
				// 如果注解存在，则返回该注解。
				if (annotation.isPresent()) {
					return annotation;
				}
			}
		}

		// 如果未找到注解，则返回缺失的 MergedAnnotation。
		return MergedAnnotation.missing();
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableListableBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public void registerResolvableDependency(Class<?> dependencyType, @Nullable Object autowiredValue) {
		Assert.notNull(dependencyType, "Dependency type must not be null");
		if (autowiredValue != null) {
			// 如果存在自动装配值
			if (!(autowiredValue instanceof ObjectFactory || dependencyType.isInstance(autowiredValue))) {
				throw new IllegalArgumentException("Value [" + autowiredValue +
						"] does not implement specified dependency type [" + dependencyType.getName() + "]");
			}
			// 添加进已解析依赖映射中
			this.resolvableDependencies.put(dependencyType, autowiredValue);
		}
	}

	@Override
	public boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor)
			throws NoSuchBeanDefinitionException {

		return isAutowireCandidate(beanName, descriptor, getAutowireCandidateResolver());
	}

	/**
	 * 确定指定的 Bean 定义是否符合自动装配候选资格，
	 * 可以注入到其他声明匹配类型依赖关系的 Bean 中。
	 *
	 * @param beanName   要检查的 Bean 定义的名称
	 * @param descriptor 要解析的依赖项描述符
	 * @param resolver   用于实际解析算法的 AutowireCandidateResolver
	 * @return 是否应将 Bean 视为自动装配候选项
	 * @throws NoSuchBeanDefinitionException 如果找不到相应的 Bean 定义
	 */
	protected boolean isAutowireCandidate(
			String beanName, DependencyDescriptor descriptor, AutowireCandidateResolver resolver)
			throws NoSuchBeanDefinitionException {
		// 将 Bean 名称转换为规范化的 BeanDefinition 名称。
		String bdName = BeanFactoryUtils.transformedBeanName(beanName);

		if (containsBeanDefinition(bdName)) {
			// 如果包含规范化的 BeanDefinition，则判断该 Bean 是否是自动装配候选对象。
			return isAutowireCandidate(beanName, getMergedLocalBeanDefinition(bdName), descriptor, resolver);
		} else if (containsSingleton(beanName)) {
			// 如果不包含规范化的 BeanDefinition，则尝试检查是否是单例 Bean。
			return isAutowireCandidate(beanName, new RootBeanDefinition(getType(beanName)), descriptor, resolver);
		}

		BeanFactory parent = getParentBeanFactory();
		if (parent instanceof DefaultListableBeanFactory) {
			// 如果父级 BeanFactory 是 DefaultListableBeanFactory，则委托给父级。
			return ((DefaultListableBeanFactory) parent).isAutowireCandidate(beanName, descriptor, resolver);
		} else if (parent instanceof ConfigurableListableBeanFactory) {
			// 如果父级 BeanFactory 是 ConfigurableListableBeanFactory，则无法传递解析器，只能进行基本的自动装配候选性检查。
			return ((ConfigurableListableBeanFactory) parent).isAutowireCandidate(beanName, descriptor);
		} else {
			// 如果父级 BeanFactory 不是以上两种类型，则默认为该 Bean 是自动装配候选对象。
			return true;
		}
	}

	/**
	 * 确定指定的 Bean 定义是否符合自动装配候选资格，
	 * 可以注入到其他声明匹配类型依赖关系的 Bean 中。
	 *
	 * @param beanName   要检查的 Bean 定义的名称
	 * @param mbd        要检查的合并 Bean 定义
	 * @param descriptor 要解析的依赖项描述符
	 * @param resolver   用于实际解析算法的 AutowireCandidateResolver
	 * @return 是否应将 Bean 视为自动装配候选项
	 */
	protected boolean isAutowireCandidate(String beanName, RootBeanDefinition mbd,
										  DependencyDescriptor descriptor, AutowireCandidateResolver resolver) {

		// 将 Bean 名称转换为规范化的 BeanDefinition 名称。
		String bdName = BeanFactoryUtils.transformedBeanName(beanName);

		// 解析 Bean 的类信息。
		resolveBeanClass(mbd, bdName);

		// 如果 Bean 的工厂方法是唯一的且尚未解析，则尝试解析工厂方法。
		if (mbd.isFactoryMethodUnique && mbd.factoryMethodToIntrospect == null) {
			new ConstructorResolver(this).resolveFactoryMethodIfPossible(mbd);
		}

		// 创建 BeanDefinitionHolder，该对象包含 BeanDefinition、Bean 名称以及 Bean 的别名。
		BeanDefinitionHolder holder = (beanName.equals(bdName) ?
				this.mergedBeanDefinitionHolders.computeIfAbsent(beanName,
						key -> new BeanDefinitionHolder(mbd, beanName, getAliases(bdName))) :
				new BeanDefinitionHolder(mbd, beanName, getAliases(bdName)));

		// 调用解析器的 isAutowireCandidate 方法，判断 Bean 是否是自动装配的候选对象。
		return resolver.isAutowireCandidate(holder, descriptor);
	}

	@Override
	public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		//根据bean名称 获取bean定义
		BeanDefinition bd = this.beanDefinitionMap.get(beanName);
		if (bd == null) {
			//如果bean定义为空，抛出异常
			if (logger.isTraceEnabled()) {
				logger.trace("No bean named '" + beanName + "' found in " + this);
			}
			throw new NoSuchBeanDefinitionException(beanName);
		}
		return bd;
	}

	@Override
	public Iterator<String> getBeanNamesIterator() {
		CompositeIterator<String> iterator = new CompositeIterator<>();
		iterator.add(this.beanDefinitionNames.iterator());
		iterator.add(this.manualSingletonNames.iterator());
		return iterator;
	}

	@Override
	protected void clearMergedBeanDefinition(String beanName) {
		//根据bean名称 清空父类方法的合并的bean定义
		super.clearMergedBeanDefinition(beanName);
		//将bean名称从合并bean定义持有者中删除
		this.mergedBeanDefinitionHolders.remove(beanName);
	}

	@Override
	public void clearMetadataCache() {
		super.clearMetadataCache();
		this.mergedBeanDefinitionHolders.clear();
		clearByTypeCache();
	}

	@Override
	public void freezeConfiguration() {
		this.configurationFrozen = true;
		this.frozenBeanDefinitionNames = StringUtils.toStringArray(this.beanDefinitionNames);
	}

	@Override
	public boolean isConfigurationFrozen() {
		return this.configurationFrozen;
	}

	/**
	 * 如果工厂的配置已标记为冻结，则将所有 Bean 视为符合元数据缓存的资格。
	 *
	 * @param beanName 要检查的 Bean 的名称
	 * @see #freezeConfiguration()
	 */
	@Override
	protected boolean isBeanEligibleForMetadataCaching(String beanName) {
		return (this.configurationFrozen || super.isBeanEligibleForMetadataCaching(beanName));
	}

	@Override
	public void preInstantiateSingletons() throws BeansException {
		if (logger.isTraceEnabled()) {
			logger.trace("Pre-instantiating singletons in " + this);
		}

		// 迭代一个副本以允许初始化方法注册新的 Bean 定义。
		// 虽然这可能不是常规工厂引导的一部分，但它在其他情况下运行良好。
		List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);

		// 触发所有非懒加载单例 Bean 的初始化...
		for (String beanName : beanNames) {
			// 获取合并后的本地 BeanDefinition。
			RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
			// 如果 Bean 不是抽象的、是单例的，并且不是懒加载的，则进行初始化。
			if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
				if (isFactoryBean(beanName)) {
					// 如果是 FactoryBean，则先获取 FactoryBean
					Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);
					if (bean instanceof FactoryBean) {
						// 如果bean是工厂bean
						FactoryBean<?> factory = (FactoryBean<?>) bean;
						boolean isEagerInit;
						// 如果存在安全管理器且 FactoryBean 是智能 FactoryBean，则使用安全管理器来获取是否提前初始化。
						if (System.getSecurityManager() != null && factory instanceof SmartFactoryBean) {
							isEagerInit = AccessController.doPrivileged(
									(PrivilegedAction<Boolean>) ((SmartFactoryBean<?>) factory)::isEagerInit,
									getAccessControlContext());
						} else {
							isEagerInit = (factory instanceof SmartFactoryBean &&
									((SmartFactoryBean<?>) factory).isEagerInit());
						}
						// 如果是提前初始化，则进行初始化。
						if (isEagerInit) {
							getBean(beanName);
						}
					}
				} else {
					// 如果不是 FactoryBean，则直接进行初始化。
					getBean(beanName);
				}
			}
		}

		// 触发所有适用 Bean 的后初始化回调...
		for (String beanName : beanNames) {
			// 获取单例 Bean 实例。
			Object singletonInstance = getSingleton(beanName);
			// 如果单例实例实现了 SmartInitializingSingleton 接口，则调用其 afterSingletonsInstantiated 方法。
			if (singletonInstance instanceof SmartInitializingSingleton) {
				// 使用应用程序启动进行智能初始化。
				StartupStep smartInitialize = this.getApplicationStartup().start("spring.beans.smart-initialize")
						.tag("beanName", beanName);
				SmartInitializingSingleton smartSingleton = (SmartInitializingSingleton) singletonInstance;
				// 如果存在安全管理器，则使用安全管理器来调用 afterSingletonsInstantiated 方法。
				if (System.getSecurityManager() != null) {
					AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
						smartSingleton.afterSingletonsInstantiated();
						return null;
					}, getAccessControlContext());
				} else {
					// 调用单例初始化后执行程序
					smartSingleton.afterSingletonsInstantiated();
				}
				// 结束智能初始化的启动步骤。
				smartInitialize.end();
			}
		}
	}


	//---------------------------------------------------------------------
	// BeanDefinitionRegistry接口的实现
	//---------------------------------------------------------------------

	@Override
	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {

		Assert.hasText(beanName, "Bean name must not be empty");
		Assert.notNull(beanDefinition, "BeanDefinition must not be null");

		if (beanDefinition instanceof AbstractBeanDefinition) {
			//如果是抽象的Bean定义
			try {
				//校验Bean定义是否合规
				((AbstractBeanDefinition) beanDefinition).validate();
			} catch (BeanDefinitionValidationException ex) {
				throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
						"Validation of bean definition failed", ex);
			}
		}
		//查看相同bean名称的Bean定义是否存在
		BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName);
		if (existingDefinition != null) {
			//如果有相同名称的bean定义存在
			if (!isAllowBeanDefinitionOverriding()) {
				//如果不允许重写Ben定义，抛出BeanDefinitionOverrideException
				throw new BeanDefinitionOverrideException(beanName, beanDefinition, existingDefinition);
			} else if (existingDefinition.getRole() < beanDefinition.getRole()) {
				//例如，是ROLE_APPLICATION，现在使用ROLE_SUPPORT或ROLE_INFRASTRUCTURE覆盖
				if (logger.isInfoEnabled()) {
					logger.info("Overriding user-defined bean definition for bean '" + beanName +
							"' with a framework-generated bean definition: replacing [" +
							existingDefinition + "] with [" + beanDefinition + "]");
				}
			} else if (!beanDefinition.equals(existingDefinition)) {
				//如果两个bean定义不相同，添加debug信息
				if (logger.isDebugEnabled()) {
					logger.debug("Overriding bean definition for bean '" + beanName +
							"' with a different definition: replacing [" + existingDefinition +
							"] with [" + beanDefinition + "]");
				}
			} else {
				if (logger.isTraceEnabled()) {
					logger.trace("Overriding bean definition for bean '" + beanName +
							"' with an equivalent definition: replacing [" + existingDefinition +
							"] with [" + beanDefinition + "]");
				}
			}
			//将该bean名称和bean定义添加进beanDefinitionMap中
			this.beanDefinitionMap.put(beanName, beanDefinition);
		} else {
			//如果有Bean已经开始创建了，通过alreadyCreated这个Set是否为空来判断。
			if (hasBeanCreationStarted()) {
				//不允许修改启动时集合元素，用于稳定迭代
				synchronized (this.beanDefinitionMap) {
					//添加bean名称和bean定义到beanDefinitionMap中
					this.beanDefinitionMap.put(beanName, beanDefinition);

					//region 确保多线程环境下对beanDefinitionNames的操作是安全的
					//添加beanName这个元素到this.beanDefinitionNames中。这里换了一个新的集合指向原来的beanDefinitionNames
					List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames.size() + 1);
					//添加所有的bean名称
					updatedDefinitions.addAll(this.beanDefinitionNames);
					updatedDefinitions.add(beanName);
					//将beanDefinitionNames替换为更新后的bean定义名称列表
					this.beanDefinitionNames = updatedDefinitions;
					//endregion

					//删除手动单例名称
					removeManualSingletonName(beanName);
				}
			} else {
				//仍然处在启动创建阶段，就不需要加锁了
				this.beanDefinitionMap.put(beanName, beanDefinition);
				//将bean名称添加到bean定义名称列表
				this.beanDefinitionNames.add(beanName);
				//手动删除单例名称
				removeManualSingletonName(beanName);
			}
			//初始化冻结的Bean定义名称数组
			this.frozenBeanDefinitionNames = null;
		}
		//如果不存在Bean定义，或者该BeanName是单例的。
		if (existingDefinition != null || containsSingleton(beanName)) {
			//如果已经存在相同名称的Bean定义，且单例Map中有该名称，重置该Bean名称的Bean定义
			resetBeanDefinition(beanName);
		} else if (isConfigurationFrozen()) {
			//如果配置是冻结的，根据类型缓存清空
			clearByTypeCache();
		}
	}

	@Override
	public void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		// 断言 beanName 不为空
		Assert.hasText(beanName, "'beanName' must not be empty");

		// 从 beanDefinitionMap 中移除 beanName 对应的 BeanDefinition
		BeanDefinition bd = this.beanDefinitionMap.remove(beanName);

		// 如果 BeanDefinition 为空，则抛出 NoSuchBeanDefinitionException 异常
		if (bd == null) {
			if (logger.isTraceEnabled()) {
				logger.trace("No bean named '" + beanName + "' found in " + this);
			}
			throw new NoSuchBeanDefinitionException(beanName);
		}

		if (hasBeanCreationStarted()) {
			// 如果 Bean 的创建已经开始，则不能修改启动时集合的元素（为了稳定的迭代）
			synchronized (this.beanDefinitionMap) {
				List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames);
				updatedDefinitions.remove(beanName);
				this.beanDefinitionNames = updatedDefinitions;
			}
		} else {
			// 如果仍在启动注册阶段，则移除 beanName
			this.beanDefinitionNames.remove(beanName);
		}

		// 清空冻结的 BeanDefinition 名称数组
		this.frozenBeanDefinitionNames = null;

		// 重置 beanName 对应的 BeanDefinition
		resetBeanDefinition(beanName);
	}

	/**
	 * 重置给定bean的所有bean定义缓存，包括从中派生的bean缓存。
	 * <p> 在替换或删除现有bean定义后调用，在给定bean和所有将给定bean作为父项的bean定义上触发
	 * {@link #clearMergedBeanDefinition} 、 {@link #destroySingleton} 和
	 * {@link MergedBeanDefinitionPostProcessor#resetBeanDefinition}。
	 *
	 * @param beanName 要重置的bean的名称
	 * @see #registerBeanDefinition
	 * @see #removeBeanDefinition
	 */
	protected void resetBeanDefinition(String beanName) {
		// 删除给定bean的合并bean定义 (如果已经创建)。
		clearMergedBeanDefinition(beanName);

		// 从单例缓存中删除相应的bean (如果有)。通常不应该是必要的，而只是意味着覆盖上下文的默认bean
		// (例如，StaticApplicationContext中的默认StaticMessageSource)。
		destroySingleton(beanName);

		// 通知所有后处理器指定的bean定义已被重置。
		for (MergedBeanDefinitionPostProcessor processor : getBeanPostProcessorCache().mergedDefinition) {
			//遍历所有的合并bean定义后置处理器，该处理器重置该bean定义
			processor.resetBeanDefinition(beanName);
		}

		//重置所有将给定bean作为父级的bean定义 (递归)。
		for (String bdName : this.beanDefinitionNames) {
			if (!beanName.equals(bdName)) {
				//如果bean名称和给定bean名称不相同，获取该bean名称的bean定义
				BeanDefinition bd = this.beanDefinitionMap.get(bdName);
				// 由于beanDefinitionMap的潜在并发修改，确保bd为非空。
				if (bd != null && beanName.equals(bd.getParentName())) {
					//如果原来的bean定义不为空，且该bean名称和bean定义的父bean名称相同，重新注册该bean定义
					resetBeanDefinition(bdName);
				}
			}
		}
	}

	/**
	 * 仅当允许 bean 定义覆盖时才允许别名覆盖。
	 */
	@Override
	protected boolean allowAliasOverriding() {
		return isAllowBeanDefinitionOverriding();
	}

	/**
	 * 还会检查别名是否覆盖了同名的 bean 定义。
	 */
	@Override
	protected void checkForAliasCircle(String name, String alias) {
		super.checkForAliasCircle(name, alias);
		if (!isAllowBeanDefinitionOverriding() && containsBeanDefinition(alias)) {
			throw new IllegalStateException("Cannot register alias '" + alias +
					"' for name '" + name + "': Alias would override bean definition '" + alias + "'");
		}
	}

	@Override
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		//调用父类方法注册单例
		super.registerSingleton(beanName, singletonObject);
		//如果bean定义Map中含有该bean名称，手动注册的单例名称列表添加该bean名称
		updateManualSingletonNames(set -> set.add(beanName), set -> !this.beanDefinitionMap.containsKey(beanName));
		//删除关于按类型映射的任何缓存。
		clearByTypeCache();
	}

	@Override
	public void destroySingletons() {
		// 销毁单例 Bean
		super.destroySingletons();

		// 更新手动注册的单例 Bean 名称集合
		updateManualSingletonNames(Set::clear, set -> !set.isEmpty());

		// 清除类型缓存
		clearByTypeCache();
	}

	@Override
	public void destroySingleton(String beanName) {
		//根据bean名称销毁单例
		super.destroySingleton(beanName);
		//手动清除bean单例名称
		removeManualSingletonName(beanName);
		//删除关于按类型映射的任何缓存。
		clearByTypeCache();
	}

	private void removeManualSingletonName(String beanName) {
		//如果含有BeanName则进行删除。
		updateManualSingletonNames(set -> set.remove(beanName), set -> set.contains(beanName));
	}

	/**
	 * 更新工厂的内部手动单例名称集。
	 *
	 * @param action    修改动作
	 * @param condition 修改动作的前提条件 (如果此条件不适用，则可以跳过该动作)
	 */
	private void updateManualSingletonNames(Consumer<Set<String>> action, Predicate<Set<String>> condition) {
		if (hasBeanCreationStarted()) {
			//无法再修改启动时集合元素（用于稳定迭代）
			synchronized (this.beanDefinitionMap) {
				if (condition.test(this.manualSingletonNames)) {
					//region 构建新的updatedSingletons，并通过updatedSingletons 删除beanName，最后重置manualSingletonNames，这确保在多线程环境下对manualSingletonNames 的安全操作
					//如果manualSingletonNames含有bean名称
					Set<String> updatedSingletons = new LinkedHashSet<>(this.manualSingletonNames);
					//updatedSingletons消费bean名称
					action.accept(updatedSingletons);
					//重置manualSingletonNames 为更新的后的单例集合
					this.manualSingletonNames = updatedSingletons;
					//endregion
				}
			}
		} else {
			// 仍处于启动注册阶段
			if (condition.test(this.manualSingletonNames)) {
				//如果manualSingletonNames含有bean名称，则直接从manualSingletonNames中删除bean名称
				action.accept(this.manualSingletonNames);
			}
		}
	}

	/**
	 * 删除关于按类型映射的任何假设。
	 */
	private void clearByTypeCache() {
		//清空所有 单例和非单例- bean名称构成的Map
		this.allBeanNamesByType.clear();
		//清空所有 仅单例-bean名称 构成的Map
		this.singletonBeanNamesByType.clear();
	}


	//---------------------------------------------------------------------
	// Dependency resolution functionality
	//---------------------------------------------------------------------

	@Override
	public <T> NamedBeanHolder<T> resolveNamedBean(Class<T> requiredType) throws BeansException {
		// 断言必需的类型不为空
		Assert.notNull(requiredType, "Required type must not be null");

		// 解析具名的 Bean
		NamedBeanHolder<T> namedBean = resolveNamedBean(ResolvableType.forRawClass(requiredType), null, false);

		// 如果找到了具名的 Bean，则返回该 Bean
		if (namedBean != null) {
			return namedBean;
		}

		// 获取父 Bean 工厂
		BeanFactory parent = getParentBeanFactory();

		// 如果父 Bean 工厂是可自动装配的 Bean 工厂，则委托给父 Bean 工厂解析具名的 Bean
		if (parent instanceof AutowireCapableBeanFactory) {
			return ((AutowireCapableBeanFactory) parent).resolveNamedBean(requiredType);
		}

		// 如果未找到具名的 Bean，则抛出 NoSuchBeanDefinitionException 异常
		throw new NoSuchBeanDefinitionException(requiredType);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private <T> NamedBeanHolder<T> resolveNamedBean(
			ResolvableType requiredType, @Nullable Object[] args, boolean nonUniqueAsNull) throws BeansException {

		// 断言必需的类型不为空
		Assert.notNull(requiredType, "Required type must not be null");

		// 获取给定类型的所有 Bean 名称
		String[] candidateNames = getBeanNamesForType(requiredType);

		if (candidateNames.length > 1) {
			// 如果有多个候选名，则进行筛选以确定自动装配的候选名
			List<String> autowireCandidates = new ArrayList<>(candidateNames.length);
			for (String beanName : candidateNames) {
				// 筛选出符合自动装配条件的候选名
				if (!containsBeanDefinition(beanName) || getBeanDefinition(beanName).isAutowireCandidate()) {
					autowireCandidates.add(beanName);
				}
			}
			// 如果存在自动装配的候选名，则使用它们
			if (!autowireCandidates.isEmpty()) {
				candidateNames = StringUtils.toStringArray(autowireCandidates);
			}
		}

		if (candidateNames.length == 1) {
			// 如果只有一个候选名，则解析该候选名的具名 Bean
			return resolveNamedBean(candidateNames[0], requiredType, args);
		} else if (candidateNames.length > 1) {
			// 如果存在多个候选名，则需要进一步处理
			// 创建一个 Map 用于存储候选名及其对应的 Bean 实例或类型
			Map<String, Object> candidates = CollectionUtils.newLinkedHashMap(candidateNames.length);
			for (String beanName : candidateNames) {
				// 如果是单例 Bean 且不需要构造参数，则直接获取 Bean 实例
				if (containsSingleton(beanName) && args == null) {
					Object beanInstance = getBean(beanName);
					// 如果是 NullBean，则将其视为空值
					candidates.put(beanName, (beanInstance instanceof NullBean ? null : beanInstance));
				} else {
					// 否则，只记录 Bean 的类型
					candidates.put(beanName, getType(beanName));
				}
			}
			// 确定主要的候选名
			String candidateName = determinePrimaryCandidate(candidates, requiredType.toClass());
			// 如果没有主要的候选名，则选择优先级最高的候选名
			if (candidateName == null) {
				candidateName = determineHighestPriorityCandidate(candidates, requiredType.toClass());
			}
			// 如果找到了候选名，则返回具名 Bean
			if (candidateName != null) {
				Object beanInstance = candidates.get(candidateName);
				// 如果 Bean 实例为空，则直接返回空
				if (beanInstance == null) {
					return null;
				}
				// 如果 Bean 实例是一个 Class，则解析该候选名的具名 Bean
				if (beanInstance instanceof Class) {
					return resolveNamedBean(candidateName, requiredType, args);
				}
				// 否则，返回具名 Bean
				return new NamedBeanHolder<>(candidateName, (T) beanInstance);
			}
			// 如果不允许多个候选名，则抛出 NoUniqueBeanDefinitionException 异常
			if (!nonUniqueAsNull) {
				throw new NoUniqueBeanDefinitionException(requiredType, candidates.keySet());
			}
		}

		// 如果没有找到符合条件的候选名，则返回空
		return null;
	}

	@Nullable
	private <T> NamedBeanHolder<T> resolveNamedBean(
			String beanName, ResolvableType requiredType, @Nullable Object[] args) throws BeansException {

		// 获取指定名称的 Bean 实例
		Object bean = getBean(beanName, null, args);

		// 如果 Bean 实例是 NullBean，则返回空
		if (bean instanceof NullBean) {
			return null;
		}

		// 将 Bean 实例适配为所需类型，并创建具名 Bean 持有者
		return new NamedBeanHolder<T>(beanName, adaptBeanInstance(beanName, bean, requiredType.toClass()));
	}

	/**
	 * 解析依赖关系的方法。
	 *
	 * @param descriptor         依赖描述符，包含了需要解析的依赖信息
	 * @param requestingBeanName 请求依赖的Bean的名称
	 * @param autowiredBeanNames 自动装配的Bean的名称集合
	 * @param typeConverter      类型转换器，用于将依赖的类型进行转换
	 * @return 解析得到的依赖对象
	 * @throws BeansException 如果解析依赖失败
	 */
	@Override
	@Nullable
	public Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName,
									@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {

		// 初始化参数名的发现器
		descriptor.initParameterNameDiscovery(getParameterNameDiscoverer());

		if (Optional.class == descriptor.getDependencyType()) {
			// 如果依赖的类型是Optional，则创建Optional类型的依赖
			return createOptionalDependency(descriptor, requestingBeanName);
		} else if (ObjectFactory.class == descriptor.getDependencyType() ||
				ObjectProvider.class == descriptor.getDependencyType()) {
			// 如果依赖的类型是ObjectFactory或ObjectProvider，则创建相应类型的依赖对象
			return new DependencyObjectProvider(descriptor, requestingBeanName);
		} else if (javaxInjectProviderClass == descriptor.getDependencyType()) {
			// 如果依赖的类型是javax.inject.Provider，则使用Jsr330Factory创建依赖提供者
			return new Jsr330Factory().createDependencyProvider(descriptor, requestingBeanName);
		} else {
			// 否则，尝试获取懒加载代理，如果获取不到，则执行实际的依赖解析
			Object result = getAutowireCandidateResolver().getLazyResolutionProxyIfNecessary(
					descriptor, requestingBeanName);
			if (result == null) {
				result = doResolveDependency(descriptor, requestingBeanName, autowiredBeanNames, typeConverter);
			}
			return result;
		}
	}

	/**
	 * 执行实际的依赖解析的方法。
	 *
	 * @param descriptor         依赖描述符，包含了需要解析的依赖信息
	 * @param beanName           Bean的名称，用于查找Bean的定义
	 * @param autowiredBeanNames 自动装配的Bean的名称集合
	 * @param typeConverter      类型转换器，用于将依赖的类型进行转换
	 * @return 解析得到的依赖对象
	 * @throws BeansException 如果解析依赖失败
	 */
	@Nullable
	public Object doResolveDependency(DependencyDescriptor descriptor, @Nullable String beanName,
									  @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {

		// 设置当前的注入点
		InjectionPoint previousInjectionPoint = ConstructorResolver.setCurrentInjectionPoint(descriptor);
		try {
			// 尝试解析依赖的快捷方式
			Object shortcut = descriptor.resolveShortcut(this);
			if (shortcut != null) {
				return shortcut;
			}

			// 获取依赖的类型和候选值
			Class<?> type = descriptor.getDependencyType();
			// 获取自动装配解析器后，尝试获取候选值
			Object value = getAutowireCandidateResolver().getSuggestedValue(descriptor);

			// 如果有建议的值，则尝试进行类型转换
			if (value != null) {
				if (value instanceof String) {
					// 如果值是字符串，则解析嵌入的值
					String strVal = resolveEmbeddedValue((String) value);
					// 获取bean定义
					BeanDefinition bd = (beanName != null && containsBean(beanName) ?
							getMergedBeanDefinition(beanName) : null);
					// 评估bean定义字符串，并解析出值
					value = evaluateBeanDefinitionString(strVal, bd);
				}
				// 获取类型转换器
				TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
				try {
					//进行类型转换
					return converter.convertIfNecessary(value, type, descriptor.getTypeDescriptor());
				} catch (UnsupportedOperationException ex) {
					// 自定义的TypeConverter不支持TypeDescriptor解析...
					return (descriptor.getField() != null ?
							converter.convertIfNecessary(value, type, descriptor.getField()) :
							converter.convertIfNecessary(value, type, descriptor.getMethodParameter()));
				}
			}

			// 尝试解析多个候选值
			Object multipleBeans = resolveMultipleBeans(descriptor, beanName, autowiredBeanNames, typeConverter);
			if (multipleBeans != null) {
				return multipleBeans;
			}

			// 查找符合自动装配要求的候选Bean
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
			if (matchingBeans.isEmpty()) {
				// 如果没有符合条件的Bean，根据isRequired判断是否抛出异常
				if (isRequired(descriptor)) {
					raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
				}
				return null;
			}

			// 自动装配的bean名称
			String autowiredBeanName;
			// 候选者实例
			Object instanceCandidate;

			if (matchingBeans.size() > 1) {
				// 有多个匹配的Bean时，确定自动装配的候选Bean
				// 按照@Primary和@Priority的顺序
				autowiredBeanName = determineAutowireCandidate(matchingBeans, descriptor);
				if (autowiredBeanName == null) {
					if (isRequired(descriptor) || !indicatesMultipleBeans(type)) {
						// 如果需要唯一的Bean或不支持多个Bean时，抛出异常
						return descriptor.resolveNotUnique(descriptor.getResolvableType(), matchingBeans);
					} else {
						// 在可选的Collection/Map的情况下，静默忽略非唯一的情况
						return null;
					}
				}
				instanceCandidate = matchingBeans.get(autowiredBeanName);
			} else {
				// 只有一个匹配的Bean时，直接获取
				Map.Entry<String, Object> entry = matchingBeans.entrySet().iterator().next();
				autowiredBeanName = entry.getKey();
				instanceCandidate = entry.getValue();
			}

			if (autowiredBeanNames != null) {
				autowiredBeanNames.add(autowiredBeanName);
			}
			if (instanceCandidate instanceof Class) {
				// 如果候选值是Class类型，则通过resolveCandidate方法解析
				instanceCandidate = descriptor.resolveCandidate(autowiredBeanName, type, this);
			}
			Object result = instanceCandidate;
			if (result instanceof NullBean) {
				// 如果候选值是NullBean，且为必需依赖，则抛出异常
				if (isRequired(descriptor)) {
					raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
				}
				result = null;
			}
			if (!ClassUtils.isAssignableValue(type, result)) {
				// 如果解析得到的依赖对象不是指定类型的子类，则抛出异常
				throw new BeanNotOfRequiredTypeException(autowiredBeanName, type, instanceCandidate.getClass());
			}
			return result;
		} finally {
			// 恢复先前的注入点
			ConstructorResolver.setCurrentInjectionPoint(previousInjectionPoint);
		}
	}

	@Nullable
	private Object resolveMultipleBeans(DependencyDescriptor descriptor, @Nullable String beanName,
										@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {

		// 获取依赖的类型
		Class<?> type = descriptor.getDependencyType();

		// 如果是流依赖描述符
		if (descriptor instanceof StreamDependencyDescriptor) {
			// 查找自动装配的候选 Bean
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
			// 如果需要记录自动装配的 Bean 名称，则添加到集合中
			if (autowiredBeanNames != null) {
				autowiredBeanNames.addAll(matchingBeans.keySet());
			}
			// 创建流，将匹配的 Bean 转换为流，并过滤掉空 Bean
			Stream<Object> stream = matchingBeans.keySet().stream()
					.map(name -> descriptor.resolveCandidate(name, type, this))
					.filter(bean -> !(bean instanceof NullBean));
			// 如果描述符指定排序，则按照适配的排序比较器进行排序
			if (((StreamDependencyDescriptor) descriptor).isOrdered()) {
				stream = stream.sorted(adaptOrderComparator(matchingBeans));
			}
			return stream;
		} else if (type.isArray()) {
			// 如果是数组类型
			// 获取数组元素类型
			Class<?> componentType = type.getComponentType();
			// 获取可解析的类型
			ResolvableType resolvableType = descriptor.getResolvableType();
			// 解析类型
			Class<?> resolvedArrayType = resolvableType.resolve(type);
			if (resolvedArrayType != type) {
				// 如果解析的类型不是数组类型，则获取组件类型，并解析
				componentType = resolvableType.getComponentType().resolve();
			}
			// 如果组件类型为空，则返回空
			if (componentType == null) {
				return null;
			}
			// 查找自动装配的候选 Bean
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, componentType,
					new MultiElementDescriptor(descriptor));
			// 如果没有匹配的 Bean，则返回空
			if (matchingBeans.isEmpty()) {
				return null;
			}
			// 如果需要记录自动装配的 Bean 名称，则添加到集合中
			if (autowiredBeanNames != null) {
				autowiredBeanNames.addAll(matchingBeans.keySet());
			}
			// 获取类型转换器，并将匹配的 Bean 转换为数组类型
			TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
			Object result = converter.convertIfNecessary(matchingBeans.values(), resolvedArrayType);
			// 如果结果是数组，则根据适配的依赖比较器进行排序
			if (result instanceof Object[]) {
				Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
				if (comparator != null) {
					Arrays.sort((Object[]) result, comparator);
				}
			}
			return result;
		} else if (Collection.class.isAssignableFrom(type) && type.isInterface()) {
			// 如果是集合类型
			// 获取集合元素类型
			Class<?> elementType = descriptor.getResolvableType().asCollection().resolveGeneric();
			// 如果元素类型为空，则返回空
			if (elementType == null) {
				return null;
			}
			// 查找自动装配的候选 Bean
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, elementType,
					new MultiElementDescriptor(descriptor));
			// 如果没有匹配的 Bean，则返回空
			if (matchingBeans.isEmpty()) {
				return null;
			}
			// 如果需要记录自动装配的 Bean 名称，则添加到集合中
			if (autowiredBeanNames != null) {
				autowiredBeanNames.addAll(matchingBeans.keySet());
			}
			// 获取类型转换器，并将匹配的 Bean 转换为集合类型
			TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
			Object result = converter.convertIfNecessary(matchingBeans.values(), type);
			// 如果结果是列表，则根据适配的依赖比较器进行排序
			if (result instanceof List) {
				if (((List<?>) result).size() > 1) {
					Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
					if (comparator != null) {
						((List<?>) result).sort(comparator);
					}
				}
			}
			return result;
		} else if (Map.class == type) {
			// 如果是 Map 类型
			// 获取键类型和值类型
			ResolvableType mapType = descriptor.getResolvableType().asMap();
			//解析第一个泛型类型
			Class<?> keyType = mapType.resolveGeneric(0);
			if (String.class != keyType) {
				return null;
			}
			//解析第二个泛型类型
			Class<?> valueType = mapType.resolveGeneric(1);
			if (valueType == null) {
				return null;
			}
			// 查找自动装配的候选 Bean
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, valueType,
					new MultiElementDescriptor(descriptor));
			// 如果没有匹配的 Bean，则返回空
			if (matchingBeans.isEmpty()) {
				return null;
			}
			// 如果需要记录自动装配的 Bean 名称，则添加到集合中
			if (autowiredBeanNames != null) {
				autowiredBeanNames.addAll(matchingBeans.keySet());
			}
			return matchingBeans;
		} else {
			return null;
		}
	}

	/**
	 * 检查依赖描述符是否为必需的。
	 *
	 * @param descriptor 依赖描述符
	 * @return 如果依赖描述符为必需，则为 true
	 */
	private boolean isRequired(DependencyDescriptor descriptor) {
		return getAutowireCandidateResolver().isRequired(descriptor);
	}

	/**
	 * 判断指定类型是否表示多个 bean。
	 *
	 * @param type 类型
	 * @return 如果类型表示数组、集合或映射，则为 true
	 */
	private boolean indicatesMultipleBeans(Class<?> type) {
		return (type.isArray() || (type.isInterface() &&
				(Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type))));
	}

	@Nullable
	private Comparator<Object> adaptDependencyComparator(Map<String, ?> matchingBeans) {
		// 获取依赖比较器
		Comparator<Object> comparator = getDependencyComparator();
		// 如果比较器是 OrderComparator 类型，则设置源提供者为工厂感知的排序源提供者
		if (comparator instanceof OrderComparator) {
			return ((OrderComparator) comparator).withSourceProvider(
					createFactoryAwareOrderSourceProvider(matchingBeans));
		} else {
			// 否则直接返回比较器
			return comparator;
		}
	}

	/**
	 * 适配顺序比较器以考虑匹配的 bean。
	 *
	 * @param matchingBeans 匹配的 bean 的映射
	 * @return 适配后的比较器
	 */
	private Comparator<Object> adaptOrderComparator(Map<String, ?> matchingBeans) {
		Comparator<Object> dependencyComparator = getDependencyComparator();
		OrderComparator comparator = (dependencyComparator instanceof OrderComparator ?
				(OrderComparator) dependencyComparator : OrderComparator.INSTANCE);
		return comparator.withSourceProvider(createFactoryAwareOrderSourceProvider(matchingBeans));
	}

	/**
	 * 创建一个考虑工厂的顺序源提供程序。
	 *
	 * @param beans bean 的映射
	 * @return 工厂感知的顺序源提供程序
	 */
	private OrderComparator.OrderSourceProvider createFactoryAwareOrderSourceProvider(Map<String, ?> beans) {
		IdentityHashMap<Object, String> instancesToBeanNames = new IdentityHashMap<>();
		beans.forEach((beanName, instance) -> instancesToBeanNames.put(instance, beanName));
		return new FactoryAwareOrderSourceProvider(instancesToBeanNames);
	}

	/**
	 * 查找匹配所需类型的bean实例。
	 * 在自动装配指定的bean期间调用。
	 *
	 * @param beanName     即将被连接的bean的名称
	 * @param requiredType 要查找的bean的实际类型
	 *                     （可能是数组组件类型或集合元素类型）
	 * @param descriptor   要解析的依赖项的描述符
	 * @return 匹配所需类型的候选名称和候选实例的Map（永远不会为null）
	 * @throws BeansException 如果发生错误
	 * @see #autowireByType
	 * @see #autowireConstructor
	 */
	protected Map<String, Object> findAutowireCandidates(
			@Nullable String beanName, Class<?> requiredType, DependencyDescriptor descriptor) {

		// 获取指定类型的所有候选Bean的名称，包括祖先容器中的Bean
		String[] candidateNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
				this, requiredType, true, descriptor.isEager());
		// 用于存储找到的候选Bean的Map
		Map<String, Object> result = CollectionUtils.newLinkedHashMap(candidateNames.length);

		// 遍历已解析的依赖项
		for (Map.Entry<Class<?>, Object> classObjectEntry : this.resolvableDependencies.entrySet()) {
			Class<?> autowiringType = classObjectEntry.getKey();
			// 如果当前类型是所需类型的子类
			if (autowiringType.isAssignableFrom(requiredType)) {
				Object autowiringValue = classObjectEntry.getValue();
				// 解析自动装配的值
				autowiringValue = AutowireUtils.resolveAutowiringValue(autowiringValue, requiredType);
				// 如果解析后的值是所需类型的实例，则加入结果Map
				if (requiredType.isInstance(autowiringValue)) {
					result.put(ObjectUtils.identityToString(autowiringValue), autowiringValue);
					break;
				}
			}
		}

		// 遍历所有候选Bean的名称
		for (String candidate : candidateNames) {
			// 检查是否是自引用或是否是自动装配的候选项
			if (!isSelfReference(beanName, candidate) && isAutowireCandidate(candidate, descriptor)) {
				// 将候选项加入结果Map
				addCandidateEntry(result, candidate, descriptor, requiredType);
			}
		}

		// 如果结果为空，则考虑使用回退匹配
		if (result.isEmpty()) {
			// 指示是否期望多个Bean的标志
			boolean multiple = indicatesMultipleBeans(requiredType);
			// 为回退匹配创建描述符
			DependencyDescriptor fallbackDescriptor = descriptor.forFallbackMatch();

			// 再次遍历所有候选Bean的名称，进行回退匹配
			for (String candidate : candidateNames) {
				// 检查是否是自引用或是否是自动装配的候选项
				if (!isSelfReference(beanName, candidate) && isAutowireCandidate(candidate, fallbackDescriptor) &&
						(!multiple || getAutowireCandidateResolver().hasQualifier(descriptor))) {
					// 将回退匹配的候选项加入结果Map
					addCandidateEntry(result, candidate, descriptor, requiredType);
				}
			}

			// 如果结果仍为空且不期望多个Bean，则考虑自引用
			if (result.isEmpty() && !multiple) {
				// 再次遍历所有候选Bean的名称，进行自引用匹配
				for (String candidate : candidateNames) {
					// 检查是否是自引用且是否是自动装配的候选项
					if (isSelfReference(beanName, candidate) &&
							(!(descriptor instanceof MultiElementDescriptor) || !beanName.equals(candidate)) &&
							isAutowireCandidate(candidate, fallbackDescriptor)) {
						// 将自引用匹配的候选项加入结果Map
						addCandidateEntry(result, candidate, descriptor, requiredType);
					}
				}
			}
		}

		// 返回最终的结果Map
		return result;
	}

	/**
	 * 向候选对象映射中添加条目：如果可用则是一个 bean 实例，否则是已解析的类型，
	 * 避免在主要候选对象选择之前进行早期的 bean 初始化。
	 *
	 * @param candidates    候选对象映射
	 * @param candidateName 候选对象的名称
	 * @param descriptor    依赖描述符
	 * @param requiredType  所需类型
	 */
	private void addCandidateEntry(Map<String, Object> candidates, String candidateName,
								   DependencyDescriptor descriptor, Class<?> requiredType) {

		// 如果描述符是 MultiElementDescriptor 类型
		if (descriptor instanceof MultiElementDescriptor) {
			// 解析候选对象并添加到候选对象集合中
			Object beanInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
			if (!(beanInstance instanceof NullBean)) {
				candidates.put(candidateName, beanInstance);
			}
		} else if (containsSingleton(candidateName) || (descriptor instanceof StreamDependencyDescriptor &&
				((StreamDependencyDescriptor) descriptor).isOrdered())) {
			// 如果候选对象是单例对象或者描述符是 StreamDependencyDescriptor 类型且是有序的
			// 解析候选对象并添加到候选对象集合中
			Object beanInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
			candidates.put(candidateName, (beanInstance instanceof NullBean ? null : beanInstance));
		} else {
			// 否则，将候选对象的类型添加到候选对象集合中
			candidates.put(candidateName, getType(candidateName));
		}
	}

	/**
	 * 在给定的一组bean中确定自动装配候选项。
	 * <p>查找{@code @Primary}和{@code @Priority}（按顺序）。
	 *
	 * @param candidates 匹配所需类型的候选名称和候选实例的Map，由{@link #findAutowireCandidates}返回
	 * @param descriptor 要匹配的目标依赖项
	 * @return 自动装配候选项的名称；如果未找到，则为{@code null}
	 */
	@Nullable
	protected String determineAutowireCandidate(Map<String, Object> candidates, DependencyDescriptor descriptor) {
		// 获取依赖项的类型
		Class<?> requiredType = descriptor.getDependencyType();
		// 确定首选候选项
		String primaryCandidate = determinePrimaryCandidate(candidates, requiredType);
		if (primaryCandidate != null) {
			return primaryCandidate;
		}

		// 确定优先级最高的候选项
		String priorityCandidate = determineHighestPriorityCandidate(candidates, requiredType);
		if (priorityCandidate != null) {
			return priorityCandidate;
		}

		// 如果没有首选项和优先级最高项，则进行回退匹配
		// 遍历所有候选项，查找与已解析的依赖项相等的候选项或与依赖项名称匹配的候选项
		for (Map.Entry<String, Object> entry : candidates.entrySet()) {
			String candidateName = entry.getKey();
			Object beanInstance = entry.getValue();
			if ((beanInstance != null && this.resolvableDependencies.containsValue(beanInstance)) ||
					matchesBeanName(candidateName, descriptor.getDependencyName())) {
				return candidateName;
			}
		}

		// 如果都没有匹配的候选项，则返回null
		return null;
	}

	/**
	 * 在给定的一组 bean 中确定主要候选对象。
	 *
	 * @param candidates   匹配所需类型的候选对象名称和候选对象实例
	 *                     （如果尚未创建则是候选对象类）的映射
	 * @param requiredType 要匹配的目标依赖类型
	 * @return 主要候选对象的名称；如果找不到，则为 {@code null}
	 * @see #isPrimary(String, Object)
	 */
	@Nullable
	protected String determinePrimaryCandidate(Map<String, Object> candidates, Class<?> requiredType) {
		// 初始化主要候选对象名称为null
		String primaryBeanName = null;
		// 遍历候选对象集合
		for (Map.Entry<String, Object> entry : candidates.entrySet()) {
			// 获取候选对象的名称和实例
			String candidateBeanName = entry.getKey();
			Object beanInstance = entry.getValue();
			// 如果该候选对象是主要候选对象
			if (isPrimary(candidateBeanName, beanInstance)) {
				// 如果已经存在主要候选对象，则抛出异常
				if (primaryBeanName != null) {
					// 检查候选对象和已存在的主要候选对象是否都是本地定义的
					boolean candidateLocal = containsBeanDefinition(candidateBeanName);
					boolean primaryLocal = containsBeanDefinition(primaryBeanName);
					if (candidateLocal && primaryLocal) {
						throw new NoUniqueBeanDefinitionException(requiredType, candidates.size(),
								"more than one 'primary' bean found among candidates: " + candidates.keySet());
					} else if (candidateLocal) {
						// 如果候选对象是本地定义的，则将候选对象设置为主要候选对象
						primaryBeanName = candidateBeanName;
					}
				} else {
					// 如果尚未存在主要候选对象，则将候选对象设置为主要候选对象
					primaryBeanName = candidateBeanName;
				}
			}
		}
		// 返回主要候选对象名称
		return primaryBeanName;
	}

	/**
	 * 在给定的一组 bean 中确定具有最高优先级的候选对象。
	 * <p>基于 {@code @javax.annotation.Priority}。如 {@link org.springframework.core.Ordered} 接口所定义，
	 * 最低值具有最高优先级。
	 *
	 * @param candidates   匹配所需类型的候选对象名称和候选对象实例
	 *                     （如果尚未创建则是候选对象类）的映射
	 * @param requiredType 要匹配的目标依赖类型
	 * @return 具有最高优先级的候选对象的名称，如果找不到，则为 {@code null}
	 * @see #getPriority(Object)
	 */
	@Nullable
	protected String determineHighestPriorityCandidate(Map<String, Object> candidates, Class<?> requiredType) {
		// 初始化最高优先级候选对象名称和优先级为null
		String highestPriorityBeanName = null;
		Integer highestPriority = null;
		// 遍历候选对象集合
		for (Map.Entry<String, Object> entry : candidates.entrySet()) {
			// 获取候选对象的名称和实例
			String candidateBeanName = entry.getKey();
			Object beanInstance = entry.getValue();
			// 如果候选对象不为null
			if (beanInstance != null) {
				// 获取候选对象的优先级
				Integer candidatePriority = getPriority(beanInstance);
				// 如果候选对象的优先级不为null
				if (candidatePriority != null) {
					// 如果已经存在最高优先级候选对象
					if (highestPriorityBeanName != null) {
						// 如果候选对象的优先级与已存在的最高优先级相同，则抛出异常
						if (candidatePriority.equals(highestPriority)) {
							throw new NoUniqueBeanDefinitionException(requiredType, candidates.size(),
									"Multiple beans found with the same priority ('" + highestPriority +
											"') among candidates: " + candidates.keySet());
						} else if (candidatePriority < highestPriority) {
							// 如果候选对象的优先级比已存在的最高优先级更高，则更新最高优先级候选对象和优先级
							highestPriorityBeanName = candidateBeanName;
							highestPriority = candidatePriority;
						}
					} else {
						// 如果尚未存在最高优先级候选对象，则将当前候选对象设置为最高优先级候选对象
						highestPriorityBeanName = candidateBeanName;
						highestPriority = candidatePriority;
					}
				}
			}
		}
		// 返回最高优先级候选对象名称
		return highestPriorityBeanName;
	}

	/**
	 * 返回给定 bean 名称的 bean 定义是否已标记为主要 bean。
	 *
	 * @param beanName     bean 的名称
	 * @param beanInstance 相应的 bean 实例（可以为 null）
	 * @return 给定 bean 是否符合主要条件
	 */
	protected boolean isPrimary(String beanName, Object beanInstance) {
		// 转换bean名称
		String transformedBeanName = transformedBeanName(beanName);
		// 检查转换后的bean名称是否存在于本地bean定义中
		if (containsBeanDefinition(transformedBeanName)) {
			// 如果存在，则返回该bean是否为主要候选对象
			return getMergedLocalBeanDefinition(transformedBeanName).isPrimary();
		}
		// 获取父级Bean工厂
		BeanFactory parent = getParentBeanFactory();
		// 如果父级Bean工厂是DefaultListableBeanFactory类型，则委托父级Bean工厂判断bean是否为主要候选对象
		return (parent instanceof DefaultListableBeanFactory &&
				((DefaultListableBeanFactory) parent).isPrimary(transformedBeanName, beanInstance));
	}

	/**
	 * 返回通过 {@code javax.annotation.Priority} 注解为给定的 bean 实例分配的优先级。
	 * <p>默认实现委托给指定的 {@link #setDependencyComparator 依赖比较器}，
	 * 如果它是 Spring 的通用 {@link OrderComparator} 的扩展（通常是 {@link org.springframework.core.annotation.AnnotationAwareOrderComparator}），
	 * 则检查其 {@link OrderComparator#getPriority 方法}。如果不存在这样的比较器，则此实现返回 {@code null}。
	 *
	 * @param beanInstance 要检查的 bean 实例（可以为 {@code null}）
	 * @return 为该 bean 分配的优先级，如果未设置则返回 {@code null}
	 */
	@Nullable
	protected Integer getPriority(Object beanInstance) {
		// 获取依赖项比较器
		Comparator<Object> comparator = getDependencyComparator();
		if (comparator instanceof OrderComparator) {
			// 如果比较器是OrderComparator类型，则获取bean实例的优先级
			return ((OrderComparator) comparator).getPriority(beanInstance);
		}
		// 否则返回空
		return null;
	}

	/**
	 * 确定给定的候选名称是否与此 bean 定义中存储的 bean 名称或别名匹配。
	 *
	 * @param beanName      bean 名称
	 * @param candidateName 候选名称
	 * @return 如果候选名称与 bean 名称或别名匹配，则为 {@code true}
	 */
	protected boolean matchesBeanName(String beanName, @Nullable String candidateName) {
		return (candidateName != null &&
				(candidateName.equals(beanName) || ObjectUtils.containsElement(getAliases(beanName), candidateName)));
	}

	/**
	 * 确定给定的 beanName/candidateName 对是否表示自引用，
	 * 即候选项是否指向原始 bean 或原始 bean 上的工厂方法。
	 *
	 * @param beanName      bean 名称
	 * @param candidateName 候选名称
	 * @return 如果候选项指向原始 bean 或原始 bean 上的工厂方法，则为 {@code true}
	 */
	private boolean isSelfReference(@Nullable String beanName, @Nullable String candidateName) {
		return (beanName != null && candidateName != null &&
				(beanName.equals(candidateName) || (containsBeanDefinition(candidateName) &&
						beanName.equals(getMergedLocalBeanDefinition(candidateName).getFactoryBeanName()))));
	}

	/**
	 * 对于无法解析的依赖项引发NoSuchBeanDefinitionException或BeanNotOfRequiredTypeException。
	 */
	private void raiseNoMatchingBeanFound(
			Class<?> type, ResolvableType resolvableType, DependencyDescriptor descriptor) throws BeansException {

		checkBeanNotOfRequiredType(type, descriptor);

		throw new NoSuchBeanDefinitionException(resolvableType,
				"expected at least 1 bean which qualifies as autowire candidate. " +
						"Dependency annotations: " + ObjectUtils.nullSafeToString(descriptor.getAnnotations()));
	}

	/**
	 * 如果适用，则为无法解析的依赖项引发 BeanNotOfRequiredTypeException，即如果 bean 的目标类型将匹配但公开的代理不匹配。
	 *
	 * @param type        目标类型
	 * @param descriptor  依赖项描述符
	 */
	private void checkBeanNotOfRequiredType(Class<?> type, DependencyDescriptor descriptor) {
		for (String beanName : this.beanDefinitionNames) {
			try {
				// 获取合并后的本地Bean定义
				RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
				// 获取目标类型
				Class<?> targetType = mbd.getTargetType();
				// 如果目标类型不为空，并且类型是可分配给目标类型的，并且是自动装配候选的
				if (targetType != null && type.isAssignableFrom(targetType) &&
						isAutowireCandidate(beanName, mbd, descriptor, getAutowireCandidateResolver())) {
					// 可能是代理干扰目标类型匹配 -> 抛出有意义的异常。
					Object beanInstance = getSingleton(beanName, false);
					// 获取bean的实际类型
					Class<?> beanType = (beanInstance != null && beanInstance.getClass() != NullBean.class ?
							beanInstance.getClass() : predictBeanType(beanName, mbd));
					// 如果bean类型不为空并且不是目标类型的子类，则抛出异常
					if (beanType != null && !type.isAssignableFrom(beanType)) {
						throw new BeanNotOfRequiredTypeException(beanName, type, beanType);
					}
				}
			} catch (NoSuchBeanDefinitionException ex) {
				// 当我们迭代时，Bean定义被删除了-> 忽略。
			}
		}

		// 获取父级Bean工厂
		BeanFactory parent = getParentBeanFactory();
		// 如果父级Bean工厂是DefaultListableBeanFactory类型，则继续检查
		if (parent instanceof DefaultListableBeanFactory) {
			((DefaultListableBeanFactory) parent).checkBeanNotOfRequiredType(type, descriptor);
		}
	}

	/**
	 * 为指定的依赖项创建一个 Optional 包装器。
	 *
	 * @param descriptor 依赖项描述符
	 * @param beanName   bean 的名称（可为 null）
	 * @param args       参数数组
	 * @return 包装后的 Optional 实例
	 */
	private Optional<?> createOptionalDependency(
			DependencyDescriptor descriptor, @Nullable String beanName, final Object... args) {

		// 创建一个自定义的DependencyDescriptor，继承自NestedDependencyDescriptor
		DependencyDescriptor descriptorToUse = new NestedDependencyDescriptor(descriptor) {
			@Override
			public boolean isRequired() {
				return false;
			}

			@Override
			public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory) {
				// 如果args不为空，则使用args来获取bean
				return (!ObjectUtils.isEmpty(args) ? beanFactory.getBean(beanName, args) :
						super.resolveCandidate(beanName, requiredType, beanFactory));
			}
		};
		// 解析依赖项
		Object result = doResolveDependency(descriptorToUse, beanName, null, null);
		// 将解析结果转换为Optional类型
		return (result instanceof Optional ? (Optional<?>) result : Optional.ofNullable(result));
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(ObjectUtils.identityToString(this));
		sb.append(": defining beans [");
		sb.append(StringUtils.collectionToCommaDelimitedString(this.beanDefinitionNames));
		sb.append("]; ");
		BeanFactory parent = getParentBeanFactory();
		if (parent == null) {
			sb.append("root of factory hierarchy");
		} else {
			sb.append("parent: ").append(ObjectUtils.identityToString(parent));
		}
		return sb.toString();
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		throw new NotSerializableException("DefaultListableBeanFactory itself is not deserializable - " +
				"just a SerializedBeanFactoryReference is");
	}

	protected Object writeReplace() throws ObjectStreamException {
		if (this.serializationId != null) {
			return new SerializedBeanFactoryReference(this.serializationId);
		} else {
			throw new NotSerializableException("DefaultListableBeanFactory has no serialization id");
		}
	}


	/**
	 * 对工厂的最小 id 引用。
	 * 在反序列化时，将解析为实际的工厂实例。
	 */
	private static class SerializedBeanFactoryReference implements Serializable {

		private final String id;

		public SerializedBeanFactoryReference(String id) {
			this.id = id;
		}

		private Object readResolve() {
			// 从serializableFactories中获取对应id的Reference
			Reference<?> ref = serializableFactories.get(this.id);
			if (ref != null) {
				// 如果ref不为null，则获取其中的对象
				Object result = ref.get();
				if (result != null) {
					// 如果result不为null，则返回结果
					return result;
				}
			}
			// 如果serializableFactories中未找到对应的对象，则创建一个默认的Bean工厂作为fallback
			DefaultListableBeanFactory dummyFactory = new DefaultListableBeanFactory();
			dummyFactory.serializationId = this.id;
			return dummyFactory;
		}
	}


	/**
	 * 用于嵌套元素的依赖描述符标记。
	 */
	private static class NestedDependencyDescriptor extends DependencyDescriptor {

		public NestedDependencyDescriptor(DependencyDescriptor original) {
			super(original);
			increaseNestingLevel();
		}
	}


	/**
	 * 用于具有嵌套元素的多元素声明的依赖描述符。
	 */
	private static class MultiElementDescriptor extends NestedDependencyDescriptor {

		public MultiElementDescriptor(DependencyDescriptor original) {
			super(original);
		}
	}


	/**
	 * 用于流访问多个元素的依赖描述符标记。
	 */
	private static class StreamDependencyDescriptor extends DependencyDescriptor {

		private final boolean ordered;

		public StreamDependencyDescriptor(DependencyDescriptor original, boolean ordered) {
			super(original);
			this.ordered = ordered;
		}

		/**
		 * 检查流访问是否有序。
		 *
		 * @return 如果流访问有序，则为true；否则为false。
		 */
		public boolean isOrdered() {
			return this.ordered;
		}
	}


	private interface BeanObjectProvider<T> extends ObjectProvider<T>, Serializable {
	}


	/**
	 * 用于延迟解析依赖关系的可序列化 ObjectFactory/ObjectProvider。
	 */
	private class DependencyObjectProvider implements BeanObjectProvider<Object>, Serializable {
		/**
		 * 依赖描述
		 */
		private final DependencyDescriptor descriptor;
		/**
		 * 是否是可选的
		 */
		private final boolean optional;
		/**
		 * bean名称
		 */
		@Nullable
		private final String beanName;

		public DependencyObjectProvider(DependencyDescriptor descriptor, @Nullable String beanName) {
			this.descriptor = new NestedDependencyDescriptor(descriptor);
			this.optional = (this.descriptor.getDependencyType() == Optional.class);
			this.beanName = beanName;
		}

		@Override
		public Object getObject() throws BeansException {
			if (this.optional) {
				// 如果依赖是可选的，则创建一个Optional对象包装解析的依赖
				return createOptionalDependency(this.descriptor, this.beanName);
			} else {
				// 如果依赖不是可选的，则尝试解析依赖
				Object result = doResolveDependency(this.descriptor, this.beanName, null, null);
				// 如果解析的结果为空，则抛出NoSuchBeanDefinitionException异常
				if (result == null) {
					throw new NoSuchBeanDefinitionException(this.descriptor.getResolvableType());
				}
				// 否则返回解析的结果
				return result;
			}
		}

		@Override
		public Object getObject(final Object... args) throws BeansException {
			if (this.optional) {
				// 如果依赖是可选的，则创建一个Optional对象包装解析的依赖
				return createOptionalDependency(this.descriptor, this.beanName, args);
			} else {
				// 如果依赖不是可选的，则创建一个新的DependencyDescriptor，并覆盖resolveCandidate方法
				DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
					@Override
					public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory) {
						// 解析依赖时传递了参数args
						return beanFactory.getBean(beanName, args);
					}
				};
				// 尝试解析依赖
				Object result = doResolveDependency(descriptorToUse, this.beanName, null, null);
				// 如果解析的结果为空，则抛出NoSuchBeanDefinitionException异常
				if (result == null) {
					throw new NoSuchBeanDefinitionException(this.descriptor.getResolvableType());
				}
				// 否则返回解析的结果
				return result;
			}
		}

		@Override
		@Nullable
		public Object getIfAvailable() throws BeansException {
			try {
				if (this.optional) {
					// 如果依赖是可选的，则创建一个Optional对象包装解析的依赖
					return createOptionalDependency(this.descriptor, this.beanName);
				} else {
					// 如果依赖不是可选的，则创建一个新的DependencyDescriptor，并覆盖isRequired方法
					DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
						@Override
						public boolean isRequired() {
							// 将isRequired方法重写为返回false，表示依赖不是必需的
							return false;
						}
					};
					// 尝试解析依赖
					return doResolveDependency(descriptorToUse, this.beanName, null, null);
				}
			} catch (ScopeNotActiveException ex) {
				// 如果ScopeNotActiveException异常被捕获，则忽略在非活动范围中解析的bean
				return null;
			}
		}

		@Override
		public void ifAvailable(Consumer<Object> dependencyConsumer) throws BeansException {
			Object dependency = getIfAvailable();
			if (dependency != null) {
				try {
					// 尝试接受依赖对象并执行操作
					dependencyConsumer.accept(dependency);
				} catch (ScopeNotActiveException ex) {
					// 如果ScopeNotActiveException异常被捕获，则忽略在非活动范围中解析的bean，即使在作用域代理调用中也是如此
				}
			}
		}

		@Override
		@Nullable
		public Object getIfUnique() throws BeansException {
			// 创建一个新的依赖描述
			DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
				@Override
				public boolean isRequired() {
					return false;
				}

				@Override
				@Nullable
				public Object resolveNotUnique(ResolvableType type, Map<String, Object> matchingBeans) {
					return null;
				}
			};
			try {
				if (this.optional) {
					// 创建可选的依赖项
					return createOptionalDependency(descriptorToUse, this.beanName);
				} else {
					// 解析依赖项
					return doResolveDependency(descriptorToUse, this.beanName, null, null);
				}
			} catch (ScopeNotActiveException ex) {
				// 如果ScopeNotActiveException异常被捕获，则忽略在非活动范围中解析的bean
				return null;
			}
		}

		@Override
		public void ifUnique(Consumer<Object> dependencyConsumer) throws BeansException {
			Object dependency = getIfUnique();
			if (dependency != null) {
				try {
					// 尝试接受依赖项
					dependencyConsumer.accept(dependency);
				} catch (ScopeNotActiveException ex) {
					// 如果ScopeNotActiveException异常被捕获，则忽略在非活动范围中解析的bean，即使是在作用域代理调用时也是如此
				}
			}
		}

		@Nullable
		protected Object getValue() throws BeansException {
			if (this.optional) {
				// 如果是可选依赖，则创建Optional依赖项
				return createOptionalDependency(this.descriptor, this.beanName);
			} else {
				// 否则，解析依赖项
				return doResolveDependency(this.descriptor, this.beanName, null, null);
			}
		}

		@Override
		public Stream<Object> stream() {
			return resolveStream(false);
		}

		@Override
		public Stream<Object> orderedStream() {
			return resolveStream(true);
		}

		@SuppressWarnings("unchecked")
		private Stream<Object> resolveStream(boolean ordered) {
			// 创建流依赖项描述符
			DependencyDescriptor descriptorToUse = new StreamDependencyDescriptor(this.descriptor, ordered);
			// 解析依赖项
			Object result = doResolveDependency(descriptorToUse, this.beanName, null, null);
			// 如果结果是流，则直接返回结果，否则将结果包装成单元素流返回
			return (result instanceof Stream ? (Stream<Object>) result : Stream.of(result));
		}
	}


	/**
	 * 为了避免对 javax.inject API 的硬依赖而单独创建的内部类。
	 * 实际的 javax.inject.Provider 实现被嵌套在这里，以使其对 DefaultListableBeanFactory 的嵌套类的 Graal 反射不可见。
	 */
	private class Jsr330Factory implements Serializable {

		public Object createDependencyProvider(DependencyDescriptor descriptor, @Nullable String beanName) {
			return new Jsr330Provider(descriptor, beanName);
		}

		private class Jsr330Provider extends DependencyObjectProvider implements Provider<Object> {

			public Jsr330Provider(DependencyDescriptor descriptor, @Nullable String beanName) {
				super(descriptor, beanName);
			}

			@Override
			@Nullable
			public Object get() throws BeansException {
				return getValue();
			}
		}
	}


	/**
	 * 一个实现 org.springframework.core.OrderComparator.OrderSourceProvider 接口的类，
	 * 它能够识别要排序的实例的 bean 元数据。
	 * <p>查找要排序的实例的方法工厂（如果有），并让比较器检索其上定义的 org.springframework.core.annotation.Order 值。
	 * 这基本上允许以下结构：
	 */
	private class FactoryAwareOrderSourceProvider implements OrderComparator.OrderSourceProvider {

		/**
		 * 实例和bean名称映射
		 */
		private final Map<Object, String> instancesToBeanNames;

		public FactoryAwareOrderSourceProvider(Map<Object, String> instancesToBeanNames) {
			this.instancesToBeanNames = instancesToBeanNames;
		}

		@Override
		@Nullable
		public Object getOrderSource(Object obj) {
			// 获取给定实例所对应的 Bean 名称
			String beanName = this.instancesToBeanNames.get(obj);
			// 如果 Bean 名称为空或者不存在于 Bean 定义中，则返回 null
			if (beanName == null || !containsBeanDefinition(beanName)) {
				return null;
			}
			// 获取合并后的本地 Bean 定义
			RootBeanDefinition beanDefinition = getMergedLocalBeanDefinition(beanName);
			// 创建源列表
			List<Object> sources = new ArrayList<>(2);
			// 获取解析后的工厂方法
			Method factoryMethod = beanDefinition.getResolvedFactoryMethod();
			// 如果工厂方法不为空，则加入源列表
			if (factoryMethod != null) {
				sources.add(factoryMethod);
			}
			// 获取目标类型
			Class<?> targetType = beanDefinition.getTargetType();
			// 如果目标类型不为空且与给定实例的类型不同，则加入源列表
			if (targetType != null && targetType != obj.getClass()) {
				sources.add(targetType);
			}
			// 将源列表转换为数组并返回
			return sources.toArray();
		}
	}

}
