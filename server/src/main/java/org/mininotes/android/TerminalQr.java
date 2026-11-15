// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.util.Map;

/**
 * A code drawn in the terminal, two rows of it to a line with half blocks, light on dark as a phone's camera reads it from a
 * screen (decision 330): the pair command's code, scanned with Mininotes from the SSH window.
 */
final class TerminalQr {
    private TerminalQr(){}

    static String draw(String text) {
        try {
            BitMatrix m=new QRCodeWriter().encode(text,BarcodeFormat.QR_CODE,0,0,
                Map.of(EncodeHintType.ERROR_CORRECTION,ErrorCorrectionLevel.L,EncodeHintType.MARGIN,1));
            StringBuilder out=new StringBuilder();
            for(int y=0;y<m.getHeight();y+=2) {
                for(int x=0;x<m.getWidth();x++) {
                    boolean top=m.get(x,y),bottom=y+1<m.getHeight()&&m.get(x,y+1);
                    // Dark modules are drawn as spaces: the light around them is the terminal's own text colour.
                    out.append(top?(bottom?' ':'▄'):(bottom?'▀':'█'));
                }
                out.append('\n');
            }
            return out.toString();
        } catch(Exception none){return "(The code could not be drawn here: paste the link below.)";}
    }
}
