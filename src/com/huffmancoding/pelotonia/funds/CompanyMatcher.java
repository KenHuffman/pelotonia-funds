package com.huffmancoding.pelotonia.funds;

import java.util.Collections;
import java.util.List;

/**
 * The interface for a company to match the fundraising raised individually.
 *
 * @author khuffman
 */
public abstract class CompanyMatcher
{
    /**
     * Add money from the company to team members who have reached their fundraising criteria.
     *
     * @param teamMemberList list of team members to add funds to
     */
    public void addFundsToTeamMembers(List<TeamMember> teamMemberList)
    {
        for (TeamMember teamMember : teamMemberList)
        {
            FundAdjustment matching = getMatchingForTeamMember(teamMember);
            if (matching != null)
            {
                teamMember.addAdjustment(matching);
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
     * This default company doesn't need any additional information.
     *
     * @return an empty list by default
     */
    public List<SpreadsheetColumn> getAdditionalColumns()
    {
        return Collections.emptyList();
    }
}