/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.format.datetime.standard;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

import java.time.format.DateTimeFormatter;

/**
 * {@link FactoryBean}，用于创建一个JSR-310 {@link java.time.format.DateTimeFormatter}。
 * 有关配置详细信息，请参阅{@link DateTimeFormatterFactory base class}。
 *
 * @author Juergen Hoeller
 * @see #setPattern
 * @see #setIso
 * @see #setDateStyle
 * @see #setTimeStyle
 * @see DateTimeFormatterFactory
 * @since 4.0
 */
public class DateTimeFormatterFactoryBean extends DateTimeFormatterFactory
		implements FactoryBean<DateTimeFormatter>, InitializingBean {
	/**
	 * 日期格式化器
	 */
	@Nullable
	private DateTimeFormatter dateTimeFormatter;


	@Override
	public void afterPropertiesSet() {
		this.dateTimeFormatter = createDateTimeFormatter();
	}

	@Override
	@Nullable
	public DateTimeFormatter getObject() {
		return this.dateTimeFormatter;
	}

	@Override
	public Class<?> getObjectType() {
		return DateTimeFormatter.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
