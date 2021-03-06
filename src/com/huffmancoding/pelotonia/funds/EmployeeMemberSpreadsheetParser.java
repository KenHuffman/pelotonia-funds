package com.huffmancoding.pelotonia.funds;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

/**
 * This class reads an excel spreadsheet that contains rows for each team member and sharable funds from Pelotonia.
 *
 * The XLSX format should be (it may have additional ignored columns after the first column):
 * 
 * "Rider ID","Employee","Paid"
 * riderId1,{"Employee"|"Family"},"y"
 * riderId2,{"Employee"|"Family"},"n"
 * ...
 *
 * @author khuffman
 */
public class EmployeeMemberSpreadsheetParser extends SpreadsheetParser
{
    /** header title for the column that contains team member rider ID */
    private static final SpreadsheetColumn RIDER_ID_COLUMN = new SpreadsheetColumn("Rider ID");

    /** header title for the column that contains whether the member is an employee or not */
    private static final SpreadsheetColumn EMPLOYEE_COLUMN = new SpreadsheetColumn("Employee");

    /** header title for the column that contains whether the member is an employee or not */
    private static final SpreadsheetColumn PAID_COLUMN = new SpreadsheetColumn("Paid");

    /** The format of the rider id assigned by Pelotonia. */
    private static final Pattern RIDER_ID_PATTERN = Pattern.compile("[A-Z][A-Z][0-9][0-9][0-9][0-9]");

    /** the list of team members in the file. */
    private final Map<String, BigDecimal> employeRiderIDs = new TreeMap<>();

    /**
     * Constructor.
     *
     * @param url the URL to read
     * @param sheetName the name of sheet containing the data
     */
    public EmployeeMemberSpreadsheetParser(URL url, String sheetName)
    {
        super(url, sheetName);
    }

    /**
     * Whether the team member qualifies for match.
     *
     * @param riderId the id of the rider
     * @return true if member qualifies for match, false otherwise
     */
    public boolean isEmployee(String riderId)
    {
        return employeRiderIDs.containsKey(riderId);
    }

    /**
     * Whether the employee's current fund balance includes the paid match.
     *
     * @param riderId the id of the rider
     * @return amount of the match that has already been pad
     */
    public BigDecimal getMatchPaid(String riderId)
    {
        return employeRiderIDs.get(riderId);
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
     *
     * Parse the employee row for the rider and employee and paid flags.
     */
    @Override
    protected void parseObjectRow(Row row) throws InvalidFormatException
    {
        String riderID = RIDER_ID_COLUMN.getRowString(row);
        if (riderID != null && RIDER_ID_PATTERN.matcher(riderID).matches())
        {
            String employee = EMPLOYEE_COLUMN.getRowString(row);
            if (employee != null && employee.equalsIgnoreCase("Employee"))
            {
                BigDecimal paid = PAID_COLUMN.getRowBigDecimal(row);
                employeRiderIDs.put(riderID, paid);
            }
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
            PAID_COLUMN.isHeaderCell(cell);
        }

        RIDER_ID_COLUMN.checkHeaderFound();
        EMPLOYEE_COLUMN.checkHeaderFound();
        PAID_COLUMN.checkHeaderFound();
    }
}
