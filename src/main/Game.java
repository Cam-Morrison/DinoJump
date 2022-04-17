package main;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

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

public class Game extends GameCore implements MouseListener{ 
	// Useful game constants
	static int screenWidth = 512;
	static int screenHeight = 384;

	// float lift = 0.005f;
	float gravity = 0.001f;

	// Game resources
	Sprite player = null;
	TileMap tmap = new TileMap();
	LinkedList<Sprite> parallaxBg = new LinkedList<>();
	private Animation idle, run, jump;
	private int level = 1;

	private boolean keyLeft;
	private boolean keyRight;

	private int score = 0;

	private boolean debugMode = false;
	private boolean screenEdge;
	private ConcurrentLinkedQueue<Sprite> coins;
	private int offsetX = 0;
	private int cameraMovementVal = 0;
	private int rotation;
	private Sprite astroid;
	private Animation dead;
	private boolean playerDead;
	private Image playBtn;
	
	private enum gameStage {
		INTRO,
		PLAY,
		FINISHED
	}
	private gameStage state = gameStage.INTRO;
	private Image background;
	private Image audioBtn;
	private Image muteBtn;
	private Sprite enemy;
	private Animation enemySpawn;
	private Animation enemyIdle;
	private Animation enemyWalk;
	private Animation enemyAttack;
	private Animation enemyDeath;
	private boolean enemyDead;
	private boolean enemySpawnedIn;
	private boolean soundBool = true;
	private Sound music;
	
	private boolean levelComplete;
	private Animation astroidAnim;
	private Animation explosion;
	private Animation coinAnim;

	/**
	 * The obligatory main method that creates an instance of our class and starts
	 * it running
	 * 
	 * @param args The list of parameters this program might use (ignored)
	 */
	public static void main(String[] args) {

		Game gct = new Game();
		gct.init();
		// Start in windowed mode with the given screen height and width
		gct.run(true, screenWidth, screenHeight);
		

	}

	/**
	 * Initialise the class, e.g. set up variables, load images, create animations,
	 * register event handlers
	 */
	public void init() {

		// Load the tile map and print it out so we can check it is valid
		tmap.loadMap("maps", "map.txt");
		setSize(tmap.getPixelWidth() / 4, tmap.getPixelHeight());
		setVisible(true);
		setResizable(false);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setTitle("Dino Jump");
		setLocationRelativeTo(null);
		addMouseListener(this);
		ImageIcon img = new ImageIcon("images/interface/gameCover.png");
		setIconImage(img.getImage());
		
		playBtn = null;
		try {
			playBtn = ImageIO.read(new File("images/interface/PlayButton.png"));
			playBtn = playBtn.getScaledInstance(screenWidth/3, screenHeight/4, Image.SCALE_SMOOTH);
			background = ImageIO.read(new File("images/interface/background.jpg"));
			background = background.getScaledInstance(screenWidth, screenHeight, Image.SCALE_SMOOTH);	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//AUDIO
		audioBtn = loadImage("images/interface/sound.png");
		muteBtn = loadImage("images/interface/mute.png");
		
		music = new Sound("sounds/dino.mid");
		music.start();
		
		
		// Player character
		idle = new Animation();
		run = new Animation();
		jump = new Animation();
		dead = new Animation();
	
		/*
		 * Dinosaur pngs from https://www.gameart2d.com/free-dino-sprites.html I had to
		 * crop white space, reduce size and combine individual images into
		 * sprite-sheets.
		 */
		idle.loadAnimationFromSheet("images/characters/dinosaur/idle.png", 3, 3, 100);
		run.loadAnimationFromSheet("images/characters/dinosaur/run.png", 3, 2, 91);
		jump.loadAnimationFromSheet("images/characters/dinosaur/jump.png", 6, 1, 100);
		dead.loadAnimationFromSheet("images/characters/dinosaur/dead.png", 8, 1, 80);
		player = new Sprite(idle);
		
		// Enemy character
		enemySpawn = new Animation();
		enemyIdle = new Animation();
		enemyWalk = new Animation();
		enemyAttack = new Animation();
		enemyDeath = new Animation();
		enemySpawn.loadAnimationFromSheet("images/characters/Skeleton_Seeker/spawn.png", 1, 11, 120);
		enemyIdle.loadAnimationFromSheet("images/characters/Skeleton_Seeker/idle.png", 1, 6, 100);
		enemyWalk.loadAnimationFromSheet("images/characters/Skeleton_Seeker/walk.png", 1, 6, 100);
		enemyAttack.loadAnimationFromSheet("images/characters/Skeleton_Seeker/attack.png", 1, 10, 100);
		enemyDeath.loadAnimationFromSheet("images/characters/Skeleton_Seeker/death.png", 1, 5, 200);

		
		astroidAnim = new Animation();
		explosion = new Animation();
		astroidAnim.loadAnimationFromSheet("images/characters/astroid.png", 1, 1, 1000);
		explosion.loadAnimationFromSheet("images/explosion.png", 8, 6, 100);
		astroid = new Sprite(astroidAnim);

		// Temporary reference to background sprites
		Sprite bg1, bg2, bg3, bg4, bg5, bg6, bg7, bg8;

		Animation background1 = new Animation();
		Animation background2 = new Animation();
		Animation background3 = new Animation();
		Animation background4 = new Animation();

		// Parallax BG vector images from https://raventale.itch.io/parallax-background
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

		// Scales size of background images
		int i = 1;
		for (Sprite bg : parallaxBg) {
			// scale to frame
			float scaleX = (float) (Game.screenWidth + 5) / 1920;
			float scaleY = (float) Game.screenHeight / 1080;
			bg.setScale(scaleX, scaleY);
			if (i % 2 == 0) {
				bg.setX(screenWidth);
			}
			i += 1;
			bg.show();
		}

		initialiseGame();
	}

	/**
	 * You will probably want to put code to restart a game in a separate method so
	 * that you can call it to restart the game.
	 */
	public void initialiseGame() {
		if(level == 1) {
			score = 0;
		}else if(level == 2) {
			tmap.loadMap("maps", "map2.txt");
		} else {
			state = gameStage.FINISHED;
		}
		
		keyLeft = false;
		keyRight = false;
		playerDead = false;
		enemyDead = false;
		enemySpawnedIn = false;
		screenEdge = false;
		levelComplete = false;
		
		offsetX = 0;
		
		// ConcurrentLinked queue is used to avoid ConcurrentModificationException
		coins = new ConcurrentLinkedQueue<Sprite>();
		for (int i = 0; i < (int)tmap.getPixelWidth()/100; i++) {
			coinAnim = new Animation();
			coinAnim.loadAnimationFromSheet("images/items/coin/coin.png", 6, 1, 100);
			Sprite coin = new Sprite(coinAnim);
			coin.setX(100 * i);
			coin.setY((int) 50 + (int)(Math.random() * (screenHeight - 100)));
			coin.show();
			coins.add(coin);
		}
		
//		enemies = new ConcurrentLinkedQueue(Sprite)();
//		for(int i = 0; i < 3; i++) {
//			Sprite enemy = new Sprite(enemyIdle);
//			
//		}
		
		//TODO MOVE METHODS FROM INIT TO HERE FOR RESTART
		player.setAnimation(idle);
		player.setPosition(50, screenHeight - 100); // 64 character height and 32 (1 tile)
		player.setVelocityX(0);
		player.setVelocityY(0);
		player.show();
		
		astroid.setAnimation(astroidAnim);
		astroid.hide();
		enemySpawn();
		
		soundControl(new Sound("levelUp.wav"));
	}
	
	public void enemySpawn() {
		enemy.setAnimation(enemySpawn);
		enemy.setPosition(300, screenHeight - 76);
		enemy.setVelocity(0, 0);
		soundControl(new Sound("sounds/roar.mp3"));
		enemy.show();
	}
	
	public int getXcenteredText(String text, Graphics2D g) {
		int length = (int) g.getFontMetrics().getStringBounds(text, g).getWidth();
		int x = screenWidth/2 - length/2;
		return x;
	}
	public int getXcenterImage(Image img) {
		return (int)(screenWidth/2 - img.getWidth(null)/2);
	}
	/**
	 * Draw the current state of the game
	 */
	public void draw(Graphics2D g) {
		if(state == gameStage.INTRO) {
			//Java AWT improved rendering
			Map<?, ?> desktopHints = (Map<?, ?>) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
			if (desktopHints != null) {
			    g.setRenderingHints(desktopHints);
			}
			g.setFont(g.getFont().deriveFont(Font.BOLD, 80));
			String title = "Dino Jump";
			int x = getXcenteredText(title, g);
			int y = (int)(screenHeight/2.5);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
			g.drawImage(background, 0, 0, null);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
			g.setColor(Color.BLACK);
			g.drawString(title, x, y);
			g.setColor(Color.WHITE);
			g.drawString(title, x+3, y+3);
			g.setStroke(new BasicStroke(WIDTH));
			x = getXcenterImage(playBtn);
			g.drawImage(playBtn, x, 200, null);
			g.setColor(Color.BLACK);
			g.setFont(g.getFont().deriveFont(Font.ITALIC, 16));
			title = "Student 2831609";
			x = getXcenteredText(title, g);
			g.drawString(title, x, screenHeight-30);
			g.setColor(Color.WHITE);
			g.drawString(title, x + 1, screenHeight-29);
			
			if(soundBool) {
				g.drawImage(audioBtn, screenWidth - 100, 40, null);
			}else {
				g.drawImage(muteBtn, screenWidth - 100, 40, null);
			}
			
		} else if(state == gameStage.PLAY){
			
			g.setFont(new Font("Verdana", Font.BOLD, 16));
			g.fillRect(0, 0, getWidth(), getHeight());
	
			int backGroundNum = 1;
	
			// Used to add movement direction to background
			int direction;
			if (keyRight)
				direction = 1;
			else
				direction = -1;
	
			// For each background (2 Sprites are required for each of the 4 images)
			for (Sprite bg : parallaxBg) {
				bg.drawTransformed(g);
				// Check character is moving left or right
				if (keyRight || keyLeft) {
					// If both are pressed background does not move as neither does Sprite
					if (!screenEdge) {
						if (keyRight && keyLeft)
							continue;
						// Sky and moon background do not move.
						if (backGroundNum > 4) {
							if (backGroundNum > 6) {
								// Parallax background, Desert moves faster.
								bg.setX(bg.getX() - (4f * direction));
							} else {
								// Parallax background, mountains move slower.
								bg.setX(bg.getX() - (1f * direction));
							}
							if (keyRight) {
								/*
								 * If character moves right and background image position goes fully off screen
								 * left then set it to start after the second duplicate image on screen Screen
								 * width has an additional number in setX to avoid gap in drawing.
								 */
								if (bg.getX() < -screenWidth)
									bg.setX(screenWidth - 5);
							} else {
								// Same applies here except opposite direction
								if (bg.getX() > screenWidth)
									bg.setX(-screenWidth + 5);
							}
	
						}
						backGroundNum += 1;
						continue;
					}
				}
			}

			cameraMovementVal = 0;
			
			if (player.getX() > screenWidth / 2 - player.getWidth()) {
				if (player.getScaleX() == 1f) {
					if(offsetX > (-tmap.getPixelWidth() + screenWidth)) {
						player.setX(screenWidth / 2 - player.getWidth());
						cameraMovementVal = 1 + (int) (player.getVelocityX() * 20);
						offsetX = offsetX - cameraMovementVal;
					}
				}
			} else {
				if (player.getX() < screenWidth / 2 - player.getWidth()) {
					if (player.getScaleX() == -1f && offsetX < 0) {
						player.setX(screenWidth / 2 - player.getWidth());
						cameraMovementVal =  -1 + (int) (player.getVelocityX() * 20);
						offsetX = offsetX + (offsetX - (cameraMovementVal + offsetX)); // +cam
					}
				}
			}

			player.drawTransformed(g);
			astroid.setRotation(rotation);
			astroid.drawTransformed(g);
			for (Sprite c : coins) {
				c.setX(c.getX() + (-cameraMovementVal));
				c.drawTransformed(g);
			}
			enemy.setX(enemy.getX()  + (-cameraMovementVal));
			astroid.setX(astroid.getX() + (-cameraMovementVal));
			enemy.drawTransformed(g);
			
			tmap.draw(g, offsetX, 0);
			// Show score and status information
			String msg = String.format("Score: %d", score / 100);
			g.setColor(Color.BLACK);
			g.drawString(msg, screenWidth - 123, 50);
			g.setColor(Color.WHITE);
			g.drawString(msg, screenWidth - 120, 50);

			if (debugMode) {
				g.setColor(Color.BLACK);
				g.drawString("DEBUG MODE", getXcenteredText("DEBUG MODE", g), screenHeight-10);
				g.setColor(Color.white);
				//player.drawBoundingBox(g);
				player.drawBoundingCircle(g);
				player.drawBoundingBox(g);
				String debug = "FPS: " + (int) getFPS();
				g.drawString(debug, 40, 50);
				g.drawString("Camera:" + offsetX, screenWidth/2, screenHeight/2);
				g.drawString("X:" + (int) (player.getX() + (-offsetX)) + ", Y:" + (int) player.getY(), player.getX(), player.getY());
				for (Sprite c : coins) {
					c.drawBoundingCircle(g);
				}
				g.setColor(Color.red);
				astroid.drawBoundingBox(g);
				enemy.drawBoundingBox(g);
				g.drawLine(screenWidth/2, 0, screenWidth/2, screenHeight);
			} else {
				g.setColor(Color.black);
				if(playerDead == false) {
					msg = coins.size() + " coins remaining!";
					g.drawString(msg, getXcenteredText(msg,g)-2, screenHeight-10);
					g.setColor(Color.white);
					g.drawString(msg, getXcenteredText(msg,g), screenHeight-10);
				}else {
					msg = "Click to play again";
					g.drawString(msg, getXcenteredText(msg,g)-2, screenHeight-10);
					g.setColor(Color.white);
					g.drawString(msg, getXcenteredText(msg,g), screenHeight-10);
				}
			}
			if(playerDead) {
				g.setColor(Color.black);
				g.setFont(g.getFont().deriveFont(Font.BOLD, 50));
				int x = getXcenteredText("GAME OVER", g);
				g.drawString("GAME OVER", x, screenHeight/3);
				g.setColor(Color.white);
				g.drawString("GAME OVER", x + 3, screenHeight/3 + 3);
				x = getXcenterImage(playBtn);
				g.drawImage(playBtn, x, 150, null); 
			}			
		} else {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, screenWidth, screenHeight);
			g.setFont(g.getFont().deriveFont(Font.BOLD, 20));
			g.setColor(Color.white);
			String title = "Congrulations! Your score was: " + score / 100;
			int x = getXcenteredText(title, g);
			int y = (int)(screenHeight/2.5);
			g.drawString(title, x, y);
			x = getXcenterImage(playBtn);
			g.drawImage(playBtn, x, 200, null); 	
		}
	}

	/**
	 * Update any sprites and check for collisions
	 * 
	 * @param elapsed The elapsed time between this call and the previous call of
	 *                elapsed
	 */
	public void update(long elapsed) {
		if(state == gameStage.PLAY) {
			player.setAnimationSpeed(1.0f);
			
			player.update(elapsed);
			enemy.update(elapsed);
			
			if(levelComplete) 
				astroidControl(elapsed);
			
			if (playerDead == false) {	
				// Make adjustments to the speed of the sprite due to gravity
				player.setVelocityY(player.getVelocityY() + (gravity * elapsed));
				
				if(enemySpawnedIn == false) {
					enemySpawnIn();
				}
				if(enemyDead == false && boundingBoxCollision(player, enemy)) {
					if(player.getY() < enemy.getY() && player.getX() > enemy.getX() && 
					   player.getX() + player.getWidth()/2 < enemy.getX() + enemy.getWidth()) {
						jumpyDinoKO(enemy);
					}else {
						enemyAttack();
					}
				}
				
				if(boundingBoxCollision(player, astroid)){
					gameOver();
				}
				
				checkCoinCollision(elapsed);
			}

			// Then check for any collisions that may have occurred
			handleScreenEdge(player, tmap);
			handleScreenEdge(enemy, tmap);
			checkTileCollision(player, tmap);
			checkTileCollision(enemy, tmap);
		}
	}
	
	private void astroidControl(long elapsed) {
		astroid.update(elapsed);
		if(astroid.getX() > player.getX()) {
			astroid.setVelocityX(astroid.getVelocityX() - 0.001f);	
		}else {
			astroid.setVelocityX(astroid.getVelocityX() + 0.001f);
		}
		if(astroid.getY() > 300) {
			astroid.stop();
			astroid.setAnimation(explosion);
			astroid.playAnimation();
			astroid.pauseAnimationAtFrame(20);
			if(playerDead == false) {				
				astroid.playAnimation();
				jumpyDinoKO(enemy);
				dinoDeathAnim();
        		level++;
			}
		}else {
			rotation++;
		}
	}

	private void enemySpawnIn() {
		enemy.show();	
		enemySpawn.setAnimationFrame(0);
		enemy.setAnimation(enemySpawn);
		enemy.playAnimation();
		soundControl(new Sound("sounds/roar.wav"));
		enemy.pauseAnimationAtFrame(10);
		enemy.setScale(-1f, 1f);
		TimerTask task = new TimerTask() {
			public void run() {
				//Fixes an error where player comes back alive if killed before movement
				if(enemyDead == false) {
					if(enemy.getX() < player.getX()) {
						enemy.setVelocityX(0.05f);
						enemy.setScale(1f, 1f);
					}else {
						enemy.setVelocityX(-0.05f);
						enemy.setScale(-1f, 1f);
					}
					enemy.setAnimation(enemyWalk);
				}
			}
		};
		Timer timer = new Timer("Timer");
		long delay = 5000l;
		timer.schedule(task, delay);
		enemySpawnedIn = true;
	}
	
	private void checkCoinCollision(Long elapsed) {
		for (Sprite c : coins) {
			c.update(elapsed);
			if (boundingBoxCollision(c, player)) {
				soundControl(new Sound("sounds/coin.wav"));
				score += 500;
				coins.remove(c);
				checkLevelComplete();
			}
		}
	}
	private void dinoDeathAnim() {
		keyLeft = false;
		keyRight = false;
		player.stop();
		player.setVelocityY(0.3f);
		player.shiftX(10f); 
		player.setAnimation(dead);
		player.setAnimationFrame(0);
		player.playAnimation();
		player.pauseAnimationAtFrame(7);
		playerDead = true;
		gameOver();

	}
	
	private void enemyAttack() {
		if(enemy.getX() > player.getX()) {
			enemy.setScale(-1f, 1f);
		}else {
			enemy.setScale(1f, 1f);
		}
		enemy.stop();
		soundControl(new Sound("sounds/roar.wav"));
		enemy.setAnimation(enemyAttack);
		enemyAttack.setAnimationFrame(0);
		enemy.playAnimation();
		dinoDeathAnim();
	}

	private void gameOver() {
		addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {	
            	if(playerDead == true) {
            		initialiseGame();
            	}
            }
        });
	}

	public void move() {
		if (keyLeft && !keyRight) {
			player.setAnimation(run);
			player.setVelocityX(-0.08f);
			if(screenEdge != true)
				player.setScale(-1f, 1f);
		} else if (keyRight && !keyLeft) {
			player.setAnimation(run);
			player.setVelocityX(0.08f);
			if(screenEdge != true)
				player.setScale(1f, 1f);
		}
	}

	/**
	 * Checks and handles collisions with the edge of the screen
	 * 
	 * @param s       The Sprite to check collisions for
	 * @param tmap    The tile map to check
	 * @param elapsed How much time has gone by since the last call
	 */
	public void handleScreenEdge(Sprite s, TileMap tmap) {
		// if player too low
		if ((s.getY() + s.getHeight() + 32) > tmap.getPixelHeight()) {
			if(s.getAnimation() == enemyWalk || s.getAnimation() == enemySpawn || s.getAnimation() == enemyDeath) {
				s.setY(tmap.getPixelHeight() - (s.getHeight() + 18));
			}else {
				s.setY(tmap.getPixelHeight() - (s.getHeight() + 32) + 1);
			}
        }
		// If player goes to left corner
		if (s.getX() + -offsetX < 0) {
			s.setX(1);
			s.setVelocityX(-s.getVelocityX());
			s.setScale(1f, 1f);
			screenEdge = true;
			return;
		}
		
		// If player goes to right corner
		if ((s.getX() + -offsetX) - s.getWidth() > tmap.getPixelWidth() - s.getWidth()*2) {
			s.setX(screenWidth - s.getWidth());
			s.setVelocityX(-s.getVelocityX());
			s.setScale(-1f, 1f);
			screenEdge = true;
			return;
		}
		// If player is too high
		if (s.getY() < 20) {
			// don't let player go above map
			s.setY(20);
			return;
		}
		screenEdge = false;
	}


	public void checkLevelComplete() {
		if(coins.size() == 0) {
			levelComplete = true;
			astroid.show();
			astroid.setAnimationFrame(0);
			astroid.setPosition(player.getX(), -20);
			astroid.setOffsets(0, -100);
			astroid.setVelocity(0f, 0.4f);
			rotation = 90;	
		}
	}

	public boolean boundingBoxCollision(Sprite s1, Sprite s2) {

		Rectangle sprite1 =  getSpriteBounds((int)s1.getX(), (int)s1.getY(), s1.getWidth(), s1.getHeight());
		Rectangle sprite2 =  getSpriteBounds((int)s2.getX(), (int)s2.getY(), s2.getWidth(), s2.getHeight());
		
		return sprite1.intersects(sprite2);
	}
	

	private void jumpyDinoKO(Sprite s) {
		s.stop();
		s.setAnimation(enemyDeath);
		s.setAnimationFrame(0);
		s.playAnimation();
		s.pauseAnimationAtFrame(4);
		soundControl(new Sound("sounds/moan.wav"));
		if(astroid.isVisible() == false)
			score = score + 5000; //50 Score
		enemyDead = true;
	}

	/**
	 * Check and handles collisions with a tile map for the given sprite 's'.
	 * Initial functionality is limited...
	 * 
	 * @param s    The Sprite to check collisions for
	 * @param tmap The tile map to check
	 */

	public void checkTileCollision(Sprite s, TileMap tmap) {
		float sx = s.getX() + -offsetX + s.getRadius();
    	float sy = s.getY() + s.getRadius();  
    	
    	int xtile, ytile;
    	double x, y;
    	
    	for(int angle = 0; angle < 6; angle++) {
    		try {
	    		x = sx + (s.getRadius() * Math.cos(Math.toRadians(angle * 60)));
	    		y = sy + (s.getRadius() * Math.sin(Math.toRadians(angle * 60)));
	    		// Find out how wide and how tall a tile is
	        	xtile = (int)(x /  tmap.getTileWidth());
	        	ytile = (int)(y / tmap.getTileHeight());
	        	Tile t = tmap.getTile(xtile, ytile);
	    		switch(t.getCharacter()) {
		    		case '.':
		    		case '2':
		    		default:
		    			continue;
		    		case 'l':
		    		case 'm':
		    		case 'r':	
		    			if(t.getYC() < s.getY() + s.getHeight()/2) {
		    				s.setVelocityY(0.2f);
		    				return;
		    			}else {
		    				s.setVelocityY(0f);
		    				s.setY(s.getY() - 1);
		    				return; 
		    			}
	    		}
	    	//Stops weird error where program crashes when enemy collides with player (only occurs in first few tiles)
	    	}catch(Exception e){} 
    	}
	}
	
	public Rectangle getBounds(int x, int y) {
		int bw = tmap.getTileWidth();
		int bh = tmap.getTileHeight();
		return (new Rectangle(x,y,bw,bh));
	}
	
	public Rectangle getSpriteBounds(int x, int y, int width, int height) {
		return (new Rectangle(x,y,width,height));
	}

	/**
	 * Override of the keyPressed event defined in GameCore to catch our own events
	 * 
	 * @param e The event that has been generated
	 */
	public void keyPressed(KeyEvent e) {
		if(!playerDead) {
			switch (e.getKeyCode()) {
			case KeyEvent.VK_A:
				keyRight = false;
				keyLeft = true;
				move();
				return;
			case KeyEvent.VK_D:
				keyLeft = false;
				keyRight = true;
				move();
				return;
			case KeyEvent.VK_SPACE:
				player.setAnimation(jump);
				player.setVelocityY(-0.4f);
				return;
			case KeyEvent.VK_1:
				debugMode = !debugMode;
				return;
			case KeyEvent.VK_2:
				offsetX = -tmap.getPixelWidth() + screenWidth;
				player.setX(400);
				return;
			case KeyEvent.VK_3:
				dinoDeathAnim();
				gameOver();
				return;
			case KeyEvent.VK_4:
				coins.clear();
				checkLevelComplete();
				return;
			case KeyEvent.VK_V:
				soundBool = !soundBool;
				soundControl(music);
				return;
			case KeyEvent.VK_B:
				//filterStream.reduceVolume();
				//soundControl(music);
				return;
			}
		}
	}

	/**
	 * Override of the keyReleased event defined in GameCore to catch our own events
	 * 
	 * @param e The event that has been generated
	 */
	public void keyReleased(KeyEvent e) {
		if(!playerDead) {
			switch (e.getKeyCode()) {
			case KeyEvent.VK_ESCAPE:
				System.exit(0);
			case KeyEvent.VK_A:
				keyLeft = false;
				player.setVelocityX(0.0f);
				player.setAnimation(idle);
				break;
			case KeyEvent.VK_D:
				keyRight = false;
				player.setVelocityX(0.0f);
				player.setAnimation(idle);
				break;
			case KeyEvent.VK_SPACE:
				TimerTask task = new TimerTask() {
					public void run() {
						if (keyLeft == false && keyRight == false) {
							if (player.getAnimation() == jump) {
								player.setAnimation(idle);
							}
						}
					}
				};
				Timer timer = new Timer("Timer");
				long delay = 1000l;
				timer.schedule(task, delay);
				break;
			}
		}
	}
	
	public void soundControl(Sound sound) {
	{
		try
		{
			if (sound.equals(music))
			{
				if (!soundBool)
				{
					music.pauseSound();
				}
				else if (soundBool)
				{
					music.unpauseSound();
				}
			}
			else
			{
				if (soundBool)
				{
					sound.start();
				}
			}
		}
		catch (Exception e)
		{}
	}
}
	
	public void mouseClicked(MouseEvent e) {
		if(state == gameStage.INTRO) {
			int mouseY = e.getY();
			
			if(mouseY > 150) {
				state = gameStage.PLAY;
			} else {
				soundBool = !soundBool;
			}
			soundControl(music);
			return;
		} else if(state == gameStage.PLAY){
			if(debugMode && enemyDead)
				enemySpawn();
		} else {
			level = 1;
			state = gameStage.PLAY;
			initialiseGame();
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
	}
	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
	}
	
}
