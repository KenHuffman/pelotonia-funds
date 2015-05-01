package com.huffmancoding.pelotonia.funds;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

/**
 * This class reads an excel spreadsheet that contains rows for sharable funds.
 *
 * The XLSX format should be (it may have additional ignored columns after the first column):
 * 
 * "Fund Type","Fund Source","Amount",
 * {"Additional"|...},source1,amount1
 * {"Additional"|...},source2,amount2
 * ...
 *
 * @author khuffman
 */
public class SharableFundsSpreadsheetParser extends SpreadsheetParser
{
    /** header title for the column that contains the name of the fund source. */
    public static final SpreadsheetColumn FUND_SOURCE_COLUMN = new SpreadsheetColumn("Fund Source");

    /** header title for the column that contains the fund source amount. */
    private static final SpreadsheetColumn FUND_AMOUNT_COLUMN = new SpreadsheetColumn("Amount");

    /** header title for the column that contains "Additional" if it should be included in funds to be shared. */
    private static final SpreadsheetColumn FUND_TYPE_COLUMN = new SpreadsheetColumn("Fund Type");

    /** the running total of the amount of funds that can be given to riders. */
    private BigDecimal shareableFunds = BigDecimal.ZERO;

    /**
     * Constructor.
     *
     * @param url the spreadsheet to read
     * @param sheetName the name of sheet containing the data
     */
    public SharableFundsSpreadsheetParser(URL url, String sheetName)
    {
        super(url, sheetName);
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
     * Load the spreadsheet containing sharable funds
     *
     * @throws IOException in case the problem reading the file
     * @throws InvalidFormatException in case of syntax or semantic xlsx format errors
     */
    public void loadFundsSpreadsheet() throws IOException, InvalidFormatException
    {
        super.loadSpreadsheet("shared peloton funds");

        FUND_SOURCE_COLUMN.checkHeaderFound();

        FundUtils.log("Initial peloton shareable funds: " + FundUtils.fmt(shareableFunds) + ".");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean parseHeaderRow(Row row) throws InvalidFormatException
    {
        int cellIndex = 0;
        if (cellIndex <= row.getLastCellNum())
        {
            Cell cell = row.getCell(cellIndex);
            if (FUND_SOURCE_COLUMN.isHeaderCell(cell))
            {
                parseFundsSourceHeaderRow(row);
                return true;
            }
        }

        return false;
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
        for (int cellIndex = 0; cellIndex <= row.getLastCellNum(); ++cellIndex)
        {
            Cell cell = row.getCell(cellIndex);
            FUND_AMOUNT_COLUMN.isHeaderCell(cell);
            FUND_TYPE_COLUMN.isHeaderCell(cell);
        }

        FUND_AMOUNT_COLUMN.checkHeaderFound();
        FUND_TYPE_COLUMN.checkHeaderFound();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void parseObjectRow(Row row) throws InvalidFormatException
    {
        BigDecimal sharedAmount = parseAdditionalFundsRow(row);
        if (sharedAmount != null)
        {
            shareableFunds = shareableFunds.add(sharedAmount);
            return;
        }
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
        String fundType = FUND_TYPE_COLUMN.getRowString(row);
        if (fundType != null && fundType.equalsIgnoreCase("Additional"))
        {
            String fundSource = FUND_SOURCE_COLUMN.getRowString(row);
            BigDecimal fundAmount = FUND_AMOUNT_COLUMN.getRowBigDecimal(row);
            FundUtils.log("Shared fund: " + fundSource + " with " + FundUtils.fmt(fundAmount) + ".");
            return fundAmount;
        }

        return null;
    }
}
