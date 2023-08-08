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

package org.springframework.beans.factory.support;

import org.springframework.beans.BeanMetadataAttributeAccessor;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Supplier;

/**
 * Base class for concrete, full-fledged {@link BeanDefinition} classes,
 * factoring out common properties of {@link GenericBeanDefinition},
 * {@link RootBeanDefinition}, and {@link ChildBeanDefinition}.
 *
 * <p>The autowire constants match the ones defined in the
 * {@link org.springframework.beans.factory.config.AutowireCapableBeanFactory}
 * interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Mark Fisher
 * @see GenericBeanDefinition
 * @see RootBeanDefinition
 * @see ChildBeanDefinition
 */
@SuppressWarnings("serial")
public abstract class AbstractBeanDefinition extends BeanMetadataAttributeAccessor
		implements BeanDefinition, Cloneable {

	/**
	 * 默认作用域名称的常量: {@code ""}，相当于单例状态，除非从父bean定义重写 (如果适用)。
	 */
	public static final String SCOPE_DEFAULT = "";

	/**
	 * 表示没有任何外部自动装配的常量
	 * 默认不进行自动装配
	 *
	 * @see #setAutowireMode
	 */
	public static final int AUTOWIRE_NO = AutowireCapableBeanFactory.AUTOWIRE_NO;

	/**
	 * 表示按名称自动装配bean属性的常量。
	 * 根据 Bean 的名字进行自动装配，byName
	 *
	 * @see #setAutowireMode
	 */
	public static final int AUTOWIRE_BY_NAME = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;

	/**
	 * 表示按照类型自动装配bean属性的常量。
	 * 根据Bean的类型进行自动装配，byType
	 *
	 * @see #setAutowireMode
	 */
	public static final int AUTOWIRE_BY_TYPE = AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;

	/**
	 * 表示自动装配一个构造函数的常量。
	 * 根据构造器自动装配
	 *
	 * @see #setAutowireMode
	 */
	public static final int AUTOWIRE_CONSTRUCTOR = AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR;

	/**
	 * 表示通过对bean类的内省来确定适当自动装配策略的常量。
	 * 首先尝试按构造器自动装配。如果失败，再尝试使用 byType 进行自动装配。（Spring 3.0 之后已废除）
	 *
	 * @see #setAutowireMode
	 * @deprecated as of Spring 3.0: 如果您使用的是混合自动装配策略，请使用基于注解的自动装配，以更清晰地划分自动装配需求。
	 */
	@Deprecated
	public static final int AUTOWIRE_AUTODETECT = AutowireCapableBeanFactory.AUTOWIRE_AUTODETECT;

	/**
	 * 表示完全没有依赖检查的常量。
	 * 通过依赖检查来查看 Bean 的每个属性是否都设置完成，这里表示是不检查。
	 *
	 * @see #setDependencyCheck
	 */
	public static final int DEPENDENCY_CHECK_NONE = 0;

	/**
	 * 常量，指示对象引用的依赖检查。
	 * 通过依赖检查来查看 Bean 的每个属性是否都设置完成，这里表示是对依赖对象检查
	 *
	 * @see #setDependencyCheck
	 */
	public static final int DEPENDENCY_CHECK_OBJECTS = 1;

	/**
	 * 常量，表示对 “简单” 属性的依赖检查。
	 * 通过依赖检查来查看 Bean 的每个属性是否都设置完成，这里表示是对基本类型，字符串和集合进行检查。
	 *
	 * @see #setDependencyCheck
	 * @see org.springframework.beans.BeanUtils#isSimpleProperty
	 */
	public static final int DEPENDENCY_CHECK_SIMPLE = 2;

	/**
	 * 常量，表示对所有属性 (对象引用以及 “简单” 属性) 进行依赖检查。
	 * 通过依赖检查来查看 Bean 的每个属性是否都设置完成，这里表示是对全部属性进行检查
	 *
	 * @see #setDependencyCheck
	 */
	public static final int DEPENDENCY_CHECK_ALL = 3;

	/**
	 * 表示容器应尝试推断bean的 {@link #setDestroyMethodName destroy方法名称}，而不是方法名称的显式规范。
	 * 值 {@value} 专门设计用于在方法名称中包含其他非法字符，从而确保与具有相同名称的合法命名方法不会发生冲突。
	 * <p> 当前，如果在特定的bean类上存在，则在destroy方法推断期间检测到的方法名称为 “close” 和 “shutdown”。
	 */
	public static final String INFER_METHOD = "(inferred)";


	/**
	 * 存放bean的Class对象
	 */
	@Nullable
	private volatile Object beanClass;

	/**
	 * Bean的作用范围，默认为单例
	 */
	@Nullable
	private String scope = SCOPE_DEFAULT;

	/**
	 * 是否为抽象bean，默认为非抽象bean
	 */
	private boolean abstractFlag = false;

	/**
	 * 是否懒加载
	 */
	@Nullable
	private Boolean lazyInit;

	/**
	 * 自动装配模式，默认不进行自动装配
	 */
	private int autowireMode = AUTOWIRE_NO;

	/**
	 * 依赖检查，默认不检查
	 */
	private int dependencyCheck = DEPENDENCY_CHECK_NONE;

	/**
	 * 依赖的Bean列表
	 */
	@Nullable
	private String[] dependsOn;

	/**
	 * 可以作为自动装配的候选者，意味着可以自动装配到其他 Bean 的某个属性中
	 */
	private boolean autowireCandidate = true;

	/**
	 * 是否是主要的候选bean，默认为false
	 */
	private boolean primary = false;

	/**
	 * 类名-自动装配候选者映射
	 */
	private final Map<String, AutowireCandidateQualifier> qualifiers = new LinkedHashMap<>();

	/**
	 * 实例提供者
	 */
	@Nullable
	private Supplier<?> instanceSupplier;

	/**
	 * 是否允许访问非公开构造函数，非公开方法
	 * 该属性主要用于构造函数解析，初始化方法,析构方法解析，bean属性的set/get方法不受该属性影响
	 */
	private boolean nonPublicAccessAllowed = true;

	/**
	 * 调用构造函数时，是否采用宽松匹配，默认为采用宽松匹配。
	 */
	private boolean lenientConstructorResolution = true;

	/**
	 * 工厂bean名称
	 */
	@Nullable
	private String factoryBeanName;

	/**
	 * 工厂方法名称
	 */
	@Nullable
	private String factoryMethodName;

	/**
	 * 构造函数参数值
	 */
	@Nullable
	private ConstructorArgumentValues constructorArgumentValues;
	/**
	 * 属性值，注意这里使用了 MutablePropertyValues ， 表示这些属性值在
	 * 最终被设置到 bean实例之前一直是可以被修改的
	 */
	@Nullable
	private MutablePropertyValues propertyValues;

	/**
	 * 方法覆盖，存放lookup-method和replaced-method这两个标签的值
	 */
	private MethodOverrides methodOverrides = new MethodOverrides();

	/**
	 * 初始化方法名称
	 */
	@Nullable
	private String initMethodName;

	/**
	 * 销毁方法名称
	 */
	@Nullable
	private String destroyMethodName;

	/**
	 * 指定配置的init方法是否是默认值
	 */
	private boolean enforceInitMethod = true;

	/**
	 * 指示配置的destroy方法是否为默认方法。
	 */
	private boolean enforceDestroyMethod = true;

	/**
	 * 返回此bean定义是否为 “合成的”，即不是由应用程序本身定义的。
	 */
	private boolean synthetic = false;

	/**
	 * bean定义的角色值，用于区分是系统的bean定义，还是用户定义的bean定义。
	 */
	private int role = BeanDefinition.ROLE_APPLICATION;

	/**
	 * bean定义的描述
	 */
	@Nullable
	private String description;

	/**
	 * bean定义的来源
	 */
	@Nullable
	private Resource resource;


	/**
	 * Create a new AbstractBeanDefinition with default settings.
	 */
	protected AbstractBeanDefinition() {
		this(null, null);
	}

	/**
	 * Create a new AbstractBeanDefinition with the given
	 * constructor argument values and property values.
	 */
	protected AbstractBeanDefinition(@Nullable ConstructorArgumentValues cargs, @Nullable MutablePropertyValues pvs) {
		this.constructorArgumentValues = cargs;
		this.propertyValues = pvs;
	}

	/**
	 * 创建一个新的AbstractBeanDefinition作为给定bean定义的深层副本。
	 *
	 * @param original 要复制的原始bean定义
	 */
	protected AbstractBeanDefinition(BeanDefinition original) {
		//复制父bean定义名称
		setParentName(original.getParentName());
		//复制bean类名
		setBeanClassName(original.getBeanClassName());
		//复制范围
		setScope(original.getScope());
		//复制是否抽象
		setAbstract(original.isAbstract());
		//复制工厂bean名称
		setFactoryBeanName(original.getFactoryBeanName());
		//复制工厂方法名
		setFactoryMethodName(original.getFactoryMethodName());
		//设置角色
		setRole(original.getRole());
		//复制源
		setSource(original.getSource());
		//复制属性名和属性值
		copyAttributesFrom(original);

		if (original instanceof AbstractBeanDefinition) {
			//如果该bean定义是AbstractBeanDefinition类型
			AbstractBeanDefinition originalAbd = (AbstractBeanDefinition) original;
			if (originalAbd.hasBeanClass()) {
				//有bean类型，则设置bean类
				setBeanClass(originalAbd.getBeanClass());
			}
			if (originalAbd.hasConstructorArgumentValues()) {
				//有构造参数值，则只是构造参数值
				setConstructorArgumentValues(new ConstructorArgumentValues(original.getConstructorArgumentValues()));
			}
			if (originalAbd.hasPropertyValues()) {
				//如果有属性值，设置属性值
				setPropertyValues(new MutablePropertyValues(original.getPropertyValues()));
			}
			if (originalAbd.hasMethodOverrides()) {
				//复制覆盖方法
				setMethodOverrides(new MethodOverrides(originalAbd.getMethodOverrides()));
			}
			Boolean lazyInit = originalAbd.getLazyInit();
			if (lazyInit != null) {
				//复制懒加载属性
				setLazyInit(lazyInit);
			}
			//复制装配模式
			setAutowireMode(originalAbd.getAutowireMode());
			//复制依赖检查
			setDependencyCheck(originalAbd.getDependencyCheck());
			//复制依赖bean名称
			setDependsOn(originalAbd.getDependsOn());
			//复制自动装配候选者
			setAutowireCandidate(originalAbd.isAutowireCandidate());
			//复制是否主要
			setPrimary(originalAbd.isPrimary());
			//复制限定符
			copyQualifiersFrom(originalAbd);
			//复制实例提供者
			setInstanceSupplier(originalAbd.getInstanceSupplier());
			//复制允许非公共访问
			setNonPublicAccessAllowed(originalAbd.isNonPublicAccessAllowed());
			//复制宽松的构造函数解析
			setLenientConstructorResolution(originalAbd.isLenientConstructorResolution());
			//复制Init方法名称
			setInitMethodName(originalAbd.getInitMethodName());
			//复制强制的初始化方法
			setEnforceInitMethod(originalAbd.isEnforceInitMethod());
			//复制销毁方法名称
			setDestroyMethodName(originalAbd.getDestroyMethodName());
			//复制是否强制销毁方法
			setEnforceDestroyMethod(originalAbd.isEnforceDestroyMethod());
			//复制是否合成bean
			setSynthetic(originalAbd.isSynthetic());
			//复制源
			setResource(originalAbd.getResource());
		} else {
			//复制构造参数对
			setConstructorArgumentValues(new ConstructorArgumentValues(original.getConstructorArgumentValues()));
			//复制属性值
			setPropertyValues(new MutablePropertyValues(original.getPropertyValues()));
			//复制是否懒加载
			setLazyInit(original.isLazyInit());
			//复制属性描述
			setResourceDescription(original.getResourceDescription());
		}
	}


	/**
	 * 从给定的bean定义 (大概是子级) 中覆盖此bean定义中的设置 (大概是从父子继承关系中复制的父级)。
	 * <ul>
	 * <li>如果在给定的bean定义中指定，将覆盖beanClass。
	 * <li>将始终从给定的bean定义中采用 {@code abstract}，{@code scope}，{@code lazyInit}，
	 * {@code autowireMode}，{@code dependencyCheck} 和 {@code dependsOn}。
	 * <li>将从给定的bean定义添加 {@code constructorArgumentValues} 、 {@code propertyValues} 、 {@code methodOverrides} 到现有的。
	 * <li>如果在给定的bean定义中指定，将覆盖 {@code factoryBeanName} 、
	 * {@code factoryMethodName} 、 {@code initMethodName} 和 {@code destroyMethodName}。
	 * </ul>
	 */
	public void overrideFrom(BeanDefinition other) {
		if (StringUtils.hasLength(other.getBeanClassName())) {
			//如果其他bean定义的bean类型不为空，则设置bean类名
			setBeanClassName(other.getBeanClassName());
		}
		if (StringUtils.hasLength(other.getScope())) {
			//如果其他bean定义的scope不为空，则设置scope
			setScope(other.getScope());
		}
		//设置是否是抽象的
		setAbstract(other.isAbstract());
		if (StringUtils.hasLength(other.getFactoryBeanName())) {
			//如果其他bean定义的工厂Bean名称不为空，则设置当前bean定义的工厂Bean名称
			setFactoryBeanName(other.getFactoryBeanName());
		}
		if (StringUtils.hasLength(other.getFactoryMethodName())) {
			//如果其他bean定义的工厂方法名称不为空，则设置当前bean定义的工厂方法名称
			setFactoryMethodName(other.getFactoryMethodName());
		}
		//复制角色属性
		setRole(other.getRole());
		//复制源
		setSource(other.getSource());
		//复制各个属性
		copyAttributesFrom(other);

		if (other instanceof AbstractBeanDefinition) {
			//如果其他bean定义是AbstractBeanDefinition类型
			AbstractBeanDefinition otherAbd = (AbstractBeanDefinition) other;
			if (otherAbd.hasBeanClass()) {
				//复制bean类型
				setBeanClass(otherAbd.getBeanClass());
			}
			if (otherAbd.hasConstructorArgumentValues()) {
				//有构造参数，添加构造参数
				getConstructorArgumentValues().addArgumentValues(other.getConstructorArgumentValues());
			}
			if (otherAbd.hasPropertyValues()) {
				//有属性值对，添加属性值对
				getPropertyValues().addPropertyValues(other.getPropertyValues());
			}
			if (otherAbd.hasMethodOverrides()) {
				//有覆盖方法，添加覆盖方法
				getMethodOverrides().addOverrides(otherAbd.getMethodOverrides());
			}
			Boolean lazyInit = otherAbd.getLazyInit();
			if (lazyInit != null) {
				//复制是否懒加载
				setLazyInit(lazyInit);
			}
			//复制自动装配模式
			setAutowireMode(otherAbd.getAutowireMode());
			//复制依赖检查
			setDependencyCheck(otherAbd.getDependencyCheck());
			//复制依赖bean名称
			setDependsOn(otherAbd.getDependsOn());
			//复制自动装配候选者
			setAutowireCandidate(otherAbd.isAutowireCandidate());
			setPrimary(otherAbd.isPrimary());
			//复制限定符
			copyQualifiersFrom(otherAbd);
			//复制实例提供者
			setInstanceSupplier(otherAbd.getInstanceSupplier());
			//复制允许非公共访问
			setNonPublicAccessAllowed(otherAbd.isNonPublicAccessAllowed());
			//复制宽松的构造函数解析
			setLenientConstructorResolution(otherAbd.isLenientConstructorResolution());
			if (otherAbd.getInitMethodName() != null) {
				//复制Init方法名称
				setInitMethodName(otherAbd.getInitMethodName());
				//复制强制的初始化方法
				setEnforceInitMethod(otherAbd.isEnforceInitMethod());
			}
			if (otherAbd.getDestroyMethodName() != null) {
				//复制是否强制销毁方法
				setDestroyMethodName(otherAbd.getDestroyMethodName());
				//复制是否强制销毁方法
				setEnforceDestroyMethod(otherAbd.isEnforceDestroyMethod());
			}
			//复制是否合成bean
			setSynthetic(otherAbd.isSynthetic());
			//复制源
			setResource(otherAbd.getResource());
		} else {
			//复制构造参数对
			getConstructorArgumentValues().addArgumentValues(other.getConstructorArgumentValues());
			//复制属性值
			getPropertyValues().addPropertyValues(other.getPropertyValues());
			//复制是否懒加载
			setLazyInit(other.isLazyInit());
			//复制属性描述
			setResourceDescription(other.getResourceDescription());
		}
	}

	/**
	 * Apply the provided default values to this bean.
	 *
	 * @param defaults the default settings to apply
	 * @since 2.5
	 */
	public void applyDefaults(BeanDefinitionDefaults defaults) {
		Boolean lazyInit = defaults.getLazyInit();
		if (lazyInit != null) {
			setLazyInit(lazyInit);
		}
		setAutowireMode(defaults.getAutowireMode());
		setDependencyCheck(defaults.getDependencyCheck());
		setInitMethodName(defaults.getInitMethodName());
		setEnforceInitMethod(false);
		setDestroyMethodName(defaults.getDestroyMethodName());
		setEnforceDestroyMethod(false);
	}


	/**
	 * 指定此bean定义的bean类名。
	 */
	@Override
	public void setBeanClassName(@Nullable String beanClassName) {
		this.beanClass = beanClassName;
	}

	/**
	 * Return the current bean class name of this bean definition.
	 */
	@Override
	@Nullable
	public String getBeanClassName() {
		Object beanClassObject = this.beanClass;
		if (beanClassObject instanceof Class) {
			return ((Class<?>) beanClassObject).getName();
		} else {
			return (String) beanClassObject;
		}
	}

	/**
	 * Specify the class for this bean.
	 *
	 * @see #setBeanClassName(String)
	 */
	public void setBeanClass(@Nullable Class<?> beanClass) {
		this.beanClass = beanClass;
	}

	/**
	 * 返回bean定义的指定类 (假设已经解析)。
	 * <p><b> 注意:<b> 这是bean元数据定义中声明的初始类引用，
	 * 可能与声明的工厂方法或 {@link org.springframework.beans.factory.FactoryBean} 结合使用，
	 * 这可能导致bean的不同运行时类型，或者在实例级工厂方法的情况下根本就不设置 (而是通过 {@link #getFactoryBeanName()} 解决)。
	 *
	 * <b>不要将其用于任意bean定义的运行时类型内省。
	 * <b> 查找特定bean的实际运行时类型的推荐方法是
	 * 对指定bean名称的 {@link org.springframework.beans.factory.BeanFactory#getType} 调用；
	 * 这将考虑上述所有情况，并返回 {@link org.springframework.beans.factory.BeanFactory#getBean}
	 * 调用将返回相同bean名称的对象类型。
	 *
	 * @return 已解析的bean类 (从不为 {@code null})
	 * @throws IllegalStateException 如果bean定义未定义bean类，或者尚未将指定的bean类名称解析为实际类
	 * @see #getBeanClassName()
	 * @see #hasBeanClass()
	 * @see #setBeanClass(Class)
	 * @see #resolveBeanClass(ClassLoader)
	 */
	public Class<?> getBeanClass() throws IllegalStateException {
		Object beanClassObject = this.beanClass;
		if (beanClassObject == null) {
			//如果bean类名为空，抛出异常
			throw new IllegalStateException("No bean class specified on bean definition");
		}
		if (!(beanClassObject instanceof Class)) {
			//如果bean类型不是Class类型，抛出异常
			throw new IllegalStateException(
					"Bean class name [" + beanClassObject + "] has not been resolved into an actual Class");
		}
		//返回Class对象
		return (Class<?>) beanClassObject;
	}

	/**
	 * 返回此定义是否指定bean类。
	 *
	 * @see #getBeanClass()
	 * @see #setBeanClass(Class)
	 * @see #resolveBeanClass(ClassLoader)
	 */
	public boolean hasBeanClass() {
		return (this.beanClass instanceof Class);
	}

	/**
	 * Determine the class of the wrapped bean, resolving it from a
	 * specified class name if necessary. Will also reload a specified
	 * Class from its name when called with the bean class already resolved.
	 *
	 * @param classLoader the ClassLoader to use for resolving a (potential) class name
	 * @return the resolved bean class
	 * @throws ClassNotFoundException if the class name could be resolved
	 */
	@Nullable
	public Class<?> resolveBeanClass(@Nullable ClassLoader classLoader) throws ClassNotFoundException {
		String className = getBeanClassName();
		if (className == null) {
			return null;
		}
		Class<?> resolvedClass = ClassUtils.forName(className, classLoader);
		this.beanClass = resolvedClass;
		return resolvedClass;
	}

	/**
	 * Return a resolvable type for this bean definition.
	 * <p>This implementation delegates to {@link #getBeanClass()}.
	 *
	 * @since 5.2
	 */
	@Override
	public ResolvableType getResolvableType() {
		return (hasBeanClass() ? ResolvableType.forClass(getBeanClass()) : ResolvableType.NONE);
	}

	/**
	 * Set the name of the target scope for the bean.
	 * <p>The default is singleton status, although this is only applied once
	 * a bean definition becomes active in the containing factory. A bean
	 * definition may eventually inherit its scope from a parent bean definition.
	 * For this reason, the default scope name is an empty string (i.e., {@code ""}),
	 * with singleton status being assumed until a resolved scope is set.
	 *
	 * @see #SCOPE_SINGLETON
	 * @see #SCOPE_PROTOTYPE
	 */
	@Override
	public void setScope(@Nullable String scope) {
		this.scope = scope;
	}

	/**
	 * Return the name of the target scope for the bean.
	 */
	@Override
	@Nullable
	public String getScope() {
		return this.scope;
	}

	/**
	 * 返回此是否为 <b>单例<b>，并从所有调用中返回单个共享实例。
	 *
	 * @see #SCOPE_SINGLETON
	 */
	@Override
	public boolean isSingleton() {
		return SCOPE_SINGLETON.equals(this.scope) || SCOPE_DEFAULT.equals(this.scope);
	}

	/**
	 * Return whether this a <b>Prototype</b>, with an independent instance
	 * returned for each call.
	 *
	 * @see #SCOPE_PROTOTYPE
	 */
	@Override
	public boolean isPrototype() {
		return SCOPE_PROTOTYPE.equals(this.scope);
	}

	/**
	 * Set if this bean is "abstract", i.e. not meant to be instantiated itself but
	 * rather just serving as parent for concrete child bean definitions.
	 * <p>Default is "false". Specify true to tell the bean factory to not try to
	 * instantiate that particular bean in any case.
	 */
	public void setAbstract(boolean abstractFlag) {
		this.abstractFlag = abstractFlag;
	}

	/**
	 * Return whether this bean is "abstract", i.e. not meant to be instantiated
	 * itself but rather just serving as parent for concrete child bean definitions.
	 */
	@Override
	public boolean isAbstract() {
		return this.abstractFlag;
	}

	/**
	 * Set whether this bean should be lazily initialized.
	 * <p>If {@code false}, the bean will get instantiated on startup by bean
	 * factories that perform eager initialization of singletons.
	 */
	@Override
	public void setLazyInit(boolean lazyInit) {
		this.lazyInit = lazyInit;
	}

	/**
	 * Return whether this bean should be lazily initialized, i.e. not
	 * eagerly instantiated on startup. Only applicable to a singleton bean.
	 *
	 * @return whether to apply lazy-init semantics ({@code false} by default)
	 */
	@Override
	public boolean isLazyInit() {
		return (this.lazyInit != null && this.lazyInit.booleanValue());
	}

	/**
	 * Return whether this bean should be lazily initialized, i.e. not
	 * eagerly instantiated on startup. Only applicable to a singleton bean.
	 *
	 * @return the lazy-init flag if explicitly set, or {@code null} otherwise
	 * @since 5.2
	 */
	@Nullable
	public Boolean getLazyInit() {
		return this.lazyInit;
	}

	/**
	 * Set the autowire mode. This determines whether any automagical detection
	 * and setting of bean references will happen. Default is AUTOWIRE_NO
	 * which means there won't be convention-based autowiring by name or type
	 * (however, there may still be explicit annotation-driven autowiring).
	 *
	 * @param autowireMode the autowire mode to set.
	 *                     Must be one of the constants defined in this class.
	 * @see #AUTOWIRE_NO
	 * @see #AUTOWIRE_BY_NAME
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_CONSTRUCTOR
	 * @see #AUTOWIRE_AUTODETECT
	 */
	public void setAutowireMode(int autowireMode) {
		this.autowireMode = autowireMode;
	}

	/**
	 * Return the autowire mode as specified in the bean definition.
	 */
	public int getAutowireMode() {
		return this.autowireMode;
	}

	/**
	 * Return the resolved autowire code,
	 * (resolving AUTOWIRE_AUTODETECT to AUTOWIRE_CONSTRUCTOR or AUTOWIRE_BY_TYPE).
	 *
	 * @see #AUTOWIRE_AUTODETECT
	 * @see #AUTOWIRE_CONSTRUCTOR
	 * @see #AUTOWIRE_BY_TYPE
	 */
	public int getResolvedAutowireMode() {
		if (this.autowireMode == AUTOWIRE_AUTODETECT) {
			// Work out whether to apply setter autowiring or constructor autowiring.
			// If it has a no-arg constructor it's deemed to be setter autowiring,
			// otherwise we'll try constructor autowiring.
			Constructor<?>[] constructors = getBeanClass().getConstructors();
			for (Constructor<?> constructor : constructors) {
				if (constructor.getParameterCount() == 0) {
					return AUTOWIRE_BY_TYPE;
				}
			}
			return AUTOWIRE_CONSTRUCTOR;
		} else {
			return this.autowireMode;
		}
	}

	/**
	 * Set the dependency check code.
	 *
	 * @param dependencyCheck the code to set.
	 *                        Must be one of the four constants defined in this class.
	 * @see #DEPENDENCY_CHECK_NONE
	 * @see #DEPENDENCY_CHECK_OBJECTS
	 * @see #DEPENDENCY_CHECK_SIMPLE
	 * @see #DEPENDENCY_CHECK_ALL
	 */
	public void setDependencyCheck(int dependencyCheck) {
		this.dependencyCheck = dependencyCheck;
	}

	/**
	 * Return the dependency check code.
	 */
	public int getDependencyCheck() {
		return this.dependencyCheck;
	}

	/**
	 * Set the names of the beans that this bean depends on being initialized.
	 * The bean factory will guarantee that these beans get initialized first.
	 * <p>Note that dependencies are normally expressed through bean properties or
	 * constructor arguments. This property should just be necessary for other kinds
	 * of dependencies like statics (*ugh*) or database preparation on startup.
	 */
	@Override
	public void setDependsOn(@Nullable String... dependsOn) {
		this.dependsOn = dependsOn;
	}

	/**
	 * Return the bean names that this bean depends on.
	 */
	@Override
	@Nullable
	public String[] getDependsOn() {
		return this.dependsOn;
	}

	/**
	 * Set whether this bean is a candidate for getting autowired into some other bean.
	 * <p>Note that this flag is designed to only affect type-based autowiring.
	 * It does not affect explicit references by name, which will get resolved even
	 * if the specified bean is not marked as an autowire candidate. As a consequence,
	 * autowiring by name will nevertheless inject a bean if the name matches.
	 *
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_BY_NAME
	 */
	@Override
	public void setAutowireCandidate(boolean autowireCandidate) {
		this.autowireCandidate = autowireCandidate;
	}

	/**
	 * Return whether this bean is a candidate for getting autowired into some other bean.
	 */
	@Override
	public boolean isAutowireCandidate() {
		return this.autowireCandidate;
	}

	/**
	 * Set whether this bean is a primary autowire candidate.
	 * <p>If this value is {@code true} for exactly one bean among multiple
	 * matching candidates, it will serve as a tie-breaker.
	 */
	@Override
	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	/**
	 * Return whether this bean is a primary autowire candidate.
	 */
	@Override
	public boolean isPrimary() {
		return this.primary;
	}

	/**
	 * 注册一个用于自动装配候选解析的限定符，并以限定符的类型名称为键。
	 *
	 * @see AutowireCandidateQualifier#getTypeName()
	 */
	public void addQualifier(AutowireCandidateQualifier qualifier) {
		this.qualifiers.put(qualifier.getTypeName(), qualifier);
	}

	/**
	 * 返回此bean是否具有指定的限定符。
	 */
	public boolean hasQualifier(String typeName) {
		return this.qualifiers.containsKey(typeName);
	}

	/**
	 * Return the qualifier mapped to the provided type name.
	 */
	@Nullable
	public AutowireCandidateQualifier getQualifier(String typeName) {
		return this.qualifiers.get(typeName);
	}

	/**
	 * Return all registered qualifiers.
	 *
	 * @return the Set of {@link AutowireCandidateQualifier} objects.
	 */
	public Set<AutowireCandidateQualifier> getQualifiers() {
		return new LinkedHashSet<>(this.qualifiers.values());
	}

	/**
	 * 将限定符从提供的AbstractBeanDefinition复制到此bean定义。
	 *
	 * @param source 要复制的抽象bean定义
	 */
	public void copyQualifiersFrom(AbstractBeanDefinition source) {
		Assert.notNull(source, "Source must not be null");
		this.qualifiers.putAll(source.qualifiers);
	}

	/**
	 * Specify a callback for creating an instance of the bean,
	 * as an alternative to a declaratively specified factory method.
	 * <p>If such a callback is set, it will override any other constructor
	 * or factory method metadata. However, bean property population and
	 * potential annotation-driven injection will still apply as usual.
	 *
	 * @see #setConstructorArgumentValues(ConstructorArgumentValues)
	 * @see #setPropertyValues(MutablePropertyValues)
	 * @since 5.0
	 */
	public void setInstanceSupplier(@Nullable Supplier<?> instanceSupplier) {
		this.instanceSupplier = instanceSupplier;
	}

	/**
	 * Return a callback for creating an instance of the bean, if any.
	 *
	 * @since 5.0
	 */
	@Nullable
	public Supplier<?> getInstanceSupplier() {
		return this.instanceSupplier;
	}

	/**
	 * Specify whether to allow access to non-public constructors and methods,
	 * for the case of externalized metadata pointing to those. The default is
	 * {@code true}; switch this to {@code false} for public access only.
	 * <p>This applies to constructor resolution, factory method resolution,
	 * and also init/destroy methods. Bean property accessors have to be public
	 * in any case and are not affected by this setting.
	 * <p>Note that annotation-driven configuration will still access non-public
	 * members as far as they have been annotated. This setting applies to
	 * externalized metadata in this bean definition only.
	 */
	public void setNonPublicAccessAllowed(boolean nonPublicAccessAllowed) {
		this.nonPublicAccessAllowed = nonPublicAccessAllowed;
	}

	/**
	 * Return whether to allow access to non-public constructors and methods.
	 */
	public boolean isNonPublicAccessAllowed() {
		return this.nonPublicAccessAllowed;
	}

	/**
	 * Specify whether to resolve constructors in lenient mode ({@code true},
	 * which is the default) or to switch to strict resolution (throwing an exception
	 * in case of ambiguous constructors that all match when converting the arguments,
	 * whereas lenient mode would use the one with the 'closest' type matches).
	 */
	public void setLenientConstructorResolution(boolean lenientConstructorResolution) {
		this.lenientConstructorResolution = lenientConstructorResolution;
	}

	/**
	 * Return whether to resolve constructors in lenient mode or in strict mode.
	 */
	public boolean isLenientConstructorResolution() {
		return this.lenientConstructorResolution;
	}

	/**
	 * 指定要使用的工厂bean (如果有)。这是要调用指定工厂方法的bean的名称。
	 *
	 * @see #setFactoryMethodName
	 */
	@Override
	public void setFactoryBeanName(@Nullable String factoryBeanName) {
		this.factoryBeanName = factoryBeanName;
	}

	/**
	 * Return the factory bean name, if any.
	 */
	@Override
	@Nullable
	public String getFactoryBeanName() {
		return this.factoryBeanName;
	}

	/**
	 * Specify a factory method, if any. This method will be invoked with
	 * constructor arguments, or with no arguments if none are specified.
	 * The method will be invoked on the specified factory bean, if any,
	 * or otherwise as a static method on the local bean class.
	 *
	 * @see #setFactoryBeanName
	 * @see #setBeanClassName
	 */
	@Override
	public void setFactoryMethodName(@Nullable String factoryMethodName) {
		this.factoryMethodName = factoryMethodName;
	}

	/**
	 * 返回工厂方法 (如果有)。
	 */
	@Override
	@Nullable
	public String getFactoryMethodName() {
		return this.factoryMethodName;
	}

	/**
	 * Specify constructor argument values for this bean.
	 */
	public void setConstructorArgumentValues(ConstructorArgumentValues constructorArgumentValues) {
		this.constructorArgumentValues = constructorArgumentValues;
	}

	/**
	 * Return constructor argument values for this bean (never {@code null}).
	 */
	@Override
	public ConstructorArgumentValues getConstructorArgumentValues() {
		if (this.constructorArgumentValues == null) {
			this.constructorArgumentValues = new ConstructorArgumentValues();
		}
		return this.constructorArgumentValues;
	}

	/**
	 * Return if there are constructor argument values defined for this bean.
	 */
	@Override
	public boolean hasConstructorArgumentValues() {
		return (this.constructorArgumentValues != null && !this.constructorArgumentValues.isEmpty());
	}

	/**
	 * Specify property values for this bean, if any.
	 */
	public void setPropertyValues(MutablePropertyValues propertyValues) {
		this.propertyValues = propertyValues;
	}

	/**
	 * 返回此bean的属性值 (从不为 {@code null})。
	 */
	@Override
	public MutablePropertyValues getPropertyValues() {
		if (this.propertyValues == null) {
			this.propertyValues = new MutablePropertyValues();
		}
		return this.propertyValues;
	}

	/**
	 * 如果为此bean定义了属性值，则返回。
	 *
	 * @since 5.0.2
	 */
	@Override
	public boolean hasPropertyValues() {
		return (this.propertyValues != null && !this.propertyValues.isEmpty());
	}

	/**
	 * Specify method overrides for the bean, if any.
	 */
	public void setMethodOverrides(MethodOverrides methodOverrides) {
		this.methodOverrides = methodOverrides;
	}

	/**
	 * 返回由IoC容器覆盖的方法信息。如果没有方法覆盖，这将是空的。<p> 从不返回 {@code null}。
	 */
	public MethodOverrides getMethodOverrides() {
		return this.methodOverrides;
	}

	/**
	 * 如果为此bean定义了方法覆盖，则返回。
	 *
	 * @since 5.0.2
	 */
	public boolean hasMethodOverrides() {
		return !this.methodOverrides.isEmpty();
	}

	/**
	 * Set the name of the initializer method.
	 * <p>The default is {@code null} in which case there is no initializer method.
	 */
	@Override
	public void setInitMethodName(@Nullable String initMethodName) {
		this.initMethodName = initMethodName;
	}

	/**
	 * Return the name of the initializer method.
	 */
	@Override
	@Nullable
	public String getInitMethodName() {
		return this.initMethodName;
	}

	/**
	 * Specify whether or not the configured initializer method is the default.
	 * <p>The default value is {@code true} for a locally specified init method
	 * but switched to {@code false} for a shared setting in a defaults section
	 * (e.g. {@code bean init-method} versus {@code beans default-init-method}
	 * level in XML) which might not apply to all contained bean definitions.
	 *
	 * @see #setInitMethodName
	 * @see #applyDefaults
	 */
	public void setEnforceInitMethod(boolean enforceInitMethod) {
		this.enforceInitMethod = enforceInitMethod;
	}

	/**
	 * 指示配置的 initializer 方法是否为默认值。
	 *
	 * @see #getInitMethodName()
	 */
	public boolean isEnforceInitMethod() {
		return this.enforceInitMethod;
	}

	/**
	 * Set the name of the destroy method.
	 * <p>The default is {@code null} in which case there is no destroy method.
	 */
	@Override
	public void setDestroyMethodName(@Nullable String destroyMethodName) {
		this.destroyMethodName = destroyMethodName;
	}

	/**
	 * Return the name of the destroy method.
	 */
	@Override
	@Nullable
	public String getDestroyMethodName() {
		return this.destroyMethodName;
	}

	/**
	 * Specify whether or not the configured destroy method is the default.
	 * <p>The default value is {@code true} for a locally specified destroy method
	 * but switched to {@code false} for a shared setting in a defaults section
	 * (e.g. {@code bean destroy-method} versus {@code beans default-destroy-method}
	 * level in XML) which might not apply to all contained bean definitions.
	 *
	 * @see #setDestroyMethodName
	 * @see #applyDefaults
	 */
	public void setEnforceDestroyMethod(boolean enforceDestroyMethod) {
		this.enforceDestroyMethod = enforceDestroyMethod;
	}

	/**
	 * 指示配置的destroy方法是否为默认方法。
	 *
	 * @see #getDestroyMethodName()
	 */
	public boolean isEnforceDestroyMethod() {
		return this.enforceDestroyMethod;
	}

	/**
	 * 设置此bean定义是否为 “合成的”，即不是由应用程序本身定义的
	 * (例如，通过 {@code <aop:config>} 创建的用于自动代理的助手之类的基础结构bean)。
	 */
	public void setSynthetic(boolean synthetic) {
		this.synthetic = synthetic;
	}

	/**
	 * 返回此bean定义是否为 “合成的”，即不是由应用程序本身定义的。
	 */
	public boolean isSynthetic() {
		return this.synthetic;
	}

	/**
	 * Set the role hint for this {@code BeanDefinition}.
	 */
	@Override
	public void setRole(int role) {
		this.role = role;
	}

	/**
	 * 返回此 {@code BeanDefinition} 的角色提示。
	 */
	@Override
	public int getRole() {
		return this.role;
	}

	/**
	 * Set a human-readable description of this bean definition.
	 */
	@Override
	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	/**
	 * Return a human-readable description of this bean definition.
	 */
	@Override
	@Nullable
	public String getDescription() {
		return this.description;
	}

	/**
	 * Set the resource that this bean definition came from
	 * (for the purpose of showing context in case of errors).
	 */
	public void setResource(@Nullable Resource resource) {
		this.resource = resource;
	}

	/**
	 * Return the resource that this bean definition came from.
	 */
	@Nullable
	public Resource getResource() {
		return this.resource;
	}

	/**
	 * Set a description of the resource that this bean definition
	 * came from (for the purpose of showing context in case of errors).
	 */
	public void setResourceDescription(@Nullable String resourceDescription) {
		this.resource = (resourceDescription != null ? new DescriptiveResource(resourceDescription) : null);
	}

	/**
	 * Return a description of the resource that this bean definition
	 * came from (for the purpose of showing context in case of errors).
	 */
	@Override
	@Nullable
	public String getResourceDescription() {
		return (this.resource != null ? this.resource.getDescription() : null);
	}

	/**
	 * Set the originating (e.g. decorated) BeanDefinition, if any.
	 */
	public void setOriginatingBeanDefinition(BeanDefinition originatingBd) {
		this.resource = new BeanDefinitionResource(originatingBd);
	}

	/**
	 * Return the originating BeanDefinition, or {@code null} if none.
	 * Allows for retrieving the decorated bean definition, if any.
	 * <p>Note that this method returns the immediate originator. Iterate through the
	 * originator chain to find the original BeanDefinition as defined by the user.
	 */
	@Override
	@Nullable
	public BeanDefinition getOriginatingBeanDefinition() {
		return (this.resource instanceof BeanDefinitionResource ?
				((BeanDefinitionResource) this.resource).getBeanDefinition() : null);
	}

	/**
	 * 验证此bean定义。
	 *
	 * @throws BeanDefinitionValidationException 在验证失败的情况下
	 */
	public void validate() throws BeanDefinitionValidationException {
		if (hasMethodOverrides() && getFactoryMethodName() != null) {
			//如果有方法被重载，并且有工厂方法，抛出 BeanDefinitionValidationException
			throw new BeanDefinitionValidationException(
					"Cannot combine factory method with container-generated method overrides: " +
							"the factory method must create the concrete bean instance.");
		}
		if (hasBeanClass()) {
			//如果该bean类为Class对象
			//准备方法覆盖
			prepareMethodOverrides();
		}
	}

	/**
	 * 验证并准备为此bean定义的方法覆盖。检查是否存在具有指定名称的方法。
	 *
	 * @throws BeanDefinitionValidationException 在验证失败的情况下
	 */
	public void prepareMethodOverrides() throws BeanDefinitionValidationException {
		// 检查查找方法是否存在，并确定其重载状态。
		//如果有方法被重载，对每一个重载方法准备重载
		if (hasMethodOverrides()) {
			//如果有重载方法，检查重载方法的数量
			getMethodOverrides().getOverrides().forEach(this::prepareMethodOverride);
		}
	}

	/**
	 * 验证并准备给定的方法覆盖。检查是否存在具有指定名称的方法，如果找不到，则将其标记为未重载。
	 *
	 * @param mo 要验证的MethodOverride对象
	 * @throws BeanDefinitionValidationException 在验证失败的情况下
	 */
	protected void prepareMethodOverride(MethodOverride mo) throws BeanDefinitionValidationException {
		//根据类名和方法名反射查找指定方法的数量
		int count = ClassUtils.getMethodCountForName(getBeanClass(), mo.getMethodName());
		if (count == 0) {
			//如果没有找到该方法，抛出异常
			throw new BeanDefinitionValidationException(
					"Invalid method override: no method with name '" + mo.getMethodName() +
							"' on class [" + getBeanClassName() + "]");
		} else if (count == 1) {
			//标记为未被重载，避免参数类型检查的开销
			mo.setOverloaded(false);
		}
	}


	/**
	 * Public declaration of Object's {@code clone()} method.
	 * Delegates to {@link #cloneBeanDefinition()}.
	 *
	 * @see Object#clone()
	 */
	@Override
	public Object clone() {
		return cloneBeanDefinition();
	}

	/**
	 * Clone this bean definition.
	 * To be implemented by concrete subclasses.
	 *
	 * @return the cloned bean definition object
	 */
	public abstract AbstractBeanDefinition cloneBeanDefinition();

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AbstractBeanDefinition)) {
			return false;
		}
		AbstractBeanDefinition that = (AbstractBeanDefinition) other;
		return (ObjectUtils.nullSafeEquals(getBeanClassName(), that.getBeanClassName()) &&
				ObjectUtils.nullSafeEquals(this.scope, that.scope) &&
				this.abstractFlag == that.abstractFlag &&
				this.lazyInit == that.lazyInit &&
				this.autowireMode == that.autowireMode &&
				this.dependencyCheck == that.dependencyCheck &&
				Arrays.equals(this.dependsOn, that.dependsOn) &&
				this.autowireCandidate == that.autowireCandidate &&
				ObjectUtils.nullSafeEquals(this.qualifiers, that.qualifiers) &&
				this.primary == that.primary &&
				this.nonPublicAccessAllowed == that.nonPublicAccessAllowed &&
				this.lenientConstructorResolution == that.lenientConstructorResolution &&
				equalsConstructorArgumentValues(that) &&
				equalsPropertyValues(that) &&
				ObjectUtils.nullSafeEquals(this.methodOverrides, that.methodOverrides) &&
				ObjectUtils.nullSafeEquals(this.factoryBeanName, that.factoryBeanName) &&
				ObjectUtils.nullSafeEquals(this.factoryMethodName, that.factoryMethodName) &&
				ObjectUtils.nullSafeEquals(this.initMethodName, that.initMethodName) &&
				this.enforceInitMethod == that.enforceInitMethod &&
				ObjectUtils.nullSafeEquals(this.destroyMethodName, that.destroyMethodName) &&
				this.enforceDestroyMethod == that.enforceDestroyMethod &&
				this.synthetic == that.synthetic &&
				this.role == that.role &&
				super.equals(other));
	}

	private boolean equalsConstructorArgumentValues(AbstractBeanDefinition other) {
		if (!hasConstructorArgumentValues()) {
			return !other.hasConstructorArgumentValues();
		}
		return ObjectUtils.nullSafeEquals(this.constructorArgumentValues, other.constructorArgumentValues);
	}

	private boolean equalsPropertyValues(AbstractBeanDefinition other) {
		if (!hasPropertyValues()) {
			return !other.hasPropertyValues();
		}
		return ObjectUtils.nullSafeEquals(this.propertyValues, other.propertyValues);
	}

	@Override
	public int hashCode() {
		int hashCode = ObjectUtils.nullSafeHashCode(getBeanClassName());
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.scope);
		if (hasConstructorArgumentValues()) {
			hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.constructorArgumentValues);
		}
		if (hasPropertyValues()) {
			hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.propertyValues);
		}
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.factoryBeanName);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.factoryMethodName);
		hashCode = 29 * hashCode + super.hashCode();
		return hashCode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("class [");
		sb.append(getBeanClassName()).append(']');
		sb.append("; scope=").append(this.scope);
		sb.append("; abstract=").append(this.abstractFlag);
		sb.append("; lazyInit=").append(this.lazyInit);
		sb.append("; autowireMode=").append(this.autowireMode);
		sb.append("; dependencyCheck=").append(this.dependencyCheck);
		sb.append("; autowireCandidate=").append(this.autowireCandidate);
		sb.append("; primary=").append(this.primary);
		sb.append("; factoryBeanName=").append(this.factoryBeanName);
		sb.append("; factoryMethodName=").append(this.factoryMethodName);
		sb.append("; initMethodName=").append(this.initMethodName);
		sb.append("; destroyMethodName=").append(this.destroyMethodName);
		if (this.resource != null) {
			sb.append("; defined in ").append(this.resource.getDescription());
		}
		return sb.toString();
	}

}
