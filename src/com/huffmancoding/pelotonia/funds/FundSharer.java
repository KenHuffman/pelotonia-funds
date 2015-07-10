package com.huffmancoding.pelotonia.funds;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Share a pool of funds to team members who have commitments.
 * The equitable algorithm is from Daniel Reinier.
 *
 * @author khuffman
 */
public class FundSharer
{
    /** the reason a sharing member is losing funds. */
    private static final String TO_TEAMMATE_REASON = "To teammate";

    /** the reason a receiving tider is gaining funds. */
    private static final String FROM_TEAMMATE_REASON = "From teammate";

    /** the minimum that can be given for a funding round. */
    private static final BigDecimal PENNY = new BigDecimal("0.01");

    /** the list of team members on the company's team, includes volunteers and non-employees */
    private final List<TeamMember> teamMemberList;

    /** the amount of money that can be shared with individual riders if necessary. */
    private BigDecimal shareableFunds;

    /**
     * Constructor.
     *
     * @param memberList all members on the team
     */
    public FundSharer(List<TeamMember> memberList)
    {
        teamMemberList = memberList;
    }

    /**
     * Disburse money to team members in rounds, to maximize the number of riders to commitment.
     *
     * @param funds the funds that are available to share
     * @return the funds remaining after sharing, could be ZERO if the funds run out before all riders reach their commitment.
     */
    public BigDecimal allocateSharableToNonHighRollers(BigDecimal funds)
    {
        shareableFunds = funds;

        AtomicBoolean usedExcessFromMembers = new AtomicBoolean(false);

        int ridersShortCount = doFundingRounds(usedExcessFromMembers);
        if (ridersShortCount == 0)
        {
            // every reached goals
            FundUtils.log("Every rider has now reached their goal, with leftover sharable funds of " + FundUtils.fmt(shareableFunds) + ".");

            if (usedExcessFromMembers.get())
            {
                if (shareableFunds.signum() > 0)
                {
                    // return remainder of sharable funds back to those we borrowed it from. 
                    returnUnusedPelotonShareableToSharers();
                }
            }
            else
            {
                FundUtils.log("Did not have to shift funds from members who exceeded their goal.");
            }
        }
        else
        {
            FundUtils.log("Shareable funds are now exhausted. " + ridersShortCount + " riders have not reached their goal.");
        }

        if (usedExcessFromMembers.get())
        {
            assignSharersToReceivers();
        }

        return shareableFunds;
    }

    /**
     * Perform funding rounds using peloton money, or if necessary, other riders.
     *
     * @param usedExcessFromMembers set true if money from "rich" members is necessary for other riders
     * @return the number of riders who can't reach their goal, even with assistance.
     */
    private int doFundingRounds(AtomicBoolean usedExcessFromMembers)
    {
        BigDecimal maxFundingAmount = BigDecimal.ZERO;

        List<TeamMember> membersWithShortfall;
        for (int round = 1; ! (membersWithShortfall = findNonHighRollersShortOfCommitment()).isEmpty(); ++round)
        {
            BigDecimal perMember = calculateFundingRoundAmount(membersWithShortfall, usedExcessFromMembers);
            if (perMember.signum() == 0)
            {
                break;
            }

            // Sometimes the perMember will be a PENNY, and we have to give money to only some of the riders
            int receiverCount = Math.min(shareableFunds.divide(perMember, RoundingMode.DOWN).intValue(), membersWithShortfall.size());

            List<TeamMember> membersReceivingMoneyThisRound = membersWithShortfall.subList(0, receiverCount);

            String sharedReason = (usedExcessFromMembers.get() ? FROM_TEAMMATE_REASON : "From peloton");

            FundUtils.log("Funding round " + round +" has sharable " + (usedExcessFromMembers.get() ? "excess member" : "peloton") +
                    " funds of " + FundUtils.fmt(shareableFunds) + ", giving " + FundUtils.fmt(perMember) + " " + sharedReason.toLowerCase() + 
                    " to " + receiverCount + " underfunded rider(s).");

            moveSharedToMembers(sharedReason, perMember, membersReceivingMoneyThisRound);

            maxFundingAmount = maxFundingAmount.add(perMember);
        }

        FundUtils.log("Funding rounds have given up to " + FundUtils.fmt(maxFundingAmount) + " to riders short of their goal.");

        return membersWithShortfall.size();
    }

    /**
     * Find the riders who have yet to reach their goal.
     *
     * @return the list of members who need money
     */
    private List<TeamMember> findNonHighRollersShortOfCommitment()
    {
        int ridersShortOfCommitment = 0;
        BigDecimal totalRemainingShortfall = BigDecimal.ZERO;

        List<TeamMember> membersWithShortfall = new ArrayList<>();
        for (TeamMember teamMember : teamMemberList)
        {
            BigDecimal shortfall = teamMember.getShortfall();
            if (shortfall.signum() > 0 && !teamMember.isHighRoller())
            {
                ++ridersShortOfCommitment;
                totalRemainingShortfall = totalRemainingShortfall.add(shortfall);
                membersWithShortfall.add(teamMember);
            }
        }

        FundUtils.log(ridersShortOfCommitment + " riders need " + FundUtils.fmt(totalRemainingShortfall) + " to reach their goal.");

        return membersWithShortfall;
    }

    /**
     * Get the funding amount for the round, using money if available and necessary from other members.
     *
     * @param membersWithShortfall riders needing money
     * @param usedExcessFromMembers set true if money from "rich" members is necessary for other riders
     * @return the amount to give this round to members needing money
     */
    private BigDecimal calculateFundingRoundAmount(List<TeamMember> membersWithShortfall, AtomicBoolean usedExcessFromMembers)
    {
        TeamMember closestToCommitment = findMemberClosestToCommitment(membersWithShortfall);

        BigDecimal perMember = pickSmallestShortfallOrSplit(closestToCommitment, membersWithShortfall);
        if (perMember.signum() == 0)
        {
            // we have no money to share, have we used money from other riders?
            if (! usedExcessFromMembers.getAndSet(true))
            {
                FundUtils.log("Initial shared funds are exhausted, now checking for members have gone beyond their goal for fund sharing.");
                addSharersFundsToPelotonShareable();

                // try again to see if we have money to give, now that we have used money from other riders
                perMember = pickSmallestShortfallOrSplit(closestToCommitment, membersWithShortfall);
            }
        }

        return perMember;
    }

    /**
     * Return the TeamMember who is closest to his/her goal.
     *
     * @param membersWithShortfall the members to search
     * @return the one who needs the least amount of money, or null if passed empty list
     */
    private TeamMember findMemberClosestToCommitment(List<TeamMember> membersWithShortfall)
    {
        TeamMember closestToCommitment = null;
        for (TeamMember teamMember : membersWithShortfall)
        {
            if (closestToCommitment == null || teamMember.getShortfall().compareTo(closestToCommitment.getShortfall()) < 0)
            {
                closestToCommitment = teamMember;
            }
        }

        if (closestToCommitment != null)
        {
            FundUtils.log(closestToCommitment.getFullName() + " is closest to individual goal, needing " +
                    FundUtils.fmt(closestToCommitment.getShortfall()) + ".");
        }

        return closestToCommitment;
    }

    /**
     * Find the minimum of
     * 1) the amount of the rider that is closest to his commitment (but not over).
     * 2) the amount of shareable funds evenly divide among those needing additional funds.
     *
     * @param closestToCommitment the rider closest to, but not over, his goal
     * @param membersWithShortfall the riders needing more money
     * @return the smallest rider short fall or possibly BigDecimal.ZERO
     */
    private BigDecimal pickSmallestShortfallOrSplit(TeamMember closestToCommitment, List<TeamMember> membersWithShortfall)
    {
        assert ! membersWithShortfall.isEmpty();
        BigDecimal evenSplit = shareableFunds.divide(new BigDecimal(membersWithShortfall.size()), RoundingMode.DOWN);

        if (evenSplit.signum() == 0 && shareableFunds.signum() > 0)
        {
            // we have so little shared funds, that we cannot even give a penny to each rider
            // so give a penny to a some of them
            evenSplit = PENNY;
        }

        return closestToCommitment.getShortfall().min(evenSplit);
    }

    /**
     * Add to the sharable funds moneys from non-riders and riders who have exceeded their individual commitment.
     * This function can be called if the initial shared funds are not sufficient for everyone on the team to reach their commitment.
     */
    private void addSharersFundsToPelotonShareable()
    {
        int memberCount = 0;

        BigDecimal totalAmountSharedByMembers = BigDecimal.ZERO;
        for (TeamMember teamMember : teamMemberList)
        {
            BigDecimal excess = teamMember.getShortfall().negate();
            if (excess.signum() > 0)
            {
                ++memberCount;

                totalAmountSharedByMembers = totalAmountSharedByMembers.add(excess);

                FundUtils.log(teamMember.getFullName() + " can contribute excess funds of " + FundUtils.fmt(excess) + " to the team.");
                moveFromShared(teamMember, new FundAdjustment(TO_TEAMMATE_REASON, excess.negate()));
            }
        }

        if (totalAmountSharedByMembers.signum() == 0)
        {
            // there is no money to borrow from other riders
            FundUtils.log("No members have gone beyond their goal.");
        }
        else
        {
            FundUtils.log(memberCount + " members can contribute " + FundUtils.fmt(totalAmountSharedByMembers) + " back to the peloton.");
            FundUtils.log("Shareable peloton funds now " + FundUtils.fmt(shareableFunds) + ".");
        }
    }

    /**
     * Compare TeamMembers by the amount of a particular adjustment
     */
    private class TeamMemberAdjustmentComparator implements Comparator<TeamMember>
    {
        /** the adjustment reason to sort on */
        private final String reasonForCompare;

        /**
         * Constructor.
         *
         * @param reason the adjustment reason to sort on
         */
        public TeamMemberAdjustmentComparator(String reason)
        {
            reasonForCompare = reason;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(TeamMember member1, TeamMember member2)
        {
            BigDecimal adjustmentAmount1 = member1.findAdjustmentAmount(reasonForCompare);
            BigDecimal adjustmentAmount2 = member2.findAdjustmentAmount(reasonForCompare);
            return adjustmentAmount1.compareTo(adjustmentAmount2);
        }
    }

    /**
     * If there are team funds that were borrowed from riders but were not necessary for
     * everyone to reach there goal, then return the unused funds to those least over their goal.
     */
    private void returnUnusedPelotonShareableToSharers()
    {
        FundUtils.log("Not using " + FundUtils.fmt(shareableFunds) + " from riders who can share.");

        List<TeamMember> sharersSortedByAmountShared = findSharers();
        while (! sharersSortedByAmountShared.isEmpty() && shareableFunds.signum() > 0)
        {
            BigDecimal perMember = shareableFunds.divide(new BigDecimal(sharersSortedByAmountShared.size()), BigDecimal.ROUND_DOWN);

            TeamMember leastSharer = sharersSortedByAmountShared.get(sharersSortedByAmountShared.size() - 1);
            BigDecimal leastSharedAmount = leastSharer.findAdjustmentAmount(TO_TEAMMATE_REASON).negate();

            perMember = perMember.min(leastSharedAmount);
            if (perMember.signum() == 0)
            {
                break;
            }

            moveSharedToMembers(TO_TEAMMATE_REASON, perMember, sharersSortedByAmountShared);

            // remove those sharers who were given back all the money we borrowed
            while (leastSharer.findAdjustmentAmount(TO_TEAMMATE_REASON).signum() == 0)
            {
                sharersSortedByAmountShared.remove(sharersSortedByAmountShared.size()-1);

                leastSharer = sharersSortedByAmountShared.get(sharersSortedByAmountShared.size() - 1);
            }
        }

        // handle what's left over (which is less that $1 per sharer)
        while (shareableFunds.signum() > 0)
        {
            TeamMember leastSharer = sharersSortedByAmountShared.remove(sharersSortedByAmountShared.size() - 1);
            moveFromShared(leastSharer, new FundAdjustment(TO_TEAMMATE_REASON, shareableFunds.min(BigDecimal.ONE)));
        }
    }

    /**
     * Assign givers and takers to each specific fund swap by changing the generic
     * {@link #FROM_TEAMMATE_REASON} and {@link #TO_TEAMMATE_REASON} reasons to specific members.
     */
    private void assignSharersToReceivers()
    {
        List<TeamMember> sharers = findSharers();
        if (sharers.isEmpty())
        {
            return;
        }

        TeamMember sharingMember = sharers.remove(0);

        for (TeamMember needingMember : teamMemberList)
        {
            // see if this teamMember was promised money from a teammate
            BigDecimal fromTeammate;
            while ((fromTeammate = needingMember.findAdjustmentAmount(FROM_TEAMMATE_REASON)).signum() > 0)
            {
                // find money from the current sharer
                BigDecimal sharingAvailAmount = sharingMember.findAdjustmentAmount(TO_TEAMMATE_REASON).negate();
                if (sharingAvailAmount.signum() == 0)
                {
                    // sharingMember has run out of money, go to next sharer and try again
                    sharingMember = sharers.remove(0);
                }
                else
                {
                    // transfer either the what the needs is or only whatever the sharer has
                    fromTeammate = fromTeammate.min(sharingAvailAmount);

                    // Change generic "To teammate" to "To Rider Orphan Annie"
                    changeAdjustmentReason(sharingMember, TO_TEAMMATE_REASON, "To " + needingMember.getFullName(), fromTeammate.negate());

                    // Change generic "From teammate" to "From Rider Daddy Warbucks"
                    changeAdjustmentReason(needingMember, FROM_TEAMMATE_REASON, "From " + sharingMember.getFullName(), fromTeammate);
                }
            }
        }
    }

    /**
     * Rename the reason for some funds. This does not change the amount of money for the TeamMember.
     *
     * @param teamMember the team member with adjustments
     * @param fromReason the old adjustment reason
     * @param toReason the new adjustment reason
     * @param amount the amount to move
     */
    private void changeAdjustmentReason(TeamMember teamMember, String fromReason, String toReason, BigDecimal amount)
    {
        teamMember.addAdjustment(new FundAdjustment(fromReason, amount.negate()));
        teamMember.addAdjustment(new FundAdjustment(toReason, amount));
    }

    /**
     * Return the list of people who have shared to their team mates.
     *
     * @return list of people who have shared to their team mates in most-shared order.
     */
    private List<TeamMember> findSharers()
    {
        List<TeamMember> sharers = new LinkedList<>();
        for (TeamMember teamMember : teamMemberList)
        {
            BigDecimal sharedWithTeam = teamMember.findAdjustmentAmount(TO_TEAMMATE_REASON).negate();
            if (sharedWithTeam.signum() > 0)
            {
                sharers.add(teamMember);
            }
        }

        Collections.sort(sharers, new TeamMemberAdjustmentComparator(TO_TEAMMATE_REASON));

        return sharers;
    }

    /**
     * Give an equal amount of money to a group of riders
     *
     * @param sharedReason the reason for sharing
     * @param perMember the amount to give to each rider
     * @param membersForFunds the riders receiving funds
     */
    private void moveSharedToMembers(String sharedReason, BigDecimal perMember, List<TeamMember> membersForFunds)
    {
        for (TeamMember teamMember : membersForFunds)
        {
            moveFromShared(teamMember, new FundAdjustment(sharedReason, perMember));
        }
    }

    /**
     * Move money from the shared team account to a team member.
     *
     * @param teamMember the one receiving the funds
     * @param adjustment to move
     */
    public void moveFromShared(TeamMember teamMember, FundAdjustment adjustment)
    {
        shareableFunds = shareableFunds.subtract(adjustment.getAmount());
        teamMember.addAdjustment(adjustment);
    }
}
