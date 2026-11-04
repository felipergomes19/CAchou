package br.com.achou.desktop.util;

import br.com.achou.desktop.api.ApiException;
import br.com.achou.desktop.ui.Ctx;
import javafx.scene.image.Image;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Cache de imagens baixadas da API e preparação (redimensionar/recortar) das fotos antes do envio. */
public final class Imagens {

    private static final long LIMITE_BYTES = 5L * 1024 * 1024;

    private static final Map<String, Image> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
            return size() > 80;
        }
    };

    private Imagens() {}

    /** Carrega uma foto do backend (com o token do usuário) e entrega na thread da interface. */
    public static void carregar(String caminho, double larguraMax, Consumer<Image> aoCarregar) {
        String chave = caminho + "@" + (int) larguraMax;
        Image emCache = CACHE.get(chave);
        if (emCache != null) {
            aoCarregar.accept(emCache);
            return;
        }
        Async.run(() -> {
            byte[] bytes = Ctx.api.baixar(caminho);
            return new Image(new ByteArrayInputStream(bytes), larguraMax, 0, true, true);
        }, img -> {
            if (!img.isError()) {
                CACHE.put(chave, img);
                aoCarregar.accept(img);
            }
        }, erro -> { /* foto indisponível: mantém o placeholder */ });
    }

    /**
     * Converte o arquivo escolhido em JPEG com no máximo {@code maxLado} pixels.
     * Se {@code quadrado}, recorta o centro (ideal para foto de perfil).
     * Formatos que o Java não lê (ex.: WebP) são enviados como estão, se couberem no limite.
     */
    public static byte[] preparar(File arquivo, int maxLado, boolean quadrado) throws IOException {
        BufferedImage origem = ImageIO.read(arquivo);
        if (origem == null) {
            byte[] bruto = Files.readAllBytes(arquivo.toPath());
            if (bruto.length > LIMITE_BYTES) {
                throw new ApiException(0, "A imagem é grande demais (máximo 5 MB).");
            }
            return bruto;
        }
        int sx = 0, sy = 0, sw = origem.getWidth(), sh = origem.getHeight();
        if (quadrado) {
            int lado = Math.min(sw, sh);
            sx = (sw - lado) / 2;
            sy = (sh - lado) / 2;
            sw = lado;
            sh = lado;
        }
        double escala = Math.min(1.0, (double) maxLado / Math.max(sw, sh));
        int w = Math.max(1, (int) Math.round(sw * escala));
        int h = Math.max(1, (int) Math.round(sh * escala));

        BufferedImage saida = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = saida.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(origem, 0, 0, w, h, sx, sy, sx + sw, sy + sh, null);
        } finally {
            g.dispose();
        }
        return jpeg(saida, 0.85f);
    }

    private static byte[] jpeg(BufferedImage img, float qualidade) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(baos)) {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(qualidade);
            writer.setOutput(out);
            writer.write(null, new IIOImage(img, null, null), param);
            out.flush();
            return baos.toByteArray();
        } finally {
            writer.dispose();
        }
    }
}
