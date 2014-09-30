package com.huffmancoding.pelotonia.funds;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

/**
 * This code calculates the best way to share funds to maximize the number of riders reaching their individual goal.
 *
 * @author khuffman
 */
public class FundCalculator
{
    /** the list of team members on the company's team, includes volunteers and non-employees */
    private List<TeamMember> teamMemberList;

    /** the amount of money that can be shared with individual riders if necessary. */
    private BigDecimal shareableFunds = BigDecimal.ZERO;

    /** the algorithm for determining company match. */
    private CompanyMatcher matcher = new BigLots2014Matcher();

    /**
     * Constructor.
     *
     * @param rosterFileName XLSX file name in {@link SpreadsheetParser} format
     * @throws IOException in case the problem reading the file
     * @throws InvalidFormatException in case of syntax or semantic xlsx format errors
     */
    private void loadRosterFile(String rosterFileName) throws InvalidFormatException, IOException
    {
        File rosterFile = new File(rosterFileName);

        SpreadsheetParser parser = new SpreadsheetParser(rosterFile);
        parser.loadRosterFile();
        teamMemberList = parser.getTeamMembers();
        shareableFunds = parser.getSharableFunds();
    }

    /**
     * Add money from the company to team members who have reached their fundraising criteria.
     */
    private void addMatchingFundsToTeamMembers()
    {
        matcher.addFundsToTeamMembers(teamMemberList);
    }

    /**
     * Disburse money to team members in rounds, to maximize the number of riders to commitment.
     */
    private void allocateSharableToTeamMembers()
    {
        FundSharer sharer = new FundSharer(teamMemberList);
        shareableFunds = sharer.allocateSharableToTeamMembers(shareableFunds);
    }

    /**
     * Dump a report of the final situation of each rider and the team.
     */
    private void reportOfTeamMembers()
    {
        BigDecimal totalCommitment = BigDecimal.ZERO;
        BigDecimal totalRaised = shareableFunds;
        int ridersWithCommitment = 0, ridersMakingCommitment = 0;
        for (TeamMember teamMember : teamMemberList)
        {
            BigDecimal memberCommitment = teamMember.getCommitment();
            totalCommitment = totalCommitment.add(memberCommitment);

            BigDecimal amountRaised = teamMember.getAmountRaised();
            BigDecimal designatedFunds = amountRaised.add(teamMember.getAdjustmentTotal());
            if (designatedFunds.signum() > 0)
            {
                String reportLine = teamMember.getFullName() + " has " + FundUtils.fmt(designatedFunds);
                if (teamMember.isRider())
                {
                    reportLine += " of " + FundUtils.fmt(memberCommitment) + " goal";

                    ++ridersWithCommitment;
                    if (designatedFunds.compareTo(memberCommitment) >= 0)
                    {
                        ++ridersMakingCommitment;
                    }
                }
                FundUtils.log(reportLine + ".");

                String indent = "  ";
                if (amountRaised.signum() != 0)
                {
                    FundUtils.log(indent + "Amount raised: " + FundUtils.fmt(amountRaised));
                }

                for (FundAdjustment adjustment : teamMember.getAdjustments())
                {
                    FundUtils.log(indent + adjustment.getReason() + ": " + FundUtils.fmt(adjustment.getAmount()));
                }

                totalRaised = totalRaised.add(designatedFunds);
            }
        }

        FundUtils.log(ridersMakingCommitment + " of " + ridersWithCommitment + " riders have reached their goal.");
        FundUtils.log("Team BIG committed to raise " + FundUtils.fmt(totalCommitment) + ".");
        FundUtils.log("Team BIG has raised " + FundUtils.fmt(totalRaised) + ".");

        BigDecimal overage = totalRaised.subtract(totalCommitment);
        if (overage.signum() < 0)
        {
            FundUtils.log("Team BIG needs to raise " + FundUtils.fmt(overage.negate()) + ".");
        }
        else
        {
            FundUtils.log("Team BIG has exceeded our goal by " + FundUtils.fmt(overage) + ".");
        }
    }

    /**
     * Run the calculator.
     *
     * @param args command line arguments: <xlsx-file> [<shared-fund-name>=<shared-amount> ...]
     */
    public static void main(String[] args)
    {
        try
        {
            FundCalculator calculator = new FundCalculator();

            calculator.loadRosterFile(args[0]);
            calculator.addMatchingFundsToTeamMembers();
            calculator.allocateSharableToTeamMembers();
            calculator.reportOfTeamMembers();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
