package d021248.cfl.ui;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JComponent;

public class CfLogo implements Runnable {

    private static final float MAX = 0.35f;
    private static final float MIN = 0.00f;
    private static final float STEPS = 400f;

    private static final String DEFAULT_LOGO = "D021248.png";

    private final BufferedImage image;
    private final JComponent component;

    private float alpha;
    private boolean isExitRequested;

    public CfLogo(JComponent component) {
        this(component, CfLogo.class.getResource(DEFAULT_LOGO).toString());
    }

    public CfLogo(JComponent component, String imageURL) {
        this.image = readImage(imageURL);
        this.component = component;
        this.isExitRequested = false;
    }

    protected BufferedImage readImage(String imageURL) {
        try {
            return ImageIO.read(new URL(imageURL));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void paintLogo(Graphics2D g2d) {
        var composite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        var y = component.getRootPane().getHeight() - image.getHeight();
        var x = component.getRootPane().getWidth() - image.getWidth();
        g2d.drawImage(image, x, y, component);
        g2d.setComposite(composite);
    }

    @Override
    public void run() {
        var delta = (MAX - MIN) / STEPS;
        while (!isExitRequested) {
            try {
                alpha = alpha + delta;
                if (alpha >= MAX) {
                    delta = -delta;
                    alpha = MAX;
                    Thread.sleep(2000);
                }

                if (alpha <= MIN) {
                    delta = -delta;
                    alpha = MIN;
                    Thread.sleep(8000);
                }
                component.repaint();
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void start() {
        new Thread(this).start();
    }

    public void exit() {
        this.isExitRequested = true;
    }
}
