package com.gmail.val59000mc.maploader;

import com.gmail.val59000mc.UhcCore;
import com.gmail.val59000mc.game.GameManager;
import com.gmail.val59000mc.game.UhcWorldBorder;
import com.gmail.val59000mc.utils.FileUtils;
import com.gmail.val59000mc.configuration.YamlFile;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;


public class MapLoader {
	

	private double chunksLoaded;
	private int veinsGenerated;
	private double totalChunksToLoad;
	private String environment;
	
	
	public MapLoader(){
		chunksLoaded = 0;
		veinsGenerated = 0;
		environment = "starting";
	}
	
	public String getLoadingState(){
		double percentage = 100*chunksLoaded/totalChunksToLoad;		
		return environment+" "+(Math.floor(10*percentage)/10);
	}
	
	public void deleteLastWorld(String uuid){
		if(uuid == null || uuid.equals("null")){
			Bukkit.getLogger().info("[UhcCore] No world to delete");
		}else{
			File worldDir = new File(uuid);
			if(worldDir.exists()){
				Bukkit.getLogger().info("[UhcCore] Deleting last world : "+uuid);
				deleteFile(worldDir);
			}else{
				Bukkit.getLogger().info("[UhcCore] World "+uuid+" can't be removed, directory not found");
			}
		}
	}
	
	private static boolean deleteFile(File file) {

	    File[] flist = null;

	    if(file == null){
	        return false;
	    }

	    if (file.isFile()) {
	        return file.delete();
	    }

	    if (!file.isDirectory()) {
	        return false;
	    }

	    flist = file.listFiles();
	    if (flist != null && flist.length > 0) {
	        for (File f : flist) {
	            if (!deleteFile(f)) {
	                return false;
	            }
	        }
	    }

	    return file.delete();
	}
	
	public void createNewWorld(Environment env){
		String worldName = UUID.randomUUID().toString();
		if (UhcCore.getPlugin().getConfig().getBoolean("worlds.permanent-world-names", false)){
			worldName = "uhc-"+env.name().toLowerCase();
		}

		Bukkit.getLogger().info("[UhcCore] Creating new world : "+worldName);
		
		GameManager gm = GameManager.getGameManager();
		WorldCreator wc = new WorldCreator(worldName);
		wc.generateStructures(true);
		wc.environment(env);
		if(env.equals(Environment.NORMAL)){
			gm.getConfiguration().setOverworldUuid(worldName);
			if(gm.getConfiguration().getPickRandomSeedFromList() && !gm.getConfiguration().getSeeds().isEmpty()){
				Random r = new Random();
				Long seed = gm.getConfiguration().getSeeds().get(r.nextInt(gm.getConfiguration().getSeeds().size()));
				Bukkit.getLogger().info("[UhcCore] Picking random seed from list : "+seed);
				wc.seed(seed);
			}else if(gm.getConfiguration().getPickRandomWorldFromList() && !gm.getConfiguration().getWorldsList().isEmpty()){
				Random r = new Random();
				String randomWorldName = gm.getConfiguration().getWorldsList().get(r.nextInt(gm.getConfiguration().getWorldsList().size()));
				copyWorld(randomWorldName,worldName);
			}
		}else if (env == Environment.NETHER){
			gm.getConfiguration().setNetherUuid(worldName);
		}else {
			gm.getConfiguration().setTheEndUuid(worldName);
		}

		YamlFile storage = FileUtils.saveResourceIfNotAvailable("storage.yml");
		storage.set("worlds." + env.name().toLowerCase(), worldName);
		try {
			storage.save();
		}catch (IOException ex){
			ex.printStackTrace();
		}
		
		wc.type(WorldType.NORMAL);
		Bukkit.getServer().createWorld(wc);
	}
	
	public void loadOldWorld(String uuid, Environment env){
		
		if(uuid == null || uuid.equals("null")){
			Bukkit.getLogger().info("[UhcCore] No world to load, defaulting to default behavior");
			this.createNewWorld(env);
		}else{
			File worldDir = new File(uuid);
			if(worldDir.exists()){
				// Loading existing world
				Bukkit.getServer().createWorld(new WorldCreator(uuid));
			}else{
				this.createNewWorld(env);
			}
		}
	}
	
	private void copyWorld(String randomWorldName, String worldName) {
		Bukkit.getLogger().info("[UhcCore] Copying " + randomWorldName + " to " + worldName);
		File worldDir = new File(randomWorldName);
		if(worldDir.exists() && worldDir.isDirectory()){
			recursiveCopy(worldDir,new File(worldName));
		}
	}
	
	private void recursiveCopy(File fSource, File fDest) {
	     try {
	          if (fSource.isDirectory()) {
	          // A simple validation, if the destination is not exist then create it
	               if (!fDest.exists()) {
	                    fDest.mkdirs();
	               }
	 
	               // Create list of files and directories on the current source
	               // Note: with the recursion 'fSource' changed accordingly
	               String[] fList = fSource.list();
	 
	               for (int index = 0; index < fList.length; index++) {
	                    File dest = new File(fDest, fList[index]);
	                    File source = new File(fSource, fList[index]);
	 
	                    // Recursion call take place here
	                    recursiveCopy(source, dest);
	               }
	          }
	          else {
	               // Found a file. Copy it into the destination, which is already created in 'if' condition above
	 
	               // Open a file for read and write (copy)
	               FileInputStream fInStream = new FileInputStream(fSource);
	               FileOutputStream fOutStream = new FileOutputStream(fDest);
	 
	               // Read 2K at a time from the file
	               byte[] buffer = new byte[2048];
	               int iBytesReads;
	 
	               // In each successful read, write back to the source
	               while ((iBytesReads = fInStream.read(buffer)) >= 0) {
	                    fOutStream.write(buffer, 0, iBytesReads);
	               }
	 
	               // Safe exit
	               if (fInStream != null) {
	                    fInStream.close();
	               }
	 
	               if (fOutStream != null) {
	                    fOutStream.close();
	               }
	          }
	     }
	     catch (Exception ex) {
	          // Please handle all the relevant exceptions here
	     }
	}

	public void generateChunks(final Environment env){
		final World world;
		GameManager gm = GameManager.getGameManager();
		UhcWorldBorder border = gm.getWorldBorder();
		int size;
		chunksLoaded = 0;
		if(env.equals(Environment.NORMAL)){
			environment = "NORMAL";
			world = Bukkit.getWorld(GameManager.getGameManager().getConfiguration().getOverworldUuid());
			size = border.getStartSize();
		}else{
			environment = "NETHER";
			world = Bukkit.getWorld(GameManager.getGameManager().getConfiguration().getNetherUuid());
			size = border.getStartSize()/2;
		}


		final int maxChunk = (size-size%16)/16;
    	totalChunksToLoad = (2*((double) maxChunk)+1)*(2*((double) maxChunk)+1);
    	final int restEveryTicks = gm.getConfiguration().getRestEveryTicks();
    	final int chunksPerTick = gm.getConfiguration().getChunksPerTick();
    	final int restDuraton = gm.getConfiguration().getRestDuraton();
    	
    	final boolean isGenerateVeins = gm.getConfiguration().getEnableGenerateVein() && env.equals(Environment.NORMAL);
    	
    	Bukkit.getLogger().info("[UhcCore] Generating environment "+env.toString());
    	Bukkit.getLogger().info("[UhcCore] World border set to "+size+" blocks from lobby");
    	Bukkit.getLogger().info("[UhcCore] Loading a total "+Math.floor(totalChunksToLoad)+" chunks, up to chunk ( "+maxChunk+" , "+maxChunk+" )");
		Bukkit.getLogger().info("[UhcCore] Resting "+restDuraton+" ticks every "+restEveryTicks+" ticks");
		Bukkit.getLogger().info("[UhcCore] Loading up to "+chunksPerTick+" chunks per tick");
		Bukkit.getLogger().info("[UhcCore] Loading map "+getLoadingState()+"%");
		

    	final VeinGenerator veinGenerator = new VeinGenerator();
    	
		Bukkit.getScheduler().runTaskAsynchronously(UhcCore.getPlugin(), new Runnable(){

			@Override
			public void run() {				

			
				class RunnableWithParameter implements Runnable {
			        private int i,j,nextRest;
			        public RunnableWithParameter(int i, int j, int nextRest) { 
			        	this.i = i; 
			        	this.j = j; 
			        	this.nextRest = nextRest;
			        }
			        
			        public void run() {
						
			        	int loaded = 0;
						while(i<= maxChunk && j <= maxChunk && loaded < chunksPerTick){
							world.loadChunk(i, j);
							if(isGenerateVeins){
								veinsGenerated += veinGenerator.generateVeinsInChunk(world.getChunkAt(i, j));
							}
							world.unloadChunk(i, j);
							loaded++;
							j++;
						}
						chunksLoaded=chunksLoaded+loaded;
						
						if(i <= maxChunk){
							if(j > maxChunk){
								j = -maxChunk;
								i++;
							}
							
							int delayTask = 0;
							nextRest--;
							if(nextRest == 0){
								delayTask = restDuraton;
								nextRest = restEveryTicks;
								String message = "[UhcCore] Loading map "+getLoadingState()+"% - "+Math.floor(chunksLoaded)+"/"+Math.floor(totalChunksToLoad)+" chunks loaded";
								if(isGenerateVeins){
									message+=" - "+veinsGenerated+" veins generated";
								}
								Bukkit.getLogger().info(message);
							}
							
							Bukkit.getScheduler().scheduleSyncDelayedTask(UhcCore.getPlugin(), new RunnableWithParameter(i,j,nextRest),delayTask);
						}else{
							chunksLoaded = totalChunksToLoad;
							Bukkit.getLogger().info("[UhcCore] Environment "+env.toString()+" 100% loaded");
							if(env.equals(Environment.NORMAL) && !gm.getConfiguration().getBanNether()) {
								generateChunks(Environment.NETHER);
							}else {
								GameManager.getGameManager().startWaitingPlayers();
							}
						}
			        }
				}
				
				Bukkit.getScheduler().scheduleSyncDelayedTask(UhcCore.getPlugin(), new RunnableWithParameter(-maxChunk,-maxChunk,restEveryTicks),0);
				
			}
			
		});
		
	}
}
