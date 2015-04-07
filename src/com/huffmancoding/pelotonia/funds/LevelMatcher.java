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
    private EmployeeMemberSpreadsheetParser employeeMemberParser = null;

    /** the number of members who have received matching funds from the company. */
    protected int matchingCount = 0;

    /** the total amount the company has given members. */
    private BigDecimal totalMatching = BigDecimal.ZERO;

    /** the number of riders who could be eligible for matching funds if their raised more money. */
    private int ridersShortCount = 0;

    /** the total amount riders still need to raise in order to receive matching funds. */
    private BigDecimal totalShortOfMatchingLevel = BigDecimal.ZERO;

    /** the total amount riders will receive once those short of level reach it.. */
    private BigDecimal totalUnattainedMatchingAmount = BigDecimal.ZERO;

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
                return getMatchingForVolunteer(teamMember);
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
     * @return {@value #RIDER_MATCHING_AMOUNT} if the rider employee has met the threshold {@value #RIDER_MATCHING_LEVEL}
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
     * @return a fixed {@value #VOLUNTEER_MATCHING_AMOUNT} for volunteering employees.
     */
    private FundAdjustment getMatchingForVolunteer(TeamMember teamMember)
    {
        ++matchingCount;
        BigDecimal volunteerMatchAmount = getVolunteerMatchingAmount(teamMember);
        if (volunteerMatchAmount.signum() != 0)
        {
            totalMatching = totalMatching.add(volunteerMatchAmount);

            FundUtils.log(teamMember.getFullName() + " earned matching " + FundUtils.fmt(volunteerMatchAmount) + " for volunteering.");
            return new FundAdjustment("Company volunteer match", volunteerMatchAmount);
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
        String[] values = getMatchAmountPair(commitment.toPlainString());
        return new BigDecimal(values[0]);
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
        String[] values = getMatchAmountPair(commitment.toPlainString());
        return new BigDecimal(values[1]);
    }

    private String[] getMatchAmountPair(String suffix)
    {
        String propertyName = "matcher_amount_" + suffix;
        String pair = getProperties().getProperty(propertyName);
        if (pair == null)
        {
            throw new IllegalArgumentException("Property file does not have: " + propertyName);
        }

        return pair.split(",");
    }

    /**
     * Return the amount volunteers with not commitment will receive.
     *
     * @param teamMember without commitment
     * @return the amount he will receive
     */
    public BigDecimal getVolunteerMatchingAmount(TeamMember teamMember)
    {
        String[] values = getMatchAmountPair("volunteer");
        return new BigDecimal(values[1]);
    }
}