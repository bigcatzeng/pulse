package com.trxs.pulse.jdbc;

public class SnakeToCamelParameterUtil
{
    /**
     * Convert a column name with underscores to the corresponding property name using "camel case".
     * A name like "customer_number" would match a "customerNumber" property name.
     * @param name the column name to be converted
     * @return the name using "camel case"
     */
    public static String snakeToCamel(String name)
    {
        boolean nextIsUpper = false;
        StringBuilder result = new StringBuilder();

        if (name != null && name.length() > 0)
        {
            if (name.length() > 1 && name.charAt(1) == '_')
            {
                result.append(Character.toUpperCase(name.charAt(0)));
            }
            else
            {
                result.append(Character.toLowerCase(name.charAt(0)));
            }
            for (int i = 1; i < name.length(); i++)
            {
                char c = name.charAt(i);
                if (c == '_')
                {
                    nextIsUpper = true;
                }
                else
                {
                    if (nextIsUpper)
                    {
                        result.append(Character.toUpperCase(c));
                        nextIsUpper = false;
                    }
                    else
                    {
                        result.append(Character.toLowerCase(c));
                    }
                }
            }
        }

        return result.toString();
    }

    public static String camelToSnake(String name)
    {
        StringBuilder result = new StringBuilder();

        if (name != null && name.length() > 0)
        {
            result.append(Character.toLowerCase(name.charAt(0)));
            for (int i = 1; i < name.length(); i++)
            {
                char c = name.charAt(i);
                if ( Character.isUpperCase(c) )
                    result.append('_').append(Character.toLowerCase(c));
                else
                    result.append(c);
            }
        }
        return result.toString();
    }
}
