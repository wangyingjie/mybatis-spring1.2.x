/**
 *    Copyright 2010-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.spring;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.apache.ibatis.io.JBoss6VFS;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeException;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.junit.Test;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.mybatis.spring.type.DummyTypeAlias;
import org.mybatis.spring.type.DummyTypeHandler;
import org.mybatis.spring.type.SuperType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.mockrunner.mock.jdbc.MockDataSource;

/**
 *
 * @version $Id$
 */
public final class SqlSessionFactoryBeanTest {

  private static final class TestObjectFactory extends DefaultObjectFactory {
    private static final long serialVersionUID = 1L;}
  private static final class TestObjectWrapperFactory extends DefaultObjectWrapperFactory {}

  private static MockDataSource dataSource = new MockDataSource();

  private SqlSessionFactoryBean factoryBean;

  public void setupFactoryBean() {
    factoryBean = new SqlSessionFactoryBean();
    factoryBean.setDataSource(dataSource);
  }

  @Test
  public void testDefaults() throws Exception {
    setupFactoryBean();

    assertDefaultConfig(factoryBean.getObject());
  }

  // DataSource is the only required property that does not have a default value, so test for both
  // not setting it at all and setting it to null
  @Test(expected = IllegalArgumentException.class)
  public void testNullDataSource() throws Exception {
    factoryBean = new SqlSessionFactoryBean();
    factoryBean.getObject();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetNullDataSource() throws Exception {
    factoryBean = new SqlSessionFactoryBean();
    factoryBean.setDataSource(null);
    factoryBean.getObject();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullSqlSessionFactoryBuilder() throws Exception {
    setupFactoryBean();
    factoryBean.setSqlSessionFactoryBuilder(null);
    factoryBean.getObject();
  }

  @Test
  public void testNullTransactionFactoryClass() throws Exception {
    setupFactoryBean();
    factoryBean.setTransactionFactory(null);

    assertConfig(factoryBean.getObject(), SpringManagedTransactionFactory.class);
  }

  @Test
  public void testOtherTransactionFactoryClass() throws Exception {
    setupFactoryBean();
    factoryBean.setTransactionFactory(new JdbcTransactionFactory());

    assertConfig(factoryBean.getObject(), JdbcTransactionFactory.class);
  }

  @Test
  public void testEmptyStringEnvironment() throws Exception {
    setupFactoryBean();

    factoryBean.setEnvironment("");

    assertConfig(factoryBean.getObject(), "", org.mybatis.spring.transaction.SpringManagedTransactionFactory.class);
  }

  @Test
  public void testNullConfigLocation() throws Exception {
    setupFactoryBean();
    // default should also be null, but test explicitly setting to null
    factoryBean.setConfigLocation(null);

    assertDefaultConfig(factoryBean.getObject());
  }

  @Test
  public void testSetConfigLocation() throws Exception {
    setupFactoryBean();

    factoryBean.setConfigLocation(new org.springframework.core.io.ClassPathResource(
        "org/mybatis/spring/mybatis-config.xml"));

    SqlSessionFactory factory = factoryBean.getObject();

    assertEquals(factory.getConfiguration().getEnvironment().getId(), SqlSessionFactoryBean.class.getSimpleName());
    assertSame(factory.getConfiguration().getEnvironment().getDataSource(), dataSource);
    assertSame(factory.getConfiguration().getEnvironment().getTransactionFactory().getClass(),
        org.mybatis.spring.transaction.SpringManagedTransactionFactory.class);
    assertSame(factory.getConfiguration().getVfsImpl(), JBoss6VFS.class);

    // properties explicitly set differently than the defaults in the config xml
    assertFalse(factory.getConfiguration().isCacheEnabled());
    assertTrue(factory.getConfiguration().isUseGeneratedKeys());
    assertSame(factory.getConfiguration().getDefaultExecutorType(), org.apache.ibatis.session.ExecutorType.REUSE);

    // for each statement in the xml file: org.mybatis.spring.TestMapper.xxx & xxx
    assertEquals(8, factory.getConfiguration().getMappedStatementNames().size());

    assertEquals(0, factory.getConfiguration().getResultMapNames().size());
    assertEquals(0, factory.getConfiguration().getParameterMapNames().size());
  }

  @Test
  public void testFragmentsAreReadWithMapperLocations() throws Exception {
    setupFactoryBean();

    factoryBean.setMapperLocations(new Resource[] { new ClassPathResource("org/mybatis/spring/TestMapper.xml") });

    SqlSessionFactory factory = factoryBean.getObject();

    // one for 'includedSql' and another for 'org.mybatis.spring.TestMapper.includedSql'
    assertEquals(2, factory.getConfiguration().getSqlFragments().size());
  }

  @Test
  public void testNullMapperLocations() throws Exception {
    setupFactoryBean();
    // default should also be null, but test explicitly setting to null
    factoryBean.setMapperLocations(null);

    assertDefaultConfig(factoryBean.getObject());
  }

  @Test
  public void testEmptyMapperLocations() throws Exception {
    setupFactoryBean();
    factoryBean.setMapperLocations(new org.springframework.core.io.Resource[0]);

    assertDefaultConfig(factoryBean.getObject());
  }

  @Test
  public void testMapperLocationsWithNullEntry() throws Exception {
    setupFactoryBean();
    factoryBean.setMapperLocations(new org.springframework.core.io.Resource[] { null });

    assertDefaultConfig(factoryBean.getObject());
  }

  @Test
  public void testAddATypeHandler() throws Exception {
    setupFactoryBean();
    factoryBean.setTypeHandlers(new TypeHandler[] { new DummyTypeHandler() });

    TypeHandlerRegistry typeHandlerRegistry = factoryBean.getObject().getConfiguration().getTypeHandlerRegistry();
    assertTrue(typeHandlerRegistry.hasTypeHandler(BigInteger.class));
  }

  @Test
  public void testAddATypeAlias() throws Exception {
    setupFactoryBean();

    factoryBean.setTypeAliases(new Class[] { DummyTypeAlias.class });
    TypeAliasRegistry typeAliasRegistry = factoryBean.getObject().getConfiguration().getTypeAliasRegistry();
    typeAliasRegistry.resolveAlias("testAlias");
  }

  @Test
  public void testSearchATypeAliasPackage() throws Exception {
    setupFactoryBean();
    factoryBean.setTypeAliasesPackage("org/mybatis/spring/type");

    TypeAliasRegistry typeAliasRegistry = factoryBean.getObject().getConfiguration().getTypeAliasRegistry();
    typeAliasRegistry.resolveAlias("testAlias");
    typeAliasRegistry.resolveAlias("testAlias2");
    typeAliasRegistry.resolveAlias("dummyTypeHandler");
    typeAliasRegistry.resolveAlias("superType");
  }

  @Test
  public void testSearchATypeAliasPackageWithSuperType() throws Exception {
    setupFactoryBean();
    factoryBean.setTypeAliasesSuperType(SuperType.class);
    factoryBean.setTypeAliasesPackage("org/mybatis/spring/type");

    TypeAliasRegistry typeAliasRegistry = factoryBean.getObject().getConfiguration().getTypeAliasRegistry();
    typeAliasRegistry.resolveAlias("testAlias2");
    typeAliasRegistry.resolveAlias("superType");

    try {
        typeAliasRegistry.resolveAlias("testAlias");
        fail();
    } catch (TypeException e) {
        // expected
    }

    try {
        typeAliasRegistry.resolveAlias("dummyTypeHandler");
        fail();
    } catch (TypeException e) {
        // expected
    }
  }

  @Test
  public void testSearchATypeHandlerPackage() throws Exception {
    setupFactoryBean();
    factoryBean.setTypeHandlersPackage("org/mybatis/spring/type");

    TypeHandlerRegistry typeHandlerRegistry = factoryBean.getObject().getConfiguration().getTypeHandlerRegistry();
    assertTrue(typeHandlerRegistry.hasTypeHandler(BigInteger.class));
    assertTrue(typeHandlerRegistry.hasTypeHandler(BigDecimal.class));
  }

  @Test
  public void testSetObjectFactory() throws Exception {
    setupFactoryBean();
    factoryBean.setObjectFactory(new TestObjectFactory());

    ObjectFactory objectFactory = factoryBean.getObject().getConfiguration().getObjectFactory();
    assertTrue(objectFactory instanceof TestObjectFactory);
  }

  @Test
  public void testSetObjectWrapperFactory() throws Exception {
    setupFactoryBean();
    factoryBean.setObjectWrapperFactory(new TestObjectWrapperFactory());

    ObjectWrapperFactory objectWrapperFactory = factoryBean.getObject().getConfiguration().getObjectWrapperFactory();
    assertTrue(objectWrapperFactory instanceof TestObjectWrapperFactory);
  }

  private void assertDefaultConfig(SqlSessionFactory factory) {
    assertConfig(factory, SqlSessionFactoryBean.class.getSimpleName(),
        org.mybatis.spring.transaction.SpringManagedTransactionFactory.class);
  }

  private void assertConfig(SqlSessionFactory factory, Class<? extends TransactionFactory> transactionFactoryClass) {
    assertConfig(factory, SqlSessionFactoryBean.class.getSimpleName(), transactionFactoryClass);
  }

  private void assertConfig(SqlSessionFactory factory, String environment,
      Class<? extends TransactionFactory> transactionFactoryClass) {
    assertEquals(factory.getConfiguration().getEnvironment().getId(), environment);
    assertSame(factory.getConfiguration().getEnvironment().getDataSource(), dataSource);
    assertSame(factory.getConfiguration().getEnvironment().getTransactionFactory().getClass(),
        transactionFactoryClass);

    // no mappers configured => no mapped statements or other parsed elements
    assertEquals(factory.getConfiguration().getMappedStatementNames().size(), 0);
    assertEquals(factory.getConfiguration().getResultMapNames().size(), 0);
    assertEquals(factory.getConfiguration().getParameterMapNames().size(), 0);
    assertEquals(factory.getConfiguration().getSqlFragments().size(), 0);
  }
}
