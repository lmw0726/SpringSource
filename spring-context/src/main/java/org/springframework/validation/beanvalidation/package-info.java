/**
 * 用于将 JSR-303 Bean Validation 提供者（例如 Hibernate Validator）集成到
 * Spring ApplicationContext 中的支持类，特别是与 Spring 的数据绑定和验证 API 集成。
 *
 * <p>核心类是 {@link
 * org.springframework.validation.beanvalidation.LocalValidatorFactoryBean}，
 * 它定义了共享的 ValidatorFactory/Validator 设置，以便其他 Spring 组件可以使用。
 */
@NonNullApi
@NonNullFields
package org.springframework.validation.beanvalidation;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
