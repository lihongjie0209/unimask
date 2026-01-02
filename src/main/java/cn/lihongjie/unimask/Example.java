package cn.lihongjie.unimask;

import java.util.Base64;

/**
 * ChineseFPEService 使用示例
 * 
 * @author lihongjie
 */
public class Example {
    
    public static void main(String[] args) {
        // 示例1：基本加密解密
        System.out.println("=== 示例1：基本加密解密 ===");
        basicExample();
        
        // 示例2：头尾保留
        System.out.println("\n=== 示例2：头尾保留 ===");
        preserveExample();
        
        // 示例3：混合内容
        System.out.println("\n=== 示例3：混合内容（中英文+数字+标点） ===");
        mixedContentExample();
        
        // 示例4：不同扰码产生不同密文
        System.out.println("\n=== 示例4：不同扰码产生不同密文 ===");
        differentTweaksExample();
        
        // 示例5：密钥管理
        System.out.println("\n=== 示例5：密钥管理 ===");
        keyManagementExample();
        
        // 示例6：实际应用场景
        System.out.println("\n=== 示例6：实际应用场景 - 姓名脱敏 ===");
        nameDesensitizationExample();
    }
    
    /**
     * 基本加密解密示例
     */
    private static void basicExample() {
        // 创建服务实例（使用随机密钥）
        ChineseFPEService service = new ChineseFPEService();
        
        String plaintext = "这是一个保密的文本内容";
        String tweak = "myTweak123";
        
        // 加密
        String encrypted = service.encrypt(0, 0, tweak, plaintext);
        System.out.println("原文: " + plaintext);
        System.out.println("密文: " + encrypted);
        System.out.println("密文长度: " + encrypted.length() + " (与原文相同)");
        
        // 解密
        String decrypted = service.decrypt(encrypted, 0, 0, tweak);
        System.out.println("解密: " + decrypted);
        System.out.println("验证: " + (plaintext.equals(decrypted) ? "✓ 成功" : "✗ 失败"));
    }
    
    /**
     * 头尾保留示例
     */
    private static void preserveExample() {
        ChineseFPEService service = new ChineseFPEService();
        
        String plaintext = "李宏杰的银行账号是1234567890";
        String tweak = "preserve";
        
        // 保留前3个字符和后0个字符
        String encrypted1 = service.encrypt(3, 0, tweak, plaintext);
        System.out.println("原文: " + plaintext);
        System.out.println("保留前3字符: " + encrypted1);
        
        // 保留前3个和后4个字符
        String encrypted2 = service.encrypt(3, 4, tweak, plaintext);
        System.out.println("保留前3后4字符: " + encrypted2);
        
        // 解密
        String decrypted = service.decrypt(encrypted2, 3, 4, tweak);
        System.out.println("解密结果: " + decrypted);
        System.out.println("验证: " + (plaintext.equals(decrypted) ? "✓ 成功" : "✗ 失败"));
    }
    
    /**
     * 混合内容示例
     */
    private static void mixedContentExample() {
        ChineseFPEService service = new ChineseFPEService();
        
        String plaintext = "Hello世界! 2026年1月2日, 欢迎使用UniMask.";
        String tweak = "mixed";
        
        String encrypted = service.encrypt(0, 0, tweak, plaintext);
        System.out.println("原文: " + plaintext);
        System.out.println("密文: " + encrypted);
        
        String decrypted = service.decrypt(encrypted, 0, 0, tweak);
        System.out.println("解密: " + decrypted);
        System.out.println("验证: " + (plaintext.equals(decrypted) ? "✓ 成功" : "✗ 失败"));
    }
    
    /**
     * 不同扰码示例
     */
    private static void differentTweaksExample() {
        ChineseFPEService service = new ChineseFPEService();
        
        String plaintext = "相同的明文";
        
        String encrypted1 = service.encrypt(0, 0, "tweak1", plaintext);
        String encrypted2 = service.encrypt(0, 0, "tweak2", plaintext);
        String encrypted3 = service.encrypt(0, 0, "tweak3", plaintext);
        
        System.out.println("原文: " + plaintext);
        System.out.println("Tweak1 密文: " + encrypted1);
        System.out.println("Tweak2 密文: " + encrypted2);
        System.out.println("Tweak3 密文: " + encrypted3);
        System.out.println("密文是否不同: " + 
            (!encrypted1.equals(encrypted2) && !encrypted2.equals(encrypted3) ? "✓ 是" : "✗ 否"));
        
        // 验证各自能正确解密
        System.out.println("Tweak1 解密: " + service.decrypt(encrypted1, 0, 0, "tweak1"));
        System.out.println("Tweak2 解密: " + service.decrypt(encrypted2, 0, 0, "tweak2"));
        System.out.println("Tweak3 解密: " + service.decrypt(encrypted3, 0, 0, "tweak3"));
    }
    
    /**
     * 密钥管理示例
     */
    private static void keyManagementExample() {
        // 使用固定密钥（实际应用中应该安全存储）
        byte[] key = new byte[16];
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) (i * 2);
        }
        
        // 创建服务实例
        ChineseFPEService service1 = new ChineseFPEService(key);
        
        String plaintext = "需要持久化的数据";
        String tweak = "persist";
        String encrypted = service1.encrypt(0, 0, tweak, plaintext);
        
        System.out.println("原文: " + plaintext);
        System.out.println("密文: " + encrypted);
        
        // 导出密钥（用于持久化或传输）
        byte[] exportedKey = service1.getKey();
        String keyBase64 = Base64.getEncoder().encodeToString(exportedKey);
        System.out.println("密钥(Base64): " + keyBase64);
        
        // 模拟：在另一个地方使用相同密钥创建服务
        byte[] importedKey = Base64.getDecoder().decode(keyBase64);
        ChineseFPEService service2 = new ChineseFPEService(importedKey);
        
        String decrypted = service2.decrypt(encrypted, 0, 0, tweak);
        System.out.println("解密: " + decrypted);
        System.out.println("验证: " + (plaintext.equals(decrypted) ? "✓ 成功" : "✗ 失败"));
    }
    
    /**
     * 实际应用场景：姓名脱敏
     */
    private static void nameDesensitizationExample() {
        ChineseFPEService service = new ChineseFPEService();
        
        // 姓名列表
        String[] names = {
            "张三",
            "李四",
            "王五",
            "赵六",
            "刘德华",
            "马化腾",
            "李宏杰"
        };
        
        String tweak = "name-desensitization-2026";
        
        System.out.println("场景：数据库中的姓名需要脱敏，但保留第一个字（姓氏）\n");
        
        for (String name : names) {
            // 保留第一个字（姓氏），加密后面的字（名字）
            // 注意：FF1算法要求至少2个字符，单字名无法加密
            if (name.length() <= 1) {
                System.out.printf("%-10s -> %-10s (名字太短，无法加密)\n", name, name);
                continue;
            }
            
            int preserveCount = 1;
            String encrypted = service.encrypt(preserveCount, 0, tweak, name);
            
            System.out.printf("%-10s -> %-10s", name, encrypted);
            
            // 验证可以解密
            String decrypted = service.decrypt(encrypted, preserveCount, 0, tweak);
            System.out.println(" (解密: " + decrypted + ")");
        }
        
        System.out.println("\n特点：");
        System.out.println("1. 姓氏可见，便于识别");
        System.out.println("2. 名字已加密，保护隐私");
        System.out.println("3. 长度不变，不影响数据库字段设计");
        System.out.println("4. 可逆加密，授权后可恢复原始数据");
    }
}
