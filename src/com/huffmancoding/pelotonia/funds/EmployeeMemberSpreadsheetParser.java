package com.huffmancoding.pelotonia.funds;

import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

/**
 * This class reads an excel spreadsheet that contains rows for each team member and sharable funds from Pelotonia.
 *
 * The XLSX format should be (it may have additional ignored columns after the first column):
 * 
 * "Rider ID","Employee"
 * riderId1,{"Employee"|"Family"}
 * riderId2,{"Employee"|"Family"}
 * ...
 *
 * @author khuffman
 */
public class EmployeeMemberSpreadsheetParser extends SpreadsheetParser
{
    /** header title for the column that contains team member rider ID */
    public static final SpreadsheetColumn RIDER_ID_COLUMN = new SpreadsheetColumn("Rider ID");

    /** header title for the column that contains whether the member is an employee or not */
    private static final SpreadsheetColumn EMPLOYEE_COLUMN = new SpreadsheetColumn("Employee");

    /** The format of the rider id assigned by Pelotonia. */
    private static final Pattern RIDER_ID_PATTERN = Pattern.compile("[A-Z][A-Z][0-9][0-9][0-9][0-9]");

    /** the list of team members in the file. */
    Set<String> employeRiderIDs = new TreeSet<>();

    public EmployeeMemberSpreadsheetParser(URL url, String sheetName)
    {
        super(url, sheetName);
    }

    /**
     * Whether the team member qualifies for match.
     *
     * @return true if member qualifies for match, false otherwise
     */
    public boolean isEmployee(String riderId)
    {
        return employeRiderIDs.contains(riderId);
    }

    /**
     * Load the current spreadsheet from pelotonia.org
     *
     * @throws IOException in case the problem reading the file
     * @throws InvalidFormatException in case of syntax or semantic xlsx format errors
     */
    public void loadEmployeeSpreadsheet() throws IOException, InvalidFormatException
    {
        super.loadSpreadsheet("employee names");

        if (employeRiderIDs.isEmpty())
        {
            throw new InvalidFormatException("Employee file does not have any valid rider IDs below '" + RIDER_ID_COLUMN.getName() + "' header");
        }

        FundUtils.log("There are " + employeRiderIDs.size() + " employees.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void parseObjectRow(Row row) throws InvalidFormatException
    {
        String employee = parseEmployeeRow(row);
        if (employee != null)
        {
            employeRiderIDs.add(employee);
        }
    }

    /**
     * Find the column indices with data to import by looking for specific column headings.
     *
     * @param row in the spreadsheet to look for headers.
     * @throws InvalidFormatException in case the row has some, but not all headers.
     * @return true if the line contained a header
     */
    @Override
    protected boolean parseHeaderRow(Row row) throws InvalidFormatException
    {
        int cellIndex = 0;
        if (cellIndex <= row.getLastCellNum())
        {
            Cell cell = row.getCell(cellIndex);
            if (RIDER_ID_COLUMN.isHeaderCell(cell))
            {
                parseEmployeeHeaderRow(row);
                return true;
            }
        }

        return false;
    }

    /**
     * Find the column indices from the team member header.
     * The column variables for team members are initialized.
     *
     * @param row in the spreadsheet to look for headers.
     * @throws InvalidFormatException in case the row has some, but not all headers.
     */
    private void parseEmployeeHeaderRow(Row row) throws InvalidFormatException
    {
        for (int cellIndex = 0; cellIndex <= row.getLastCellNum(); ++cellIndex)
        {
            Cell cell = row.getCell(cellIndex);
            RIDER_ID_COLUMN.isHeaderCell(cell);
            EMPLOYEE_COLUMN.isHeaderCell(cell);
        }

        RIDER_ID_COLUMN.checkHeaderFound();
        EMPLOYEE_COLUMN.checkHeaderFound();
    }

    /**
     * Return the rider id if (s)he is an employee from a row in the spreadsheet.
     *
     * @param row the row of the spreadsheet
     * @return null, if this is not a TeamMember row
     * @throws InvalidFormatException in case the row is not formatted correctly.
     */
    private String parseEmployeeRow(Row row) throws InvalidFormatException
    {
        String riderID = RIDER_ID_COLUMN.getRowString(row);
        if (riderID != null && RIDER_ID_PATTERN.matcher(riderID).matches())
        {
            String employee = EMPLOYEE_COLUMN.getRowString(row);
            if (employee != null && employee.equalsIgnoreCase("Employee"))
            {
                return riderID;
            }
        }

        return null;
    }
}
