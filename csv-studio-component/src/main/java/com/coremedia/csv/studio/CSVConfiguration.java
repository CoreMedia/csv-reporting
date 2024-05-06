package com.coremedia.csv.studio;

import com.coremedia.cap.content.ContentRepository;
import com.coremedia.rest.cap.CapRestServiceBaseConfiguration;
import com.coremedia.rest.cap.CapRestServiceSearchConfiguration;
import com.coremedia.rest.cap.config.StudioConfigurationProperties;
import com.coremedia.rest.cap.content.search.CapObjectFormat;
import com.coremedia.rest.cap.content.search.SearchService;
import com.coremedia.rest.linking.LinkResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

import static java.lang.invoke.MethodHandles.lookup;

@Configuration
@Import({
        CapRestServiceBaseConfiguration.class,
        CapRestServiceSearchConfiguration.class
})
@EnableConfigurationProperties({
        CSVConfigurationProperties.class,
        StudioConfigurationProperties.class
})
class CSVConfiguration {
  private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

  @Bean
  public CSVExportAuthorization csvExportAuthorization(ContentRepository contentRepository) {
    List<String> authorizedGroups = List.of("csv-reporter", "csv-reporter@cognito");
    return new CSVExportAuthorization(contentRepository, true, authorizedGroups);
  }

  @Bean
  CSVExportSearchService csvExportSearchService(ContentRepository contentRepository,
                                                SearchService searchService,
                                                CapObjectFormat capObjectFormat,
                                                LinkResolver linkResolver,
                                                CSVConfigurationProperties csvConfigurationProperties,
                                                StudioConfigurationProperties studioConfigurationProperties) {
    final StudioConfigurationProperties.Rest.SearchService searchServiceConfig = studioConfigurationProperties.getRest().getSearchService();
    int defaultItemLimit = csvConfigurationProperties.getDefaultItemLimit();
    if(defaultItemLimit > searchServiceConfig.getMaxSearchLimit()) {
      LOG.warn("defaultItemLimit of {} exceeds configured SearchService.maxSearchLimit of {}", defaultItemLimit, searchServiceConfig.getMaxSearchLimit());
    }
    return new CSVExportSearchService(contentRepository, searchService, capObjectFormat, linkResolver, defaultItemLimit);
  }

  @Bean
  public CSVExportResource csvExportResource(CSVExportAuthorization csvExportAuthorization,
                                             CSVExportSearchService csvExportSearchService,
                                             CSVFileRetriever csvFileRetriever) {
    return new CSVExportResource(csvExportAuthorization, csvExportSearchService, csvFileRetriever);
  }

  @Bean
  public CSVFileRetriever csvFileRetriever(CSVConfigurationProperties csvConfigurationProperties) {
    return new CSVFileRetriever(csvConfigurationProperties.getPreviewRestUrlPrefix(),
            csvConfigurationProperties.getBatchSize());
  }

  @Bean
  public CSVExportJobFactory csvExportJobFactory(CSVExportAuthorization csvExportAuthorization,
                                                 CSVExportSearchService csvExportSearchService,
                                                 CSVFileRetriever csvFileRetriever,
                                                 ContentRepository contentRepository) {
    return new CSVExportJobFactory(csvExportAuthorization, csvExportSearchService,
            csvFileRetriever, contentRepository);
  }
}
