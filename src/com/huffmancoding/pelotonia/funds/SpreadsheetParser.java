package com.huffmancoding.pelotonia.funds;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * This class reads an excel spreadsheet that contains rows of data under header column names.
 *
 * @author khuffman
 */
public abstract class SpreadsheetParser
{
    /** The Excel file to read. */
    private final URL url;

    /**
     * Constructor for reading a spreadsheet file.
     *
     * @param rosterFile the file to read
     */
    public SpreadsheetParser(URL url)
    {
        this.url = url;
    }

    /**
     * Load the TeamMembers from the {@link #rosterFile}.
     *
     * @throws IOException in case the problem reading the file
     * @throws InvalidFormatException in case of syntax or semantic xlsx format errors
     */
    public void loadSpreadsheet(String sheetName) throws IOException, InvalidFormatException
    {
        try (InputStream rosterStream = url.openStream())
        {
            Workbook wb = WorkbookFactory.create(rosterStream);
            Sheet sheet;
            if (sheetName != null)
            {
                sheet = wb.getSheet(sheetName);
                if (sheet == null)
                {
                    throw new InvalidFormatException("Spreadsheet " + url.toExternalForm() + " is missing a sheet named " + sheetName);
                }
            }
            else
            {
                sheet = wb.getSheetAt(0);
            }

            for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); ++rowIndex)
            {
                Row row = sheet.getRow(rowIndex);
                if (row != null)
                {
                    parseRow(row);
                }
            }
        }
    }

    /**
     * Parse a row of the spreadsheet looking for a header or member row.
     *
     * @param row the row to check
     * @throws InvalidFormatException if the row does not have the expected format
     */
    private void parseRow(Row row) throws InvalidFormatException
    {
        try
        {
            boolean isHeader = parseHeaderRow(row);
            if (isHeader)
            {
                return;
            }

            parseObjectRow(row);
        }
        catch (Exception ex)
        {
            InvalidFormatException rowException = new InvalidFormatException("Error on row " + row.getRowNum() + " of " + url.getPath());
            rowException.initCause(ex);
            throw rowException;
        }
    }

    /**
     * Find the column indices with data to import by looking for specific column headings.
     *
     * @param row in the spreadsheet to look for headers.
     * @throws InvalidFormatException in case the row has some, but not all headers.
     * @return true if the line contained a header
     */
    abstract protected boolean parseHeaderRow(Row row) throws InvalidFormatException;

    /**
     * Find the object at this non-object row
     *
     * @param row the row in the spreadsheet
     * @throws InvalidFormatException if the row has data, but it is in the wrong format 
     */
    abstract protected void parseObjectRow(Row row) throws InvalidFormatException;
}
