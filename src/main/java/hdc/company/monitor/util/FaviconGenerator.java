package hdc.company.monitor.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple programmatic favicon renderer.
 * Produces PNGs (16x16, 32x32) and an ICO that embeds the PNGs.
 */
public class FaviconGenerator {

    public static void main(String[] args) throws Exception {
        Path out = Path.of("src/main/webapp");
        if (args != null && args.length > 0) {
            out = Path.of(args[0]);
        }
        generateAll(out);
        System.out.println("Favicon files written to: " + out.toAbsolutePath());
    }

    public static void generateAll(Path outDir) throws IOException {
        Files.createDirectories(outDir);

        BufferedImage big = renderBase(64);

        BufferedImage png32 = scale(big, 32, 32);
        BufferedImage png16 = scale(big, 16, 16);

        Path p32 = outDir.resolve("favicon-32x32.png");
        Path p16 = outDir.resolve("favicon-16x16.png");
        ImageIO.write(png32, "PNG", p32.toFile());
        ImageIO.write(png16, "PNG", p16.toFile());

        // Build ICO that contains both sizes (32 then 16)
        byte[] ico = buildIco(List.of(p32, p16));
        try (FileOutputStream fos = new FileOutputStream(outDir.resolve("favicon.ico").toFile())) {
            fos.write(ico);
        }
    }

    private static BufferedImage renderBase(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // background
            g.setColor(new Color(255,255,255,0)); // transparent
            g.fillRect(0,0,size,size);

            // rounded rect white fill with light border
            int pad = Math.max(2, size/12);
            RoundRectangle2D rr = new RoundRectangle2D.Float(pad, pad, size-2*pad, size-2*pad, size/6f, size/6f);
            g.setColor(Color.WHITE);
            g.fill(rr);
            g.setColor(new Color(0xDDDDEE));
            g.setStroke(new BasicStroke(Math.max(1f,size/32f)));
            g.draw(rr);

            // draw the command symbol (⌘)
            g.setColor(new Color(0x111111));
            float fontSize = size * 0.55f;
            Font font = new Font("Segoe UI Symbol", Font.PLAIN, (int)fontSize);
            g.setFont(font);
            FontRenderContext frc = g.getFontRenderContext();
            String symbol = "⌘";
            Rectangle bounds = font.getStringBounds(symbol, frc).getBounds();
            int x = (size - bounds.width) / 2 - bounds.x;
            int y = (size - bounds.height) / 2 - bounds.y;
            g.drawString(symbol, x, y);
        } finally {
            g.dispose();
        }
        return img;
    }

    private static BufferedImage scale(BufferedImage src, int w, int h) {
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(src, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }
        return dst;
    }

    private static byte[] buildIco(List<Path> pngFiles) throws IOException {
        List<byte[]> images = new ArrayList<>();
        for (Path p : pngFiles) {
            images.add(Files.readAllBytes(p));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // ICONDIR: 2 bytes reserved, 2 bytes type (1=cur,2=icon), 2 bytes count
        out.write(shortToBytesLittle((short)0));
        out.write(shortToBytesLittle((short)1));
        out.write(shortToBytesLittle((short)images.size()));

        int headerSize = 6 + 16 * images.size();
        int offset = headerSize;

        // write entries
        for (byte[] img : images) {
            BufferedImage bimg = ImageIO.read(new java.io.ByteArrayInputStream(img));
            int width = bimg.getWidth();
            int height = bimg.getHeight();
            out.write((byte)(width >= 256 ? 0 : width)); // width
            out.write((byte)(height >= 256 ? 0 : height)); // height
            out.write((byte)0); // color count
            out.write((byte)0); // reserved
            out.write(shortToBytesLittle((short)1)); // color planes
            out.write(shortToBytesLittle((short)32)); // bits per pixel
            out.write(intToBytesLittle(img.length)); // bytes in resource
            out.write(intToBytesLittle(offset)); // image offset
            offset += img.length;
        }

        // write image data
        for (byte[] img : images) {
            out.write(img);
        }

        return out.toByteArray();
    }

    private static byte[] shortToBytesLittle(short v) {
        ByteBuffer bb = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort(v);
        return bb.array();
    }

    private static byte[] intToBytesLittle(int v) {
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(v);
        return bb.array();
    }
}
