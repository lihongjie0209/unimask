package cn.lihongjie.unimask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChineseFPEService 单元测试
 */
class ChineseFPEServiceTest {
    
    private ChineseFPEService service;
    
    @BeforeEach
    void setUp() {
        // 使用固定密码派生密钥以确保测试可重复
        service = new ChineseFPEService("test-password-123");
    }
    
    @Test
    void testBasicEncryptDecrypt() {
        String plaintext = "这是一个测试文本";
        String tweak = "test-tweak";
        
        String encrypted = service.encrypt(0, 0, tweak, plaintext);
        assertNotNull(encrypted);
        assertNotEquals(plaintext, encrypted);
        assertEquals(plaintext.length(), encrypted.length());
        
        String decrypted = service.decrypt(encrypted, 0, 0, tweak);
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    void testHeadPreserve() {
        String plaintext = "李宏杰的测试文本";
        String tweak = "tweak";
        
        String encrypted = service.encrypt(3, 0, tweak, plaintext);
        
        // 前3个字符应该保持不变
        assertEquals("李宏杰", encrypted.substring(0, 3));
        assertNotEquals(plaintext, encrypted);
        
        String decrypted = service.decrypt(encrypted, 3, 0, tweak);
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    void testTailPreserve() {
        String plaintext = "测试文本保留尾部";
        String tweak = "tweak";
        
        String encrypted = service.encrypt(0, 2, tweak, plaintext);
        
        // 后2个字符应该保持不变
        assertEquals("尾部", encrypted.substring(encrypted.length() - 2));
        assertNotEquals(plaintext, encrypted);
        
        String decrypted = service.decrypt(encrypted, 0, 2, tweak);
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    void testHeadAndTailPreserve() {
        String plaintext = "保留开头和结尾的文本";
        String tweak = "tweak";
        
        String encrypted = service.encrypt(4, 4, tweak, plaintext);
        
        // 开头4个和结尾4个字符应该保持不变
        assertEquals("保留开头", encrypted.substring(0, 4));
        assertEquals("尾的文本", encrypted.substring(encrypted.length() - 4));
        
        String decrypted = service.decrypt(encrypted, 4, 4, tweak);
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    void testMixedContent() {
        String plaintext = "Hello世界123！Test测试";
        String tweak = "mixed-tweak";
        
        String encrypted = service.encrypt(0, 0, tweak, plaintext);
        assertNotEquals(plaintext, encrypted);
        assertEquals(plaintext.length(), encrypted.length());
        
        String decrypted = service.decrypt(encrypted, 0, 0, tweak);
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    void testDifferentTweaks() {
        String plaintext = "相同的明文不同的扰码";
        
        String encrypted1 = service.encrypt(0, 0, "tweak1", plaintext);
        String encrypted2 = service.encrypt(0, 0, "tweak2", plaintext);
        
        // 不同的 tweak 应该产生不同的密文
        assertNotEquals(encrypted1, encrypted2);
        
        // 但都能正确解密
        assertEquals(plaintext, service.decrypt(encrypted1, 0, 0, "tweak1"));
        assertEquals(plaintext, service.decrypt(encrypted2, 0, 0, "tweak2"));
    }
    
    @Test
    void testEmptyString() {
        String plaintext = "";
        String encrypted = service.encrypt(0, 0, "tweak", plaintext);
        assertEquals("", encrypted);
        
        String decrypted = service.decrypt(encrypted, 0, 0, "tweak");
        assertEquals("", decrypted);
    }
    
    @Test
    void testSingleCharacter() {
        // FF1 算法通过添加固定后缀"一"来支持单字符加密
        String plaintext = "中";
        String tweak = "tweak";
        
        String encrypted = service.encrypt(0, 0, tweak, plaintext);
        assertNotEquals(plaintext, encrypted); // 单字符现在会被加密
        assertEquals(1, encrypted.length());
        
        String decrypted = service.decrypt(encrypted, 0, 0, tweak);
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    void testPreserveLengthExceedsTotal() {
        String plaintext = "短文本";
        String encrypted = service.encrypt(2, 2, "tweak", plaintext);
        
        // 优先保证头部保留，尾部不足时会被调整
        // headPreserve=2保留"短文"，actualTailPreserve=0，加密"本"
        assertNotEquals(plaintext, encrypted);
        assertEquals(3, encrypted.length()); // "短文" + 加密后的"本"
        assertEquals(plaintext.charAt(0), encrypted.charAt(0)); // 首字符保留
        assertEquals(plaintext.charAt(1), encrypted.charAt(1)); // 第二字符保留
        
        // 解密应能还原
        String decrypted = service.decrypt(encrypted, 2, 2, "tweak");
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    void testLongText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("这是第").append(i).append("行测试文本。");
        }
        String plaintext = sb.toString();
        String tweak = "long-text-tweak";
        
        String encrypted = service.encrypt(10, 10, tweak, plaintext);
        assertNotEquals(plaintext, encrypted);
        assertEquals(plaintext.length(), encrypted.length());
        
        String decrypted = service.decrypt(encrypted, 10, 10, tweak);
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    void testIdempotence() {
        String plaintext = "测试幂等性";
        String tweak = "idempotence";
        
        // 多次加密应该产生相同的结果
        String encrypted1 = service.encrypt(0, 0, tweak, plaintext);
        String encrypted2 = service.encrypt(0, 0, tweak, plaintext);
        assertEquals(encrypted1, encrypted2);
        
        // 多次解密也应该产生相同的结果
        String decrypted1 = service.decrypt(encrypted1, 0, 0, tweak);
        String decrypted2 = service.decrypt(encrypted2, 0, 0, tweak);
        assertEquals(decrypted1, decrypted2);
        assertEquals(plaintext, decrypted1);
    }
    
    @Test
    void testInvalidCharacter() {
        // 测试字典外的字符（如emoji）应原样保留
        String plaintext = "包含特殊字符：\uD83D\uDE00"; // 包含 emoji
        
        // 不应抛出异常，emoji 应原样保留
        String encrypted = service.encrypt(0, 0, "tweak", plaintext);
        assertTrue(encrypted.contains("\uD83D\uDE00")); // emoji 原样保留
        
        String decrypted = service.decrypt(encrypted, 0, 0, "tweak");
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    void testNullTweak() {
        String plaintext = "测试空扰码";
        
        // null tweak 应该被当作空字符串处理
        String encrypted = service.encrypt(0, 0, null, plaintext);
        String decrypted = service.decrypt(encrypted, 0, 0, null);
        
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    void testLevel1Characters() {
        // 测试一级字表中的常用字
        String plaintext = "人民政府工作报告";
        String tweak = "level1";
        
        String encrypted = service.encrypt(0, 0, tweak, plaintext);
        assertNotEquals(plaintext, encrypted);
        
        String decrypted = service.decrypt(encrypted, 0, 0, tweak);
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    void testPasswordBasedKeyDerivation() {
        // 测试从密码派生密钥
        String password = "my-secure-password-123";
        ChineseFPEService service1 = new ChineseFPEService(password);
        ChineseFPEService service2 = new ChineseFPEService(password);
        
        String plaintext = "测试密码派生";
        String tweak = "test-kdf";
        
        // 相同密码应该产生相同的加密结果
        String encrypted1 = service1.encrypt(0, 0, tweak, plaintext);
        String encrypted2 = service2.encrypt(0, 0, tweak, plaintext);
        assertEquals(encrypted1, encrypted2, "相同密码应产生相同加密结果");
        
        // 不同密码应该产生不同的加密结果
        ChineseFPEService service3 = new ChineseFPEService("different-password");
        String encrypted3 = service3.encrypt(0, 0, tweak, plaintext);
        assertNotEquals(encrypted1, encrypted3, "不同密码应产生不同加密结果");
    }
    
    @Test
    void testCustomSaltKeyDerivation() {
        // 测试使用自定义盐值派生密钥
        String password = "test-password";
        byte[] salt1 = "custom-salt-1234".getBytes();
        byte[] salt2 = "another-salt-xyz".getBytes();
        
        byte[] key1 = ChineseFPEService.deriveKeyFromPassword(password, salt1, 16);
        byte[] key2 = ChineseFPEService.deriveKeyFromPassword(password, salt2, 16);
        byte[] key3 = ChineseFPEService.deriveKeyFromPassword(password, salt1, 16);
        
        // 相同密码和盐应产生相同密钥
        assertArrayEquals(key1, key3, "相同密码和盐应产生相同密钥");
        
        // 不同盐应产生不同密钥
        assertFalse(java.util.Arrays.equals(key1, key2), "不同盐应产生不同密钥");
        
        // 验证密钥长度
        assertEquals(16, key1.length);
    }
    
    @Test
    void testDifferentKeyLengths() {
        // 测试不同长度的密钥派生
        String password = "test-password";
        
        ChineseFPEService service128 = new ChineseFPEService(password, 16); // AES-128
        ChineseFPEService service192 = new ChineseFPEService(password, 24); // AES-192
        ChineseFPEService service256 = new ChineseFPEService(password, 32); // AES-256
        
        String plaintext = "测试不同密钥长度";
        String tweak = "test";
        
        // 所有长度的密钥都应该能正常工作
        String encrypted128 = service128.encrypt(0, 0, tweak, plaintext);
        String decrypted128 = service128.decrypt(encrypted128, 0, 0, tweak);
        assertEquals(plaintext, decrypted128);
        
        String encrypted192 = service192.encrypt(0, 0, tweak, plaintext);
        String decrypted192 = service192.decrypt(encrypted192, 0, 0, tweak);
        assertEquals(plaintext, decrypted192);
        
        String encrypted256 = service256.encrypt(0, 0, tweak, plaintext);
        String decrypted256 = service256.decrypt(encrypted256, 0, 0, tweak);
        assertEquals(plaintext, decrypted256);
        
        // 不同密钥长度应产生不同的加密结果
        assertNotEquals(encrypted128, encrypted192);
        assertNotEquals(encrypted128, encrypted256);
        assertNotEquals(encrypted192, encrypted256);
    }
}
