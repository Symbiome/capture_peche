package fr.inrae.fishola.rest;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ImageHelper {

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
