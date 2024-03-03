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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.cglib.core.ClassLoaderAwareGeneratorStrategy;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.*;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * 用于在BeanFactories中使用的默认对象实例化策略。
 *
 * <p>如果容器需要通过动态生成子类覆盖方法来实现<em>方法注入</em>，则使用CGLIB。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.1
 */
public class CglibSubclassingInstantiationStrategy extends SimpleInstantiationStrategy {

	/**
	 * CGLIB回调数组中的索引，用于透传行为，在这种情况下子类不会覆盖原始类。
	 */
	private static final int PASSTHROUGH = 0;

	/**
	 * CGLIB回调数组中的索引，用于应该被覆盖以提供<em>方法查找</em>的方法。
	 */
	private static final int LOOKUP_OVERRIDE = 1;

	/**
	 * CGLIB回调数组中的索引，用于应该使用通用的<em>方法替换</em>功能进行覆盖的方法。
	 */
	private static final int METHOD_REPLACER = 2;


	@Override
	protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
		return instantiateWithMethodInjection(bd, beanName, owner, null);
	}

	@Override
	protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
													@Nullable Constructor<?> ctor, Object... args) {

		// 必须生成CGLIB子类...
		return new CglibSubclassCreator(bd, owner).instantiate(ctor, args);
	}


	/**
	 * 由于在早于 3.2 版本的 Spring 中避免外部 CGLIB 依赖而创建的内部类，出于历史原因。
	 */
	private static class CglibSubclassCreator {

		/**
		 * 回调类型数组，包括NoOp类、LookupOverrideMethodInterceptor类和ReplaceOverrideMethodInterceptor类。
		 */
		private static final Class<?>[] CALLBACK_TYPES = new Class<?>[]{
				NoOp.class, LookupOverrideMethodInterceptor.class, ReplaceOverrideMethodInterceptor.class};

		/**
		 * bean定义的根对象。
		 */
		private final RootBeanDefinition beanDefinition;

		/**
		 * 拥有者Bean工厂。
		 */
		private final BeanFactory owner;

		CglibSubclassCreator(RootBeanDefinition beanDefinition, BeanFactory owner) {
			this.beanDefinition = beanDefinition;
			this.owner = owner;
		}

		/**
		 * 创建一个实现所需查找的动态生成子类的新实例。
		 *
		 * @param ctor 构造函数。如果为 {@code null}，则使用无参构造函数（无参数化，或Setter注入）。
		 * @param args 用于构造函数的参数。如果 {@code ctor} 参数为 {@code null}，则忽略。
		 * @return 动态生成子类的新实例
		 */
		public Object instantiate(@Nullable Constructor<?> ctor, Object... args) {
			// 创建增强的子类
			Class<?> subclass = createEnhancedSubclass(this.beanDefinition);
			// 创建实例
			Object instance;
			if (ctor == null) {
				// 如果构造函数为空，则直接实例化子类
				instance = BeanUtils.instantiateClass(subclass);
			} else {
				try {
					// 获取增强子类的构造函数，并使用传入的参数实例化
					Constructor<?> enhancedSubclassConstructor = subclass.getConstructor(ctor.getParameterTypes());
					instance = enhancedSubclassConstructor.newInstance(args);
				} catch (Exception ex) {
					// 构造函数实例化失败，抛出异常
					throw new BeanInstantiationException(this.beanDefinition.getBeanClass(),
							"Failed to invoke constructor for CGLIB enhanced subclass [" + subclass.getName() + "]", ex);
				}
			}

			// SPR-10785: 直接在实例上设置回调，而不是在增强类（通过Enhancer）上设置回调，以避免内存泄漏。
			// 将回调设置到实例上
			Factory factory = (Factory) instance;
			factory.setCallbacks(new Callback[]{NoOp.INSTANCE,
					new LookupOverrideMethodInterceptor(this.beanDefinition, this.owner),
					new ReplaceOverrideMethodInterceptor(this.beanDefinition, this.owner)});
			// 返回实例
			return instance;

		}

		/**
		 * 使用CGLIB为提供的bean定义创建增强的子类。
		 *
		 * @param beanDefinition 提供的bean定义
		 * @return 增强的子类
		 */
		private Class<?> createEnhancedSubclass(RootBeanDefinition beanDefinition) {
			// 创建Enhancer实例
			Enhancer enhancer = new Enhancer();
			// 设置被增强类为目标类
			enhancer.setSuperclass(beanDefinition.getBeanClass());
			// 设置命名策略为Spring命名策略
			enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
			// 如果拥有者是可配置的Bean工厂，则设置类加载器
			if (this.owner instanceof ConfigurableBeanFactory) {
				// 获取Bean工厂的类加载器
				ClassLoader cl = ((ConfigurableBeanFactory) this.owner).getBeanClassLoader();
				// 设置Enhancer的策略为ClassLoaderAwareGeneratorStrategy
				enhancer.setStrategy(new ClassLoaderAwareGeneratorStrategy(cl));
			}
			// 设置回调过滤器为MethodOverrideCallbackFilter，用于选择要增强的方法
			enhancer.setCallbackFilter(new MethodOverrideCallbackFilter(beanDefinition));
			// 设置回调类型为CALLBACK_TYPES
			enhancer.setCallbackTypes(CALLBACK_TYPES);
			// 创建并返回增强后的类
			return enhancer.createClass();

		}
	}


	/**
	 * 该类提供了CGLIB所需的hashCode和equals方法，以确保CGLIB不会为每个bean生成一个独立的类。身份基于类和bean定义。
	 */
	private static class CglibIdentitySupport {
		/**
		 * bean定义
		 */
		private final RootBeanDefinition beanDefinition;

		public CglibIdentitySupport(RootBeanDefinition beanDefinition) {
			this.beanDefinition = beanDefinition;
		}

		public RootBeanDefinition getBeanDefinition() {
			return this.beanDefinition;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (other != null && getClass() == other.getClass() &&
					this.beanDefinition.equals(((CglibIdentitySupport) other).beanDefinition));
		}

		@Override
		public int hashCode() {
			return this.beanDefinition.hashCode();
		}
	}


	/**
	 * CGLIB回调，用于过滤方法拦截行为。
	 */
	private static class MethodOverrideCallbackFilter extends CglibIdentitySupport implements CallbackFilter {

		private static final Log logger = LogFactory.getLog(MethodOverrideCallbackFilter.class);

		public MethodOverrideCallbackFilter(RootBeanDefinition beanDefinition) {
			super(beanDefinition);
		}

		@Override
		public int accept(Method method) {
			// 获取方法的方法覆盖信息
			MethodOverride methodOverride = getBeanDefinition().getMethodOverrides().getOverride(method);
			// 如果日志级别为跟踪，则记录方法的方法覆盖信息
			if (logger.isTraceEnabled()) {
				logger.trace("MethodOverride for " + method + ": " + methodOverride);
			}
			// 根据方法覆盖信息的类型进行处理
			if (methodOverride == null) {
				// 如果方法覆盖信息为null，则返回PASSTHROUGH
				return PASSTHROUGH;
			} else if (methodOverride instanceof LookupOverride) {
				// 如果方法覆盖信息是LookupOverride类型，则返回LOOKUP_OVERRIDE
				return LOOKUP_OVERRIDE;
			} else if (methodOverride instanceof ReplaceOverride) {
				// 如果方法覆盖信息是ReplaceOverride类型，则返回METHOD_REPLACER
				return METHOD_REPLACER;
			}
			// 如果方法覆盖信息的类型是其他类型，则抛出UnsupportedOperationException异常
			throw new UnsupportedOperationException("Unexpected MethodOverride subclass: " +
					methodOverride.getClass().getName());
		}
	}


	/**
	 * CGLIB MethodInterceptor用于覆盖方法，将其替换为从容器中查找的bean的实现。
	 */
	private static class LookupOverrideMethodInterceptor extends CglibIdentitySupport implements MethodInterceptor {
		/**
		 * 拥有的bean工厂
		 */
		private final BeanFactory owner;

		public LookupOverrideMethodInterceptor(RootBeanDefinition beanDefinition, BeanFactory owner) {
			super(beanDefinition);
			this.owner = owner;
		}

		@Override
		public Object intercept(Object obj, Method method, Object[] args, MethodProxy mp) throws Throwable {
			// 类型转换是安全的，因为CallbackFilter会有选择地使用过滤器。
			LookupOverride lo = (LookupOverride) getBeanDefinition().getMethodOverrides().getOverride(method);
			// 断言LookupOverride对象不为null
			Assert.state(lo != null, "LookupOverride not found");
			// 如果参数不为空，则使用参数；如果参数为空，则不要求有参数
			Object[] argsToUse = (args.length > 0 ? args : null);
			if (StringUtils.hasText(lo.getBeanName())) {
				// 如果LookupOverride中指定了beanName
				Object bean = (argsToUse != null ? this.owner.getBean(lo.getBeanName(), argsToUse) :
						this.owner.getBean(lo.getBeanName()));
				// 通过equals(null)检查来检测包内 NullBean 实例
				return (bean.equals(null) ? null : bean);
			} else {
				// 查找与（可能是通用的）方法返回类型匹配的目标bean
				ResolvableType genericReturnType = ResolvableType.forMethodReturnType(method);
				return (argsToUse != null ? this.owner.getBeanProvider(genericReturnType).getObject(argsToUse) :
						this.owner.getBeanProvider(genericReturnType).getObject());
			}
		}
	}


	/**
	 * CGLIB MethodInterceptor用于覆盖方法，将其替换为调用通用的MethodReplacer。
	 */
	private static class ReplaceOverrideMethodInterceptor extends CglibIdentitySupport implements MethodInterceptor {

		/**
		 * 拥有的bean工厂
		 */
		private final BeanFactory owner;

		public ReplaceOverrideMethodInterceptor(RootBeanDefinition beanDefinition, BeanFactory owner) {
			super(beanDefinition);
			this.owner = owner;
		}

		@Override
		public Object intercept(Object obj, Method method, Object[] args, MethodProxy mp) throws Throwable {
			ReplaceOverride ro = (ReplaceOverride) getBeanDefinition().getMethodOverrides().getOverride(method);
			// 断言ReplaceOverride对象不为null
			Assert.state(ro != null, "ReplaceOverride not found");
			// 获取MethodReplacer对象
			// TODO: 可以缓存为单例以进行轻微的性能优化
			MethodReplacer mr = this.owner.getBean(ro.getMethodReplacerBeanName(), MethodReplacer.class);
			// 使用MethodReplacer重新实现方法
			return mr.reimplement(obj, method, args);

		}
	}

}
