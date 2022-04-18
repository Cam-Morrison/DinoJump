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

/**
 * <h1> Dino-Jump </h1>
 * 
 * <p> The objective of the game is to collect all the coins without getting attacked by the enemies.
 * Enemies can be defeated by jumping on their head. This requires high ground. Enemies grant higher score.</p>
 * 
 * @author Student 2831609
 * @version 1.0
 * @since 03/2022
 */
@SuppressWarnings("serial")

public class Game extends GameCore implements MouseListener{ 

	//Sprites and tile map 
	private Sprite player = null;
	private Sprite asteroid;
	private TileMap tmap = new TileMap();
	private LinkedList<Sprite> parallaxBg = new LinkedList<>();
	private ConcurrentLinkedQueue<Sprite> coins;
	private ConcurrentLinkedQueue<Sprite> enemies;
	
	//Measurement variables for calculations 
	private int jumpingCount = 2;
	private int level = 1;
	private int numberSpawned;
	private int offsetX = 0;
	private int score = 0;
	private int rotation;
	private final int numbOfEnemies = 3;
	private final static int screenWidth = 512;
	private final static int screenHeight = 384;
	private float playerMovementSpeed = 0.08f;
	private final float enemyMovementSpeed = 0.05f;
	private final float gravity = 0.001f;
	
	//Game state controllers 
	private boolean keyLeft;
	private boolean keyRight;
	private boolean debugMode;
	private boolean screenEdge;
	private boolean playerDead;
	private boolean levelComplete;
	private boolean enemySpawnedIn;
	private boolean isSoundOn = true;
	private enum gameStage { INTRO, PLAY, FINISHED }
	private gameStage state = gameStage.INTRO;
	
	//Game resources (Images, animations and theme song)
	private Image playBtn, background, audioBtn, muteBtn;
	private Animation idle, run, jump, dead, enemySpawn, enemyIdle, enemyWalk, enemyAttack, 
	enemyDeath, asteroidAnim, explosion, coinAnim;
	private Sound music;
	
	/**
	 * The obligatory main method that creates an instance of our class and starts
	 * it running
	 * 
	 * @param args The list of parameters this program might use (ignored)
	 */
	public static void main(String[] args) {
		Game gct = new Game();
		gct.init();
		gct.run(true, screenWidth, screenHeight);
	}
	/**
	 * Initialise the class, e.g. set up variables, load images, create animations,
	 * register event handlers
	 */
	private void init() {
		//Frame and tile map set up
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
		
		//Play button that is shown when player dies
		playBtn = null;
		try {
			playBtn = ImageIO.read(new File("images/interface/PlayButton.png"));
			playBtn = playBtn.getScaledInstance(screenWidth/3, screenHeight/4, Image.SCALE_SMOOTH);
			background = ImageIO.read(new File("images/interface/background.jpg"));
			background = background.getScaledInstance(screenWidth, screenHeight, Image.SCALE_SMOOTH);	
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//AUDIO buttons and Jurassic park theme mid 
		audioBtn = loadImage("images/interface/sound.png");
		muteBtn = loadImage("images/interface/mute.png");
		music = new Sound("sounds/dino.mid");
		music.start();
		
		// Player character from https://www.gameart2d.com/free-dino-sprites.html
		// I had to crop, resize and merge images into their own sprite sheets to work
		idle = new Animation();
		run = new Animation();
		jump = new Animation();
		dead = new Animation();
		idle.loadAnimationFromSheet("images/characters/dinosaur/idle.png", 3, 3, 100);
		run.loadAnimationFromSheet("images/characters/dinosaur/run.png", 3, 2, 91);
		jump.loadAnimationFromSheet("images/characters/dinosaur/jump.png", 6, 1, 100);
		dead.loadAnimationFromSheet("images/characters/dinosaur/dead.png", 8, 1, 80);
		player = new Sprite(idle);
		
		// Enemy character from https://eddies-workshop.itch.io/seeker
		enemySpawn = new Animation();
		enemyIdle = new Animation();
		enemyWalk = new Animation();
		enemyAttack = new Animation();
		enemyDeath = new Animation();
		enemySpawn.loadAnimationFromSheet("images/characters/Skeleton_Seeker/spawn.png", 1, 11, 300);
		enemyIdle.loadAnimationFromSheet("images/characters/Skeleton_Seeker/idle.png", 1, 6, 100);
		enemyWalk.loadAnimationFromSheet("images/characters/Skeleton_Seeker/walk.png", 1, 6, 150);
		enemyAttack.loadAnimationFromSheet("images/characters/Skeleton_Seeker/attack.png", 1, 10, 100);
		enemyDeath.loadAnimationFromSheet("images/characters/Skeleton_Seeker/death.png", 1, 5, 200);
		
		//Astroid image from https://www.kindpng.com/imgv/wmmowx_pixel-art-asteroid-sprite-hd-png-download/
		asteroidAnim = new Animation();
		explosion = new Animation();
		asteroidAnim.loadAnimationFromSheet("images/characters/astroid.png", 1, 1, 1000);
		explosion.loadAnimationFromSheet("images/explosion.png", 8, 6, 100);
		asteroid = new Sprite(asteroidAnim);

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
	 * Method that resets game states and variables, loads level and positions 
	 * new sprites. Called when game is created and restarted.
	 */
	private void initialiseGame() {
		if(level == 1) {
			score = 0;
			tmap.loadMap("maps", "map.txt");
		}else if(level == 2) {
			tmap.loadMap("maps", "map2.txt");
		} else {
			state = gameStage.FINISHED;
		}
		//Reset game states
		keyLeft = false;
		keyRight = false;
		playerDead = false;
		enemySpawnedIn = false;
		screenEdge = false;
		levelComplete = false;
		//Reset camera position and number of enemies spawned
		numberSpawned = 0;
		offsetX = 0;
		
		// ConcurrentLinked queue is used to avoid ConcurrentModificationException
		coins = new ConcurrentLinkedQueue<Sprite>();
		for (int i = 0; i < (int)tmap.getPixelWidth()/100; i++) { 
			coinAnim = new Animation();
			coinAnim.loadAnimationFromSheet("images/items/coin/coin.png", 6, 1, 100);
			Sprite coin = new Sprite(coinAnim);
			int x = 100 * i; //Every 100 pixels place a coin
			int y = 50 + (int)(Math.random() * (screenHeight - 100));
			//While coin is not placed within an empty tile, reposition the y axis of coin
			while(tmap.getTile((int)x/tmap.getTileWidth(),(int)y/tmap.getTileHeight()).getCharacter() != '.') {
				y = 50 + (int)(Math.random() * (screenHeight - 100));
			}
			coin.setX(x);
			coin.setY(y);
			coin.show();
			coins.add(coin);
		}
		//Reset and create player controlled sprite
		if(player != null)
			player = null;
		player = new Sprite(idle);
		player.setPosition(50, screenHeight - 100);
		player.setVelocityX(0);
		player.setVelocityY(0);
		player.show();
		//Set asteroid animation back to the asteroid png
		asteroid.setAnimation(asteroidAnim);
		asteroid.hide();
		//Reset enemy players
		if(enemies != null)
			enemies.clear();
		enemies = new ConcurrentLinkedQueue<Sprite>();
		for(int i = 0; i < numbOfEnemies; i++) {
			Sprite enemy = new Sprite(enemyIdle);
			enemy.setPosition(400 * (i+1), screenHeight - 76);
			enemySpawn(enemy); 
			enemies.add(enemy);
		}
	}
	/**
	 * Sets animation to enemySpawn, speed to 0 and displays enemy.
	 * @param s - Takes in a singular Sprite 
	 */
	private void enemySpawn(Sprite s) {
		s.setAnimation(enemySpawn);
		s.setVelocity(0, 0);
		s.show();
	}
	/**
	 * enemySpawnIn handles the spawn animation for sprite and then walk towards player
	 * @param enemy - A Sprite that represents a singular enemy
	 */
	private void enemySpawnIn(Sprite enemy) {
		enemy.show();	
		enemy.setAnimation(enemySpawn);
		enemy.setAnimationFrame(0);
		enemy.playAnimation();
		enemy.pauseAnimationAtFrame(10);
		enemy.setScale(-1f, 1f);
		TimerTask task = new TimerTask() {
			public void run() {
				//Fixes an error where player comes back alive if killed before movement
				if(enemy.getAnimation() != enemyDeath) {
					stalkPlayer(enemy);
					enemy.setAnimation(enemyWalk);
				}
			}
		};
		Timer timer = new Timer("Timer");
		long delay = 3000l;
		timer.schedule(task, delay);
		numberSpawned++;
		if(numberSpawned == numbOfEnemies) {
			enemySpawnedIn = true;
			soundControl(new Sound("sounds/roar.wav"));
		}
	}
	
	/**
	 * Update any sprites and check for collisions
	 * @param elapsed The elapsed time between this call and the previous call of elapsed
	 */
	public void update(long elapsed) {
		if(state == gameStage.PLAY) {
			player.setAnimationSpeed(1.0f);
			player.update(elapsed);
			for(Sprite enemy : enemies) {
				if(enemySpawnedIn == false) {
					enemySpawnIn(enemy);
				}	
				//If enemy isn't dead and hitbox is colliding with player
				if(enemy.getAnimation() != enemyDeath && boundingBoxCollision(player, enemy) == true) {
					//Check player is higher than enemy and centred on head
					if(player.getY() < enemy.getY() && player.getX() > enemy.getX() && 
					   player.getX() + player.getWidth()/2 < enemy.getX() + enemy.getWidth()) {
						jumpyDinoKO(enemy);
					}else {
						if(playerDead == false)
							//If player is not higher than enemy and still alive, enemy attack
							enemyAttack(enemy);
					}
					checkTileCollision(enemy, tmap);
				}
				handleScreenEdge(enemy, tmap, false);
				
				//Check if enemies are colliding with other enemies and redirect their direction
				for(Sprite enemy2 : enemies) {
					if(boundingBoxCollision(enemy, enemy2)) {
						if(enemy.getAnimation() != enemyDeath && enemy2.getAnimation() != enemyDeath) {
							enemy.setVelocityX(-enemy.getVelocityX());
							enemy.setScale((float)-enemy.getScaleX(), 1);
							enemy2.setVelocityX(-enemy2.getVelocityX());
							enemy2.setScale((float)-enemy2.getScaleX(), 1);
						}
					}
				}
				enemy.update(elapsed);
			}
		//If level complete call asteroid animation
		if(levelComplete) {
			asteroidControl(elapsed);
		}
		//If player isn't dead update gravity, check if user collides with coin
		if (playerDead == false) {	
			// Make adjustments to the speed of the sprite due to gravity
			player.setVelocityY(player.getVelocityY() + (gravity * elapsed));
			checkCoinCollision(elapsed);
		}
		// finally check for any collisions that may have occurred
		handleScreenEdge(player, tmap, true);
		checkTileCollision(player, tmap);
		}
	}
	
	/**
	 * Draws the current state of the game
	 */
	public void draw(Graphics2D g) {
		//Makes it so nothing renders out of players view so fps is higher
		Rectangle clip = new Rectangle(0,0,screenWidth, screenHeight);
		g.setClip(clip);

		if(state == gameStage.INTRO) {
			//Java AWT improved rendering so text is more readable
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
			//Toggle sound button appearance 
			if(isSoundOn) {
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
			if (keyRight) direction = 1;
			else direction = -1;
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
								/* If character moves right and background image position goes fully off screen
								 * left then set it to start after the second duplicate image on screen Screen
								 * width has an additional number in setX to avoid gap in drawing. */
								if (bg.getX() < -screenWidth)
									bg.setX(screenWidth - 11);
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

			//Mario centred camera implementation 
			int cameraMovementVal = 0;		
			//If player touches middle of the screen from left then change offset and stop character moving
			if (player.getX() > screenWidth / 2 - player.getWidth()) {
				if (player.getScaleX() == 1f) { //Check direction
					if(offsetX > (-tmap.getPixelWidth() + screenWidth)) {
						player.setX(screenWidth / 2 - player.getWidth());
						cameraMovementVal = 1 + (int) (player.getVelocityX() * 20);
						offsetX = offsetX - cameraMovementVal;
					}
				}
			} else {
				//If player touches middle of the screen plus width from
				//right then change offset and stop character moving
				if (player.getX() < screenWidth / 2 - player.getWidth()) {
					if (player.getScaleX() == -1f && offsetX < 0) { //Check direction
						player.setX(screenWidth / 2 - player.getWidth());
						cameraMovementVal =  -1 + (int) (player.getVelocityX() * 20);
						offsetX = offsetX + (offsetX - (cameraMovementVal + offsetX)); 
					}
				}
			}
			//Displays jumping count above players head in colour code
			if(jumpingCount != 0) g.setColor(Color.GREEN);
			else g.setColor(Color.RED);
			g.drawString(jumpingCount + " Jumps", player.getX(), player.getY() - 20);
			
			//Draws player, updates asteroid and enemies position
			player.drawTransformed(g);
			asteroid.setRotation(rotation);
			asteroid.drawTransformed(g);
			asteroid.setX(asteroid.getX() + (-cameraMovementVal));
			for (Sprite enemy : enemies) {
				enemy.setX(enemy.getX()  + (-cameraMovementVal));
				enemy.drawTransformed(g);
			}
			//Draw tile map with offset caused by camera movement maths above and update coins
			tmap.draw(g, offsetX, 0);
			for (Sprite c : coins) {
				c.setX(c.getX() + (-cameraMovementVal));
				c.drawTransformed(g);
			}
			
			// Show score and status information
			String msg = String.format("Score: %d", score / 100);
			String msg2 = "Level " + level;
			g.setColor(Color.BLACK);
			g.drawString(msg2, getXcenteredText(msg2,g) + 3, 50);
			g.drawString(msg, screenWidth - 123, 50);
			g.setColor(Color.WHITE);
			g.drawString(msg2, getXcenteredText(msg2,g), 50);
			g.drawString(msg, screenWidth - 120, 50);
			//Debug mode activated by pressing 1 
			if (debugMode) {
				g.setColor(Color.BLACK);
				g.drawString("DEBUG MODE", getXcenteredText("DEBUG MODE", g), screenHeight-10);
				g.setColor(Color.white);
				player.drawBoundingCircle(g);
				String debug = "FPS: " + (int) getFPS();
				g.drawString(debug, 40, 50);
				g.drawString("Camera:" + offsetX, screenWidth/2, screenHeight/2);
				g.drawString("X:" + (int) (player.getX() + (-offsetX)) + ", Y:" + (int) player.getY(), player.getX(), player.getY());
				for (Sprite c : coins) {
					c.drawBoundingCircle(g);
				}
				for (Sprite enemy : enemies) {
					if(enemy.getAnimation() == enemyDeath)
						g.setColor(Color.white); //If enemy dead, no longer hostile so white box
					else
						g.setColor(Color.red); //Enemy is alive, red box to show danger
					enemy.drawBoundingBox(g);
					g.drawString("X:" + (int) (enemy.getX() + (-offsetX)) + ", Y:" + (int) enemy.getY(), enemy.getX(), enemy.getY());
				}
				//Mario movement line, character cannot pass point till end of map
				g.drawLine(screenWidth/2, 0, screenWidth/2, screenHeight);
			} else {
				//If debug mode is off then draw coins remaining whilst alive
				g.setColor(Color.black);
				if(playerDead == false) {
					msg = "Remaining coins: " + coins.size();
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
			//Player dead overlay to restart game
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
			//If game state is not intro or play then its finished, draw end screen
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
	 * Returns an x value for placing a String centred
	 * @param text - The String you wish to centre.
	 * @param g - Graphics2D object for getting font size and style.
	 */
	private int getXcenteredText(String text, Graphics2D g) {
		int length = (int) g.getFontMetrics().getStringBounds(text, g).getWidth();
		int x = screenWidth/2 - length/2;
		return x;
	}
	/**
	 * Returns an x value for placing an Image centred
	 * @param img - The Image you wish to centre.
	 */
	private int getXcenterImage(Image img) {
		return (int)(screenWidth/2 - img.getWidth(null)/2);
	}
	/**
	 * Asteroid control to handle crash animation once level is complete
	 * @param elapsed The elapsed time between this call and the previous call of elapsed
	 */
	private void asteroidControl(long elapsed) {
		asteroid.update(elapsed);
		//Asteroid heads to players general direction
		if(asteroid.getX() > player.getX()) {
			asteroid.setVelocityX(asteroid.getVelocityX() - 0.001f);	
		}else {
			asteroid.setVelocityX(asteroid.getVelocityX() + 0.001f);
		}
		//If asteroid is appropriately on screen then execute explosion animation and end game
		if(asteroid.getY() > 300) {
			asteroid.stop();
			asteroid.setAnimation(explosion);
			asteroid.playAnimation();
			asteroid.pauseAnimationAtFrame(20);
			if(playerDead == false) {				
				asteroid.playAnimation();
				for(Sprite e : enemies) 
					jumpyDinoKO(e);
				dinoDeathAnim();
        		level++;
			}
		}else {
			//If still falling rotate asteroid
			rotation++;
		}
	}
	/**
	 * checkCoinCollision checks if coin and player overlap then removes it from list
	 * and then plays a sound and checks if there are no coins left. It also updates coin animation.
	 * @param elapsed - The elapsed time between this call and the previous call of elapsed
	 */
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
	/**
	 * dinoDeathAnim performs the animation for Dino's death and ends the game.
	 */
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
		soundControl(new Sound("sounds/roar.wav"));
		playerDead = true;
		score = 0;
		gameOver();
	}
	/**
	 * enemyAttack performs the animation for enemy attacking player. 
	 * Once finished it executes a call to dinoDeathAnim() which plays death animation.
	 * @param enemy - A singular Sprite object representing enemy. 
	 */
	private void enemyAttack(Sprite enemy) {
		if(enemy.getX() > player.getX()) {
			enemy.setScale(-1f, 1f);
		}else {
			enemy.setScale(1f, 1f);
		}
		enemy.setVelocity(0f, 0f);
		enemy.stop();
		enemy.setAnimation(enemyAttack);
		enemy.setAnimationFrame(0);
		enemy.playAnimation();
		dinoDeathAnim();
	}
	/**
	 * gameOver stops the movement of enemies and waits for user to click screen.
	 * Once a user clicks the screen, the game is restarted.
	 */
	private void gameOver() {
		addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {	
            	if(playerDead == true) {
            		for(Sprite enemy : enemies) {
            			enemy.stop();
            		}
            		initialiseGame();
            	}
            }
        });
	}
	/**
	 * Handles keyboard input of A and D keys so that characters movement and animation is updated.
	 * This implementation makes it so user cannot press both inputs causing a convulsion effect on sprite. 
	 */
	private void move() {
		if (keyLeft && !keyRight) {
			player.setAnimation(run);
			player.setVelocityX(-playerMovementSpeed);
			if(screenEdge != true)
				player.setScale(-1f, 1f);
		} else if (keyRight && !keyLeft) {
			player.setAnimation(run);
			player.setVelocityX(playerMovementSpeed);
			if(screenEdge != true)
				player.setScale(1f, 1f);
		}
	}
	/**
	 * Checks and handles collisions with the edge of the screen
	 * 
	 * @param s - The Sprite to check collisions for
	 * @param tmap - The tile map to check
	 * @param spriteIsPlayer - True if parameter s is the player sprite.
	 */
	private void handleScreenEdge(Sprite s, TileMap tmap, boolean spriteIsPlayer) {
		// if player too low
		if ((s.getY() + s.getHeight() + 32) > tmap.getPixelHeight()) {
			if(spriteIsPlayer == false)
				s.setY(tmap.getPixelHeight() - (s.getHeight() + 18));
			else {
				s.setVelocityY(0f);
				s.setY(s.getY());
			}
		}
		
		// If player goes to left corner
		if (s.getX() + -offsetX < 0) {
			if(spriteIsPlayer == false) {
				s.setVelocityX(enemyMovementSpeed);
				s.setScale(1f, 1f);
			}else
				s.setX(1 + -offsetX);
			screenEdge = true;
			return;
		}
		
		// If player goes to right corner
		if ((s.getX() + -offsetX) - s.getWidth() > tmap.getPixelWidth() - s.getWidth()*2) {
			if(spriteIsPlayer == false) {
				s.setVelocityX(-enemyMovementSpeed);
				s.setScale(-1f, 1f);
			}else
				s.setX(screenWidth - s.getWidth());
			screenEdge = true;
			return;
		}
		// If player is too high, don't let player go above map
		if (s.getY() < 20) {
			s.setY(20);
			return;
		}
		//Used so parallax background doesn't move when player is hugging wall
		screenEdge = false;
	}
	/**
	 * Checks the level is complete when no coins are remaining then 
	 * spawn in the asteroid.
	 */
	private void checkLevelComplete() {
		if(coins.size() == 0) {
			levelComplete = true;
			asteroid.show();
			asteroid.setAnimationFrame(0);
			asteroid.setPosition(player.getX(), -20);
			asteroid.setOffsets(0, -100);
			asteroid.setVelocity(0f, 0.4f);
			rotation = 90;	
		}
	}
	/**
	 * Makes two rectangular hit boxes for each sprite then checks if they intersect.
	 * @return boolean - true means overlap has occurred. 
	 */
	private boolean boundingBoxCollision(Sprite s1, Sprite s2) {

		Rectangle sprite1 =  getSpriteBounds((int)s1.getX(), (int)s1.getY(), s1.getWidth(), s1.getHeight());
		Rectangle sprite2 =  getSpriteBounds((int)s2.getX(), (int)s2.getY(), s2.getWidth(), s2.getHeight());
		
		return sprite2.intersects(sprite1);
	}
	
	/**
	 * stalkPlayer sends the enemy in the direction of the current players location
	 * every 10 seconds. This makes their movement more unpredictable than if they were to just follow player.
	 * @param enemy - A singular Sprite representing an enemy.  
	 */
	private void stalkPlayer(Sprite enemy) {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
		  @Override
		  public void run() {
			  if(enemy.getAnimation() != enemyDeath) {
				  if(enemy.getX() < player.getX()) {
					  enemy.setVelocityX(enemyMovementSpeed);
					  enemy.setScale(1f, 1f);
				  }else {
					  enemy.setVelocityX(-enemyMovementSpeed);
					  enemy.setScale(-1f, 1f);
				  }
			  }
		  }
		}, 0, 10000); //Every 10 seconds stalk player
	}
	/**
	 * jumpyDinoKO is the animation of an enemy death when the player successfully jumps on its head.
	 * Enemy's death animation and a sound is played, score is updated.
	 * @param s - A singular Sprite representing an enemy.  
	 */
	private void jumpyDinoKO(Sprite s) {
		s.setVelocity(0f, 0f);
		s.stop();
		s.setAnimationFrame(0);
		s.setAnimation(enemyDeath);
		s.playAnimation();
		s.pauseAnimationAtFrame(4);
		if(asteroid.isVisible() == false) {
			soundControl(new Sound("sounds/moan.wav"));
			score = score + 5000; //50 Score
		}
	}
	/**
	 * Checks if sprite intersects with any tiles. 
	 * 
	 * @param s - The Sprite to check collisions for
	 * @param tmap - The tile map to check
	 */
	private void checkTileCollision(Sprite s, TileMap tmap) {
		//Circular collision detection using bipolar coordinates 
		float sx = s.getX() + -offsetX + s.getRadius();
    	float sy = s.getY() + s.getRadius();  	
    	int xtile, ytile;
    	double x, y;
    	//for 6 points of the 360* circle, check collision
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
		    			//If player is below island, bounce back.
		    			if(t.getYC() < s.getY() + s.getHeight()/2) {
		    				s.setVelocityY(0.2f);
		    				return;
		    			//else player stand on top.
		    			}else {
		    				s.setVelocityY(0f);
		    				s.setY(s.getY());
		    				return; 
		    			}
	    		}
	    	//Stops weird error where program crashes when enemy collides with player (only occurs in first few tiles)
	    	}catch(Exception e){
	    		continue;
	    	} 
    	}
	}
	/**
	 * Creates a new rectangle object.
	 * @param x - Horizontal location on map 
	 * @param y - Vertical Location on map 
	 * @param width - of the new box
	 * @param height - of the new box
	 * 
	 * @return new Rectangle
	 */
	private Rectangle getSpriteBounds(int x, int y, int width, int height) {
		return (new Rectangle(x,y,width,height));
	}
	/**
	 * Override of the keyPressed event defined in GameCore to catch our own events
	 * @param e The event that has been generated
	 */
	public void keyPressed(KeyEvent e) {
		if(!playerDead) {
			switch (e.getKeyCode()) {
			case KeyEvent.VK_A:
			case KeyEvent.VK_LEFT:
				keyRight = false;
				keyLeft = true;
				move();
				return;
			case KeyEvent.VK_D:
			case KeyEvent.VK_RIGHT:
				keyLeft = false;
				keyRight = true;
				move();
				return;
			case KeyEvent.VK_SPACE:
			case KeyEvent.VK_UP:
				jump();
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
				isSoundOn = !isSoundOn;
				soundControl(music);
				return;
			}
		}
	}
	/**
	 * Override of the keyReleased event defined in GameCore to catch our own events
	 * @param e The event that has been generated
	 */
	public void keyReleased(KeyEvent e) {
		if(!playerDead) {
			switch (e.getKeyCode()) {
			case KeyEvent.VK_ESCAPE:
				System.exit(0);
			case KeyEvent.VK_A:
			case KeyEvent.VK_LEFT:
				keyLeft = false;
				player.setVelocityX(0.0f);
				player.setAnimation(idle);
				break;
			case KeyEvent.VK_D:
			case KeyEvent.VK_RIGHT:
				keyRight = false;
				player.setVelocityX(0.0f);
				player.setAnimation(idle);
				break;
			}
		}
	}
	/**
	 * handles user key press to jump. Double jumping is allowed. 
	 */
	private void jump() {
		//If remaining jumps = 0, no jump
		if(jumpingCount == 0) {
			return;
		} //If there are remaining jumps, remove 1 from number of free jump 
		jumpingCount--;
		player.setAnimation(jump);
		player.setVelocityY(-0.4f);
		TimerTask task = new TimerTask() {
			public void run() {
				//After 1.5seconds add a jump and change animation to idle is player isn't moving
				//If player is moving the animation will be changed to run anyway.
				jumpingCount++;
				if (keyLeft == false && keyRight == false) {
					if (player.getAnimation() == jump) {
						player.setAnimation(idle);
					}
				}
			}
		};
		Timer timer = new Timer("Timer");
		long delay = 1500l;
		timer.schedule(task, delay);
	}
	/**
	 * handles user key press to jump. Double jumping is allowed. 
	 * Method uses the isSoundOn variable to control whether user wants sound.
	 * @param sound - The Sound object we want to play or pause.
	 */
	private void soundControl(Sound sound) {
		try{
			//if sound ends with wav file then play it once
			if (isSoundOn && sound.filename.endsWith("wav"))
				sound.start();
			else {
			//else if sound isn't wav file it must be mid track so play or pause 
				if (isSoundOn == false)
					music.pauseSound();
				else
					music.unpauseSound();
			}
		}
		catch (Exception e){}
	}
	/**
	 * Override of the mouseClicked event defined in GameCore to catch our own mouse events
	 * @param e The MouseEvent that has been generated
	 */
	public void mouseClicked(MouseEvent e) {
		if(state == gameStage.INTRO) {
			int mouseY = e.getY();
			//on the intro screen there is only two buttons so if user clicks closer
			// to audio button then it toggles mute/play or if its closer to play button, the game starts.
			if(mouseY > 150) {
				state = gameStage.PLAY;
			} else {
				isSoundOn = !isSoundOn;
			}
			soundControl(music);
			return;
		} else if(state == gameStage.PLAY){
			//If game is currently playing and debug mode is active then send enemies towards player
			if(debugMode) {
				for(Sprite s : enemies) {
					stalkPlayer(s);
				}
			}
			return;
		} else {
			//If game is finished and player clicks screen then restart at level 1
			level = 1;
			state = gameStage.PLAY;
			initialiseGame();
			return;
		}
	}
	//Currently unused
	@Override public void mousePressed(MouseEvent e) {}
	@Override public void mouseReleased(MouseEvent e) {}
	@Override public void mouseEntered(MouseEvent e) {}
	@Override public void mouseExited(MouseEvent e) {}
}
