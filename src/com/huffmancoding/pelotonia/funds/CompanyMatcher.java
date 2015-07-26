package com.huffmancoding.pelotonia.funds;

import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;

/**
 * The interface for a company to match the fundraising raised individually.
 *
 * @author khuffman
 */
public abstract class CompanyMatcher
{
    /** the properties files for the application, in case the Matcher needs configuration parameters. */
    private final Properties properties;

    /**
     * Constructor.
     *
     * @param properties the properties necessary for configuring the match algorithm
     * @throws Exception in case of error
     */
    public CompanyMatcher(Properties properties) throws Exception
    {
        this.properties = properties;
    }

    /**
     * Get the app properties.
     *
     * @return the properties of the app
     */
    public Properties getProperties()
    {
        return properties;
    }

    /**
     * Add money from the company to team members who have reached some fundraising criteria.
     *
     * @param teamMemberList list of team members to add funds to
     */
    public void addFundsToTeamMembers(List<TeamMember> teamMemberList)
    {
        for (TeamMember teamMember : teamMemberList)
        {
            if (teamMember.isHighRoller() && teamMember.getAmountRaised().compareTo(teamMember.getCommitment()) < 0)
            {
                FundUtils.log(teamMember.getFullName() + " cannot receive matching funds until individual " +
                        FundUtils.fmt(teamMember.getCommitment()) + " high roller goal is met.");
            }
            else
            {
                FundAdjustment matching = getMatchingForTeamMember(teamMember);
                if (matching != null)
                {
                    teamMember.addAdjustment(matching);
                }
            }
        }
    }

    /**
     * Return the matching amount a team member should receive from the company.
     *
     * @param teamMember the member to check
     * @return the matching amount, or null if there's is no matching for this team member
     */
    public abstract FundAdjustment getMatchingForTeamMember(TeamMember teamMember);

    /**
     * Return the match already deposited on the teamMember's account.
     *
     * @param teamMember the team member to check
     * @return the match amount already deposited
     */
    public abstract BigDecimal getMatchPaid(TeamMember teamMember);
}