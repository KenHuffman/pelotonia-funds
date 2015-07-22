package com.huffmancoding.pelotonia.funds;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.Properties;

/**
 * Match a team member that reaches a particular fund level.
 * 
 * @author khuffman
 */
public class LevelMatcher extends CompanyMatcher
{
    /** the parser that knows whether a team member is an employee. */
    private EmployeeMemberSpreadsheetParser employeeMemberParser = null;

    /** the number of members who have received matching funds from the company. */
    private int matchingCount = 0;

    /** the total amount the company has given members. */
    private BigDecimal totalMatching = BigDecimal.ZERO;

    /** the number of riders who could be eligible for matching funds if their raised more money. */
    private int ridersShortCount = 0;

    /** the total amount riders still need to raise in order to receive matching funds. */
    private BigDecimal totalShortOfMatchingLevel = BigDecimal.ZERO;

    /** the total amount riders will receive once those short of level reach it.. */
    private BigDecimal totalUnattainedMatchingAmount = BigDecimal.ZERO;

    /**
     * Constructor.
     *
     * @param properties the properties necessary for configuring the match algorithm
     * @throws Exception in case of error
     */
    public LevelMatcher(Properties properties) throws Exception
    {
        super(properties);

        String employeeURL = properties.getProperty("matcher_spreadsheet");
        if (employeeURL != null)
        {
            String sheetName = properties.getProperty("matcher_sheetname");
            employeeMemberParser = new EmployeeMemberSpreadsheetParser(new URL(employeeURL), sheetName);
            employeeMemberParser.loadEmployeeSpreadsheet();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Overridden to add logging
     */
    @Override
    public void addFundsToTeamMembers(List<TeamMember> teamMemberList)
    {
        super.addFundsToTeamMembers(teamMemberList);

        FundUtils.log(matchingCount + " members earned " + FundUtils.fmt(totalMatching) + " matching funds.");
        FundUtils.log(ridersShortCount + " riders need to raise " + FundUtils.fmt(totalShortOfMatchingLevel) +
                " for the remaining " + FundUtils.fmt(totalUnattainedMatchingAmount) + " matching funds.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FundAdjustment getMatchingForTeamMember(TeamMember teamMember)
    {
        if (isEmployee(teamMember))
        {
            if (teamMember.isRider())
            {
                return getMatchingForRider(teamMember);
            }
            else
            {
                return getMatchingForNonRider(teamMember);
            }
        }
        else
        {
            FundUtils.log(teamMember.getFullName() + " is not an employee eligible for matching funds.");
            return null;
        }
    }

    /**
     * Whether the team member qualifies for match.
     *
     * @param teamMember the team member to check
     * @return true if member qualifies for match, false otherwise
     */
    public boolean isEmployee(TeamMember teamMember)
    {
        return employeeMemberParser == null || employeeMemberParser.isEmployee(teamMember.getRiderId());
    }

    /**
     * Return the matching amount a rider should receive from the company.
     *
     * @param teamMember the member to check
     * @return the FundAdjust from the company if the rider employee has met his threshold
     */
    private FundAdjustment getMatchingForRider(TeamMember teamMember)
    {
        BigDecimal shortOfMatching = getRiderMatchingLevel(teamMember).subtract(teamMember.getAmountRaised());
        if (shortOfMatching.signum() <= 0)
        {
            ++matchingCount;
            BigDecimal riderMatchingAmount = getRiderMatchingAmount(teamMember);
            totalMatching = totalMatching.add(riderMatchingAmount);

            FundUtils.log(teamMember.getFullName() + " earned matching " + FundUtils.fmt(riderMatchingAmount) +
                    " after raising " + FundUtils.fmt(teamMember.getAmountRaised()) + ".");
            return new FundAdjustment("Company rider match", riderMatchingAmount);
        }
        else 
        {
            ++ridersShortCount;
            totalShortOfMatchingLevel = totalShortOfMatchingLevel.add(shortOfMatching);
            totalUnattainedMatchingAmount = totalUnattainedMatchingAmount.add(getRiderMatchingAmount(teamMember));

            FundUtils.log(teamMember.getFullName() + " needs to raise " + FundUtils.fmt(shortOfMatching) + " before receiving matching funds.");
            return null;
        }
    }

    /**
     * Return the matching amount a volunteer should receive from the company.
     *
     * @param teamMember the member to check
     * @return the FundAdjust from the company for employee volunteers
     */
    private FundAdjustment getMatchingForNonRider(TeamMember teamMember)
    {
        ++matchingCount;
        BigDecimal nonRiderMatchAmount = getNonRideratchingAmount(teamMember);
        if (nonRiderMatchAmount.signum() != 0)
        {
            totalMatching = totalMatching.add(nonRiderMatchAmount);

            FundUtils.log(teamMember.getFullName() + " earned matching " + FundUtils.fmt(nonRiderMatchAmount) + " for being a non-rider.");
            return new FundAdjustment("Company non-rider match", nonRiderMatchAmount);
        }

        return null;
    }

    /**
     * Return the amount the rider must reach before he receives the matching amount.
     *
     * @param teamMember with a commitment
     * @return the amount he has to raise
     */
    public BigDecimal getRiderMatchingLevel(TeamMember teamMember)
    {
        BigDecimal commitment = teamMember.getCommitment();
        BigDecimal[] values = getMatchAmountPair(commitment.toPlainString());
        return values[0];
    }

    /**
     * Return the amount the rider will receive after he reaches his matching level.
     *
     * @param teamMember with a commitment
     * @return the amount he will receive
     */
    public BigDecimal getRiderMatchingAmount(TeamMember teamMember)
    {
        BigDecimal commitment = teamMember.getCommitment();
        BigDecimal[] values = getMatchAmountPair(commitment.toPlainString());
        return values[1];
    }

    /**
     * Return an array of BigDecimal values from the properties.
     *
     * @param suffix the properties to find
     * @return an array of BigDecimals parsed for the comma separated property value
     */
    private BigDecimal[] getMatchAmountPair(String suffix)
    {
        String propertyName = "matcher_amount_" + suffix;
        String pair = getProperties().getProperty(propertyName);
        if (pair == null)
        {
            throw new IllegalArgumentException("Property file does not have: " + propertyName);
        }

        String[] strValues = pair.split(",");

        BigDecimal[] values = new BigDecimal[strValues.length];
        for (int i = 0; i < strValues.length; ++i)
        {
            values[i] = new BigDecimal(strValues[i]);
        }

        return values;
    }

    /**
     * Return the amount volunteers with not commitment will receive.
     *
     * @param teamMember without commitment
     * @return the amount he will receive
     */
    public BigDecimal getNonRideratchingAmount(TeamMember teamMember)
    {
        String suffix;
        if (teamMember.isVolunteer())
        {
            suffix = "volunteer";
        }
        else
        {
            suffix = "virtual";
        }

        BigDecimal[] values = getMatchAmountPair(suffix);
        return values[1];
    }
}