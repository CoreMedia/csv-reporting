package com.coremedia.csv.cae.utils;

import com.coremedia.cap.common.IdHelper;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentType;
import com.coremedia.cap.struct.Struct;
import com.coremedia.cap.struct.StructBuilder;
import com.coremedia.cotopaxi.common.CapConnectionImpl;
import com.coremedia.cotopaxi.content.ContentRepositoryImpl;
import com.coremedia.cotopaxi.struct.StructServiceImpl;
import com.coremedia.csv.test.CSVTestHelper;
import com.coremedia.xml.Markup;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.coremedia.cap.common.CapPropertyDescriptorType.*;
import static java.util.Calendar.FEBRUARY;
import static java.util.Calendar.NOVEMBER;
import static org.mockito.Mockito.*;

public class CSVUtilsTest {

  private CSVUtils csvUtils;
  private CSVTestHelper csvTestHelper;
  private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
  private ContentRepositoryImpl contentRepository;
  private StructServiceImpl structService;

  @Before
  public void setup() {
    csvUtils = new CSVUtils();
    csvTestHelper = new CSVTestHelper();

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

// --- evaluateContentProperty() Tests ---------------------------------------------------------------------------------

  //  --- General Tests ---
  @Test
  public void evaluateContentPropertyTest() {
    String expectedPropertyValue = "Sunny Day";
    String propertyName = "title";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, STRING, expectedPropertyValue, contentRepository);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(expectedPropertyValue, returnedProperty);
  }

  @Test
  public void evaluateContentPropertyTestEmptyPropertyName() {
    String expectedPropertyValue = "";
    String propertyName = "";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, STRING, expectedPropertyValue, contentRepository);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(expectedPropertyValue, returnedProperty);
  }

  @Test
  public void evaluateContentPropertyTestPropertyDNE() {
    String expectedPropertyValue = "";
    String propertyName = "title";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, STRING, "", contentRepository);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, "DNE");
    Assert.assertEquals(expectedPropertyValue, returnedProperty);
  }

  //  --- Link Tests ---
  @Test
  public void evaluateContentPropertyTestLink() {
    // Null values since it doesn't matter here
    Content linkContent1 = csvTestHelper.generateContentWithProperty(null, null, null, contentRepository);
    Content linkContent2 = csvTestHelper.generateContentWithProperty(null, null, null, contentRepository);
    Content linkContent3 = csvTestHelper.generateContentWithProperty(null, null, null, contentRepository);

    List<Content> linkedContent = new ArrayList<>();
    linkedContent.add(linkContent1);
    linkedContent.add(linkContent2);
    linkedContent.add(linkContent3);

    List<Integer> expectedPropertyValue = new ArrayList<>();
    expectedPropertyValue.add(IdHelper.parseContentId(linkContent1.getId()));
    expectedPropertyValue.add(IdHelper.parseContentId(linkContent2.getId()));
    expectedPropertyValue.add(IdHelper.parseContentId(linkContent3.getId()));

    String propertyName = "children";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, LINK, linkedContent, contentRepository);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(expectedPropertyValue.toString(), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestLinkNull() {
    String propertyName = "children";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, LINK, null, contentRepository);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals("[]", returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestLinkEmptyList() {
    List<Content> linkedContent = new ArrayList<>();

    String propertyName = "children";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, LINK, linkedContent, contentRepository);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals("[]", returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestLinkSingleItem() {
    // Null values since it doesn't matter here
    Content linkContent1 = csvTestHelper.generateContentWithProperty(null, null, null, contentRepository);

    List<Content> linkedContent = new ArrayList<>();
    linkedContent.add(linkContent1);

    List<Integer> expectedPropertyValue = new ArrayList<>();
    expectedPropertyValue.add(IdHelper.parseContentId(linkContent1.getId()));

    String propertyName = "children";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, LINK, linkedContent, contentRepository);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(expectedPropertyValue.toString(), returnedProperty.toString());
  }

//  --- Markup Tests ---

  @Test
  public void evaluateContentPropertyTestMarkup() {
    Markup testMarkup = csvTestHelper.toMarkup("This is a test");
    String propertyName = "teaserText";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, MARKUP, testMarkup, contentRepository);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(testMarkup.toString(), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestMarkupNull() {
    String propertyName = "teaserText";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, MARKUP, null, contentRepository);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals("", returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestMarkupEmpty() {
    Markup testMarkup = csvTestHelper.toMarkup("");
    String propertyName = "teaserText";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, MARKUP, testMarkup, contentRepository);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(testMarkup.toString(), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestMarkupOnlyWhiteSpace() {
    Markup testMarkup = csvTestHelper.toMarkup(" ");
    String propertyName = "teaserText";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, MARKUP, testMarkup, contentRepository);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(testMarkup.toString(), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestMarkupTrailingWhiteSpace() {
    Markup testMarkup = csvTestHelper.toMarkup("White Space Trailing ");
    String propertyName = "teaserText";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, MARKUP, testMarkup, contentRepository);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(testMarkup.toString(), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestMarkupCarriageReturns() {
    Markup testMarkup = csvTestHelper.toMarkup("Carriage\rReturns\r");
    String propertyName = "teaserText";
    Markup expected = csvTestHelper.toMarkup("CarriageReturns");
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, MARKUP, testMarkup, contentRepository);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(expected.toString(), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestMarkupNewLines() {
    Markup testMarkup = csvTestHelper.toMarkup("New\nLines\n");
    String propertyName = "teaserText";
    Markup expected = csvTestHelper.toMarkup("NewLines");
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, MARKUP, testMarkup, contentRepository);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(expected.toString(), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestMarkupUnicodeCharacters() {
    Markup testMarkup = csvTestHelper.toMarkup("Testing «ταБЬℓσ»: 12 4+13, now 20% off!٩(-̮̮̃-̃)۶ ٩(●̮̮̃•̃)۶ ٩(͡๏̯͡๏)۶ ٩(-̮̮̃•̃).макдональдс");
    String propertyName = "teaserText";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, MARKUP, testMarkup, contentRepository);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(testMarkup.toString(), returnedProperty.toString());
  }

  // --- Date Tests ---

  @Test
  public void evaluateContentPropertyTestDate() {
    Calendar testDate = Calendar.getInstance();
    String propertyName = "creationDate";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, DATE, testDate, contentRepository);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(dateFormat.format(testDate.getTime()), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestDateBeforeEpoch() {
    Calendar testDate = Calendar.getInstance();
    testDate.set(1950, NOVEMBER, 17);
    String propertyName = "creationDate";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, DATE, testDate, contentRepository);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(dateFormat.format(testDate.getTime()), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestDateAfterJanuary19th2038() {
    Calendar testDate = Calendar.getInstance();
    testDate.set(2038, FEBRUARY, 20);
    String propertyName = "creationDate";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, DATE, testDate, contentRepository);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(dateFormat.format(testDate.getTime()), returnedProperty.toString());
  }

  @Test
  public void evaluateContentPropertyTestDateNull() {
    String propertyName = "creationDate";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, DATE, null, contentRepository);
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

    Content linkedContent = csvTestHelper.generateContentWithProperty("", STRING, "", contentRepository);
    Content linkedContent2 = csvTestHelper.generateContentWithProperty("", STRING, "", contentRepository);
    Content linkedContent3 = csvTestHelper.generateContentWithProperty("", STRING, "", contentRepository);
    Content linkedContent4 = csvTestHelper.generateContentWithProperty("", STRING, "", contentRepository);

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
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, STRUCT, struct, contentRepository);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(expected, returnedProperty);
  }

  @Test
  public void evaluateContentPropertyTestStructNull() {
    String propertyName = "localSettings";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, STRUCT, null, contentRepository);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals("", returnedProperty);
  }

  @Test
  public void evaluateContentPropertyTestEmpty() {
    Struct struct = structService.emptyStruct();
    String propertyName = "localSettings";
    String expected = struct.toMarkup().toString().replaceAll("\n", "").replaceAll("\r", "");
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, STRUCT, struct, contentRepository);
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
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, STRUCT, struct, contentRepository);
    Object returnedProperty = csvUtils.evaluateContentProperty(testContent, propertyName);
    Assert.assertEquals(expected, returnedProperty);
  }

//  --- General Content Property Tests ---

}
