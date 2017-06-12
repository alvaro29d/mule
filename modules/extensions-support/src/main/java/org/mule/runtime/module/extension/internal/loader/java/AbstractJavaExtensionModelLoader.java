/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.annotation.privileged.DeclarationEnrichers;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.module.extension.internal.loader.enricher.*;
import org.mule.runtime.module.extension.internal.loader.validation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;

public class AbstractJavaExtensionModelLoader extends ExtensionModelLoader {

  public static final String TYPE_PROPERTY_NAME = "type";
  public static final String VERSION = "version";
  private final List<ExtensionModelValidator> customValidators = unmodifiableList(asList(
                                                                                         new ConfigurationModelValidator(),
                                                                                         new ConnectionProviderModelValidator(),
                                                                                         new ExportedTypesModelValidator(),
                                                                                         new JavaSubtypesModelValidator(),
                                                                                         new MetadataComponentModelValidator(),
                                                                                         new NullSafeModelValidator(),
                                                                                         new OperationReturnTypeModelValidator(),
                                                                                         new OperationParametersTypeModelValidator(),
                                                                                         new ParameterGroupModelValidator(),
                                                                                         new ParameterTypeModelValidator(),
                                                                                         new OAuthConnectionProviderModelValidator()));

  private final List<DeclarationEnricher> customDeclarationEnrichers = unmodifiableList(asList(
                                                                                               new ClassLoaderDeclarationEnricher(),
                                                                                               new JavaXmlDeclarationEnricher(),
                                                                                               new ConfigNameDeclarationEnricher(),
                                                                                               new ConnectionDeclarationEnricher(),
                                                                                               new ErrorsDeclarationEnricher(),
                                                                                               new ExtensionsErrorsDeclarationEnricher(),
                                                                                               new DataTypeDeclarationEnricher(),
                                                                                               new DisplayDeclarationEnricher(),
                                                                                               new DynamicMetadataDeclarationEnricher(),
                                                                                               new ImportedTypesDeclarationEnricher(),
                                                                                               new JavaConfigurationDeclarationEnricher(),
                                                                                               new JavaExportedTypesDeclarationEnricher(),
                                                                                               new JavaOAuthDeclarationEnricher(),
                                                                                               new SubTypesDeclarationEnricher(),
                                                                                               new ExtensionDescriptionsEnricher(),
                                                                                               new ParameterLayoutOrderDeclarationEnricher()));

  private final String id;
  private final BiFunction<Class<?>, String, ModelLoaderDelegate> delegateFactory;

  public AbstractJavaExtensionModelLoader(String id, BiFunction<Class<?>, String, ModelLoaderDelegate> delegate) {
    this.id = id;
    this.delegateFactory = delegate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getId() {
    return id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void configureContextBeforeDeclaration(ExtensionLoadingContext context) {
    context.addCustomValidators(customValidators);
    context.addCustomDeclarationEnrichers(customDeclarationEnrichers);
    context.addCustomDeclarationEnrichers(getPrivilegedDeclarationEnrichers(context));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void declareExtension(ExtensionLoadingContext context) {
    Class<?> extensionType = getExtensionType(context);
    String version =
        context.<String>getParameter(VERSION).orElseThrow(() -> new IllegalArgumentException("version not specified"));
    delegateFactory.apply(extensionType, version).declare(context);
  }

  private Collection<DeclarationEnricher> getPrivilegedDeclarationEnrichers(ExtensionLoadingContext context) {
    Class<?> extensionType = getExtensionType(context);
    try {
      // TODO: MULE-12744. If this call throws an exception it means that the extension cannot access the privileged API.
      ClassLoader extensionClassLoader = context.getExtensionClassLoader();
      Class annotation = extensionClassLoader.loadClass(DeclarationEnrichers.class.getName());
      DeclarationEnrichers enrichers = extensionType.getAnnotation((Class<DeclarationEnrichers>) annotation);
      if (enrichers != null) {
        return withContextClassLoader(extensionClassLoader,
                                      () -> stream(enrichers.value()).map(this::instantiateOrFail).collect(toList()));
      }
    } catch (ClassNotFoundException e) {
      // Do nothing
    }
    return emptyList();
  }

  private Class<?> getExtensionType(ExtensionLoadingContext context) {
    String type = context.<String>getParameter(TYPE_PROPERTY_NAME).get();
    if (isBlank(type)) {
      throw new IllegalArgumentException(format("Property '%s' has not been specified", TYPE_PROPERTY_NAME));
    }
    try {
      return loadClass(type, context.getExtensionClassLoader());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(format("Class '%s' cannot be loaded", type), e);
    }
  }

  private <R> R instantiateOrFail(Class<R> clazz) {
    try {
      return ClassUtils.instantiateClass(clazz);
    } catch (Exception e) {
      throw new IllegalArgumentException("Error instantiating class: [" + clazz + "].", e);
    }
  }
}
