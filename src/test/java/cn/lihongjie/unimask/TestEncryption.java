package cn.lihongjie.unimask;

public class TestEncryption {
    public static void main(String[] args) {
        ChineseFPEService service = new ChineseFPEService();
        
        String name = "张小三";
        System.out.println("原文: " + name + " (长度: " + name.length() + ")");
        
        String encrypted = service.encrypt(1, 1, "test-tweak", name);
        System.out.println("加密: " + encrypted + " (长度: " + encrypted.length() + ")");
        System.out.println("首字符: 原=" + name.charAt(0) + ", 加密=" + encrypted.charAt(0));
        System.out.println("末字符: 原=" + name.charAt(name.length()-1) + ", 加密=" + encrypted.charAt(encrypted.length()-1));
        System.out.println("中间字符应被加密: " + name.charAt(1) + " -> " + encrypted.charAt(1));
        
        String decrypted = service.decrypt(encrypted, 1, 1, "test-tweak");
        System.out.println("解密: " + decrypted + " (长度: " + decrypted.length() + ")");
        System.out.println("匹配: " + name.equals(decrypted));
    }
}
