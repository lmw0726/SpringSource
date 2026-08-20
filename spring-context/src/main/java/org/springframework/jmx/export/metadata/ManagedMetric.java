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

package org.springframework.jmx.export.metadata;

import org.springframework.jmx.support.MetricType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 表示将给定 bean 属性作为 JMX 属性暴露的元数据，
 * 并带有指示该属性是度量指标的附加描述符属性。
 * 仅在 JavaBean getter 上使用时有效。
 *
 * @author Jennifer Hickey
 * @since 3.0
 * @see org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler
 */
public class ManagedMetric extends AbstractJmxAttribute {

	@Nullable
	private String category;

	@Nullable
	private String displayName;

	private MetricType metricType = MetricType.GAUGE;

	private int persistPeriod = -1;

	@Nullable
	private String persistPolicy;

	@Nullable
	private String unit;


	/**
	 * 此度量指标的类别（例如：吞吐量、性能、利用率）。
	 */
	public void setCategory(@Nullable String category) {
		this.category = category;
	}

	/**
	 * 此度量指标的类别（例如：吞吐量、性能、利用率）。
	 */
	@Nullable
	public String getCategory() {
		return this.category;
	}

	/**
	 * 此度量指标的显示名称。
	 */
	public void setDisplayName(@Nullable String displayName) {
		this.displayName = displayName;
	}

	/**
	 * 此度量指标的显示名称。
	 */
	@Nullable
	public String getDisplayName() {
		return this.displayName;
	}

	/**
	 * 描述此度量指标的值随时间变化的方式。
	 */
	public void setMetricType(MetricType metricType) {
		Assert.notNull(metricType, "MetricType must not be null");
		this.metricType = metricType;
	}

	/**
	 * 描述此度量指标的值随时间变化的方式。
	 */
	public MetricType getMetricType() {
		return this.metricType;
	}

	/**
	 * 此度量指标的持久化周期。
	 */
	public void setPersistPeriod(int persistPeriod) {
		this.persistPeriod = persistPeriod;
	}

	/**
	 * 此度量指标的持久化周期。
	 */
	public int getPersistPeriod() {
		return this.persistPeriod;
	}

	/**
	 * 此度量指标的持久化策略。
	 */
	public void setPersistPolicy(@Nullable String persistPolicy) {
		this.persistPolicy = persistPolicy;
	}

	/**
	 * 此度量指标的持久化策略。
	 */
	@Nullable
	public String getPersistPolicy() {
		return this.persistPolicy;
	}

	/**
	 * 度量值的预期单位。
	 */
	public void setUnit(@Nullable String unit) {
		this.unit = unit;
	}

	/**
	 * 度量值的预期单位。
	 */
	@Nullable
	public String getUnit() {
		return this.unit;
	}

}
