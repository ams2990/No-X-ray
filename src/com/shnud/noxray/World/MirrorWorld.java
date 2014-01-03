package com.shnud.noxray.World;

import com.shnud.noxray.NoXray;
import com.shnud.noxray.Utilities.DynamicCoordinates;
import com.shnud.noxray.Utilities.MagicValues;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Created by Andrew on 28/12/2013.
 */
public class MirrorWorld implements Listener {

    private MirrorRegionMap _regionMap;
    private RoomList _rooms;
    private World _world;
    private File _worldFolder;
    private static final long MILLISECONDS_TO_WAIT_BETWEEN_POSSIBLE_REGION_SAVES_AFTER_CHUNK_CHANGE = 120 * 1000;

    public MirrorWorld(World world) {
        if(world == null)
            throw new IllegalArgumentException("World cannot be null");

        _world = world;
        _worldFolder = new File(NoXray.getInstance().getDataFolder().getPath() + "/" + _world.getName() + "/");
        createWorldDirectoryIfNotExist();

        _regionMap = new MirrorRegionMap();
        _rooms = new RoomList(this);

        Bukkit.getPluginManager().registerEvents(this, NoXray.getInstance());
    }

    public File getFolder() { return _worldFolder; }

    private void createWorldDirectoryIfNotExist() {
        if(!_worldFolder.isDirectory())
            _worldFolder.mkdir();
    }

    private void loadRegion(int x, int z) {
        if(_regionMap.containsRegion(x, z))
            return;

        MirrorRegion region = new MirrorRegion(x, z, this._worldFolder);
        region.loadFromFile();
        _regionMap.putRegion(region);
    }

    private void unloadRegion(int x, int z) {
        if(!_regionMap.containsRegion(x, z))
            return;

        MirrorRegion region = _regionMap.getRegion(x, z);
        region.saveToFile();
        _regionMap.removeRegion(x, z);
    }

    @EventHandler (priority = EventPriority.MONITOR)
    private void onChunkLoad(ChunkLoadEvent event) {
        if(!event.getWorld().equals(_world))
            return;

        int regionX = event.getChunk().getX() >> MagicValues.BITSHIFTS_RIGHT_CHUNK_TO_REGION;
        int regionZ = event.getChunk().getZ() >> MagicValues.BITSHIFTS_RIGHT_CHUNK_TO_REGION;

        if(!_regionMap.containsRegion(regionX, regionZ))
            loadRegion(regionX, regionZ);

        _regionMap.getRegion(regionX, regionZ).chunkInUse();
    }

    @EventHandler (priority = EventPriority.MONITOR)
    private void onChunkUnload(ChunkUnloadEvent event) {
        if(!event.getWorld().equals(_world))
            return;

        int regionX = event.getChunk().getX() >> MagicValues.BITSHIFTS_RIGHT_CHUNK_TO_REGION;
        int regionZ = event.getChunk().getZ() >> MagicValues.BITSHIFTS_RIGHT_CHUNK_TO_REGION;
        if(!_regionMap.containsRegion(regionX, regionZ))
            return;

        MirrorRegion region = _regionMap.getRegion(regionX, regionZ);
        region.chunkNotInUse();

        if(region.getChunksInUse() == 0) {
            unloadRegion(region.getX(), regionZ);
        }
    }

    public String getWorldName() {
        return _world.getName();
    }

    public void saveAllData() {
        _rooms.saveRooms();

        for(MirrorRegion region : _regionMap) {
            region.saveToFile();
        }
    }
}
