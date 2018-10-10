package org.mybatis.extended.sql;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.Case;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Lang;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Options.FlushCachePolicy;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.annotations.TypeDiscriminator;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.FetchType;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.UnknownTypeHandler;
import org.mybatis.extended.sql.annotation.Sql;
import org.mybatis.extended.sql.provider.SqlSourceProvider;
import org.mybatis.extended.sql.utils.ExtendedSqlUtil;

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

/**
 * @author kingw
 *
 */
public class ExtendedSqlMapperScanner
{
	private Configuration configuration;

	private Map<String, SqlSourceProvider> sqlSourceProviders;

	/**
	 * 
	 */
	public ExtendedSqlMapperScanner(SqlSessionFactory sqlSessionFactory)
	{
		this.configuration = sqlSessionFactory.getConfiguration();

		this.sqlSourceProviders = new HashMap<>();
	}

	/**
	 * @param configuration
	 *            the configuration to set
	 */
	public void setConfiguration(Configuration configuration)
	{
		this.configuration = configuration;
	}

	/**
	 * @param sqlSourceProviders
	 *            the sqlSourceProviders to set
	 */
	public void setSqlSourceProviders(
	                Map<String, SqlSourceProvider> sqlSourceProviders)
	{
		this.sqlSourceProviders = sqlSourceProviders;
	}

	/**
	 * 
	 * @param name
	 * @param provider
	 */
	public void putSqlSourceProvider(String name, SqlSourceProvider provider)
	{
		this.sqlSourceProviders.put(name, provider);
	}

	/**
	 * scan for mybatis mapper
	 */
	public void scan()
	{
		Collection<Class<?>> mappers = configuration.getMapperRegistry()
		                .getMappers();

		if (mappers != null && mappers.size() > 0)
		{
			for (Class<?> mapper : mappers)
			{
				MapperBuilderAssistant assistant = new MapperBuilderAssistant(
				                configuration, getResource(mapper));

				assistant.setCurrentNamespace(mapper.getName());

				Method[] methods = mapper.getMethods();

				for (Method method : methods)
				{
					if (method.isAnnotationPresent(Sql.class))
					{
						addMappedStatement(assistant, mapper, method);
					}
				}
			}
		}
	}

	void addMappedStatement(MapperBuilderAssistant assistant, Class<?> mapper,
	                Method method)
	{
		Class<?> parameterTypeClass = getParameterType(method);
		LanguageDriver languageDriver = getLanguageDriver(assistant, method);
		SqlSource sqlSource = getSqlSourceFromAnnotations(assistant, mapper,
		                method, parameterTypeClass, languageDriver);
		if (sqlSource != null)
		{
			Options options = method.getAnnotation(Options.class);
			String mappedStatementId = getMappedStatementId(mapper, method);
			Integer fetchSize = null;
			Integer timeout = null;
			StatementType statementType = StatementType.PREPARED;
			ResultSetType resultSetType = null;
			SqlCommandType sqlCommandType = getSqlCommandType(method);
			boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
			boolean flushCache = !isSelect;
			boolean useCache = isSelect;

			KeyGenerator keyGenerator;
			String keyProperty = null;
			String keyColumn = null;
			if (SqlCommandType.INSERT.equals(sqlCommandType)
			                || SqlCommandType.UPDATE.equals(sqlCommandType))
			{
				// first check for SelectKey annotation - that overrides
				// everything else
				SelectKey selectKey = method.getAnnotation(SelectKey.class);
				if (selectKey != null)
				{
					keyGenerator = handleSelectKeyAnnotation(assistant,
					                selectKey, mappedStatementId,
					                getParameterType(method), languageDriver);
					keyProperty = selectKey.keyProperty();
				}
				else if (options == null)
				{
					keyGenerator = configuration.isUseGeneratedKeys()
					                ? Jdbc3KeyGenerator.INSTANCE
					                : NoKeyGenerator.INSTANCE;
				}
				else
				{
					keyGenerator = options.useGeneratedKeys()
					                ? Jdbc3KeyGenerator.INSTANCE
					                : NoKeyGenerator.INSTANCE;
					keyProperty = options.keyProperty();
					keyColumn = options.keyColumn();
				}
			}
			else
			{
				keyGenerator = NoKeyGenerator.INSTANCE;
			}

			if (options != null)
			{
				if (FlushCachePolicy.TRUE.equals(options.flushCache()))
				{
					flushCache = true;
				}
				else if (FlushCachePolicy.FALSE.equals(options.flushCache()))
				{
					flushCache = false;
				}
				useCache = options.useCache();
				fetchSize = options.fetchSize() > -1
				                || options.fetchSize() == Integer.MIN_VALUE
				                                ? options.fetchSize()
				                                : null; // issue #348
				timeout = options.timeout() > -1 ? options.timeout() : null;
				statementType = options.statementType();
				resultSetType = options.resultSetType();
			}

			String resultMapId = null;
			ResultMap resultMapAnnotation = method
			                .getAnnotation(ResultMap.class);
			if (resultMapAnnotation != null)
			{
				String[] resultMaps = resultMapAnnotation.value();
				StringBuilder sb = new StringBuilder();
				for (String resultMap : resultMaps)
				{
					if (sb.length() > 0)
					{
						sb.append(",");
					}
					sb.append(resultMap);
				}
				resultMapId = sb.toString();
			}
			else if (isSelect)
			{
				resultMapId = parseResultMap(assistant, mapper, method);
			}

			assistant.addMappedStatement(mappedStatementId, sqlSource,
			                statementType, sqlCommandType, fetchSize, timeout,
			                // ParameterMapID
			                null, parameterTypeClass, resultMapId,
			                getReturnType(mapper, method), resultSetType,
			                flushCache, useCache,
			                // TODO gcode issue #577
			                false, keyGenerator, keyProperty, keyColumn,
			                // DatabaseID
			                null, languageDriver,
			                // ResultSets
			                options != null ? nullOrEmpty(options.resultSets())
			                                : null);
		}
	}

	private String parseResultMap(MapperBuilderAssistant assistant,
	                Class<?> mapper, Method method)
	{
		Class<?> returnType = getReturnType(mapper, method);
		ConstructorArgs args = method.getAnnotation(ConstructorArgs.class);
		Results results = method.getAnnotation(Results.class);
		TypeDiscriminator typeDiscriminator = method
		                .getAnnotation(TypeDiscriminator.class);
		String resultMapId = ExtendedSqlUtil.generateResultMapName(mapper,
		                method);
		applyResultMap(assistant, mapper, resultMapId, returnType, argsIf(args),
		                resultsIf(results), typeDiscriminator);
		return resultMapId;
	}

	private void applyResultMap(MapperBuilderAssistant assistant,
	                Class<?> mapper, String resultMapId, Class<?> returnType,
	                Arg[] args, Result[] results,
	                TypeDiscriminator discriminator)
	{
		List<ResultMapping> resultMappings = new ArrayList<>();
		applyConstructorArgs(assistant, args, returnType, resultMappings);
		applyResults(assistant, mapper, results, returnType, resultMappings);
		Discriminator disc = applyDiscriminator(assistant, resultMapId,
		                returnType, discriminator);
		// TODO add AutoMappingBehaviour
		assistant.addResultMap(resultMapId, returnType, null, disc,
		                resultMappings, null);
		createDiscriminatorResultMaps(assistant, mapper, resultMapId,
		                returnType, discriminator);
	}

	private void createDiscriminatorResultMaps(MapperBuilderAssistant assistant,
	                Class<?> mapper, String resultMapId, Class<?> resultType,
	                TypeDiscriminator discriminator)
	{
		if (discriminator != null)
		{
			for (Case c : discriminator.cases())
			{
				String caseResultMapId = resultMapId + "-" + c.value();
				List<ResultMapping> resultMappings = new ArrayList<>();
				// issue #136
				applyConstructorArgs(assistant, c.constructArgs(), resultType,
				                resultMappings);
				applyResults(assistant, mapper, c.results(), resultType,
				                resultMappings);
				// TODO add AutoMappingBehaviour
				assistant.addResultMap(caseResultMapId, c.type(), resultMapId,
				                null, resultMappings, null);
			}
		}
	}

	private Discriminator applyDiscriminator(MapperBuilderAssistant assistant,
	                String resultMapId, Class<?> resultType,
	                TypeDiscriminator discriminator)
	{
		if (discriminator != null)
		{
			String column = discriminator.column();
			Class<?> javaType = discriminator.javaType() == void.class
			                ? String.class
			                : discriminator.javaType();
			JdbcType jdbcType = discriminator.jdbcType() == JdbcType.UNDEFINED
			                ? null
			                : discriminator.jdbcType();
			@SuppressWarnings("unchecked")
			Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>) (discriminator
			                .typeHandler() == UnknownTypeHandler.class ? null
			                                : discriminator.typeHandler());
			Case[] cases = discriminator.cases();
			Map<String, String> discriminatorMap = new HashMap<>();
			for (Case c : cases)
			{
				String value = c.value();
				String caseResultMapId = resultMapId + "-" + value;
				discriminatorMap.put(value, caseResultMapId);
			}
			return assistant.buildDiscriminator(resultType, column, javaType,
			                jdbcType, typeHandler, discriminatorMap);
		}
		return null;
	}

	private Class<?> getParameterType(Method method)
	{
		Class<?> parameterType = null;
		Class<?>[] parameterTypes = method.getParameterTypes();
		for (Class<?> currentParameterType : parameterTypes)
		{
			if (!RowBounds.class.isAssignableFrom(currentParameterType)
			                && !ResultHandler.class.isAssignableFrom(
			                                currentParameterType))
			{
				if (parameterType == null)
				{
					parameterType = currentParameterType;
				}
				else
				{
					// issue #135
					parameterType = ParamMap.class;
				}
			}
		}
		return parameterType;
	}

	private Class<?> getReturnType(Class<?> mapper, Method method)
	{
		Class<?> returnType = method.getReturnType();
		Type resolvedReturnType = TypeParameterResolver
		                .resolveReturnType(method, mapper);
		if (resolvedReturnType instanceof Class)
		{
			returnType = (Class<?>) resolvedReturnType;
			if (returnType.isArray())
			{
				returnType = returnType.getComponentType();
			}
			// gcode issue #508
			if (void.class.equals(returnType))
			{
				ResultType rt = method.getAnnotation(ResultType.class);
				if (rt != null)
				{
					returnType = rt.value();
				}
			}
		}
		else if (resolvedReturnType instanceof ParameterizedType)
		{
			ParameterizedType parameterizedType = (ParameterizedType) resolvedReturnType;
			Class<?> rawType = (Class<?>) parameterizedType.getRawType();
			if (Collection.class.isAssignableFrom(rawType)
			                || Cursor.class.isAssignableFrom(rawType))
			{
				Type[] actualTypeArguments = parameterizedType
				                .getActualTypeArguments();
				if (actualTypeArguments != null
				                && actualTypeArguments.length == 1)
				{
					Type returnTypeParameter = actualTypeArguments[0];
					if (returnTypeParameter instanceof Class<?>)
					{
						returnType = (Class<?>) returnTypeParameter;
					}
					else if (returnTypeParameter instanceof ParameterizedType)
					{
						// (gcode issue #443) actual type can be a also a
						// parameterized type
						returnType = (Class<?>) ((ParameterizedType) returnTypeParameter)
						                .getRawType();
					}
					else if (returnTypeParameter instanceof GenericArrayType)
					{
						Class<?> componentType = (Class<?>) ((GenericArrayType) returnTypeParameter)
						                .getGenericComponentType();
						// (gcode issue #525) support List<byte[]>
						returnType = Array.newInstance(componentType, 0)
						                .getClass();
					}
				}
			}
			else if (method.isAnnotationPresent(MapKey.class)
			                && Map.class.isAssignableFrom(rawType))
			{
				// (gcode issue 504) Do not look into Maps if there is not
				// MapKey annotation
				Type[] actualTypeArguments = parameterizedType
				                .getActualTypeArguments();
				if (actualTypeArguments != null
				                && actualTypeArguments.length == 2)
				{
					Type returnTypeParameter = actualTypeArguments[1];
					if (returnTypeParameter instanceof Class<?>)
					{
						returnType = (Class<?>) returnTypeParameter;
					}
					else if (returnTypeParameter instanceof ParameterizedType)
					{
						// (gcode issue 443) actual type can be a also a
						// parameterized type
						returnType = (Class<?>) ((ParameterizedType) returnTypeParameter)
						                .getRawType();
					}
				}
			}
			else if (Optional.class.equals(rawType))
			{
				Type[] actualTypeArguments = parameterizedType
				                .getActualTypeArguments();
				Type returnTypeParameter = actualTypeArguments[0];
				if (returnTypeParameter instanceof Class<?>)
				{
					returnType = (Class<?>) returnTypeParameter;
				}
			}
		}

		return returnType;
	}

	/**
	 * 
	 * @param assistant
	 * @param mapper
	 * @param method
	 * @param parameterType
	 * @param languageDriver
	 * @return
	 */
	private SqlSource getSqlSourceFromAnnotations(
	                MapperBuilderAssistant assistant, Class<?> mapper,
	                Method method, Class<?> parameterType,
	                LanguageDriver languageDriver)
	{
		Sql sql = method.getAnnotation(Sql.class);

		SqlSourceProvider provider = sqlSourceProviders.get(sql.provider());

		if (provider != null)
		{
			return provider.sqlSource(configuration, languageDriver, mapper,
			                method, parameterType);
		}

		return null;
	}

	private SqlSource buildSqlSourceFromStrings(String[] strings,
	                Class<?> parameterTypeClass, LanguageDriver languageDriver)
	{
		final StringBuilder sql = new StringBuilder();
		for (String fragment : strings)
		{
			sql.append(fragment);
			sql.append(" ");
		}
		return languageDriver.createSqlSource(configuration,
		                sql.toString().trim(), parameterTypeClass);
	}

	private SqlCommandType getSqlCommandType(Method method)
	{
		try
		{
			return method.getAnnotation(Sql.class).type();
		}
		catch (Exception e)
		{
			return SqlCommandType.UNKNOWN;
		}
	}

	private void applyResults(MapperBuilderAssistant assistant, Class<?> mapper,
	                Result[] results, Class<?> resultType,
	                List<ResultMapping> resultMappings)
	{
		for (Result result : results)
		{
			List<ResultFlag> flags = new ArrayList<>();
			if (result.id())
			{
				flags.add(ResultFlag.ID);
			}
			@SuppressWarnings("unchecked")
			Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>) ((result
			                .typeHandler() == UnknownTypeHandler.class) ? null
			                                : result.typeHandler());
			ResultMapping resultMapping = assistant.buildResultMapping(
			                resultType, nullOrEmpty(result.property()),
			                nullOrEmpty(result.column()),
			                result.javaType() == void.class ? null
			                                : result.javaType(),
			                result.jdbcType() == JdbcType.UNDEFINED ? null
			                                : result.jdbcType(),
			                hasNestedSelect(result)
			                                ? nestedSelectId(mapper, result)
			                                : null,
			                null, null, null, typeHandler, flags, null, null,
			                isLazy(result));
			resultMappings.add(resultMapping);
		}
	}

	private String nestedSelectId(Class<?> mapper, Result result)
	{
		String nestedSelect = result.one().select();
		if (nestedSelect.length() < 1)
		{
			nestedSelect = result.many().select();
		}
		if (!nestedSelect.contains("."))
		{
			nestedSelect = mapper.getName() + "." + nestedSelect;
		}
		return nestedSelect;
	}

	private boolean isLazy(Result result)
	{
		boolean isLazy = configuration.isLazyLoadingEnabled();
		if (result.one().select().length() > 0
		                && FetchType.DEFAULT != result.one().fetchType())
		{
			isLazy = result.one().fetchType() == FetchType.LAZY;
		}
		else if (result.many().select().length() > 0
		                && FetchType.DEFAULT != result.many().fetchType())
		{
			isLazy = result.many().fetchType() == FetchType.LAZY;
		}
		return isLazy;
	}

	private boolean hasNestedSelect(Result result)
	{
		if (result.one().select().length() > 0
		                && result.many().select().length() > 0)
		{
			throw new BuilderException(
			                "Cannot use both @One and @Many annotations in the same @Result");
		}
		return result.one().select().length() > 0
		                || result.many().select().length() > 0;
	}

	private void applyConstructorArgs(MapperBuilderAssistant assistant,
	                Arg[] args, Class<?> resultType,
	                List<ResultMapping> resultMappings)
	{
		for (Arg arg : args)
		{
			List<ResultFlag> flags = new ArrayList<>();
			flags.add(ResultFlag.CONSTRUCTOR);
			if (arg.id())
			{
				flags.add(ResultFlag.ID);
			}
			@SuppressWarnings("unchecked")
			Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>) (arg
			                .typeHandler() == UnknownTypeHandler.class ? null
			                                : arg.typeHandler());
			ResultMapping resultMapping = assistant.buildResultMapping(
			                resultType, nullOrEmpty(arg.name()),
			                nullOrEmpty(arg.column()),
			                arg.javaType() == void.class ? null
			                                : arg.javaType(),
			                arg.jdbcType() == JdbcType.UNDEFINED ? null
			                                : arg.jdbcType(),
			                nullOrEmpty(arg.select()),
			                nullOrEmpty(arg.resultMap()), null,
			                nullOrEmpty(arg.columnPrefix()), typeHandler, flags,
			                null, null, false);
			resultMappings.add(resultMapping);
		}
	}

	private String nullOrEmpty(String value)
	{
		return value == null || value.trim().length() == 0 ? null : value;
	}

	private Result[] resultsIf(Results results)
	{
		return results == null ? new Result[0] : results.value();
	}

	private Arg[] argsIf(ConstructorArgs args)
	{
		return args == null ? new Arg[0] : args.value();
	}

	private KeyGenerator handleSelectKeyAnnotation(
	                MapperBuilderAssistant assistant,
	                SelectKey selectKeyAnnotation, String baseStatementId,
	                Class<?> parameterTypeClass, LanguageDriver languageDriver)
	{
		String id = baseStatementId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
		Class<?> resultTypeClass = selectKeyAnnotation.resultType();
		StatementType statementType = selectKeyAnnotation.statementType();
		String keyProperty = selectKeyAnnotation.keyProperty();
		String keyColumn = selectKeyAnnotation.keyColumn();
		boolean executeBefore = selectKeyAnnotation.before();

		// defaults
		boolean useCache = false;
		KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
		Integer fetchSize = null;
		Integer timeout = null;
		boolean flushCache = false;
		String parameterMap = null;
		String resultMap = null;
		ResultSetType resultSetTypeEnum = null;

		SqlSource sqlSource = buildSqlSourceFromStrings(
		                selectKeyAnnotation.statement(), parameterTypeClass,
		                languageDriver);
		SqlCommandType sqlCommandType = SqlCommandType.SELECT;

		assistant.addMappedStatement(id, sqlSource, statementType,
		                sqlCommandType, fetchSize, timeout, parameterMap,
		                parameterTypeClass, resultMap, resultTypeClass,
		                resultSetTypeEnum, flushCache, useCache, false,
		                keyGenerator, keyProperty, keyColumn, null,
		                languageDriver, null);

		id = assistant.applyCurrentNamespace(id, false);

		MappedStatement keyStatement = configuration.getMappedStatement(id,
		                false);
		SelectKeyGenerator answer = new SelectKeyGenerator(keyStatement,
		                executeBefore);
		configuration.addKeyGenerator(id, answer);
		return answer;
	}

	private LanguageDriver getLanguageDriver(MapperBuilderAssistant assistant,
	                Method method)
	{
		Lang lang = method.getAnnotation(Lang.class);
		Class<? extends LanguageDriver> langClass = null;
		if (lang != null)
		{
			langClass = lang.value();
		}
		return assistant.getLanguageDriver(langClass);
	}

	private String getResource(Class<?> mapper)
	{
		return mapper.getName().replace('.', '/') + ".java (best guess)";
	}

	private String getMappedStatementId(Class<?> mapper, Method method)
	{
		return mapper.getName() + "." + method.getName();
	}
}
