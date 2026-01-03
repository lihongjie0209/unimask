package cn.lihongjie.unimask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字符映射工具类
 * 负责建立原始字符与索引以及加密后Unicode字符的映射关系
 * 
 * 映射规则：
 * - 常用区（ASCII + 标点 + 一二级字表）：映射到 BMP PUA (U+E000 - U+F8FF, 6400个位置)
 * - 罕见区（三级字表）：映射到罕用韩文音节区 (U+CF70开始, 2100个位置)
 * 
 * 字符集从资源文件中读取：
 * - level-1.txt: 通用规范汉字表一级字表（3500字，常用字）
 * - level-2.txt: 通用规范汉字表二级字表（3000字，次常用字）
 * - level-3.txt: 通用规范汉字表三级字表（1605字，较少使用）
 * 
 * 支持的字符类型：
 * - 数字：0-9 (10个)
 * - 英文字母：A-Z, a-z (52个)
 * - 标点符号：半角、全角、中文专用、通用标点 (约300个)
 * - 汉字：通用规范汉字表 8105 字
 * 
 * 总字符集大小：约 8500 个字符
 * 不在字符集中的字符将原样输出（不加密）
 */
public class CharacterMapping {
    
    private static final Logger logger = LoggerFactory.getLogger(CharacterMapping.class);
    
    /** BMP PUA 起始位置 */
    private static final int PUA_START = 0xE000;
    
    /** PUA 区容量 */
    private static final int PUA_CAPACITY = 6400;
    
    /** 罕用韩文音节区起始位置 */
    private static final int RARE_KOREAN_START = 0xCF70;
    
    /** 韩文音节区容量 */
    private static final int RARE_KOREAN_CAPACITY = 2100;
    
    /** 最大字符集大小 */
    private static final int MAX_RADIX = PUA_CAPACITY + RARE_KOREAN_CAPACITY; // 8500
    
    /** 字符到索引的映射 */
    private final Map<Character, Integer> charToIndex;
    
    /** 索引到字符的数组 */
    private char[] indexToChar;
    
    /** 实际字符集大小（Radix） */
    private int radix;
    
    /** 常用字符区大小（映射到PUA区） */
    private int commonZoneSize;
    
    public CharacterMapping() {
        charToIndex = new HashMap<>(MAX_RADIX);
        try {
            initializeMapping();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize character mapping", e);
        }
    }
    
    /**
     * 初始化字符映射
     * 从资源文件中读取字符，按文件顺序建立映射
     */
    private void initializeMapping() throws IOException {
        List<Character> allChars = new ArrayList<>();
        
        // 1. 添加数字 0-9 (10个)
        for (char c = '0'; c <= '9'; c++) {
            allChars.add(c);
        }
        
        // 2. 添加大写英文字母 A-Z (26个)
        for (char c = 'A'; c <= 'Z'; c++) {
            allChars.add(c);
        }
        
        // 3. 添加小写英文字母 a-z (26个)
        for (char c = 'a'; c <= 'z'; c++) {
            allChars.add(c);
        }
        
        // 4. 添加标点符号
        addPunctuationChars(allChars);
        
        // 5. 从资源文件中读取汉字（按优先级顺序）
        // 一级字表（3500字，常用字）
        List<Character> level1Chars = loadCharsFromResource("/level-1.txt");
        for (char c : level1Chars) {
            if (!allChars.contains(c) && allChars.size() < MAX_RADIX) {
                allChars.add(c);
            }
        }
        logger.info("Loaded Level 1 characters: {}", level1Chars.size());
        
        // 二级字表（3000字，次常用字）
        List<Character> level2Chars = loadCharsFromResource("/level-2.txt");
        for (char c : level2Chars) {
            if (!allChars.contains(c) && allChars.size() < MAX_RADIX) {
                allChars.add(c);
            }
        }
        logger.info("Loaded Level 2 characters: {}", level2Chars.size());
        
        // 三级字表（1605字，较少使用）
        List<Character> level3Chars = loadCharsFromResource("/level-3.txt");
        for (char c : level3Chars) {
            if (!allChars.contains(c) && allChars.size() < MAX_RADIX) {
                allChars.add(c);
            }
        }
        logger.info("Loaded Level 3 characters: {}", level3Chars.size());
        
        // 记录常用区大小（前6400个字符映射到PUA区，剩余映射到韩文区）
        commonZoneSize = Math.min(allChars.size(), PUA_CAPACITY);
        
        // 限制总数不超过 MAX_RADIX
        if (allChars.size() > MAX_RADIX) {
            logger.warn("Character set exceeds MAX_RADIX ({}), truncating to {}", 
                allChars.size(), MAX_RADIX);
            allChars = allChars.subList(0, MAX_RADIX);
        }
        
        // 设置实际的字符集大小
        radix = allChars.size();
        indexToChar = new char[radix];
        
        // 建立映射
        for (int i = 0; i < allChars.size(); i++) {
            addMapping(i, allChars.get(i));
        }
        
        logger.info("Character mapping initialized:");
        logger.info("  - Total characters: {}", radix);
        logger.info("  - Common zone size (mapped to PUA): {}", commonZoneSize);
        logger.info("  - Rare zone size (mapped to Korean): {}", (radix - commonZoneSize));
        logger.info("  - PUA capacity: {} (used: {})", PUA_CAPACITY, commonZoneSize);
        logger.info("  - Korean capacity: {} (used: {})", RARE_KOREAN_CAPACITY, (radix - commonZoneSize));
    }
    
    /**
     * 添加标点符号
     * 按用户指定范围添加：
     * - 半角符号 U+0020 - U+007E (95个，含数字英文，已添加数字英文，这里只添加符号部分)
     * - 全角符号 U+FF01 - U+FF5E (94个)
     * - 中文专用 U+3000 - U+303F (64个)
     * - 通用标点 U+2010 - U+203F (48个)
     */
    private void addPunctuationChars(List<Character> allChars) {
        // 半角符号（除了数字和字母，这些已添加）
        for (int cp = 0x0020; cp <= 0x007E; cp++) {
            char c = (char) cp;
            if (!Character.isLetterOrDigit(c) && !allChars.contains(c)) {
                allChars.add(c);
            }
        }
        
        // 全角符号 U+FF01 - U+FF5E
        for (int cp = 0xFF01; cp <= 0xFF5E; cp++) {
            char c = (char) cp;
            if (!allChars.contains(c)) {
                allChars.add(c);
            }
        }
        
        // 中文专用标点 U+3000 - U+303F
        for (int cp = 0x3000; cp <= 0x303F; cp++) {
            char c = (char) cp;
            if (!allChars.contains(c)) {
                allChars.add(c);
            }
        }
        
        // 通用标点 U+2010 - U+203F
        for (int cp = 0x2010; cp <= 0x203F; cp++) {
            char c = (char) cp;
            if (!allChars.contains(c)) {
                allChars.add(c);
            }
        }
        
        logger.debug("Added punctuation characters, total: {}", allChars.size());
    }
    
    /**
     * 从资源文件中加载字符列表
     * @param resourcePath 资源文件路径
     * @return 字符列表
     */
    private List<Character> loadCharsFromResource(String resourcePath) throws IOException {
        List<Character> chars = new ArrayList<>();
        
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        // 每行应该只有一个字符
                        for (char c : line.toCharArray()) {
                            // 跳过空白字符
                            if (!Character.isWhitespace(c)) {
                                chars.add(c);
                            }
                        }
                    }
                }
            }
        }
        
        return chars;
    }
    
    /**
     * 添加字符映射
     */
    private void addMapping(int index, char originalChar) {
        charToIndex.put(originalChar, index);
        indexToChar[index] = originalChar;
    }
    
    /**
     * 获取字符对应的索引
     * @param c 原始字符
     * @return 索引值 (0 到 radix-1)，如果字符不在字典中返回 -1
     */
    public int getCharIndex(char c) {
        Integer index = charToIndex.get(c);
        return index != null ? index : -1;
    }
    
    /**
     * 根据索引获取原始字符
     * @param index 索引值 (0 到 radix-1)
     * @return 原始字符
     */
    public char getOriginalChar(int index) {
        if (index < 0 || index >= radix) {
            throw new IllegalArgumentException("Index out of range: " + index);
        }
        return indexToChar[index];
    }
    
    /**
     * 将加密后的索引映射为对应的 Unicode 字符
     * 
     * 映射规则：
     * - 索引 0 到 PUA_CAPACITY-1 (0-6399) -> BMP PUA 区 (U+E000 - U+F8FF)
     * - 索引 PUA_CAPACITY 到 MAX_RADIX-1 (6400-8499) -> 罕用韩文音节区 (U+CF70开始)
     * 
     * @param index 加密后的索引
     * @return 映射后的 Unicode 字符
     */
    public char mapToEncryptedChar(int index) {
        if (index < 0 || index >= radix) {
            throw new IllegalArgumentException("Index out of range: " + index);
        }
        
        if (index < PUA_CAPACITY) {
            // 前6400个：映射到 PUA 区
            return (char) (PUA_START + index);
        } else {
            // 后2100个：映射到罕用韩文音节区
            int offset = index - PUA_CAPACITY;
            return (char) (RARE_KOREAN_START + offset);
        }
    }
    
    /**
     * 将加密字符反向映射回索引
     * @param encryptedChar 加密后的字符
     * @return 索引值
     */
    public int mapFromEncryptedChar(char encryptedChar) {
        int codePoint = (int) encryptedChar;
        
        // 检查是否在 PUA 区
        if (codePoint >= PUA_START && codePoint < PUA_START + PUA_CAPACITY) {
            return codePoint - PUA_START;
        }
        
        // 检查是否在罕用韩文音节区
        if (codePoint >= RARE_KOREAN_START && codePoint < RARE_KOREAN_START + RARE_KOREAN_CAPACITY) {
            return PUA_CAPACITY + (codePoint - RARE_KOREAN_START);
        }
        
        throw new IllegalArgumentException("Character is not a valid encrypted character: " + encryptedChar);
    }
    
    /**
     * 检查字符是否为加密字符（在 PUA 或罕用韩文音节区）
     * @param c 待检查的字符
     * @return 如果是加密字符返回 true，否则返回 false
     */
    public boolean isEncryptedChar(char c) {
        int codePoint = (int) c;
        
        // 检查是否在 PUA 区
        if (codePoint >= PUA_START && codePoint < PUA_START + PUA_CAPACITY) {
            return true;
        }
        
        // 检查是否在罕用韩文音节区
        if (codePoint >= RARE_KOREAN_START && codePoint < RARE_KOREAN_START + RARE_KOREAN_CAPACITY) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 将原始索引映射为对应的加密字符索引
     * 此方法用于在填充场景下获取加密字符的索引
     * @param index 原始字符的索引
     * @return 加密字符的索引（本质上就是index本身，因为FF1在索引空间内操作）
     */
    public int mapToEncryptedCharIndex(int index) {
        // FF1算法在索引空间内操作，输入索引和输出索引在同一空间
        // 只是实际显示时会映射到PUA/韩文区
        // 因此这里直接返回索引本身
        if (index < 0 || index >= radix) {
            throw new IllegalArgumentException("Index out of range: " + index);
        }
        return index;
    }
    
    /**
     * 检查字符是否在字典中
     */
    public boolean containsChar(char c) {
        return charToIndex.containsKey(c);
    }
    
    /**
     * 获取字符集大小
     */
    public int getRadix() {
        return radix;
    }
    
    /**
     * 获取常用区大小
     */
    public int getCommonZoneSize() {
        return commonZoneSize;
    }
}
