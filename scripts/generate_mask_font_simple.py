#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
UniMask 字体生成器 (fontTools 版本)
使用 fontTools 库生成将加密区域字符显示为 * 的字体文件

依赖: pip install fonttools brotli
"""

from fontTools.fontBuilder import FontBuilder
from fontTools.pens.t2CharStringPen import T2CharStringPen
from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.ttLib import TTFont
from fontTools.pens.recordingPen import RecordingPen
from fontTools.pens.reverseContourPen import ReverseContourPen
import sys
import os


class UniMaskFontGeneratorSimple:
    """使用 fontTools 的简化字体生成器"""
    
    # Unicode 区域定义
    PUA_START = 0xE000      # PUA 区起始
    PUA_END = 0xF8FF        # PUA 区结束
    KOREAN_START = 0xCF70   # 罕用韩文音节区起始
    KOREAN_END = 0xD7A3     # 韩文音节区结束
    
    def __init__(self, font_name="UniMask"):
        self.font_name = font_name
        self.glyphs = {}
        self.cmap = {}
        self.glyph_order = ['.notdef']
    
    def load_asterisk_from_system_font(self):
        """从系统字体加载星号字形"""
        # 尝试的系统字体列表（Windows）
        system_fonts = [
            r"C:\Windows\Fonts\msyh.ttc",      # 微软雅黑
            r"C:\Windows\Fonts\simsun.ttc",     # 宋体
            r"C:\Windows\Fonts\arial.ttf",      # Arial
            r"C:\Windows\Fonts\segoeui.ttf",    # Segoe UI
            r"C:\Windows\Fonts\times.ttf",      # Times New Roman
        ]
        
        for font_path in system_fonts:
            if os.path.exists(font_path):
                try:
                    print(f"尝试从系统字体加载星号: {os.path.basename(font_path)}")
                    font = TTFont(font_path, fontNumber=0 if font_path.endswith('.ttc') else None)
                    
                    # 查找星号字符的字形名称
                    cmap = font.getBestCmap()
                    if cmap and ord('*') in cmap:
                        glyph_name = cmap[ord('*')]
                        print(f"找到星号字形: {glyph_name}")
                        
                        # 获取字形数据
                        glyph_set = font.getGlyphSet()
                        if glyph_name in glyph_set:
                            glyph = glyph_set[glyph_name]
                            
                            # 获取原字体的单位
                            source_upem = font['head'].unitsPerEm
                            target_upem = 1000  # 我们的目标字体单位
                            scale = target_upem / source_upem
                            
                            print(f"  原字体单位: {source_upem}, 缩放比例: {scale:.4f}")
                            
                            # 使用 RecordingPen 记录绘图命令
                            rec_pen = RecordingPen()
                            glyph.draw(rec_pen)
                            
                            # 将记录的命令重放到 T2CharStringPen，应用缩放
                            t2_pen = T2CharStringPen(int(glyph.width * scale), None)
                            
                            # 重放命令并缩放坐标
                            for op, args in rec_pen.value:
                                if op == 'moveTo':
                                    t2_pen.moveTo((args[0][0] * scale, args[0][1] * scale))
                                elif op == 'lineTo':
                                    t2_pen.lineTo((args[0][0] * scale, args[0][1] * scale))
                                elif op == 'curveTo':
                                    scaled_args = tuple((x * scale, y * scale) for x, y in args)
                                    t2_pen.curveTo(*scaled_args)
                                elif op == 'qCurveTo':
                                    scaled_args = tuple((x * scale, y * scale) for x, y in args)
                                    t2_pen.qCurveTo(*scaled_args)
                                elif op == 'closePath':
                                    t2_pen.closePath()
                                elif op == 'endPath':
                                    t2_pen.endPath()
                            
                            print(f"✓ 成功从 {os.path.basename(font_path)} 加载并缩放星号字形")
                            font.close()
                            return t2_pen.getCharString(), int(glyph.width * scale)
                    
                    font.close()
                except Exception as e:
                    print(f"  加载失败: {e}")
                    continue
        
        print("⚠ 无法从系统字体加载星号，使用手绘字形")
        return None, None
        
    def create_asterisk_glyph(self):
        """创建星号字形（优先从系统字体加载）"""
        # 先尝试从系统字体加载
        char_string, width = self.load_asterisk_from_system_font()
        if char_string:
            return char_string
        
        # 如果加载失败，使用手绘星号
        print("使用手绘星号字形")
        pen = T2CharStringPen(600, None)
        
        # 绘制简单的星号（十字形）
        # 垂直线
        pen.moveTo((280, 150))
        pen.lineTo((320, 150))
        pen.lineTo((320, 650))
        pen.lineTo((280, 650))
        pen.closePath()
        
        # 水平线
        pen.moveTo((100, 380))
        pen.lineTo((500, 380))
        pen.lineTo((500, 420))
        pen.lineTo((100, 420))
        pen.closePath()
        
        return pen.getCharString()
    
    def create_notdef_glyph(self):
        """创建 .notdef 字形（空方框）"""
        pen = T2CharStringPen(600, None)
        
        # 外框
        pen.moveTo((50, 0))
        pen.lineTo((550, 0))
        pen.lineTo((550, 700))
        pen.lineTo((50, 700))
        pen.closePath()
        
        # 内框（镂空）
        pen.moveTo((100, 50))
        pen.lineTo((100, 650))
        pen.lineTo((500, 650))
        pen.lineTo((500, 50))
        pen.closePath()
        
        return pen.getCharString()
    
    def build_font(self):
        """构建字体"""
        print(f"构建字体: {self.font_name}")
        
        # 创建 FontBuilder
        fb = FontBuilder(1000, isTTF=False)  # 使用 CFF/OTF 格式
        
        # 1. 创建 .notdef 字形
        print("创建 .notdef 字形...")
        notdef_glyph = self.create_notdef_glyph()
        self.glyphs['.notdef'] = notdef_glyph
        
        # 2. 创建唯一的星号字形（所有加密字符共享）
        print("创建共享星号字形...")
        asterisk_glyph = self.create_asterisk_glyph()
        self.glyph_order.append('asterisk')
        self.glyphs['asterisk'] = asterisk_glyph
        
        # 3. 为 PUA 区域添加字符映射（共享同一个星号字形）
        print(f"映射 PUA 区域 (U+{self.PUA_START:04X} - U+{self.PUA_END:04X})...")
        count_pua = 0
        for codepoint in range(self.PUA_START, self.PUA_END + 1):
            # 所有PUA字符都映射到同一个asterisk字形
            self.cmap[codepoint] = 'asterisk'
            count_pua += 1
            
            if count_pua % 1000 == 0:
                print(f"  已映射 PUA: {count_pua} 个字符...")
        
        print(f"  完成 PUA 区域: {count_pua} 个字符")
        
        # 4. 为韩文音节区域添加字符映射（共享同一个星号字形）
        print(f"映射韩文音节区 (U+{self.KOREAN_START:04X} - U+{self.KOREAN_END:04X})...")
        count_korean = 0
        for codepoint in range(self.KOREAN_START, self.KOREAN_END + 1):
            # 所有韩文字符都映射到同一个asterisk字形
            self.cmap[codepoint] = 'asterisk'
            count_korean += 1
            
            if count_korean % 500 == 0:
                print(f"  已映射韩文: {count_korean} 个字符...")
        
        print(f"  完成韩文音节区: {count_korean} 个字符")
        
        # 5. 添加空格字符
        print("添加空格字符...")
        space_pen = T2CharStringPen(300, None)
        self.glyph_order.append('space')
        self.glyphs['space'] = space_pen.getCharString()
        self.cmap[0x0020] = 'space'
        
        # 6. 设置字体信息
        print("设置字体元数据...")
        fb.setupGlyphOrder(self.glyph_order)
        fb.setupCharacterMap(self.cmap)
        
        # 字符串表
        fb.setupNameTable({
            'familyName': self.font_name,
            'styleName': 'Regular',
            'uniqueFontIdentifier': f'{self.font_name}-Regular',
            'fullName': f'{self.font_name} Regular',
            'psName': self.font_name.replace(' ', ''),
            'version': 'Version 1.0',
        })
        
        # 头部表
        import time
        # Mac epoch (1904) to current time
        mac_epoch = 2082844800  # Seconds between 1904 and 1970
        current_time = int(time.time())
        created_timestamp = current_time + mac_epoch  # Add to get Mac epoch time
        
        fb.setupHead(
            unitsPerEm=1000,
            created=created_timestamp,
            lowestRecPPEM=8
        )
        
        # 水平度量
        metrics = {}
        for glyph_name in self.glyph_order:
            if glyph_name == 'space':
                metrics[glyph_name] = (300, 0)
            elif glyph_name == 'asterisk':
                metrics[glyph_name] = (600, 50)
            else:
                metrics[glyph_name] = (600, 50)
        
        fb.setupHorizontalMetrics(metrics)
        
        # OS/2 表
        fb.setupOS2(
            sTypoAscender=800,
            sTypoDescender=-200,
            sTypoLineGap=200,
            usWinAscent=1000,
            usWinDescent=200,
        )
        
        # post 表
        fb.setupPost()
        
        # CFF 表（包含字形轮廓）
        private = {
            'BlueValues': [],
            'FamilyBlues': [],
        }
        fontInfo = {
            'FullName': f'{self.font_name} Regular',
            'FamilyName': self.font_name,
            'Weight': 'Regular',
            'version': '1.0',
        }
        fb.setupCFF(
            psName=self.font_name.replace(' ', ''),
            charStringsDict=self.glyphs,
            privateDict=private,
            fontInfo=fontInfo
        )
        
        # hmtx 表（水平度量）
        fb.setupHorizontalHeader(ascent=800, descent=-200)
        
        return fb
    
    def generate(self, output_dir=".", formats=None):
        """生成字体文件"""
        import os
        
        if formats is None:
            formats = ['otf']
        
        os.makedirs(output_dir, exist_ok=True)
        
        print("\n开始生成字体...")
        fb = self.build_font()
        
        for fmt in formats:
            output_file = os.path.join(output_dir, f"{self.font_name}.{fmt}")
            print(f"\n生成 {fmt.upper()} 格式: {output_file}")
            
            try:
                if fmt == 'otf':
                    fb.font.save(output_file)
                    print(f"  ✓ 成功生成 OTF 字体")
                elif fmt == 'woff':
                    fb.font.flavor = 'woff'
                    fb.font.save(output_file)
                    print(f"  ✓ 成功生成 WOFF 字体")
                elif fmt == 'woff2':
                    fb.font.flavor = 'woff2'
                    fb.font.save(output_file)
                    print(f"  ✓ 成功生成 WOFF2 字体")
                else:
                    print(f"  ✗ 不支持的格式: {fmt}")
                    
            except Exception as e:
                print(f"  ✗ 生成失败: {e}")
        
        print("\n字体信息:")
        print(f"  字形数量: {len(self.glyph_order)}")
        print(f"  字符映射: {len(self.cmap)} 个")
        print(f"  PUA 区域: U+{self.PUA_START:04X} - U+{self.PUA_END:04X}")
        print(f"  韩文区域: U+{self.KOREAN_START:04X} - U+{self.KOREAN_END:04X}")


def main():
    """主函数"""
    print("=" * 60)
    print("UniMask 字体生成器 (fontTools 版本)")
    print("将 PUA 区和韩文音节区字符映射为星号 (*)")
    print("=" * 60)
    print()
    
    import os
    
    # 创建生成器
    generator = UniMaskFontGeneratorSimple("UniMask")
    
    # 生成字体
    output_dir = os.path.join(os.path.dirname(__file__), '..', 'fonts')
    generator.generate(
        output_dir=output_dir,
        formats=['otf', 'woff', 'woff2']
    )
    
    print("\n" + "=" * 60)
    print("字体生成完成！")
    print("=" * 60)
    print()
    print("使用方法:")
    print()
    print("1. Web 中使用 (CSS):")
    print("   @font-face {")
    print("       font-family: 'UniMask';")
    print("       src: url('fonts/UniMask.woff2') format('woff2'),")
    print("            url('fonts/UniMask.woff') format('woff'),")
    print("            url('fonts/UniMask.otf') format('opentype');")
    print("       font-weight: normal;")
    print("       font-style: normal;")
    print("   }")
    print()
    print("   .encrypted-text {")
    print("       font-family: 'UniMask', monospace;")
    print("   }")
    print()
    print("2. 系统中安装:")
    print("   - Windows: 双击 UniMask.otf 安装")
    print("   - macOS: 双击 UniMask.otf 添加到字体册")
    print("   - Linux: 复制到 ~/.fonts/ 目录")
    print()


if __name__ == "__main__":
    try:
        main()
    except ImportError as e:
        print("\n错误: 缺少依赖库")
        print()
        print("请安装所需的 Python 包:")
        print("  pip install fonttools brotli")
        print()
        print("其中:")
        print("  - fonttools: 字体处理核心库")
        print("  - brotli: WOFF2 格式压缩支持（可选）")
        print()
        print(f"详细错误: {e}")
        sys.exit(1)
    except Exception as e:
        print(f"\n错误: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
