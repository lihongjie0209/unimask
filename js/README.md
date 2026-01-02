# UniMask JavaScript 工具库

前端 JavaScript 工具类，用于检测和处理加密文本。

## 📦 功能特性

- ✅ **加密检测**：判断文本是否包含加密字符
- ✅ **字符替换**：将加密字符替换为星号，方便导出
- ✅ **批量处理**：支持对象和数组的批量处理
- ✅ **文本分析**：提供详细的加密文本分析功能
- ✅ **格式转换**：转换为可导出格式（CSV、Excel 等）

## 🚀 快速开始

### 浏览器引入

```html
<script src="js/unimask-utils.js"></script>
<script>
    // 检测文本是否加密
    const text = "张\uE123\uE456";
    console.log(UniMaskUtils.isEncrypted(text)); // true
    
    // 替换为星号
    const result = UniMaskUtils.replaceEncryptedWithAsterisk(text);
    console.log(result); // "张**"
</script>
```

### ES6 模块

```javascript
import { UniMaskUtils } from './unimask-utils.js';

const text = "张\uE123\uE456";
const result = UniMaskUtils.replaceEncryptedWithAsterisk(text);
console.log(result); // "张**"
```

### Node.js

```javascript
const UniMaskUtils = require('./unimask-utils.js');

const text = "张\uE123\uE456";
const result = UniMaskUtils.replaceEncryptedWithAsterisk(text);
console.log(result); // "张**"
```

## 📖 API 文档

### 核心方法

#### `isEncrypted(text)`

检测文本是否包含加密字符。

```javascript
UniMaskUtils.isEncrypted("张伟明");          // false
UniMaskUtils.isEncrypted("张\uE123\uE456");  // true
```

**参数：**
- `text` (string): 待检测文本

**返回：**
- (boolean): 是否包含加密字符

---

#### `replaceEncryptedWithAsterisk(text, replacement)`

将加密字符替换为指定字符（默认为星号）。

```javascript
// 默认替换为星号
UniMaskUtils.replaceEncryptedWithAsterisk("张\uE123\uE456");
// 返回: "张**"

// 自定义替换字符
UniMaskUtils.replaceEncryptedWithAsterisk("张\uE123\uE456", "●");
// 返回: "张●●"
```

**参数：**
- `text` (string): 原始文本
- `replacement` (string, 可选): 替换字符，默认为 `*`

**返回：**
- (string): 替换后的文本

---

#### `countEncryptedChars(text)`

统计文本中加密字符的数量。

```javascript
UniMaskUtils.countEncryptedChars("张\uE123\uE456");  // 2
```

**参数：**
- `text` (string): 待统计文本

**返回：**
- (number): 加密字符数量

---

### 批量处理方法

#### `replaceFieldsInObject(obj, fields, replacement)`

批量处理对象中的指定字段。

```javascript
const user = {
    name: "张\uE123\uE456",
    phone: "138\uE234\uE345\uE456\uE567",
    email: "test@example.com"
};

const result = UniMaskUtils.replaceFieldsInObject(user, ['name', 'phone']);
console.log(result);
// {
//     name: "张**",
//     phone: "138****",
//     email: "test@example.com"
// }
```

**参数：**
- `obj` (Object): 待处理对象
- `fields` (string[]): 需要处理的字段名数组
- `replacement` (string, 可选): 替换字符，默认为 `*`

**返回：**
- (Object): 处理后的新对象

---

#### `replaceFieldsInArray(array, fields, replacement)`

批量处理数组中所有对象的指定字段。

```javascript
const users = [
    { id: 1, name: "张\uE123\uE456", age: 25 },
    { id: 2, name: "李\uE234\uE567", age: 30 }
];

const result = UniMaskUtils.replaceFieldsInArray(users, ['name']);
console.log(result);
// [
//     { id: 1, name: "张**", age: 25 },
//     { id: 2, name: "李**", age: 30 }
// ]
```

**参数：**
- `array` (Array): 对象数组
- `fields` (string[]): 需要处理的字段名数组
- `replacement` (string, 可选): 替换字符，默认为 `*`

**返回：**
- (Array): 处理后的新数组

---

### 分析方法

#### `analyzeText(text)`

分析文本的加密情况。

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

**参数：**
- `text` (string): 待分析文本

**返回：**
- (Object): 分析结果对象

---

#### `getEncryptedPositions(text)`

获取文本中加密字符的位置信息。

```javascript
const positions = UniMaskUtils.getEncryptedPositions("张\uE123\uE456");
console.log(positions);
// [
//     { index: 1, char: "\uE123", codePoint: "0xE123" },
//     { index: 2, char: "\uE456", codePoint: "0xE456" }
// ]
```

**参数：**
- `text` (string): 待分析文本

**返回：**
- (Array): 位置信息数组

---

### 导出方法

#### `toExportFormat(text, options)`

将加密文本转换为可导出的格式（用于 CSV、Excel 等）。

```javascript
// 保持长度
UniMaskUtils.toExportFormat("张\uE123\uE456", { preserveLength: true });
// 返回: "张**"

// 不保持长度（连续加密字符合并）
UniMaskUtils.toExportFormat("张\uE123\uE456", { preserveLength: false });
// 返回: "张**"

// 自定义替换字符
UniMaskUtils.toExportFormat("张\uE123\uE456", { 
    replacement: '●',
    preserveLength: true 
});
// 返回: "张●●"
```

**参数：**
- `text` (string): 原始加密文本
- `options` (Object, 可选): 配置选项
  - `replacement` (string): 替换字符，默认 `*`
  - `preserveLength` (boolean): 是否保持长度一致，默认 `true`

**返回：**
- (string): 可导出的文本

---

### 验证方法

#### `validate(text)`

验证文本是否为合法的加密文本格式。

```javascript
// 有效的加密文本
UniMaskUtils.validate("张\uE123\uE456");
// { valid: true, analysis: {...} }

// 纯明文
UniMaskUtils.validate("张伟明");
// { valid: false, error: "文本不包含加密字符" }

// 全部加密
UniMaskUtils.validate("\uE123\uE456\uE789");
// { valid: false, error: "文本全部加密，可能不是有效的部分加密文本" }
```

**参数：**
- `text` (string): 待验证文本

**返回：**
- (Object): 验证结果
  - `valid` (boolean): 是否有效
  - `error` (string): 错误信息（如果无效）
  - `analysis` (Object): 分析结果（如果有效）

---

## 🎯 使用场景

### 1. 数据导出

```javascript
// 导出到 CSV
function exportToCSV(users) {
    const processedUsers = UniMaskUtils.replaceFieldsInArray(
        users, 
        ['name', 'phone', 'address']
    );
    
    // 转换为 CSV 格式
    const csv = convertToCSV(processedUsers);
    downloadFile(csv, 'users.csv');
}
```

### 2. 表格展示

```javascript
// React 示例
function UserTable({ users }) {
    const [showPlainText, setShowPlainText] = useState(false);
    
    const displayUsers = showPlainText 
        ? users 
        : UniMaskUtils.replaceFieldsInArray(users, ['name', 'phone']);
    
    return (
        <div>
            <button onClick={() => setShowPlainText(!showPlainText)}>
                {showPlainText ? '显示加密' : '显示明文'}
            </button>
            <table>
                {displayUsers.map(user => (
                    <tr key={user.id}>
                        <td>{user.name}</td>
                        <td>{user.phone}</td>
                    </tr>
                ))}
            </table>
        </div>
    );
}
```

### 3. 数据验证

```javascript
// 表单提交前验证
function handleSubmit(data) {
    const validation = UniMaskUtils.validate(data.name);
    
    if (!validation.valid) {
        alert('姓名格式不正确：' + validation.error);
        return;
    }
    
    // 提交数据
    submitForm(data);
}
```

### 4. 数据分析

```javascript
// 分析加密文本统计
function analyzeEncryptedData(dataList) {
    let totalEncrypted = 0;
    let totalChars = 0;
    
    dataList.forEach(text => {
        const analysis = UniMaskUtils.analyzeText(text);
        totalEncrypted += analysis.encryptedChars;
        totalChars += analysis.totalChars;
    });
    
    const rate = (totalEncrypted / totalChars * 100).toFixed(2);
    console.log(`加密率: ${rate}%`);
}
```

### 5. 打印/PDF 导出

```javascript
// 准备打印数据
function preparePrintData(users) {
    // 将加密字符替换为星号，便于打印
    return UniMaskUtils.replaceFieldsInArray(
        users, 
        ['name', 'idCard', 'phone'],
        '●'  // 使用实心圆点
    );
}
```

## 💡 最佳实践

1. **导出数据时统一替换**：避免导出文件中包含乱码的加密字符
2. **批量处理优化性能**：使用 `replaceFieldsInArray` 而不是循环调用
3. **验证数据格式**：使用 `validate` 方法确保数据格式正确
4. **自定义替换字符**：根据场景选择合适的替换字符（`*`、`●`、`■` 等）
5. **分析加密情况**：使用 `analyzeText` 了解数据加密比例

## 🔧 高级用法

### 自定义加密区域

如果需要扩展支持其他 Unicode 区域：

```javascript
// 扩展 UniMaskUtils
UniMaskUtils.REGIONS.CUSTOM_START = 0x10000;
UniMaskUtils.REGIONS.CUSTOM_END = 0x10FFF;

// 修改检测逻辑（需要扩展原代码）
const originalIsEncryptedChar = UniMaskUtils.isEncryptedChar;
UniMaskUtils.isEncryptedChar = function(char) {
    if (originalIsEncryptedChar.call(this, char)) {
        return true;
    }
    const codePoint = char.charCodeAt(0);
    return codePoint >= this.REGIONS.CUSTOM_START && 
           codePoint <= this.REGIONS.CUSTOM_END;
};
```

## 📝 注意事项

1. **不可逆性**：替换为星号后无法还原为原始加密字符
2. **前端显示**：如果只是前端显示，建议使用 UniMask 字体而不是字符替换
3. **数据导出**：导出到外部系统时才需要字符替换
4. **性能考虑**：大批量数据处理时注意性能优化

## 📄 许可证

MIT License
