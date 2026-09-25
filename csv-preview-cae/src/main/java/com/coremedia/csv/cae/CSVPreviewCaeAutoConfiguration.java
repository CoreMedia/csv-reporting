package com.coremedia.csv.cae;

import com.coremedia.blueprint.base.links.impl.BlueprintLinksPostprocessorsConfiguration;
import com.coremedia.blueprint.base.settings.impl.BlueprintSettingsServiceConfiguration;
import com.coremedia.blueprint.base.settings.SettingsService;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.csv.cae.handlers.ContentSetCSVHandler;
import com.coremedia.csv.cae.utils.CSVCaeCsrfIgnoringRequestMatcher;
import com.coremedia.csv.cae.utils.ContentSetCSVUtil;
import com.coremedia.csv.common.CSVConfig;
import com.coremedia.objectserver.beans.ContentBeanFactory;
import com.coremedia.objectserver.web.links.LinkFormatter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({
        BlueprintLinksPostprocessorsConfiguration.class,
        BlueprintSettingsServiceConfiguration.class,
})
public class CSVPreviewCaeAutoConfiguration {

  @Bean
  CSVCaeCsrfIgnoringRequestMatcher csvCaeCsrfIgnoringRequestMatcher() {
    return new CSVCaeCsrfIgnoringRequestMatcher();
  }

  @Bean
  ContentSetCSVUtil contentSetCSVUtil(ContentRepository contentRepository,
                                      ContentBeanFactory contentBeanFactory,
                                      SettingsService settingsService,
                                      LinkFormatter linkFormatter,
                                      CSVConfig csvConfig) {
    return new ContentSetCSVUtil(contentRepository, contentBeanFactory, settingsService, linkFormatter,
            csvConfig, 100, "CM_ContentReport_");
  }

  @Bean
  ContentSetCSVHandler contentSetCSVHandler(ContentSetCSVUtil contentSetCSVUtil, CSVConfig csvConfig) {
    return new ContentSetCSVHandler(contentSetCSVUtil, csvConfig);
  }
}
