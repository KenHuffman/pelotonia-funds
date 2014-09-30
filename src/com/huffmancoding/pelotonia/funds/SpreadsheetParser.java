package com.huffmancoding.pelotonia.funds;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * This class reads an excel spreadsheet that contains rows for each team member and sharable funds.
 *
 * The XLSX format should be (it may have additional ignored columns after the first column):
 * 
 * "Rider ID","First Name","Last Name","Employee","Commitment","Amount Raised"
 * riderId1,firstName1,lastName1,{"Employee"|"Family"},commitment1,raised1
 * riderId2,firstName2,lastName2,{"Employee"|"Family"},commitment1,raised2
 * ...
 * "Fund Source","Amount","Fund Type"
 * source1,amount1,{"Additional"|...}
 * source2,amount2,{"Additional"|...}
 * ...
 *
 * @author khuffman
 */
public class SpreadsheetParser
{
    static private class Column
    {
        private final String name;
        private final boolean isRequired;
        private int index = -1;

        public Column(String name, boolean isRequired)
        {
            this.name = name;
            this.isRequired = isRequired;
        }

        public String getName()
        {
            return name;
        }

        public boolean isHeaderCell(Cell cell)
        {
            if (cell != null && cell.getCellType() == Cell.CELL_TYPE_STRING)
            {
                String cellValue = cell.getStringCellValue();
                if (cellValue.equalsIgnoreCase(name))
                {
                    index = cell.getColumnIndex();
                    return true;
                }
            }

            return false;
        }

        /**
         * Verifies that a column has been found in the spreadsheet
         *
         * @throws InvalidFormatException thrown if the column is missing but required, if optional it is only logged
         */
        public void checkHeaderFound() throws InvalidFormatException
        {
            if (index < 0)
            {
                String message = "Header row does not contain column \"" + name + "\"";
                if (isRequired)
                {
                    throw new InvalidFormatException(message);
                }

                FundUtils.log(message);
            }
        }

        public boolean isHeaderFound()
        {
            return index >= 0;
        }

        /**
         * Return a string value for a row and column in the spreadsheet.
         *
         * @param row to read from
         * @return the value at that row and column
         * @throws InvalidFormatException if the cell does not contain a valid string value
         */
        public String getRowString(Row row) throws InvalidFormatException
        {
            Cell cell = row.getCell(index);
            if (cell == null ||
                cell.getColumnIndex() != index)
            {
                return null;
            }

            int cellType = cell.getCellType();
            switch (cellType)
            {
            case Cell.CELL_TYPE_STRING:
                return cell.getStringCellValue();

            case Cell.CELL_TYPE_NUMERIC:
                return getRowBigDecimal(row).toPlainString();

            default:
                return null;
            }
        }

        /**
         * Return a BigDecimal value for a row and column in the spreadsheet.
         *
         * @param row to read from
         * @return the value at that row and column
         * @throws InvalidFormatException if the cell does not contain a valid BigDecimal value
         */
        public BigDecimal getRowBigDecimal(Row row) throws InvalidFormatException
        {
            Cell cell = row.getCell(index);
            if (cell == null ||
                cell.getColumnIndex() != index)
            {
                throw new InvalidFormatException("No cell at column: " + index);
            }

            int cellType = cell.getCellType();
            switch (cellType)
            {
            case Cell.CELL_TYPE_NUMERIC:
                return new BigDecimal(cell.getNumericCellValue());

            default:
                return BigDecimal.ZERO;
            }
        }
    }

    /** header title for the column that contains team member rider ID */
    private static final Column RIDER_ID_COLUMN = new Column("Rider ID", true);

    /** header title for the column that contains team member first name */
    private static final Column FIRST_NAME_COLUMN = new Column("First Name", true);

    /** header title for the column that contains team member last name */
    private static final Column LAST_NAME_COLUMN = new Column("Last Name", true);

    /** header title for the column that contains team member Employee value: "Employee" or "Family". */
    private static final Column EMPLOYEE_COLUMN = new Column("Employee", false);

    /** header title for the column that contains team member commitment */
    private static final Column COMMITMENT_COLUMN = new Column("Commitment", true);

    /** header title for the column that contains team member amount individually raised */
    private static final Column AMOUNT_RAISED_COLUMN = new Column("Amount Raised", true);

    /** The format of the rider id assigned by Pelotonia. */
    private static final Pattern RIDER_ID_PATTERN = Pattern.compile("[A-Z][A-Z][0-9][0-9][0-9][0-9]");

    /** header title for the column that contains the name of the fund source. */
    private static final Column FUND_SOURCE_COLUMN = new Column("Fund Source", false);

    /** header title for the column that contains the fund source amount. */
    private static final Column FUND_AMOUNT_COLUMN = new Column("Amount", true);

    /** header title for the column that contains "Additional" if it should be included in funds to be shared. */
    private static final Column FUND_TYPE_COLUMN = new Column("Fund Type", true);

    /** The Excel file to read. */
    private final File rosterFile;

    /** the list of team members in the file. */
    List<TeamMember> teamMemberList = new ArrayList<>();

    BigDecimal initialAmountRaised = BigDecimal.ZERO;

    /** the running total of the {@link #fundAmountColumn}. */
    private BigDecimal shareableFunds = BigDecimal.ZERO;

    /**
     * Constructor for reading a spreadsheet file.
     *
     * @param rosterFile the file to read
     */
    public SpreadsheetParser(File rosterFile)
    {
        this.rosterFile = rosterFile;
    }

    /**
     * Load the TeamMembers from the {@link #rosterFile}.
     *
     * @throws IOException in case the problem reading the file
     * @throws InvalidFormatException in case of syntax or semantic xlsx format errors
     */
    public void loadRosterFile() throws IOException, InvalidFormatException
    {
        Date dateOfFile = new Date(rosterFile.lastModified());
        FundUtils.log("Loading spreadsheet " + rosterFile.getPath() + " dated " + dateOfFile + ".");

        try (FileInputStream rosterStream = new FileInputStream(rosterFile))
        {
            Workbook wb = WorkbookFactory.create(rosterStream);
            Sheet sheet = wb.getSheetAt(0);

            int rowCount = sheet.getLastRowNum();
            for (int rowIndex = 0; rowIndex < rowCount; ++rowIndex)
            {
                Row row = sheet.getRow(rowIndex);
                if (row != null)
                {
                    parseRow(row);
                }
            }
        }

        RIDER_ID_COLUMN.checkHeaderFound();
        FUND_SOURCE_COLUMN.checkHeaderFound();

        if (teamMemberList.isEmpty())
        {
            throw new InvalidFormatException("Roster file does not have any valid rider IDs below '" + RIDER_ID_COLUMN.getName() + "' header");
        }

        FundUtils.log("There are " + teamMemberList.size() + " Team BIG members.");
        FundUtils.log("Initial individually raised funds: " + FundUtils.fmt(initialAmountRaised) + ".");
        FundUtils.log("Initial peloton shareable funds: " + FundUtils.fmt(shareableFunds) + ".");
    }

    /**
     * Return the parsed TeamMembers
     *
     * @return the list of TeamMembers
     */
    public List<TeamMember> getTeamMembers()
    {
        return teamMemberList;
    }

    /**
     * Return the total sharable funds.
     *
     * @return total of sharable fund lines
     */
    public BigDecimal getSharableFunds()
    {
        return shareableFunds;
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

            TeamMember teamMember = parseTeamMemberRow(row);
            if (teamMember != null)
            {
                teamMemberList.add(teamMember);
                initialAmountRaised = initialAmountRaised.add(teamMember.getAmountRaised());
                return;
            }

            BigDecimal sharedAmount = parseAdditionalFundsRow(row);
            if (sharedAmount != null)
            {
                shareableFunds = shareableFunds.add(sharedAmount);
                return;
            }
        }
        catch (Exception ex)
        {
            InvalidFormatException rowException = new InvalidFormatException("Error on row " + row.getRowNum() + " of " + rosterFile.getPath());
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
    private boolean parseHeaderRow(Row row) throws InvalidFormatException
    {
        int cellIndex = 0;
        if (cellIndex < row.getLastCellNum())
        {
            Cell cell = row.getCell(cellIndex);
            if (RIDER_ID_COLUMN.isHeaderCell(cell))
            {
                parseTeamMemberHeaderRow(row);
                return true;
            }
            else if (FUND_SOURCE_COLUMN.isHeaderCell(cell))
            {
                parseFundsSourceHeaderRow(row);
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
    private void parseTeamMemberHeaderRow(Row row) throws InvalidFormatException
    {
        int cellCount = row.getLastCellNum();
        for (int cellIndex = 0; cellIndex < cellCount; ++cellIndex)
        {
            Cell cell = row.getCell(cellIndex);
            FIRST_NAME_COLUMN.isHeaderCell(cell);
            LAST_NAME_COLUMN.isHeaderCell(cell);
            EMPLOYEE_COLUMN.isHeaderCell(cell);
            COMMITMENT_COLUMN.isHeaderCell(cell);
            AMOUNT_RAISED_COLUMN.isHeaderCell(cell);
        }

        FIRST_NAME_COLUMN.checkHeaderFound();
        LAST_NAME_COLUMN.checkHeaderFound();
        EMPLOYEE_COLUMN.checkHeaderFound();
        COMMITMENT_COLUMN.checkHeaderFound();
        AMOUNT_RAISED_COLUMN.checkHeaderFound();
    }

    /**
     * Find the column indices from the fund sources header.
     * The column variables for fund sources are initialized.
     *
     * @param row in the spreadsheet to look for headers.
     * @throws InvalidFormatException in case the row has some, but not all headers.
     */
    private void parseFundsSourceHeaderRow(Row row) throws InvalidFormatException
    {
        int cellCount = row.getLastCellNum();
        for (int cellIndex = 0; cellIndex < cellCount; ++cellIndex)
        {
            Cell cell = row.getCell(cellIndex);
            FUND_AMOUNT_COLUMN.isHeaderCell(cell);
            FUND_TYPE_COLUMN.isHeaderCell(cell);
        }

        FUND_AMOUNT_COLUMN.checkHeaderFound();
        FUND_TYPE_COLUMN.checkHeaderFound();
    }

    /**
     * Return a TeamMember from a row in the spreadsheet.
     *
     * @param row the row of the spreadsheet
     * @return null, if this is not a TeamMember row
     * @throws InvalidFormatException in case the row is not formatted correctly.
     */
    private TeamMember parseTeamMemberRow(Row row) throws InvalidFormatException
    {
        if (RIDER_ID_COLUMN.isHeaderFound())
        {
            String riderID = RIDER_ID_COLUMN.getRowString(row);
            if (riderID != null && RIDER_ID_PATTERN.matcher(riderID).matches())
            {
                String fullName = FIRST_NAME_COLUMN.getRowString(row) + " " + LAST_NAME_COLUMN.getRowString(row);
                BigDecimal commitment = COMMITMENT_COLUMN.getRowBigDecimal(row);
                BigDecimal raised = AMOUNT_RAISED_COLUMN.getRowBigDecimal(row);
                boolean isEmployee;
                if (EMPLOYEE_COLUMN.isHeaderFound())
                {
                    isEmployee = EMPLOYEE_COLUMN.getRowString(row).equalsIgnoreCase("employee");
                }
                else
                {
                    isEmployee = true;
                }

                TeamMember teamMember = new TeamMember(fullName, isEmployee, commitment, raised);
                if (teamMember.isRider() || teamMember.getAmountRaised().signum() > 0)
                {
                    FundUtils.log(teamMember.getFullName() + " raised " + FundUtils.fmt(teamMember.getAmountRaised()) + ".");
                }

                return teamMember;
            }
        }

        return null;
    }

    /**
     * Return a shared funds amount from a row in the spreadsheet.
     *
     * @param row the row of the spreadsheet
     * @return null, if this is not a shared funds row
     * @throws InvalidFormatException in case the row is not formatted correctly.
     */
    private BigDecimal parseAdditionalFundsRow(Row row) throws InvalidFormatException
    {
        if (FUND_SOURCE_COLUMN.isHeaderFound())
        {
            String fundType = FUND_TYPE_COLUMN.getRowString(row);
            if (fundType != null && fundType.equalsIgnoreCase("Additional"))
            {
                String fundSource = FUND_SOURCE_COLUMN.getRowString(row);
                BigDecimal fundAmount = FUND_AMOUNT_COLUMN.getRowBigDecimal(row);
                FundUtils.log("Shared fund: " + fundSource + " with " + FundUtils.fmt(fundAmount) + ".");
                return fundAmount;
            }
        }

        return null;
    }

}
