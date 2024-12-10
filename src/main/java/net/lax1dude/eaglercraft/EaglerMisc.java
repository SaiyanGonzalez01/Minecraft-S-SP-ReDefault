package net.lax1dude.eaglercraft;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import com.jcraft.jzlib.InflaterInputStream;

public class EaglerMisc {
	
    public static byte[] uncompress(byte[] input) throws IOException {
    	return getBytesFromInputStream(new InflaterInputStream(new ByteArrayInputStream(input)));
    }
    
    public static byte[] getBytesFromInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(); 
        byte[] buffer = new byte[0xFFFF];
        for (int len = is.read(buffer); len != -1; len = is.read(buffer)) { 
            os.write(buffer, 0, len);
        }
        return os.toByteArray();
    }

    public static String bytesToString(byte[] bb) {
        if (bb == null) return "";
        return new String(bb, Charset.forName("UTF-8"));
    }

    public static String[] bytesToLines(byte[] bb) {
        String contents = bytesToString(bb);
        if(contents.isEmpty()) {
            return new String[0];
        }else {
            return contents.replace("\r\n", "\n").split("[\r\n]");
        }
    }
}
