# UniMask 字体生成器

自动生成将加密区域字符显示为星号(*)的字体文件。

## 📋 功能

- 将 **PUA 私有使用区** (U+E000-U+F8FF, 6400个字符) 映射为星号
- 将 **罕用韩文音节区** (U+CF70-U+D7A3, 2100个字符) 映射为星号
- 支持生成 OTF、WOFF、WOFF2 格式
- 保留基本 ASCII 字符的正常显示

## 🚀 快速开始

### 1. 安装依赖

```bash
pip install -r requirements.txt
# 或者
pip install fonttools brotli
```

### 2. 生成字体

```bash
python generate_mask_font_simple.py
```

生成的字体文件位于 `fonts/` 目录：
- `UniMask.otf` - OpenType 字体（桌面使用）
- `UniMask.woff` - Web 字体格式 1
- `UniMask.woff2` - Web 字体格式 2（更小）

## 📖 使用方法

### 在网页中使用 (CSS)

```css
@font-face {
    font-family: 'UniMask';
    src: url('fonts/UniMask.woff2') format('woff2'),
         url('fonts/UniMask.woff') format('woff'),
         url('fonts/UniMask.otf') format('opentype');
    font-weight: normal;
    font-style: normal;
}

.encrypted-text {
    font-family: 'UniMask', monospace;
    letter-spacing: 2px;
}
```

```html
<p class="encrypted-text">张</p>
<!-- PUA 区字符会显示为 ** -->
```

### 在桌面应用中使用

#### Windows
1. 双击 `UniMask.otf` 或 `UniMask.ttf`
2. 点击"安装"按钮
3. 在 Word、Excel 等应用中选择 "UniMask" 字体

#### macOS
1. 双击 `UniMask.otf` 或 `UniMask.ttf`
2. 在"字体册"中点击"安装字体"
3. 在任意应用中选择 "UniMask" 字体

#### Linux
```bash
mkdir -p ~/.fonts
cp fonts/UniMask.otf ~/.fonts/
fc-cache -f -v
```

### 与 UniMask 加密库配合

```java
// 1. 加密姓名
ChineseFPEService service = new ChineseFPEService("password");
String name = "张伟明";
String encrypted = service.encrypt(name, "context", 1);
// encrypted = "张" + PUA字符

// 2. 在网页中显示
// <span class="encrypted-text">张</span>
// 使用 UniMask 字体后，PUA 字符显示为 **
// 最终显示效果: 张**
```

## 🎨 字体效果

| 原文 | 加密后（Unicode） | 使用 UniMask 字体显示 |
|------|------------------|---------------------|
| 张伟明 | 张<U+E123><U+E456> | 张** |
| 李小红 | 李<U+E789><U+EABC> | 李** |
| 王大锤 | 王<U+EDEF><U+F012> | 王** |

## 🔧 自定义

### 修改星号样式

编辑 `generate_mask_font_simple.py`，在 `create_asterisk_glyph()` 方法中修改字形绘制代码：

```python
def create_asterisk_glyph(self):
    """创建星号字形"""
    pen = T2CharStringPen(600, None)
    
    # 在这里绘制你想要的形状
    # 例如：圆点、问号、方块等
    
    # 示例：绘制圆点
    pen.moveTo((250, 350))
    pen.curveTo((250, 400), (290, 440), (340, 440))
    pen.curveTo((390, 440), (430, 400), (430, 350))
    pen.curveTo((430, 300), (390, 260), (340, 260))
    pen.curveTo((290, 260), (250, 300), (250, 350))
    pen.closePath()
    
    return pen.getCharString()
```

### 修改字符范围

编辑脚本中的常量：

```python
# Unicode 区域定义
PUA_START = 0xE000      # 修改起始位置
PUA_END = 0xF8FF        # 修改结束位置
KOREAN_START = 0xCF70   # 修改起始位置
KOREAN_END = 0xD7A3     # 修改结束位置
```

## 📊 字体信息

- **字体名称**: UniMask
- **字体家族**: UniMask Mono
- **字形数量**: ~8,500+
  - PUA 区: 6,400 个
  - 韩文区: ~2,100 个
- **EM 单位**: 1000
- **支持格式**: OTF, TTF, WOFF, WOFF2

## 🐛 故障排除

### 问题：ImportError: No module named 'fontTools'

**解决方案:**
```bash
pip install fonttools brotli
```

### 问题：字体文件生成失败

**解决方案:**
1. 检查输出目录权限
2. 确保有足够的磁盘空间（字体文件约 1-5 MB）
3. 查看详细错误信息

### 问题：生成的字体无法使用

**解决方案:**
1. 确认字体文件完整性
2. 尝试重新安装字体
3. 清除字体缓存：
   - Windows: 删除 `C:\Windows\Fonts\` 中的旧版本
   - macOS: 在字体册中验证字体
   - Linux: 运行 `fc-cache -f -v`

## 📝 注意事项

1. **字体大小**: 生成的字体文件可能较大（1-5 MB），因为包含大量字形
2. **性能**: 在网页中使用时，优先使用 WOFF2 格式以获得更好的加载性能
3. **兼容性**: 确保目标浏览器支持自定义字体（现代浏览器均支持）
4. **许可**: 生成的字体文件仅用于项目内部使用

## 📄 许可证

MIT License
