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
	 * 表示容器应尝试推断bean的 {@link #setDestroyMethodName 销毁 方法名称}，而不是方法名称的显式规范。
	 * 值 {@value} 专门设计用于在方法名称中包含其他非法字符，从而确保与具有相同名称的合法命名方法不会发生冲突。
	 * <p> 当前，如果在特定的bean类上存在，则在销毁 方法推断期间检测到的方法名称为 “close” 和 “shutdown”。
	 */
	public static final String INFER_METHOD = "(inferred)";


	/**
	 * 存放bean的Class对象，这里有两种情况，一种是字符串类型的类名，一种是Class对象
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
	 * 是否执行初始化方法，默认执行初始化方法
	 */
	private boolean enforceInitMethod = true;

	/**
	 * 是否执行销毁方法，默认执行销毁方法。
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
	 * 使用默认设置创建新的AbstractBeanDefinition。
	 */
	protected AbstractBeanDefinition() {
		this(null, null);
	}

	/**
	 * 使用给定的构造函数参数值和属性值创建一个新的AbstractBeanDefinition。
	 */
	protected AbstractBeanDefinition(@Nullable ConstructorArgumentValues cargs, @Nullable MutablePropertyValues pvs) {
		//构造参数的值
		this.constructorArgumentValues = cargs;
		//bean定义的属性值
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
			//如果该bean定义是AbstractBeanDefinition类型，对应的是以XML的合并bean定义
			AbstractBeanDefinition originalAbd = (AbstractBeanDefinition) original;
			if (originalAbd.hasBeanClass()) {
				//有bean类型，则设置bean类
				setBeanClass(originalAbd.getBeanClass());
			}
			if (originalAbd.hasConstructorArgumentValues()) {
				//有构造参数值，则复制构造参数值
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
			//AnnotatedBeanDefinition体系分支，对应以注解形式的合并bean定义。
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
			//对应的是AnnotatedBeanDefinition体系分支，注解的属性复制。
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
	 * 将提供的默认值应用于此bean。
	 *
	 * @param defaults 要应用的默认设置
	 * @since 2.5
	 */
	public void applyDefaults(BeanDefinitionDefaults defaults) {
		//获取默认的懒加载策略
		Boolean lazyInit = defaults.getLazyInit();
		if (lazyInit != null) {
			setLazyInit(lazyInit);
		}
		//设置默认的自动装配模式
		setAutowireMode(defaults.getAutowireMode());
		//设置依赖检查
		setDependencyCheck(defaults.getDependencyCheck());
		//设置初始化方法名称
		setInitMethodName(defaults.getInitMethodName());
		//不执行初始化方法
		setEnforceInitMethod(false);
		//设置销毁方法名称
		setDestroyMethodName(defaults.getDestroyMethodName());
		//不执行销毁方法
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
	 * 返回此bean定义的当前bean类名称。
	 */
	@Override
	@Nullable
	public String getBeanClassName() {
		Object beanClassObject = this.beanClass;
		if (beanClassObject instanceof Class) {
			//如果是Class类，获取类名
			return ((Class<?>) beanClassObject).getName();
		} else {
			//直接强转为字符串返回
			return (String) beanClassObject;
		}
	}

	/**
	 * 指定此bean的类。
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
	 * 确定包装bean的类，必要时从指定的类名解析它。
	 * 当使用已经解析的bean类调用时，将从其名称重新加载为指定的类。
	 *
	 * @param classLoader 用于解析 (潜在) 类名的类加载器
	 * @return 解析的bean类
	 * @throws ClassNotFoundException 如果可以解析类名
	 */
	@Nullable
	public Class<?> resolveBeanClass(@Nullable ClassLoader classLoader) throws ClassNotFoundException {
		//获取类名
		String className = getBeanClassName();
		if (className == null) {
			//如果bean类名为空，返回null
			return null;
		}
		//通过类名和类加载器反射获取Class对象。
		Class<?> resolvedClass = ClassUtils.forName(className, classLoader);
		//设置bean类，并返回该Class对象。
		this.beanClass = resolvedClass;
		return resolvedClass;
	}

	/**
	 * 返回此bean定义的可解析类型。
	 * <p> 此实现委托给 {@link #getBeanClass()}。
	 *
	 * @since 5.2
	 */
	@Override
	public ResolvableType getResolvableType() {
		//如果bean类为Class类型，返回可解析类型，否则返回NONE实例。
		return (hasBeanClass() ? ResolvableType.forClass(getBeanClass()) : ResolvableType.NONE);
	}

	/**
	 * 设置bean的目标作用域的名称。
	 * <p> 默认值为单例状态，尽管仅在包含工厂中的bean定义变为活动状态后才应用此状态。
	 * bean定义最终可能会从父bean定义继承其作用域。
	 * 因此，默认作用域名称是一个空字符串 (即 {@code ""})，在设置已解析作用域之前，假定为单例状态。
	 *
	 * @see #SCOPE_SINGLETON
	 * @see #SCOPE_PROTOTYPE
	 */
	@Override
	public void setScope(@Nullable String scope) {
		this.scope = scope;
	}

	/**
	 * 返回bean的目标作用域的名称。
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
	 * 返回此是否为 <b> 原型 <b>，并为每个调用返回一个独立的实例。
	 *
	 * @see #SCOPE_PROTOTYPE
	 */
	@Override
	public boolean isPrototype() {
		return SCOPE_PROTOTYPE.equals(this.scope);
	}

	/**
	 * 设置此bean是否为 “抽象”，即不意味着实例化自己，而只是充当具体子bean定义的父级。
	 * <p> 默认值为 “false”。指定true以告诉bean工厂在任何情况下，都不要尝试实例化该特定bean。
	 */
	public void setAbstract(boolean abstractFlag) {
		this.abstractFlag = abstractFlag;
	}

	/**
	 * 返回此bean是否为 “抽象”，即不意味着实例化本身，而是仅充当具体子bean定义的父级。
	 */
	@Override
	public boolean isAbstract() {
		return this.abstractFlag;
	}

	/**
	 * 设置这个bean是否应该被懒惰地初始化。
	 * <p> 如果 {@code false}，则bean将通过执行早期初始化单例的bean工厂，在启动时实例化。
	 */
	@Override
	public void setLazyInit(boolean lazyInit) {
		this.lazyInit = lazyInit;
	}

	/**
	 * 返回这个bean是否应该被懒惰地初始化，即在启动时不急切地实例化。仅适用于单例bean。
	 *
	 * @return 是否应用lazy-init语义 (默认为 {@code false})
	 */
	@Override
	public boolean isLazyInit() {
		return (this.lazyInit != null && this.lazyInit.booleanValue());
	}

	/**
	 * 返回这个bean是否应该被懒惰地初始化，即在启动时不急切地实例化。仅适用于单例bean。
	 *
	 * @return 如果显式设置了lazy-init标志，否则为 {@code null}
	 * @since 5.2
	 */
	@Nullable
	public Boolean getLazyInit() {
		return this.lazyInit;
	}

	/**
	 * 设置自动装配模式。
	 * 这决定了是否会发生任何自动检测和设置bean引用。
	 * 默认为AUTOWIRE_NO，这意味着不会按名称或类型进行基于约定的自动装配 (但是，可能仍然存在显式注释驱动的自动装配)。
	 *
	 * @param autowireMode 要设置的自动装配模式。必须是该类中定义的常量之一。
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
	 * 返回bean定义中指定的自动装配模式。
	 */
	public int getAutowireMode() {
		return this.autowireMode;
	}

	/**
	 * 返回已解决的自动装配 值 (将AUTOWIRE_AUTODETECT解析为AUTOWIRE_CONSTRUCTOR或AUTOWIRE_BY_TYPE)。
	 *
	 * @see #AUTOWIRE_AUTODETECT
	 * @see #AUTOWIRE_CONSTRUCTOR
	 * @see #AUTOWIRE_BY_TYPE
	 */
	public int getResolvedAutowireMode() {
		if (this.autowireMode == AUTOWIRE_AUTODETECT) {
			//如果当前装配模式为自动检测
			//确定是应用 设值自动装配 还是 构造器自动装配。
			// 如果它具有无参构造函数，则将其视为设值自动装配，否则我们将尝试构造函数自动装配。
			//获取构造函数列表
			Constructor<?>[] constructors = getBeanClass().getConstructors();
			for (Constructor<?> constructor : constructors) {
				//遍历是否有无参构造函数，有则返回基于类型的自动装配。
				if (constructor.getParameterCount() == 0) {
					return AUTOWIRE_BY_TYPE;
				}
			}
			//否则返回基于构造函数的自动装配。
			return AUTOWIRE_CONSTRUCTOR;
		} else {
			return this.autowireMode;
		}
	}

	/**
	 * 设置依赖检查值。
	 *
	 * @param dependencyCheck 要设置的值。必须是该类中定义的四个常量之一。
	 * @see #DEPENDENCY_CHECK_NONE
	 * @see #DEPENDENCY_CHECK_OBJECTS
	 * @see #DEPENDENCY_CHECK_SIMPLE
	 * @see #DEPENDENCY_CHECK_ALL
	 */
	public void setDependencyCheck(int dependencyCheck) {
		this.dependencyCheck = dependencyCheck;
	}

	/**
	 * 返回依赖检查值。
	 */
	public int getDependencyCheck() {
		return this.dependencyCheck;
	}

	/**
	 * 设置此bean依赖于被初始化的bean的名称。
	 * bean工厂将保证这些bean首先得到初始化。
	 * <p> 请注意，依赖关系通常通过bean属性或构造函数参数来表示。
	 * 此属性对于其他类型的依赖项 (例如静态 (ugh) 或启动时的数据库准备) 应该是必需的。
	 */
	@Override
	public void setDependsOn(@Nullable String... dependsOn) {
		this.dependsOn = dependsOn;
	}

	/**
	 * 返回此bean依赖的bean名称。
	 */
	@Override
	@Nullable
	public String[] getDependsOn() {
		return this.dependsOn;
	}

	/**
	 * 设置该bean是否是自动装配到其他bean的候选对象。
	 * <p> 请注意，此标志旨在仅影响基于类型的自动装配。
	 * 它不会影响按名称进行的显式引用，即使指定的bean未标记为自动装配候选者，该名称也会得到解决。
	 * 因此，如果名称匹配，则按名称进行自动查询仍将注入一个bean。
	 *
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_BY_NAME
	 */
	@Override
	public void setAutowireCandidate(boolean autowireCandidate) {
		this.autowireCandidate = autowireCandidate;
	}

	/**
	 * 返回此bean是否是自动装配到其他bean的候选对象。
	 */
	@Override
	public boolean isAutowireCandidate() {
		return this.autowireCandidate;
	}

	/**
	 * 设置此bean是否是主要的自动装配候选者。
	 * <p> 如果此值是多个匹配候选对象中恰好一个bean的 { code true}，则它将成为决胜局。
	 */
	@Override
	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	/**
	 * 返回此bean是否是主要的自动装配候选者。
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
	 * 返回映射到提供的类型名称的限定符。
	 */
	@Nullable
	public AutowireCandidateQualifier getQualifier(String typeName) {
		return this.qualifiers.get(typeName);
	}

	/**
	 * 返回所有已注册的限定符。
	 *
	 * @return {@link AutowireCandidateQualifier} 对象的集合。
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
	 * 指定用于创建bean实例的回调，作为声明式指定的工厂方法的替代方法。
	 * <p> 如果设置了这样的回调，它将覆盖任何其他构造函数或工厂方法元数据。
	 * 但是，bean属性填充和潜在的注解驱动注入仍将照常适用。
	 *
	 * @see #setConstructorArgumentValues(ConstructorArgumentValues)
	 * @see #setPropertyValues(MutablePropertyValues)
	 * @since 5.0
	 */
	public void setInstanceSupplier(@Nullable Supplier<?> instanceSupplier) {
		this.instanceSupplier = instanceSupplier;
	}

	/**
	 * 返回用于创建bean实例的回调 (如果有)。
	 *
	 * @since 5.0
	 */
	@Nullable
	public Supplier<?> getInstanceSupplier() {
		return this.instanceSupplier;
	}

	/**
	 * 指定是否允许访问非公共构造函数和方法，对于外部化元数据指向这些构造函数和方法的情况。
	 * 默认值为 {@code true}; 将其切换到 {@code false} 仅用于公共访问。
	 * <p> 这适用于构造函数解析，工厂方法解析以及初始化或销毁方法。
	 * Bean属性访问器在任何情况下都必须是公开的，并且不受此设置的影响。
	 * <p> 请注意，注释驱动的配置仍将访问已注释的非公共成员。
	 * 此设置仅适用于此bean定义中的外部化元数据。
	 */
	public void setNonPublicAccessAllowed(boolean nonPublicAccessAllowed) {
		this.nonPublicAccessAllowed = nonPublicAccessAllowed;
	}

	/**
	 * 返回是否允许访问非公共构造函数和方法。
	 */
	public boolean isNonPublicAccessAllowed() {
		return this.nonPublicAccessAllowed;
	}

	/**
	 * 指定是在宽松模式下解析构造函数 ({@code true}，这是默认值) 还是切换到严格解决
	 * (在转换参数时所有匹配的不明确构造函数的情况下引发异常，而宽松模式将使用具有 “最接近” 类型匹配的类型)。
	 */
	public void setLenientConstructorResolution(boolean lenientConstructorResolution) {
		this.lenientConstructorResolution = lenientConstructorResolution;
	}

	/**
	 * 返回是在宽模式还是在严格模式下解析构造函数。
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
	 * 返回工厂bean名称 (如果有)。
	 */
	@Override
	@Nullable
	public String getFactoryBeanName() {
		return this.factoryBeanName;
	}

	/**
	 * 指定工厂方法 (如果有)。
	 * 此方法将使用构造函数参数调用，如果未指定任何参数，则不使用任何参数调用。
	 * 该方法将在指定的工厂bean (如果有的话) 上调用，或者作为本地bean类上的静态方法调用。
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
	 * 为此bean指定构造函数参数值。
	 */
	public void setConstructorArgumentValues(ConstructorArgumentValues constructorArgumentValues) {
		this.constructorArgumentValues = constructorArgumentValues;
	}

	/**
	 * 返回此bean的构造函数参数值 (从不 {@code null})。
	 */
	@Override
	public ConstructorArgumentValues getConstructorArgumentValues() {
		if (this.constructorArgumentValues == null) {
			this.constructorArgumentValues = new ConstructorArgumentValues();
		}
		return this.constructorArgumentValues;
	}

	/**
	 * 如果为此bean定义了构造函数参数值，则返回true。
	 */
	@Override
	public boolean hasConstructorArgumentValues() {
		return (this.constructorArgumentValues != null && !this.constructorArgumentValues.isEmpty());
	}

	/**
	 * 指定此bean的属性值 (如果有)。
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
	 * 为bean指定方法重写 (如果有)。
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
	 * 设置 初始化程序 方法的名称。
	 * <p> 默认值为 {@code null}，在这种情况下，没有初始化方法。
	 */
	@Override
	public void setInitMethodName(@Nullable String initMethodName) {
		this.initMethodName = initMethodName;
	}

	/**
	 * 返回 初始化程序 方法的名称。
	 */
	@Override
	@Nullable
	public String getInitMethodName() {
		return this.initMethodName;
	}

	/**
	 * 指定所配置的 初始化程序 方法是否为默认值。
	 * <p> 对于本地指定的初始化方法，默认值为 {@code true}，但对于默认部分中的共享设置，默认值为 {@code false}
	 * (例如 在XML中 {@code bean init-method} 与 {@code bean default-init-method} 级别 )，可能不适用于所有包含的bean定义。
	 *
	 * @see #setInitMethodName
	 * @see #applyDefaults
	 */
	public void setEnforceInitMethod(boolean enforceInitMethod) {
		this.enforceInitMethod = enforceInitMethod;
	}

	/**
	 * 指示配置的 初始化程序 方法是否为默认值。
	 *
	 * @see #getInitMethodName()
	 */
	public boolean isEnforceInitMethod() {
		return this.enforceInitMethod;
	}

	/**
	 * 设置 销毁 方法的名称。
	 * <p> 默认值为 {@code null}，在这种情况下，没有销毁方法。
	 */
	@Override
	public void setDestroyMethodName(@Nullable String destroyMethodName) {
		this.destroyMethodName = destroyMethodName;
	}

	/**
	 * 返回 销毁 方法的名称。
	 */
	@Override
	@Nullable
	public String getDestroyMethodName() {
		return this.destroyMethodName;
	}

	/**
	 * 指定配置的销毁方法是否为默认方法。
	 * <p> 对于本地指定的 销毁 方法，默认值为 {@code true}，但对于默认值部分中的共享设置，默认值为 {@code false}
	 * (例如XML中 {@code bean destroy-method} 与 {@code beans default-destroy-method} 级别 )，可能不适用于所有包含的bean定义。
	 *
	 * @see #setDestroyMethodName
	 * @see #applyDefaults
	 */
	public void setEnforceDestroyMethod(boolean enforceDestroyMethod) {
		this.enforceDestroyMethod = enforceDestroyMethod;
	}

	/**
	 * 指示配置的销毁 方法是否为默认方法。
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
	 * 为此 {@code BeanDefinition} 设置角色提示。
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
	 * 设置此bean定义的人类可读描述。
	 */
	@Override
	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	/**
	 * 返回这个bean定义的人类可读的描述。
	 */
	@Override
	@Nullable
	public String getDescription() {
		return this.description;
	}

	/**
	 * 设置此bean定义来自的资源 (用于在出现错误时显示上下文)。
	 */
	public void setResource(@Nullable Resource resource) {
		this.resource = resource;
	}

	/**
	 * 返回此bean定义来自的资源。
	 */
	@Nullable
	public Resource getResource() {
		return this.resource;
	}

	/**
	 * 设置此bean定义来自的资源的描述 (用于在出现错误时显示上下文)。
	 */
	public void setResourceDescription(@Nullable String resourceDescription) {
		this.resource = (resourceDescription != null ? new DescriptiveResource(resourceDescription) : null);
	}

	/**
	 * 返回此bean定义来自的资源的描述 (用于在出现错误时显示上下文)。
	 */
	@Override
	@Nullable
	public String getResourceDescription() {
		return (this.resource != null ? this.resource.getDescription() : null);
	}

	/**
	 * 设置原始 (例如装饰) Bean定义 (如果有)。
	 */
	public void setOriginatingBeanDefinition(BeanDefinition originatingBd) {
		this.resource = new BeanDefinitionResource(originatingBd);
	}

	/**
	 * 返回原始BeanDefinition，如果没有，则返回 {@code null}。
	 * 允许检索装饰的bean定义 (如果有)。
	 * <p> 请注意，此方法返回直接发起者。
	 * 遍历发起者链以找到用户定义的原始BeanDefinition。
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
	 * 对象的 {@code clone()} 方法的公开声明。委托给 {@link #cloneBeanDefinition()}。
	 *
	 * @see Object#clone()
	 */
	@Override
	public Object clone() {
		return cloneBeanDefinition();
	}

	/**
	 * 克隆此bean定义。由具体子类实现。
	 *
	 * @return 克隆的bean定义对象
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
