package d021248.cfl;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.net.URL;


import javax.imageio.ImageIO;
import javax.swing.JComponent;

public class CfLogo {

	private static float MAX = 0.35f;
	private static float MIN = 0.00f;
	private static float STEPS = 400;
	
	private final BufferedImage image;
	private final JComponent component;
	
	private float alpha;
	
	public CfLogo(String imageURL, JComponent component ) {
		this.image = readImage(imageURL);
		this.component = component;
		this.start();
	}
		
	protected BufferedImage readImage(String imageURL) {
		try {
			return ImageIO.read(new URL(imageURL));
		} catch (Exception e) {
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

	protected void start() {
		new Thread(() -> {

			float delta = (MAX - MIN) / STEPS;
			while (true) {
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
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();		
	}

}
