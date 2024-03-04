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

import org.springframework.beans.*;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.*;
import org.springframework.core.AttributeAccessor;
import org.springframework.core.DecoratingClassLoader;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.log.LogMessage;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import java.beans.PropertyEditor;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * {@link org.springframework.beans.factory.BeanFactory} 实现的抽象基类，提供了
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory} SPI 的全部功能。
 * 不假设为可列出的 Bean 工厂：因此也可以用作从某些后端资源获取 Bean 定义的 Bean 工厂实现的基类
 * （其中 Bean 定义访问是一项昂贵的操作）。
 *
 * <p>此类提供了单例缓存（通过其基类 {@link org.springframework.beans.factory.support.DefaultSingletonBeanRegistry}），
 * 单例/原型确定，{@link org.springframework.beans.factory.FactoryBean} 处理、别名、
 * 子 Bean 定义的 Bean 定义合并和 Bean 销毁（{@link org.springframework.beans.factory.DisposableBean} 接口、自定义销毁方法）。
 * 此外，它还可以通过实现 {@link org.springframework.beans.factory.HierarchicalBeanFactory} 接口来管理 Bean 工厂层次结构
 * （在未知 Bean 的情况下委托给父 Bean 工厂）。
 *
 * <p>子类要实现的主要模板方法是 {@link #getBeanDefinition} 和 {@link #createBean}，
 * 分别为给定的 Bean 名称检索 Bean 定义和为给定的 Bean 定义创建 Bean 实例。
 * 这些操作的默认实现可以在 {@link DefaultListableBeanFactory} 和
 * {@link AbstractAutowireCapableBeanFactory} 中找到。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Costin Leau
 * @author Chris Beams
 * @author Phillip Webb
 * @see #getBeanDefinition
 * @see #createBean
 * @see AbstractAutowireCapableBeanFactory#createBean
 * @see DefaultListableBeanFactory#getBeanDefinition
 * @since 15 April 2001
 */
public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport implements ConfigurableBeanFactory {

	/**
	 * 父bean工厂，用于支持继承bean
	 */
	@Nullable
	private BeanFactory parentBeanFactory;

	/**
	 * 类加载器来解析bean类名，如有必要。
	 */
	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	/**
	 * 类加载器临时解析bean类名，如有必要。
	 */
	@Nullable
	private ClassLoader tempClassLoader;

	/**
	 * 是缓存bean元数据，还是为每次访问重新获取它。
	 */
	private boolean cacheBeanMetadata = true;

	/**
	 * bean定义值中表达式的解析策略
	 */
	@Nullable
	private BeanExpressionResolver beanExpressionResolver;

	/**
	 * 类型转换器
	 * Spring 使用 ConversionService而不是PropertyEditors。
	 */
	@Nullable
	private ConversionService conversionService;

	/**
	 * 属性编辑器
	 * 自定义 PropertyEditorRegistrars ，应用于此工厂的bean。
	 */
	private final Set<PropertyEditorRegistrar> propertyEditorRegistrars = new LinkedHashSet<>(4);

	/**
	 * 类的属性编辑器
	 * 自定义属性编辑器PropertyEditors，应用于此工厂的bean。
	 */
	private final Map<Class<?>, Class<? extends PropertyEditor>> customEditors = new HashMap<>(4);

	/**
	 * 类型转换器
	 * 要使用的自定义类型转换程序，覆盖默认的PropertyEditor机制。
	 */
	@Nullable
	private TypeConverter typeConverter;

	/**
	 * 为嵌入的值(如注释属性)添加字符串解析器
	 * 要应用于例如注释属性值的字符串解析器。
	 */
	private final List<StringValueResolver> embeddedValueResolvers = new CopyOnWriteArrayList<>();

	/**
	 * 要应用的bean后置处理器
	 */
	private final List<BeanPostProcessor> beanPostProcessors = new BeanPostProcessorCacheAwareList();

	/**
	 * 预过滤后处理器的缓存。
	 */
	@Nullable
	private volatile BeanPostProcessorCache beanPostProcessorCache;

	/**
	 * 从范围标识符字符串映射到相应的范围。
	 */
	private final Map<String, Scope> scopes = new LinkedHashMap<>(8);

	/**
	 * 使用SecurityManager运行时使用的安全上下文。
	 */
	@Nullable
	private SecurityContextProvider securityContextProvider;

	/**
	 * 从bean名称到合并的RootBeanDefinition的Map
	 */
	private final Map<String, RootBeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<>(256);

	/**
	 * 已经创建至少一次的bean名称。
	 */
	private final Set<String> alreadyCreated = Collections.newSetFromMap(new ConcurrentHashMap<>(256));

	/**
	 * 当前正在创建的bean的名称。
	 */
	private final ThreadLocal<Object> prototypesCurrentlyInCreation =
			new NamedThreadLocal<>("Prototype beans currently in creation");

	/**
	 * 应用程序启动指标
	 **/
	private ApplicationStartup applicationStartup = ApplicationStartup.DEFAULT;

	/**
	 * 创建一个新的AbstractBeanFactory。
	 */
	public AbstractBeanFactory() {
	}

	/**
	 * 使用给定的父级创建一个新的AbstractBeanFactory。
	 *
	 * @param parentBeanFactory 父bean工厂，如果没有，则为 {@code null}
	 * @see #getBean
	 */
	public AbstractBeanFactory(@Nullable BeanFactory parentBeanFactory) {
		this.parentBeanFactory = parentBeanFactory;
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public Object getBean(String name) throws BeansException {
		return doGetBean(name, null, null, false);
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		return doGetBean(name, requiredType, null, false);
	}

	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		return doGetBean(name, null, args, false);
	}

	/**
	 * 返回指定bean的实例，该实例可能是共享的或独立的。
	 *
	 * @param name         要检索的bean名称
	 * @param requiredType 要检索的bean的所需类型
	 * @param args         使用显式参数创建bean实例时要使用的参数 (仅在创建新实例而不是检索现有实例时应用)
	 * @return bean的实例
	 * @throws BeansException 如果无法创建bean
	 */
	public <T> T getBean(String name, @Nullable Class<T> requiredType, @Nullable Object... args)
			throws BeansException {

		return doGetBean(name, requiredType, args, false);
	}

	/**
	 * 返回指定bean的实例，该实例可能是共享的或独立的。
	 *
	 * @param name          要检索的bean的名称
	 * @param requiredType  要检索的bean的所需类型
	 * @param args          使用显式参数创建bean实例时要使用的参数 (仅在创建新实例而不是检索现有实例时应用)
	 * @param typeCheckOnly 获取实例是否用于类型检查，而不是用于实际使用
	 * @return bean的实例
	 * @throws BeansException 如果无法创建bean
	 */
	@SuppressWarnings("unchecked")
	protected <T> T doGetBean(
			String name, @Nullable Class<T> requiredType, @Nullable Object[] args, boolean typeCheckOnly)
			throws BeansException {
		//返回规范的bean名称
		String beanName = transformedBeanName(name);
		Object beanInstance;

		// 立即检查单例缓存是否有手动注册的单例。
		Object sharedInstance = getSingleton(beanName);
		if (sharedInstance != null && args == null) {
			//如果共享实例不为空，且参数为空
			if (logger.isTraceEnabled()) {
				if (isSingletonCurrentlyInCreation(beanName)) {
					logger.trace("Returning eagerly cached instance of singleton bean '" + beanName +
							"' that is not fully initialized yet - a consequence of a circular reference");
				} else {
					logger.trace("Returning cached instance of singleton bean '" + beanName + "'");
				}
			}
			//完成FactoryBean的相关处理，并用来获取FactoryBean的处理结果。
			beanInstance = getObjectForBeanInstance(sharedInstance, name, beanName, null);
		} else {
			//如果我们已经在创建此bean实例，则失败: 假设我们在一个循环引用中。
			if (isPrototypeCurrentlyInCreation(beanName)) {
				//如果bean名称对应的原型bean当前正在创建中，则抛出异常
				throw new BeanCurrentlyInCreationException(beanName);
			}

			// 检查此工厂中是否存在bean定义。
			//获取父bean工厂
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
				//如果父bean工厂不为空，且bean工厂中没有该bean名称的bean定义。
				// 找不到-> 检查父级。
				//获取原始bean名称
				String nameToLookup = originalBeanName(name);
				if (parentBeanFactory instanceof AbstractBeanFactory) {
					//如果父bean工厂是AbstractBeanFactory类型，递归调用，获取nameToLookup对应的bean实例
					return ((AbstractBeanFactory) parentBeanFactory).doGetBean(
							nameToLookup, requiredType, args, typeCheckOnly);
				} else if (args != null) {
					// 用明确的args委托给父bean工厂。
					//根据父bean工厂获取bean名称和参数对应的bean实例
					return (T) parentBeanFactory.getBean(nameToLookup, args);
				} else if (requiredType != null) {
					// 没有args -> 委托到标准getBean方法。
					//根据父bean工厂获取bean 名称和类型对应的bean实例
					return parentBeanFactory.getBean(nameToLookup, requiredType);
				} else {
					//根据父bean工厂获取bean 名称对应的bean实例
					return (T) parentBeanFactory.getBean(nameToLookup);
				}
			}

			if (!typeCheckOnly) {
				//如果不仅仅进行检查类型，将bean名称标记为已创建
				markBeanAsCreated(beanName);
			}
			//设置当前bean名称的启动步骤-bean初始化阶段
			StartupStep beanCreation = this.applicationStartup.start("spring.beans.instantiate")
					.tag("beanName", name);
			try {
				//如果传入的类型不为空，bean初始化的启动步骤，添加该bean类型信息
				if (requiredType != null) {
					beanCreation.tag("beanType", requiredType::toString);
				}
				//获取合并bean定义，即RootBeanDefinition实例
				RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
				//检查合并的bean定义
				checkMergedBeanDefinition(mbd, beanName, args);

				// 保证当前bean所依赖的bean的初始化。
				String[] dependsOn = mbd.getDependsOn();
				if (dependsOn != null) {
					for (String dep : dependsOn) {
						if (isDependent(beanName, dep)) {
							//如果该bean名称依赖于被依赖的bean名称，则抛出异常
							throw new BeanCreationException(mbd.getResourceDescription(), beanName,
									"Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
						}
						//注册依赖的bean
						registerDependentBean(dep, beanName);
						try {
							//获取依赖的bean
							getBean(dep);
						} catch (NoSuchBeanDefinitionException ex) {
							throw new BeanCreationException(mbd.getResourceDescription(), beanName,
									"'" + beanName + "' depends on missing bean '" + dep + "'", ex);
						}
					}
				}

				// 创建bean实例
				if (mbd.isSingleton()) {
					sharedInstance = getSingleton(beanName, () -> {
						try {
							return createBean(beanName, mbd, args);
						} catch (BeansException ex) {
							// 显式地从单例缓存中删除实例: 创建过程可能急切地将其放在那里，以允许循环引用解析。
							// 还要删除任何收到对该bean的临时引用的bean
							destroySingleton(beanName);
							throw ex;
						}
					});
					//获取指定bean实例的对象。
					beanInstance = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
				} else if (mbd.isPrototype()) {
					// 如果bean是原型bean，则创建一个新的实例
					Object prototypeInstance = null;
					try {
						//在创建原型对象之前执行的程序
						beforePrototypeCreation(beanName);
						//创建一个bean
						prototypeInstance = createBean(beanName, mbd, args);
					} finally {
						//创建原型对象之后。
						afterPrototypeCreation(beanName);
					}
					//根据原型对象获取bean实例。
					beanInstance = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
				} else {
					//其他的bean范围
					String scopeName = mbd.getScope();
					if (!StringUtils.hasLength(scopeName)) {
						//如果bean范围为空，抛出异常。
						throw new IllegalStateException("No scope name defined for bean '" + beanName + "'");
					}
					//根据范围名称获取范围对象。
					Scope scope = this.scopes.get(scopeName);
					if (scope == null) {
						//如果范围对象为空，抛出异常。
						throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
					}
					try {
						//获取范围实例。
						Object scopedInstance = scope.get(beanName, () -> {
							//在创建实例之前执行的程序
							beforePrototypeCreation(beanName);
							try {
								//根据bean名称，bean定义，参数创建一个实例。
								return createBean(beanName, mbd, args);
							} finally {
								//创建原型后执行的程序。
								afterPrototypeCreation(beanName);
							}
						});
						//根据范围实例和bean名称以及bean定义名称获取bean对象。
						beanInstance = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
					} catch (IllegalStateException ex) {
						//创建失败，抛出范围未激活异常。
						throw new ScopeNotActiveException(beanName, scopeName, ex);
					}
				}
			} catch (BeansException ex) {
				//创建实例失败，将异常信息添加到bean初始化阶段的启动步骤中
				beanCreation.tag("exception", ex.getClass().toString());
				beanCreation.tag("message", String.valueOf(ex.getMessage()));
				//将bean名称设置为还未创建
				cleanupAfterBeanCreationFailure(beanName);
				throw ex;
			} finally {
				//结束bean初始化阶段
				beanCreation.end();
			}
		}

		return adaptBeanInstance(name, beanInstance, requiredType);
	}

	@SuppressWarnings("unchecked")
	<T> T adaptBeanInstance(String name, Object bean, @Nullable Class<?> requiredType) {
		// 检查所需类型是否与实际bean实例的类型匹配。
		if (requiredType != null && !requiredType.isInstance(bean)) {
			//如果所需类型不为空，且当前bean不是所需类型的实例。
			try {
				//获取类型转换器转换类型
				Object convertedBean = getTypeConverter().convertIfNecessary(bean, requiredType);
				if (convertedBean == null) {
					//转换后的类型为空，抛出异常
					throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
				}
				return (T) convertedBean;
			} catch (TypeMismatchException ex) {
				if (logger.isTraceEnabled()) {
					logger.trace("Failed to convert bean '" + name + "' to required type '" +
							ClassUtils.getQualifiedName(requiredType) + "'", ex);
				}
				throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
			}
		}
		return (T) bean;
	}

	@Override
	public boolean containsBean(String name) {
		//转换后的BeanName
		String beanName = transformedBeanName(name);
		if (containsSingleton(beanName) || containsBeanDefinition(beanName)) {
			return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(name));
		}
		// Not found -> check parent.
		BeanFactory parentBeanFactory = getParentBeanFactory();
		return (parentBeanFactory != null && parentBeanFactory.containsBean(originalBeanName(name)));
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		// 转换 Bean 名称为规范形式
		String beanName = transformedBeanName(name);

		// 获取单例对象实例
		Object beanInstance = getSingleton(beanName, false);
		// 如果单例对象实例不为空
		if (beanInstance != null) {
			// 如果单例对象是 FactoryBean
			if (beanInstance instanceof FactoryBean) {
				// 如果是 FactoryBean 的引用或者 FactoryBean 返回的对象是单例的，则返回 true
				return (BeanFactoryUtils.isFactoryDereference(name) || ((FactoryBean<?>) beanInstance).isSingleton());
			} else {
				// 如果不是 FactoryBean 的引用，则返回 true
				return !BeanFactoryUtils.isFactoryDereference(name);
			}
		}

		// 如果没有找到单例实例，则检查 Bean 定义
		BeanFactory parentBeanFactory = getParentBeanFactory();
		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
			// 如果在当前工厂中未找到 Bean 定义，则委托给父工厂
			return parentBeanFactory.isSingleton(originalBeanName(name));
		}

		// 获取合并后的 Bean 定义
		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

		// 如果 Bean 是单例的
		if (mbd.isSingleton()) {
			// 如果是 FactoryBean
			if (isFactoryBean(beanName, mbd)) {
				// 如果是 FactoryBean 的引用，直接返回 true
				if (BeanFactoryUtils.isFactoryDereference(name)) {
					return true;
				}
				// 如果是 FactoryBean 返回的对象是单例的，则返回 true
				FactoryBean<?> factoryBean = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
				return factoryBean.isSingleton();
			} else {
				// 如果不是 FactoryBean 的引用，则返回 true
				return !BeanFactoryUtils.isFactoryDereference(name);
			}
		} else {
			// 如果 Bean 不是单例的，则返回 false
			return false;
		}
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		// 转换 Bean 名称为规范形式
		String beanName = transformedBeanName(name);

		BeanFactory parentBeanFactory = getParentBeanFactory();
		// 如果父工厂不为空且当前工厂中未找到 Bean 定义
		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
			// 委托给父工厂判断是否是原型 Bean
			return parentBeanFactory.isPrototype(originalBeanName(name));
		}

		// 获取合并后的 Bean 定义
		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
		// 如果 Bean 是原型的
		if (mbd.isPrototype()) {
			// 如果是 FactoryBean，则返回创建的对象是否是原型的
			return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(beanName, mbd));
		}

		// 单例或作用域 Bean，不是原型的
		// 但是，FactoryBean 仍然可能产生原型对象...
		// 如果是 FactoryBean 的引用，则返回 false
		if (BeanFactoryUtils.isFactoryDereference(name)) {
			return false;
		}
		// 如果是 FactoryBean
		if (isFactoryBean(beanName, mbd)) {
			FactoryBean<?> fb = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
			// 如果启用了安全管理器
			if (System.getSecurityManager() != null) {
				// 使用特权操作判断是否是原型 Bean
				return AccessController.doPrivileged(
						(PrivilegedAction<Boolean>) () ->
								((fb instanceof SmartFactoryBean && ((SmartFactoryBean<?>) fb).isPrototype()) ||
										!fb.isSingleton()),
						getAccessControlContext());
			} else {
				// 判断是否是原型 Bean
				return ((fb instanceof SmartFactoryBean && ((SmartFactoryBean<?>) fb).isPrototype()) ||
						!fb.isSingleton());
			}
		} else {
			// 如果不是 FactoryBean 的引用，则返回 false
			return false;
		}
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		return isTypeMatch(name, typeToMatch, true);
	}

	/**
	 * {@link #isTypeMatch(String, ResolvableType)} 的内部扩展变体，用于检查给定名称的 Bean 是否与指定类型匹配。
	 * 允许应用额外的约束以确保不会提前创建 Bean。
	 *
	 * @param name                 要查询的 Bean 名称
	 * @param typeToMatch          要匹配的类型（作为 {@code ResolvableType}）
	 * @param allowFactoryBeanInit 是否允许 FactoryBean 的初始化
	 * @return 如果 Bean 类型匹配，则为 {@code true}；如果不匹配或尚不能确定，则为 {@code false}
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的 Bean
	 * @see #getBean
	 * @see #getType
	 * @since 5.2
	 */
	protected boolean isTypeMatch(String name, ResolvableType typeToMatch, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException {

		// 转换 Bean 名称为规范形式
		String beanName = transformedBeanName(name);
		// 是否是工厂引用
		boolean isFactoryDereference = BeanFactoryUtils.isFactoryDereference(name);

		// 检查手动注册的单例
		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null && beanInstance.getClass() != NullBean.class) {
			if (beanInstance instanceof FactoryBean) {
				if (!isFactoryDereference) {
					// 如果不是工厂引用，则返回创建的对象是否与给定类型匹配
					Class<?> type = getTypeForFactoryBean((FactoryBean<?>) beanInstance);
					return (type != null && typeToMatch.isAssignableFrom(type));
				} else {
					// 如果是工厂引用，则直接检查对象是否与给定类型兼容
					return typeToMatch.isInstance(beanInstance);
				}
			} else if (!isFactoryDereference) {
				// 如果不是工厂引用，直接检查对象是否与给定类型匹配
				if (typeToMatch.isInstance(beanInstance)) {
					return true;
				} else if (typeToMatch.hasGenerics() && containsBeanDefinition(beanName)) {
					// 泛型匹配只可能在目标类上匹配，而不是在代理上匹配
					// 获取合并的本地 Bean 定义
					RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
					// 获取目标类型
					Class<?> targetType = mbd.getTargetType();
					// 如果目标类型不为空且不是 beanInstance 的用户类
					if (targetType != null && targetType != ClassUtils.getUserClass(beanInstance)) {
						// 解析给定类型
						Class<?> classToMatch = typeToMatch.resolve();
						// 如果解析后的类型不为空且不是 beanInstance 的实例，则返回 false
						if (classToMatch != null && !classToMatch.isInstance(beanInstance)) {
							return false;
						}
						// 如果给定类型可以从目标类型分配，则返回 true
						if (typeToMatch.isAssignableFrom(targetType)) {
							return true;
						}
					}
					// 获取可解析类型
					ResolvableType resolvableType = mbd.targetType;
					// 如果可解析类型为空，则尝试使用工厂方法返回类型
					if (resolvableType == null) {
						resolvableType = mbd.factoryMethodReturnType;
					}
					// 返回给定类型是否可以从可解析类型分配
					return (resolvableType != null && typeToMatch.isAssignableFrom(resolvableType));
				}
			}
			return false;
		} else if (containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
			// 已注册但为空的单例实例
			return false;
		}

		// 未找到单例实例 -> 检查 Bean 定义。
		BeanFactory parentBeanFactory = getParentBeanFactory();
		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
			// 在此工厂中未找到 Bean 定义 -> 委托给父工厂。
			return parentBeanFactory.isTypeMatch(originalBeanName(name), typeToMatch);
		}

		// 获取对应的 Bean 定义。
		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
		BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();

		// 设置要匹配的类型
		Class<?> classToMatch = typeToMatch.resolve();
		if (classToMatch == null) {
			classToMatch = FactoryBean.class;
		}
		Class<?>[] typesToMatch = (FactoryBean.class == classToMatch ?
				new Class<?>[]{classToMatch} : new Class<?>[]{FactoryBean.class, classToMatch});

		// 尝试预测 Bean 的类型
		Class<?> predictedType = null;

		// 如果是非工厂引用且是装饰的 Bean 定义，则返回与 FactoryBean 返回类型相同的目标 Bean 类型
		if (!isFactoryDereference && dbd != null && isFactoryBean(beanName, mbd)) {
			// 只有在用户明确设置了延迟初始化为 true 且我们知道合并的 Bean 定义是 FactoryBean 时才尝试
			// 如果不是延迟初始化或允许工厂Bean初始化
			if (!mbd.isLazyInit() || allowFactoryBeanInit) {
				// 获取合并的装饰 Bean 定义
				RootBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
				// 预测 Bean 类型
				Class<?> targetType = predictBeanType(dbd.getBeanName(), tbd, typesToMatch);
				// 如果目标类型不为空且不是 FactoryBean 类型，则将预测的类型设置为目标类型
				if (targetType != null && !FactoryBean.class.isAssignableFrom(targetType)) {
					predictedType = targetType;
				}
			}
		}

		// 如果无法使用目标类型，请尝试正常预测。
		if (predictedType == null) {
			predictedType = predictBeanType(beanName, mbd, typesToMatch);
			if (predictedType == null) {
				return false;
			}
		}

		// 尝试获取实际的 ResolvableType
		ResolvableType beanType = null;

		// 如果是 FactoryBean，则使用它创建的对象而不是工厂类本身。
		if (FactoryBean.class.isAssignableFrom(predictedType)) {
			if (beanInstance == null && !isFactoryDereference) {
				beanType = getTypeForFactoryBean(beanName, mbd, allowFactoryBeanInit);
				predictedType = beanType.resolve();
				if (predictedType == null) {
					return false;
				}
			}
		} else if (isFactoryDereference) {
			// 特殊情况：SmartInstantiationAwareBeanPostProcessor 返回了非 FactoryBean 类型，
			// 但我们仍然要求解引用 FactoryBean...
			// 让我们检查原始 Bean 类并在它是 FactoryBean 时继续。
			predictedType = predictBeanType(beanName, mbd, FactoryBean.class);
			if (predictedType == null || !FactoryBean.class.isAssignableFrom(predictedType)) {
				return false;
			}
		}

		// 如果没有确切的类型，则回退到预测的类型
		if (beanType == null) {
			ResolvableType definedType = mbd.targetType;
			if (definedType == null) {
				definedType = mbd.factoryMethodReturnType;
			}
			if (definedType != null && definedType.resolve() == predictedType) {
				beanType = definedType;
			}
		}

		// 如果我们有 Bean 类型，则使用它以便考虑泛型
		if (beanType != null) {
			return typeToMatch.isAssignableFrom(beanType);
		}

		// 如果我们没有 Bean 类型，则回退到预测的类型
		return typeToMatch.isAssignableFrom(predictedType);
	}

	@Override
	public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		return isTypeMatch(name, ResolvableType.forRawClass(typeToMatch));
	}

	@Override
	@Nullable
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		return getType(name, true);
	}

	@Override
	@Nullable
	public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
		//获取规范的bean名称
		String beanName = transformedBeanName(name);

		// 检查手动注册的单例对象。
		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null && beanInstance.getClass() != NullBean.class) {
			//如果该单例对象不为空，且该单例对象的类型不是NullBean类
			if (beanInstance instanceof FactoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
				//如果该单例对象是FactoryBean的子类，且该bean名称不是bean工厂的取消引用，
				// 调用getTypeForFactoryBean方法获取bean实例。
				return getTypeForFactoryBean((FactoryBean<?>) beanInstance);
			} else {
				return beanInstance.getClass();
			}
		}

		//找不到单例实例-> 检查bean定义。
		//获取父bean工厂
		BeanFactory parentBeanFactory = getParentBeanFactory();
		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
			//如果父bean工厂不为空，且bean工厂中不包含此名称的bean定义
			//在此工厂中找不到bean定义-> 委托给父级。
			return parentBeanFactory.getType(originalBeanName(name));
		}

		// 根据bean名称获取合并的本地bean定义
		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

		// 检查装饰的bean定义 (如果有的话): 我们假设确定装饰的bean的类型比代理的类型更容易。
		// 获取装饰定义
		BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
		if (dbd != null && !BeanFactoryUtils.isFactoryDereference(name)) {
			//如果装饰bean定义存在，且该名称不是bean工厂的取消引用
			//获取该装饰bean定义的根合并bean定义
			RootBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
			//根据装饰bean定义的bean名称，预测bean类型
			Class<?> targetClass = predictBeanType(dbd.getBeanName(), tbd);
			if (targetClass != null && !FactoryBean.class.isAssignableFrom(targetClass)) {
				//目标类型不为空，且该类型不是FactoryBean接口的子类，返回该目标类型
				return targetClass;
			}
		}
		//根据bean名称和本地bean定义预测bean类型
		Class<?> beanClass = predictBeanType(beanName, mbd);

		// 检查bean类我们是否在处理工厂bean。
		if (beanClass != null && FactoryBean.class.isAssignableFrom(beanClass)) {
			//如果bean类不为空，且该bean类为FactoryBean的子类
			if (BeanFactoryUtils.isFactoryDereference(name)) {
				// 如果该名称是工厂取消引用返回该类型。
				return beanClass;
			} else {
				//如果它是FactoryBean，我们想看看它创建了什么，而不是工厂类。
				return getTypeForFactoryBean(beanName, mbd, allowFactoryBeanInit).resolve();
			}
		} else if (BeanFactoryUtils.isFactoryDereference(name)) {
			// 如果该名称是工厂取消引用返回null
			return null;
		} else {
			//否则返回该类型
			return beanClass;
		}
	}

	@Override
	public String[] getAliases(String name) {
		// 转换 Bean 名称
		String beanName = transformedBeanName(name);
		// 初始化别名列表
		List<String> aliases = new ArrayList<>();
		// 检查是否具有工厂前缀
		boolean factoryPrefix = name.startsWith(FACTORY_BEAN_PREFIX);
		// 获取完整的 Bean 名称
		String fullBeanName = beanName;
		if (factoryPrefix) {
			fullBeanName = FACTORY_BEAN_PREFIX + beanName;
		}
		// 添加完整 Bean 名称到别名列表
		if (!fullBeanName.equals(name)) {
			aliases.add(fullBeanName);
		}
		// 获取父类工厂中的别名
		String[] retrievedAliases = super.getAliases(beanName);
		String prefix = factoryPrefix ? FACTORY_BEAN_PREFIX : "";
		// 将父类工厂中的别名添加到别名列表中
		for (String retrievedAlias : retrievedAliases) {
			String alias = prefix + retrievedAlias;
			if (!alias.equals(name)) {
				aliases.add(alias);
			}
		}
		// 如果不包含单例 Bean 和 Bean 定义，则从父类工厂获取别名
		if (!containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null) {
				aliases.addAll(Arrays.asList(parentBeanFactory.getAliases(fullBeanName)));
			}
		}
		// 将别名列表转换为字符串数组并返回
		return StringUtils.toStringArray(aliases);
	}


	//---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	@Nullable
	public BeanFactory getParentBeanFactory() {
		return this.parentBeanFactory;
	}

	@Override
	public boolean containsLocalBean(String name) {
		String beanName = transformedBeanName(name);
		return ((containsSingleton(beanName) || containsBeanDefinition(beanName)) &&
				(!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(beanName)));
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public void setParentBeanFactory(@Nullable BeanFactory parentBeanFactory) {
		if (this.parentBeanFactory != null && this.parentBeanFactory != parentBeanFactory) {
			throw new IllegalStateException("Already associated with parent BeanFactory: " + this.parentBeanFactory);
		}
		if (this == parentBeanFactory) {
			throw new IllegalStateException("Cannot set parent bean factory to self");
		}
		this.parentBeanFactory = parentBeanFactory;
	}

	@Override
	public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
		this.beanClassLoader = (beanClassLoader != null ? beanClassLoader : ClassUtils.getDefaultClassLoader());
	}

	@Override
	@Nullable
	public ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	@Override
	public void setTempClassLoader(@Nullable ClassLoader tempClassLoader) {
		this.tempClassLoader = tempClassLoader;
	}

	@Override
	@Nullable
	public ClassLoader getTempClassLoader() {
		return this.tempClassLoader;
	}

	@Override
	public void setCacheBeanMetadata(boolean cacheBeanMetadata) {
		this.cacheBeanMetadata = cacheBeanMetadata;
	}

	@Override
	public boolean isCacheBeanMetadata() {
		return this.cacheBeanMetadata;
	}

	@Override
	public void setBeanExpressionResolver(@Nullable BeanExpressionResolver resolver) {
		this.beanExpressionResolver = resolver;
	}

	@Override
	@Nullable
	public BeanExpressionResolver getBeanExpressionResolver() {
		return this.beanExpressionResolver;
	}

	@Override
	public void setConversionService(@Nullable ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	@Nullable
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	@Override
	public void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar) {
		Assert.notNull(registrar, "PropertyEditorRegistrar must not be null");
		this.propertyEditorRegistrars.add(registrar);
	}

	/**
	 * 返回 属性编辑器 的集合。
	 *
	 * @return 属性编辑器 的集合
	 */
	public Set<PropertyEditorRegistrar> getPropertyEditorRegistrars() {
		return this.propertyEditorRegistrars;
	}

	@Override
	public void registerCustomEditor(Class<?> requiredType, Class<? extends PropertyEditor> propertyEditorClass) {
		Assert.notNull(requiredType, "Required type must not be null");
		Assert.notNull(propertyEditorClass, "PropertyEditor class must not be null");
		this.customEditors.put(requiredType, propertyEditorClass);
	}

	@Override
	public void copyRegisteredEditorsTo(PropertyEditorRegistry registry) {
		registerCustomEditors(registry);
	}

	/**
	 * 返回自定义编辑器的映射，其中键是类，值是 PropertyEditor 类。
	 *
	 * @return 自定义编辑器的映射
	 */
	public Map<Class<?>, Class<? extends PropertyEditor>> getCustomEditors() {
		return this.customEditors;
	}

	@Override
	public void setTypeConverter(TypeConverter typeConverter) {
		this.typeConverter = typeConverter;
	}

	/**
	 * 返回要使用的自定义 TypeConverter（如果有）。
	 *
	 * @return 自定义 TypeConverter，如果未指定则返回 {@code null}
	 */
	@Nullable
	protected TypeConverter getCustomTypeConverter() {
		return this.typeConverter;
	}

	@Override
	public TypeConverter getTypeConverter() {
		// 获取自定义类型转换器
		TypeConverter customConverter = getCustomTypeConverter();
		if (customConverter != null) {
			// 如果存在自定义类型转换器，则直接返回
			return customConverter;
		} else {
			// 构建默认的类型转换器，并注册自定义编辑器
			SimpleTypeConverter typeConverter = new SimpleTypeConverter();
			typeConverter.setConversionService(getConversionService());
			registerCustomEditors(typeConverter);
			return typeConverter;
		}
	}

	@Override
	public void addEmbeddedValueResolver(StringValueResolver valueResolver) {
		Assert.notNull(valueResolver, "StringValueResolver must not be null");
		this.embeddedValueResolvers.add(valueResolver);
	}

	@Override
	public boolean hasEmbeddedValueResolver() {
		return !this.embeddedValueResolvers.isEmpty();
	}

	@Override
	@Nullable
	public String resolveEmbeddedValue(@Nullable String value) {
		if (value == null) {
			// 如果值为null，则直接返回null
			return null;
		}
		String result = value;
		for (StringValueResolver resolver : this.embeddedValueResolvers) {
			// 遍历嵌入式值解析器列表，依次解析属性值
			result = resolver.resolveStringValue(result);
			if (result == null) {
				// 如果解析结果为null，则直接返回null
				return null;
			}
		}
		return result;
	}

	@Override
	public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
		Assert.notNull(beanPostProcessor, "BeanPostProcessor must not be null");
		// 从原来的位置去除，添加到列表的末尾
		this.beanPostProcessors.remove(beanPostProcessor);
		// 添加到列表的末尾
		this.beanPostProcessors.add(beanPostProcessor);
	}

	/**
	 * 添加新的BeanPostProcessor，它将应用于由该工厂创建的bean。
	 * 在工厂配置期间调用。
	 *
	 * @param beanPostProcessors 要添加的BeanPostProcessor集合
	 * @see #addBeanPostProcessor
	 * @since 5.3
	 */
	public void addBeanPostProcessors(Collection<? extends BeanPostProcessor> beanPostProcessors) {
		this.beanPostProcessors.removeAll(beanPostProcessors);
		this.beanPostProcessors.addAll(beanPostProcessors);
	}

	@Override
	public int getBeanPostProcessorCount() {
		return this.beanPostProcessors.size();
	}

	/**
	 * 返回将应用于使用此工厂创建的bean的BeanPostProcessors列表。
	 */
	public List<BeanPostProcessor> getBeanPostProcessors() {
		return this.beanPostProcessors;
	}

	/**
	 * 返回预先过滤后处理器的内部缓存，如有必要，重新 (重新) 构建它。
	 *
	 * @since 5.3
	 */
	BeanPostProcessorCache getBeanPostProcessorCache() {
		//获取预先过滤后处理器的内部缓存
		BeanPostProcessorCache bpCache = this.beanPostProcessorCache;
		if (bpCache == null) {
			//如果后置处理器缓存为空，构建个BeanPostProcessorCache 实例
			bpCache = new BeanPostProcessorCache();
			for (BeanPostProcessor bp : this.beanPostProcessors) {
				//遍历bean后置处理器
				if (bp instanceof InstantiationAwareBeanPostProcessor) {
					//如果该后置处理器是InstantiationAwareBeanPostProcessor类型，添加到有关集合
					bpCache.instantiationAware.add((InstantiationAwareBeanPostProcessor) bp);
					if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
						//如果它还是 SmartInstantiationAwareBeanPostProcessor 实例，添加到smartInstantiationAware集合中
						bpCache.smartInstantiationAware.add((SmartInstantiationAwareBeanPostProcessor) bp);
					}
				}
				if (bp instanceof DestructionAwareBeanPostProcessor) {
					//如果该后置处理器是 DestructionAwareBeanPostProcessor 实例，添加到destructionAware集合
					bpCache.destructionAware.add((DestructionAwareBeanPostProcessor) bp);
				}
				if (bp instanceof MergedBeanDefinitionPostProcessor) {
					//如果该后置处理器是 MergedBeanDefinitionPostProcessor 实例，添加到mergedDefinition集合
					bpCache.mergedDefinition.add((MergedBeanDefinitionPostProcessor) bp);
				}
			}
			//设置缓存
			this.beanPostProcessorCache = bpCache;
		}
		return bpCache;
	}

	/**
	 * 返回此工厂是否拥有将在创建时应用于单例bean的InstantiationAwareBeanPostProcessor。
	 *
	 * @see #addBeanPostProcessor
	 * @see org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor
	 */
	protected boolean hasInstantiationAwareBeanPostProcessors() {
		return !getBeanPostProcessorCache().instantiationAware.isEmpty();
	}

	/**
	 * 返回此工厂是否持有一个 DestructionAwareBeanPostProcessor，
	 * 该处理器将在关闭时应用于单例 bean。
	 *
	 * @see #addBeanPostProcessor
	 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor
	 */
	protected boolean hasDestructionAwareBeanPostProcessors() {
		return !getBeanPostProcessorCache().destructionAware.isEmpty();
	}

	@Override
	public void registerScope(String scopeName, Scope scope) {
		Assert.notNull(scopeName, "Scope identifier must not be null");
		Assert.notNull(scope, "Scope must not be null");
		// 如果范围名称为 singleton 或 prototype，则抛出异常
		if (SCOPE_SINGLETON.equals(scopeName) || SCOPE_PROTOTYPE.equals(scopeName)) {
			throw new IllegalArgumentException("Cannot replace existing scopes 'singleton' and 'prototype'");
		}
		// 将作用域添加到作用域映射中，并获取之前的作用域
		Scope previous = this.scopes.put(scopeName, scope);
		// 如果之前的作用域存在且与当前作用域不同，则记录替换信息
		if (previous != null && previous != scope) {
			if (logger.isDebugEnabled()) {
				logger.debug("Replacing scope '" + scopeName + "' from [" + previous + "] to [" + scope + "]");
			}
		} else {
			// 否则记录注册信息
			if (logger.isTraceEnabled()) {
				logger.trace("Registering scope '" + scopeName + "' with implementation [" + scope + "]");
			}
		}
	}

	@Override
	public String[] getRegisteredScopeNames() {
		return StringUtils.toStringArray(this.scopes.keySet());
	}

	@Override
	@Nullable
	public Scope getRegisteredScope(String scopeName) {
		Assert.notNull(scopeName, "Scope identifier must not be null");
		return this.scopes.get(scopeName);
	}

	/**
	 * 设置此bean工厂的安全上下文提供程序。如果设置了安全管理器，
	 * 则将使用所提供的安全上下文的权限执行与用户代码的交互。
	 */
	public void setSecurityContextProvider(SecurityContextProvider securityProvider) {
		this.securityContextProvider = securityProvider;
	}

	@Override
	public void setApplicationStartup(ApplicationStartup applicationStartup) {
		Assert.notNull(applicationStartup, "applicationStartup should not be null");
		this.applicationStartup = applicationStartup;
	}

	@Override
	public ApplicationStartup getApplicationStartup() {
		return this.applicationStartup;
	}

	/**
	 * 将访问控制上下文的创建委托给  {@link #setSecurityContextProvider SecurityContextProvider}.。
	 */
	@Override
	public AccessControlContext getAccessControlContext() {
		return (this.securityContextProvider != null ?
				//如果安全上下文提供者不为空，则获取缓存的AccessControlContext，否则通过AccessController获取上下文
				this.securityContextProvider.getAccessControlContext() :
				AccessController.getContext());
	}

	@Override
	public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
		// 断言其他工厂不为null
		Assert.notNull(otherFactory, "BeanFactory must not be null");
		// 设置类加载器
		setBeanClassLoader(otherFactory.getBeanClassLoader());
		// 设置是否缓存Bean元数据
		setCacheBeanMetadata(otherFactory.isCacheBeanMetadata());
		// 设置Bean表达式解析器
		setBeanExpressionResolver(otherFactory.getBeanExpressionResolver());
		// 设置转换服务
		setConversionService(otherFactory.getConversionService());
		// 如果otherFactory是AbstractBeanFactory的实例
		if (otherFactory instanceof AbstractBeanFactory) {
			AbstractBeanFactory otherAbstractFactory = (AbstractBeanFactory) otherFactory;
			// 添加其他工厂的属性编辑器注册器
			this.propertyEditorRegistrars.addAll(otherAbstractFactory.propertyEditorRegistrars);
			// 将其他工厂的自定义编辑器添加到当前工厂中
			this.customEditors.putAll(otherAbstractFactory.customEditors);
			// 设置类型转换器
			this.typeConverter = otherAbstractFactory.typeConverter;
			// 添加其他工厂的Bean后置处理器
			this.beanPostProcessors.addAll(otherAbstractFactory.beanPostProcessors);
			// 添加其他工厂的作用域
			this.scopes.putAll(otherAbstractFactory.scopes);
			// 设置安全上下文提供程序
			this.securityContextProvider = otherAbstractFactory.securityContextProvider;
		} else {
			// 否则设置类型转换器
			setTypeConverter(otherFactory.getTypeConverter());
			// 获取其他工厂注册的所有作用域名称
			String[] otherScopeNames = otherFactory.getRegisteredScopeNames();
			// 遍历注册的作用域名称
			for (String scopeName : otherScopeNames) {
				// 将其他工厂的作用域添加到当前工厂中
				this.scopes.put(scopeName, otherFactory.getRegisteredScope(scopeName));
			}
		}
	}

	/**
	 * 返回给定bean名称的 “合并” bean定义，并在必要时将子bean定义与其父项合并。
	 * <p> 这个 {@code getMergedBeanDefinition} 也考虑了祖先中的bean定义。
	 *
	 * @param name 要检索的合并定义的bean的名称 (可能是别名)
	 * @return 给定bean的 (可能合并的) 根bean定义
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 * @throws BeanDefinitionStoreException  在bean定义无效的情况下
	 */
	@Override
	public BeanDefinition getMergedBeanDefinition(String name) throws BeansException {
		//获取规范的bean名称
		String beanName = transformedBeanName(name);
		// 有效地检查此工厂中是否存在bean定义。
		if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
			//如果该工厂中没有bean定义，且父bean工厂是 ConfigurableBeanFactory类型，
			// 则调用父工厂中的getMergedBeanDefinition方法合并bean定义
			return ((ConfigurableBeanFactory) getParentBeanFactory()).getMergedBeanDefinition(beanName);
		}
		// 在本地解析合并的bean定义。
		return getMergedLocalBeanDefinition(beanName);
	}

	@Override
	public boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException {
		// 获取转换后的bean名称
		String beanName = transformedBeanName(name);
		// 获取单例对象
		Object beanInstance = getSingleton(beanName, false);
		// 如果单例对象不为null，则判断是否是FactoryBean的实例
		if (beanInstance != null) {
			return (beanInstance instanceof FactoryBean);
		}
		// 如果当前工厂中不存在对应的bean定义，并且父工厂是可配置的工厂
		if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
			// 没有在当前工厂中找到bean定义，则委托给父工厂来判断是否是FactoryBean
			return ((ConfigurableBeanFactory) getParentBeanFactory()).isFactoryBean(name);
		}
		// 否则判断是否是FactoryBean
		return isFactoryBean(beanName, getMergedLocalBeanDefinition(beanName));
	}

	@Override
	public boolean isActuallyInCreation(String beanName) {
		return (isSingletonCurrentlyInCreation(beanName) || isPrototypeCurrentlyInCreation(beanName));
	}

	/**
	 * 返回指定的原型bean当前是否正在创建 (在当前线程内)。
	 *
	 * @param beanName bean名称
	 */
	protected boolean isPrototypeCurrentlyInCreation(String beanName) {
		//获取目前正在创建的bean名称
		Object curVal = this.prototypesCurrentlyInCreation.get();
		//如果当前的bean名称为Null，返回false
		//如果当前的bean名称与传入的bean名称相同，返回true
		//如果当前值为Set类型，且该Set包含传入的bean名称，返回true
		return (curVal != null &&
				(curVal.equals(beanName) || (curVal instanceof Set && ((Set<?>) curVal).contains(beanName))));
	}

	/**
	 * 在原型创建之前的回调方法。
	 * <p>默认实现将原型注册为当前正在创建的状态。
	 *
	 * @param beanName 即将创建的原型的名称
	 * @see #isPrototypeCurrentlyInCreation
	 */
	@SuppressWarnings("unchecked")
	protected void beforePrototypeCreation(String beanName) {
		// 获取当前正在创建中的原型 Bean 的集合
		Object curVal = this.prototypesCurrentlyInCreation.get();

		// 如果当前集合为空，直接将当前 Bean 名称设置为正在创建中的原型 Bean
		if (curVal == null) {
			this.prototypesCurrentlyInCreation.set(beanName);
		} else if (curVal instanceof String) {
			// 如果当前集合为单个 Bean 名称，创建一个包含两个 Bean 名称的集合
			Set<String> beanNameSet = new HashSet<>(2);
			beanNameSet.add((String) curVal);
			beanNameSet.add(beanName);
			this.prototypesCurrentlyInCreation.set(beanNameSet);
		} else {
			// 如果当前集合为多个 Bean 名称的集合，直接将当前 Bean 名称添加到集合中
			Set<String> beanNameSet = (Set<String>) curVal;
			beanNameSet.add(beanName);
		}
	}

	/**
	 * 在原型创建之后的回调方法。
	 * <p>默认实现标记原型为不再处于创建状态。
	 *
	 * @param beanName 已创建的原型的名称
	 * @see #isPrototypeCurrentlyInCreation
	 */
	@SuppressWarnings("unchecked")
	protected void afterPrototypeCreation(String beanName) {
		// 获取当前正在创建中的原型 Bean 的集合
		Object curVal = this.prototypesCurrentlyInCreation.get();

		// 如果当前集合为单个 Bean 名称，直接移除
		if (curVal instanceof String) {
			this.prototypesCurrentlyInCreation.remove();
		} else if (curVal instanceof Set) {
			// 如果当前集合为多个 Bean 名称的集合
			Set<String> beanNameSet = (Set<String>) curVal;

			// 移除当前 Bean 名称
			beanNameSet.remove(beanName);

			// 如果集合为空，移除整个集合
			if (beanNameSet.isEmpty()) {
				this.prototypesCurrentlyInCreation.remove();
			}
		}
	}

	@Override
	public void destroyBean(String beanName, Object beanInstance) {
		destroyBean(beanName, beanInstance, getMergedLocalBeanDefinition(beanName));
	}

	/**
	 * 根据给定的bean定义销毁给定的bean实例（通常是从此工厂获得的原型实例）。
	 *
	 * @param beanName bean名称 definition
	 * @param bean     要销毁的bean实例
	 * @param mbd      合并的bean定义
	 */
	protected void destroyBean(String beanName, Object bean, RootBeanDefinition mbd) {
		new DisposableBeanAdapter(
				bean, beanName, mbd, getBeanPostProcessorCache().destructionAware, getAccessControlContext()).destroy();
	}

	@Override
	public void destroyScopedBean(String beanName) {
		// 获取合并后的本地Bean定义
		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
		// 如果Bean定义的作用域为单例或原型，则抛出异常，因为这些作用域下的对象不可修改
		if (mbd.isSingleton() || mbd.isPrototype()) {
			throw new IllegalArgumentException(
					"Bean name '" + beanName + "' does not correspond to an object in a mutable scope");
		}
		// 获取Bean定义的作用域名称
		String scopeName = mbd.getScope();
		// 根据作用域名称从scopes中获取相应的Scope对象
		Scope scope = this.scopes.get(scopeName);
		// 如果Scope对象为null，则抛出异常，表示未注册相应的Scope SPI
		if (scope == null) {
			throw new IllegalStateException("No Scope SPI registered for scope name '" + scopeName + "'");
		}
		// 从Scope中移除Bean对象，并进行销毁
		Object bean = scope.remove(beanName);
		if (bean != null) {
			destroyBean(beanName, bean, mbd);
		}
	}


	//---------------------------------------------------------------------
	// Implementation methods
	//---------------------------------------------------------------------

	/**
	 * 返回bean名称，必要时剥离工厂取消引用前缀，并将别名解析为规范名称。
	 *
	 * @param name 用户指定的名称
	 * @return 转换后的bean名称
	 */
	protected String transformedBeanName(String name) {
		return canonicalName(BeanFactoryUtils.transformedBeanName(name));
	}

	/**
	 * 确定原始bean名称，将本地定义的别名解析为规范名称。
	 *
	 * @param name the user-specified name
	 * @return the original bean name
	 */
	protected String originalBeanName(String name) {
		//获取规范的bean名称
		String beanName = transformedBeanName(name);
		if (name.startsWith(FACTORY_BEAN_PREFIX)) {
			//如果该名称是以&符号开头，则该bean名称添加 & 符号前缀
			beanName = FACTORY_BEAN_PREFIX + beanName;
		}
		return beanName;
	}

	/**
	 * 使用此工厂注册的自定义编辑器初始化给定的 BeanWrapper。应该为将创建和填充bean实例的 BeanWrapper 调用此方法。
	 * <p>默认实现委托给 {@link #registerCustomEditors}。可以在子类中重写。
	 *
	 * @param bw 要初始化的 BeanWrapper
	 */
	protected void initBeanWrapper(BeanWrapper bw) {
		bw.setConversionService(getConversionService());
		registerCustomEditors(bw);
	}

	/**
	 * 使用在此 BeanFactory 中注册的自定义编辑器初始化给定的 PropertyEditorRegistry。
	 * <p>应该为将创建和填充bean实例的 BeanWrapper，以及用于构造函数参数和工厂方法类型转换的 SimpleTypeConverter 调用此方法。
	 *
	 * @param registry 要初始化的 PropertyEditorRegistry
	 */
	protected void registerCustomEditors(PropertyEditorRegistry registry) {
		// 如果注册表实现了 PropertyEditorRegistrySupport 接口，则使用配置值编辑器
		if (registry instanceof PropertyEditorRegistrySupport) {
			((PropertyEditorRegistrySupport) registry).useConfigValueEditors();
		}
		// 如果存在自定义的属性编辑器注册器
		if (!this.propertyEditorRegistrars.isEmpty()) {
			// 遍历所有注册的属性编辑器注册器
			for (PropertyEditorRegistrar registrar : this.propertyEditorRegistrars) {
				try {
					// 注册自定义编辑器
					registrar.registerCustomEditors(registry);
				} catch (BeanCreationException ex) {
					// 捕获 BeanCreationException 异常
					Throwable rootCause = ex.getMostSpecificCause();
					if (rootCause instanceof BeanCurrentlyInCreationException) {
						BeanCreationException bce = (BeanCreationException) rootCause;
						String bceBeanName = bce.getBeanName();
						// 如果存在当前正在创建的 bean
						if (bceBeanName != null && isCurrentlyInCreation(bceBeanName)) {
							if (logger.isDebugEnabled()) {
								logger.debug("PropertyEditorRegistrar [" + registrar.getClass().getName() +
										"] failed because it tried to obtain currently created bean '" +
										ex.getBeanName() + "': " + ex.getMessage());
							}
							// 记录抑制的异常
							onSuppressedException(ex);
							continue;
						}
					}
					// 抛出异常
					throw ex;
				}
			}
		}
		// 如果存在自定义的编辑器
		if (!this.customEditors.isEmpty()) {
			// 遍历所有自定义编辑器
			this.customEditors.forEach((requiredType, editorClass) ->
					// 为所需类型注册自定义编辑器
					registry.registerCustomEditor(requiredType, BeanUtils.instantiateClass(editorClass)));
		}
	}


	/**
	 * 返回一个合并的RootBeanDefinition，如果指定的bean对应于子bean定义，则遍历父bean定义。
	 *
	 * @param beanName 要检索的合并定义的bean的名称
	 * @return 给定bean的 (可能合并的) 根bean定义
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 * @throws BeanDefinitionStoreException  在bean定义无效的情况下
	 */
	protected RootBeanDefinition getMergedLocalBeanDefinition(String beanName) throws BeansException {
		//首先在并发Map上快速检查，锁定最少。
		RootBeanDefinition mbd = this.mergedBeanDefinitions.get(beanName);
		if (mbd != null && !mbd.stale) {
			//如果根bean定义不为空，且该根bean定义不需要合并，返回该根bean定义
			return mbd;
		}
		//获取bean定义，并获取合并bean定义
		return getMergedBeanDefinition(beanName, getBeanDefinition(beanName));
	}

	/**
	 * 如果给定bean的定义是子bean定义，则通过与父级bean合并来返回给定顶级bean的RootBeanDefinition。
	 *
	 * @param beanName bean定义的名称
	 * @param bd       原始bean定义 (Root/ChildBeanDefinition)
	 * @return 给定bean的 (可能合并的) 根bean定义
	 * @throws BeanDefinitionStoreException 在bean定义无效的情况下
	 */
	protected RootBeanDefinition getMergedBeanDefinition(String beanName, BeanDefinition bd)
			throws BeanDefinitionStoreException {

		return getMergedBeanDefinition(beanName, bd, null);
	}

	/**
	 * 如果给定bean的定义是子bean定义，则通过与父级合并来返回给定bean的RootBeanDefinition。
	 *
	 * @param beanName     bean定义的名称
	 * @param bd           原始bean定义 (Root/ChildBeanDefinition)
	 * @param containingBd 在内部bean的情况下包含bean定义，或者在顶级bean的情况下 为{@code null}
	 * @return 给定bean的 (可能合并的) 根bean定义
	 * @throws BeanDefinitionStoreException 在bean定义无效的情况下
	 */
	protected RootBeanDefinition getMergedBeanDefinition(
			String beanName, BeanDefinition bd, @Nullable BeanDefinition containingBd)
			throws BeanDefinitionStoreException {

		synchronized (this.mergedBeanDefinitions) {
			//锁定合并bean定义Map
			RootBeanDefinition mbd = null;
			RootBeanDefinition previous = null;

			// 立即使用完全锁定进行检查，以强制执行相同的合并实例。
			if (containingBd == null) {
				//如果包含bean定义为空，则获取该bean名称对应的bean定义来作为根bean定义
				mbd = this.mergedBeanDefinitions.get(beanName);
			}

			if (mbd == null || mbd.stale) {
				//如果根bean定义不存在，或者根bean定义需要合并
				//前一个根bean定义为当前根bean定义
				previous = mbd;
				if (bd.getParentName() == null) {
					//如果bean定义中不存在父bean定义名称
					// 使用给定根bean定义的副本。
					if (bd instanceof RootBeanDefinition) {
						//如果该bean定义是根bean定义类型，则复制一个实例作为根bean定义
						mbd = ((RootBeanDefinition) bd).cloneBeanDefinition();
					} else {
						//根据bean定义构建一个根bean定义实例，作为根bean定义
						mbd = new RootBeanDefinition(bd);
					}
				} else {
					// 子bean定义: 需要与parent合并。
					BeanDefinition pbd;
					try {
						//获取父bean的规范bean名称
						String parentBeanName = transformedBeanName(bd.getParentName());
						if (!beanName.equals(parentBeanName)) {
							//如果bean名称与父bean名称不同，合并bean定义
							pbd = getMergedBeanDefinition(parentBeanName);
						} else {
							//否则获取父bean工厂
							BeanFactory parent = getParentBeanFactory();
							if (parent instanceof ConfigurableBeanFactory) {
								//如果父bean工厂是ConfigurableBeanFactory类型，则使用ConfigurableBeanFactory 的合并bean定义方法
								pbd = ((ConfigurableBeanFactory) parent).getMergedBeanDefinition(parentBeanName);
							} else {
								throw new NoSuchBeanDefinitionException(parentBeanName,
										"Parent name '" + parentBeanName + "' is equal to bean name '" + beanName +
												"': cannot be resolved without a ConfigurableBeanFactory parent");
							}
						}
					} catch (NoSuchBeanDefinitionException ex) {
						throw new BeanDefinitionStoreException(bd.getResourceDescription(), beanName,
								"Could not resolve parent bean definition '" + bd.getParentName() + "'", ex);
					}
					//深拷贝覆盖值
					mbd = new RootBeanDefinition(pbd);
					//将bean定义的各个属性覆盖到根bean定义
					//将子bean定义的各项属性合并到父bean定义中。
					mbd.overrideFrom(bd);
				}

				// 设置默认单例范围 (如果之前未配置)。
				if (!StringUtils.hasLength(mbd.getScope())) {
					//如果没有配置返回，则设置为单例
					mbd.setScope(SCOPE_SINGLETON);
				}
				//非单例bean中包含的bean本身不能是单例bean。
				// 让我们在这里进行纠正，因为这可能是外部bean的父子合并的结果，
				// 在这种情况下，原始的内部bean定义将不会继承合并的外部bean的单例状态。
				if (containingBd != null && !containingBd.isSingleton() && mbd.isSingleton()) {
					//如果包含有bean定义别切包含bean定义不是单例的且当前的根bean定义是单例的，
					// 则将根bean定义的范围设置为包含bean定义的范围。
					mbd.setScope(containingBd.getScope());
				}

				// 暂时缓存合并的bean定义 (以后可能仍会重新合并以获取元数据更改)
				if (containingBd == null && isCacheBeanMetadata()) {
					//如果包含bean定义为空，且需要缓存bean定义的元数据，则将其缓存进合并bean定义中
					this.mergedBeanDefinitions.put(beanName, mbd);
				}
			}
			if (previous != null) {
				//如果前一个bean定义不为空，复制相关的合并Bean定义缓存
				copyRelevantMergedBeanDefinitionCaches(previous, mbd);
			}
			return mbd;
		}
	}

	private void copyRelevantMergedBeanDefinitionCaches(RootBeanDefinition previous, RootBeanDefinition mbd) {
		if (ObjectUtils.nullSafeEquals(mbd.getBeanClassName(), previous.getBeanClassName()) &&
				ObjectUtils.nullSafeEquals(mbd.getFactoryBeanName(), previous.getFactoryBeanName()) &&
				ObjectUtils.nullSafeEquals(mbd.getFactoryMethodName(), previous.getFactoryMethodName())) {
			//如果二者bean类名、工厂bean名、工厂方法名相同，则复制相关的合并Bean定义缓存
			ResolvableType targetType = mbd.targetType;
			ResolvableType previousTargetType = previous.targetType;
			if (targetType == null || targetType.equals(previousTargetType)) {
				//复制目标类型
				mbd.targetType = previousTargetType;
				//复制是否是工厂bean
				mbd.isFactoryBean = previous.isFactoryBean;
				//复制解析好的目标类型
				mbd.resolvedTargetType = previous.resolvedTargetType;
				//复制工厂方法返回类型
				mbd.factoryMethodReturnType = previous.factoryMethodReturnType;
				//复制工厂方法实例
				mbd.factoryMethodToIntrospect = previous.factoryMethodToIntrospect;
			}
		}
	}

	/**
	 * 检查给定的合并bean定义，可能引发验证异常。
	 *
	 * @param mbd      要检查的合并bean定义
	 * @param beanName bean名称
	 * @param args     创建bean的参数 (如果有)
	 * @throws BeanDefinitionStoreException 在验证失败的情况下
	 */
	protected void checkMergedBeanDefinition(RootBeanDefinition mbd, String beanName, @Nullable Object[] args)
			throws BeanDefinitionStoreException {

		if (mbd.isAbstract()) {
			//如果该根bean定义仍然是抽象的，则抛出异常
			throw new BeanIsAbstractException(beanName);
		}
	}

	/**
	 * 删除指定bean的合并bean定义，在下次访问时重新创建它。
	 *
	 * @param beanName 清除合并定义的bean名称
	 */
	protected void clearMergedBeanDefinition(String beanName) {
		//根据bean名称获取根Bean定义
		RootBeanDefinition bd = this.mergedBeanDefinitions.get(beanName);
		if (bd != null) {
			//如果根bean定义存在，合并bean定义
			bd.stale = true;
		}
	}

	/**
	 * 清除合并的bean定义缓存，删除对于尚未被视为完全元数据缓存的bean的条目。
	 * <p>通常在对原始bean定义进行更改后触发，例如在应用{@code BeanFactoryPostProcessor}后。请注意，在此时已经创建的bean的元数据将被保留。
	 *
	 * @since 4.2
	 */
	public void clearMetadataCache() {
		this.mergedBeanDefinitions.forEach((beanName, bd) -> {
			if (!isBeanEligibleForMetadataCaching(beanName)) {
				bd.stale = true;
			}
		});
	}

	/**
	 * 解析指定bean定义的bean类，将bean类名解析为类引用 (如有必要)，并将解析的类存储在bean定义中以供进一步使用。
	 *
	 * @param mbd          用于确定类的合并的bean定义
	 * @param beanName     bean的名称 (用于错误处理)
	 * @param typesToMatch 在内部类型匹配的情况下要匹配的类型 (也表示返回的 {@code Class} 永远不会暴露于应用程序代码)
	 * @return 解析的bean类 (如果没有，则为 {@code null})
	 * @throws CannotLoadBeanClassException 如果我们未能加载类
	 */
	@Nullable
	protected Class<?> resolveBeanClass(RootBeanDefinition mbd, String beanName, Class<?>... typesToMatch)
			throws CannotLoadBeanClassException {

		try {
			if (mbd.hasBeanClass()) {
				//如果根bean定义中有bean类，返回该根bean定义中的bean类
				return mbd.getBeanClass();
			}
			if (System.getSecurityManager() != null) {
				//如果系统安全接口存在，授予解析bean类方法的特权
				return AccessController.doPrivileged((PrivilegedExceptionAction<Class<?>>)
						() -> doResolveBeanClass(mbd, typesToMatch), getAccessControlContext());
			} else {
				//否则直接解析bean类
				return doResolveBeanClass(mbd, typesToMatch);
			}
		} catch (PrivilegedActionException pae) {
			ClassNotFoundException ex = (ClassNotFoundException) pae.getException();
			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), ex);
		} catch (ClassNotFoundException ex) {
			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), ex);
		} catch (LinkageError err) {
			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), err);
		}
	}

	@Nullable
	private Class<?> doResolveBeanClass(RootBeanDefinition mbd, Class<?>... typesToMatch)
			throws ClassNotFoundException {
		//获取bean类加载器
		ClassLoader beanClassLoader = getBeanClassLoader();
		//动态类加载器
		ClassLoader dynamicLoader = beanClassLoader;
		boolean freshResolve = false;

		if (!ObjectUtils.isEmpty(typesToMatch)) {
			// 当只是进行类型检查 (即尚未创建实际实例) 时，请使用指定的临时类加载器 (例如在编织场景中)。
			//如果匹配的类型不为空，获取临时的类加载器
			ClassLoader tempClassLoader = getTempClassLoader();
			if (tempClassLoader != null) {
				//如果临时类加载器不为空，当前动态类加载器为临时类加载器
				dynamicLoader = tempClassLoader;
				//新的解析设置为true
				freshResolve = true;
				if (tempClassLoader instanceof DecoratingClassLoader) {
					//如果临时类加载器是DecoratingClassLoader类型
					DecoratingClassLoader dcl = (DecoratingClassLoader) tempClassLoader;
					for (Class<?> typeToMatch : typesToMatch) {
						//装饰类加载器，排除匹配的类名
						dcl.excludeClass(typeToMatch.getName());
					}
				}
			}
		}
		//获取bean类名
		String className = mbd.getBeanClassName();
		if (className != null) {
			//如果类名不为空，评估bean定义中包含的给定字符串，将其解析为表达式。
			Object evaluated = evaluateBeanDefinitionString(className, mbd);
			if (!className.equals(evaluated)) {
				// 如果类名和评估值不同，按照类型进行特殊处理。
				// 一个动态解析的表达式，从4.2开始支持...
				if (evaluated instanceof Class) {
					//如果评估值为Class类型，强转为Class后返回该评估值。
					return (Class<?>) evaluated;
				} else if (evaluated instanceof String) {
					//如果评估值为字符串类型，将类名设置为评估值
					className = (String) evaluated;
					//新的解析标志设为true
					freshResolve = true;
				} else {
					throw new IllegalStateException("Invalid class name expression result: " + evaluated);
				}
			}
			if (freshResolve) {
				// 如果新解析标志为true
				// 当针对临时类加载器进行解析时，请提前退出，以避免将解析的类存储在bean定义中。
				// 该分支和 mbd.resolveBeanClass(beanClassLoader) 的区别是解析class后，没有设置bean定义的beanClass
				if (dynamicLoader != null) {
					//如果动态类加载器存在，使用动态类加载加载类名，并返回对应的类型。
					try {
						return dynamicLoader.loadClass(className);
					} catch (ClassNotFoundException ex) {
						if (logger.isTraceEnabled()) {
							logger.trace("Could not load class [" + className + "] from " + dynamicLoader + ": " + ex);
						}
					}
				}
				return ClassUtils.forName(className, dynamicLoader);
			}
		}

		// 定期解析，将结果缓存在BeanDefinition中...
		return mbd.resolveBeanClass(beanClassLoader);
	}

	/**
	 * 评估bean定义中包含的给定字符串，有可能将其解析为表达式。
	 *
	 * @param value          要检查的值
	 * @param beanDefinition 值来自的bean定义
	 * @return 已经解析的值
	 * @see #setBeanExpressionResolver
	 */
	@Nullable
	protected Object evaluateBeanDefinitionString(@Nullable String value, @Nullable BeanDefinition beanDefinition) {
		if (this.beanExpressionResolver == null) {
			//如果bean表达式解析器为空，直接返回原始值
			return value;
		}

		Scope scope = null;
		if (beanDefinition != null) {
			//如果bean定义不为空，获取bean定义的范围
			String scopeName = beanDefinition.getScope();
			if (scopeName != null) {
				//如果范围不为空，获取对应的Scope类
				scope = getRegisteredScope(scopeName);
			}
		}
		//使用bean表达式解析器解析，调用Spring EL表达式解析value值
		return this.beanExpressionResolver.evaluate(value, new BeanExpressionContext(this, scope));
	}


	/**
	 * 预测指定bean的最终bean类型 (已处理的bean实例的)。
	 * 由 {@link #getType} 和 {@link #isTypeMatch} 调用。不需要专门处理FactoryBeans，因为它只应该对未加工的bean类型进行操作。
	 * <p> 此实现过于简单，因为它无法处理工厂方法和InstantiationAwareBeanPostProcessors。
	 * 它仅正确预测标准bean的bean类型。要在子类中覆盖，应用更复杂的类型检测。
	 *
	 * @param beanName     bean名称
	 * @param mbd          用来确定类型的合并bean定义
	 * @param typesToMatch 在内部类型匹配的情况下要匹配的类型 (也表示返回的 {@code Class} 永远不会暴露于应用程序代码)
	 * @return bean的类型，如果不可预测，则为 {@code null}
	 */
	@Nullable
	protected Class<?> predictBeanType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
		//如果目标类型
		Class<?> targetType = mbd.getTargetType();
		if (targetType != null) {
			//如果目标类型不为空，则返回该类型
			return targetType;
		}
		if (mbd.getFactoryMethodName() != null) {
			//如果该工厂方法名称不为空，则返回null
			return null;
		}
		return resolveBeanClass(mbd, beanName, typesToMatch);
	}

	/**
	 * 检查给定的bean是否定义为{@link FactoryBean}。
	 *
	 * @param beanName bean名称
	 * @param mbd      相应的bean定义
	 * @return 如果给定的bean是FactoryBean，则返回true；否则返回false
	 */
	protected boolean isFactoryBean(String beanName, RootBeanDefinition mbd) {
		// 获取是否为FactoryBean的预测结果
		Boolean result = mbd.isFactoryBean;
		// 如果预测结果为null，则尝试预测Bean的类型是否为FactoryBean类型
		if (result == null) {
			// 预测Bean的类型，以便判断是否为FactoryBean类型
			Class<?> beanType = predictBeanType(beanName, mbd, FactoryBean.class);
			// 如果预测到的Bean类型不为null，并且是FactoryBean的子类，则返回true，否则返回false
			result = (beanType != null && FactoryBean.class.isAssignableFrom(beanType));
			// 将预测结果保存到Bean定义中，以便下次使用
			mbd.isFactoryBean = result;
		}
		return result;
	}

	/**
	 * 尽可能确定给定FactoryBean定义的bean类型。仅当没有为目标bean注册的单例实例时才调用。
	 * 如果 {@code allowInit} 为 {@code true}，并且无法以另一种方式确定类型，则允许实现实例化目标工厂bean;
	 * 否则，它仅限于内省签名和相关元数据。
	 * <p> 如果在bean定义上设置没有 {@link FactoryBean#OBJECT_TYPE_ATTRIBUTE}，并且 {@code allowInit} 为 {@code true}，
	 * 则默认实现将通过 {@code getBean} 创建FactoryBean以调用其 {@code getObjectType} 方法。
	 * 鼓励子类对此进行优化，通常是通过检查工厂bean类或创建它的工厂方法的通用签名。
	 * 如果子类确实实例化了FactoryBean，他们应该考虑在不完全填充bean的情况下尝试 {@code getObjectType} 方法。
	 * 如果失败，则应将此实现执行的完整FactoryBean创建用作后备。
	 *
	 * @param beanName  bean名称
	 * @param mbd       bean的合并bean定义
	 * @param allowInit 如果无法以另一种方式确定类型，则允许初始化FactoryBean
	 * @return bean的类型 (如果可确定)，否则 {@code ResolvableType.NONE}
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 * @see #getBean(String)
	 * @since 5.2
	 */
	protected ResolvableType getTypeForFactoryBean(String beanName, RootBeanDefinition mbd, boolean allowInit) {
		//根据bean定义的属性值获取可解析的类型
		ResolvableType result = getTypeForFactoryBeanFromAttributes(mbd);
		if (result != ResolvableType.NONE) {
			//如果是不是空置，则返回该解析类型
			return result;
		}

		if (allowInit && mbd.isSingleton()) {
			//如果允许初始化，且该bean定义是单例类型
			try {
				//获取工厂bean
				FactoryBean<?> factoryBean = doGetBean(FACTORY_BEAN_PREFIX + beanName, FactoryBean.class, null, true);
				//根据工厂bean获取对象类型
				Class<?> objectType = getTypeForFactoryBean(factoryBean);
				//如果是对象类型为空，返回ResolvableType.NONE，否则，包装成可解析类型返回
				return (objectType == null ? ResolvableType.NONE : ResolvableType.forClass(objectType));
			} catch (BeanCreationException ex) {
				if (ex.contains(BeanCurrentlyInCreationException.class)) {
					logger.trace(LogMessage.format("Bean currently in creation on FactoryBean type check: %s", ex));
				} else if (mbd.isLazyInit()) {
					logger.trace(LogMessage.format("Bean creation exception on lazy FactoryBean type check: %s", ex));
				} else {
					logger.debug(LogMessage.format("Bean creation exception on eager FactoryBean type check: %s", ex));
				}
				//将其丢入异常栈中
				onSuppressedException(ex);
			}
		}
		return ResolvableType.NONE;
	}

	/**
	 * 通过检查FactoryBean的属性以获取 {@link FactoryBean#OBJECT_TYPE_ATTRIBUTE} 值来确定FactoryBean的bean类型。
	 *
	 * @param attributes 要检查的属性
	 * @return 从属性提取出的一个{@code ResolvableType} 或 {@code ResolvableType.NONE}
	 * @since 5.2
	 */
	ResolvableType getTypeForFactoryBeanFromAttributes(AttributeAccessor attributes) {
		//获取FactoryBean的属性
		Object attribute = attributes.getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE);
		if (attribute instanceof ResolvableType) {
			//如果是ResolvableType类型，返回该类型值
			return (ResolvableType) attribute;
		}
		if (attribute instanceof Class) {
			//如果是Class类型，包装成ResolvableType类型后返回。
			return ResolvableType.forClass((Class<?>) attribute);
		}
		//否则返回NONE
		return ResolvableType.NONE;
	}

	/**
	 * 确定给定FactoryBean定义的bean类型，尽可能确定。
	 * 仅当目标bean尚未注册单例实例时才调用。
	 * <p>默认实现通过{@code getBean}创建FactoryBean来调用其{@code getObjectType}方法。
	 * 鼓励子类进行优化，通常只是实例化FactoryBean但尚未填充它，
	 * 尝试其{@code getObjectType}方法是否已返回类型。
	 * 如果找不到类型，则应使用此实现执行的完整FactoryBean创建作为备用。
	 *
	 * @param beanName bean名称
	 * @param mbd      bean的合并bean定义
	 * @return 如果可确定，则为bean的类型；否则为{@code null}
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 * @see #getBean(String)
	 * @deprecated 自5.2起，不建议使用，而应改用{@link #getTypeForFactoryBean(String, RootBeanDefinition, boolean)}
	 */
	@Nullable
	@Deprecated
	protected Class<?> getTypeForFactoryBean(String beanName, RootBeanDefinition mbd) {
		return getTypeForFactoryBean(beanName, mbd, true).resolve();
	}

	/**
	 * 将指定的bean标记为已创建 (或即将创建)。
	 * <p> 这允许bean工厂优化其缓存，以重复创建指定的bean。
	 *
	 * @param beanName bean名称
	 */
	protected void markBeanAsCreated(String beanName) {
		if (!this.alreadyCreated.contains(beanName)) {
			//如果该bean名称还未创建，锁定mergedBeanDefinitions
			synchronized (this.mergedBeanDefinitions) {
				if (!this.alreadyCreated.contains(beanName)) {
					//让bean定义重新合并，现在我们实际上是在创建bean...以防它的一些元数据在此期间发生变化。
					//清除该bean名称的合并bean定义
					clearMergedBeanDefinition(beanName);
					//将该bean名称设置已创建
					this.alreadyCreated.add(beanName);
				}
			}
		}
	}

	/**
	 * bean创建失败后，对缓存的元数据执行适当的清理。
	 *
	 * @param beanName bean名称
	 */
	protected void cleanupAfterBeanCreationFailure(String beanName) {
		synchronized (this.mergedBeanDefinitions) {
			//清除当前已经创建的bean名称
			this.alreadyCreated.remove(beanName);
		}
	}

	/**
	 * 确定指定的bean是否有资格将其bean定义元数据缓存。
	 *
	 * @param beanName bean名称
	 * @return 如果在此时已经可以缓存bean的元数据，则为{@code true}
	 */
	protected boolean isBeanEligibleForMetadataCaching(String beanName) {
		return this.alreadyCreated.contains(beanName);
	}

	/**
	 * 如果单例实例尚未用于除类型检查之外的其他目的，则删除给定 bean 名称的单例实例。
	 *
	 * @param beanName 要删除单例的 bean 名称
	 * @return 如果实际移除了，则返回 {@code true}，否则返回 {@code false}
	 */
	protected boolean removeSingletonIfCreatedForTypeCheckOnly(String beanName) {
		if (!this.alreadyCreated.contains(beanName)) {
			// 如果 beanName 不在已创建的集合中
			removeSingleton(beanName);
			// 从单例缓存中移除 bean
			return true;
		} else {
			// 如果 beanName 已经创建过
			return false;
		}
	}

	/**
	 * 检查此工厂的bean创建阶段是否已经开始，即在此期间是否已将任何bean标记为创建。
	 *
	 * @see #markBeanAsCreated
	 * @since 4.2.2
	 */
	protected boolean hasBeanCreationStarted() {
		return !this.alreadyCreated.isEmpty();
	}

	/**
	 * 获取给定bean实例的对象，无论是bean实例本身还是在FactoryBean的情况下创建的对象。
	 *
	 * @param beanInstance 共享bean实例
	 * @param name         可能包含工厂取消引用前缀的名称
	 * @param beanName     规范的bean名称
	 * @param mbd          合并的bean定义
	 * @return the object to expose for the bean
	 */
	protected Object getObjectForBeanInstance(
			Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {

		// 如果bean不是工厂，请不要让调用代码尝试取消引用工厂。
		if (BeanFactoryUtils.isFactoryDereference(name)) {
			//如果该名称是工厂取消引用前缀
			if (beanInstance instanceof NullBean) {
				//如果该bean实例是NullBean类型，则返回该实例
				return beanInstance;
			}
			if (!(beanInstance instanceof FactoryBean)) {
				//如果该bean实例不是FactoryBean类型，则抛出异常
				throw new BeanIsNotAFactoryException(beanName, beanInstance.getClass());
			}
			if (mbd != null) {
				//如果合并bean定义不为空，将合并bean定义设置为工厂bean
				mbd.isFactoryBean = true;
			}
			//返回该bean实例
			return beanInstance;
		}

		//如果它是一个FactoryBean，我们使用它来创建一个bean实例，除非调用者实际上想要对工厂的引用。
		if (!(beanInstance instanceof FactoryBean)) {
			//如果bean实例不是FactoryBean类型，返回bean实例
			return beanInstance;
		}

		Object object = null;
		if (mbd != null) {
			//如果合并的bean定义不为，将其标记为工厂bean
			mbd.isFactoryBean = true;
		} else {
			//从缓存中获取bean名称对应的工厂bean实例。
			object = getCachedObjectForFactoryBean(beanName);
		}
		if (object == null) {
			// 如果该实例为空，从工厂返回bean实例。
			FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
			// 如果它是一个单例，缓存从FactoryBean获得的对象。
			if (mbd == null && containsBeanDefinition(beanName)) {
				//如果合并bean定义为空，且bean工厂中包含bean名称的bean定义，获取本地的合并bean定义。
				mbd = getMergedLocalBeanDefinition(beanName);
			}
			//如果该bean定义是合成的
			boolean synthetic = (mbd != null && mbd.isSynthetic());
			//从bean工厂中获取实例
			object = getObjectFromFactoryBean(factory, beanName, !synthetic);
		}
		return object;
	}

	/**
	 * 确定给定的bean名称是否已在此工厂中使用，即在此名称下是否已注册了本地bean或别名，或者已创建了使用此名称的内部bean。
	 *
	 * @param beanName 要检查的名称
	 * @return 如果名称已在使用，则为true
	 */
	public boolean isBeanNameInUse(String beanName) {
		return isAlias(beanName) || containsLocalBean(beanName) || hasDependentBean(beanName);
	}

	/**
	 * 确定给定的bean是否需要在关闭时销毁。
	 * 默认实现会检查DisposableBean接口以及指定的destroy方法和已注册的DestructionAwareBeanPostProcessors。
	 *
	 * @param bean 要检查的bean实例
	 * @param mbd  相应的bean定义
	 * @return 如果需要销毁，则为true
	 * @see org.springframework.beans.factory.DisposableBean
	 * @see AbstractBeanDefinition#getDestroyMethodName()
	 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor
	 */
	protected boolean requiresDestruction(Object bean, RootBeanDefinition mbd) {
		return (bean.getClass() != NullBean.class && (DisposableBeanAdapter.hasDestroyMethod(bean, mbd) ||
				(hasDestructionAwareBeanPostProcessors() && DisposableBeanAdapter.hasApplicableProcessors(
						bean, getBeanPostProcessorCache().destructionAware))));
	}

	/**
	 * 将给定的 bean 添加到此工厂中的可销毁 bean 列表中，注册其 DisposableBean 接口和/或给定的销毁方法，
	 * 以便在工厂关闭时调用（如果适用）。仅适用于单例。
	 *
	 * @param beanName bean 的名称
	 * @param bean     bean 实例
	 * @param mbd      bean 的定义
	 * @see RootBeanDefinition#isSingleton
	 * @see RootBeanDefinition#getDependsOn
	 * @see #registerDisposableBean
	 * @see #registerDependentBean
	 */
	protected void registerDisposableBeanIfNecessary(String beanName, Object bean, RootBeanDefinition mbd) {
		AccessControlContext acc = (System.getSecurityManager() != null ? getAccessControlContext() : null);
		if (!mbd.isPrototype() && requiresDestruction(bean, mbd)) {
			// 如果不是 prototype 且需要销毁
			if (mbd.isSingleton()) {
				// 如果是 单例，则注册 DisposableBean 适配器，执行销毁工作
				// 包括 DestructionAwareBeanPostProcessors，DisposableBean 接口，自定义销毁方法
				registerDisposableBean(beanName, new DisposableBeanAdapter(
						bean, beanName, mbd, getBeanPostProcessorCache().destructionAware, acc));
			} else {
				// 如果具有自定义范围的 bean...
				Scope scope = this.scopes.get(mbd.getScope());
				if (scope == null) {
					// 如果范围未注册，则抛出异常
					throw new IllegalStateException("No Scope registered for scope name '" + mbd.getScope() + "'");
				}
				// 在自定义范围中注册销毁回调
				scope.registerDestructionCallback(beanName, new DisposableBeanAdapter(
						bean, beanName, mbd, getBeanPostProcessorCache().destructionAware, acc));
			}
		}

	}


	//---------------------------------------------------------------------
	// Abstract methods to be implemented by subclasses
	//---------------------------------------------------------------------

	/**
	 * 检查此bean工厂是否包含具有给定名称的bean定义。
	 * 不考虑该工厂可能参与的任何层次结构。当找不到缓存的单例实例时，由 {@code containsBean} 调用。
	 * <p> 根据具体bean工厂实现的性质，此操作可能会很昂贵 (例如，由于外部注册表中的目录查找)。
	 * 但是，对于 列表 bean工厂，这通常仅相当于本地哈希查找: 因此，该操作是那里公共接口的一部分。
	 * 在这种情况下，相同的实现可以同时用于此模板方法和公共接口方法。
	 *
	 * @param beanName 要找的bean名称
	 * @return 如果此bean工厂包含具有给定名称的bean定义
	 * @see #containsBean
	 * @see org.springframework.beans.factory.ListableBeanFactory#containsBeanDefinition
	 */
	protected abstract boolean containsBeanDefinition(String beanName);

	/**
	 * 返回给定bean名称的bean定义。
	 * 子类通常应该实现缓存，因为每次需要bean定义元数据时，该类都会调用此方法。
	 * <p> 根据具体bean工厂实现的性质，此操作可能会很昂贵 (例如，由于外部注册表中的目录查找)。
	 * 但是，对于列表 bean工厂，这通常仅相当于本地哈希查找:
	 * 因此，该操作是公共接口的一部分。在这种情况下，相同的实现可以同时用于此模板方法和公共接口方法。
	 *
	 * @param beanName 要查找的定义的bean的名称
	 * @return 此原型名称的BeanDefinition (从不为 {@code null})
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException 如果无法解析bean定义
	 * @throws BeansException                                                  在错误的情况下
	 * @see RootBeanDefinition
	 * @see ChildBeanDefinition
	 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#getBeanDefinition
	 */
	protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;

	/**
	 * 为给定的合并bean定义 (和参数) 创建一个bean实例。
	 * 如果是子定义，则bean定义将已经与父定义合并。
	 * <p> 所有bean检索方法都委托给此方法，以进行实际的bean创建。
	 *
	 * @param beanName bean名称
	 * @param mbd      bean的合并bean定义
	 * @param args     用于构造函数或工厂方法调用的显式参数
	 * @return bean的新实例
	 * @throws BeanCreationException if the bean could not be created
	 */
	protected abstract Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
			throws BeanCreationException;


	/**
	 * CopyOnWriteArrayList，修改时重置beanPostProcessorCache字段。
	 *
	 * @since 5.3
	 */
	private class BeanPostProcessorCacheAwareList extends CopyOnWriteArrayList<BeanPostProcessor> {

		@Override
		public BeanPostProcessor set(int index, BeanPostProcessor element) {
			BeanPostProcessor result = super.set(index, element);
			beanPostProcessorCache = null;
			return result;
		}

		@Override
		public boolean add(BeanPostProcessor o) {
			boolean success = super.add(o);
			beanPostProcessorCache = null;
			return success;
		}

		@Override
		public void add(int index, BeanPostProcessor element) {
			super.add(index, element);
			beanPostProcessorCache = null;
		}

		@Override
		public BeanPostProcessor remove(int index) {
			BeanPostProcessor result = super.remove(index);
			beanPostProcessorCache = null;
			return result;
		}

		@Override
		public boolean remove(Object o) {
			boolean success = super.remove(o);
			if (success) {
				beanPostProcessorCache = null;
			}
			return success;
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			boolean success = super.removeAll(c);
			if (success) {
				beanPostProcessorCache = null;
			}
			return success;
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			boolean success = super.retainAll(c);
			if (success) {
				beanPostProcessorCache = null;
			}
			return success;
		}

		@Override
		public boolean addAll(Collection<? extends BeanPostProcessor> c) {
			boolean success = super.addAll(c);
			if (success) {
				beanPostProcessorCache = null;
			}
			return success;
		}

		@Override
		public boolean addAll(int index, Collection<? extends BeanPostProcessor> c) {
			boolean success = super.addAll(index, c);
			if (success) {
				beanPostProcessorCache = null;
			}
			return success;
		}

		@Override
		public boolean removeIf(Predicate<? super BeanPostProcessor> filter) {
			boolean success = super.removeIf(filter);
			if (success) {
				beanPostProcessorCache = null;
			}
			return success;
		}

		@Override
		public void replaceAll(UnaryOperator<BeanPostProcessor> operator) {
			super.replaceAll(operator);
			beanPostProcessorCache = null;
		}
	}


	/**
	 * 预过滤后处理器的内部缓存。
	 *
	 * @since 5.3
	 */
	static class BeanPostProcessorCache {
		/**
		 * 实例化感知Bean后处理器列表
		 */
		final List<InstantiationAwareBeanPostProcessor> instantiationAware = new ArrayList<>();
		/**
		 * 智能实例化感知Bean后置处理器列表
		 */
		final List<SmartInstantiationAwareBeanPostProcessor> smartInstantiationAware = new ArrayList<>();
		/**
		 * 销毁感知Bean后处理器列表
		 */
		final List<DestructionAwareBeanPostProcessor> destructionAware = new ArrayList<>();
		/**
		 * 合并的Bean定义后处理器
		 */
		final List<MergedBeanDefinitionPostProcessor> mergedDefinition = new ArrayList<>();
	}

}
