pelotonia-funds
===============

This Java tool is used to calculate equitable fund sharing for a Pelotonia fund raising team.
The input to the program is an XLSX spreadsheet pasted from the Peloton Member Information page.
The output of the program is a report of what funds should be added or removed from each member's account.

The command line usage to the program is: spreadsheet.xlsx [full.class.name.of.custom.matching.algorithm]

It has a mechanism for companies to specify an algorithm for matching funds.
The Java implementation of that custom matching algorithm is the second (optional) argument.
By default it does not calculate any rider matching.

The program will take shared peloton funds and calculate how much to give to each rider to maximize
the number of riders meeting their goal. It will also use funds, if necessary, from riders who have
exceeded their goal.

The current version of the program does not handle riders who are High Rollers.
Those riders have to be removed from the spreadsheet before feeding it to this program.
