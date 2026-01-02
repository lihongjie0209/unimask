package cn.lihongjie.unimask;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 使用 jqwik 进行基于属性的测试
 * 验证字符映射的核心属性
 */
class CharacterMappingPropertyTest {
    
    private static final Logger logger = LoggerFactory.getLogger(CharacterMappingPropertyTest.class);
    
    private static final int PUA_START = 0xE000;
    private static final int PUA_END = 0xF8FF;
    private static final int RARE_KOREAN_START = 0xCF70;
    private static final int PUA_CAPACITY = 6400;
    
    private final CharacterMapping mapping = new CharacterMapping();
    
    /**
     * 属性1: Level 1 常用字加密后必须在 PUA 区域
     */
    @Property
    @Label("Level 1 常用字加密后必须在 PUA 区域")
    void level1CharsAlwaysMapToPuaRegion(@ForAll("level1Chars") char c) {
        if (mapping.containsChar(c)) {
            int index = mapping.getCharIndex(c);
            char encrypted = mapping.mapToEncryptedChar(index);
            int codePoint = (int) encrypted;
            
            // 断言：常用字的索引必须小于 PUA_CAPACITY，加密后必须在 PUA 区域
            Assume.that(index < PUA_CAPACITY);
            assert codePoint >= PUA_START && codePoint <= PUA_END : 
                String.format("Level 1 字符 '%s' (U+%04X) 索引 %d 加密后 U+%04X 不在 PUA 区域 [U+%04X-U+%04X]",
                    c, (int)c, index, codePoint, PUA_START, PUA_END);
        }
    }
    
    /**
     * 属性2: Level 2 后部分（索引 >= 6400）加密后必须在韩文区域
     */
    @Property
    @Label("Level 2 后部分（罕用字）加密后必须在韩文区域")
    void level2RareCharsAlwaysMapToKoreanRegion(@ForAll("level2RareChars") char c) {
        if (mapping.containsChar(c)) {
            int index = mapping.getCharIndex(c);
            
            // 只测试索引 >= 6400 的字符（罕用字）
            Assume.that(index >= PUA_CAPACITY);
            
            char encrypted = mapping.mapToEncryptedChar(index);
            int codePoint = (int) encrypted;
            
            // 断言：罕用字加密后必须在韩文区域
            assert codePoint >= RARE_KOREAN_START : 
                String.format("Level 2 罕用字符 '%s' (U+%04X) 索引 %d 加密后 U+%04X 不在韩文区域 (应 >= U+%04X)",
                    c, (int)c, index, codePoint, RARE_KOREAN_START);
        }
    }
    
    /**
     * 属性3: Level 3 罕见字（去重后）加密后必须在韩文区域
     */
    @Property
    @Label("Level 3 罕见字（去重后）加密后必须在韩文区域")
    void level3CharsAlwaysMapToKoreanRegion(@ForAll("level3UniqueChars") char c) {
        if (mapping.containsChar(c)) {
            int index = mapping.getCharIndex(c);
            
            // 只测试索引 >= 6400 的字符（非重复字符）
            Assume.that(index >= PUA_CAPACITY);
            
            char encrypted = mapping.mapToEncryptedChar(index);
            int codePoint = (int) encrypted;
            
            // 断言：非重复的 Level 3 字符加密后必须在韩文区域
            assert codePoint >= RARE_KOREAN_START : 
                String.format("Level 3 字符 '%s' (U+%04X) 索引 %d 加密后 U+%04X 不在韩文区域 (应 >= U+%04X)",
                    c, (int)c, index, codePoint, RARE_KOREAN_START);
        }
    }
    
    /**
     * 属性4: 任意常见字（索引 < 6400）加密后必在 PUA 区
     */
    @Property
    @Label("所有常见字（索引 < 6400）加密后必在 PUA 区")
    void allCommonCharsMapToPua(@ForAll @IntRange(min = 0, max = 6399) int index) {
        char encrypted = mapping.mapToEncryptedChar(index);
        int codePoint = (int) encrypted;
        
        assert codePoint >= PUA_START && codePoint <= PUA_END :
            String.format("索引 %d 加密后 U+%04X 不在 PUA 区域 [U+%04X-U+%04X]",
                index, codePoint, PUA_START, PUA_END);
    }
    
    /**
     * 属性5: 任意罕见字（索引 >= 6400）加密后必在韩文区
     */
    @Property
    @Label("所有罕见字（索引 >= 6400）加密后必在韩文区")
    void allRareCharsMapToKorean(@ForAll @IntRange(min = 6400, max = 8409) int index) {
        char encrypted = mapping.mapToEncryptedChar(index);
        int codePoint = (int) encrypted;
        
        assert codePoint >= RARE_KOREAN_START :
            String.format("索引 %d 加密后 U+%04X 不在韩文区域 (应 >= U+%04X)",
                index, codePoint, RARE_KOREAN_START);
    }
    
    /**
     * 属性6: 加密-解密可逆性
     */
    @Property
    @Label("字符映射的可逆性：encrypt(decrypt(x)) == x")
    void mappingIsReversible(@ForAll @IntRange(min = 0, max = 8409) int originalIndex) {
        char encrypted = mapping.mapToEncryptedChar(originalIndex);
        int decryptedIndex = mapping.mapFromEncryptedChar(encrypted);
        
        assert decryptedIndex == originalIndex :
            String.format("索引 %d -> 加密 U+%04X -> 解密索引 %d (不一致)",
                originalIndex, (int)encrypted, decryptedIndex);
    }
    
    /**
     * 属性7: 映射是单射（一对一）
     */
    @Property
    @Label("映射是单射：不同索引映射到不同字符")
    void mappingIsInjective(
            @ForAll @IntRange(min = 0, max = 8409) int index1,
            @ForAll @IntRange(min = 0, max = 8409) int index2) {
        
        Assume.that(index1 != index2);
        
        char encrypted1 = mapping.mapToEncryptedChar(index1);
        char encrypted2 = mapping.mapToEncryptedChar(index2);
        
        assert encrypted1 != encrypted2 :
            String.format("不同索引 %d 和 %d 映射到相同字符 U+%04X",
                index1, index2, (int)encrypted1);
    }
    
    // ==================== 数据提供器 ====================
    
    /**
     * 提供 Level 1 字符
     */
    @Provide
    Arbitrary<Character> level1Chars() {
        try {
            List<Character> chars = loadCharsFromResource("/level-1.txt");
            return Arbitraries.of(chars);
        } catch (IOException e) {
            logger.error("Failed to load Level 1 chars", e);
            return Arbitraries.of('中', '国', '人');
        }
    }
    
    /**
     * 提供 Level 2 后部分字符（罕用字）
     */
    @Provide
    Arbitrary<Character> level2RareChars() {
        try {
            List<Character> allLevel2 = loadCharsFromResource("/level-2.txt");
            List<Character> rareChars = new ArrayList<>();
            
            // 筛选索引 >= 6400 的字符
            for (char c : allLevel2) {
                if (mapping.containsChar(c)) {
                    int index = mapping.getCharIndex(c);
                    if (index >= PUA_CAPACITY) {
                        rareChars.add(c);
                    }
                }
            }
            
            if (rareChars.isEmpty()) {
                // 如果没有找到，返回默认值
                return Arbitraries.of('螨', '蟎', '蠃');
            }
            
            return Arbitraries.of(rareChars);
        } catch (IOException e) {
            logger.error("Failed to load Level 2 rare chars", e);
            return Arbitraries.of('螨', '蟎', '蠃');
        }
    }
    
    /**
     * 提供 Level 3 非重复字符（索引 >= 6400）
     */
    @Provide
    Arbitrary<Character> level3UniqueChars() {
        try {
            List<Character> allLevel3 = loadCharsFromResource("/level-3.txt");
            List<Character> uniqueChars = new ArrayList<>();
            Set<Integer> seenIndices = new HashSet<>();
            
            // 筛选索引 >= 6400 且未重复的字符
            for (char c : allLevel3) {
                if (mapping.containsChar(c)) {
                    int index = mapping.getCharIndex(c);
                    if (index >= PUA_CAPACITY && !seenIndices.contains(index)) {
                        uniqueChars.add(c);
                        seenIndices.add(index);
                    }
                }
            }
            
            if (uniqueChars.isEmpty()) {
                // 如果没有找到，返回默认值
                return Arbitraries.of('亍', '尢', '彳');
            }
            
            return Arbitraries.of(uniqueChars);
        } catch (IOException e) {
            logger.error("Failed to load Level 3 unique chars", e);
            return Arbitraries.of('亍', '尢', '彳');
        }
    }
    
    /**
     * 从资源文件加载字符列表
     */
    private List<Character> loadCharsFromResource(String resourcePath) throws IOException {
        List<Character> chars = new ArrayList<>();
        
        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }
        
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    for (char c : line.toCharArray()) {
                        if (!Character.isWhitespace(c)) {
                            chars.add(c);
                        }
                    }
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (is != null) {
                is.close();
            }
        }
        
        return chars;
    }
}
