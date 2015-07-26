/**
 * 
 */
package com.huffmancoding.pelotonia.funds;

import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;

/**
 * This is the default fund matcher that doesn't give money to anyone.
 * Should be used when there is no external source of matching funds.
 *
 * @author khuffman
 */
public class NonExistentCompanyMatcher extends CompanyMatcher
{
    /**
     * Constructor.
     *
     * @param properties the properties necessary for configuring the match algorithm
     * @throws Exception in case of error
     */
    public NonExistentCompanyMatcher(Properties properties) throws Exception
    {
        super(properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FundAdjustment getMatchingForTeamMember(TeamMember teamMember)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal getMatchPaid(TeamMember teamMember)
    {
        return BigDecimal.ZERO;
    }

    /**
     * {@inheritDoc}
     *
     * Overridden to add logging
     */
    @Override
    public void addFundsToTeamMembers(List<TeamMember> teamMemberList)
    {
        super.addFundsToTeamMembers(teamMemberList);

        FundUtils.log("The program was configured to not calculate any matching funds per team member.");
    }
}
