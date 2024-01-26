package fr.inrae.fishola.rest;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import com.google.common.base.Preconditions;
import fr.inrae.fishola.exceptions.FisholaTechnicalException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;

public class ImageHelper {

    private static final Log log = LogFactory.getLog(ImageHelper.class);

    public static byte[] base64ImageToJpegBytes(String base64Image, float rawImageQuality) {

        byte[] bytes = Base64.getDecoder().decode(base64Image);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(bis);

            image = ImageHelper.removeAlphaIfPresent(image);

//            Set<String> formats = ImmutableSet.of("jpeg");
//            for (String format : formats) {
//                for (float quality = 1f; quality >= 0.90f; quality -= 0.01f) {
//                    // write the image to a file
//                    byte[] testBytes = imageToBytes(image, format, quality);
//                    File parent = new File("/tmp/taiste-0.01");
//                    parent.mkdirs();
//                    File file = new File(parent, String.format("%s-%s-%.3f.%s", format, catchId, quality, format));
//                    Files.write(testBytes, file);
//
//                    if (testBytes.length > 0) {
//                        log.info(String.format("%s/%.3f=%dkb en %s", format, quality, testBytes.length / 1024, file.getAbsolutePath()));
//                    }
//                }
//            }

            byte[] jpegBytes = ImageHelper.imageToBytes(image, "jpeg", rawImageQuality);
            if (jpegBytes.length > 0) {
                log.info("Pas de soucis pour: " + image);
                LogFactory.getLog(ImageHelper.class).info(String.format("Taille: %dkb", jpegBytes.length / 1024));
            }

            Preconditions.checkState(jpegBytes.length > 0, "Contenu vide pour l'image : %s".formatted(image));

            return jpegBytes;
        } catch (IOException ioe) {
            throw new FisholaTechnicalException("Impossible de lire l'image", ioe);
        }

    }

    public static BufferedImage removeAlphaIfPresent(BufferedImage image) {
        // On gère le cas de photos avec un canal alpha -> on le remplace par du noir
        if (image.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
            BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g2d = copy.createGraphics();
            g2d.setColor(Color.BLACK); // Or what ever fill color you want...
            g2d.fillRect(0, 0, copy.getWidth(), copy.getHeight());
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();
            return copy;
        }
        return image;
    }

    public static byte[] imageToBytes(BufferedImage image, String format, float quality) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        imageToStream(image, format, quality, outputStream);

        byte[] result = outputStream.toByteArray();
        return result;
    }

    public static void imageToStream(BufferedImage image, String format, float quality, OutputStream outputStream) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); // Nécessaire pour pouvoir impacter la qualité
        param.setCompressionQuality(quality);
        ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream);
        writer.setOutput(imageOutputStream);

        writer.write(null, new IIOImage(image, null, null), param);
    }


}
