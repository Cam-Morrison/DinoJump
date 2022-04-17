package game2D;

import java.io.*;
import javax.sound.sampled.*;

public class Sound extends Thread
{
	public Clip clip;
	public long time;
	public String filename; // The name of the file to play
	public boolean finished; // A flag showing that the thread has finished


	/**
	 * Constructor method for the Sound object.
	 * 
	 * @param fname The name of the file associated to the Sound object.
	 */
	public Sound(String fname)
	{
		filename = fname;
	}
	
	public void pauseSound() {
		if (clip.isRunning()){
			this.time = clip.getMicrosecondPosition();
			clip.stop();
		}
	}
	public void unpauseSound() {
		clip.setMicrosecondPosition(this.time);
		clip.start();
	}
	/**
	 * run will play the actual sound but you should not call it directly. You
	 * need to call the 'start' method of your sound object (inherited from
	 * Thread, you do not need to declare your own). 'run' will eventually be
	 * called by 'start' when it has been scheduled by the process scheduler.
	 */
	public void run(){
		try{
			File file = new File(filename);
			AudioInputStream stream = AudioSystem.getAudioInputStream(file);
			AudioFormat format = stream.getFormat();
			DataLine.Info info = new DataLine.Info(Clip.class, format);
			clip = (Clip) AudioSystem.getLine(info);
			if (filename.equals("dino.mid")){
				clip.open(stream);
				clip.start();
				clip.loop(Clip.LOOP_CONTINUOUSLY);
			}else{
				FadeFilterStream filter = new FadeFilterStream(stream);
				AudioInputStream sound = new AudioInputStream(filter, format, stream.getFrameLength());
				clip.open(sound);
				clip.start();
			}
		}catch (Exception e) {} 
		finally
		{
			this.finished = true;
		}
	}
}
