package com.huffmancoding.pelotonia.funds;

import java.util.List;
import java.util.Properties;

/**
 * The interface for a company to match the fundraising raised individually.
 *
 * @author khuffman
 */
public abstract class CompanyMatcher
{
    final Properties properties;
    
    /**
     * @param properties the properties necessary for configuring the match algorithm
     */
    public CompanyMatcher(Properties properties) throws Exception
    {
        this.properties = properties;
    }

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
}