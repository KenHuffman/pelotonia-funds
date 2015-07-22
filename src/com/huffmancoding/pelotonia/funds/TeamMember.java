package com.huffmancoding.pelotonia.funds;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a person signed up on the pelotonia.org site on our team.
 *
 * @author khuffman
 */
public class TeamMember
{
    /** The rider id of the team member. */
    private final String riderId;

    /** The first and last name of the team member. */
    private final String fullName;

    /** the member's role. */
    private final String participant;

    /** the amount the team member HAS to raise. */
    private final BigDecimal commitment;

    /** whether the rider is a high roller. */
    private final boolean isHighRoller;

    /** the amount the team member has raised on his/her own. */
    private final BigDecimal raised;

    /** the amount the team member has been given from shared funds. */
    private final List<FundAdjustment> adjustments = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param riderId the Pelotonia rider id
     * @param fullName first and last name of team member
     * @param participant whether the team member is a rider, volunteer, etc.
     * @param commitment commitment by rider or BigDecimal.ZERO
     * @param isHighRoller whether the team member is a high roller
     * @param raised amount member has raised on his/her own
     */
    public TeamMember(String riderId, String fullName, String participant, BigDecimal commitment, boolean isHighRoller, BigDecimal raised)
    {
        this.riderId = riderId;
        this.fullName = fullName;
        this.participant = participant;
        this.commitment = commitment;
        this.isHighRoller = isHighRoller;
        this.raised = raised;
    }

    /**
     * Return the riderId for this team member.
     *
     * @return the rider id
     */
    public String getRiderId()
    {
        return riderId;
    }

    /**
     * Returns the name of member.
     *
     * @return the name of member prefaced with his/her title
     */
    public String getFullName()
    {
        return participant + " " + fullName;
    }

    /**
     * Returns the amount the member is REQUIRED to raise
     *
     * @return the minimum the rider has to raise, or ZERO otherwise
     */
    public BigDecimal getCommitment()
    {
        return commitment;
    }

    /**
     * Whether the team member is a high roller.
     *
     * @return true, if this team member is a high roller.
     */
    public boolean isHighRoller()
    {
        return isHighRoller;
    }

    /**
     * Return the amount member raised by himself.
     *
     * @return the amount member raised by himself
     */
    public BigDecimal getAmountRaised()
    {
        return raised;
    }

    /**
     * Just add money to a partipants account
     *
     * @param newAdjustment the amount to add
     */
    public void addAdjustment(FundAdjustment newAdjustment)
    {
        if (isHighRoller && raised.compareTo(commitment) < 0)
        {
            throw new AssertionError("Cannot add or remove funds from " + getFullName() + " until individual goal is met.");
        }

        if (newAdjustment.getAmount().signum() != 0)
        {
            String newReason = newAdjustment.getReason();

            for (int i = 0; i < adjustments.size(); ++i)
            {
                FundAdjustment previousAdjustment = adjustments.get(i);
                if (previousAdjustment.getReason().equals(newReason))
                {
                    BigDecimal combinedAmount = newAdjustment.getAmount().add(previousAdjustment.getAmount());
                    if (combinedAmount.signum() == 0)
                    {
                        adjustments.remove(i);
                    }
                    else
                    {
                        adjustments.set(i, new FundAdjustment(newReason, combinedAmount));
                    }

                    return;
                }
            }

            adjustments.add(newAdjustment);
        }
    }

    /**
     * The List of current adjustments.
     *
     * @return the list in order added
     */
    public List<FundAdjustment> getAdjustments()
    {
        return adjustments;
    }

    /**
     * Return the amount of an adjust for a particular reason
     * @param reason the reason to search for
     * @return the amount or ZERO if there is none
     */
    public BigDecimal findAdjustmentAmount(String reason)
    {
        for (FundAdjustment adjustment : adjustments)
        {
            if (adjustment.getReason().equals(reason))
            {
                return adjustment.getAmount();
            }
        }

        return BigDecimal.ZERO;
    }

    /**
     * Return the total of all current adjustments.
     *
     * @return the total
     */
    public BigDecimal getAdjustmentTotal()
    {
        BigDecimal totalAdjustments = BigDecimal.ZERO;
        for (FundAdjustment adjustment : adjustments)
        {
            totalAdjustments = totalAdjustments.add(adjustment.getAmount());
        }

        return totalAdjustments;
    }

    /**
     * Whether this team member has requirement to raise funds.
     *
     * @return true, if the rider has been committed to raise funds
     */
    public boolean isRider()
    {
        return participant.equalsIgnoreCase("Rider");
    }

    /**
     * Whether this team member has requirement to raise funds.
     *
     * @return true, if the rider has been committed to raise funds
     */
    public boolean isVolunteer()
    {
        return participant.equalsIgnoreCase("Volunteer");
    }

    /**
     * Return the amount that a rider is short of his/her committment.
     *
     * @return the amount they raised plus any amount shared with them
     */
    public BigDecimal getShortfall()
    {
        return commitment.subtract(raised).subtract(getAdjustmentTotal());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return getFullName();
    }
}
