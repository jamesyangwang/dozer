/*
 * Copyright 2005-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dozer.loader.api;

import org.dozer.BeanFactory;
import org.dozer.loader.DozerBuilder;

/**
 * @author Dmitry Buzdin
 */
public class TypeDefinition {

  private String name;
  private String beanFactory;
  private String createMethod;
  private String factoryBeanId;
  private boolean mapEmptyString;
  private String mapGetMethod;
  private String mapSetMethod;
  private boolean mapNull;

  public TypeDefinition(Class<?> type) {
    this.name = type.getName();
  }

  public TypeDefinition(String name) {
    this.name = name;
  }

  public void build(DozerBuilder.ClassDefinitionBuilder typeBuilder) {
    typeBuilder.beanFactory(this.beanFactory);
    typeBuilder.createMethod(this.createMethod);
    typeBuilder.factoryBeanId(this.factoryBeanId);

    typeBuilder.mapEmptyString(this.mapEmptyString);
    typeBuilder.mapNull(this.mapNull);

    typeBuilder.mapGetMethod(this.mapGetMethod);
    typeBuilder.mapSetMethod(this.mapSetMethod);
  }

  public TypeDefinition mapMethods(String getMethod, String setMethod) {
    this.mapGetMethod = getMethod;
    this.mapSetMethod = setMethod;
    return this;
  }

  public TypeDefinition beanFactory(Class<? extends BeanFactory> type) {
    this.beanFactory = type.getName();
    return this;
  }

  public TypeDefinition beanFactory(String name) {
    this.beanFactory = name;
    return this;
  }

  public TypeDefinition createMethod(String method) {
    this.createMethod = method;
    return this;
  }

  public TypeDefinition mapMethods(String factoryBeanId) {
    this.factoryBeanId = factoryBeanId;
    return this;
  }

  public TypeDefinition mapEmptyString(boolean value) {
    this.mapEmptyString = value;
    return this;
  }

  public TypeDefinition mapNull(boolean value) {
    this.mapNull = value;
    return this;
  }

  public String getName() {
    return name;
  }

}
