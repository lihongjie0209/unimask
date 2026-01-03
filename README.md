# UniMask - 中文格式保留加密

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/lihongjie0209/unimask)
[![Java Version](https://img.shields.io/badge/Java-8%2B-blue)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

UniMask 是一个专门为中文设计的格式保留加密（Format-Preserving Encryption, FPE）库。它能够对中文文本进行加密，同时保持加密后文本的长度、格式和可读性，特别适用于姓名、地址等敏感信息的脱敏处理。

## ✨ 核心特性

### 🔐 安全性
- **PBKDF2-HMAC-SHA256** 密钥派生：从密码安全派生加密密钥
- **100,000 次迭代**：符合 OWASP 安全标准，有效防止暴力破解
- **FF1 加密算法**：NIST 标准的格式保留加密算法（2+ 字符）
- **SHA-256 哈希**：单字符加密使用密钥哈希进行偏移计算

### 📝 智能处理
- **字符级加密**：支持单字符到任意长度的文本加密
- **头部优先保留**：当保留长度不足时，优先保留文本头部
- **自动字符映射**：8,410 个中文字符映射到 PUA 区和韩文区
- **可逆加密**：加密后的文本可以完全解密回原文

### 🎯 实用功能
- **灵活保留长度**：支持自定义明文保留字符数
- **Tweak 支持**：同一文本在不同上下文中产生不同密文
- **多密钥长度**：支持 AES-128/192/256

## 🎯 应用场景

- **数据脱敏**：姓名、地址、电话号码等敏感信息脱敏
- **隐私保护**：保护用户隐私的同时保持数据格式
- **安全传输**：数据传输过程中的格式保留加密
- **数据库加密**：不改变字段长度的数据库字段加密
- **日志脱敏**：日志中敏感信息的自动脱敏处理

## 📦 依赖

```xml
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk15on</artifactId>
    <version>1.70</version>
</dependency>
```

## 🚀 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>cn.lihongjie</groupId>
    <artifactId>unimask</artifactId>
    <version>1.0.1</version>
</dependency>
```

### 基本使用

```java
import cn.lihongjie.unimask.ChineseFPEService;

// 1. 创建服务实例（使用密码）
ChineseFPEService service = new ChineseFPEService("my-secret-password");

// 2. 加密姓名（保留姓氏）
String name = "张伟明";
String encrypted = service.encrypt(name, "user-context", 1);
System.out.println("原文: " + name);        // 输出: 张伟明
System.out.println("密文: " + encrypted);   // 输出: 张××

// 3. 解密
String decrypted = service.decrypt(encrypted, "user-context", 1);
System.out.println("解密: " + decrypted);   // 输出: 张伟明
```

## 📖 详细示例

### 1. 姓名脱敏

```java
ChineseFPEService service = new ChineseFPEService("password-123");

// 保留姓氏，加密名字
String name1 = "李小明";
String masked1 = service.encrypt(name1, "context-1", 1);  // 李××

// 完全加密
String name2 = "王大锤";
String masked2 = service.encrypt(name2, "context-2", 0);  // 完全加密

// 解密恢复
String original1 = service.decrypt(masked1, "context-1", 1);  // 李小明
String original2 = service.decrypt(masked2, "context-2", 0);  // 王大锤
```

### 2. 手机号脱敏

```java
// 手机号当作文本处理
String phone = "13812345678";
String maskedPhone = service.encrypt(phone, "phone", 3);  // 138××××××××

// 解密
String originalPhone = service.decrypt(maskedPhone, "phone", 3);  // 13812345678
```

### 3. 地址脱敏

```java
String address = "北京市朝阳区建国路1号";

// 保留省市，加密详细地址
String maskedAddress = service.encrypt(address, "address", 5);
// 输出: 北京市朝阳××××××

// 解密
String originalAddress = service.decrypt(maskedAddress, "address", 5);
// 输出: 北京市朝阳区建国路1号
```

### 4. 单字符处理

```java
// 单字符自动使用 SHA-256 哈希偏移加密
String singleChar = "中";
String encrypted = service.encrypt(singleChar, "tweak-1", 0);
String decrypted = service.decrypt(encrypted, "tweak-1", 0);

System.out.println("原文: " + singleChar);
System.out.println("密文: " + encrypted);  // 加密后的字符（PUA 区或韩文区）
System.out.println("解密: " + decrypted);  // 中
```

### 5. 使用不同密钥长度

```java
// AES-128 (默认)
ChineseFPEService service128 = new ChineseFPEService("password", 16);

// AES-192
ChineseFPEService service192 = new ChineseFPEService("password", 24);

// AES-256
ChineseFPEService service256 = new ChineseFPEService("password", 32);
```

### 6. 自定义盐（多租户场景）

```java
import java.nio.charset.StandardCharsets;

// 为不同用户/租户生成不同的密钥
String password = "shared-password";
byte[] userSalt = "user-12345".getBytes(StandardCharsets.UTF_8);
byte[] key = ChineseFPEService.deriveKeyFromPassword(password, userSalt, 16);

// 使用派生的密钥创建服务
ChineseFPEService service = new ChineseFPEService(key);
```

### 7. Tweak 的作用

```java
ChineseFPEService service = new ChineseFPEService("password");

String text = "张伟明";

// 使用不同的 tweak，相同文本产生不同密文
String encrypted1 = service.encrypt(text, "context-A", 1);
String encrypted2 = service.encrypt(text, "context-B", 1);

System.out.println("Tweak A: " + encrypted1);  // 张××
System.out.println("Tweak B: " + encrypted2);  // 张△△ (不同的加密结果)

// 必须使用相同的 tweak 才能解密
String decrypted1 = service.decrypt(encrypted1, "context-A", 1);  // ✓ 张伟明
String decrypted2 = service.decrypt(encrypted1, "context-B", 1);  // ✗ 错误结果
```

### 8. 批量处理

```java
ChineseFPEService service = new ChineseFPEService("password");

List<String> names = Arrays.asList(
    "张三", "李四", "王五", "赵六", "孙七"
);

// 批量加密（保留姓氏）
List<String> encrypted = names.stream()
    .map(name -> service.encrypt(name, "batch", 1))
    .collect(Collectors.toList());

System.out.println("加密结果: " + encrypted);
// 输出: [张×, 李×, 王×, 赵×, 孙×]

// 批量解密
List<String> decrypted = encrypted.stream()
    .map(name -> service.decrypt(name, "batch", 1))
    .collect(Collectors.toList());

System.out.println("解密结果: " + decrypted);
// 输出: [张三, 李四, 王五, 赵六, 孙七]
```

## 🏗️ 技术实现

### 加密流程

```
输入文本 → 分段处理 → 字符映射 → 加密算法 → 反向映射 → 输出密文
    ↓           ↓           ↓          ↓          ↓         ↓
 张伟明    [张][伟明]   [1234,5678]  FF1/SHA-256 [9012,3456] 张××
(明文)   (头部/中间)   (索引序列)   (加密运算)  (密文索引)  (密文)
```

### 字符映射策略

- **Level 1 常用字**（3,500 字）→ PUA 区（U+E000-U+F8FF）
- **Level 2 次常用字**（3,003 字）→ 部分 PUA + 韩文区
- **Level 3 罕见字**（1,798 字）→ 韩文音节区（U+CF70+）
- **总计**：8,410 个中文字符映射

### 加密算法选择

| 文本长度 | 加密算法 | 说明 |
|---------|---------|------|
| 1 字符 | SHA-256 哈希偏移 | `shift = abs(SHA256(key\|\|tweak)) % radix` |
| 2+ 字符 | FF1 (NIST) | 标准格式保留加密算法 |

### 密钥派生（PBKDF2）

```java
算法: PBKDF2WithHmacSHA256
迭代次数: 100,000
盐: "UniMaskFPE2026" (固定) 或自定义
密钥长度: 16/24/32 字节 (AES-128/192/256)
```

## 🔧 API 文档

### 构造函数

```java
// 使用密码创建服务（默认 AES-128）
public ChineseFPEService(String password)

// 使用密码 + 指定密钥长度
public ChineseFPEService(String password, int keyLength)

// 使用预生成的密钥
public ChineseFPEService(byte[] key)
```

### 核心方法

```java
/**
 * 加密文本
 * @param plaintext 明文
 * @param tweak 调整参数（上下文标识）
 * @param preserveLength 明文保留长度（从头部开始）
 * @return 密文
 */
public String encrypt(String plaintext, String tweak, int preserveLength)

/**
 * 解密文本
 * @param ciphertext 密文
 * @param tweak 调整参数（必须与加密时相同）
 * @param preserveLength 明文保留长度（必须与加密时相同）
 * @return 明文
 */
public String decrypt(String ciphertext, String tweak, int preserveLength)

/**
 * 从密码派生密钥（自定义盐）
 * @param password 密码
 * @param salt 盐值
 * @param keyLength 密钥长度（字节）
 * @return 派生的密钥
 */
public static byte[] deriveKeyFromPassword(String password, byte[] salt, int keyLength)
```

## 🧪 测试

项目包含完整的测试套件：

- **单元测试**：17 个测试用例
- **字符映射测试**：6 个测试用例
- **单字符测试**：2 个测试用例
- **属性测试**（jqwik）：7 个属性测试

运行测试：

```bash
mvn test
```

测试覆盖：
- ✅ 基本加密/解密
- ✅ 单字符加密
- ✅ 保留长度处理
- ✅ Tweak 变化测试
- ✅ 字符映射区域验证
- ✅ PBKDF2 密钥派生
- ✅ 多密钥长度支持
- ✅ 可逆性验证
- ✅ 单射性验证

测试结果示例：

```
[INFO] Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO]
[INFO] Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] BUILD SUCCESS
```

## 📊 性能建议

1. **服务实例复用**：创建 `ChineseFPEService` 实例有 PBKDF2 开销，建议复用实例
2. **批量处理**：使用流式处理批量数据
3. **并行处理**：线程安全，可并行调用加密/解密方法

```java
// ✓ 推荐：复用实例
ChineseFPEService service = new ChineseFPEService("password");
for (String text : texts) {
    String encrypted = service.encrypt(text, "context", 1);
}

// ✗ 不推荐：每次创建新实例
for (String text : texts) {
    ChineseFPEService service = new ChineseFPEService("password");  // 慢！
    String encrypted = service.encrypt(text, "context", 1);
}
```

## 🔐 安全注意事项

1. **密码强度**：使用强密码（至少 12 位，包含大小写字母、数字和特殊字符）
2. **盐值管理**：多租户场景建议为每个租户使用不同的盐
3. **Tweak 使用**：不同场景使用不同的 tweak，增加安全性
4. **密钥存储**：妥善保管密码和派生的密钥
5. **传输安全**：加密文本在传输时仍需使用 HTTPS/TLS

## 🛠️ 依赖

```xml
<!-- Bouncy Castle: FF1 加密算法 -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk15on</artifactId>
    <version>1.70</version>
</dependency>

<!-- SLF4J: 日志 -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.16</version>
</dependency>
```

## 📝 注意事项

1. **密钥管理**：使用密码方式时，相同密码总是产生相同密钥（确定性）
2. **Tweak 一致性**：加密和解密时必须使用相同的 tweak 参数
3. **保留长度一致性**：加密和解密时 preserveLength 参数必须一致
4. **字符集限制**：仅支持 8,410 个预定义的中文字符
5. **实例复用**：建议复用服务实例以提升性能

## 🎨 字体脱敏展示

UniMask 提供了专用字体文件，可以在前端自动将加密区域显示为星号 `*`，实现优雅的视觉脱敏效果。

### 📥 字体文件

项目包含三种格式的字体文件（位于 `fonts/` 目录）：

| 格式 | 文件大小 | 适用场景 | 浏览器支持 |
|------|---------|---------|-----------|
| **WOFF2** | 516 字节 | Web 推荐（最小） | Chrome 36+, Firefox 39+, Safari 10+ |
| **WOFF** | 792 字节 | Web 通用 | 所有现代浏览器 |
| **OTF** | 17.5 KB | 桌面应用 | 系统级安装 |

### 🌐 前端使用

#### 方式一：全局引入

在全局 CSS 文件中引入字体，适用于整个网站的加密文本展示：

```css
/* global.css 或 app.css */
@font-face {
    font-family: 'UniMask';
    src: url('/fonts/UniMask.woff2') format('woff2'),
         url('/fonts/UniMask.woff') format('woff'),
         url('/fonts/UniMask.otf') format('opentype');
    font-weight: normal;
    font-style: normal;
    font-display: swap;
}

/* 为所有加密文本应用字体 */
.encrypted-text {
    font-family: 'UniMask', 'Microsoft YaHei', sans-serif;
    letter-spacing: 2px;
}
```

**HTML 使用：**

```html
<!-- 后端返回的加密文本 -->
<div class="encrypted-text">张\uE123\uE456</div>
<!-- 浏览器显示为: 张** -->

<table>
  <tr>
    <td class="encrypted-text">李\uE234\uE567</td>
    <td class="encrypted-text">王\uE345</td>
  </tr>
</table>
```

#### 方式二：指定表单/组件引入

只在特定表单或组件中使用，避免影响其他区域：

```css
/* user-form.css */
@font-face {
    font-family: 'UniMask';
    src: url('/fonts/UniMask.woff2') format('woff2');
    font-weight: normal;
    font-style: normal;
}

/* 只为用户表单中的敏感字段应用 */
#userForm .sensitive-field {
    font-family: 'UniMask', monospace;
}

#userForm .encrypted-name,
#userForm .encrypted-phone {
    font-family: 'UniMask', sans-serif;
    letter-spacing: 3px;
}
```

**React 组件示例：**

```jsx
import React from 'react';
import './UserForm.css';

function UserForm({ user }) {
    return (
        <div id="userForm">
            <div className="form-group">
                <label>姓名：</label>
                <span className="encrypted-name">{user.name}</span>
                {/* 显示: 张** */}
            </div>
            <div className="form-group">
                <label>手机：</label>
                <span className="encrypted-phone">{user.phone}</span>
                {/* 显示: 138****1234 */}
            </div>
        </div>
    );
}
```

**Vue 组件示例：**

```vue
<template>
  <div id="userForm">
    <div class="form-group">
      <label>姓名：</label>
      <span class="encrypted-name">{{ userName }}</span>
      <!-- 显示: 李** -->
    </div>
  </div>
</template>

<style scoped>
@font-face {
    font-family: 'UniMask';
    src: url('@/assets/fonts/UniMask.woff2') format('woff2');
}

.encrypted-name {
    font-family: 'UniMask', sans-serif;
    letter-spacing: 2px;
}
</style>

<script>
export default {
    data() {
        return {
            userName: '李\uE123\uE456'  // 后端返回的加密数据
        };
    }
};
</script>
```

### 🔗 完整集成示例

**后端 Java 代码：**

```java
import cn.lihongjie.unimask.ChineseFPEService;

@RestController
public class UserController {
    private final ChineseFPEService fpeService = new ChineseFPEService("secret-key");
    
    @GetMapping("/api/user/{id}")
    public Map<String, String> getUser(@PathVariable String id) {
        User user = userService.findById(id);
        
        // 加密姓名，保留姓氏
        String encryptedName = fpeService.encrypt(user.getName(), "user-" + id, 1);
        
        Map<String, String> result = new HashMap<>();
        result.put("name", encryptedName);  // 例如: "张\uE123\uE456"
        result.put("email", user.getEmail());
        return result;
    }
}
```

**前端 JavaScript 代码：**

```javascript
// 获取用户数据并展示
fetch('/api/user/123')
    .then(response => response.json())
    .then(data => {
        // 直接设置到 DOM，UniMask 字体会自动将 \uE123\uE456 显示为 **
        document.querySelector('.user-name').textContent = data.name;
        // 浏览器显示: 张**
    });
```

### 🎯 工作原理

1. **后端加密**：Java 库将中文字符加密为 PUA 区（U+E000-U+F8FF）或韩文区（U+CF70-U+D7A3）的 Unicode 字符
2. **数据传输**：加密后的 Unicode 字符随 JSON 传输到前端
3. **字体渲染**：前端应用 UniMask 字体后，这些 Unicode 字符自动渲染为星号 `*`
4. **用户体验**：用户看到 "张**" 而不是乱码，实现优雅的脱敏效果

### 📊 字体演示

在浏览器中打开 `fonts/demo.html` 可以查看完整的字体效果演示，包括：
- 原始文本 vs 加密效果对比
- 三种字体格式的独立演示
- 互动输入测试
- 使用方法说明

### 💡 最佳实践

1. **优先使用 WOFF2**：文件最小（516 字节），加载速度最快
2. **添加 font-display: swap**：避免字体加载时的文本闪烁
3. **设置 letter-spacing**：增加字间距，提升可读性
4. **提供降级字体**：`font-family: 'UniMask', sans-serif;` 确保未加载时仍可显示
5. **按需加载**：只在需要脱敏的组件中引入字体，减少全局影响

## �️ JavaScript 工具库

UniMask 提供了前端 JavaScript 工具库，用于检测和处理加密文本，特别适用于数据导出场景。

### 📥 引入方式

**浏览器直接引入：**

```html
<script src="js/unimask-utils.js"></script>
<script>
    const text = "张\uE123\uE456";
    console.log(UniMaskUtils.isEncrypted(text)); // true
    console.log(UniMaskUtils.replaceEncryptedWithAsterisk(text)); // "张**"
</script>
```

**ES6 模块：**

```javascript
import { UniMaskUtils } from './js/unimask-utils.js';

const result = UniMaskUtils.replaceEncryptedWithAsterisk("张\uE123\uE456");
console.log(result); // "张**"
```

**Node.js：**

```javascript
const UniMaskUtils = require('./js/unimask-utils.js');
```

### 🎯 核心功能

#### 1. 加密检测

```javascript
// 检测文本是否包含加密字符
UniMaskUtils.isEncrypted("张伟明");          // false
UniMaskUtils.isEncrypted("张\uE123\uE456");  // true

// 统计加密字符数量
UniMaskUtils.countEncryptedChars("张\uE123\uE456");  // 2
```

#### 2. 字符替换（用于数据导出）

```javascript
// 将加密字符替换为星号
UniMaskUtils.replaceEncryptedWithAsterisk("张\uE123\uE456");
// 返回: "张**"

// 自定义替换字符
UniMaskUtils.replaceEncryptedWithAsterisk("张\uE123\uE456", "●");
// 返回: "张●●"
```

#### 3. 批量处理对象

```javascript
const user = {
    name: "张\uE123\uE456",
    phone: "138\uE234\uE345\uE456\uE567",
    email: "test@example.com"
};

// 处理指定字段
const result = UniMaskUtils.replaceFieldsInObject(user, ['name', 'phone']);
// {
//     name: "张**",
//     phone: "138****",
//     email: "test@example.com"
// }
```

#### 4. 批量处理数组（数据导出）

```javascript
const users = [
    { id: 1, name: "张\uE123\uE456", age: 25 },
    { id: 2, name: "李\uE234\uE567", age: 30 }
];

// 批量处理所有对象
const exported = UniMaskUtils.replaceFieldsInArray(users, ['name']);
// [
//     { id: 1, name: "张**", age: 25 },
//     { id: 2, name: "李**", age: 30 }
// ]

// 可直接导出到 CSV、Excel
exportToCSV(exported); // 导出的文件中显示为 "张**" 而不是乱码
```

#### 5. 文本分析

```javascript
const analysis = UniMaskUtils.analyzeText("张\uE123\uE456");
console.log(analysis);
// {
//     isEncrypted: true,
//     totalChars: 3,
//     encryptedChars: 2,
//     plainChars: 1,
//     encryptionRate: "66.67%",
//     positions: [
//         { index: 1, char: "\uE123", codePoint: "0xE123" },
//         { index: 2, char: "\uE456", codePoint: "0xE456" }
//     ]
// }
```

#### 6. 导出格式转换

```javascript
// 转换为可导出格式（用于 CSV、Excel）
UniMaskUtils.toExportFormat("张\uE123\uE456", { 
    preserveLength: true,  // 保持长度
    replacement: '*' 
});
// 返回: "张**"
```

### 📊 完整导出示例

```javascript
// 完整的数据导出流程
function exportUserData() {
    // 1. 从后端获取数据（包含加密字符）
    const users = await fetchUsers();
    
    // 2. 批量替换加密字符为星号
    const exportData = UniMaskUtils.replaceFieldsInArray(
        users, 
        ['name', 'phone', 'idCard', 'address']
    );
    
    // 3. 转换为 CSV 格式
    const csv = convertToCSV(exportData);
    
    // 4. 下载文件
    downloadFile(csv, 'users.csv');
    // 导出的 CSV 文件中显示为 "张**" 而不是乱码
}
```

### 🎨 完整集成示例

```javascript
// React 组件示例
import React, { useState } from 'react';
import { UniMaskUtils } from './js/unimask-utils.js';

function UserList({ users }) {
    const [showMasked, setShowMasked] = useState(true);
    
    const displayUsers = showMasked 
        ? UniMaskUtils.replaceFieldsInArray(users, ['name', 'phone'])
        : users;
    
    const handleExport = () => {
        // 导出时替换为星号
        const exportData = UniMaskUtils.replaceFieldsInArray(
            users, 
            ['name', 'phone', 'idCard']
        );
        exportToCSV(exportData);
    };
    
    return (
        <div>
            <button onClick={() => setShowMasked(!showMasked)}>
                {showMasked ? '显示原文' : '显示脱敏'}
            </button>
            <button onClick={handleExport}>导出 CSV</button>
            
            <table>
                <tbody>
                    {displayUsers.map(user => (
                        <tr key={user.id}>
                            <td>{user.name}</td>
                            <td>{user.phone}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
```

### 📚 更多信息

- **完整 API 文档**：查看 [js/README.md](js/README.md)
- **互动演示**：打开 [js/demo.html](js/demo.html) 查看所有功能演示
- **使用场景**：数据导出、打印预览、CSV/Excel 生成、数据分析

### 🔑 关键特性

- ✅ **零依赖**：纯 JavaScript 实现
- ✅ **多环境**：支持浏览器、ES6、Node.js
- ✅ **高性能**：优化的批量处理算法
- ✅ **类型安全**：完善的参数校验
- ✅ **易集成**：简洁的 API 设计
## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request
## �📮 联系方式

- 作者：Li Hongjie
- 项目地址：https://github.com/lihongjie0209/unimask

## 🙏 致谢

- [Bouncy Castle](https://www.bouncycastle.org/) - 提供 FF1 加密算法实现
- [jqwik](https://jqwik.net/) - 属性测试框架
- NIST - FF1 格式保留加密标准

---

**⭐️ Star 本项目，如果它对你有帮助！**
