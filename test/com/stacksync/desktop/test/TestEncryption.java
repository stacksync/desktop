/*
 * Syncany
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.stacksync.desktop.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import com.stacksync.desktop.encryption.BasicEncryption;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class TestEncryption {
    public static void main(String[] args) throws Exception {
        BasicEncryption enc = new BasicEncryption("encpass");
        enc.setPassword("encpass");

        // Encrypt
        File file = new File("/home/pheckel/.face");
        FileInputStream fis = new FileInputStream(file);

        System.out.println(file.length() + " -- "+fis.available());
        byte[] buffer = new byte[fis.available()];

        fis.read(buffer);
        fis.close();

        byte[] encrypted = enc.encrypt(buffer);

        FileOutputStream fos = new FileOutputStream(new File("/tmp/face-enc.jpg"));

        fos.write(encrypted);
        fos.close();

        // Decrypt
        fis = new FileInputStream(new File("/tmp/face-enc.jpg"));
        buffer = new byte[fis.available()];

        fis.read(buffer);
        fis.close();

        byte[] decrypted = enc.decrypt(buffer);

        fos = new FileOutputStream(new File("/tmp/face-dec.jpg"));

        fos.write(decrypted);
        fos.close();
    }
}
