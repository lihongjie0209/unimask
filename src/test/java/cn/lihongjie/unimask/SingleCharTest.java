package cn.lihongjie.unimask;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 单字符加密测试
 */
public class SingleCharTest {
    
    private static final Logger logger = LoggerFactory.getLogger(SingleCharTest.class);
    private final ChineseFPEService service = new ChineseFPEService("test-single-char-password");
    
    @Test
    public void testSingleCharacterEncryption() {
        // 对于三字名，保留首尾各1个字符，中间只有1个字符需要加密
        String[] testCases = {"张小三", "李小四", "王小五", "赵小六", "孙小七", "周小八", "吴小九", "郑小十"};
        
        for (String name : testCases) {
            // 保留首尾各1个字符，中间1个字符使用单字符加密
            String encrypted = service.encrypt(1, 1, "test-tweak", name);
            String decrypted = service.decrypt(encrypted, 1, 1, "test-tweak");
            
            logger.info("原文: {} -> 加密: {} -> 解密: {}", name, encrypted, decrypted);
            
            // 中间的字符应该被加密
            assert !name.equals(encrypted) : "单字符应被加密: " + name;
            // 首尾应该被保留
            assert encrypted.charAt(0) == name.charAt(0) : "首字符应被保留";
            assert encrypted.charAt(encrypted.length() - 1) == name.charAt(name.length() - 1) : "末字符应被保留";
            // 解密应还原原文
            assert name.equals(decrypted) : "解密应还原原文: " + name + " != " + decrypted;
        }
    }
    
    @Test
    public void testSingleCharacterWithDifferentTweaks() {
        String name = "张小三";
        
        String encrypted1 = service.encrypt(1, 1, "tweak1", name);
        String encrypted2 = service.encrypt(1, 1, "tweak2", name);
        
        logger.info("使用tweak1加密 {} -> {}", name, encrypted1);
        logger.info("使用tweak2加密 {} -> {}", name, encrypted2);
        
        assert !encrypted1.equals(encrypted2) : "不同tweak应产生不同的加密结果";
        assert name.equals(service.decrypt(encrypted1, 1, 1, "tweak1")) : "tweak1应能正确解密";
        assert name.equals(service.decrypt(encrypted2, 1, 1, "tweak2")) : "tweak2应能正确解密";
    }
}
