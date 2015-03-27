pelotonia-funds
===============

This Java tool is used to calculate equitable fund sharing for a
Pelotonia fund raising team.

PROGRAM ARGUMENTS
-----------------
The command line arguments to the program are:

    properties.txt

PROGRAM INPUT
-------------
The input to the program is a properties file that contains parameters on
how to run the program.

At a minimum the file should contain the line:

    pelotonia_spreadsheet=https://www.mypelotonia.org/files/300889roster.xls

The value points to a spreadsheet that Pelotonia provides for you.
Log onto you account and click on the "Current Peloton Member Information" link.
That will bring up a simple web page and at the top of the page will be a
"Click here to download this list as an Excel file" Use THAT link, which ends
in .xls as the property above.

You can also download that spreadsheet to a file and specify the downloaded
file as input

    pelotonia_spreadsheet=file:///path_to_member_file.xls

PROGRAM OUTPUT
--------------
The program will take shared peloton funds and calculate how much to equitably
give to each rider to maximize the number of riders meeting their goal. It
will also use funds, if necessary, from riders who have exceeded their goal.
Per Pelotonia rules, it will not allow funds to be given to High Roller who
have not me their individual goal. 

The output of the program is a report of what funds should be added or removed
from each member's account.

PELOTON FUNDS
-------------
If a team's matching funds are NOT already accounted for in each member's account,
there is a mechanism for this program to add those amounts before attempting to
share funds.

Since the spreadsheet from the Peloton Member Information page does not
reflect the peloton donations, you can create another spreadsheet with team funds.
The spreadsheet should have three columns:

"Fund Type" | "Fund Source" | "Amount"
--------- | ----------- | ------
"Additional" | Peloton Funds | 1234.00
"Additional" | Bake Sale | 1000.00
"Additional" | Sponsor X Donation | 800.00

Those specific headers (e.g. "Fund Type", "Fund Source", and "Amount")
must match exactly in the spreadsheet as well as "Additional" on each
of Peloton donation rows.

If you create this spreadsheet, add another line (or two) to the properties.txt file

    teamfunds_spreadsheet=file:///path_to_peloton_file.xlsx
    teamfunds_sheetname=Sheet1

The second line is only necessary if the spreadsheet has more that one tab
and you need to specify the name of the tab that it is on.

MATCHING CALCULATION
--------------------
If a team matched funds and those funds are NOT already accounted for in either
of the above spreadsheets, there is a mechanism for this program to
compute matching amounts before attempting to share funds.

The name of the custom matching algorithm Java class is another parameter.

    matcher_class=com.huffmancoding.pelotonia.funds.LevelMatcher

This class parameter, if specified, requires more parameters:

    matcher_amount_volunteer=0,25
    matcher_amount_1200=400,300
    matcher_amount_1250=415,300
    matcher_amount_1800=600,400
    matcher_amount_2200=730,450

The numbers left of the equal sign indicate the rider's commitment,
and the number pair after the sign indicate the amount he has to raise
and the amount he will receive from the company as a match.

Without the matcher_class parameter, the program does not calculate any rider matching.

The LevelMatcher algorithm has an optional parameter to determine which riders
are eligible for a match based on other information. If you create another spreadsheet
(or another tab on the one of the spreadsheets above) that contains this 
information it will only choose to give matching funds to Riders that are listed
as "Employee". Anything else will skip

"Rider ID" | "Employee"
--------- | --------
"JS0123" | Employee
"KW0099" | NonEmployee

Those specific headers (e.g. "Rider ID" and "Employee") must exist if you create this
spreadsheet and you must add another line (or two) to the properties.txt file

    matcher_spreadsheet=file:///path_to_employee_file.xlsx
    matcher_sheetname=Sheet1

Again, the second line is only necessary if the spreadsheet has more that one tab
and you need to specify the name of the tab that it is on.

If a company's matching funds are already accounted for in each member's
account or the peloton account, then this parameter should NOT be supplied
because the other spreadsheets will already reflect the amounts.