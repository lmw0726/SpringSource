/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.convert.support;

import org.springframework.core.convert.converter.Converter;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * 从 Java 8 的 {@link java.time.ZoneId} 到 {@link java.util.TimeZone} 的简单转换器。
 *
 * <p>请注意，Spring 的默认 ConversionService 设置理解 JSR-310 {@code java.time} 包
 * 始终使用的“from”/“to”约定。该约定通过反射在 {@link ObjectToObjectConverter} 中实现，
 * 而非在特定的 JSR-310 转换器中。它也涵盖了 {@link java.util.TimeZone#toZoneId()}，
 * 以及 {@link java.util.Date#from(java.time.Instant)} 和 {@link java.util.Date#toInstant()}。
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see TimeZone#getTimeZone(java.time.ZoneId)
 */
final class ZoneIdToTimeZoneConverter implements Converter<ZoneId, TimeZone> {

	@Override
	public TimeZone convert(ZoneId source) {
		return TimeZone.getTimeZone(source);
	}

}
