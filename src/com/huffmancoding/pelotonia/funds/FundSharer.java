package com.huffmancoding.pelotonia.funds;

import java.math.BigDecimal;
import java.util.ArrayList;
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

        List<TeamMember> membersWithShortfall;
        for (int round = 1; ! (membersWithShortfall = findNonHighRollersShortOfCommitment()).isEmpty(); ++round)
        {
            BigDecimal perMember = calculateFundingRoundAmount(membersWithShortfall, usedExcessFromMembers);
            if (perMember.signum() == 0)
            {
                // we have so little shared funds, that we cannot even give a buck to each rider
                break;
            }

            FundUtils.log("Funding round " + round + " has sharable team funds of " + FundUtils.fmt(shareableFunds) +
                    ", giving " + FundUtils.fmt(perMember) + " to " + membersWithShortfall.size() + " underfunded rider(s).");

            moveFromPelotonToMembers(perMember, membersWithShortfall);
        }

        if (membersWithShortfall.isEmpty())
        {
            // every reached goals
            FundUtils.log("Every rider has now reached their goal, with leftover sharable funds of " + FundUtils.fmt(shareableFunds) + ".");

            if (!usedExcessFromMembers.get())
            {
                FundUtils.log("Did not have to shift funds from members who exceeded their goal.");
            }
        }
        else
        {
            FundUtils.log("Shareable funds are now exhausted. " + membersWithShortfall.size() + " riders have not reached their goal.");
        }

        return shareableFunds;
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

        FundUtils.log(ridersShortOfCommitment + " (non-high roller) riders need " + FundUtils.fmt(totalRemainingShortfall) + " to reach their goal.");

        return membersWithShortfall;
    }

    /**
     * Get the funding amount for the round, using money if available and necessary from other members.
     *
     * @param membersWithShortfall riders needing money
     * @param usedExcessFromMembers whether we have already borrowed money from members beyond their commitment
     * @return the amount to give this round to members needing money
     */
    private BigDecimal calculateFundingRoundAmount(List<TeamMember> membersWithShortfall, AtomicBoolean usedExcessFromMembers)
    {
        TeamMember closestToCommitment = findMemberClosestToCommitment(membersWithShortfall);

        BigDecimal perMember = pickSmallestShortfallOrEvenSplit(closestToCommitment, membersWithShortfall);
        if (perMember.signum() == 0)
        {
            // we have no money to share, have we used money from other riders?
            if (! usedExcessFromMembers.getAndSet(true))
            {
                FundUtils.log("Initial shared funds are exhausted, now checking for members have gone beyond their goal for fund sharing.");
                addBeyondCommitmentToSharable();

                // try again to see if we have money to give, now that we have used money from other riders
                perMember = pickSmallestShortfallOrEvenSplit(closestToCommitment, membersWithShortfall);
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
     * @param membersWithShortfall the riders needing more money
     * @return the smallest rider shortfall or possibly BigDecimal.ZERO
     */
    private BigDecimal pickSmallestShortfallOrEvenSplit(TeamMember closestToCommitment, List<TeamMember> membersWithShortfall)
    {
        assert ! membersWithShortfall.isEmpty();
        BigDecimal evenSplit = shareableFunds.divide(new BigDecimal(membersWithShortfall.size()), BigDecimal.ROUND_DOWN);

        return closestToCommitment.getShortfall().min(evenSplit);
    }

    /**
     * Give an equal amount of money to all rider needing money. No rider will be given more than he needs to reach their commitment.
     * 
     * @param perMember the amount to give to each rider
     * @param membersWithShortfall the riders needing more money
     */
    private void moveFromPelotonToMembers(BigDecimal perMember, List<TeamMember> membersWithShortfall)
    {
        for (TeamMember teamMember : membersWithShortfall)
        {
            moveFromPeloton(teamMember, new FundAdjustment("Funds from peloton", perMember));
        }
    }

    /**
     * Add to the sharable funds moneys from non-riders and riders who have exceeded their individual commitment.
     * This function can be called before {@link #allocateSharableToTeamMembers()} if the initial shared funds
     * are not sufficient for everyone on the team to reach their commitment.
     *
     * @return the amount borrowed from over-achieving riders
     */
    private BigDecimal addBeyondCommitmentToSharable()
    {
        int memberCount = 0;

        BigDecimal totalAmountSharedByMembers = BigDecimal.ZERO;
        for (TeamMember teamMember : teamMemberList)
        {
            BigDecimal excess = teamMember.getShortfall().negate();
            String excessCalculation = FundUtils.fmt(teamMember.getAmountRaised().add(teamMember.getAdjustmentTotal())) +
                    " - " + FundUtils.fmt(teamMember.getCommitment());

            if (excess.signum() > 0)
            {
                ++memberCount;

                totalAmountSharedByMembers = totalAmountSharedByMembers.add(excess);

                FundUtils.log(teamMember.getFullName() + " contributing excess funds of " + FundUtils.fmt(excess) +
                        " (" + excessCalculation + ") to the team.");
                moveFromPeloton(teamMember, new FundAdjustment("Shared with team", excess.negate()));
            }
        }

        if (totalAmountSharedByMembers.signum() == 0)
        {
            // there is no money to borrow from other riders
            FundUtils.log("No members have gone beyond their goal.");
        }
        else
        {
            FundUtils.log(memberCount + " members have contributed " + FundUtils.fmt(totalAmountSharedByMembers) + " back to the peloton.");
            FundUtils.log("Shareable peloton funds now " + FundUtils.fmt(shareableFunds) + ".");
        }

        return totalAmountSharedByMembers;
    }

    /**
     * Move money from the shared team account to a team member.
     *
     * @param teamMember the one receiving the funds
     * @param adjustment to move
     */
    public void moveFromPeloton(TeamMember teamMember, FundAdjustment adjustment)
    {
        shareableFunds = shareableFunds.subtract(adjustment.getAmount());
        teamMember.addAdjustment(adjustment);
    }
}
