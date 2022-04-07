
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;

import java.awt.*;


import game2D.*;

// Game demonstrates how we can override the GameCore class
// to create our own 'game'. We usually need to implement at
// least 'draw' and 'update' (not including any local event handling)
// to begin the process. You should also add code to the 'init'
// method that will initialise event handlers etc. By default GameCore
// will handle the 'Escape' key to quit the game but you should
// override this with your own event handler.

/**
 * @author David Cairns
 *
 */
@SuppressWarnings("serial")

public class Game extends GameCore 
{
	// Useful game constants
	static int screenWidth = 512;
	static int screenHeight = 384;

    //float lift = 0.005f;
    float	gravity = 0.0001f;

    // Game resources
    Sprite	player = null;
    TileMap tmap = new TileMap();
	LinkedList<Sprite> parallaxBg = new LinkedList<>();
	private Animation warriorRun, warriorIdle, warriorJump, warriorFall, warriorAttack, warriorAttack2, warriorHurt, warriorDeath;
    private int level = 1;
	
	private boolean keyLeft;
	private boolean keyUp;
	private boolean keyRight;
	private boolean keyDown ;
	
	private int xo = 0;
	int cameraX = 0;
	
    /**
	 * The obligatory main method that creates
     * an instance of our class and starts it running
     * 
     * @param args	The list of parameters this program might use (ignored)
     */
    public static void main(String[] args) {

        Game gct = new Game();
        gct.init();
        // Start in windowed mode with the given screen height and width
        gct.run(false,screenWidth,screenHeight);
    }

    /**
     * Initialise the class, e.g. set up variables, load images,
     * create animations, register event handlers
     */
    public void init()
    {         
    	// Temporary reference to background sprites
        Sprite bg1, bg2, bg3, bg4, bg5, bg6, bg7, bg8;	

        // Load the tile map and print it out so we can check it is valid
        tmap.loadMap("maps", "map.txt");
        setSize(tmap.getPixelWidth()/4, tmap.getPixelHeight());
        setVisible(true);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("The Venerer [Level " + level + "]");
        setLocationRelativeTo(null);
        
        //Warrior animations
        warriorIdle = new Animation();
        warriorRun = new Animation(); 
        warriorJump = new Animation(); 
        warriorFall = new Animation(); 
        warriorAttack = new Animation(); 
        warriorAttack2 = new Animation(); 
        warriorHurt = new Animation(); 
        warriorDeath = new Animation(); 
        
        /*Initialise the player with an animation
        Huntress imagesheets obtained from https://luizmelo.itch.io/huntress */
        warriorIdle.loadAnimationFromSheet("images/characters/huntress/Idle.png", 8, 1, 100);
        warriorRun.loadAnimationFromSheet("images/characters/huntress/Run.png", 8, 1, 100);
        warriorJump.loadAnimationFromSheet("src/images/characters/huntress/Jump.png", 2, 1, 100);
        warriorFall.loadAnimationFromSheet("images/characters/huntress/Fall.png", 2, 1, 100);
        warriorAttack.loadAnimationFromSheet("images/characters/huntress/Attack1.png", 5, 1, 100);
        warriorAttack2.loadAnimationFromSheet("images/characters/huntress/Attack2.png", 5, 1, 100);
        warriorHurt.loadAnimationFromSheet("images/characters/huntress/Hit.png", 3, 1, 100);
        warriorDeath.loadAnimationFromSheet("images/characters/huntress/Death.png", 8, 1, 100);

        player = new Sprite(warriorIdle);
        player.setScale(1.5f);
        player.setOffsets(-35, -35);
        
        Animation background1 = new Animation();
        Animation background2 = new Animation();
        Animation background3 = new Animation();
        Animation background4 = new Animation();
        
  		//Parallax BG vector images from https://raventale.itch.io/parallax-background
    	background1.loadAnimationFromSheet("images/background/Sky.png", 1, 1, 1000);
    	background2.loadAnimationFromSheet("images/background/Moon.png", 1, 1, 1000);
    	background3.loadAnimationFromSheet("images/background/Mountains.png", 1, 1, 1000);
    	background4.loadAnimationFromSheet("images/background/Desert.png", 1, 1, 1000);

		bg1 = new Sprite(background1);
		bg2 = new Sprite(background1);
		bg3 = new Sprite(background2);
		bg4 = new Sprite(background2);
		bg5 = new Sprite(background3);
		bg6 = new Sprite(background3);
		bg7 = new Sprite(background4);
		bg8 = new Sprite(background4);

		parallaxBg.add(bg1);
		parallaxBg.add(bg2);
		parallaxBg.add(bg3);
		parallaxBg.add(bg4);
		parallaxBg.add(bg5);
		parallaxBg.add(bg6);
		parallaxBg.add(bg7);
		parallaxBg.add(bg8);
		
		//Scales size of background images
		int i = 1;
		for(Sprite bg : parallaxBg){
			//scale to frame
			float scaleX = (float) (Game.screenWidth + 5) / 1920;
			float scaleY = (float) Game.screenHeight / 1080;
			bg.setScale(scaleX, scaleY);
			if(i % 2 == 0) {
				bg.setX(screenWidth);
			}
			i+=1;
			bg.show();
		}

		initialiseGame();  
    }

    /**
     * You will probably want to put code to restart a game in
     * a separate method so that you can call it to restart
     * the game.
     */
    public void initialiseGame()
    {
        player.setY(210f);
        player.setX(20f);
        player.setVelocityX(0);
        player.setVelocityY(0);
        player.show();
    }
    
    /**
     * Draw the current state of the game
     */
    public void draw(Graphics2D g)
    {    	
    	// Be careful about the order in which you draw objects - you
    	// should draw the background first, then work your way 'forward'

    	// First work out how much we need to shift the view 
    	// in order to see where the player is.


        // If relative, adjust the offset so that
        // it is relative to the player

        // ...?
        
        g.setColor(Color.white);
        g.fillRect(0, 0, getWidth(), getHeight());
        

        int backGroundNum = 1;
        
        //Used to add movement direction to background 
        int direction;
        if(keyRight) direction = 1;
        else direction = -1;
        
        //For each background (2 Sprites are required for each of the 4 images)
        for(Sprite bg : parallaxBg) {
        	bg.drawTransformed(g);
        	//Check character is moving left or right
        	if(keyRight || keyLeft) {
        		//Sky and moon background do not move.
				if (backGroundNum > 4) {
					if (backGroundNum > 6) {
						//Parallax background, Desert moves faster.
						bg.setX(bg.getX() - (3f * direction));
					} else {
						//Parallax background, mountains move slower.
						bg.setX(bg.getX() - (1f * direction));
					}
					if(keyRight) {
						/*If character moves right and background image position
						 *goes fully off screen left then set it to start after the second duplicate image on screen
						 *Screen width has an additional number in setX to avoid gap in drawing.*/
						if (bg.getX() < -screenWidth) bg.setX(screenWidth - 5);
					}else {
						//Same applies here except opposite direction 
						if (bg.getX() > screenWidth) bg.setX(-screenWidth + 5);	
					}
					
				}
				backGroundNum += 1;
				continue;
			}
        }

        // Apply offsets to player and draw 
        
        
        player.drawBoundingBox(g);
        player.drawBoundingCircle(g);
        
//        if(player.getX() < 15) {
//        	tmap.draw(g,0,0); 
//        }else {
//        	  var targetX = player.getX() - tmap.getMapWidth()/2 + player.getWidth()/2;
//        	  xo += (targetX - xo) * 0.1;
//        	  tmap.draw(g,xo ,0); 
//        }



          tmap.draw(g, 0 ,0); 
          player.drawTransformed(g);
        
    }

    /**
     * Update any sprites and check for collisions
     * 
     * @param elapsed The elapsed time between this call and the previous call of elapsed
     */    
    public void update(long elapsed)
    {
    	
	
       	player.setAnimationSpeed(1.0f);
       	
        // Make adjustments to the speed of the sprite due to gravity
       	if(keyDown) {
       		player.setVelocityY(player.getVelocityY()+(gravity*elapsed));  	
       	}
       	//move();
       	handleScreenEdge(player, tmap, elapsed);
        // Now update the sprites animation and position
        player.update(elapsed);
       
        // Then check for any collisions that may have occurred
        //handleScreenEdge(player, tmap, elapsed);
        //checkTileCollision(player, tmap);
    }
    
    
    /**
     * Checks and handles collisions with the edge of the screen
     * 
     * @param s			The Sprite to check collisions for
     * @param tmap		The tile map to check 
     * @param elapsed	How much time has gone by since the last call
     */
    public void handleScreenEdge(Sprite s, TileMap tmap, long elapsed)
    {
//  	  if (player.getX() < 0){
//  	    player.setX(0);
//  	    player.setVelocityX(-player.getVelocityX());
//  	  }
//  	  if (player.getX() > tmap.getMapWidth() - player.getWidth()){
//  		player.setX(tmap.getMapWidth() - player.getWidth());
//  		player.setVelocityX(-player.getVelocityX());
//  	  }
    }
    
    
     


    public boolean boundingBoxCollision(Sprite s1, Sprite s2)
    {
		int dx,dy,minimum;   
		  
		dx = (int) (s1.getX() - s2.getX()); 
		dy = (int) (s1.getY() - s2.getY()); 
		minimum = (int) (s1.getRadius() + s2.getRadius()); 
		    
		return (((dx * dx) + (dy * dy)) < (minimum * minimum));
   }
    
    /**
     * Check and handles collisions with a tile map for the
     * given sprite 's'. Initial functionality is limited...
     * 
     * @param s			The Sprite to check collisions for
     * @param tmap		The tile map to check 
     */

    public void checkTileCollision(Sprite s, TileMap tmap)
    {    	
    	float sx = s.getX() + s.getRadius()/4;
    	float sy = s.getY() + s.getRadius()/4; 
    	int xtile, ytile;
    	double x, y;
    	
    	for(int angle = 0; angle < 6; angle++) {
    		x = sx + (s.getRadius()/4 * Math.cos(Math.toRadians(angle * 60)));
    		y = sy + (s.getRadius()/4 * Math.sin(Math.toRadians(angle * 60)));
    		// Find out how wide and how tall a tile is
        	xtile = (int)(x /  tmap.getTileWidth());
        	ytile = (int)(y / tmap.getTileHeight());

    		switch(tmap.getTileChar(xtile, ytile)) {
	    		case '.':
	    			if(tmap.getTileChar((int) (x/  tmap.getTileWidth()), (int)s.getY() + s.getHeight()/ tmap.getTileHeight()) == '.'){
	    				keyDown = true;
	    			}
	    			continue;
	    			
	    		default:
	    			continue;
	    		case 'b':
	    			keyDown = false;
	    			s.setVelocityY(0f);
	    			continue;
	    		case 'f':
	    		case 't':
	    			System.out.println("Collision");
	        		//TODO handleCollison(s);
	    	        return;
    		}
        }
    }

    /**
     * Override of the keyPressed event defined in GameCore to catch our
     * own events
     * 
     *  @param e The event that has been generated
     */
    public void keyPressed(KeyEvent e) 
    { 
    	switch(e.getKeyCode()){
	    	case KeyEvent.VK_A:
	    		keyLeft = true;
	    		move();
	    		return;	
	    	case KeyEvent.VK_W:
	    		keyUp = true;
	    		move();
	    		return;
	    	case KeyEvent.VK_S:
	    		keyDown = true;
	    		move();
	    		return;	
	    	case KeyEvent.VK_D:
	    		keyRight = true;
	    		move();
	    		return;
	    	case KeyEvent.VK_SPACE:	
	    		//player.setAnimation(warriorJump);
	    		player.setAnimation(warriorAttack2);
	    		return;
    	}
    }
    
    public void move() {
    	if(keyLeft && keyRight || !keyLeft && !keyRight) {
    		player.setAnimation(warriorIdle);
    		player.setVelocityX(0f); return;
    	}
    	if(keyLeft && !keyRight) {
    		player.setAnimation(warriorRun);
    		player.setScale(-1.5f, 1.5f);
    		player.setVelocityX(-0.08f);
    	} else if(keyRight && !keyLeft) { 
    		player.setAnimation(warriorRun);
    		player.setScale(1.5f, 1.5f);
    		player.setVelocityX(0.08f);
    	}
//		  if (player.getX() < 0){
//			    player.setX(0);
//			    player.setVelocityX(-player.getVelocityX());
//			  }
//		  if (player.getX() > tmap.getMapWidth() - player.getWidth()){
//			player.setX(tmap.getMapWidth() - player.getWidth());
//			player.setVelocityX(-player.getVelocityX());
//		  }
//		  
//          var targetX = player.getX() - screenWidth/2 + player.getX()/2;
//          
//          targetX = Math.min(tmap.getMapWidth()/4 - screenWidth, Math.max(0, (int) targetX));
//          
//          cameraX += (targetX - cameraX) * 0.1;      
    }
    /**
     * Override of the keyReleased event defined in GameCore to catch our
     * own events
     * 
     *  @param e The event that has been generated
     */
	public void keyReleased(KeyEvent e) { 
		
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_ESCAPE : System.exit(0);
			case KeyEvent.VK_A: 
				keyLeft = false;
				player.setAnimation(warriorIdle);
				player.setVelocityX(0.0f);
				return;
			case KeyEvent.VK_W:
				keyUp = false;
				player.setAnimation(warriorIdle);
				return;
			case KeyEvent.VK_S: 
				keyDown = false;
				player.setAnimation(warriorIdle);
				return;
			case KeyEvent.VK_D:
				keyRight = false;
				player.setAnimation(warriorIdle);
				player.setVelocityX(0.0f);
				return;
		}
		
	}
}
