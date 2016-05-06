package guessthenextword.util;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class ImageWindow extends JFrame{
	
	private static final long serialVersionUID = 7129743392995707784L;

	//==> Fields
	
	private static final int x = 100, y = 100, width = 670, height = 500, offset=4;
	private BufferedImage image;


	
	//==> Constructors
	
	public ImageWindow(int id) {
		setResizable(false);
		initialize(id);
	}//ImageWindow
	
	public ImageWindow(int id, BufferedImage img) {
		this.image = img;
		//
		setResizable(false);
		initialize(id);
	}//ImageWindow


	
	//==> Methods
	
	public void setImage( BufferedImage img ){
		this.image = img;
	}//setImage
	
	private void initialize(int id) {
		setBounds(x+id*(width+offset), y, width, height);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}//initialize
	
	@Override
	public void paint(Graphics g){
		super.paint(g);
		if( image != null ){
			((Graphics2D)g).drawImage(image, null, 0, 0);
		}
	}//paint

}//ImageWindow
