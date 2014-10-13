package com.huffmancoding.pelotonia.funds;

import java.util.Collections;
import java.util.List;

/**
 * This matcher will only calculate FundAdjustments for TeamMembers that are
 * marked as an Employee in the spreadsheet.
 *
 * @author khuffman
 */
public abstract class EmployeeOnlyMatcher extends CompanyMatcher
{
    /** header title for the column that contains team member Employee value: "Employee" or "Family". */
    public static final SpreadsheetColumn EMPLOYEE_COLUMN = new SpreadsheetColumn("Employee", true);

    /**
     * {@inheritDoc}
     * 
     * This matcher needs to know if the team member is an employee
     *
     * @return the extra column this matcher cares about
     */
    @Override
    public List<SpreadsheetColumn> getAdditionalColumns()
    {
        return Collections.singletonList(EMPLOYEE_COLUMN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FundAdjustment getMatchingForTeamMember(TeamMember teamMember)
    {
        if (isEmployee(teamMember))
        {
            return getMatchingForEmployee(teamMember);
        }
        else
        {
            FundUtils.log(teamMember.getFullName() + " is not an employee eligible for matching funds.");
            return null;
        }
    }

    /**
     * Whether the team member qualifies for match.
     *
     * @return true if member qualifies for match, false otherwise
     */
    public boolean isEmployee(TeamMember teamMember)
    {
        String columnValue = teamMember.getAdditionalProperties().getProperty(EMPLOYEE_COLUMN.getName());
        return columnValue == null || columnValue.equalsIgnoreCase("employee");
    }

    /**
     * Return the matching amount an employee should receive from the company.
     * 
     * @param teamMember the member to check
     * @return either a rider or volunteer match
     */
    public abstract FundAdjustment getMatchingForEmployee(TeamMember teamMember);
}