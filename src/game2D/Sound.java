package game2D;

import java.io.*;
import javax.sound.sampled.*;

public class Sound extends Thread
{
	public Clip audio; //Audio clip
	public long time; //time stamp of audio clip playing
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
	/**
	 * Pauses sound at time it was last played at.
	 */
	public void pauseSound() {
		if (audio.isRunning()){
			this.time = audio.getMicrosecondPosition();
			audio.stop();
		}
	}
	/**
	 * Plays sound at time it was last played at.
	 */
	public void unpauseSound() {
		audio.setMicrosecondPosition(this.time);
		audio.start();
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
			audio = (Clip) AudioSystem.getLine(info);
			if (filename.contains("mid")){
				audio.open(stream);
				audio.start();
				audio.loop(Clip.LOOP_CONTINUOUSLY);
			}else{
				FadeFilterStream filter = new FadeFilterStream(stream);
				AudioInputStream sound = new AudioInputStream(filter, format, stream.getFrameLength());
				audio.open(sound);
				audio.start();
			}
		}catch (Exception e) {} 
		finally{
			this.finished = true;
		}
	}
}
