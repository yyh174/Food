package com.xl.can.utils;

/**
 * Token 计算工具类
 * 用于估算文本的 token 数量
 *
 * 中文文本：约 1 token ≈ 1.5-2 个字符
 * 英文文本：约 1 token ≈ 4 个字符
 * 代码文本：约 1 token ≈ 4 个字符
 */
public class TokenUtils {

    private static final double CHINESE_CHARS_PER_TOKEN = 1.5;
    private static final double ENGLISH_CHARS_PER_TOKEN = 4.0;
    private static final double CODE_CHARS_PER_TOKEN = 3.5;

    /**
     * 估算文本的 token 数量
     * 使用简化的启发式方法
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int totalChars = text.length();
        int chineseChars = countChineseChars(text);
        int englishChars = countEnglishChars(text);
        int codeChars = countCodeChars(text);
        int otherChars = totalChars - chineseChars - englishChars - codeChars;

        // 分别计算不同类型字符的 token
        double tokens = 0;
        tokens += chineseChars / CHINESE_CHARS_PER_TOKEN;
        tokens += englishChars / ENGLISH_CHARS_PER_TOKEN;
        tokens += codeChars / CODE_CHARS_PER_TOKEN;
        tokens += otherChars / ENGLISH_CHARS_PER_TOKEN;

        return (int) Math.ceil(tokens);
    }

    /**
     * 计算中文字符数量
     */
    private static int countChineseChars(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (isChinese(c)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 计算英文字符数量
     */
    private static int countEnglishChars(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                count++;
            }
        }
        return count;
    }

    /**
     * 计算代码相关字符数量（符号、数字等）
     */
    private static int countCodeChars(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if ((c >= '0' && c <= '9') || isCodeSymbol(c)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 判断是否为中文字符
     */
    private static boolean isChinese(char c) {
        return (c >= '\u4e00' && c <= '\u9fff') ||
               (c >= '\u3400' && c <= '\u4dbf') ||
               (c >= '\uf900' && c <= '\ufaff');
    }

    /**
     * 判断是否为代码常用符号
     */
    private static boolean isCodeSymbol(char c) {
        return c == '{' || c == '}' || c == '(' || c == ')' || c == '[' || c == ']' ||
               c == '<' || c == '>' || c == '=' || c == '+' || c == '-' || c == '*' ||
               c == '/' || c == '\\' || c == '|' || c == '&' || c == '^' || c == '%' ||
               c == '#' || c == '@' || c == '!' || c == '?' || c == ':' || c == ';' ||
               c == ',' || c == '.' || c == '_' || c == '"' || c == '\'' || c == '`' ||
               c == '~' || c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    /**
     * 判断字符是否为空白字符
     */
    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }
}
