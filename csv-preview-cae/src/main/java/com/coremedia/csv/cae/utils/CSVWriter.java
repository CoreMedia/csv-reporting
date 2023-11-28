package com.coremedia.csv.cae.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CSVWriter {
  private final CSVPrinter csvPrinter;

  public CSVWriter(Writer writer) throws IOException {
    csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL);
  }

  public void writeHeader(String[] header) throws IOException {
    csvPrinter.printRecord(Arrays.asList(header));
  }

  public void flush() throws IOException {
    csvPrinter.flush();
  }

  public void close() throws IOException {
    csvPrinter.close();
  }

  public void write(Map<String, String> csvRecord, String[] header) throws IOException {
    List<String> values = Arrays.stream(header).map(csvRecord::get).collect(Collectors.toList());
    csvPrinter.printRecord(values);
  }
}
