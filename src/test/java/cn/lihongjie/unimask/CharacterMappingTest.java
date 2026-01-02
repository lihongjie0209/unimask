package cn.lihongjie.unimask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CharacterMapping 单元测试
 * 验证字符映射规则，特别是PUA区和韩文音节区的分配
 */
public class CharacterMappingTest {
    
    private static final Logger logger = LoggerFactory.getLogger(CharacterMappingTest.class);
    private CharacterMapping mapping;
    
    // Unicode区域常量
    private static final int PUA_START = 0xE000;
    private static final int PUA_END = 0xF8FF;
    private static final int RARE_KOREAN_START = 0xCF70;
    private static final int PUA_CAPACITY = 6400;
    
    @BeforeEach
    void setUp() {
        mapping = new CharacterMapping();
    }
    
    @Test
    void testBasicMapping() {
        // 测试基本字符在字典中
        assertTrue(mapping.containsChar('0'));
        assertTrue(mapping.containsChar('9'));
        assertTrue(mapping.containsChar('A'));
        assertTrue(mapping.containsChar('Z'));
        assertTrue(mapping.containsChar('a'));
        assertTrue(mapping.containsChar('z'));
        
        // 测试索引获取
        assertTrue(mapping.getCharIndex('0') >= 0);
        assertTrue(mapping.getCharIndex('A') >= 0);
        assertTrue(mapping.getCharIndex('a') >= 0);
    }
    
    @Test
    void testLevel1CharsInPuaRegion() throws IOException {
        // Level 1字表应该在PUA区（因为数字+字母+标点+level1总共不超过6400）
        List<Character> level1Chars = loadCharsFromResource("/level-1.txt");
        
        logger.info("Testing {} Level 1 characters", level1Chars.size());
        
        int puaCount = 0;
        int koreanCount = 0;
        
        for (char c : level1Chars) {
            if (mapping.containsChar(c)) {
                int index = mapping.getCharIndex(c);
                char encrypted = mapping.mapToEncryptedChar(index);
                int codePoint = (int) encrypted;
                
                if (codePoint >= PUA_START && codePoint <= PUA_END) {
                    puaCount++;
                } else if (codePoint >= RARE_KOREAN_START) {
                    koreanCount++;
                }
            }
        }
        
        logger.info("Level 1: {} in PUA, {} in Korean region", puaCount, koreanCount);
        
        // Level 1字表应该全部在PUA区
        assertEquals(level1Chars.size(), puaCount, "Level 1字表应该全部在PUA区");
        assertEquals(0, koreanCount, "Level 1字表不应该有字符在韩文区");
    }
    
    @Test
    void testLevel2CharsPartiallyInKoreanRegion() throws IOException {
        // Level 2字表的最后部分应该在韩文区
        List<Character> level2Chars = loadCharsFromResource("/level-2.txt");
        
        logger.info("Testing {} Level 2 characters", level2Chars.size());
        
        int puaCount = 0;
        int koreanCount = 0;
        Character firstKoreanChar = null;
        int firstKoreanIndex = -1;
        
        for (char c : level2Chars) {
            if (mapping.containsChar(c)) {
                int index = mapping.getCharIndex(c);
                char encrypted = mapping.mapToEncryptedChar(index);
                int codePoint = (int) encrypted;
                
                if (codePoint >= PUA_START && codePoint <= PUA_END) {
                    puaCount++;
                } else if (codePoint >= RARE_KOREAN_START) {
                    koreanCount++;
                    if (firstKoreanChar == null) {
                        firstKoreanChar = c;
                        firstKoreanIndex = index;
                    }
                }
            }
        }
        
        logger.info("Level 2: {} in PUA, {} in Korean region", puaCount, koreanCount);
        
        // Level 2应该有部分字符在韩文区
        assertTrue(koreanCount > 0, "Level 2应该有字符在韩文区");
        assertTrue(puaCount > 0, "Level 2应该有字符在PUA区");
        
        if (firstKoreanChar != null) {
            logger.info("First Level 2 char in Korean region: '{}' (index: {}, encrypted: U+{})", 
                firstKoreanChar, firstKoreanIndex, 
                Integer.toHexString(mapping.mapToEncryptedChar(firstKoreanIndex)).toUpperCase());
            
            // 第一个进入韩文区的字符索引应该正好是6400
            assertEquals(PUA_CAPACITY, firstKoreanIndex, 
                "第一个进入韩文区的字符索引应该是" + PUA_CAPACITY);
        }
    }
    
    @Test
    void testMappingBoundary() {
        // 测试6400边界前后的映射
        int boundaryIndex = PUA_CAPACITY - 1;
        
        // 索引6399应该映射到PUA区的最后一个字符
        char encrypted6399 = mapping.mapToEncryptedChar(boundaryIndex);
        int codePoint6399 = (int) encrypted6399;
        assertTrue(codePoint6399 >= PUA_START && codePoint6399 <= PUA_END, 
            "索引6399应该在PUA区");
        assertEquals(PUA_END, codePoint6399, "索引6399应该映射到PUA区的最后一个字符");
        
        // 索引6400应该映射到韩文区的第一个字符
        if (mapping.getRadix() > PUA_CAPACITY) {
            char encrypted6400 = mapping.mapToEncryptedChar(PUA_CAPACITY);
            int codePoint6400 = (int) encrypted6400;
            assertTrue(codePoint6400 >= RARE_KOREAN_START, 
                "索引6400应该在韩文区");
            assertEquals(RARE_KOREAN_START, codePoint6400, 
                "索引6400应该映射到韩文区的第一个字符");
            
            logger.info("Boundary test: index {} -> U+{} (PUA), index {} -> U+{} (Korean)",
                boundaryIndex, Integer.toHexString(codePoint6399).toUpperCase(),
                PUA_CAPACITY, Integer.toHexString(codePoint6400).toUpperCase());
        }
    }
    
    @Test
    void testReverseMapping() {
        // 测试正向和反向映射的一致性
        for (int i = 0; i < Math.min(100, mapping.getRadix()); i++) {
            char encrypted = mapping.mapToEncryptedChar(i);
            int reversedIndex = mapping.mapFromEncryptedChar(encrypted);
            assertEquals(i, reversedIndex, "索引" + i + "的正反映射应该一致");
        }
        
        // 测试边界附近的反向映射
        if (mapping.getRadix() > PUA_CAPACITY) {
            // PUA区最后几个
            for (int i = PUA_CAPACITY - 5; i < PUA_CAPACITY; i++) {
                char encrypted = mapping.mapToEncryptedChar(i);
                int reversedIndex = mapping.mapFromEncryptedChar(encrypted);
                assertEquals(i, reversedIndex);
            }
            
            // 韩文区开始几个
            for (int i = PUA_CAPACITY; i < Math.min(PUA_CAPACITY + 5, mapping.getRadix()); i++) {
                char encrypted = mapping.mapToEncryptedChar(i);
                int reversedIndex = mapping.mapFromEncryptedChar(encrypted);
                assertEquals(i, reversedIndex);
            }
        }
    }
    
    @Test
    void testCharacterDistribution() {
        // 统计字符分布
        int totalChars = mapping.getRadix();
        int commonZone = mapping.getCommonZoneSize();
        int rareZone = totalChars - commonZone;
        
        logger.info("Character distribution:");
        logger.info("  Total: {}", totalChars);
        logger.info("  Common zone (PUA): {}", commonZone);
        logger.info("  Rare zone (Korean): {}", rareZone);
        
        assertEquals(PUA_CAPACITY, commonZone, "常用区大小应该等于PUA容量");
        assertTrue(rareZone > 0, "应该有字符在韩文区");
        
        // 验证总数约为8410（根据日志）
        assertTrue(totalChars >= 8400 && totalChars <= 8500, 
            "总字符数应该在8400-8500之间，实际: " + totalChars);
    }
    
    /**
     * 从资源文件加载字符列表（复制自CharacterMapping）
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
                        for (char c : line.toCharArray()) {
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
}
