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
 * Abstract base class for {@link org.springframework.beans.factory.BeanFactory}
 * implementations, providing the full capabilities of the
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory} SPI.
 * Does <i>not</i> assume a listable bean factory: can therefore also be used
 * as base class for bean factory implementations which obtain bean definitions
 * from some backend resource (where bean definition access is an expensive operation).
 *
 * <p>This class provides a singleton cache (through its base class
 * {@link org.springframework.beans.factory.support.DefaultSingletonBeanRegistry},
 * singleton/prototype determination, {@link org.springframework.beans.factory.FactoryBean}
 * handling, aliases, bean definition merging for child bean definitions,
 * and bean destruction ({@link org.springframework.beans.factory.DisposableBean}
 * interface, custom destroy methods). Furthermore, it can manage a bean factory
 * hierarchy (delegating to the parent in case of an unknown bean), through implementing
 * the {@link org.springframework.beans.factory.HierarchicalBeanFactory} interface.
 *
 * <p>The main template methods to be implemented by subclasses are
 * {@link #getBeanDefinition} and {@link #createBean}, retrieving a bean definition
 * for a given bean name and creating a bean instance for a given bean definition,
 * respectively. Default implementations of those operations can be found in
 * {@link DefaultListableBeanFactory} and {@link AbstractAutowireCapableBeanFactory}.
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
		String beanName = transformedBeanName(name);

		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null) {
			if (beanInstance instanceof FactoryBean) {
				return (BeanFactoryUtils.isFactoryDereference(name) || ((FactoryBean<?>) beanInstance).isSingleton());
			} else {
				return !BeanFactoryUtils.isFactoryDereference(name);
			}
		}

		// No singleton instance found -> check bean definition.
		BeanFactory parentBeanFactory = getParentBeanFactory();
		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
			// No bean definition found in this factory -> delegate to parent.
			return parentBeanFactory.isSingleton(originalBeanName(name));
		}

		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

		// In case of FactoryBean, return singleton status of created object if not a dereference.
		if (mbd.isSingleton()) {
			if (isFactoryBean(beanName, mbd)) {
				if (BeanFactoryUtils.isFactoryDereference(name)) {
					return true;
				}
				FactoryBean<?> factoryBean = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
				return factoryBean.isSingleton();
			} else {
				return !BeanFactoryUtils.isFactoryDereference(name);
			}
		} else {
			return false;
		}
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);

		BeanFactory parentBeanFactory = getParentBeanFactory();
		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
			// No bean definition found in this factory -> delegate to parent.
			return parentBeanFactory.isPrototype(originalBeanName(name));
		}

		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
		if (mbd.isPrototype()) {
			// In case of FactoryBean, return singleton status of created object if not a dereference.
			return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(beanName, mbd));
		}

		// Singleton or scoped - not a prototype.
		// However, FactoryBean may still produce a prototype object...
		if (BeanFactoryUtils.isFactoryDereference(name)) {
			return false;
		}
		if (isFactoryBean(beanName, mbd)) {
			FactoryBean<?> fb = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
			if (System.getSecurityManager() != null) {
				return AccessController.doPrivileged(
						(PrivilegedAction<Boolean>) () ->
								((fb instanceof SmartFactoryBean && ((SmartFactoryBean<?>) fb).isPrototype()) ||
										!fb.isSingleton()),
						getAccessControlContext());
			} else {
				return ((fb instanceof SmartFactoryBean && ((SmartFactoryBean<?>) fb).isPrototype()) ||
						!fb.isSingleton());
			}
		} else {
			return false;
		}
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		return isTypeMatch(name, typeToMatch, true);
	}

	/**
	 * Internal extended variant of {@link #isTypeMatch(String, ResolvableType)}
	 * to check whether the bean with the given name matches the specified type. Allow
	 * additional constraints to be applied to ensure that beans are not created early.
	 *
	 * @param name        bean名称 to query
	 * @param typeToMatch the type to match against (as a
	 *                    {@code ResolvableType})
	 * @return {@code true} if the bean type matches, {@code false} if it
	 * doesn't match or cannot be determined yet
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @see #getBean
	 * @see #getType
	 * @since 5.2
	 */
	protected boolean isTypeMatch(String name, ResolvableType typeToMatch, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException {

		String beanName = transformedBeanName(name);
		boolean isFactoryDereference = BeanFactoryUtils.isFactoryDereference(name);

		// Check manually registered singletons.
		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null && beanInstance.getClass() != NullBean.class) {
			if (beanInstance instanceof FactoryBean) {
				if (!isFactoryDereference) {
					Class<?> type = getTypeForFactoryBean((FactoryBean<?>) beanInstance);
					return (type != null && typeToMatch.isAssignableFrom(type));
				} else {
					return typeToMatch.isInstance(beanInstance);
				}
			} else if (!isFactoryDereference) {
				if (typeToMatch.isInstance(beanInstance)) {
					// Direct match for exposed instance?
					return true;
				} else if (typeToMatch.hasGenerics() && containsBeanDefinition(beanName)) {
					// Generics potentially only match on the target class, not on the proxy...
					RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
					Class<?> targetType = mbd.getTargetType();
					if (targetType != null && targetType != ClassUtils.getUserClass(beanInstance)) {
						// Check raw class match as well, making sure it's exposed on the proxy.
						Class<?> classToMatch = typeToMatch.resolve();
						if (classToMatch != null && !classToMatch.isInstance(beanInstance)) {
							return false;
						}
						if (typeToMatch.isAssignableFrom(targetType)) {
							return true;
						}
					}
					ResolvableType resolvableType = mbd.targetType;
					if (resolvableType == null) {
						resolvableType = mbd.factoryMethodReturnType;
					}
					return (resolvableType != null && typeToMatch.isAssignableFrom(resolvableType));
				}
			}
			return false;
		} else if (containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
			// null instance registered
			return false;
		}

		// No singleton instance found -> check bean definition.
		BeanFactory parentBeanFactory = getParentBeanFactory();
		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
			// No bean definition found in this factory -> delegate to parent.
			return parentBeanFactory.isTypeMatch(originalBeanName(name), typeToMatch);
		}

		// Retrieve corresponding bean definition.
		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
		BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();

		// Setup the types that we want to match against
		Class<?> classToMatch = typeToMatch.resolve();
		if (classToMatch == null) {
			classToMatch = FactoryBean.class;
		}
		Class<?>[] typesToMatch = (FactoryBean.class == classToMatch ?
				new Class<?>[]{classToMatch} : new Class<?>[]{FactoryBean.class, classToMatch});


		// Attempt to predict the bean type
		Class<?> predictedType = null;

		// We're looking for a regular reference but we're a factory bean that has
		// a decorated bean definition. The target bean should be the same type
		// as FactoryBean would ultimately return.
		if (!isFactoryDereference && dbd != null && isFactoryBean(beanName, mbd)) {
			// We should only attempt if the user explicitly set lazy-init to true
			// and we know the merged bean definition is for a factory bean.
			if (!mbd.isLazyInit() || allowFactoryBeanInit) {
				RootBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
				Class<?> targetType = predictBeanType(dbd.getBeanName(), tbd, typesToMatch);
				if (targetType != null && !FactoryBean.class.isAssignableFrom(targetType)) {
					predictedType = targetType;
				}
			}
		}

		// If we couldn't use the target type, try regular prediction.
		if (predictedType == null) {
			predictedType = predictBeanType(beanName, mbd, typesToMatch);
			if (predictedType == null) {
				return false;
			}
		}

		// Attempt to get the actual ResolvableType for the bean.
		ResolvableType beanType = null;

		// If it's a FactoryBean, we want to look at what it creates, not the factory class.
		if (FactoryBean.class.isAssignableFrom(predictedType)) {
			if (beanInstance == null && !isFactoryDereference) {
				beanType = getTypeForFactoryBean(beanName, mbd, allowFactoryBeanInit);
				predictedType = beanType.resolve();
				if (predictedType == null) {
					return false;
				}
			}
		} else if (isFactoryDereference) {
			// Special case: A SmartInstantiationAwareBeanPostProcessor returned a non-FactoryBean
			// type but we nevertheless are being asked to dereference a FactoryBean...
			// Let's check the original bean class and proceed with it if it is a FactoryBean.
			predictedType = predictBeanType(beanName, mbd, FactoryBean.class);
			if (predictedType == null || !FactoryBean.class.isAssignableFrom(predictedType)) {
				return false;
			}
		}

		// We don't have an exact type but if bean definition target type or the factory
		// method return type matches the predicted type then we can use that.
		if (beanType == null) {
			ResolvableType definedType = mbd.targetType;
			if (definedType == null) {
				definedType = mbd.factoryMethodReturnType;
			}
			if (definedType != null && definedType.resolve() == predictedType) {
				beanType = definedType;
			}
		}

		// If we have a bean type use it so that generics are considered
		if (beanType != null) {
			return typeToMatch.isAssignableFrom(beanType);
		}

		// If we don't have a bean type, fallback to the predicted type
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
		String beanName = transformedBeanName(name);
		List<String> aliases = new ArrayList<>();
		boolean factoryPrefix = name.startsWith(FACTORY_BEAN_PREFIX);
		String fullBeanName = beanName;
		if (factoryPrefix) {
			fullBeanName = FACTORY_BEAN_PREFIX + beanName;
		}
		if (!fullBeanName.equals(name)) {
			aliases.add(fullBeanName);
		}
		String[] retrievedAliases = super.getAliases(beanName);
		String prefix = factoryPrefix ? FACTORY_BEAN_PREFIX : "";
		for (String retrievedAlias : retrievedAliases) {
			String alias = prefix + retrievedAlias;
			if (!alias.equals(name)) {
				aliases.add(alias);
			}
		}
		if (!containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null) {
				aliases.addAll(Arrays.asList(parentBeanFactory.getAliases(fullBeanName)));
			}
		}
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
	 * Return the set of PropertyEditorRegistrars.
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
	 * Return the map of custom editors, with Classes as keys and PropertyEditor classes as values.
	 */
	public Map<Class<?>, Class<? extends PropertyEditor>> getCustomEditors() {
		return this.customEditors;
	}

	@Override
	public void setTypeConverter(TypeConverter typeConverter) {
		this.typeConverter = typeConverter;
	}

	/**
	 * Return the custom TypeConverter to use, if any.
	 *
	 * @return the custom TypeConverter, or {@code null} if none specified
	 */
	@Nullable
	protected TypeConverter getCustomTypeConverter() {
		return this.typeConverter;
	}

	@Override
	public TypeConverter getTypeConverter() {
		TypeConverter customConverter = getCustomTypeConverter();
		if (customConverter != null) {
			return customConverter;
		} else {
			// Build default TypeConverter, registering custom editors.
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
			return null;
		}
		String result = value;
		for (StringValueResolver resolver : this.embeddedValueResolvers) {
			result = resolver.resolveStringValue(result);
			if (result == null) {
				return null;
			}
		}
		return result;
	}

	@Override
	public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
		Assert.notNull(beanPostProcessor, "BeanPostProcessor must not be null");
		// Remove from old position, if any
		//从原来的位置去除，添加到列表的末尾
		this.beanPostProcessors.remove(beanPostProcessor);
		// Add to end of list
		this.beanPostProcessors.add(beanPostProcessor);
	}

	/**
	 * Add new BeanPostProcessors that will get applied to beans created
	 * by this factory. To be invoked during factory configuration.
	 *
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
	 * Return whether this factory holds a DestructionAwareBeanPostProcessor
	 * that will get applied to singleton beans on shutdown.
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
		if (SCOPE_SINGLETON.equals(scopeName) || SCOPE_PROTOTYPE.equals(scopeName)) {
			throw new IllegalArgumentException("Cannot replace existing scopes 'singleton' and 'prototype'");
		}
		Scope previous = this.scopes.put(scopeName, scope);
		if (previous != null && previous != scope) {
			if (logger.isDebugEnabled()) {
				logger.debug("Replacing scope '" + scopeName + "' from [" + previous + "] to [" + scope + "]");
			}
		} else {
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
	 * Set the security context provider for this bean factory. If a security manager
	 * is set, interaction with the user code will be executed using the privileged
	 * of the provided security context.
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
		Assert.notNull(otherFactory, "BeanFactory must not be null");
		setBeanClassLoader(otherFactory.getBeanClassLoader());
		setCacheBeanMetadata(otherFactory.isCacheBeanMetadata());
		setBeanExpressionResolver(otherFactory.getBeanExpressionResolver());
		setConversionService(otherFactory.getConversionService());
		if (otherFactory instanceof AbstractBeanFactory) {
			AbstractBeanFactory otherAbstractFactory = (AbstractBeanFactory) otherFactory;
			this.propertyEditorRegistrars.addAll(otherAbstractFactory.propertyEditorRegistrars);
			this.customEditors.putAll(otherAbstractFactory.customEditors);
			this.typeConverter = otherAbstractFactory.typeConverter;
			this.beanPostProcessors.addAll(otherAbstractFactory.beanPostProcessors);
			this.scopes.putAll(otherAbstractFactory.scopes);
			this.securityContextProvider = otherAbstractFactory.securityContextProvider;
		} else {
			setTypeConverter(otherFactory.getTypeConverter());
			String[] otherScopeNames = otherFactory.getRegisteredScopeNames();
			for (String scopeName : otherScopeNames) {
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
		String beanName = transformedBeanName(name);
		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null) {
			return (beanInstance instanceof FactoryBean);
		}
		// No singleton instance found -> check bean definition.
		if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
			// No bean definition found in this factory -> delegate to parent.
			return ((ConfigurableBeanFactory) getParentBeanFactory()).isFactoryBean(name);
		}
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
	 * Callback before prototype creation.
	 * <p>The default implementation register the prototype as currently in creation.
	 *
	 * @param beanName the name of the prototype about to be created
	 * @see #isPrototypeCurrentlyInCreation
	 */
	@SuppressWarnings("unchecked")
	protected void beforePrototypeCreation(String beanName) {
		Object curVal = this.prototypesCurrentlyInCreation.get();
		if (curVal == null) {
			this.prototypesCurrentlyInCreation.set(beanName);
		} else if (curVal instanceof String) {
			Set<String> beanNameSet = new HashSet<>(2);
			beanNameSet.add((String) curVal);
			beanNameSet.add(beanName);
			this.prototypesCurrentlyInCreation.set(beanNameSet);
		} else {
			Set<String> beanNameSet = (Set<String>) curVal;
			beanNameSet.add(beanName);
		}
	}

	/**
	 * Callback after prototype creation.
	 * <p>The default implementation marks the prototype as not in creation anymore.
	 *
	 * @param beanName the name of the prototype that has been created
	 * @see #isPrototypeCurrentlyInCreation
	 */
	@SuppressWarnings("unchecked")
	protected void afterPrototypeCreation(String beanName) {
		Object curVal = this.prototypesCurrentlyInCreation.get();
		if (curVal instanceof String) {
			this.prototypesCurrentlyInCreation.remove();
		} else if (curVal instanceof Set) {
			Set<String> beanNameSet = (Set<String>) curVal;
			beanNameSet.remove(beanName);
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
	 * Destroy the given bean instance (usually a prototype instance
	 * obtained from this factory) according to the given bean definition.
	 *
	 * @param beanName bean名称 definition
	 * @param bean     the bean instance to destroy
	 * @param mbd      the merged bean definition
	 */
	protected void destroyBean(String beanName, Object bean, RootBeanDefinition mbd) {
		new DisposableBeanAdapter(
				bean, beanName, mbd, getBeanPostProcessorCache().destructionAware, getAccessControlContext()).destroy();
	}

	@Override
	public void destroyScopedBean(String beanName) {
		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
		if (mbd.isSingleton() || mbd.isPrototype()) {
			throw new IllegalArgumentException(
					"Bean name '" + beanName + "' does not correspond to an object in a mutable scope");
		}
		String scopeName = mbd.getScope();
		Scope scope = this.scopes.get(scopeName);
		if (scope == null) {
			throw new IllegalStateException("No Scope SPI registered for scope name '" + scopeName + "'");
		}
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
	 * Initialize the given BeanWrapper with the custom editors registered
	 * with this factory. To be called for BeanWrappers that will create
	 * and populate bean instances.
	 * <p>The default implementation delegates to {@link #registerCustomEditors}.
	 * Can be overridden in subclasses.
	 *
	 * @param bw the BeanWrapper to initialize
	 */
	protected void initBeanWrapper(BeanWrapper bw) {
		bw.setConversionService(getConversionService());
		registerCustomEditors(bw);
	}

	/**
	 * Initialize the given PropertyEditorRegistry with the custom editors
	 * that have been registered with this BeanFactory.
	 * <p>To be called for BeanWrappers that will create and populate bean
	 * instances, and for SimpleTypeConverter used for constructor argument
	 * and factory method type conversion.
	 *
	 * @param registry the PropertyEditorRegistry to initialize
	 */
	protected void registerCustomEditors(PropertyEditorRegistry registry) {
		if (registry instanceof PropertyEditorRegistrySupport) {
			((PropertyEditorRegistrySupport) registry).useConfigValueEditors();
		}
		if (!this.propertyEditorRegistrars.isEmpty()) {
			for (PropertyEditorRegistrar registrar : this.propertyEditorRegistrars) {
				try {
					registrar.registerCustomEditors(registry);
				} catch (BeanCreationException ex) {
					Throwable rootCause = ex.getMostSpecificCause();
					if (rootCause instanceof BeanCurrentlyInCreationException) {
						BeanCreationException bce = (BeanCreationException) rootCause;
						String bceBeanName = bce.getBeanName();
						if (bceBeanName != null && isCurrentlyInCreation(bceBeanName)) {
							if (logger.isDebugEnabled()) {
								logger.debug("PropertyEditorRegistrar [" + registrar.getClass().getName() +
										"] failed because it tried to obtain currently created bean '" +
										ex.getBeanName() + "': " + ex.getMessage());
							}
							onSuppressedException(ex);
							continue;
						}
					}
					throw ex;
				}
			}
		}
		if (!this.customEditors.isEmpty()) {
			this.customEditors.forEach((requiredType, editorClass) ->
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
	 * Clear the merged bean definition cache, removing entries for beans
	 * which are not considered eligible for full metadata caching yet.
	 * <p>Typically triggered after changes to the original bean definitions,
	 * e.g. after applying a {@code BeanFactoryPostProcessor}. Note that metadata
	 * for beans which have already been created at this point will be kept around.
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
	 * Check whether the given bean is defined as a {@link FactoryBean}.
	 *
	 * @param beanName bean名称
	 * @param mbd      the corresponding bean definition
	 */
	protected boolean isFactoryBean(String beanName, RootBeanDefinition mbd) {
		Boolean result = mbd.isFactoryBean;
		if (result == null) {
			Class<?> beanType = predictBeanType(beanName, mbd, FactoryBean.class);
			result = (beanType != null && FactoryBean.class.isAssignableFrom(beanType));
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
	 * Determine the bean type for the given FactoryBean definition, as far as possible.
	 * Only called if there is no singleton instance registered for the target bean already.
	 * <p>The default implementation creates the FactoryBean via {@code getBean}
	 * to call its {@code getObjectType} method. Subclasses are encouraged to optimize
	 * this, typically by just instantiating the FactoryBean but not populating it yet,
	 * trying whether its {@code getObjectType} method already returns a type.
	 * If no type found, a full FactoryBean creation as performed by this implementation
	 * should be used as fallback.
	 *
	 * @param beanName bean名称
	 * @param mbd      bean的合并bean定义
	 * @return the type for the bean if determinable, or {@code null} otherwise
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 * @see #getBean(String)
	 * @deprecated since 5.2 in favor of {@link #getTypeForFactoryBean(String, RootBeanDefinition, boolean)}
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
	 * Determine whether the specified bean is eligible for having
	 * its bean definition metadata cached.
	 *
	 * @param beanName bean名称
	 * @return {@code true} if the bean's metadata may be cached
	 * at this point already
	 */
	protected boolean isBeanEligibleForMetadataCaching(String beanName) {
		return this.alreadyCreated.contains(beanName);
	}

	/**
	 * Remove the singleton instance (if any) for the given bean name,
	 * but only if it hasn't been used for other purposes than type checking.
	 *
	 * @param beanName bean名称
	 * @return {@code true} if actually removed, {@code false} otherwise
	 */
	protected boolean removeSingletonIfCreatedForTypeCheckOnly(String beanName) {
		if (!this.alreadyCreated.contains(beanName)) {
			removeSingleton(beanName);
			return true;
		} else {
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
	 * Determine whether the given bean name is already in use within this factory,
	 * i.e. whether there is a local bean or alias registered under this name or
	 * an inner bean created with this name.
	 *
	 * @param beanName the name to check
	 */
	public boolean isBeanNameInUse(String beanName) {
		return isAlias(beanName) || containsLocalBean(beanName) || hasDependentBean(beanName);
	}

	/**
	 * Determine whether the given bean requires destruction on shutdown.
	 * <p>The default implementation checks the DisposableBean interface as well as
	 * a specified destroy method and registered DestructionAwareBeanPostProcessors.
	 *
	 * @param bean the bean instance to check
	 * @param mbd  the corresponding bean definition
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
	 * Add the given bean to the list of disposable beans in this factory,
	 * registering its DisposableBean interface and/or the given destroy method
	 * to be called on factory shutdown (if applicable). Only applies to singletons.
	 *
	 * @param beanName bean名称
	 * @param bean     the bean instance
	 * @param mbd      the bean definition for the bean
	 * @see RootBeanDefinition#isSingleton
	 * @see RootBeanDefinition#getDependsOn
	 * @see #registerDisposableBean
	 * @see #registerDependentBean
	 */
	protected void registerDisposableBeanIfNecessary(String beanName, Object bean, RootBeanDefinition mbd) {
		AccessControlContext acc = (System.getSecurityManager() != null ? getAccessControlContext() : null);
		if (!mbd.isPrototype() && requiresDestruction(bean, mbd)) {
			if (mbd.isSingleton()) {
				// Register a DisposableBean implementation that performs all destruction
				// work for the given bean: DestructionAwareBeanPostProcessors,
				// DisposableBean interface, custom destroy method.
				registerDisposableBean(beanName, new DisposableBeanAdapter(
						bean, beanName, mbd, getBeanPostProcessorCache().destructionAware, acc));
			} else {
				// A bean with a custom scope...
				Scope scope = this.scopes.get(mbd.getScope());
				if (scope == null) {
					throw new IllegalStateException("No Scope registered for scope name '" + mbd.getScope() + "'");
				}
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
	 * CopyOnWriteArrayList which resets the beanPostProcessorCache field on modification.
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
