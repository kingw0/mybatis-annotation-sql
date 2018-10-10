/**
 *    Copyright 2018 the original author or authors.
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
package org.mybatis.extended.sql;

import static org.junit.Assert.assertEquals;

import java.io.Reader;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mybatis.extended.sql.annotation.Sql;
import org.mybatis.extended.sql.provider.SqlSourceProvider;
import org.mybatis.extended.sql.provider.script.ScriptSqlSourceProvider;
import org.mybatis.extended.sql.source.annotated.AnnotatedMapperScriptSource;
import org.mybatis.extended.sql.utils.DatabaseUtil;

/**
 * @author kingw
 *
 */
public class ExtendedSqlMapperScannerTest
{
	private static SqlSessionFactory sqlSessionFactory;

	@BeforeClass
	public static void setUp() throws Exception
	{
		// create a SqlSessionFactory
		try (Reader reader = Resources.getResourceAsReader(
		                "org/mybatis/extended/sql/mybatis-config.xml"))
		{
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
			sqlSessionFactory.getConfiguration()
			                .addMapper(SqlProviderTestMapper.class);

			ExtendedSqlMapperScanner scanner = new ExtendedSqlMapperScanner(
			                sqlSessionFactory);

			ScriptSqlSourceProvider provider = new ScriptSqlSourceProvider();
			provider.setScriptSource(new AnnotatedMapperScriptSource());

			scanner.putSqlSourceProvider(
			                SqlSourceProvider.DEFAULT_SQLSOURCE_PROVIDER,
			                provider);

			scanner.scan();
		}

		// populate in-memory database
		DatabaseUtil.runScript(
		                sqlSessionFactory.getConfiguration().getEnvironment()
		                                .getDataSource(),
		                "org/mybatis/extended/sql/CreateDB.sql");
	}

	@Test
	public void testDefault()
	{
		try (SqlSession sqlSession = sqlSessionFactory.openSession())
		{
			SqlProviderTestMapper mapper = sqlSession
			                .getMapper(SqlProviderTestMapper.class);

			List<Map<String, Integer>> result = mapper.count(2);

			assertEquals(1, result.size());
		}
	}

	public interface SqlProviderTestMapper
	{
		@ResultType(Map.class)
		@Sql
		List<Map<String, Integer>> count(@Param("type") Integer type);
	}
}
