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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.SimpleAliasRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用注册表，用于共享 bean 实例，实现了 {@link org.springframework.beans.factory.config.SingletonBeanRegistry}。
 * 允许注册应该在注册表的所有调用者之间共享的单例实例，通过 bean 名称获取。
 *
 * <p>还支持注册 {@link org.springframework.beans.factory.DisposableBean} 实例
 * （这些实例可能与注册的单例实例对应，也可能不对应），
 * 以在注册表关闭时销毁。可以注册 bean 之间的依赖关系，以强制执行适当的关闭顺序。
 *
 * <p>此类主要用作 {@link org.springframework.beans.factory.BeanFactory} 实现的基类，
 * 提取了单例 bean 实例的常见管理。注意，
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory} 接口扩展了 {@link SingletonBeanRegistry} 接口。
 *
 * <p>请注意，与 {@link AbstractBeanFactory} 和 {@link DefaultListableBeanFactory} 不同（它们从中继承），
 * 此类既不假设 bean 定义概念，也不假设特定的 bean 实例创建过程。
 * 也可以用作委托的嵌套辅助工具。
 *
 * @author Juergen Hoeller
 * @see #registerSingleton
 * @see #registerDisposableBean
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory
 * @since 2.0
 */
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {

	/**
	 * 要保留的被封锁异常的最大数量。
	 */
	private static final int SUPPRESSED_EXCEPTIONS_LIMIT = 100;


	/**
	 * 单例对象的缓存: bean名称到bean实例。
	 */
	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

	/**
	 * 单例工厂的缓存: bean名称到ObjectFactory。
	 */
	private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

	/**
	 * 早期单例对象的缓存: bean名称到bean实例。
	 * 它与 {@link #singletonFactories} 区别在于 earlySingletonObjects 中存放的 bean 不一定是完整。
	 * <p>
	 * 从 {@link #getSingleton(String)} 方法中，我们可以了解，bean 在创建过程中就已经加入到 earlySingletonObjects 中了。
	 * 所以当在 bean 的创建过程中，就可以通过 getBean() 方法获取。
	 */
	private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);

	/**
	 * 一组已注册的单例，包含按注册顺序的bean名称。
	 */
	private final Set<String> registeredSingletons = new LinkedHashSet<>(256);

	/**
	 * 当前正在创建的bean的名称。
	 */
	private final Set<String> singletonsCurrentlyInCreation =
			Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	/**
	 * 当前在创建检查中排除的bean名称。
	 */
	private final Set<String> inCreationCheckExclusions =
			Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	/**
	 * 被封锁的异常集合，可用于关联相关原因。
	 */
	@Nullable
	private Set<Exception> suppressedExceptions;

	/**
	 * 标志，表明我们目前是否在销毁单例。
	 */
	private boolean singletonsCurrentlyInDestruction = false;

	/**
	 * 一次性bean实例: bean名称到一次性的实例
	 */
	private final Map<String, Object> disposableBeans = new LinkedHashMap<>();

	/**
	 * 包含bean名称之间的映射: bean名称到bean包含的bean名称集。
	 */
	private final Map<String, Set<String>> containedBeanMap = new ConcurrentHashMap<>(16);

	/**
	 * 依赖bean名称之间的映射: bean名称到依赖bean名称集。
	 */
	private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64);

	/**
	 * 依赖的bean名称之间的映射: bean名称到依赖于它的bean名称集合的映射。
	 */
	private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<>(64);


	@Override
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		Assert.notNull(beanName, "Bean name must not be null");
		Assert.notNull(singletonObject, "Singleton object must not be null");
		//对单例Map添加锁，保证线程安全。
		synchronized (this.singletonObjects) {
			//获取单例对象对应的单例
			Object oldObject = this.singletonObjects.get(beanName);
			if (oldObject != null) {
				//如果该单例对象不为空，抛出异常，提示该bean名称已注册。
				throw new IllegalStateException("Could not register object [" + singletonObject +
						"] under bean name '" + beanName + "': there is already object [" + oldObject + "] bound");
			}
			//添加名称和单例对象到缓存中。
			addSingleton(beanName, singletonObject);
		}
	}

	/**
	 * 将给定的单例对象添加到此工厂的单例缓存中。
	 * <p> 被要求急切注册单身单例对象。
	 *
	 * @param beanName        bean名称
	 * @param singletonObject 单例对象
	 */
	protected void addSingleton(String beanName, Object singletonObject) {
		synchronized (this.singletonObjects) {
			//单例对象缓存添加该bean名称和单例对象。
			this.singletonObjects.put(beanName, singletonObject);
			//单例工厂缓存，移除该bean名称
			this.singletonFactories.remove(beanName);
			//早期单例对象的缓存，移除该bean名称
			this.earlySingletonObjects.remove(beanName);
			//已注册的单例对象，添加该bean名称
			this.registeredSingletons.add(beanName);
		}
	}

	/**
	 * 如果需要，为构建指定的单例添加给定的单例工厂。
	 * <p>用于急切注册单例，例如为了能够解析循环引用。
	 *
	 * @param beanName         bean的名称
	 * @param singletonFactory 单例对象的工厂
	 */
	protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(singletonFactory, "Singleton factory must not be null");
		// 使用同步块确保线程安全
		synchronized (this.singletonObjects) {
			// 如果单例对象集合中不包含指定名称的Bean
			if (!this.singletonObjects.containsKey(beanName)) {
				// 将单例工厂放入单例工厂集合中
				this.singletonFactories.put(beanName, singletonFactory);
				// 移除提前创建的单例对象
				this.earlySingletonObjects.remove(beanName);
				// 将Bean名称添加到已注册的单例集合中
				this.registeredSingletons.add(beanName);
			}
		}
	}

	@Override
	@Nullable
	public Object getSingleton(String beanName) {
		return getSingleton(beanName, true);
	}

	/**
	 * 返回在给定名称下注册的 (原始) 单例对象。
	 * <p> 检查已经实例化的单例，还允许对当前创建的单例进行早期引用 (解析循环引用)。
	 *
	 * @param beanName            要找的bean名称
	 * @param allowEarlyReference 是否应该创建早期引用
	 * @return 已注册的单例对象，如果找不到则为 {@code null}
	 */
	@Nullable
	protected Object getSingleton(String beanName, boolean allowEarlyReference) {
		//在没有完全单例锁的情况下，快速检查存在的实例
		//快速检查有没有单例实例
		Object singletonObject = this.singletonObjects.get(beanName);
		//从单例对象池中获取该单例，如果当前单例实例为空，并且还在创建中
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
			//就从早期单例对象池中获取该单例。
			singletonObject = this.earlySingletonObjects.get(beanName);
			//如果该单例为空，并且允许创建早期引用
			if (singletonObject == null && allowEarlyReference) {
				synchronized (this.singletonObjects) {
					//在完全单例锁中，一致性创建早期引用
					//使用双重检测锁从单例对象池中获取单例对象
					singletonObject = this.singletonObjects.get(beanName);
					//如果取不到
					if (singletonObject == null) {
						//再从早期单例单例对象池中获取。
						singletonObject = this.earlySingletonObjects.get(beanName);
						//取不到，就从单例工厂中获取单例工厂。
						if (singletonObject == null) {
							//获取单例工厂
							ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
							if (singletonFactory != null) {
								//从单例工厂中创建单例对象
								singletonObject = singletonFactory.getObject();
								//将其放入早期单例对象池中。
								this.earlySingletonObjects.put(beanName, singletonObject);
								//并将beanName从对应的单例工厂移除。
								this.singletonFactories.remove(beanName);
							}
						}
					}
				}
			}
		}
		return singletonObject;
	}

	/**
	 * 返回以给定名称注册的 (原始) 单例对象，如果尚未注册，则创建并注册一个新对象。
	 *
	 * @param beanName         bean名称
	 * @param singletonFactory 如有必要，ObjectFactory将懒加载地创建单例
	 * @return 注册的单例对象
	 */
	public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(beanName, "Bean name must not be null");
		synchronized (this.singletonObjects) {
			//从单例对象池中获取该单例对象
			Object singletonObject = this.singletonObjects.get(beanName);
			//如果从单例对象池中获取不到
			if (singletonObject == null) {
				if (this.singletonsCurrentlyInDestruction) {
					//如果当前正在销毁单例，抛出异常
					throw new BeanCreationNotAllowedException(beanName,
							"Singleton bean creation not allowed while singletons of this factory are in destruction " +
									"(Do not request a bean from a BeanFactory in a destroy method implementation!)");
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
				}
				//创建单例前，执行的方法
				beforeSingletonCreation(beanName);
				//是否是新的单例
				boolean newSingleton = false;
				//是否记录被封锁的异常
				boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
				if (recordSuppressedExceptions) {
					this.suppressedExceptions = new LinkedHashSet<>();
				}
				try {
					//从单例工厂中获取单例
					singletonObject = singletonFactory.getObject();
					//获取到了单例，将是否为单例设置为true。
					newSingleton = true;
				} catch (IllegalStateException ex) {
					// 同时是否隐式出现了 单例 对象-> 如果是，请继续进行，因为异常指示该状态。
					//通过单例对象缓存获取该bean名称对应的单例对象。
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						//如果该单例对象为空，则抛出异常
						throw ex;
					}
				} catch (BeanCreationException ex) {
					if (recordSuppressedExceptions) {
						//如果要记录封锁的异常，将太难
						for (Exception suppressedException : this.suppressedExceptions) {
							//添加相关原因
							ex.addRelatedCause(suppressedException);
						}
					}
					throw ex;
				} finally {
					if (recordSuppressedExceptions) {
						//清除封锁的异常
						this.suppressedExceptions = null;
					}
					//执行单例创建后的方法
					afterSingletonCreation(beanName);
				}
				if (newSingleton) {
					//如果是新的单例对象，添加单例对象
					addSingleton(beanName, singletonObject);
				}
			}
			return singletonObject;
		}
	}

	/**
	 * 注册在创建 bean单例实例期间碰巧被封锁的异常，例如临时循环引用解决问题。
	 * <p> 默认实现将保留此注册表的被封锁异常集合中的任何给定异常，最高限制为100异常，
	 * 并将它们作为相关原因添加到最终的顶级 {@link BeanCreationException}。
	 *
	 * @param ex 注册的异常
	 * @see BeanCreationException#getRelatedCauses()
	 */
	protected void onSuppressedException(Exception ex) {
		synchronized (this.singletonObjects) {
			if (this.suppressedExceptions != null && this.suppressedExceptions.size() < SUPPRESSED_EXCEPTIONS_LIMIT) {
				this.suppressedExceptions.add(ex);
			}
		}
	}

	/**
	 * 从该工厂的单例缓存中删除具有给定名称的bean，以便在创建失败时清理急切注册的单例。
	 *
	 * @param beanName bean名称
	 * @see #getSingletonMutex()
	 */
	protected void removeSingleton(String beanName) {
		synchronized (this.singletonObjects) {
			//将bean名称从单例对象缓存中移除
			this.singletonObjects.remove(beanName);
			//将bean名称从单例工厂缓存中移除
			this.singletonFactories.remove(beanName);
			//将bean名称从早期单例对象缓存中移除
			this.earlySingletonObjects.remove(beanName);
			//将bean名称从已注册的单例对象池中移除
			this.registeredSingletons.remove(beanName);
		}
	}

	@Override
	public boolean containsSingleton(String beanName) {
		//单例对象Map含有该bean名称
		return this.singletonObjects.containsKey(beanName);
	}

	@Override
	public String[] getSingletonNames() {
		synchronized (this.singletonObjects) {
			//返回已注册的单例bean名称数组
			return StringUtils.toStringArray(this.registeredSingletons);
		}
	}

	@Override
	public int getSingletonCount() {
		synchronized (this.singletonObjects) {
			//返回已注册的单例数量。
			return this.registeredSingletons.size();
		}
	}


	public void setCurrentlyInCreation(String beanName, boolean inCreation) {
		Assert.notNull(beanName, "Bean name must not be null");
		if (!inCreation) {
			this.inCreationCheckExclusions.add(beanName);
		} else {
			this.inCreationCheckExclusions.remove(beanName);
		}
	}

	public boolean isCurrentlyInCreation(String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		return (!this.inCreationCheckExclusions.contains(beanName) && isActuallyInCreation(beanName));
	}

	protected boolean isActuallyInCreation(String beanName) {
		return isSingletonCurrentlyInCreation(beanName);
	}

	/**
	 * 返回指定的单例 bean当前是否正在创建 (在整个工厂内)。
	 *
	 * @param beanName bean名称
	 */
	public boolean isSingletonCurrentlyInCreation(String beanName) {
		return this.singletonsCurrentlyInCreation.contains(beanName);
	}

	/**
	 * 单例创建前的回调。
	 * <p> 默认实现将当前创建中的单例注册。
	 *
	 * @param beanName 即将创建的 单例 的名称
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void beforeSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.add(beanName)) {
			//如果在创建检查中没有该对象，并且正在创建的单例中有该bean名称，抛出异常
			throw new BeanCurrentlyInCreationException(beanName);
		}
	}

	/**
	 * 单例创建后的回调。
	 * <p> 默认实现将单例标记为不再创建。
	 *
	 * @param beanName the name of the singleton that has been created
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void afterSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.remove(beanName)) {
			//如果在创建检查中排除的bean名称中没有该bean名称，且当前创建的单例中移除该bean名称失败，抛出异常
			throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
		}
	}


	/**
	 * 将给定的 bean 添加到此注册表中的可销毁 bean 列表中。
	 * <p>可销毁的 bean 通常与已注册的单例相对应，匹配 bean 名称但可能是不同的实例
	 * （例如，对于不自然实现 Spring 的 DisposableBean 接口的单例的 DisposableBean 适配器）。
	 *
	 * @param beanName bean 的名称
	 * @param bean     bean 实例
	 */
	public void registerDisposableBean(String beanName, DisposableBean bean) {
		synchronized (this.disposableBeans) {
			this.disposableBeans.put(beanName, bean);
		}
	}

	/**
	 * 注册两个 bean 之间的包含关系，
	 * 例如，内部 bean 和其包含的外部 bean 之间的关系。
	 * <p>在销毁顺序方面，还将包含的 bean 注册为依赖于包含的 bean。
	 *
	 * @param containedBeanName  包含的（内部）bean 的名称
	 * @param containingBeanName 包含的（外部）bean 的名称
	 * @see #registerDependentBean
	 */
	public void registerContainedBean(String containedBeanName, String containingBeanName) {
		// 同步块，锁定 containedBeanMap
		synchronized (this.containedBeanMap) {
			// 获取包含 Bean 名称对应的已包含 Bean 集合，如果不存在，则新建一个集合
			Set<String> containedBeans =
					this.containedBeanMap.computeIfAbsent(containingBeanName, k -> new LinkedHashSet<>(8));
			// 将被包含的 Bean 添加到集合中，如果添加失败（即已存在），则直接返回
			if (!containedBeans.add(containedBeanName)) {
				return;
			}
		}
		// 注册依赖关系，即被包含的 Bean 依赖于包含的 Bean
		registerDependentBean(containedBeanName, containingBeanName);
	}

	/**
	 * 为给定的bean注册一个依赖的bean，在给定的bean被销毁之前要销毁。
	 *
	 * @param beanName          bean名称
	 * @param dependentBeanName 依赖bean的名称
	 */
	public void registerDependentBean(String beanName, String dependentBeanName) {
		//获取规范bean名称
		String canonicalName = canonicalName(beanName);

		synchronized (this.dependentBeanMap) {
			//如果该规范名称存在，则返回该他依赖的bean名称集合，否则返回一个空的LinkedHashSet
			Set<String> dependentBeans =
					this.dependentBeanMap.computeIfAbsent(canonicalName, k -> new LinkedHashSet<>(8));
			if (!dependentBeans.add(dependentBeanName)) {
				//如果该依赖集合中不存在该依赖bean名称，直接结束
				return;
			}
		}

		synchronized (this.dependenciesForBeanMap) {
			//获取该依赖bean名称的依赖集合，不存在则创建一个LinkedHashSet
			Set<String> dependenciesForBean =
					this.dependenciesForBeanMap.computeIfAbsent(dependentBeanName, k -> new LinkedHashSet<>(8));
			//依赖于该bean名称的规范bean名称添加到依赖集合中
			dependenciesForBean.add(canonicalName);
		}
	}

	/**
	 * 确定指定的从属bean是否已被注册为依赖于给定bean或其任何传递依赖性。
	 *
	 * @param beanName          要检查的bean名称
	 * @param dependentBeanName 依赖bean的名称
	 * @since 4.0
	 */
	protected boolean isDependent(String beanName, String dependentBeanName) {
		synchronized (this.dependentBeanMap) {
			return isDependent(beanName, dependentBeanName, null);
		}
	}

	private boolean isDependent(String beanName, String dependentBeanName, @Nullable Set<String> alreadySeen) {
		if (alreadySeen != null && alreadySeen.contains(beanName)) {
			//如果存在已发现bean名称集合，且该集合中包含该bean名称。返回false
			return false;
		}
		//返回规范bean名称
		String canonicalName = canonicalName(beanName);
		//获取依赖的bean名称
		Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
		if (dependentBeans == null) {
			//没有依赖bean名称，返回false
			return false;
		}
		if (dependentBeans.contains(dependentBeanName)) {
			//有该bean名称的依赖，返回true
			return true;
		}
		for (String transitiveDependency : dependentBeans) {
			if (alreadySeen == null) {
				alreadySeen = new HashSet<>();
			}
			alreadySeen.add(beanName);
			if (isDependent(transitiveDependency, dependentBeanName, alreadySeen)) {
				//递归查找依赖的bean名称，有则返回true
				return true;
			}
		}
		return false;
	}

	/**
	 * 检查给定名称的 bean 是否已经注册了依赖的 bean。
	 *
	 * @param beanName 要检查的 bean 的名称
	 * @return 如果已经注册了依赖的 bean，则为 true；否则为 false
	 */
	protected boolean hasDependentBean(String beanName) {
		return this.dependentBeanMap.containsKey(beanName);
	}

	/**
	 * 返回所有依赖于指定 bean 的 bean 的名称（如果有）。
	 *
	 * @param beanName bean 的名称
	 * @return 依赖的 bean 名称数组，如果没有，则返回一个空数组
	 */
	public String[] getDependentBeans(String beanName) {
		// 获取依赖于指定 Bean 的所有 Bean 名称集合
		Set<String> dependentBeans = this.dependentBeanMap.get(beanName);
		// 如果依赖集合为空，则返回空数组
		if (dependentBeans == null) {
			return new String[0];
		}
		// 同步块，锁定 dependentBeanMap
		synchronized (this.dependentBeanMap) {
			// 将依赖集合转换为数组返回
			return StringUtils.toStringArray(dependentBeans);
		}
	}

	/**
	 * 返回指定 bean 依赖的所有 bean 的名称（如果有）。
	 *
	 * @param beanName bean 的名称
	 * @return 依赖的 bean 名称数组，如果没有，则返回一个空数组
	 */
	public String[] getDependenciesForBean(String beanName) {
		// 获取指定 Bean 的所有依赖的 Bean 名称集合
		Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(beanName);
		// 如果依赖集合为空，则返回空数组
		if (dependenciesForBean == null) {
			return new String[0];
		}
		// 同步块，锁定 dependenciesForBeanMap
		synchronized (this.dependenciesForBeanMap) {
			// 将依赖集合转换为数组返回
			return StringUtils.toStringArray(dependenciesForBean);
		}
	}

	public void destroySingletons() {
		if (logger.isTraceEnabled()) {
			logger.trace("Destroying singletons in " + this);
		}
		// 锁定 singletonObjects，标记当前正在销毁单例对象
		synchronized (this.singletonObjects) {
			this.singletonsCurrentlyInDestruction = true;
		}

		String[] disposableBeanNames;
		// 锁定 disposableBeans，获取所有可销毁的 Bean 名称数组
		synchronized (this.disposableBeans) {
			disposableBeanNames = StringUtils.toStringArray(this.disposableBeans.keySet());
		}
		// 逆序遍历所有可销毁的 Bean 名称数组
		for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
			// 销毁单例对象
			destroySingleton(disposableBeanNames[i]);
		}

		// 清空 containedBeanMap、dependentBeanMap 和 dependenciesForBeanMap
		this.containedBeanMap.clear();
		this.dependentBeanMap.clear();
		this.dependenciesForBeanMap.clear();

		// 清除单例对象缓存
		clearSingletonCache();
	}

	/**
	 * 清除此注册表中的所有缓存的单例实例。
	 *
	 * @since 4.3.15
	 */
	protected void clearSingletonCache() {
		synchronized (this.singletonObjects) {
			this.singletonObjects.clear();
			this.singletonFactories.clear();
			this.earlySingletonObjects.clear();
			this.registeredSingletons.clear();
			this.singletonsCurrentlyInDestruction = false;
		}
	}

	/**
	 * 销毁给定的bean。如果找到相应的一次性bean实例，则委托给 {@code destroyBean}。
	 *
	 * @param beanName bean名称
	 * @see #destroyBean
	 */
	public void destroySingleton(String beanName) {
		//删除给定名称的注册单例 (如果有)。
		removeSingleton(beanName);

		//销毁对应的 DisposableBean 实例。
		DisposableBean disposableBean;
		synchronized (this.disposableBeans) {
			//将bean名称从disposableBeans中移除
			disposableBean = (DisposableBean) this.disposableBeans.remove(beanName);
		}
		destroyBean(beanName, disposableBean);
	}

	/**
	 * 销毁给定的bean。必须在bean本身之前，销毁依赖于给定bean的bean。不应该抛出任何异常。
	 *
	 * @param beanName bean名称
	 * @param bean     要销毁的bean实例
	 */
	protected void destroyBean(String beanName, @Nullable DisposableBean bean) {
		//首先触发依赖beans的销毁...
		Set<String> dependencies;
		synchronized (this.dependentBeanMap) {
			// 在完全同步内，以保证断开的集合
			//从dependentBeanMap中将当前bean名称移除，得到依赖该bean的bean名称集合
			dependencies = this.dependentBeanMap.remove(beanName);
		}
		if (dependencies != null) {
			//如果存在依赖的bean名称集合
			if (logger.isTraceEnabled()) {
				logger.trace("Retrieved dependent beans for bean '" + beanName + "': " + dependencies);
			}
			for (String dependentBeanName : dependencies) {
				//递归调用销毁该依赖bean名称的单例
				destroySingleton(dependentBeanName);
			}
		}

		// 实际上现在销毁了bean...
		if (bean != null) {
			//如果该bean实现了DisposableBean接口
			try {
				//执行该bean的销毁方法
				bean.destroy();
			} catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Destruction of bean with name '" + beanName + "' threw an exception", ex);
				}
			}
		}

		// 触发销毁包含的bean...
		Set<String> containedBeans;
		synchronized (this.containedBeanMap) {
			//在完全同步内，以保证断开的集合
			//从containedBeanMap中将当前bean名称移除，得到该bean包含的bean名称集合
			containedBeans = this.containedBeanMap.remove(beanName);
		}
		if (containedBeans != null) {
			//如果该bean包含了其他bean
			for (String containedBeanName : containedBeans) {
				//递归调用销毁包含的bean名称的单例
				destroySingleton(containedBeanName);
			}
		}

		// 从其他bean的依赖项中删除被破坏的bean。
		synchronized (this.dependentBeanMap) {
			for (Iterator<Map.Entry<String, Set<String>>> it = this.dependentBeanMap.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<String, Set<String>> entry = it.next();
				//要清理的依赖关系
				Set<String> dependenciesToClean = entry.getValue();
				//清理所有依赖于该bean的所有关系
				dependenciesToClean.remove(beanName);
				if (dependenciesToClean.isEmpty()) {
					//如果没有要依赖的bean了，则清除该映射关系
					it.remove();
				}
			}
		}

		// 删除销毁bean时，准备好的依赖信息。
		this.dependenciesForBeanMap.remove(beanName);
	}

	/**
	 * 向子类和外部合作者公开单例互斥体。
	 * <p> 如果子类执行任何类型的扩展单例创建阶段，则它们应在给定对象上同步。
	 * 特别是，子类应 <i> 不 </i> 在单例创建中涉及自己的互斥体，以避免在惰性初始化情况下可能出现死锁。
	 */
	@Override
	public final Object getSingletonMutex() {
		//返回单例Map
		return this.singletonObjects;
	}

}
