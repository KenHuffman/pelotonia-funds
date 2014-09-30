package com.huffmancoding.pelotonia.funds;

import java.math.BigDecimal;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

/**
 * This is a column in the roster file that has a header title an values for each team member
 * @author khuffman
 */
public class SpreadsheetColumn
{
    private final String name;
    private final boolean isRequired;
    private int index = -1;

    /**
     * Constructor.
     *
     * @param name the title to match against the roster file cell value
     * @param isRequired whether this column has to exist in the spreadsheet
     */
    public SpreadsheetColumn(String name, boolean isRequired)
    {
        this.name = name;
        this.isRequired = isRequired;
    }

    /**
     * Return the header name of this column.
     *
     * @return the header name of this column
     */
    public String getName()
    {
        return name;
    }

    /**
     * Whether this cell contains the name of this column.
     *
     * @param cell the cell to look at
     * @return true if this the cell that matches the name of this column
     */
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
        if (! isHeaderFound())
        {
            String message = "Header row does not contain column \"" + name + "\"";
            if (isRequired)
            {
                throw new InvalidFormatException(message);
            }

            FundUtils.log(message);
        }
    }

    /**
     * Returns true if an earlier call to {@link #isHeaderCell(Cell)} returned true.
     *
     * @return
     */
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