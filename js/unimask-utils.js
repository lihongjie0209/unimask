/**
 * UniMask 前端工具类
 * 
 * 提供加密文本的检测和处理功能
 * @version 1.0.0
 * @author Li Hongjie
 */

class UniMaskUtils {
    /**
     * Unicode 区域定义
     */
    static REGIONS = {
        // PUA 私有使用区
        PUA_START: 0xE000,
        PUA_END: 0xF8FF,
        // 罕用韩文音节区
        KOREAN_START: 0xCF70,
        KOREAN_END: 0xD7A3
    };

    /**
     * 检测字符是否为加密字符
     * @param {string} char - 单个字符
     * @returns {boolean} 是否为加密字符
     */
    static isEncryptedChar(char) {
        if (!char || char.length === 0) {
            return false;
        }
        
        const codePoint = char.charCodeAt(0);
        
        // 检测是否在 PUA 区
        if (codePoint >= this.REGIONS.PUA_START && codePoint <= this.REGIONS.PUA_END) {
            return true;
        }
        
        // 检测是否在韩文区
        if (codePoint >= this.REGIONS.KOREAN_START && codePoint <= this.REGIONS.KOREAN_END) {
            return true;
        }
        
        return false;
    }

    /**
     * 检测文本是否包含加密字符
     * @param {string} text - 待检测文本
     * @returns {boolean} 是否包含加密字符
     */
    static isEncrypted(text) {
        if (!text || typeof text !== 'string') {
            return false;
        }
        
        for (let i = 0; i < text.length; i++) {
            if (this.isEncryptedChar(text[i])) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 统计文本中加密字符的数量
     * @param {string} text - 待统计文本
     * @returns {number} 加密字符数量
     */
    static countEncryptedChars(text) {
        if (!text || typeof text !== 'string') {
            return 0;
        }
        
        let count = 0;
        for (let i = 0; i < text.length; i++) {
            if (this.isEncryptedChar(text[i])) {
                count++;
            }
        }
        
        return count;
    }

    /**
     * 将加密字符替换为星号 (*)
     * @param {string} text - 原始文本
     * @param {string} [replacement='*'] - 替换字符，默认为星号
     * @returns {string} 替换后的文本
     */
    static replaceEncryptedWithAsterisk(text, replacement = '*') {
        if (!text || typeof text !== 'string') {
            return text;
        }
        
        let result = '';
        for (let i = 0; i < text.length; i++) {
            const char = text[i];
            if (this.isEncryptedChar(char)) {
                result += replacement;
            } else {
                result += char;
            }
        }
        
        return result;
    }

    /**
     * 批量处理对象中的加密字段
     * @param {Object} obj - 待处理对象
     * @param {string[]} fields - 需要处理的字段名数组
     * @param {string} [replacement='*'] - 替换字符
     * @returns {Object} 处理后的新对象
     */
    static replaceFieldsInObject(obj, fields, replacement = '*') {
        if (!obj || typeof obj !== 'object') {
            return obj;
        }
        
        const result = { ...obj };
        
        for (const field of fields) {
            if (field in result && typeof result[field] === 'string') {
                result[field] = this.replaceEncryptedWithAsterisk(result[field], replacement);
            }
        }
        
        return result;
    }

    /**
     * 批量处理数组中所有对象的加密字段
     * @param {Array} array - 对象数组
     * @param {string[]} fields - 需要处理的字段名数组
     * @param {string} [replacement='*'] - 替换字符
     * @returns {Array} 处理后的新数组
     */
    static replaceFieldsInArray(array, fields, replacement = '*') {
        if (!Array.isArray(array)) {
            return array;
        }
        
        return array.map(obj => this.replaceFieldsInObject(obj, fields, replacement));
    }

    /**
     * 获取文本中加密字符的位置信息
     * @param {string} text - 待分析文本
     * @returns {Array<{index: number, char: string, codePoint: string}>} 加密字符位置数组
     */
    static getEncryptedPositions(text) {
        if (!text || typeof text !== 'string') {
            return [];
        }
        
        const positions = [];
        for (let i = 0; i < text.length; i++) {
            const char = text[i];
            if (this.isEncryptedChar(char)) {
                positions.push({
                    index: i,
                    char: char,
                    codePoint: '0x' + char.charCodeAt(0).toString(16).toUpperCase()
                });
            }
        }
        
        return positions;
    }

    /**
     * 分析文本的加密情况
     * @param {string} text - 待分析文本
     * @returns {Object} 分析结果
     */
    static analyzeText(text) {
        if (!text || typeof text !== 'string') {
            return {
                isEncrypted: false,
                totalChars: 0,
                encryptedChars: 0,
                plainChars: 0,
                encryptionRate: 0,
                positions: []
            };
        }
        
        const encryptedChars = this.countEncryptedChars(text);
        const totalChars = text.length;
        const plainChars = totalChars - encryptedChars;
        
        return {
            isEncrypted: encryptedChars > 0,
            totalChars: totalChars,
            encryptedChars: encryptedChars,
            plainChars: plainChars,
            encryptionRate: totalChars > 0 ? (encryptedChars / totalChars * 100).toFixed(2) + '%' : '0%',
            positions: this.getEncryptedPositions(text)
        };
    }

    /**
     * 将加密文本转换为可导出的格式（用于 CSV、Excel 等）
     * @param {string} text - 原始加密文本
     * @param {Object} options - 配置选项
     * @param {string} [options.replacement='*'] - 替换字符
     * @param {boolean} [options.preserveLength=true] - 是否保持长度一致
     * @returns {string} 可导出的文本
     */
    static toExportFormat(text, options = {}) {
        const {
            replacement = '*',
            preserveLength = true
        } = options;
        
        if (!text || typeof text !== 'string') {
            return text;
        }
        
        if (preserveLength) {
            return this.replaceEncryptedWithAsterisk(text, replacement);
        } else {
            // 不保持长度，连续的加密字符合并为指定数量的星号
            let result = '';
            let inEncrypted = false;
            
            for (let i = 0; i < text.length; i++) {
                const char = text[i];
                if (this.isEncryptedChar(char)) {
                    if (!inEncrypted) {
                        result += replacement.repeat(2); // 默认用两个星号表示
                        inEncrypted = true;
                    }
                } else {
                    result += char;
                    inEncrypted = false;
                }
            }
            
            return result;
        }
    }

    /**
     * 验证文本是否为合法的加密文本格式
     * @param {string} text - 待验证文本
     * @returns {Object} 验证结果
     */
    static validate(text) {
        if (!text || typeof text !== 'string') {
            return {
                valid: false,
                error: '文本为空或格式不正确'
            };
        }
        
        const analysis = this.analyzeText(text);
        
        if (!analysis.isEncrypted) {
            return {
                valid: false,
                error: '文本不包含加密字符'
            };
        }
        
        // 检测是否有明文字符（至少应该保留一些明文）
        if (analysis.plainChars === 0) {
            return {
                valid: false,
                error: '文本全部加密，可能不是有效的部分加密文本'
            };
        }
        
        return {
            valid: true,
            analysis: analysis
        };
    }
}

// 支持 CommonJS (Node.js)
if (typeof module !== 'undefined' && module.exports) {
    module.exports = UniMaskUtils;
}

// 支持 ES6 模块
if (typeof exports !== 'undefined') {
    exports.UniMaskUtils = UniMaskUtils;
}

// 浏览器全局变量
if (typeof window !== 'undefined') {
    window.UniMaskUtils = UniMaskUtils;
}
