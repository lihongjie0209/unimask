
## 🤖 提示词：Java 格式保留加密（FPE）库开发专家

### 📋 角色定位

你是一位精通 Java 安全开发和密码学的专家。你的任务是编写一个名为 `ChineseFPEService` 的 Java 类库。该库使用  **FF1 算法** （基于 Bouncy Castle 库实现）对包含汉字、英文和标点的文本进行格式保留加密，并采用特殊的  **3 字节 Unicode 映射方案** 。

### 🛠 核心算法与逻辑要求

1. **加密引擎** ：使用 NIST SP 800-38G 标准的  **FF1 算法** 。底层依赖 `org.bouncycastle.crypto.fpe.FF1Engine`。
2. **字符集 (Alphabet) 规划** （总 Radix = 8500）：

* **索引 0 - 6399** ：常用汉字（6100个）+ ASCII（数字、大小写英文）+ 常用标点。映射至 **BMP PUA** 区块 (`U+E000` - `U+F8FF`)。
* **索引 6400 - 8499** ：罕见汉字。映射至 **罕用韩文音节区** (`U+CF70` - `U+D7A3`)。

1. **映射转换要求** ：

* 需实现一个内部方法 `getCharIndex(char c)`，将原始字符映射为 0-8499 的索引。
* 需实现一个内部方法 `mapToEncryptedChar(int index)`，根据上述规则将加密后的索引转为对应的 3 字节 Unicode 字符。

### 接口定义 (Interface)

你编写的类需包含以下两个核心接口：

**Java**

```
/**
 * 加密接口
 * @param headPreserve  开头保留不加密的字符数
 * @param tailPreserve  末尾保留不加密的字符数
 * @param tweak         扰码（字符串格式，内部需转为字节数组）
 * @param plaintext     待加密的明文
 * @return 密文（保留开头结尾，中间部分映射为 PUA/韩文 字符）
 */
public String encrypt(int headPreserve, int tailPreserve, String tweak, String plaintext);

/**
 * 解密接口
 * @param encryptedText 包含保留部分的密文
 * @param tweak         必须与加密时一致的扰码
 * @return 原始明文
 */
public String decrypt(String encryptedText, String tweak);
```

### 💡 关键实现细节指南

1. **字典初始化** ：在构造函数中预加载 8500 个字符的 `String`（或数组），并建立 `Map<Character, Integer>` 以提高查找速度。字符顺序必须固定（建议：常用汉字按 Unicode 升序，罕见汉字按 Unicode 升序）。
2. **部分加密逻辑** ：

* 在 `encrypt` 中，将字符串拆分为 `prefix` + `middle` + `suffix`。
* 只对 `middle` 部分的字符序列进行 FF1 加密。
* 解密时需准确识别哪些部分是加密区，哪些是保留区。

1. **Tweak 处理** ：将传入的 String 类型的 `tweak` 转换为 `byte[]`（使用 UTF-8）。
2. **异常处理** ：若明文字符不在 8500 字典内，请定义处理策略（如原样保留或抛出异常）。
