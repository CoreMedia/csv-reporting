package com.coremedia.csv.test;

import com.coremedia.cap.common.CapPropertyDescriptor;
import com.coremedia.cap.common.CapPropertyDescriptorType;
import com.coremedia.cap.common.IdHelper;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.content.ContentType;
import com.coremedia.cotopaxi.content.ContentRepositoryImpl;
import com.coremedia.objectserver.beans.ContentBean;
import com.coremedia.xml.Markup;
import com.coremedia.xml.MarkupFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class CSVTestHelper {

  public CSVTestHelper() {
    // Default
  }

  public Content generateContentWithProperty(String propertyName, CapPropertyDescriptorType propertyType,
                                             Object propertyValue, ContentRepositoryImpl contentRepository) {
    Content content = mock(Content.class);
    ContentType contentType = mock(ContentType.class);
    CapPropertyDescriptor descriptor = mock(CapPropertyDescriptor.class);

    when(content.getId()).thenReturn(IdHelper.formatContentId(generateRandomContentId()));

    when(descriptor.getType()).thenReturn(propertyType);
    when(contentType.getDescriptor(propertyName)).thenReturn(descriptor);
    when(content.get(propertyName)).thenReturn(propertyValue);
    when(content.getType()).thenReturn(contentType);
    when(content.getRepository()).thenReturn(contentRepository);

    int contentId = IdHelper.parseContentId(content.getId());
    when(contentRepository.getContent(Integer.toString(contentId))).thenReturn(content);
    when(contentRepository.getContentUnchecked(content.getId())).thenReturn(content);

    return content;
  }

  public Content generateFolderWithPath(String folderPath, ContentRepository contentRepository, Content baseFolder) {
    List<Content> currentFolderHierarchy = new ArrayList<>();
    String[] delimitedFolderArray = folderPath.split(",");

    for (String folderString : delimitedFolderArray) {
      Content content = mock(Content.class);

      when(content.getId()).thenReturn(IdHelper.formatContentId(generateRandomFolderId()));
      when(content.getName()).thenReturn(folderString);

      if (!currentFolderHierarchy.isEmpty()) {
        Content lastFolder = currentFolderHierarchy.get(currentFolderHierarchy.size() - 1);
        Set<Content> children = lastFolder.getChildren();
        children.add(content);
        when(lastFolder.getChildren()).thenReturn(children);
        when(lastFolder.get("children")).thenReturn(children);
      }
      currentFolderHierarchy.add(content);
    }

    if (baseFolder != null) {
      Set<Content> children = baseFolder.getChildren();
      children.add(currentFolderHierarchy.get(0));
      when(baseFolder.getChildren()).thenReturn(children);
      when(baseFolder.get("children")).thenReturn(children);
    }

    Content childFolder = currentFolderHierarchy.get(currentFolderHierarchy.size() - 1);
    when(contentRepository.getChild(folderPath)).thenReturn(childFolder);

    return childFolder;
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

  public int generateRandomContentId() {
    int x = new Random().nextInt(1000);
    x += (x % 2 == 0 ? 1 : 0);
    return x;
  }

  public int generateRandomFolderId() {
    int x = new Random().nextInt(1000);
    x += (x % 2 == 0 ? 0 : 1);
    return x;
  }
}
