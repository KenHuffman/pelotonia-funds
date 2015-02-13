package com.huffmancoding.pelotonia.funds;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;


/**
 * Match rider and volunteer funds according to Big Lots 2015 matching policy.
 * 
 * @author khuffman
 */
public class BigLots2015Matcher extends EmployeeOnlyLevelMatcher
{
    /** The amount a rider must raise before receiving matching funds {@link #RIDER_MATCHING_LEVEL} */
    private static final Map<BigDecimal, BigDecimal> riderMatchThresholdByCommitment = new TreeMap<>();
 
    /** the amount a rider will receive after reaching {@link #RIDER_MATCHING_LEVEL} */
    private static final Map<BigDecimal, BigDecimal> riderMatchAmountByCommitment = new TreeMap<>();

    public BigLots2015Matcher()
    {
        // when you reach about a third of your goal
        riderMatchThresholdByCommitment.put(new BigDecimal(1200), new BigDecimal(400));
        riderMatchThresholdByCommitment.put(new BigDecimal(1250), new BigDecimal(415));
        riderMatchThresholdByCommitment.put(new BigDecimal(1800), new BigDecimal(600));
        riderMatchThresholdByCommitment.put(new BigDecimal(2200), new BigDecimal(730));

        // the company will kick the match amount
        riderMatchAmountByCommitment.put(new BigDecimal(1200), new BigDecimal(300));
        riderMatchAmountByCommitment.put(new BigDecimal(1250), new BigDecimal(300));
        riderMatchAmountByCommitment.put(new BigDecimal(1800), new BigDecimal(400));
        riderMatchAmountByCommitment.put(new BigDecimal(2200), new BigDecimal(450));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal getRiderMatchingLevel(TeamMember teamMember)
    {
        return riderMatchThresholdByCommitment.get(teamMember.getCommitment());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal getRiderMatchingAmount(TeamMember teamMember)
    {
        return riderMatchAmountByCommitment.get(teamMember.getCommitment());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal getVolunteerMatchingAmount(TeamMember teamMember)
    {
        return BigDecimal.ZERO;
    }
}
