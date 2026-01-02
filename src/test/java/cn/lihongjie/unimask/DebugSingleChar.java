package cn.lihongjie.unimask;

public class DebugSingleChar {
    public static void main(String[] args) {
        try {
            byte[] key = new byte[16];
            for (int i = 0; i < 16; i++) {
                key[i] = (byte) i;
            }
            
            ChineseFPEService service = new ChineseFPEService(key);
            CharacterMapping cm = new CharacterMapping();
            
            String plaintext = "中";
            String tweak = "tweak";
            
            int zhongIndex = cm.getCharIndex('中');
            int yiIndex = cm.getCharIndex('一');
            System.out.println("中的索引: " + zhongIndex);
            System.out.println("一的索引: " + yiIndex);
            
            System.out.println("\n原文: " + plaintext);
            String encrypted = service.encrypt(0, 0, tweak, plaintext);
            System.out.println("密文: " + encrypted);
            System.out.println("密文字符码: " + Integer.toHexString(encrypted.charAt(0)));
            
            int encryptedIndex = cm.mapFromEncryptedChar(encrypted.charAt(0));
            System.out.println("密文对应的索引: " + encryptedIndex);
            
            String decrypted = service.decrypt(encrypted, 0, 0, tweak);
            System.out.println("\n解密: " + decrypted);
            System.out.println("解密字符码: " + Integer.toHexString(decrypted.charAt(0)));
            
            System.out.println("\n原文字符码: " + Integer.toHexString(plaintext.charAt(0)));
            System.out.println("匹配: " + plaintext.equals(decrypted));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
