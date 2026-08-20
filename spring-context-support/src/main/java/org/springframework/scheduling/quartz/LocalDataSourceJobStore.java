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

package org.springframework.scheduling.quartz;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.quartz.SchedulerConfigException;
import org.quartz.impl.jdbcjobstore.JobStoreCMT;
import org.quartz.impl.jdbcjobstore.SimpleSemaphore;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.utils.ConnectionProvider;
import org.quartz.utils.DBConnectionManager;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.lang.Nullable;

/**
 * Quartz 的 {@link JobStoreCMT} 类的子类，委托给 Spring 管理的
 * {@link DataSource}，而非使用 Quartz 管理的 JDBC 连接池。
 * 如果设置了 SchedulerFactoryBean 的 "dataSource" 属性，将使用此 JobStore。
 * 也可以显式配置它，可以作为此 {@code LocalDataSourceJobStore}
 * 的自定义子类，或者作为等效的 {@code JobStoreCMT} 变体。
 *
 * <p>支持事务性和非事务性的 DataSource 访问。
 * 使用非 XA DataSource 和本地 Spring 事务时，只需提供一个 DataSource
 * 参数即可。如果使用 XA DataSource 和全局 JTA 事务，
 * 应设置 SchedulerFactoryBean 的 "nonTransactionalDataSource" 属性，
 * 传入一个不会参与全局事务的非 XA DataSource。
 *
 * <p>此 JobStore 执行的操作将正确参与任何类型的 Spring 管理的事务，
 * 因为它使用了 Spring 的 DataSourceUtils 连接处理方法，
 * 这些方法能够感知当前事务。
 *
 * <p>请注意，所有影响持久化作业存储的 Quartz Scheduler 操作
 * 通常应在活动事务中执行，因为它们假定能够获取适当的锁等。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see SchedulerFactoryBean#setDataSource
 * @see SchedulerFactoryBean#setNonTransactionalDataSource
 * @see SchedulerFactoryBean#getConfigTimeDataSource()
 * @see SchedulerFactoryBean#getConfigTimeNonTransactionalDataSource()
 * @see org.springframework.jdbc.datasource.DataSourceUtils#doGetConnection
 * @see org.springframework.jdbc.datasource.DataSourceUtils#releaseConnection
 */
@SuppressWarnings("unchecked")  // 由于 Quartz 2.2 的 JobStoreCMT 中的警告
public class LocalDataSourceJobStore extends JobStoreCMT {

	/**
	 * Quartz 事务性 ConnectionProvider 使用的名称。
	 * 此提供者将委托给本地 Spring 管理的 DataSource。
	 * @see org.quartz.utils.DBConnectionManager#addConnectionProvider
	 * @see SchedulerFactoryBean#setDataSource
	 */
	public static final String TX_DATA_SOURCE_PREFIX = "springTxDataSource.";

	/**
	 * Quartz 非事务性 ConnectionProvider 使用的名称。
	 * 此提供者将委托给本地 Spring 管理的 DataSource。
	 * @see org.quartz.utils.DBConnectionManager#addConnectionProvider
	 * @see SchedulerFactoryBean#setDataSource
	 */
	public static final String NON_TX_DATA_SOURCE_PREFIX = "springNonTxDataSource.";


	@Nullable
	private DataSource dataSource;


	@Override
	public void initialize(ClassLoadHelper loadHelper, SchedulerSignaler signaler) throws SchedulerConfigException {
		// 绝对需要线程绑定的 DataSource 来初始化。
		this.dataSource = SchedulerFactoryBean.getConfigTimeDataSource();
		if (this.dataSource == null) {
			throw new SchedulerConfigException("No local DataSource found for configuration - " +
					"'dataSource' property must be set on SchedulerFactoryBean");
		}

		// 为 Quartz 配置事务性连接设置。
		setDataSource(TX_DATA_SOURCE_PREFIX + getInstanceName());
		setDontSetAutoCommitFalse(true);

		// 为 Quartz 注册事务性 ConnectionProvider。
		DBConnectionManager.getInstance().addConnectionProvider(
				TX_DATA_SOURCE_PREFIX + getInstanceName(),
				new ConnectionProvider() {
					@Override
					public Connection getConnection() throws SQLException {
						// 返回一个事务性连接（如果有的话）。
						return DataSourceUtils.doGetConnection(dataSource);
					}
					@Override
					public void shutdown() {
						// 不做任何操作 - Spring 管理的 DataSource 有自己的生命周期。
					}
					@Override
					public void initialize() {
						// 不做任何操作 - Spring 管理的 DataSource 有自己的生命周期。
					}
				}
		);

		// 非事务性 DataSource 是可选的：如果未显式指定，
		// 则回退到默认的 DataSource。
		DataSource nonTxDataSource = SchedulerFactoryBean.getConfigTimeNonTransactionalDataSource();
		final DataSource nonTxDataSourceToUse = (nonTxDataSource != null ? nonTxDataSource : this.dataSource);

		// 为 Quartz 配置非事务性连接设置。
		setNonManagedTXDataSource(NON_TX_DATA_SOURCE_PREFIX + getInstanceName());

		// 为 Quartz 注册非事务性 ConnectionProvider。
		DBConnectionManager.getInstance().addConnectionProvider(
				NON_TX_DATA_SOURCE_PREFIX + getInstanceName(),
				new ConnectionProvider() {
					@Override
					public Connection getConnection() throws SQLException {
						// 始终返回一个非事务性连接。
						return nonTxDataSourceToUse.getConnection();
					}
					@Override
					public void shutdown() {
						// 不做任何操作 - Spring 管理的 DataSource 有自己的生命周期。
					}
					@Override
					public void initialize() {
						// 不做任何操作 - Spring 管理的 DataSource 有自己的生命周期。
					}
				}
		);

		// 如果平台是 HSQL，我们确实不想使用锁...
		try {
			String productName = JdbcUtils.extractDatabaseMetaData(this.dataSource,
					DatabaseMetaData::getDatabaseProductName);
			productName = JdbcUtils.commonDatabaseName(productName);
			if (productName != null && productName.toLowerCase().contains("hsql")) {
				setUseDBLocks(false);
				setLockHandler(new SimpleSemaphore());
			}
		}
		catch (MetaDataAccessException ex) {
			logWarnIfNonZero(1, "Could not detect database type. Assuming locks can be taken.");
		}

		super.initialize(loadHelper, signaler);

	}

	@Override
	protected void closeConnection(Connection con) {
		// 适用于事务性和非事务性连接。
		DataSourceUtils.releaseConnection(con, this.dataSource);
	}

}
