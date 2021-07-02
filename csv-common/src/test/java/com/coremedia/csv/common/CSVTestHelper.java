package com.coremedia.csv.common;


import com.coremedia.cap.common.CapPropertyDescriptor;
import com.coremedia.cap.common.CapPropertyDescriptorType;
import com.coremedia.cap.common.IdHelper;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.content.ContentType;
import com.coremedia.objectserver.beans.ContentBean;
import com.coremedia.objectserver.beans.ContentBeanFactory;
import com.coremedia.xml.Markup;
import com.coremedia.xml.MarkupFactory;
import static org.mockito.Mockito.*;
import com.coremedia.blueprint.cae.contentbeans.CMTaxonomyImpl;
import com.coremedia.blueprint.common.contentbeans.CMTaxonomy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;


public class CSVTestHelper {
  private List<CMTaxonomy> currentTaxonomyPathList = new ArrayList<>();

  public Content generateContentWithProperty(String propertyName, CapPropertyDescriptorType propertyType,
                                             Object propertyValue, ContentRepository contentRepository) {
    Content content = mock(Content.class);
    ContentType contentType = mock(ContentType.class);
    CapPropertyDescriptor descriptor = mock(CapPropertyDescriptor.class);

    when(content.getId()).thenReturn(IdHelper.formatContentId(generateRandomContentId()));

    when(descriptor.getType()).thenReturn(propertyType);
    when(contentType.getDescriptor(propertyName)).thenReturn(descriptor);
    when(content.get(propertyName)).thenReturn(propertyValue);
    when(content.getType()).thenReturn(contentType);
    when(content.getRepository()).thenReturn(contentRepository);

    return content;
  }

  public Content generateContentWithFunction(Function<Content, String> function, String value) {
    Content content = mock(Content.class);
    when(content.getId()).thenReturn(IdHelper.formatContentId(generateRandomContentId()));
    when(function.apply(content)).thenReturn(value);
    return content;
  }

  public <T extends ContentBean> T generateContentBean(Class<T> type, Content content) {
    T bean = mock(type);
    when(bean.getContent()).thenReturn(content);
    return bean;
  }

  public CMTaxonomy generateCMTaxonomy(Content content) {
    CMTaxonomyImpl taxonomy = generateContentBean(CMTaxonomyImpl.class, content);
    currentTaxonomyPathList.add(taxonomy);
    List<CMTaxonomy> tempList = new ArrayList<>(currentTaxonomyPathList);
    doReturn(tempList).when(taxonomy).getTaxonomyPathList();
    return taxonomy;
  }

  public ContentBeanFactory generateContentBeanFactory(Map<String, List<Content>> taxonomyStructure) {
    ContentBeanFactory factory = mock(ContentBeanFactory.class);

    for (String taxSet : taxonomyStructure.keySet()) {
      List<Content> taxonomies = taxonomyStructure.get(taxSet);
      for (Content taxonomy : taxonomies) {
        CMTaxonomy tax = generateCMTaxonomy(taxonomy);
        when(factory.createBeanFor(taxonomy, CMTaxonomy.class)).thenReturn(tax);
      }
      resetCurrentTaxonomyPathList();
    }

    return factory;
  }

  public Markup toMarkup(String testString) {
    Markup markup = null;
    if (testString != null) {
      String markupPrefix = "<div xmlns=\"http://www.coremedia.com/2003/richtext-1.0\" " +
              "xmlns:xlink=\"http://www.w3.org/1999/xlink\">";
      if (!testString.contains(markupPrefix)) {
        testString = markupPrefix + testString + "</div>";
      }
      markup = MarkupFactory.fromString(testString).withGrammar("coremedia-richtext-1.0");
    }
    return markup;
  }

//  public <T> T mockAndInjectInto(Class<T> targetClass, Object injectionTarget) {
//    T blobConverter = mock(targetClass);
//    inject(injectionTarget, blobConverter);
//    return blobConverter;
//  }

  private void resetCurrentTaxonomyPathList() {
    currentTaxonomyPathList = new ArrayList<>();
  }

  private int generateRandomContentId() {
    return new Random().nextInt(1000);
  }
}
