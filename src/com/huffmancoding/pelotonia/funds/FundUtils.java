package com.huffmancoding.pelotonia.funds;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * Utilities that are used by multiple classes.
 *
 * @author khuffman
 */
public class FundUtils
{
    /** How number should appear. */
    private static final DecimalFormat FORMAT = new DecimalFormat("$#,##0.00");

    /**
     * Convert an amount to a pretty currency string.
     *
     * @param amount the amount
     * @return a string with currency punctuation
     */
    public static String fmt(BigDecimal amount)
    {
        // program not multi-threaded, but synchronizing is harmless 
        synchronized (FORMAT)
        {
            return FORMAT.format(amount);
        }
    }

    public static void log(String message)
    {
        System.out.println(message);
    }
}
