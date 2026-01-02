package cn.lihongjie.unimask;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.fpe.FPEEngine;
import org.bouncycastle.crypto.fpe.FPEFF1Engine;
import org.bouncycastle.crypto.params.FPEParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

/**
 * 中文格式保留加密服务
 * 
 * 使用 FF1 算法对包含汉字、英文和标点的文本进行格式保留加密
 * 采用特殊的 3 字节 Unicode 映射方案
 * 
 * 特性：
 * - 基于 NIST SP 800-38G 标准的 FF1 算法
 * - 支持 8500 个字符的字符集（常用汉字、ASCII、标点、罕见汉字）
 * - 支持头尾保留功能
 * - 加密后的字符映射到 PUA 和罕用韩文区，保持 3 字节 Unicode
 * 
 * @author lihongjie
 */
public class ChineseFPEService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChineseFPEService.class);
    
    /** 字符映射工具 */
    private final CharacterMapping charMapping;
    
    /** AES 密钥（128位/16字节） */
    private final byte[] key;
    
    /** FF1 引擎 */
    private final FPEEngine ff1Engine;
    
    /**
     * 构造函数（从密码字符串派生密钥）
     * 使用 PBKDF2-HMAC-SHA256 从密码派生 AES-128 密钥
     * 
     * @param password 密码字符串
     */
    public ChineseFPEService(String password) {
        this(deriveKeyFromPassword(password, 16));
    }
    
    /**
     * 构造函数（从密码字符串派生指定长度的密钥）
     * 
     * @param password 密码字符串
     * @param keyLength 密钥长度（16=AES-128, 24=AES-192, 32=AES-256）
     */
    public ChineseFPEService(String password, int keyLength) {
        this(deriveKeyFromPassword(password, keyLength));
    }
    
    /**
     * 构造函数（直接使用字节数组密钥）
     * 
     * @param key AES 密钥（必须是 16、24 或 32 字节）
     */
    public ChineseFPEService(byte[] key) {
        if (key == null || (key.length != 16 && key.length != 24 && key.length != 32)) {
            throw new IllegalArgumentException(
                "Key must be 16, 24, or 32 bytes (128, 192, or 256 bits)");
        }
        
        this.key = Arrays.copyOf(key, key.length);
        this.charMapping = new CharacterMapping();
        
        // 初始化 FF1 引擎
        BlockCipher aesEngine = new AESEngine();
        this.ff1Engine = new FPEFF1Engine(aesEngine);
    }
    
    /**
     * 加密接口
     * 
     * @param headPreserve 开头保留不加密的字符数
     * @param tailPreserve 末尾保留不加密的字符数
     * @param tweak 扰码（字符串格式，内部需转为字节数组）
     * @param plaintext 待加密的明文
     * @return 密文（保留开头结尾，中间部分映射为 PUA/韩文 字符）
     * @throws IllegalArgumentException 如果参数无效
     */
    public String encrypt(int headPreserve, int tailPreserve, String tweak, String plaintext) {
        // 参数验证
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        
        if (headPreserve < 0 || tailPreserve < 0) {
            throw new IllegalArgumentException("headPreserve and tailPreserve must be non-negative");
        }
        
        // 优先保证头部，如果长度不够，调整或忽略尾部
        if (headPreserve >= plaintext.length()) {
            return plaintext; // 连头部都无法完全保留，返回原文
        }
        
        // 调整尾部保留：确保至少有1个字符可以加密
        int actualTailPreserve = Math.min(tailPreserve, plaintext.length() - headPreserve - 1);
        if (actualTailPreserve < 0) {
            actualTailPreserve = 0;
        }
        
        if (tweak == null) {
            tweak = "";
        }
        
        // 分割字符串（使用调整后的尾部保留）
        String prefix = plaintext.substring(0, headPreserve);
        String suffix = plaintext.substring(plaintext.length() - actualTailPreserve);
        String middle = plaintext.substring(headPreserve, plaintext.length() - actualTailPreserve);
        
        if (middle.isEmpty()) {
            return plaintext;
        }
        
        // 加密中间部分
        String encryptedMiddle = encryptMiddlePart(middle, tweak);
        
        // 组合结果
        return prefix + encryptedMiddle + suffix;
    }
    
    /**
     * 解密接口（整个字符串作为加密内容）
     * 
     * @param encryptedText 密文（不包含保留部分）
     * @param tweak 必须与加密时一致的扰码
     * @return 原始明文
     * @throws IllegalArgumentException 如果参数无效
     */
    public String decrypt(String encryptedText, String tweak) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        
        if (tweak == null) {
            tweak = "";
        }
        
        // 注意：解密时无法自动识别保留区，需要调用者知道哪些是加密区
        // 这里假设整个字符串都是加密区（实际使用中需要配合元数据）
        return decryptMiddlePart(encryptedText, tweak);
    }
    
    /**
     * 解密接口（带头尾保留信息）
     * 
     * @param encryptedText 包含保留部分的密文
     * @param headPreserve 开头保留的字符数
     * @param tailPreserve 末尾保留的字符数
     * @param tweak 必须与加密时一致的扰码
     * @return 原始明文
     */
    public String decrypt(String encryptedText, int headPreserve, int tailPreserve, String tweak) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        
        if (headPreserve < 0 || tailPreserve < 0) {
            throw new IllegalArgumentException("headPreserve and tailPreserve must be non-negative");
        }
        
        // 优先保证头部，如果长度不够，调整或忽略尾部
        if (headPreserve >= encryptedText.length()) {
            return encryptedText; // 连头部都无法完全保留，返回原文
        }
        
        // 调整尾部保留：确保至少有1个字符可以解密
        int actualTailPreserve = Math.min(tailPreserve, encryptedText.length() - headPreserve - 1);
        if (actualTailPreserve < 0) {
            actualTailPreserve = 0;
        }
        
        if (tweak == null) {
            tweak = "";
        }
        
        // 分割字符串（使用调整后的尾部保留）
        String prefix = encryptedText.substring(0, headPreserve);
        String suffix = encryptedText.substring(encryptedText.length() - actualTailPreserve);
        String middle = encryptedText.substring(headPreserve, encryptedText.length() - actualTailPreserve);
        
        if (middle.isEmpty()) {
            return encryptedText;
        }
        
        // 解密中间部分
        String decryptedMiddle = decryptMiddlePart(middle, tweak);
        
        // 组合结果
        return prefix + decryptedMiddle + suffix;
    }
    
    /**
     * 计算单字符加密的偏移量
     * 使用 key 和 tweak 的哈希值生成一个确定性的偏移
     */
    private int calculateShift(String tweak) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 将 key 和 tweak 结合后计算哈希
            digest.update(key);
            digest.update(tweak.getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest();
            
            // 使用前4个字节转为int
            int shift = ((hash[0] & 0xFF) << 24) | 
                       ((hash[1] & 0xFF) << 16) | 
                       ((hash[2] & 0xFF) << 8) | 
                       (hash[3] & 0xFF);
            
            // 取绝对值后对 radix 取模
            return Math.abs(shift) % charMapping.getRadix();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * 加密中间部分
     */
    private String encryptMiddlePart(String middle, String tweak) {
        // 1. 将字符串转换为索引数组，记录不在字典中的字符位置
        int[] plainIndices = new int[middle.length()];
        boolean[] shouldEncrypt = new boolean[middle.length()];
        int encryptableCount = 0;
        
        for (int i = 0; i < middle.length(); i++) {
            char c = middle.charAt(i);
            int index = charMapping.getCharIndex(c);
            if (index == -1) {
                // 字符不在字典中，原样保留
                plainIndices[i] = -1;
                shouldEncrypt[i] = false;
                logger.debug("Character '{}' (U+{}) not in dictionary, will be kept as-is", 
                    c, Integer.toHexString(c).toUpperCase());
            } else {
                plainIndices[i] = index;
                shouldEncrypt[i] = true;
                encryptableCount++;
            }
        }
        
        // 如果没有可加密的字符，直接返回原文
        if (encryptableCount == 0) {
            logger.debug("No encryptable characters in middle part, returning as-is");
            return middle;
        }
        
        // 对于单字符，使用基于密钥的模加法置换（Keyed Substitution）
        if (encryptableCount == 1) {
            int shift = calculateShift(tweak);
            logger.debug("Single character encryption using modular addition with shift: {}", shift);
            
            StringBuilder result = new StringBuilder(middle.length());
            for (int i = 0; i < middle.length(); i++) {
                if (shouldEncrypt[i]) {
                    int oldIndex = plainIndices[i];
                    int newIndex = (oldIndex + shift) % charMapping.getRadix();
                    char encryptedChar = charMapping.mapToEncryptedChar(newIndex);
                    result.append(encryptedChar);
                } else {
                    result.append(middle.charAt(i));
                }
            }
            return result.toString();
        }
        
        // FF1 算法处理2个及以上字符
        int[] toEncrypt = new int[encryptableCount];
        
        // 2. 提取可加密的字符索引
        int idx = 0;
        for (int i = 0; i < plainIndices.length; i++) {
            if (shouldEncrypt[i]) {
                toEncrypt[idx++] = plainIndices[i];
            }
        }
        
        // 3. 使用 FF1 算法加密
        int[] encrypted = ff1Encrypt(toEncrypt, tweak);
        
        // 4. 将加密后的索引映射回字符串，保留原样的字符
        StringBuilder result = new StringBuilder(middle.length());
        idx = 0;
        for (int i = 0; i < middle.length(); i++) {
            if (shouldEncrypt[i]) {
                char encryptedChar = charMapping.mapToEncryptedChar(encrypted[idx++]);
                result.append(encryptedChar);
            } else {
                // 原样保留
                result.append(middle.charAt(i));
            }
        }
        
        return result.toString();
    }
    
    /**
     * 解密中间部分
     */
    private String decryptMiddlePart(String encryptedMiddle, String tweak) {
        // 1. 识别哪些字符是加密字符，哪些是原样保留的字符
        int[] encryptedIndices = new int[encryptedMiddle.length()];
        boolean[] isEncrypted = new boolean[encryptedMiddle.length()];
        int encryptedCount = 0;
        
        for (int i = 0; i < encryptedMiddle.length(); i++) {
            char c = encryptedMiddle.charAt(i);
            try {
                // 尝试反向映射，如果成功说明是加密字符
                encryptedIndices[i] = charMapping.mapFromEncryptedChar(c);
                isEncrypted[i] = true;
                encryptedCount++;
            } catch (IllegalArgumentException e) {
                // 不是加密字符，可能是原样保留的字符
                int index = charMapping.getCharIndex(c);
                if (index != -1) {
                    // 在字典中但不是加密字符，可能在保留区
                    encryptedIndices[i] = index;
                    isEncrypted[i] = true;
                    encryptedCount++;
                } else {
                    // 不在字典中，是原样保留的字符
                    encryptedIndices[i] = -1;
                    isEncrypted[i] = false;
                    logger.debug("Character '{}' is not encrypted, keeping as-is", c);
                }
            }
        }
        
        // 如果没有加密字符，直接返回原文
        if (encryptedCount == 0) {
            return encryptedMiddle;
        }
        
        // 对于单字符，使用基于密钥的模减法置换（逆操作）
        if (encryptedCount == 1) {
            int shift = calculateShift(tweak);
            logger.debug("Single character decryption using modular subtraction with shift: {}", shift);
            
            StringBuilder result = new StringBuilder(encryptedMiddle.length());
            for (int i = 0; i < encryptedMiddle.length(); i++) {
                if (isEncrypted[i]) {
                    int encryptedIndex = encryptedIndices[i];
                    int originalIndex = (encryptedIndex - shift + charMapping.getRadix()) % charMapping.getRadix();
                    char originalChar = charMapping.getOriginalChar(originalIndex);
                    result.append(originalChar);
                } else {
                    result.append(encryptedMiddle.charAt(i));
                }
            }
            return result.toString();
        }
        
        // FF1 算法处理2个及以上字符
        
        // 2. 提取加密的索引
        int[] toDecrypt = new int[encryptedCount];
        int idx = 0;
        for (int i = 0; i < encryptedIndices.length; i++) {
            if (isEncrypted[i]) {
                toDecrypt[idx++] = encryptedIndices[i];
            }
        }
        
        // 3. 使用 FF1 算法解密
        int[] plainIndices = ff1Decrypt(toDecrypt, tweak);
        
        // 4. 将索引转换回原始字符，保留原样的字符
        StringBuilder result = new StringBuilder(encryptedMiddle.length());
        idx = 0;
        for (int i = 0; i < encryptedMiddle.length(); i++) {
            if (isEncrypted[i]) {
                char originalChar = charMapping.getOriginalChar(plainIndices[idx++]);
                result.append(originalChar);
            } else {
                // 保留非加密字符
                result.append(encryptedMiddle.charAt(i));
            }
        }
        
        return result.toString();
    }
    
    /**
     * FF1 加密
     */
    private int[] ff1Encrypt(int[] plainIndices, String tweak) {
        byte[] tweakBytes = tweak.getBytes(StandardCharsets.UTF_8);
        
        // 对于宽基数(radix > 256)，需要使用2字节编码
        // 且数据长度必须是偶数字节
        int dataLength = plainIndices.length * 2;
        byte[] plainBytes = new byte[dataLength];
        
        // 将 int[] 转换为 byte[]（大端序，2字节per值）
        for (int i = 0; i < plainIndices.length; i++) {
            plainBytes[i * 2] = (byte) (plainIndices[i] >>> 8);
            plainBytes[i * 2 + 1] = (byte) (plainIndices[i] & 0xFF);
        }
        
        // 创建 FPE 参数
        FPEParameters params = new FPEParameters(
            new KeyParameter(key),
            charMapping.getRadix(),
            tweakBytes
        );
        
        // 初始化加密模式
        ff1Engine.init(true, params);
        
        // 执行加密
        byte[] resultBytes = new byte[dataLength];
        ff1Engine.processBlock(plainBytes, 0, dataLength, resultBytes, 0);
        
        // 转换 byte[] 回 int[]（大端序）
        int[] result = new int[plainIndices.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = ((resultBytes[i * 2] & 0xFF) << 8) | (resultBytes[i * 2 + 1] & 0xFF);
        }
        
        return result;
    }
    
    /**
     * FF1 解密
     */
    private int[] ff1Decrypt(int[] encryptedIndices, String tweak) {
        byte[] tweakBytes = tweak.getBytes(StandardCharsets.UTF_8);
        
        // 对于宽基数(radix > 256)，需要使用2字节编码
        int dataLength = encryptedIndices.length * 2;
        byte[] encryptedBytes = new byte[dataLength];
        
        // 将 int[] 转换为 byte[]（大端序，2字节per值）
        for (int i = 0; i < encryptedIndices.length; i++) {
            encryptedBytes[i * 2] = (byte) (encryptedIndices[i] >>> 8);
            encryptedBytes[i * 2 + 1] = (byte) (encryptedIndices[i] & 0xFF);
        }
        
        // 创建 FPE 参数
        FPEParameters params = new FPEParameters(
            new KeyParameter(key),
            charMapping.getRadix(),
            tweakBytes
        );
        
        // 初始化解密模式
        ff1Engine.init(false, params);
        
        // 执行解密
        byte[] resultBytes = new byte[dataLength];
        ff1Engine.processBlock(encryptedBytes, 0, dataLength, resultBytes, 0);
        
        // 转换 byte[] 回 int[]（大端序）
        int[] result = new int[encryptedIndices.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = ((resultBytes[i * 2] & 0xFF) << 8) | (resultBytes[i * 2 + 1] & 0xFF);
        }
        
        return result;
    }
    
    /**
     * 从密码字符串派生 AES 密钥
     * 使用 PBKDF2-HMAC-SHA256 算法
     * 
     * @param password 密码字符串
     * @param keyLength 密钥长度（16、24 或 32 字节）
     * @return 派生的密钥
     */
    private static byte[] deriveKeyFromPassword(String password, int keyLength) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        if (keyLength != 16 && keyLength != 24 && keyLength != 32) {
            throw new IllegalArgumentException(
                "Key length must be 16, 24, or 32 bytes (128, 192, or 256 bits)");
        }
        
        try {
            // 使用固定的盐值（在实际应用中，可以根据应用名称或用户ID生成）
            // 注意：这里使用固定盐是为了确保相同密码总是生成相同密钥
            byte[] salt = "UniMaskFPE2026".getBytes(StandardCharsets.UTF_8);
            
            // PBKDF2 参数：
            // - 迭代次数：100000（OWASP 推荐最小值）
            // - 密钥长度：以位为单位
            int iterations = 100000;
            int keyLengthInBits = keyLength * 8;
            
            KeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                iterations,
                keyLengthInBits
            );
            
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] derivedKey = factory.generateSecret(spec).getEncoded();
            
            logger.debug("Derived {}-bit key from password using PBKDF2", keyLengthInBits);
            return derivedKey;
            
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Failed to derive key from password", e);
        }
    }
    
    /**
     * 从密码字符串和自定义盐值派生密钥
     * 允许用户提供自己的盐值以增加安全性
     * 
     * @param password 密码字符串
     * @param salt 盐值（建议至少16字节）
     * @param keyLength 密钥长度（16、24 或 32 字节）
     * @return 派生的密钥
     */
    public static byte[] deriveKeyFromPassword(String password, byte[] salt, int keyLength) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        if (salt == null || salt.length < 8) {
            throw new IllegalArgumentException("Salt must be at least 8 bytes");
        }
        
        if (keyLength != 16 && keyLength != 24 && keyLength != 32) {
            throw new IllegalArgumentException(
                "Key length must be 16, 24, or 32 bytes (128, 192, or 256 bits)");
        }
        
        try {
            int iterations = 100000;
            int keyLengthInBits = keyLength * 8;
            
            KeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                iterations,
                keyLengthInBits
            );
            
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
            
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Failed to derive key from password", e);
        }
    }
    
    /**
     * 获取当前使用的密钥（用于持久化）
     * 注意：不建议直接暴露密钥，仅用于特殊场景
     */
    public byte[] getKey() {
        return Arrays.copyOf(key, key.length);
    }
}
