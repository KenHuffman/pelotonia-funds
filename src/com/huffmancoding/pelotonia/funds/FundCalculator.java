package com.huffmancoding.pelotonia.funds;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
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
    private CompanyMatcher matcher = new NonExistentCompanyMatcher();

    /**
     * Run the program.
     *
     * @param args the command line arguments
     * @throws Exception in case of error
     */
    private void doFunding(String[] args) throws Exception
    {
        if (args.length < 0)
        {
            throw new IOException("Usage: java <jar> rosterfile.xlsx [com.huffmancoding.pelotonia.funds.BigLots2014Matcher]");
        }

        String rosterFileName = args[0]; 
        if (args.length > 1)
        {
            String matcherClass = args[1];
            matcher = (CompanyMatcher)Class.forName(matcherClass).getConstructor().newInstance();
        }

        loadRosterFile(rosterFileName);
        addMatchingFundsToTeamMembers();
        reportOfShortHighRollers();
        allocateSharableToTeamMembers();
        reportOfTeamMembers();
    }

    /**
     * Parse the spreadsheet of team members.
     *
     * @param rosterFileName XLSX file name in {@link SpreadsheetParser} format
     * @throws IOException in case the problem reading the file
     * @throws InvalidFormatException in case of syntax or semantic xlsx format errors
     */
    private void loadRosterFile(String rosterFileName) throws InvalidFormatException, IOException
    {
        TeamMemberFactory teamMemberFactory = new TeamMemberFactory(matcher.getAdditionalColumns());

        File rosterFile = new File(rosterFileName);
        Date dateOfFile = new Date(rosterFile.lastModified());
        FundUtils.log("Loading spreadsheet " + rosterFile.getPath() + " dated " + dateOfFile + ".");

        TeamMemberSpreadsheetParser teamMemberParser = new TeamMemberSpreadsheetParser(rosterFile, teamMemberFactory);
        teamMemberParser.loadSpreadsheet();
        teamMemberList = teamMemberParser.getTeamMembers();

        SharableFundsSpreadsheetParser sharableFundsParser = new SharableFundsSpreadsheetParser(rosterFile);
        sharableFundsParser.loadSpreadsheet();
        shareableFunds = sharableFundsParser.getSharableFunds();
    }

    /**
     * Add money from the company to team members who have reached their fundraising criteria.
     */
    private void addMatchingFundsToTeamMembers()
    {
        matcher.addFundsToTeamMembers(teamMemberList);
    }

    /**
     * Report on high rollers needing to get to
     */
    private void reportOfShortHighRollers()
    {
        int highRollersShortOfCommitment = 0;
        BigDecimal highRollersRemainingShortfall = BigDecimal.ZERO;

        for (TeamMember teamMember : teamMemberList)
        {
            BigDecimal shortfall = teamMember.getShortfall();
            if (shortfall.signum() > 0 && teamMember.isHighRoller())
            {
                ++highRollersShortOfCommitment;
                highRollersRemainingShortfall = highRollersRemainingShortfall.add(shortfall);
            }
        }

        FundUtils.log(highRollersShortOfCommitment + " high rollers need " + FundUtils.fmt(highRollersRemainingShortfall) + " to reach their goal ON THEIR OWN.");
    }

    /**
     * Disburse money to team members in rounds, to maximize the number of riders to commitment.
     */
    private void allocateSharableToTeamMembers()
    {
        FundSharer sharer = new FundSharer(teamMemberList);
        shareableFunds = sharer.allocateSharableToNonHighRollers(shareableFunds);
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
            if (teamMember.isRider() || designatedFunds.signum() > 0)
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
        FundUtils.log("The team committed to raise " + FundUtils.fmt(totalCommitment) + ".");
        FundUtils.log("The team has raised " + FundUtils.fmt(totalRaised) + ".");

        BigDecimal overage = totalRaised.subtract(totalCommitment);
        if (overage.signum() < 0)
        {
            FundUtils.log("The team needs to raise " + FundUtils.fmt(overage.negate()) + ".");
        }
        else
        {
            FundUtils.log("The team has exceeded their goal by " + FundUtils.fmt(overage) + ".");
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
            calculator.doFunding(args);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
