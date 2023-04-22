package org.gotoobfuscator.runtime;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.HashMap;

@SuppressWarnings("unused")
public final class Const {
    private static final HashMap<String,Object> map = new HashMap<>();

    public static Object get(String key) {
        return map.get(key);
    }

    static {
        final InputStream stream = Const.class.getResourceAsStream("/Const");

        if (stream == null) {
            System.out.println("Const pool not found");
        } else {
            try (final DataInputStream dis = new DataInputStream(stream)) {
                final int size = dis.readInt();

                for (int i = 0; i < size; i++) {
                    final String sha = dis.readUTF();
                    final int type = dis.readInt();

                    switch (type) {
                        case 0:
                            final String s = dis.readUTF();

                            map.put(sha,s);

                            break;
                        case 1:
                            final int j = dis.readInt();

                            map.put(sha,j);

                            break;
                        case 2:
                            final long l = dis.readLong();

                            map.put(sha,l);

                            break;
                        case 3:
                            final float f = dis.readFloat();

                            map.put(sha,f);

                            break;
                        case 4:
                            final double d = dis.readDouble();

                            map.put(sha,d);

                            break;
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}
