package com.huffmancoding.pelotonia.funds;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

/**
 * This class reads an excel spreadsheet that contains rows for each team member and sharable funds from Pelotonia.
 *
 * @author khuffman
 */
public class TeamMemberSpreadsheetParser extends SpreadsheetParser
{
    /** header title for the column that contains team member rider ID */
    public static final SpreadsheetColumn RIDER_ID_COLUMN = new SpreadsheetColumn("Rider ID");

    /** header title for the column that contains team member first name */
    private static final SpreadsheetColumn FIRST_NAME_COLUMN = new SpreadsheetColumn("First Name");

    /** header title for the column that contains team member last name */
    private static final SpreadsheetColumn LAST_NAME_COLUMN = new SpreadsheetColumn("Last Name");

    /** header title for the column that contains whether the member is a Rider, Virtual Rider, or Volunteer */
    private static final SpreadsheetColumn PARTICIPANT_COLUMN = new SpreadsheetColumn("Participant");

    /** header title for the column that contains the high roller flag "Yes" */
    private static final SpreadsheetColumn HIGH_ROLLER_COLUMN = new SpreadsheetColumn("High Roller");

    /** header title for the column that contains team member commitment */
    private static final SpreadsheetColumn COMMITMENT_COLUMN = new SpreadsheetColumn("Commitment");

    /** header title for the column that contains team member amount individually raised */
    private static final SpreadsheetColumn AMOUNT_RAISED_COLUMN = new SpreadsheetColumn("Amount Raised");

    /** The format of the rider id assigned by Pelotonia. */
    private static final Pattern RIDER_ID_PATTERN = Pattern.compile("[A-Z][A-Z][0-9][0-9][0-9][0-9]");

    private final List<SpreadsheetColumn> teamMemberColumns = new ArrayList<>();

    private BigDecimal initialAmountRaised = BigDecimal.ZERO;

    /** the list of team members in the file. */
    List<TeamMember> teamMemberList = new ArrayList<>();

    public TeamMemberSpreadsheetParser(URL url)
    {
        super(url, null);

        teamMemberColumns.add(FIRST_NAME_COLUMN);
        teamMemberColumns.add(LAST_NAME_COLUMN);
        teamMemberColumns.add(PARTICIPANT_COLUMN);
        teamMemberColumns.add(HIGH_ROLLER_COLUMN);
        teamMemberColumns.add(COMMITMENT_COLUMN);
        teamMemberColumns.add(AMOUNT_RAISED_COLUMN);
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
     * Load the current spreadsheet from pelotonia.org
     *
     * @throws IOException in case the problem reading the file
     * @throws InvalidFormatException in case of syntax or semantic xlsx format errors
     */
    public void loadPelotoniaSpreadsheet() throws IOException, InvalidFormatException
    {
        super.loadSpreadsheet("individually raised funds");

        RIDER_ID_COLUMN.checkHeaderFound();
        if (teamMemberList.isEmpty())
        {
            throw new InvalidFormatException("Roster file does not have any valid rider IDs below '" + RIDER_ID_COLUMN.getName() + "' header");
        }

        FundUtils.log("There are " + teamMemberList.size() + " team members.");
        FundUtils.log("Initial individually raised funds: " + FundUtils.fmt(initialAmountRaised) + ".");
    }

    /**
     * {@inheritDoc}
     */
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
        String riderID = RIDER_ID_COLUMN.getRowString(row);
        if (riderID != null && RIDER_ID_PATTERN.matcher(riderID).matches())
        {
            TeamMember teamMember = createTeamMember(row);
            if (teamMember.isRider() || teamMember.getAmountRaised().signum() > 0)
            {
                FundUtils.log(teamMember.getFullName() + " raised " + FundUtils.fmt(teamMember.getAmountRaised()) + ".");
            }

            return teamMember;
        }

            return null;
    }

    /**
     * Return a TeamMember for a row in the spreadsheet.
     *
     * @param row the spreadsheet row
     * @return the TeamMember with properties set
     */
    public TeamMember createTeamMember(Row row) throws InvalidFormatException
    {
        String riderId = RIDER_ID_COLUMN.getRowString(row);
        String fullName = FIRST_NAME_COLUMN.getRowString(row) + " " + LAST_NAME_COLUMN.getRowString(row);
        String participant = PARTICIPANT_COLUMN.getRowString(row);
        BigDecimal commitment = COMMITMENT_COLUMN.getRowBigDecimal(row);
        String highRollerValue = HIGH_ROLLER_COLUMN.getRowString(row);
        boolean isHighRoller = highRollerValue != null && !highRollerValue.trim().isEmpty() && !highRollerValue.equalsIgnoreCase("No");
        BigDecimal raised = AMOUNT_RAISED_COLUMN.getRowBigDecimal(row);

        return new TeamMember(riderId, fullName, participant, commitment, isHighRoller, raised);
    }
}
