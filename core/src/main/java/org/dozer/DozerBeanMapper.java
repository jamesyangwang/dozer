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
package org.dozer;

import org.dozer.cache.CacheManager;
import org.dozer.cache.DozerCacheManager;
import org.dozer.cache.DozerCacheType;
import org.dozer.classmap.ClassMap;
import org.dozer.classmap.ClassMappings;
import org.dozer.classmap.Configuration;
import org.dozer.classmap.MappingFileData;
import org.dozer.config.GlobalSettings;
import org.dozer.factory.DestBeanCreator;
import org.dozer.loader.CustomMappingsLoader;
import org.dozer.loader.LoadMappingsResult;
import org.dozer.loader.api.BeanMappingBuilder;
import org.dozer.stats.GlobalStatistics;
import org.dozer.stats.StatisticType;
import org.dozer.stats.StatisticsInterceptor;
import org.dozer.stats.StatisticsManager;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Public Dozer Mapper implementation. This should be used/defined as a singleton within your application. This class
 * perfoms several one time initializations and loads the custom xml mappings, so you will not want to create many
 * instances of it for performance reasons. Typically a system will only have one DozerBeanMapper instance per VM. If
 * you are using an IOC framework(i.e Spring), define the Mapper as singleton="true". If you are not using an IOC
 * framework, a DozerBeanMapperSingletonWrapper convenience class has been provided in the Dozer jar.
 *
 * It is technically possible to have multiple DozerBeanMapper instances initialized, but it will hinder internal
 * performance optimizations such as caching.
 * 
 * @author tierney.matt
 * @author garsombke.franz
 */
public class DozerBeanMapper implements Mapper {

  private static final Logger log = LoggerFactory.getLogger(DozerBeanMapper.class);
  private static final StatisticsManager statsMgr = GlobalStatistics.getInstance().getStatsMgr();

  private final AtomicBoolean initialized = new AtomicBoolean(false);
  /*
   * Accessible for custom injection
   */
  private List<String> mappingFiles; // String file names
  private List<CustomConverter> customConverters;
  private List<DozerEventListener> eventListeners;
  private CustomFieldMapper customFieldMapper;
  private Map<String, CustomConverter> customConvertersWithId;
  private final List<MappingFileData> builderMappings = new ArrayList<MappingFileData>();

  /*
   * Not accessible for injection
   */
  private ClassMappings customMappings;
  private Configuration globalConfiguration;
  // There are no global caches. Caches are per bean mapper instance
  private final CacheManager cacheManager = new DozerCacheManager();

  public DozerBeanMapper() {
    this(null);
  }

  public DozerBeanMapper(List<String> mappingFiles) {
    this.mappingFiles = mappingFiles;
    init();
  }

  public void map(Object source, Object destination, String mapId) throws MappingException {
    getMappingProcessor().map(source, destination, mapId);
  }

  public <T> T map(Object source, Class<T> destinationClass, String mapId) throws MappingException {
    return getMappingProcessor().map(source, destinationClass, mapId);
  }

  public <T> T map(Object source, Class<T> destinationClass) throws MappingException {
    return getMappingProcessor().map(source, destinationClass);
  }

  public void map(Object source, Object destination) throws MappingException {
    getMappingProcessor().map(source, destination);
  }

  public List<String> getMappingFiles() {
    return mappingFiles;
  }

  public void setMappingFiles(List<String> mappingFiles) {
    this.mappingFiles = mappingFiles;
  }

  public void setFactories(Map<String, BeanFactory> factories) {
    DestBeanCreator.setStoredFactories(factories);
  }

  public void setCustomConverters(List<CustomConverter> customConverters) {
    this.customConverters = customConverters;
  }

  private void init() {
    DozerInitializer.getInstance().init();

    log.info("Initializing a new instance of dozer bean mapper.");

    // initialize any bean mapper caches. These caches are only visible to the bean mapper instance and
    // are not shared across the VM.
    cacheManager.addCache(DozerCacheType.CONVERTER_BY_DEST_TYPE.name(), GlobalSettings.getInstance()
        .getConverterByDestTypeCacheMaxSize());
    cacheManager.addCache(DozerCacheType.SUPER_TYPE_CHECK.name(), GlobalSettings.getInstance().getSuperTypesCacheMaxSize());

    // stats
    statsMgr.increment(StatisticType.MAPPER_INSTANCES_COUNT);
  }

  public void destroy() {
    DozerInitializer.getInstance().destroy();
  }

  protected Mapper getMappingProcessor() {
    if (initialized.compareAndSet(false, true)) {
      loadCustomMappings();
    }

    Mapper processor = new MappingProcessor(customMappings, globalConfiguration, cacheManager, statsMgr, customConverters,
        getEventListeners(), getCustomFieldMapper(), customConvertersWithId);

    // If statistics are enabled, then Proxy the processor with a statistics interceptor
    if (statsMgr.isStatisticsEnabled()) {
      processor = (Mapper) Proxy.newProxyInstance(processor.getClass().getClassLoader(), processor.getClass().getInterfaces(),
          new StatisticsInterceptor(processor, statsMgr));
    }

    return processor;
  }

  void loadCustomMappings() {
    CustomMappingsLoader customMappingsLoader = new CustomMappingsLoader();
    LoadMappingsResult loadMappingsResult = customMappingsLoader.load(mappingFiles, builderMappings);
    this.customMappings = loadMappingsResult.getCustomMappings();
    this.globalConfiguration = loadMappingsResult.getGlobalConfiguration();
  }

  public void addMapping(BeanMappingBuilder mappingBuilder) {
    if (initialized.get()) {
      throw new MappingException("Dozer Bean Mapper already initialized! Add custom mappings before calling map()");
    }
    MappingFileData mappingFileData = mappingBuilder.build();
    builderMappings.add(mappingFileData);
  }

  public List<DozerEventListener> getEventListeners() {
    return eventListeners;
  }

  public void setEventListeners(List<DozerEventListener> eventListeners) {
    this.eventListeners = eventListeners;
  }

  public CustomFieldMapper getCustomFieldMapper() {
    return customFieldMapper;
  }

  public void setCustomFieldMapper(CustomFieldMapper customFieldMapper) {
    this.customFieldMapper = customFieldMapper;
  }

  public Map<String, CustomConverter> getCustomConvertersWithId() {
    return customConvertersWithId;
  }

  public void setCustomConvertersWithId(Map<String, CustomConverter> customConvertersWithId) {
    this.customConvertersWithId = customConvertersWithId;
  }

}
