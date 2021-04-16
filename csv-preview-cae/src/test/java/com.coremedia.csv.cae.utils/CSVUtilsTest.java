package com.coremedia.csv.cae.utils;

import com.coremedia.blueprint.cae.contentbeans.CMTaxonomyImpl;
import com.coremedia.blueprint.common.contentbeans.CMTaxonomy;
import com.coremedia.cap.common.CapPropertyDescriptor;
import com.coremedia.cap.common.CapPropertyDescriptorType;
import com.coremedia.cap.common.IdHelper;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentType;
import com.coremedia.cap.struct.Struct;
import com.coremedia.cap.struct.StructBuilder;
import com.coremedia.cap.undoc.content.ContentRepository;
import com.coremedia.cotopaxi.common.CapConnectionImpl;
import com.coremedia.cotopaxi.content.ContentRepositoryImpl;
import com.coremedia.cotopaxi.struct.StructServiceImpl;
import com.coremedia.objectserver.beans.ContentBean;
import com.coremedia.objectserver.beans.ContentBeanFactory;
import com.coremedia.xml.Markup;
import com.coremedia.xml.MarkupFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

import static com.coremedia.cap.common.CapPropertyDescriptorType.*;
import static com.coremedia.csv.common.CSVConstants.PROPERTY_LOCATION_TAGS;
import static com.coremedia.csv.common.CSVConstants.PROPERTY_SUBJECT_TAGS;
import static com.coremedia.elastic.core.test.Injection.inject;
import static java.util.Calendar.FEBRUARY;
import static java.util.Calendar.NOVEMBER;
import static org.mockito.Mockito.*;

public class CSVUtilsTest {

  private CSVUtils csvUtils;
  private List<CMTaxonomy> currentTaxonomyPathList;
  private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
  private ContentRepositoryImpl contentRepository;
  private StructServiceImpl structService;

  @Before
  public void setup() {
    csvUtils = new CSVUtils();
    currentTaxonomyPathList = new ArrayList<>();

    contentRepository = mock(ContentRepositoryImpl.class);
    structService = new StructServiceImpl(contentRepository);

    CapConnectionImpl connection = mock(CapConnectionImpl.class);
    when(connection.getStructService()).thenReturn(structService);
    when(connection.getContentRepository()).thenReturn(contentRepository);

    when(contentRepository.getConnection()).thenReturn(connection);
    when(contentRepository.makeContentType(anyString())).thenCallRealMethod();
    ContentType contentContentType = contentRepository.makeContentType(IdHelper.formatContentTypeId(ContentType.CONTENT));
    when(contentRepository.getContentContentType()).thenReturn(contentContentType);
    ContentType documentContentType = contentRepository.makeContentType(IdHelper.formatContentTypeId(ContentType.DOCUMENT));
    when(contentRepository.getDocumentContentType()).thenReturn(documentContentType);
  }

// --- evaluateContentProperty() Tests ----------------------------------------------------------------------------------

//  --- General Tests ---
  @Test
  public void evaluateContentPropertyTest() {
    String expectedPropertyValue = "Sunny Day";
    String propertyName = "title";
    Content testContent = generateContentWithProperty(propertyName, STRING, expectedPropertyValue);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(expectedPropertyValue, returnedProperty);
  }

  @Test
  public void evaluateContentPropertyTestEmptyPropertyName() {
    String expectedPropertyValue = "";
    String propertyName = "";
    Content testContent = generateContentWithProperty(propertyName, STRING, expectedPropertyValue);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(expectedPropertyValue, returnedProperty);
  }

  @Test
  public void evaluateContentPropertyTestPropertyDNE() {
    String expectedPropertyValue = "";
    String propertyName = "title";
    Content testContent = generateContentWithProperty(propertyName, STRING, "");
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, "DNE");
    Assert.assertEquals(expectedPropertyValue, returnedProperty);
  }

//  --- Link Tests ---
  @Test
  public void evaluateContentPropertyTestLink() {
    // Null values since it doesn't matter here
    Content linkContent1 = generateContentWithProperty(null, null, null);
    Content linkContent2 = generateContentWithProperty(null, null, null);
    Content linkContent3 = generateContentWithProperty(null, null, null);

    List<Content> linkedContent = new ArrayList<>();
    linkedContent.add(linkContent1);
    linkedContent.add(linkContent2);
    linkedContent.add(linkContent3);

    List<Integer> expectedPropertyValue = new ArrayList<>();
    expectedPropertyValue.add(IdHelper.parseContentId(linkContent1.getId()));
    expectedPropertyValue.add(IdHelper.parseContentId(linkContent2.getId()));
    expectedPropertyValue.add(IdHelper.parseContentId(linkContent3.getId()));

    String propertyName = "children";
    Content testContent = generateContentWithProperty(propertyName, LINK, linkedContent);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(expectedPropertyValue.toString(), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestLinkNull() {
    String propertyName = "children";
    Content testContent = generateContentWithProperty(propertyName, LINK, null);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals("[]", returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestLinkEmptyList() {
    List<Content> linkedContent = new ArrayList<>();

    String propertyName = "children";
    Content testContent = generateContentWithProperty(propertyName, LINK, linkedContent);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals("[]", returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestLinkSingleItem() {
    // Null values since it doesn't matter here
    Content linkContent1 = generateContentWithProperty(null, null, null);

    List<Content> linkedContent = new ArrayList<>();
    linkedContent.add(linkContent1);

    List<Integer> expectedPropertyValue = new ArrayList<>();
    expectedPropertyValue.add(IdHelper.parseContentId(linkContent1.getId()));

    String propertyName = "children";
    Content testContent = generateContentWithProperty(propertyName, LINK, linkedContent);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(expectedPropertyValue.toString(), returnedProperty.toString());
  }

//  --- Subject Tag Tests --
  @Test
  public void evaluateContentPropertyTestLinkSubjectTags() {
    Map<String, List<Content>> taxonomyStructure = new HashMap<>();

    Content tag1 = generateContentWithFunction(Content::getName, "tag1");
    Content tag2 = generateContentWithFunction(Content::getName, "tag2");
    Content tag3 = generateContentWithFunction(Content::getName, "tag3");

    List<Content> firstSetList = new ArrayList<>();
    firstSetList.add(tag1);
    firstSetList.add(tag2);
    firstSetList.add(tag3);

    taxonomyStructure.put("firstSet", firstSetList);

    Content tag4 = generateContentWithFunction(Content::getName, "tag4");
    Content tag5 = generateContentWithFunction(Content::getName, "tag5");
    Content tag6 = generateContentWithFunction(Content::getName, "tag6");

    List<Content> secondSetList = new ArrayList<>();
    secondSetList.add(tag4);
    secondSetList.add(tag5);
    secondSetList.add(tag6);

    taxonomyStructure.put("secondSet", secondSetList);

    ContentBeanFactory testTaxonomyFactory = generateContentBeanFactory(taxonomyStructure);

    inject(csvUtils, testTaxonomyFactory);

    List<Content> testTaxonomies = new ArrayList<>();
    testTaxonomies.add(tag3);
    testTaxonomies.add(tag5);
    testTaxonomies.add(tag6);

    List<String> expectedPropertyValue = new ArrayList<>();
    expectedPropertyValue.add("/tag1/tag2/tag3/");
    expectedPropertyValue.add("/tag4/tag5/");
    expectedPropertyValue.add("/tag4/tag5/tag6/");

    Content testContent = generateContentWithProperty(PROPERTY_SUBJECT_TAGS, LINK, testTaxonomies);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, PROPERTY_SUBJECT_TAGS);
    Assert.assertEquals(expectedPropertyValue.toString(), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestLinkLocationTags() {
    Map<String, List<Content>> taxonomyStructure = new HashMap<>();

    Content tag1 = generateContentWithFunction(Content::getName, "tag1");
    Content tag2 = generateContentWithFunction(Content::getName, "tag2");
    Content tag3 = generateContentWithFunction(Content::getName, "tag3");

    List<Content> firstSetList = new ArrayList<>();
    firstSetList.add(tag1);
    firstSetList.add(tag2);
    firstSetList.add(tag3);

    taxonomyStructure.put("firstSet", firstSetList);

    Content tag4 = generateContentWithFunction(Content::getName, "tag4");
    Content tag5 = generateContentWithFunction(Content::getName, "tag5");
    Content tag6 = generateContentWithFunction(Content::getName, "tag6");

    List<Content> secondSetList = new ArrayList<>();
    secondSetList.add(tag4);
    secondSetList.add(tag5);
    secondSetList.add(tag6);

    taxonomyStructure.put("secondSet", secondSetList);

    ContentBeanFactory testTaxonomyFactory = generateContentBeanFactory(taxonomyStructure);

    inject(csvUtils, testTaxonomyFactory);

    List<Content> testTaxonomies = new ArrayList<>();
    testTaxonomies.add(tag3);
    testTaxonomies.add(tag5);
    testTaxonomies.add(tag6);

    List<String> expectedPropertyValue = new ArrayList<>();
    expectedPropertyValue.add("/tag1/tag2/tag3/");
    expectedPropertyValue.add("/tag4/tag5/");
    expectedPropertyValue.add("/tag4/tag5/tag6/");

    Content testContent = generateContentWithProperty(PROPERTY_LOCATION_TAGS, LINK, testTaxonomies);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, PROPERTY_LOCATION_TAGS);
    Assert.assertEquals(expectedPropertyValue.toString(), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestLinkSubjectTagsNull() {
    Content testContent = generateContentWithProperty(PROPERTY_SUBJECT_TAGS, LINK, null);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, PROPERTY_SUBJECT_TAGS);
    Assert.assertEquals("[]", returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestLinkSubjectTagsEmptyList() {
    List<Content> emptyList = new ArrayList<>();

    Content testContent = generateContentWithProperty(PROPERTY_SUBJECT_TAGS, LINK, emptyList);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, PROPERTY_SUBJECT_TAGS);
    Assert.assertEquals("[]", returnedProperty.toString());
  }

  @Test(expected = NullPointerException.class)
  public void evaluateContentPropertyTestLinkSubjectTagsListWithNullContent() {
    Map<String, List<Content>> taxonomyStructure = new HashMap<>();

    Content tag1 = generateContentWithFunction(Content::getName, "tag1");
    Content tag2 = generateContentWithFunction(Content::getName, "tag2");
    Content tag3 = null;

    List<Content> firstSetList = new ArrayList<>();
    firstSetList.add(tag1);
    firstSetList.add(tag2);
    firstSetList.add(tag3);

    taxonomyStructure.put("firstSet", firstSetList);

    Content tag4 = generateContentWithFunction(Content::getName, "tag4");
    Content tag5 = null;
    Content tag6 = generateContentWithFunction(Content::getName, "tag6");;

    List<Content> secondSetList = new ArrayList<>();
    secondSetList.add(tag4);
    secondSetList.add(tag5);
    secondSetList.add(tag6);

    taxonomyStructure.put("secondSet", secondSetList);

    ContentBeanFactory testTaxonomyFactory = generateContentBeanFactory(taxonomyStructure);

    inject(csvUtils, testTaxonomyFactory);

    List<Content> testTaxonomies = new ArrayList<>();
    testTaxonomies.add(tag3);
    testTaxonomies.add(tag5);
    testTaxonomies.add(tag6);

    Content testContent = generateContentWithProperty(PROPERTY_SUBJECT_TAGS, LINK, testTaxonomies);
    csvUtils.evaluateContentProperty(testContent, PROPERTY_SUBJECT_TAGS);
  }

  @Test
  public void evaluateContentPropertyTestLinkSubjectTagsSingleTag() {
    Map<String, List<Content>> taxonomyStructure = new HashMap<>();

    Content tag1 = generateContentWithFunction(Content::getName, "tag1");
    Content tag2 = generateContentWithFunction(Content::getName, "tag2");

    List<Content> firstSetList = new ArrayList<>();
    firstSetList.add(tag1);
    firstSetList.add(tag2);

    taxonomyStructure.put("firstSet", firstSetList);

    ContentBeanFactory testTaxonomyFactory = generateContentBeanFactory(taxonomyStructure);

    inject(csvUtils, testTaxonomyFactory);

    List<Content> testTaxonomies = new ArrayList<>();
    testTaxonomies.add(tag2);

    List<String> expectedPropertyValue = new ArrayList<>();
    expectedPropertyValue.add("/tag1/tag2/");

    Content testContent = generateContentWithProperty(PROPERTY_SUBJECT_TAGS, LINK, testTaxonomies);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, PROPERTY_SUBJECT_TAGS);
    Assert.assertEquals(expectedPropertyValue.toString(), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestLinkSubjectTagsUnicodeCharacters() {
    Map<String, List<Content>> taxonomyStructure = new HashMap<>();

    Content tag1 = generateContentWithFunction(Content::getName, "Testing «ταБЬℓσ»: 1<2 & 4+1>3, now 20% off!");
    Content tag2 = generateContentWithFunction(Content::getName, "٩(-̮̮̃-̃)۶ ٩(●̮̮̃•̃)۶ ٩(͡๏̯͡๏)۶ ٩(-̮̮̃•̃).");
    Content tag3 = generateContentWithFunction(Content::getName, "макдональдс");

    List<Content> firstSetList = new ArrayList<>();
    firstSetList.add(tag1);
    firstSetList.add(tag2);
    firstSetList.add(tag3);

    taxonomyStructure.put("firstSet", firstSetList);

    ContentBeanFactory testTaxonomyFactory = generateContentBeanFactory(taxonomyStructure);

    inject(csvUtils, testTaxonomyFactory);

    List<Content> testTaxonomies = new ArrayList<>();
    testTaxonomies.add(tag3);

    List<String> expectedPropertyValue = new ArrayList<>();
    expectedPropertyValue.add("/Testing «ταБЬℓσ»: 1<2 & 4+1>3, now 20% off!/٩(-̮̮̃-̃)۶ ٩(●̮̮̃•̃)۶ ٩(͡๏̯͡๏)۶ ٩(-̮̮̃•̃)./макдональдс/");

    Content testContent = generateContentWithProperty(PROPERTY_SUBJECT_TAGS, LINK, testTaxonomies);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, PROPERTY_SUBJECT_TAGS);
    Assert.assertEquals(expectedPropertyValue.toString(), returnedProperty.toString());
  }

//  --- Markup Tests ---

  @Test
  public void evaluateContentPropertyTestMarkup() {
    Markup testMarkup = toMarkup("This is a test");
    String propertyName = "teaserText";
    Content testContent = generateContentWithProperty(propertyName, MARKUP, testMarkup);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(testMarkup.toString(), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestMarkupNull() {
    String propertyName = "teaserText";
    Content testContent = generateContentWithProperty(propertyName, MARKUP, null);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals("", returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestMarkupEmpty() {
    Markup testMarkup = toMarkup("");
    String propertyName = "teaserText";
    Content testContent = generateContentWithProperty(propertyName, MARKUP, testMarkup);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(testMarkup.toString(), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestMarkupOnlyWhiteSpace() {
    Markup testMarkup = toMarkup(" ");
    String propertyName = "teaserText";
    Content testContent = generateContentWithProperty(propertyName, MARKUP, testMarkup);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(testMarkup.toString(), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestMarkupTrailingWhiteSpace() {
    Markup testMarkup = toMarkup("White Space Trailing ");
    String propertyName = "teaserText";
    Content testContent = generateContentWithProperty(propertyName, MARKUP, testMarkup);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(testMarkup.toString(), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestMarkupCarriageReturns() {
    Markup testMarkup = toMarkup("Carriage\rReturns\r");
    String propertyName = "teaserText";
    Markup expected = toMarkup("CarriageReturns");
    Content testContent = generateContentWithProperty(propertyName, MARKUP, testMarkup);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(expected.toString(), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestMarkupNewLines() {
    Markup testMarkup = toMarkup("New\nLines\n");
    String propertyName = "teaserText";
    Markup expected = toMarkup("NewLines");
    Content testContent = generateContentWithProperty(propertyName, MARKUP, testMarkup);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(expected.toString(), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestMarkupUnicodeCharacters() {
    Markup testMarkup = toMarkup("Testing «ταБЬℓσ»: 12 4+13, now 20% off!٩(-̮̮̃-̃)۶ ٩(●̮̮̃•̃)۶ ٩(͡๏̯͡๏)۶ ٩(-̮̮̃•̃).макдональдс");
    String propertyName = "teaserText";
    Content testContent = generateContentWithProperty(propertyName, MARKUP, testMarkup);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(testMarkup.toString(), returnedProperty.toString());
  }

  // --- Date Tests ---

  @Test
  public void evaluateContentPropertyTestDate() {
    Date testDate = new Date();
    String propertyName = "creationDate";
    Content testContent = generateContentWithProperty(propertyName, DATE, testDate);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(testDate.toString(), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestDateBeforeEpoch() {
    Calendar cal = Calendar.getInstance();
    cal.set(1950, NOVEMBER, 17);
    Date testDate = cal.getTime();
    String propertyName = "creationDate";
    Content testContent = generateContentWithProperty(propertyName, DATE, testDate);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(testDate.toString(), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestDateAfterJanuary19th2038() {
    Calendar cal = Calendar.getInstance();
    cal.set(2038, FEBRUARY, 20);
    Date testDate = cal.getTime();
    String propertyName = "creationDate";
    Content testContent = generateContentWithProperty(propertyName, DATE, testDate);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(testDate.toString(), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestDateNull() {
    String propertyName = "creationDate";
    Content testContent = generateContentWithProperty(propertyName, DATE, null);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals("", returnedProperty.toString());
  }

//  --- Struct Tests ---

  @Test
  public void evaluateContentPropertyTestStruct() {
    StructBuilder structBuilder = structService.createStructBuilder();

    String value = "String Value";
    String value2 = "String Value 2";
    String value3 = "String Value 3";
    String value4 = "String Value 4";

    Content linkedContent = generateContentWithProperty("", STRING, "");
    Content linkedContent2 = generateContentWithProperty("", STRING, "");
    Content linkedContent3 = generateContentWithProperty("", STRING, "");
    Content linkedContent4 = generateContentWithProperty("", STRING, "");

    List<String> stringList = new ArrayList<>();
    stringList.add(value2);
    stringList.add(value3);
    stringList.add(value4);

    List<Boolean> booleanList = new ArrayList<>();
    booleanList.add(true);
    booleanList.add(false);
    booleanList.add(true);
    booleanList.add(false);

    List<Integer> intList = new ArrayList<>();
    intList.add(-1);
    intList.add(0);
    intList.add(1);
    intList.add(Integer.MAX_VALUE);
    intList.add(Integer.MIN_VALUE);

    List<Content> contentList = new ArrayList<>();
    contentList.add(linkedContent2);
    contentList.add(linkedContent3);
    contentList.add(linkedContent4);

    Calendar cal = Calendar.getInstance();

    List<Calendar> dateList = new ArrayList<>();
    dateList.add(cal);
    dateList.add(cal);
    dateList.add(cal);
    dateList.add(cal);
    dateList.add(cal);

    structBuilder.set("String Property", value);
    structBuilder.set("Boolean Property", true);
    structBuilder.set("Integer Property", 25);
    structBuilder.declareLink("Link Property", contentRepository.getContentContentType(), linkedContent);
    structBuilder.declareDate("Date Property", cal);
    structBuilder.set("String List Property", stringList);
    structBuilder.set("Boolean List Property", booleanList);
    structBuilder.set("Integer List Property", intList);
    structBuilder.declareLinks("Link List Property", contentRepository.getContentContentType(), contentList);
    structBuilder.declareDates("Date List Property", dateList);

    Struct subSubStruct = structBuilder.build();
    structBuilder.set("Struct Property", subSubStruct);
    Struct subStruct = structBuilder.build();
    Struct subStruct2 = structBuilder.build();
    Struct subStruct3 = structBuilder.build();
    Struct subStruct4 = structBuilder.build();

    List<Struct> structList = new ArrayList<>();
    structList.add(subStruct);
    structList.add(subStruct2);
    structList.add(subStruct3);
    structList.add(subStruct4);

    structBuilder.set("Struct List Property", structList);

    Struct struct = structBuilder.build();

    String propertyName = "localSettings";
    String expected = struct.toMarkup().toString().replaceAll("\n", "").replaceAll("\r", "");
    Content testContent = generateContentWithProperty(propertyName, STRUCT, struct);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(expected, returnedProperty);
  }

  @Test
  public void evaluateContentPropertyTestStructNull() {
    String propertyName = "localSettings";
    Content testContent = generateContentWithProperty(propertyName, STRUCT, null);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals("", returnedProperty);
  }

  @Test
  public void evaluateContentPropertyTestEmpty() {
    Struct struct = structService.emptyStruct();
    String propertyName = "localSettings";
    String expected = struct.toMarkup().toString().replaceAll("\n", "").replaceAll("\r", "");
    Content testContent = generateContentWithProperty(propertyName, STRUCT, struct);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(expected, returnedProperty);
  }

  @Test
  public void evaluateContentPropertyTestStructUnicodeCharacters() {
    StructBuilder structBuilder = structService.createStructBuilder();

    String value1 = "Testing";
    String value2 = "«ταБЬℓσ»: 1<2 & 4+1>3";
    String value3 = "now 20% off!";
    String value4 = "макдональдс";

    List<String> stringList = new ArrayList<>();
    stringList.add(value1);
    stringList.add(value2);
    stringList.add(value3);
    stringList.add(value4);

    structBuilder.set("Testing", value1);
    structBuilder.set("«ταБЬℓσ»: 1<2 & 4+1>3", value1);
    structBuilder.set("now 20% off!", value1);
    structBuilder.set("٩(-̮̮̃-̃)۶ ٩(●̮̮̃•̃)۶ ٩(͡๏̯͡๏)۶ ٩(-̮̮̃•̃).", value1);

    structBuilder.set("String List", stringList);

    Struct struct = structBuilder.build();

    String propertyName = "localSettings";
    String expected = struct.toMarkup().toString().replaceAll("\n", "").replaceAll("\r", "");
    Content testContent = generateContentWithProperty(propertyName, STRUCT, struct);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(expected, returnedProperty);
  }

//  --- General Content Property Tests ---






  // --- Helper methods ------------------------------------------------------------------------------------------------

  private Content generateContentWithProperty(String propertyName, CapPropertyDescriptorType propertyType, Object propertyValue) {
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

  private Content generateContentWithFunction(Function<Content, String> function, String value) {
    Content content = mock(Content.class);
    when(content.getId()).thenReturn(IdHelper.formatContentId(generateRandomContentId()));
    when(function.apply(content)).thenReturn(value);
    return content;
  }

  private <T extends ContentBean> T generateContentBean(Class<T> type, Content content) {
    T bean = mock(type);
    when(bean.getContent()).thenReturn(content);
    return bean;
  }

  private CMTaxonomy generateCMTaxonomy(Content content) {
    CMTaxonomyImpl taxonomy = generateContentBean(CMTaxonomyImpl.class, content);
    currentTaxonomyPathList.add(taxonomy);
    List<CMTaxonomy> tempList = new ArrayList<>(currentTaxonomyPathList);
    doReturn(tempList).when(taxonomy).getTaxonomyPathList();
    return taxonomy;
  }

  private ContentBeanFactory generateContentBeanFactory(Map<String, List<Content>> taxonomyStructure) {
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

  private Markup toMarkup(String testString) {
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

  private <T> T mockAndInjectInto(Class<T> targetClass, Object injectionTarget) {
    T blobConverter = mock(targetClass);
    inject(injectionTarget, blobConverter);
    return blobConverter;
  }

  private void resetCurrentTaxonomyPathList() {
    currentTaxonomyPathList = new ArrayList<>();
  }

  private int generateRandomContentId() {
    return new Random().nextInt(1000);
  }
}
