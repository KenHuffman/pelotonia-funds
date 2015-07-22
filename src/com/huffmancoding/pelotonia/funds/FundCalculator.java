package com.huffmancoding.pelotonia.funds;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

/**
 * This code calculates the best way to share funds to maximize the number of riders reaching their individual goal.
 *
 * @author khuffman
 */
public class FundCalculator
{
    /** properties for the program. */
    private final Properties properties = new Properties();

    /** the list of team members on the company's team, includes volunteers and non-employees */
    private List<TeamMember> teamMemberList;

    /** the amount of money that can be shared with individual riders if necessary. */
    private BigDecimal shareableFunds;

    /** the algorithm for determining company match. */
    private CompanyMatcher matcher;

    /**
     * Run the program.
     *
     * @param args the command line arguments
     * @throws Exception in case of error
     */
    private void doFunding(String[] args) throws Exception
    {
        if (args.length < 1)
        {
            throw new IOException("Usage: java <jar> properties.txt");
        }

        String propertiesFile = args[0];
        try (FileReader reader = new FileReader(propertiesFile))
        {
            properties.load(reader);

            String matcherClassName = properties.getProperty("matcher_class");
            if (matcherClassName == null)
            {
                matcherClassName = "com.huffmancoding.pelotonia.funds.NonExistentCompanyMatcher";
            }
            @SuppressWarnings("unchecked")
            Class<? extends CompanyMatcher> matcherClass = (Class<? extends CompanyMatcher>) Class.forName(matcherClassName);
            Constructor<? extends CompanyMatcher> constructor = matcherClass.getConstructor(Properties.class);
            matcher = constructor.newInstance(properties);

            String rosterURLSpec = properties.getProperty("pelotonia_spreadsheet");
            if (rosterURLSpec == null)
            {
                throw new Exception("Properties file " + propertiesFile + " must contain value for pelotonia_spreadsheet.");
            }
            loadRosterURL(rosterURLSpec);

            String fundsURLSpec = properties.getProperty("teamfunds_spreadsheet");
            if (fundsURLSpec != null)
            {
                String sheetName = properties.getProperty("teamfunds_sheetname");
                loadFundsURL(fundsURLSpec, sheetName);
            }

            addMatchingFundsToTeamMembers();
            reportOfShortHighRollers();
            allocateSharableToTeamMembers();
            reportOfTeamMembers();
        }
    }

    /**
     * Parse the spreadsheet of team members.
     *
     * @param rosterURL XLSX file name in {@link SpreadsheetParser} format
     * @throws IOException in case the problem reading the file
     * @throws InvalidFormatException in case of syntax or semantic xlsx format errors
     */
    private void loadRosterURL(String rosterURL) throws InvalidFormatException, IOException
    {
        TeamMemberSpreadsheetParser teamMemberParser = new TeamMemberSpreadsheetParser(new URL(rosterURL));
        teamMemberParser.loadPelotoniaSpreadsheet();
        teamMemberList = teamMemberParser.getTeamMembers();
    }

    /**
     * Parse the spreadsheet of sharable funds.
     *
     * @param fundsURL XLSX file name in {@link SpreadsheetParser} format
     * @param sheetName the name of the sheet in the fundsURL
     * @throws IOException in case the problem reading the file
     * @throws InvalidFormatException in case of syntax or semantic xlsx format errors
     */
    private void loadFundsURL(String fundsURL, String sheetName) throws InvalidFormatException, IOException
    {
        SharableFundsSpreadsheetParser sharableFundsParser = new SharableFundsSpreadsheetParser(new URL(fundsURL), sheetName);
        sharableFundsParser.loadFundsSpreadsheet();

        // Add 0.00 to number to convert whole dollar decimal to one with penny precision
        shareableFunds = sharableFundsParser.getSharableFunds().add(new BigDecimal("0.00"));
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
            if (teamMember.isRider() || amountRaised.signum() != 0 || !teamMember.getAdjustments().isEmpty())
            {
                BigDecimal designatedFunds = amountRaised.add(teamMember.getAdjustmentTotal());
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
                    FundUtils.log(indent + adjustment.toString());
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
