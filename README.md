pelotonia-funds
===============

This Java tool is used to calculate equitable fund sharing for a
Pelotonia fund raising team.

PROGRAM ARGUMENTS
-----------------
The command line arguments to the program are:

    memberspreadsheet.xlsx [full.class.name.of.custom.matching.algorithm]

PROGRAM INPUT
-------------
The input to the program is an XLSX spreadsheet pasted from the
"Peloton Member Information" page. The output of the program is a report
of what funds should be added or removed from each member's account.

Since the spreadsheet from the Peloton Member Information page does not
reflect the peloton donations, the program will look at the input spreadsheet
for three additional column after of the rider section for peloton funds:

"Fund Type" | "Fund Source" | "Amount"
--------- | ----------- | ------
"Additional" | Bake Sale | 1000.00
"Additional" | Sponsor X Donation | 800.00

Those specific headers (e.g. Fund Type", "Fund Source", and "Amount")
must match exactly in the spreadsheet as well as "Additional" on each
of Peloton donation rows.

MATCHING CALCULATION
--------------------
If a team's matching funds are NOT already accounted for in each member's
account or the peloton account, there is a mechanism for this program to
have a Java class compute matching amounts before attempting to share funds.
The name of the custom matching algorithm Java class is the second
(optional) argument.

It requires you to write code unique to your company fund matching algorithm.
Without the extra argument, the program does not calculate any rider matching.

If a company's matching funds are already accounted for in each member's
account or the peloton account, then this argument should NOT be supplied
because the Pelotonia spreadsheet will already reflect the amounts.

PROGRAM OUTPUT
--------------
The program will take shared peloton funds and calculate how much to equitably
give to each rider to maximize the number of riders meeting their goal. It
will also use funds, if necessary, from riders who have exceeded their goal.
Per Pelotonia rules, it will not allow funds to be given to High Roller who
have not me their individual goal. 
