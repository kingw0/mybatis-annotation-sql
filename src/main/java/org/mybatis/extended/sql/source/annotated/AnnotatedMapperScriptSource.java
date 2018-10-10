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
package org.mybatis.extended.sql.source.annotated;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.mybatis.extended.sql.annotation.Column;
import org.mybatis.extended.sql.source.ScriptSource;
import org.mybatis.extended.sql.utils.ExtendedSqlUtil;

/**
 * @author kingw
 *
 */
public class AnnotatedMapperScriptSource implements ScriptSource
{
	private static final String SPACE = " ";

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mybatis.extended.sql.source.ScriptSource#script(org.apache.ibatis.
	 * session.Configuration, java.lang.Class, java.lang.reflect.Method,
	 * java.lang.Class)
	 */
	@Override
	public String script(Configuration configuration, Class<?> mapper,
	                Method method, Class<?> parameterType)
	{
		return "<script>select count_ from count <where><if test='type &gt; 0'>type_= #{type}</if></where></script>";
	}

	private String buildSelectColumns(Map<String, String> columnMap)
	{
		StringBuilder columnsBuilder = new StringBuilder();

		for (Entry<String, String> column : columnMap.entrySet())
		{
			if (column.getKey().equals(column.getValue()))
			{
				columnsBuilder.append(column.getKey());
			}
			else
			{
				// TODO different database has different column alias,how to
				// deal with it?
				columnsBuilder.append(column.getKey()).append(SPACE).append("'")
				                .append(column.getValue()).append("'");
			}

			columnsBuilder.append(SPACE);
		}

		return columnsBuilder.toString();
	}

	private Map<String, String> getColumnMap(Configuration configuration,
	                Class<?> mapper, Method method)
	{
		Map<String, String> map = new HashMap<>();

		ResultMap resultMap = configuration.getResultMap(
		                ExtendedSqlUtil.generateResultMapName(mapper, method));

		if (resultMap != null)
		{
			List<ResultMapping> resultMappings = resultMap.getResultMappings();

			if (resultMappings != null && resultMappings.size() > 0)
			{
				for (ResultMapping resultMapping : resultMappings)
				{
					// the result map already exists,so we set the column and
					// property the same means that no need to generate column
					// alias in sql
					map.put(resultMapping.getColumn(),
					                resultMapping.getColumn());
				}

				return map;
			}
		}

		Class<?> returnType = method.getReturnType();

		Field[] fields = returnType.getFields();

		String columnName = null;

		String tempColumnName = null;

		String property = null;

		for (Field field : fields)
		{
			columnName = property = field.getName();

			if (field.isAnnotationPresent(Column.class))
			{
				tempColumnName = field.getAnnotation(Column.class).name();

				if (tempColumnName != null
				                && tempColumnName.trim().length() > 0)
				{
					columnName = tempColumnName;
				}
			}

			map.put(columnName, property);
		}

		return map;
	}

}
