package com.huffmancoding.pelotonia.funds;

import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;

public interface TeamMemberFactory
{
    public List<SpreadsheetColumn> getTeamMemberColumns();

    public TeamMember createTeamMember(Row row) throws InvalidFormatException;
}
