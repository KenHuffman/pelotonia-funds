package com.huffmancoding.pelotonia.funds;

import java.math.BigDecimal;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

/**
 * This is a column in the roster file that has a header title an values for each team member
 *
 * @author khuffman
 */
public class SpreadsheetColumn
{
    /** the header name of the column. */
    private final String name;

    /** the index of this header in the spreadsheet. */
    private int index = -1;

    /**
     * Constructor.
     *
     * @param name the title to match against the roster file cell value
     */
    public SpreadsheetColumn(String name)
    {
        this.name = name;
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
            throw new InvalidFormatException("Header row does not contain column \"" + name + "\"");
        }
    }

    /**
     * Returns true if an earlier call to {@link #isHeaderCell(Cell)} returned true.
     *
     * @return true if the header is present in the spreadsheet
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
            return new BigDecimal(cell.getNumericCellValue()).toPlainString();

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

        case Cell.CELL_TYPE_STRING:
            String stringCellValue = cell.getStringCellValue();
            if (stringCellValue.charAt(0) == '$')
            {
                stringCellValue = stringCellValue.substring(1);
                int dot = stringCellValue.indexOf('.');
                if (dot >= 0 && stringCellValue.length() > dot + 3)
                {
                    stringCellValue = stringCellValue.substring(0, dot + 3);
                }
            }
            return new BigDecimal(stringCellValue);

        default:
            return BigDecimal.ZERO;
        }
    }
}