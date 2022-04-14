
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D.Float;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

	private boolean keyLeft = false;
	private boolean keyRight = false;

	private int total = 0;

	private boolean debugMode = false;
	private boolean screenEdge = false;
	private ConcurrentLinkedQueue<Sprite> coins;
	private int offsetX = 0;
	private int cameraMovementVal = 0;
	private int rotation;
	private Sprite astroid;
	private Animation dead;
	private boolean playerDead = false;
	private Image playBtn;
	
	private enum gameStage {
		INTRO,
		PLAY,
		DEAD
	}
	private gameStage state = gameStage.INTRO;
	private Image background;
	private LinkedList<Rectangle> tileCollison;
	private Image audio;
	private Image audioOff;
	private Sprite enemy;
	private Animation enemySpawn;
	private Animation enemyIdle;
	private Animation enemyWalk;
	private Animation enemyAttack;
	private Animation enemyDeath;
	private boolean enemyDead = false;
	private boolean enemySpawnedIn = false;
	private Float y_transposed_bounds;
	private Float x_transposed_bounds;
	private Object xo;

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
		ImageIcon img = new ImageIcon("images/gameCover.png");
		setIconImage(img.getImage());
		
		playBtn = null;
		try {
			playBtn = ImageIO.read(new File("images/PlayButton.png"));
			playBtn = playBtn.getScaledInstance(screenWidth/3, screenHeight/4, Image.SCALE_SMOOTH);
			background = ImageIO.read(new File("images/background.jpg"));
			background = background.getScaledInstance(screenWidth, screenHeight, Image.SCALE_SMOOTH);	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		audio = loadImage("images/title/sound_on.png");
		audioOff = loadImage("images/title/sound_off.png");
		
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
		enemy = new Sprite(enemyIdle);

		// ConcurrentLinked queue is used to avoid ConcurrentModificationException
		coins = new ConcurrentLinkedQueue<Sprite>();
		for (int i = 0; i < (int)tmap.getPixelWidth()/100; i++) {
			Animation coinAnim = new Animation();
			coinAnim.loadAnimationFromSheet("images/items/coin/coin.png", 6, 1, 100);
			Sprite coin = new Sprite(coinAnim);
			coin.setX(100 * i);
			coin.setY((int) 50 + (int)(Math.random() * (screenHeight - 100)));
			coin.show();
			coins.add(coin);
		}
		Animation astroidAnim = new Animation();
		astroidAnim.loadAnimationFromSheet("images/characters/astroid.png", 1, 1, 1000);
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
		playerDead = false;
		enemySpawnedIn = true;
		//TODO MOVE METHODS FROM INIT TO HERE FOR RESTART
		player.setAnimation(idle);
		player.setPosition(50, screenHeight - 100); // 64 character height and 32 (1 tile)
		player.setVelocityX(0);
		player.setVelocityY(0);
		player.show();
		
		enemy.setAnimation(enemySpawn);
		enemy.pauseAnimationAtFrame(0);
		enemy.setPosition(300, screenHeight - 96);
		enemy.setVelocity(0, 0);
		enemy.show();
		
		astroid.show();
		astroid.setPosition(400, 40);
		astroid.setVelocity(-0.2f, 0.2f);
		rotation = 90;
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
			title = "Cameron Morrison";
			x = getXcenteredText(title, g);
			g.drawString(title, x, screenHeight-30);
			g.setColor(Color.WHITE);
			g.drawString(title, x + 1, screenHeight-29);
		} else if(state == gameStage.PLAY){
			g.setFont(new Font("Verdana", Font.BOLD, 18));
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
									bg.setX(screenWidth - 10);
							} else {
								// Same applies here except opposite direction
								if (bg.getX() > screenWidth)
									bg.setX(-screenWidth + 10);
							}
	
						}
						backGroundNum += 1;
						continue;
					}
				}
			}
//			xo = (screenWidth / 2) - Math.round(player.getX()) - tmap.getTileWidth();
//			offsetX = Math.min(offsetX, 0);
//			offsetX = Math.max(offsetX, screenWidth - tmap.getPixelWidth());
			
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
			String msg = String.format("Score: %d", total / 100);
			g.setColor(Color.WHITE);
			g.drawString(msg, screenWidth - 120, 50);

			if (debugMode) {
				g.setColor(Color.BLACK);
				g.drawString("DEBUG MODE", getXcenteredText("DEBUG MODE", g), screenHeight-10);
				g.setColor(Color.white);
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
			}
			if(playerDead) {
				g.setFont(g.getFont().deriveFont(Font.BOLD, 50));
				int x = getXcenteredText("GAME OVER", g);
				g.drawString("GAME OVER", x, screenHeight/3);
				x = getXcenterImage(playBtn);
				g.drawImage(playBtn, x, 150, null); 
			}
			g.draw(x_transposed_bounds);
			g.draw(y_transposed_bounds);
		} else {
			g.fillRect(0, 0, screenWidth, screenHeight);
			g.setColor(Color.WHITE);
			String title = "Congrulations! You have beaten the game with a score of: " + total / 100;
			int x = getXcenteredText(title, g);
			int y = (int)(screenHeight/2.5);
			g.drawString(title, x, y);
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
			if (playerDead == false) {
				// Increase score
				total++;
				// Make adjustments to the speed of the sprite due to gravity
				player.setVelocityY(player.getVelocityY() + (gravity * elapsed));
				if(enemySpawnedIn) {
					enemy.shiftY(20);
					enemy.playAnimation();
					enemy.pauseAnimationAtFrame(10);
					TimerTask task = new TimerTask() {
						public void run() {
							enemy.shiftY(-10);
							enemy.setScale(-1f, 1f);
							enemy.setAnimation(enemyWalk);
							enemy.setVelocityX(-0.05f);
						}
					};
					Timer timer = new Timer("Timer");
					long delay = 1000l;
					timer.schedule(task, delay);
					enemySpawnedIn = false;
				}
				if(enemy.getX() - offsetX < 0) {
					enemy.setX(1);
					enemy.setVelocityX(-enemy.getVelocityX());
					enemy.setScale(1f, 1f);
				}
				
				//astroid.update(elapsed);
				rotation++;
				
				if(enemyDead == false && boundingBoxCollision(player, enemy)) {
					if(player.getY() < enemy.getY()) {
						jumpyDinoKO(enemy);
					}else {
						enemyAttack();
						dinoDeathAnim();
					}
				}
				if(boundingBoxCollision(player, astroid)){
					gameOver();
				}
				if(astroid.getY() > screenHeight) {
					astroid.setY(0);
					astroid.shiftX(400);
				}
			}
			for (Sprite c : coins) {
				c.update(elapsed);
				if (boundingBoxCollision(c, player)) {
					total += 500;
					coins.remove(c);
				}
			}
			// Then check for any collisions that may have occurred
			handleScreenEdge(player, tmap, elapsed);
			handleScreenEdge(enemy, tmap, elapsed);
			checkTileCollision(player, tmap);
//			checkTileCollision(enemy, tmap);
		}
	}
	
	private void dinoDeathAnim() {
		keyLeft = false;
		keyRight = false;
		player.stop();
		player.shiftY(5);
		player.setAnimation(dead);
		player.setAnimationFrame(0);
		player.playAnimation();
		player.pauseAnimationAtFrame(7);
		playerDead = true;
		gameOver();

	}
	
	private void enemyAttack() {
		enemy.stop();
		enemy.setAnimation(enemyAttack);
		enemy.setAnimationFrame(0);
		enemy.playAnimation();
		enemy.pauseAnimationAtFrame(9);
	}

	private void gameOver() {
		addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
            	initialiseGame();
            	playerDead = false;
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
	public void handleScreenEdge(Sprite s, TileMap tmap, long elapsed) {
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


	public boolean boundingBoxCollision(Sprite s1, Sprite s2) {
		Rectangle sprite1 =  getSpriteBounds((int)s1.getX() + cameraMovementVal, (int)s1.getY(), s1.getWidth(), s1.getHeight());
		Rectangle sprite2 =  getSpriteBounds((int)s2.getX() + cameraMovementVal, (int)s2.getY(), s2.getWidth(), s2.getHeight());
		return sprite1.intersects(sprite2);
	}

	private void jumpyDinoKO(Sprite s) {
		s.stop();
		s.setAnimation(enemyDeath);
		s.setAnimationFrame(0);
		s.playAnimation();
		s.pauseAnimationAtFrame(4);
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
        Rectangle spriteBounds = getSpriteBounds((int)s.getX(), (int)s.getY(), s.getWidth(), s.getHeight());
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
				gameOver();
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
	
	public void mouseClicked(MouseEvent e) {
		if(state == gameStage.INTRO) {
			state = gameStage.PLAY;
			return;
		}
		if(debugMode) {
//			int x = e.getX();
//			camX = camX - x;
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
