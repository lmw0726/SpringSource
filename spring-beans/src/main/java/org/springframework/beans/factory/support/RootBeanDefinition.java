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

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.lang.reflect.*;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A root bean definition represents the merged bean definition that backs
 * a specific bean in a Spring BeanFactory at runtime. It might have been created
 * from multiple original bean definitions that inherit from each other,
 * typically registered as {@link GenericBeanDefinition GenericBeanDefinitions}.
 * A root bean definition is essentially the 'unified' bean definition view at runtime.
 *
 * <p>Root bean definitions may also be used for registering individual bean definitions
 * in the configuration phase. However, since Spring 2.5, the preferred way to register
 * bean definitions programmatically is the {@link GenericBeanDefinition} class.
 * GenericBeanDefinition has the advantage that it allows to dynamically define
 * parent dependencies, not 'hard-coding' the role as a root bean definition.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see GenericBeanDefinition
 * @see ChildBeanDefinition
 */
@SuppressWarnings("serial")
public class RootBeanDefinition extends AbstractBeanDefinition {

	/**
	 * 装饰bean定义持有者，存储 Bean 的名称、别名、BeanDefinition
	 */
	@Nullable
	private BeanDefinitionHolder decoratedDefinition;

	/**
	 * 注解元素，AnnotatedElement 是java反射包的接口，通过它可以查看 Bean 的注解信息
	 */
	@Nullable
	private AnnotatedElement qualifiedElement;

	/**
	 * 确定定义是否需要重新合并。
	 */
	volatile boolean stale;

	/**
	 * 是否允许缓存
	 */
	boolean allowCaching = true;

	/**
	 * 工厂方法是否唯一
	 */
	boolean isFactoryMethodUnique;

	/**
	 * 目标类型，封装了 java.lang.reflect.Type，提供了泛型相关的操作
	 */
	@Nullable
	volatile ResolvableType targetType;

	/**
	 * 用于缓存给定bean定义的确定类的包可见字段。
	 * 缓存 Class，表示 RootBeanDefinition 存储哪个类的信息
	 */
	@Nullable
	volatile Class<?> resolvedTargetType;

	/**
	 * 如果bean是工厂bean，则用于包可见字段的缓存。
	 */
	@Nullable
	volatile Boolean isFactoryBean;

	/**
	 * 封装可见字段，用于缓存通用类型工厂方法的返回类型。
	 * 用于缓存工厂方法的返回类型
	 */
	@Nullable
	volatile ResolvableType factoryMethodReturnType;

	/**
	 * 用于缓存用于内省的唯一工厂方法候选的包可见字段。
	 */
	@Nullable
	volatile Method factoryMethodToIntrospect;

	/**
	 * 用于缓存已解析的destroy方法名称的包可见字段 (也用于推断)。
	 */
	@Nullable
	volatile String resolvedDestroyMethodName;

	/**
	 * 下面四个构造函数字段的公共锁。
	 */
	final Object constructorArgumentLock = new Object();

	/**
	 * 用于缓存解析的构造函数或工厂方法的包可见字段。
	 */
	@Nullable
	Executable resolvedConstructorOrFactoryMethod;

	/**
	 * 包可见字段，将构造函数参数标记为已解析好。
	 */
	boolean constructorArgumentsResolved = false;

	/**
	 * 用于缓存完全解析的构造函数参数的包可见字段。
	 */
	@Nullable
	Object[] resolvedConstructorArguments;

	/**
	 * 用于缓存部分准备的构造函数参数的包可见字段。
	 */
	@Nullable
	Object[] preparedConstructorArguments;

	/**
	 * 下面两个后处理字段的公共锁。
	 */
	final Object postProcessingLock = new Object();

	/**
	 * 包可见字段，指示MergedBeanDefinitionPostProcessor已应用。
	 */
	boolean postProcessed = false;

	/**
	 * 包可见字段，指示实例化后处理器是否已启动。
	 */
	@Nullable
	volatile Boolean beforeInstantiationResolved;

	/**
	 * 外部托管配置成员
	 * 实际缓存的类型是 Constructor、Field、Method 类型
	 */
	@Nullable
	private Set<Member> externallyManagedConfigMembers;

	/**
	 * 外部管理的Init方法
	 * InitializingBean中 的 init 回调函数名 afterPropertiesSet 会在这里记录，以便进行生命周期回调
	 */
	@Nullable
	private Set<String> externallyManagedInitMethods;

	/**
	 * 外部管理销毁方法
	 * DisposableBean 的 销毁方法 回调函数名 销毁方法 会在这里记录，以便进生命周期回调
	 */
	@Nullable
	private Set<String> externallyManagedDestroyMethods;


	/**
	 * 创建一个新的RootBeanDefinition，要通过其bean属性和配置方法进行配置。
	 *
	 * @see #setBeanClass
	 * @see #setScope
	 * @see #setConstructorArgumentValues
	 * @see #setPropertyValues
	 */
	public RootBeanDefinition() {
		super();
	}

	/**
	 * 为单例创建新的根bean定义。
	 *
	 * @param beanClass 要实例化的bean的类
	 * @see #setBeanClass
	 */
	public RootBeanDefinition(@Nullable Class<?> beanClass) {
		super();
		setBeanClass(beanClass);
	}

	/**
	 * 为单例bean创建一个新的RootBeanDefinition，
	 * 通过调用给定的实例供应者 (可能是lambda或方法引用) 来构造每个实例。
	 *
	 * @param beanClass        要实例化的bean的类
	 * @param instanceSupplier 供应商构造bean实例，作为声明指定的工厂方法的替代
	 * @see #setInstanceSupplier
	 * @since 5.0
	 */
	public <T> RootBeanDefinition(@Nullable Class<T> beanClass, @Nullable Supplier<T> instanceSupplier) {
		super();
		setBeanClass(beanClass);
		setInstanceSupplier(instanceSupplier);
	}

	/**
	 * 为作用域bean创建一个新的RootBeanDefinition，
	 * 通过调用给定的实例供应者 (可能是lambda或方法引用) 来构造每个实例。
	 *
	 * @param beanClass        要实例化的bean的类
	 * @param scope            对应范围的名称
	 * @param instanceSupplier 供应商构造bean实例，作为声明指定的工厂方法的替代
	 * @see #setInstanceSupplier
	 * @since 5.0
	 */
	public <T> RootBeanDefinition(@Nullable Class<T> beanClass, String scope, @Nullable Supplier<T> instanceSupplier) {
		super();
		setBeanClass(beanClass);
		setScope(scope);
		setInstanceSupplier(instanceSupplier);
	}

	/**
	 * 使用给定的自动装配模式为单例创建新的RootBeanDefinition。
	 *
	 * @param beanClass       要实例化的bean的类
	 * @param autowireMode    按名称或类型，使用此接口中的常量
	 * @param dependencyCheck 是否对对象执行依赖检查 (不适用于自动生成构造函数，因此在那里忽略)
	 */
	public RootBeanDefinition(@Nullable Class<?> beanClass, int autowireMode, boolean dependencyCheck) {
		super();
		setBeanClass(beanClass);
		setAutowireMode(autowireMode);
		if (dependencyCheck && getResolvedAutowireMode() != AUTOWIRE_CONSTRUCTOR) {
			//如果需要检查依赖，并且最后的自动装配模式不是构造函数的话
			//仅检查依赖对象，不检查其属性。
			setDependencyCheck(DEPENDENCY_CHECK_OBJECTS);
		}
	}

	/**
	 * 为单例创建一个新的RootBeanDefinition，提供构造函数参数和属性值。
	 *
	 * @param beanClass 要实例化的bean的类
	 * @param cargs     要应用的构造函数参数值
	 * @param pvs       要应用的属性值
	 */
	public RootBeanDefinition(@Nullable Class<?> beanClass, @Nullable ConstructorArgumentValues cargs,
							  @Nullable MutablePropertyValues pvs) {

		super(cargs, pvs);
		setBeanClass(beanClass);
	}

	/**
	 * 为单例创建一个新的RootBeanDefinition，提供构造函数参数和属性值。
	 * <p> 采用bean类名称，以避免早期加载bean类。
	 *
	 * @param beanClassName 要实例化的类的名称
	 */
	public RootBeanDefinition(String beanClassName) {
		setBeanClassName(beanClassName);
	}

	/**
	 * 为单例创建一个新的RootBeanDefinition，提供构造函数参数和属性值。
	 * <p> 采用bean类名称，以避免早期加载bean类。
	 *
	 * @param beanClassName 要实例化的类的名称
	 * @param cargs         要应用的构造函数参数值
	 * @param pvs           要应用的属性值
	 */
	public RootBeanDefinition(String beanClassName, ConstructorArgumentValues cargs, MutablePropertyValues pvs) {
		super(cargs, pvs);
		setBeanClassName(beanClassName);
	}

	/**
	 * 创建一个新的RootBeanDefinition作为给定bean定义的深度副本。
	 *
	 * @param original 要复制的原始bean定义
	 */
	public RootBeanDefinition(RootBeanDefinition original) {
		super(original);
		//装饰bean定义
		this.decoratedDefinition = original.decoratedDefinition;
		//注解元素
		this.qualifiedElement = original.qualifiedElement;
		//是否允许缓存
		this.allowCaching = original.allowCaching;
		//工厂方法是否唯一
		this.isFactoryMethodUnique = original.isFactoryMethodUnique;
		//目标类型
		this.targetType = original.targetType;
		//工厂方法实例
		this.factoryMethodToIntrospect = original.factoryMethodToIntrospect;
	}

	/**
	 * 创建一个新的RootBeanDefinition作为给定bean定义的深度副本。
	 *
	 * @param original 要复制的原始bean定义
	 */
	RootBeanDefinition(BeanDefinition original) {
		super(original);
	}


	@Override
	public String getParentName() {
		return null;
	}

	@Override
	public void setParentName(@Nullable String parentName) {
		if (parentName != null) {
			throw new IllegalArgumentException("Root bean cannot be changed into a child bean with parent reference");
		}
	}

	/**
	 * 注册由此bean定义修饰的目标定义。
	 */
	public void setDecoratedDefinition(@Nullable BeanDefinitionHolder decoratedDefinition) {
		this.decoratedDefinition = decoratedDefinition;
	}

	/**
	 * 返回由此bean定义修饰的目标定义 (如果有)。
	 */
	@Nullable
	public BeanDefinitionHolder getDecoratedDefinition() {
		return this.decoratedDefinition;
	}

	/**
	 * 指定定义限定符的 {@link AnnotatedElement}，以代替目标类或工厂方法。
	 *
	 * @see #setTargetType(ResolvableType)
	 * @see #getResolvedFactoryMethod()
	 * @since 4.3.3
	 */
	public void setQualifiedElement(@Nullable AnnotatedElement qualifiedElement) {
		this.qualifiedElement = qualifiedElement;
	}

	/**
	 * 返回定义限定符 (如果有) 的 {@link AnnotatedElement}。否则，将检查工厂方法和目标类。
	 *
	 * @since 4.3.3
	 */
	@Nullable
	public AnnotatedElement getQualifiedElement() {
		return this.qualifiedElement;
	}

	/**
	 * 如果事先知道，请指定此bean定义的包含泛型的目标类型。
	 *
	 * @since 4.3.3
	 */
	public void setTargetType(ResolvableType targetType) {
		this.targetType = targetType;
	}

	/**
	 * 指定此bean定义的目标类型 (如果事先知道)。
	 *
	 * @since 3.2.2
	 */
	public void setTargetType(@Nullable Class<?> targetType) {
		this.targetType = (targetType != null ? ResolvableType.forClass(targetType) : null);
	}

	/**
	 * 如果已知，则返回此bean定义的目标类型 (预先指定或在第一次实例化时解析)。
	 *
	 * @since 3.2.2
	 */
	@Nullable
	public Class<?> getTargetType() {
		if (this.resolvedTargetType != null) {
			return this.resolvedTargetType;
		}
		ResolvableType targetType = this.targetType;
		return (targetType != null ? targetType.resolve() : null);
	}

	/**
	 * 从运行时缓存的类型信息或从配置时 {@link #setTargetType(ResolvableType)} 或 {@link #setBeanClass(Class)}
	 * 返回此bean定义的 {@link ResolvableType}，也考虑已解析的工厂方法定义。
	 *
	 * @see #setTargetType(ResolvableType)
	 * @see #setBeanClass(Class)
	 * @see #setResolvedFactoryMethod(Method)
	 * @since 5.1
	 */
	@Override
	public ResolvableType getResolvableType() {
		ResolvableType targetType = this.targetType;
		if (targetType != null) {
			return targetType;
		}
		ResolvableType returnType = this.factoryMethodReturnType;
		if (returnType != null) {
			return returnType;
		}
		Method factoryMethod = this.factoryMethodToIntrospect;
		if (factoryMethod != null) {
			return ResolvableType.forMethodReturnType(factoryMethod);
		}
		return super.getResolvableType();
	}

	/**
	 * 确定用于默认构造的首选构造函数 (如果有)。如有必要，构造函数参数将是自动的。
	 *
	 * @return 一个或多个首选的构造函数，或者 {@code null}，
	 * 如果没有 (在这种情况下，常规的无参默认构造函数将被调用)
	 * @since 5.1
	 */
	@Nullable
	public Constructor<?>[] getPreferredConstructors() {
		return null;
	}

	/**
	 * 指定引用非重载方法的工厂方法名称。
	 */
	public void setUniqueFactoryMethodName(String name) {
		Assert.hasText(name, "Factory method name must not be empty");
		setFactoryMethodName(name);
		this.isFactoryMethodUnique = true;
	}

	/**
	 * 指定引用重载方法的工厂方法名称。
	 *
	 * @since 5.2
	 */
	public void setNonUniqueFactoryMethodName(String name) {
		Assert.hasText(name, "Factory method name must not be empty");
		setFactoryMethodName(name);
		this.isFactoryMethodUnique = false;
	}

	/**
	 * 检查给定的候选人是否符合工厂方法的条件。
	 */
	public boolean isFactoryMethod(Method candidate) {
		return candidate.getName().equals(getFactoryMethodName());
	}

	/**
	 * 在此bean定义上设置工厂方法的已解析Java方法。
	 *
	 * @param method 已解析的工厂方法，或将它重置为 {@code null}
	 * @since 5.2
	 */
	public void setResolvedFactoryMethod(@Nullable Method method) {
		this.factoryMethodToIntrospect = method;
	}

	/**
	 * 如果可用，则将已解析的工厂方法作为Java方法对象返回。
	 *
	 * @return 工厂方法，如果找不到或尚未解决，则为 {@code null}
	 */
	@Nullable
	public Method getResolvedFactoryMethod() {
		return this.factoryMethodToIntrospect;
	}

	/**
	 * 注册外部托管的配置方法或字段。
	 */
	public void registerExternallyManagedConfigMember(Member configMember) {
		synchronized (this.postProcessingLock) {
			if (this.externallyManagedConfigMembers == null) {
				this.externallyManagedConfigMembers = new LinkedHashSet<>(1);
			}
			this.externallyManagedConfigMembers.add(configMember);
		}
	}

	/**
	 * 确定给定的方法或字段是否是外部托管的配置成员。
	 */
	public boolean isExternallyManagedConfigMember(Member configMember) {
		synchronized (this.postProcessingLock) {
			return (this.externallyManagedConfigMembers != null &&
					this.externallyManagedConfigMembers.contains(configMember));
		}
	}

	/**
	 * 获取所有外部托管的配置方法和字段 (作为不可变集)。
	 *
	 * @since 5.3.11
	 */
	public Set<Member> getExternallyManagedConfigMembers() {
		synchronized (this.postProcessingLock) {
			return (this.externallyManagedConfigMembers != null ?
					Collections.unmodifiableSet(new LinkedHashSet<>(this.externallyManagedConfigMembers)) :
					Collections.emptySet());
		}
	}

	/**
	 * 注册一个外部管理的配置初始化方法-例如，
	 * 用JSR-250的 {@link javax.annotation.PostConstruct} 注解注释的方法。
	 * <p> 提供的 {@code initMethod} 可以是非私有方法的 {@linkplain Method#getName() 简单方法名称}，
	 * 也可以是 {@code private} 方法的 {@linkplain org.springframework.util.ClassUtils#getQualifiedMethodName(Method) 限定方法名称}。
	 * 为了消除类层次结构中具有相同名称的多个私有方法之间的歧义，{@code private} 方法必须使用限定名称。
	 */
	public void registerExternallyManagedInitMethod(String initMethod) {
		synchronized (this.postProcessingLock) {
			if (this.externallyManagedInitMethods == null) {
				this.externallyManagedInitMethods = new LinkedHashSet<>(1);
			}
			this.externallyManagedInitMethods.add(initMethod);
		}
	}

	/**
	 * 确定给定的方法名称是否表示外部管理的初始化方法。
	 * <p> 有关提供的 {@code initMethod} 格式的详细信息，请参见 {@link #registerExternallyManagedInitMethod}。
	 */
	public boolean isExternallyManagedInitMethod(String initMethod) {
		synchronized (this.postProcessingLock) {
			return (this.externallyManagedInitMethods != null &&
					this.externallyManagedInitMethods.contains(initMethod));
		}
	}

	/**
	 * 确定给定的方法名称是否指示外部管理的初始化方法，而与方法可见性无关。
	 * <p> 与 {@link #isExternallyManagedInitMethod(String)} 相反，如果存在 {@code private} 外部管理的初始化方法，
	 * 该方法使用限定的方法名称，而不是简单的方法名称进行注册 {@linkplain #registerExternallyManagedInitMethod(String)}，
	 * 则此方法还返回 {@code true}。
	 *
	 * @since 5.3.17
	 */
	boolean hasAnyExternallyManagedInitMethod(String initMethod) {
		synchronized (this.postProcessingLock) {
			if (isExternallyManagedInitMethod(initMethod)) {
				//如果存在该名称的外部管理的初始化方法，返回true
				return true;
			}
			if (this.externallyManagedInitMethods != null) {
				//对外部管理的初始化方法名称进行遍历。
				for (String candidate : this.externallyManagedInitMethods) {
					//获取.的位置
					int indexOfDot = candidate.lastIndexOf('.');
					if (indexOfDot >= 0) {
						//如果含有.字符，获取.字符后的方法名称
						String methodName = candidate.substring(indexOfDot + 1);
						if (methodName.equals(initMethod)) {
							return true;
						}
					}
				}
			}
			return false;
		}
	}

	/**
	 * 返回所有外部管理的初始化方法 (作为不可变集)。
	 * <p> 有关返回的集合中初始化方法的格式的详细信息，请参见 {@link #registerExternallyManagedInitMethod}。
	 *
	 * @since 5.3.11
	 */
	public Set<String> getExternallyManagedInitMethods() {
		synchronized (this.postProcessingLock) {
			return (this.externallyManagedInitMethods != null ?
					Collections.unmodifiableSet(new LinkedHashSet<>(this.externallyManagedInitMethods)) :
					Collections.emptySet());
		}
	}

	/**
	 * 注册外部托管的配置销毁方法-——例如，用JSR-250的 {@link javax.annotation.PreDestroy} 注释注释的方法。
	 * <p> 提供的 {@code destroyMethod} 可以是非私有方法的 {@linkplain Method#getName() 简单方法名称}，
	 * 也可以是 {@code private} 方法的 {@linkplain org.springframework.util.ClassUtils#getQualifiedMethodName(Method) 限定方法名称}。
	 * 为了消除类层次结构中具有相同名称的多个私有方法之间的歧义，{@code private} 方法必须使用限定名称。
	 */
	public void registerExternallyManagedDestroyMethod(String destroyMethod) {
		synchronized (this.postProcessingLock) {
			if (this.externallyManagedDestroyMethods == null) {
				this.externallyManagedDestroyMethods = new LinkedHashSet<>(1);
			}
			this.externallyManagedDestroyMethods.add(destroyMethod);
		}
	}

	/**
	 * 确定给定的方法名称是否表示外部托管的销毁方法。
	 * <p> 有关提供的 {@code destroyMethod} 格式的详细信息，请参见 {@link #registerExternallyManagedDestroyMethod}。
	 */
	public boolean isExternallyManagedDestroyMethod(String destroyMethod) {
		synchronized (this.postProcessingLock) {
			return (this.externallyManagedDestroyMethods != null &&
					this.externallyManagedDestroyMethods.contains(destroyMethod));
		}
	}

	/**
	 * 确定给定的方法名称是否指示外部管理的销毁方法，而不考虑方法可见性。
	 * <p> 与 {@link #isExternallyManagedDestroyMethod(String)} 相反，
	 * 如果存在 {@code private} 外部管理的销毁方法，该方法使用限定的方法名称，
	 * 而不是简单的方法名称注册了 {@linkplain #registerExternallyManagedDestroyMethod(String)}，该方法还返回 {@code true}。
	 *
	 * @since 5.3.17
	 */
	boolean hasAnyExternallyManagedDestroyMethod(String destroyMethod) {
		synchronized (this.postProcessingLock) {
			if (isExternallyManagedDestroyMethod(destroyMethod)) {
				//如果存在该名称的外部管理的销毁方法，返回true
				return true;
			}
			if (this.externallyManagedDestroyMethods != null) {
				//对外部管理的销毁方法名称进行遍历。
				for (String candidate : this.externallyManagedDestroyMethods) {
					//获取.的位置
					int indexOfDot = candidate.lastIndexOf('.');
					if (indexOfDot >= 0) {
						//如果含有.字符，获取.字符后的方法名称
						String methodName = candidate.substring(indexOfDot + 1);
						if (methodName.equals(destroyMethod)) {
							return true;
						}
					}
				}
			}
			return false;
		}
	}

	/**
	 * 获取所有外部管理的销毁方法 (作为不可变集)。
	 * <p> 有关返回的集中销毁方法格式的详细信息，请参见 {@link #registerExternallyManagedDestroyMethod}。
	 *
	 * @since 5.3.11
	 */
	public Set<String> getExternallyManagedDestroyMethods() {
		synchronized (this.postProcessingLock) {
			return (this.externallyManagedDestroyMethods != null ?
					Collections.unmodifiableSet(new LinkedHashSet<>(this.externallyManagedDestroyMethods)) :
					Collections.emptySet());
		}
	}


	@Override
	public RootBeanDefinition cloneBeanDefinition() {
		return new RootBeanDefinition(this);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof RootBeanDefinition && super.equals(other)));
	}

	@Override
	public String toString() {
		return "Root bean: " + super.toString();
	}

}
