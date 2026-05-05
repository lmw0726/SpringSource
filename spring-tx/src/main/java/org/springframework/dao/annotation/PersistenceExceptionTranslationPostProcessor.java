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

package org.springframework.dao.annotation;

import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;

/**
 * Bean 后处理器，自动将持久化异常转换应用于任何带有 Spring
 * {@link org.springframework.stereotype.Repository Repository} 注解标记的 bean，
 * 向暴露的代理（已有 AOP 代理或新生成的、实现目标所有接口的代理）
 * 添加相应的 {@link PersistenceExceptionTranslationAdvisor}。
 *
 * <p>将原生资源异常转换为 Spring 的
 * {@link org.springframework.dao.DataAccessException DataAccessException} 层次结构。
 * 自动检测实现
 * {@link org.springframework.dao.support.PersistenceExceptionTranslator
 * PersistenceExceptionTranslator} 接口的 bean，随后请求它们转换候选异常。
 *
 * <p>所有适用的 Spring 资源工厂（例如
 * {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean}）
 * 都开箱即用地实现了 {@code PersistenceExceptionTranslator} 接口。
 * 因此，要启用自动异常转换，通常只需用 {@code @Repository} 注解
 * 标记所有受影响的 bean（例如 Repositories 或 DAOs），并在应用程序上下文中
 * 将此后处理器定义为 bean。
 *
 * <p>自 5.3 起，{@code PersistenceExceptionTranslator} bean 将根据
 * Spring 的依赖排序规则进行排序：请参阅 {@link org.springframework.core.Ordered}
 * 和 {@link org.springframework.core.annotation.Order}。注意，自此 5.3 修订版起，
 * 这些 bean 将从任何作用域中检索，而不仅仅是 singleton 作用域。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see PersistenceExceptionTranslationAdvisor
 * @see org.springframework.stereotype.Repository
 * @see org.springframework.dao.DataAccessException
 * @see org.springframework.dao.support.PersistenceExceptionTranslator
 */
@SuppressWarnings("serial")
public class PersistenceExceptionTranslationPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor {

	private Class<? extends Annotation> repositoryAnnotationType = Repository.class;


	/**
	 * 设置 'repository' 注解类型。
	 * 默认 repository 注解类型为 {@link Repository} 注解。
	 * <p>此 setter 属性存在的目的是让开发者可以提供自己的
	 * （非 Spring 特定）注解类型，用于指示某个类具有 repository 角色。
	 * @param repositoryAnnotationType 所需的注解类型
	 */
	public void setRepositoryAnnotationType(Class<? extends Annotation> repositoryAnnotationType) {
		Assert.notNull(repositoryAnnotationType, "'repositoryAnnotationType' must not be null");
		this.repositoryAnnotationType = repositoryAnnotationType;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);

		if (!(beanFactory instanceof ListableBeanFactory)) {
			throw new IllegalArgumentException(
					"Cannot use PersistenceExceptionTranslator autodetection without ListableBeanFactory");
		}
		this.advisor = new PersistenceExceptionTranslationAdvisor(
				(ListableBeanFactory) beanFactory, this.repositoryAnnotationType);
	}

}
