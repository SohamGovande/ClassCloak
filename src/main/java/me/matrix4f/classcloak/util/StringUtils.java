package me.matrix4f.classcloak.util;

import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class StringUtils {

    public static String hash(String function, String in) {
        try {
            return DatatypeConverter.printHexBinary(
                    MessageDigest
                            .getInstance(function)
                            .digest(in.getBytes("UTF-8"))
            );
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            //never occurrs
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] hashBytes(String function, String in) {
        try {
            return MessageDigest
                            .getInstance(function)
                            .digest(in.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            //never occurrs
            e.printStackTrace();
        }
        return null;
    }

    public static String sha256(String in) {
        return hash("SHA-256", in);
    }

    public static int encrypt(int input, Random random) {
        return (char) (input ^ random.nextInt(Character.MAX_VALUE));
    }

    public static String salt(String input, long seed) {
        Random random = new Random(seed);
        StringBuilder builder = new StringBuilder(input);
        for(int i = 0; i < input.length(); i++)
            builder.setCharAt(i, (char) (builder.charAt(i) ^ random.nextInt(Character.MAX_VALUE)));
        return builder.toString();
    }

    public static byte[] sha256Bytes(String in, boolean salt) {
        return hashBytes("SHA-256", salt ? salt(in, 0xff4a3d) : in);
    }
}
