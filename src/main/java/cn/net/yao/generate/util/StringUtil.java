package cn.net.yao.generate.util;

import org.apache.commons.lang3.StringUtils;
import java.util.Collection;
import java.util.Map;

public class StringUtil extends StringUtils {
    private static final String NULLSTR = "";
    private static final char SEPARATOR = '_';

    public static <T> T nvl(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static boolean isEmpty(Collection<?> coll) {
        return isNull(coll) || coll.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> coll) {
        return !isEmpty(coll);
    }

    public static boolean isEmpty(Object[] objects) {
        return isNull(objects) || (objects.length == 0);
    }

    public static boolean isNotEmpty(Object[] objects) {
        return !isEmpty(objects);
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return isNull(map) || map.isEmpty();
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    public static boolean isEmpty(String str) {
        return isNull(str) || NULLSTR.equals(str.trim());
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static boolean isNull(Object object) {
        return object == null;
    }

    public static boolean isNotNull(Object object) {
        return !isNull(object);
    }

    public static boolean isArray(Object object) {
        return isNotNull(object) && object.getClass().isArray();
    }

    public static String trim(String str) {
        return (str == null ? "" : str.trim());
    }

    public static String substring(final String str, int start) {
        if (str == null) return NULLSTR;
        if (start < 0) start = str.length() + start;
        if (start < 0) start = 0;
        if (start > str.length()) return NULLSTR;
        return str.substring(start);
    }

    public static String substring(final String str, int start, int end) {
        if (str == null) return NULLSTR;
        if (end < 0) end = str.length() + end;
        if (start < 0) start = str.length() + start;
        if (end > str.length()) end = str.length();
        if (start > end) return NULLSTR;
        if (start < 0) start = 0;
        if (end < 0) end = 0;
        return str.substring(start, end);
    }

    public static String format(String template, Object... params) {
        if (isEmpty(params) || isEmpty(template)) return template;
        return StrFormatter.format(template, params);
    }

    public static String toUnderScoreCase(String str) {
        if (str == null) return null;
        StringBuilder sb = new StringBuilder();
        boolean preCharIsUpperCase = true;
        boolean curreCharIsUpperCase;
        boolean nexteCharIsUpperCase = true;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (i > 0) preCharIsUpperCase = Character.isUpperCase(str.charAt(i - 1));
            else preCharIsUpperCase = false;
            curreCharIsUpperCase = Character.isUpperCase(c);
            if (i < (str.length() - 1)) nexteCharIsUpperCase = Character.isUpperCase(str.charAt(i + 1));
            if (preCharIsUpperCase && curreCharIsUpperCase && !nexteCharIsUpperCase) {
                sb.append(SEPARATOR);
            } else if ((i != 0 && !preCharIsUpperCase) && curreCharIsUpperCase) {
                sb.append(SEPARATOR);
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    public static boolean inStringIgnoreCase(String str, String... strs) {
        if (str != null && strs != null) {
            for (String s : strs) {
                if (str.equalsIgnoreCase(trim(s))) return true;
            }
        }
        return false;
    }

    public static String convertToCamelCase(String name) {
        if (name == null || name.isEmpty()) return "";
        if (!name.contains("_")) return name.substring(0, 1).toUpperCase() + name.substring(1);
        StringBuilder result = new StringBuilder();
        String[] camels = name.split("_");
        for (String camel : camels) {
            if (camel.isEmpty()) continue;
            result.append(camel.substring(0, 1).toUpperCase());
            result.append(camel.substring(1).toLowerCase());
        }
        return result.toString();
    }
}
