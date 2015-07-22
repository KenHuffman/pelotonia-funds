package com.huffmancoding.pelotonia.funds;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
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

    /**
     * Log a message to the output.
     *
     * @param message the message to log
     */
    public static void log(String message)
    {
        System.out.println(message);
    }

    /**
     * Convert String to URL handling the '~' -> user.home.
     *
     * @param url the URL from the properties file
     * @return the URL
     * @throws MalformedURLException if the url is invalid
     */
    public static URL propertyToURL(String url) throws MalformedURLException
    {
    	return new URL(url.replace("~", System.getProperty("user.home")));
    }
}