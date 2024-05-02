package com.coremedia.csv.studio;

import com.coremedia.cap.content.ContentRepository;
import com.coremedia.rest.cap.CapRestServiceBaseConfiguration;
import com.coremedia.rest.cap.CapRestServiceSearchConfiguration;
import com.coremedia.rest.cap.content.search.CapObjectFormat;
import com.coremedia.rest.cap.content.search.SearchService;
import com.coremedia.rest.linking.LinkResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

@Configuration
@Import({
        CapRestServiceBaseConfiguration.class,
        CapRestServiceSearchConfiguration.class
})
@EnableConfigurationProperties({
        CSVConfigurationProperties.class
})
class CSVConfiguration {

  @Bean
  public CSVExportAuthorization csvExportAuthorization(ContentRepository contentRepository) {
    List<String> authorizedGroups = List.of("csv-reporter", "csv-reporter@cognito");
    return new CSVExportAuthorization(contentRepository, true, authorizedGroups);
  }

  @Bean
  CSVExportSearchService csvExportSearchService(ContentRepository contentRepository,
                                                SearchService searchService,
                                                CapObjectFormat capObjectFormat,
                                                LinkResolver linkResolver) {
    return new CSVExportSearchService(contentRepository, searchService, capObjectFormat, linkResolver);
  }

  @Bean
  public CSVExportResource csvExportResource(CSVExportAuthorization csvExportAuthorization,
                                             CSVExportSearchService csvExportSearchService,
                                             CSVFileRetriever csvFileRetriever) {
    return new CSVExportResource(csvExportAuthorization, csvExportSearchService, csvFileRetriever);
  }

  @Bean
  public CSVFileRetriever csvFileRetriever(CSVConfigurationProperties csvConfigurationProperties) {
    return new CSVFileRetriever(csvConfigurationProperties.getPreviewRestUrlPrefix());
  }

  @Bean
  public CSVExportJobFactory csvExportJobFactory(CSVExportAuthorization csvExportAuthorization,
                                                 CSVExportSearchService csvExportSearchService) {
    return new CSVExportJobFactory(csvExportAuthorization, csvExportSearchService);
  }
}
