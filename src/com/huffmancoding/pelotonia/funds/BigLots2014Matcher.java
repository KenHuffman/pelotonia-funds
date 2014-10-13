package com.huffmancoding.pelotonia.funds;

import java.math.BigDecimal;
import java.util.List;

/**
 * Match rider and volunteer funds according to Big Lots 2014 matching policy.
 * 
 * @author khuffman
 */
public class BigLots2014Matcher extends EmployeeOnlyMatcher
{
    /** The amount a rider must raise before receiving matching funds {@link #RIDER_MATCHING_LEVEL} */
    private static final BigDecimal RIDER_MATCHING_LEVEL = new BigDecimal(500);

    /** the amount a rider will receive after reaching {@link #RIDER_MATCHING_LEVEL} */
    private static final BigDecimal RIDER_MATCHING_AMOUNT = new BigDecimal(300);

    /** the amount a volunteer will receive. */ 
    private static final BigDecimal VOLUNTEER_MATCHING_AMOUNT = new BigDecimal(25);

    /** the number of members who have received matching funds from the company. */
    protected int matchingCount = 0;

    /** the total amount the company has given members. */
    private BigDecimal totalMatching = BigDecimal.ZERO;

    /** the number of riders who could be eligible for matching funds if their raised more money. */
    private int ridersShortCount = 0;

    /** the total amount riders still need to raise in order to receive matching funds. */
    private BigDecimal totalShortOfMatching = BigDecimal.ZERO;

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
        FundUtils.log(ridersShortCount + " riders need to raise " + FundUtils.fmt(totalShortOfMatching) +
                " for the remaining " + FundUtils.fmt(RIDER_MATCHING_AMOUNT.multiply(new BigDecimal(ridersShortCount))) + " matching funds.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FundAdjustment getMatchingForEmployee(TeamMember teamMember)
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

    /**
     * Return the matching amount a rider should receive from the company.
     *
     * @param teamMember the member to check
     * @return {@value #RIDER_MATCHING_AMOUNT} if the rider employee has met the threshold {@value #RIDER_MATCHING_LEVEL}
     */
    private FundAdjustment getMatchingForRider(TeamMember teamMember)
    {
        BigDecimal shortOfMatching = RIDER_MATCHING_LEVEL.subtract(teamMember.getAmountRaised());
        if (shortOfMatching.signum() <= 0)
        {
            ++matchingCount;
            totalMatching = totalMatching.add(RIDER_MATCHING_AMOUNT);

            FundUtils.log(teamMember.getFullName() + " earned matching " + FundUtils.fmt(RIDER_MATCHING_AMOUNT) +
                    " after raising " + FundUtils.fmt(teamMember.getAmountRaised()) + ".");
            return new FundAdjustment("Company rider match", RIDER_MATCHING_AMOUNT);
        }
        else 
        {
            ++ridersShortCount;
            totalShortOfMatching = totalShortOfMatching.add(shortOfMatching);

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
        totalMatching = totalMatching.add(VOLUNTEER_MATCHING_AMOUNT);

        FundUtils.log(teamMember.getFullName() + " earned matching " + FundUtils.fmt(VOLUNTEER_MATCHING_AMOUNT) + " for volunteering.");
        return new FundAdjustment("Company volunteer match", VOLUNTEER_MATCHING_AMOUNT);
    }
}
