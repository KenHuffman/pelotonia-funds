package com.huffmancoding.pelotonia.funds;

import java.math.BigDecimal;


/**
 * Match rider and volunteer funds according to Big Lots 2014 matching policy.
 * 
 * @author khuffman
 */
public class BigLots2014Matcher extends EmployeeOnlyLevelMatcher
{
    /** The amount a rider must raise before receiving matching funds {@link #RIDER_MATCHING_LEVEL} */
    private static final BigDecimal RIDER_MATCHING_LEVEL = new BigDecimal(500);
 
    /** the amount a rider will receive after reaching {@link #RIDER_MATCHING_LEVEL} */
    private static final BigDecimal RIDER_MATCHING_AMOUNT = new BigDecimal(300);

    /** the amount a volunteer will receive. */
    private static final BigDecimal VOLUNTEER_MATCHING_AMOUNT = new BigDecimal(25);

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal getRiderMatchingLevel(TeamMember teamMember)
    {
        return RIDER_MATCHING_LEVEL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal getRiderMatchingAmount(TeamMember teamMember)
    {
        return RIDER_MATCHING_AMOUNT;
    }

    @Override
    public BigDecimal getVolunteerMatchingAmount(TeamMember teamMember)
    {
        return VOLUNTEER_MATCHING_AMOUNT;
    }
}
