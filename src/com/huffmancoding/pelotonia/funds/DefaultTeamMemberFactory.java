package com.huffmancoding.pelotonia.funds;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;

/**
 * This the class that creates TeamMembers from rows in a spreadsheet.
 *
 * @author khuffman
 */
public class DefaultTeamMemberFactory implements TeamMemberFactory
{
    /** header title for the column that contains team member first name */
    private static final SpreadsheetColumn FIRST_NAME_COLUMN = new SpreadsheetColumn("First Name", true);

    /** header title for the column that contains team member last name */
    private static final SpreadsheetColumn LAST_NAME_COLUMN = new SpreadsheetColumn("Last Name", true);

    /** header title for the column that contains team member commitment */
    private static final SpreadsheetColumn COMMITMENT_COLUMN = new SpreadsheetColumn("Commitment", true);

    /** header title for the column that contains team member amount individually raised */
    private static final SpreadsheetColumn AMOUNT_RAISED_COLUMN = new SpreadsheetColumn("Amount Raised", true);

    /** the list of columns to check for in the spreadsheet. */
    private List<SpreadsheetColumn> necessaryColumns = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param additionalColumns the columns that are expected in the spreadsheet
     */
    DefaultTeamMemberFactory(List<SpreadsheetColumn> additionalColumns)
    {
        necessaryColumns.add(FIRST_NAME_COLUMN);
        necessaryColumns.add(LAST_NAME_COLUMN);
        necessaryColumns.add(COMMITMENT_COLUMN);
        necessaryColumns.add(AMOUNT_RAISED_COLUMN);
        necessaryColumns.addAll(additionalColumns);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SpreadsheetColumn> getTeamMemberColumns()
    {
        return necessaryColumns;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TeamMember createTeamMember(Row row) throws InvalidFormatException
    {
        String fullName = FIRST_NAME_COLUMN.getRowString(row) + " " + LAST_NAME_COLUMN.getRowString(row);
        BigDecimal commitment = COMMITMENT_COLUMN.getRowBigDecimal(row);
        BigDecimal raised = AMOUNT_RAISED_COLUMN.getRowBigDecimal(row);

        TeamMember teamMember = new TeamMember(fullName, commitment, raised);
        for (SpreadsheetColumn column : necessaryColumns)
        {
            if (column.isHeaderFound())
            {
                String value = column.getRowString(row);
                if (value != null)
                {
                    teamMember.setAdditionalProperty(column.getName(), value);
                }
            }
        }

        return teamMember;
    }
}
