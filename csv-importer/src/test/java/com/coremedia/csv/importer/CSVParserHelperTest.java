package com.coremedia.csv.importer;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static com.coremedia.cap.common.CapPropertyDescriptorType.*;
import static org.mockito.Mockito.*;

public class CSVParserHelperTest {

  private CSVParserHelper csvParserHelper;
  private CSVTestHelper csvTestHelper;
  private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
  private ContentRepositoryImpl contentRepository;
  private StructServiceImpl structService;
  private Logger logger;

  @Before
  public void setup() {
    csvTestHelper = new CSVTestHelper();
    logger = LoggerFactory.getLogger(CSVParserHelper.class);

    contentRepository = mock(ContentRepositoryImpl.class);
    structService = new StructServiceImpl(contentRepository);

    CapConnectionImpl connection = mock(CapConnectionImpl.class);
    when(connection.getStructService()).thenReturn(structService);
    when(connection.getContentRepository()).thenReturn(contentRepository);

    when(contentRepository.getConnection()).thenReturn(connection);
    when(contentRepository.makeContentType(anyString())).thenCallRealMethod();
    ContentType contentContentType =
            contentRepository.makeContentType(IdHelper.formatContentTypeId(ContentType.CONTENT));
    when(contentRepository.getContentContentType()).thenReturn(contentContentType);
    ContentType documentContentType =
            contentRepository.makeContentType(IdHelper.formatContentTypeId(ContentType.DOCUMENT));
    when(contentRepository.getDocumentContentType()).thenReturn(documentContentType);

    when(contentRepository.getContentType(IdHelper.formatContentTypeId(ContentType.CONTENT))).thenReturn(contentContentType);

    csvParserHelper = new CSVParserHelper(false, contentRepository, logger);
  }

// --- convertObjectToPropertyValue() Tests ----------------------------------------------------------------------

  // --- General Tests ---------------------------------------------------------------------------------

  @Test
  public void convertObjectToPropertyValueTestString() throws Exception {
    String expectedPropertyValue = "Sunny Day";
    String propertyName = "title";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, STRING, "",
            contentRepository);
    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, expectedPropertyValue);
    Assert.assertEquals(expectedPropertyValue, result);
  }

  @Test
  public void convertObjectToPropertyValueTestStringUnicodeCharacters() throws Exception {
    String expectedPropertyValue = "Testing «ταБЬℓσ»: 12 4+13, now 20% off!٩(-̮̮̃-̃)۶ ٩(●̮̮̃•̃)۶ ٩(͡๏̯͡๏)۶ ٩(-̮̮̃•̃).макдональдс";
    String propertyName = "title";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, STRING, "",
            contentRepository);
    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, expectedPropertyValue);
    Assert.assertEquals(expectedPropertyValue, result);

  }

  @Test
  public void convertObjectToPropertyValueTestStringEmpty() throws Exception {
    String expectedPropertyValue = "";
    String propertyName = "title";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, STRING, "Value to remove",
            contentRepository);
    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, expectedPropertyValue);
    Assert.assertEquals(expectedPropertyValue, result);
  }

  @Test
  public void convertObjectToPropertyValueTestStringWhitespace() throws Exception {
    String expectedPropertyValue = "   ";
    String propertyName = "title";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, STRING, "Value to remove",
            contentRepository);
    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, expectedPropertyValue);
    Assert.assertEquals(result, expectedPropertyValue);

  }

  // --- Markup Tests ---------------------------------------------------------------------------------

  @Test
  public void convertObjectToPropertyValueTestMarkup() throws Exception {
    Markup expectedPropertyValue = csvTestHelper.toMarkup("Sunny Day");
    String propertyName = "teaserText";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, MARKUP,
            csvTestHelper.toMarkup("Value to remove"), contentRepository);
    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, expectedPropertyValue);
    Assert.assertEquals(result, expectedPropertyValue);
  }

  @Test
  public void convertObjectToPropertyValueTestMarkupMissingPrefix() throws Exception {
    String expectedPropertyValue = "To Convert";
    String propertyName = "teaserText";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, MARKUP,
            csvTestHelper.toMarkup("Value to Remove"), contentRepository);
    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, expectedPropertyValue);
    Assert.assertEquals(result, csvTestHelper.toMarkup(expectedPropertyValue));
  }

  @Test
  public void convertObjectToPropertyValueTestMarkupEmpty() throws Exception {
    String propertyValue = "";
    String propertyName = "teaserText";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, MARKUP,
            csvTestHelper.toMarkup("Value to remove"), contentRepository);
    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, propertyValue);
    Assert.assertNull(result);
  }

  @Test
  public void convertObjectToPropertyValueTestMarkupOnlyWhitespace() throws Exception {
    Markup expectedPropertyValue = csvTestHelper.toMarkup("   ");
    String propertyName = "teaserText";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, MARKUP,
            csvTestHelper.toMarkup("Value to remove"), contentRepository);
    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, expectedPropertyValue);
    Assert.assertEquals(result, expectedPropertyValue);
  }

  @Test
  public void convertObjectToPropertyValueTestMarkupTrailingWhitespace() throws Exception {
    Markup expectedPropertyValue = csvTestHelper.toMarkup("This has a trailing whitespace! ");
    String propertyName = "teaserText";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, MARKUP,
            csvTestHelper.toMarkup("Value to remove"), contentRepository);
    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, expectedPropertyValue);
    Assert.assertEquals(result, expectedPropertyValue);
  }

  @Test
  public void convertObjectToPropertyValueTestMarkupCarriageReturns() throws Exception {
    Markup expectedPropertyValue = csvTestHelper.toMarkup("This\rhas\rcarriage\rreturns\r");
    String propertyName = "teaserText";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, MARKUP,
            csvTestHelper.toMarkup("Value to remove"), contentRepository);
    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, expectedPropertyValue);
    Assert.assertEquals(result, expectedPropertyValue);
  }

  @Test
  public void convertObjectToPropertyValueTestMarkupNewLines() throws Exception {
    Markup expectedPropertyValue = csvTestHelper.toMarkup("This\nhas\nnew\nlines");
    String propertyName = "teaserText";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, MARKUP,
            csvTestHelper.toMarkup("Value to remove"), contentRepository);
    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, expectedPropertyValue);
    Assert.assertEquals(result, expectedPropertyValue);
  }

  @Test
  public void convertObjectToPropertyValueTestMarkupUnicode() throws Exception {
    Markup expectedPropertyValue = csvTestHelper.toMarkup("Testing «ταБЬℓσ»: 12 4+13, now 20% off!٩(-̮̮̃-̃)۶ " +
            "٩(●̮̮̃•̃)۶ ٩(͡๏̯͡๏)۶ ٩(-̮̮̃•̃).макдональдс");
    String propertyName = "teaserText";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, MARKUP,
            csvTestHelper.toMarkup("Value to remove"), contentRepository);
    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, expectedPropertyValue);
    Assert.assertEquals(result, expectedPropertyValue);
  }

  // --- Integer Tests ---------------------------------------------------------------------------------

  @Test
  public void convertObjectToPropertyValueTestInteger() throws Exception {
    Integer expectedPropertyValue = 30;
    String propertyName = "randomIntProperty";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, INTEGER, 29,
            contentRepository);
    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, expectedPropertyValue);
    Assert.assertEquals(result, expectedPropertyValue);
  }

  @Test
  public void convertObjectToPropertyValueTestIntegerMax() throws Exception {
    Integer expectedPropertyValue = Integer.MAX_VALUE;
    String propertyName = "randomIntProperty";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, INTEGER, 29,
            contentRepository);
    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, expectedPropertyValue);
    Assert.assertEquals(result, expectedPropertyValue);
  }

  @Test
  public void convertObjectToPropertyValueTestIntegerMin() throws Exception {
    Integer expectedPropertyValue = Integer.MIN_VALUE;
    String propertyName = "randomIntProperty";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, INTEGER, 29,
            contentRepository);
    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, expectedPropertyValue);
    Assert.assertEquals(result, expectedPropertyValue);
  }

  @Test(expected = NumberFormatException.class)
  public void convertObjectToPropertyValueTestIntegerNotInteger() throws Exception {
    String expectedPropertyValue = "This is not an integer";
    String propertyName = "randomIntProperty";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, INTEGER, 29,
            contentRepository);
    csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, expectedPropertyValue);
  }

  @Test
  public void convertObjectToPropertyValueTestIntegerZero() throws Exception {
    Integer expectedPropertyValue = 0;
    String propertyName = "randomIntProperty";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, INTEGER, 29,
            contentRepository);
    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, expectedPropertyValue);
    Assert.assertEquals(result, expectedPropertyValue);
  }

  @Test
  public void convertObjectToPropertyValueTestIntegerNegative() throws Exception {
    Integer expectedPropertyValue = -29;
    String propertyName = "randomIntProperty";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, INTEGER, 29,
            contentRepository);
    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, expectedPropertyValue);
    Assert.assertEquals(result, expectedPropertyValue);
  }

  @Test(expected = NumberFormatException.class)
  public void convertObjectToPropertyValueTestIntegerEmpty() throws Exception {
    String expectedPropertyValue = "";
    String propertyName = "randomIntProperty";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, INTEGER, 29,
            contentRepository);
    csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, expectedPropertyValue);
  }

  // --- Link Tests ---------------------------------------------------------------------------------

  @Test
  public void convertObjectToPropertyValueTestLink() throws Exception {

    Content baseLinkedContent = csvTestHelper.generateContentWithProperty(null, null,
            null, contentRepository);

    // Null values since it doesn't matter here
    Content linkContent1 = csvTestHelper.generateContentWithProperty(null, null, null,
            contentRepository);
    Content linkContent2 = csvTestHelper.generateContentWithProperty(null, null, null,
            contentRepository);
    Content linkContent3 = csvTestHelper.generateContentWithProperty(null, null, null,
            contentRepository);

    List<Content> expectedPropertyValue = new ArrayList<>();
    expectedPropertyValue.add(linkContent1);
    expectedPropertyValue.add(linkContent2);
    expectedPropertyValue.add(linkContent3);

    // Because linked content in the report comes in the form of an array with content IDs, we need to
    // set this to the content and test for equality on the content added into the content repo
    List<Integer> linkedContentIds = new ArrayList<>();
    linkedContentIds.add(IdHelper.parseContentId(linkContent1.getId()));
    linkedContentIds.add(IdHelper.parseContentId(linkContent2.getId()));
    linkedContentIds.add(IdHelper.parseContentId(linkContent3.getId()));

    String propertyName = "randomLinkProperty";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, LINK, baseLinkedContent,
            contentRepository);
    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, linkedContentIds);
    Assert.assertEquals(result.toString(), expectedPropertyValue.toString());
  }

  @Test
  public void convertObjectToPropertyValueTestLinkEmpty() throws Exception {

    Content baseLinkedContent = csvTestHelper.generateContentWithProperty(null, null,
            null, contentRepository);

    // Null values since it doesn't matter here
    csvTestHelper.generateContentWithProperty(null, null, null, contentRepository);
    csvTestHelper.generateContentWithProperty(null, null, null, contentRepository);
    csvTestHelper.generateContentWithProperty(null, null, null, contentRepository);

    List<Content> expectedPropertyValue = new ArrayList<>();

    // Because linked content in the report comes in the form of an array with content IDs, we need to
    // set this to the content and test for equality on the content added into the content repo
    List<Integer> linkedContentIds = new ArrayList<>();

    String propertyName = "randomLinkProperty";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, LINK, baseLinkedContent,
            contentRepository);
    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, linkedContentIds);
    Assert.assertEquals(result.toString(), expectedPropertyValue.toString());
  }

  @Test(expected = Exception.class)
  public void convertObjectToPropertyValueTestLinkNotLink() throws Exception {
    Content baseLinkedContent = csvTestHelper.generateContentWithProperty(null, null,
            null, contentRepository);

    String testString = "Not a Link";

    String propertyName = "randomLinkProperty";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, LINK, baseLinkedContent,
            contentRepository);
    csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, testString);
  }

  @Test
  public void convertObjectToPropertyValueTestLinkSingleLinkNoArray() throws Exception {
    Content baseLinkedContent = csvTestHelper.generateContentWithProperty(null, null,
            null, contentRepository);

    // Null values since it doesn't matter here
    Content expectedPropertyValue = csvTestHelper.generateContentWithProperty(null, null,
            null, contentRepository);
    ArrayList<Content> expectedPropertyArray = new ArrayList<>();
    expectedPropertyArray.add(expectedPropertyValue);

    // Because linked content in the report comes in the form of an array with content IDs, we need to
    // set this to the content and test for equality on the content added into the content repo
    Integer linkedContentId = IdHelper.parseContentId(expectedPropertyValue.getId());

    String propertyName = "randomLinkProperty";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, LINK, baseLinkedContent,
            contentRepository);
    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, linkedContentId);
    Assert.assertEquals(result.toString(), expectedPropertyArray.toString());
  }

  @Test
  public void convertObjectToPropertyValueTestLinkSingleLink() throws Exception {

    Content baseLinkedContent = csvTestHelper.generateContentWithProperty(null, null,
            null, contentRepository);

    // Null values since it doesn't matter here
    Content linkContent1 = csvTestHelper.generateContentWithProperty(null, null, null,
            contentRepository);

    List<Content> expectedPropertyValue = new ArrayList<>();
    expectedPropertyValue.add(linkContent1);

    // Because linked content in the report comes in the form of an array with content IDs, we need to
    // set this to the content and test for equality on the content added into the content repo
    List<Integer> linkedContentIds = new ArrayList<>();
    linkedContentIds.add(IdHelper.parseContentId(linkContent1.getId()));

    String propertyName = "randomLinkProperty";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, LINK, baseLinkedContent, contentRepository);
    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, linkedContentIds);
    Assert.assertEquals(result.toString(), expectedPropertyValue.toString());
  }

  @Test
  public void convertObjectToPropertyValueTestLinkMissingContent() throws Exception {

    Content baseLinkedContent = csvTestHelper.generateContentWithProperty(null, null,
            null, contentRepository);

    // Null values since it doesn't matter here
    Content linkContent1 = csvTestHelper.generateContentWithProperty(null, null, null,
            contentRepository);
    Content linkContent2 = csvTestHelper.generateContentWithProperty(null, null, null,
            contentRepository);
    Content linkContent3 = csvTestHelper.generateContentWithProperty(null, null, null,
            contentRepository);

    List<Content> expectedPropertyValue = new ArrayList<>();
    expectedPropertyValue.add(linkContent1);
    expectedPropertyValue.add(linkContent2);
    expectedPropertyValue.add(linkContent3);

    // Because linked content in the report comes in the form of an array with content IDs, we need to
    // set this to the content and test for equality on the content added into the content repo
    List<Integer> badLinkedContentIds = new ArrayList<>();
    badLinkedContentIds.add(IdHelper.parseContentId(linkContent1.getId()));
    badLinkedContentIds.add(IdHelper.parseContentId(linkContent2.getId()));
    badLinkedContentIds.add(IdHelper.parseContentId(linkContent3.getId()));
    badLinkedContentIds.add(csvTestHelper.generateRandomContentId());

    String propertyName = "randomLinkProperty";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, LINK, baseLinkedContent, contentRepository);
    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, badLinkedContentIds);
    Assert.assertEquals(result.toString(), expectedPropertyValue.toString());

    // TODO: Check message was logged
  }

  // --- Date Tests ---------------------------------------------------------------------------------

  @Test
  public void convertObjectToPropertyValueTestDate() throws Exception {
    Calendar exampleDate = Calendar.getInstance();
    exampleDate.set(1991, Calendar.APRIL, 24);
    String propertyName = "creationDate";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, DATE, exampleDate, contentRepository);

    String expectedDateString = "04-13-2021 16:46:02";
    Calendar expectedDate = Calendar.getInstance();
    expectedDate.setTime(dateFormat.parse(expectedDateString));

    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName,
            expectedDateString);

    Assert.assertEquals(expectedDate, result);
  }

  @Test(expected = ParseException.class)
  public void convertObjectToPropertyValueTestDateNotDate() throws Exception {
    Calendar exampleDate = Calendar.getInstance();
    exampleDate.set(1991, Calendar.APRIL, 24);
    String propertyName = "creationDate";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, DATE, exampleDate, contentRepository);

    csvParserHelper.convertObjectToPropertyValue(testContent, propertyName,
            "This is not a date");
  }

  @Test(expected = ParseException.class)
  public void convertObjectToPropertyValueTestDateBadDateFormat() throws Exception {
    Calendar exampleDate = Calendar.getInstance();
    exampleDate.set(1991, Calendar.APRIL, 24);
    String propertyName = "creationDate";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, DATE, exampleDate, contentRepository);

    String expectedDateString = "04/13/2021 16:46:02";
    Calendar expectedDate = Calendar.getInstance();
    expectedDate.setTime(dateFormat.parse(expectedDateString));

    csvParserHelper.convertObjectToPropertyValue(testContent, propertyName,
            expectedDateString);
  }

  @Test
  public void convertObjectToPropertyValueTestDateBeforeEpoch() throws Exception {
    Calendar exampleDate = Calendar.getInstance();
    exampleDate.set(1991, Calendar.APRIL, 24);
    String propertyName = "creationDate";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, DATE, exampleDate, contentRepository);

    String expectedDateString = "04-24-1950 16:46:02";
    Calendar expectedDate = Calendar.getInstance();
    expectedDate.setTime(dateFormat.parse(expectedDateString));

    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName,
            expectedDateString);

    Assert.assertEquals(expectedDate, result);
  }

  @Test
  public void evaluateContentPropertyTestDateAfterJanuary19th2038() throws Exception {
    Calendar exampleDate = Calendar.getInstance();
    exampleDate.set(1991, Calendar.APRIL, 24);
    String propertyName = "creationDate";
    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, DATE, exampleDate, contentRepository);

    String expectedDateString = "02-20-2038 16:46:02";
    Calendar expectedDate = Calendar.getInstance();
    expectedDate.setTime(dateFormat.parse(expectedDateString));

    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName,
            expectedDateString);

    Assert.assertEquals(expectedDate, result);
  }

  // --- Struct Tests ---------------------------------------------------------------------------------

  @Test
  public void convertObjectToPropertyValueTestStruct() throws Exception {

    String propertyName = "localSettings";

    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, STRUCT, null, contentRepository);

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

    structBuilder.declareString("String Property", 1000, value);
    structBuilder.declareBoolean("Boolean Property", true);
    structBuilder.declareInteger("Integer Property", 25);
    structBuilder.declareLink("Link Property", contentRepository.getContentContentType(), linkedContent);
    structBuilder.declareDate("Date Property", cal);
    structBuilder.declareStrings("String List Property", Integer.MAX_VALUE, stringList);
    structBuilder.declareBooleans("Boolean List Property", booleanList);
    structBuilder.declareIntegers("Integer List Property", intList);
    structBuilder.declareLinks("Link List Property", contentRepository.getContentContentType(), Collections.emptyList());
    structBuilder.add("Link List Property", linkedContent2);
    structBuilder.add("Link List Property", linkedContent3);
    structBuilder.add("Link List Property", linkedContent4);
    structBuilder.declareDates("Date List Property", dateList);

    Struct subSubStruct = structBuilder.build();
    structBuilder.declareStruct("Struct Property", subSubStruct);
    Struct subStruct = structBuilder.build();
    Struct subStruct2 = structBuilder.build();
    Struct subStruct3 = structBuilder.build();
    Struct subStruct4 = structBuilder.build();

    List<Struct> structList = new ArrayList<>();
    structList.add(subStruct);
    structList.add(subStruct2);
    structList.add(subStruct3);
    structList.add(subStruct4);

    structBuilder.declareStructs("Struct List Property", structList);

    Struct struct = structBuilder.build();
    String expectedStructString = struct.toMarkup().toString().replaceAll("\n", "").replaceAll("\r", "");

    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName,
            expectedStructString);

    Assert.assertEquals(struct, result);
  }

  @Test
  public void convertObjectToPropertyValueTestStructEmpty() throws Exception {

    String propertyName = "localSettings";

    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, STRUCT, null, contentRepository);

    StructBuilder structBuilder = structService.createStructBuilder();
    Struct struct = structBuilder.build();
    String expectedStructString = struct.toMarkup().toString().replaceAll("\n", "").replaceAll("\r", "");

    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName,
            expectedStructString);

    Assert.assertEquals(struct, result);
  }

  @Test(expected = NullPointerException.class)
  public void convertObjectToPropertyValueTestStructNonExistentContent() throws Exception {

    String propertyName = "localSettings";

    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, STRUCT, null, contentRepository);

    StructBuilder structBuilder = structService.createStructBuilder();

    String value = "String Value";
    String value2 = "String Value 2";
    String value3 = "String Value 3";
    String value4 = "String Value 4";

    Content linkedContent = csvTestHelper.generateContentWithProperty("", STRING, "", contentRepository);
    Content linkedContent2 = csvTestHelper.generateContentWithProperty("", STRING, "", contentRepository);
    Content linkedContent3 = csvTestHelper.generateContentWithProperty("", STRING, "", contentRepository);
    Content linkedContent4 = csvTestHelper.generateContentWithFunction(Content::getName, "Not Added to Mock Repo");

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

    structBuilder.declareString("String Property", 1000, value);
    structBuilder.declareBoolean("Boolean Property", true);
    structBuilder.declareInteger("Integer Property", 25);
    structBuilder.declareLink("Link Property", contentRepository.getContentContentType(), linkedContent);
    structBuilder.declareDate("Date Property", cal);
    structBuilder.declareStrings("String List Property", Integer.MAX_VALUE, stringList);
    structBuilder.declareBooleans("Boolean List Property", booleanList);
    structBuilder.declareIntegers("Integer List Property", intList);
    structBuilder.declareLinks("Link List Property", contentRepository.getContentContentType(), Collections.emptyList());
    structBuilder.add("Link List Property", linkedContent2);
    structBuilder.add("Link List Property", linkedContent3);
    structBuilder.add("Link List Property", linkedContent4);
    structBuilder.declareDates("Date List Property", dateList);

    Struct subSubStruct = structBuilder.build();
    structBuilder.declareStruct("Struct Property", subSubStruct);
    Struct subStruct = structBuilder.build();
    Struct subStruct2 = structBuilder.build();
    Struct subStruct3 = structBuilder.build();
    Struct subStruct4 = structBuilder.build();

    List<Struct> structList = new ArrayList<>();
    structList.add(subStruct);
    structList.add(subStruct2);
    structList.add(subStruct3);
    structList.add(subStruct4);

    structBuilder.declareStructs("Struct List Property", structList);

    Struct struct = structBuilder.build();
    String expectedStructString = struct.toMarkup().toString().replaceAll("\n", "").replaceAll("\r", "");

    csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, expectedStructString);
  }

  @Test
  public void convertObjectToPropertyValueTestStructUnicodeCharacters() throws Exception {

    String propertyName = "localSettings";

    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, STRUCT, null, contentRepository);

    StructBuilder structBuilder = structService.createStructBuilder();

    String value = "макдональдс";
    String value2 = "Testing «ταБЬℓσ»:";
    String value3 = "٩(●̮̮̃•̃)۶ ٩(͡๏̯͡๏)۶ ٩(-̮̮̃•̃)";
    String value4 = "<>/.!\\/>";

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

    Calendar cal = Calendar.getInstance();

    List<Calendar> dateList = new ArrayList<>();
    dateList.add(cal);
    dateList.add(cal);
    dateList.add(cal);
    dateList.add(cal);
    dateList.add(cal);

    structBuilder.declareString("String Property", 1000, value);
    structBuilder.declareBoolean("Boolean Property", true);
    structBuilder.declareInteger("Integer Property", 25);
    structBuilder.declareLink("Link Property", contentRepository.getContentContentType(), linkedContent);
    structBuilder.declareDate("Date Property", cal);
    structBuilder.declareStrings("String List Property", Integer.MAX_VALUE, stringList);
    structBuilder.declareBooleans("Boolean List Property", booleanList);
    structBuilder.declareIntegers("Integer List Property", intList);
    structBuilder.declareLinks("Link List Property", contentRepository.getContentContentType(), Collections.emptyList());
    structBuilder.add("Link List Property", linkedContent2);
    structBuilder.add("Link List Property", linkedContent3);
    structBuilder.add("Link List Property", linkedContent4);
    structBuilder.declareDates("Date List Property", dateList);

    Struct subSubStruct = structBuilder.build();
    structBuilder.declareStruct("Struct Property", subSubStruct);
    Struct subStruct = structBuilder.build();
    Struct subStruct2 = structBuilder.build();
    Struct subStruct3 = structBuilder.build();
    Struct subStruct4 = structBuilder.build();

    List<Struct> structList = new ArrayList<>();
    structList.add(subStruct);
    structList.add(subStruct2);
    structList.add(subStruct3);
    structList.add(subStruct4);

    structBuilder.declareStructs("Struct List Property", structList);

    Struct struct = structBuilder.build();
    String expectedStructString = struct.toMarkup().toString().replaceAll("\n", "").replaceAll("\r", "");

    Object result = csvParserHelper.convertObjectToPropertyValue(testContent, propertyName,
            expectedStructString);

    Assert.assertEquals(struct, result);
  }

  @Test(expected = IllegalArgumentException.class)
  public void convertObjectToPropertyValueTestStructMalformed() throws Exception {

    String propertyName = "localSettings";

    Content testContent = csvTestHelper.generateContentWithProperty(propertyName, STRUCT, null, contentRepository);

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

    Calendar cal = Calendar.getInstance();

    List<Calendar> dateList = new ArrayList<>();
    dateList.add(cal);
    dateList.add(cal);
    dateList.add(cal);
    dateList.add(cal);
    dateList.add(cal);

    structBuilder.declareString("String Property", 1000, value);
    structBuilder.declareBoolean("Boolean Property", true);
    structBuilder.declareInteger("Integer Property", 25);
    structBuilder.declareLink("Link Property", contentRepository.getContentContentType(), linkedContent);
    structBuilder.declareDate("Date Property", cal);
    structBuilder.declareStrings("String List Property", Integer.MAX_VALUE, stringList);
    structBuilder.declareBooleans("Boolean List Property", booleanList);
    structBuilder.declareIntegers("Integer List Property", intList);
    structBuilder.declareLinks("Link List Property", contentRepository.getContentContentType(), Collections.emptyList());
    structBuilder.add("Link List Property", linkedContent2);
    structBuilder.add("Link List Property", linkedContent3);
    structBuilder.add("Link List Property", linkedContent4);
    structBuilder.declareDates("Date List Property", dateList);

    Struct subSubStruct = structBuilder.build();
    structBuilder.declareStruct("Struct Property", subSubStruct);
    Struct subStruct = structBuilder.build();
    Struct subStruct2 = structBuilder.build();
    Struct subStruct3 = structBuilder.build();
    Struct subStruct4 = structBuilder.build();

    List<Struct> structList = new ArrayList<>();
    structList.add(subStruct);
    structList.add(subStruct2);
    structList.add(subStruct3);
    structList.add(subStruct4);

    structBuilder.declareStructs("Struct List Property", structList);

    Struct struct = structBuilder.build();
    String expectedStructString = struct.toMarkup().toString().replaceAll("\n", "").replaceAll("\r", "");
    expectedStructString = expectedStructString.replaceAll("LinkListProperty", "DateListProperty");
    expectedStructString = expectedStructString.replaceAll("StringProperty", "IntProperty");

    csvParserHelper.convertObjectToPropertyValue(testContent, propertyName, expectedStructString);
  }

}
