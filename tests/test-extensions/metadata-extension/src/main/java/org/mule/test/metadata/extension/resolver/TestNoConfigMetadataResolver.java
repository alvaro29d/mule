/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.resolver;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyBuilder;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.MetadataAttributesResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.extension.api.introspection.metadata.NullMetadataKey;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class TestNoConfigMetadataResolver implements TypeKeysResolver, InputTypeResolver<Object>,
    OutputTypeResolver<Object>, MetadataAttributesResolver<Object> {

  @Override
  public String getCategoryName() {
    return "MetadataExtensionResolver";
  }

  @Override
  public Set<MetadataKey> getKeys(MetadataContext context) {
    return Arrays.stream(KeyIds.values()).map(e -> MetadataKeyBuilder.newKey(e.name()).build()).collect(Collectors.toSet());
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, Object key) {
    if (key instanceof NullMetadataKey) {
      return BaseTypeBuilder.create(JAVA).nullType().build();
    }

    return BaseTypeBuilder.create(JAVA).stringType().build();
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, Object key) {
    if (key instanceof NullMetadataKey) {
      return BaseTypeBuilder.create(JAVA).nullType().build();
    }

    return BaseTypeBuilder.create(JAVA).booleanType().build();
  }

  @Override
  public MetadataType getAttributesMetadata(MetadataContext context, Object key)
      throws MetadataResolvingException, ConnectionException {
    if (key instanceof NullMetadataKey) {
      return BaseTypeBuilder.create(JAVA).nullType().build();
    }

    return BaseTypeBuilder.create(JAVA).booleanType().build();
  }

  public enum KeyIds {
    BOOLEAN, STRING
  }
}
