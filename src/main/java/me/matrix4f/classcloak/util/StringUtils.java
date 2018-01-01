package me.matrix4f.classcloak.util;

import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    public static String sha256(String in) {
        return hash("SHA-256", in);
    }
}
