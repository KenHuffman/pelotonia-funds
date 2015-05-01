package com.huffmancoding.pelotonia.funds;

import java.math.BigDecimal;

/**
 * An adjustment made by the captain to a team member's funds at the close of the fundraising period.
 *
 * @author khuffman
 */
public class FundAdjustment
{
    /** the reason why the team member has this adjustment. */
    private final String reason;

    /** the amount added (removed, if negative) from an individuals account. */
    private final BigDecimal amount;

    /**
     * Constructor.
     *
     * @param r the reason for the adjustment
     * @param a the amount of the adjustment
     */
    public FundAdjustment(String r, BigDecimal a)
    {
        reason = r;
        amount = a;
    }

    /**
     * Get the reason for the adjustment.
     *
     * @return the reason
     */
    public String getReason()
    {
        return reason;
    }

    /**
     * Get the amount of the adjustment.
     *
     * @return the amount
     */
    public BigDecimal getAmount()
    {
        return amount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return reason + ": " + FundUtils.fmt(amount);
    }
}
