package com.huffmancoding.pelotonia.funds;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

/**
 * This class reads an excel spreadsheet that contains rows for each team member and sharable funds.
 *
 * The XLSX format should be (it may have additional ignored columns after the first column):
 * 
 * "Rider ID","First Name","Last Name","Employee","Commitment","Amount Raised"
 * riderId1,firstName1,lastName1,{"Employee"|"Family"},commitment1,raised1
 * riderId2,firstName2,lastName2,{"Employee"|"Family"},commitment1,raised2
 * ...
 *
 * @author khuffman
 */
public class TeamMemberSpreadsheetParser extends SpreadsheetParser
{
    /** header title for the column that contains team member rider ID */
    public static final SpreadsheetColumn RIDER_ID_COLUMN = new SpreadsheetColumn("Rider ID", true);

    /** The format of the rider id assigned by Pelotonia. */
    private static final Pattern RIDER_ID_PATTERN = Pattern.compile("[A-Z][A-Z][0-9][0-9][0-9][0-9]");

    private final TeamMemberFactory teamMemberFactory;

    private final List<SpreadsheetColumn> teamMemberColumns;

    private BigDecimal initialAmountRaised = BigDecimal.ZERO;

    /** the list of team members in the file. */
    List<TeamMember> teamMemberList = new ArrayList<>();

    public TeamMemberSpreadsheetParser(File file, TeamMemberFactory factory)
    {
        super(file);
        teamMemberFactory = factory;
        teamMemberColumns = teamMemberFactory.getTeamMemberColumns();
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

    @Override
    public void loadSpreadsheet() throws IOException, InvalidFormatException
    {
        super.loadSpreadsheet();

        RIDER_ID_COLUMN.checkHeaderFound();
        if (teamMemberList.isEmpty())
        {
            throw new InvalidFormatException("Roster file does not have any valid rider IDs below '" + RIDER_ID_COLUMN.getName() + "' header");
        }

        FundUtils.log("There are " + teamMemberList.size() + " Team BIG members.");
        FundUtils.log("Initial individually raised funds: " + FundUtils.fmt(initialAmountRaised) + ".");
    }

    @Override
    protected void parseObjectRow(Row row) throws InvalidFormatException
    {
        TeamMember teamMember = parseTeamMemberRow(row);
        if (teamMember != null)
        {
            teamMemberList.add(teamMember);
            initialAmountRaised = initialAmountRaised.add(teamMember.getAmountRaised());
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
                parseTeamMemberHeaderRow(row);
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
        for (int cellIndex = 0; cellIndex <= row.getLastCellNum(); ++cellIndex)
        {
            Cell cell = row.getCell(cellIndex);
            for (SpreadsheetColumn column : teamMemberColumns)
            {
                column.isHeaderCell(cell);
            }
        }

        for (SpreadsheetColumn column : teamMemberColumns)
        {
            column.checkHeaderFound();
        }
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
                TeamMember teamMember = teamMemberFactory.createTeamMember(row);
                if (teamMember.isRider() || teamMember.getAmountRaised().signum() > 0)
                {
                    FundUtils.log(teamMember.getFullName() + " raised " + FundUtils.fmt(teamMember.getAmountRaised()) + ".");
                }

                return teamMember;
            }
        }

        return null;
    }
}
