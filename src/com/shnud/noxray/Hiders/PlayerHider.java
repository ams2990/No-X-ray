package com.shnud.noxray.Hiders;

import com.shnud.noxray.Entities.EntityCoupleHidable;
import com.shnud.noxray.Entities.EntityCoupleList;
import com.shnud.noxray.Events.BasePacketEvent;
import com.shnud.noxray.Events.EntityUpdatePacketEvent;
import com.shnud.noxray.Events.PlayerSpawnPacketEvent;
import com.shnud.noxray.NoXray;
import com.shnud.noxray.Packets.PacketDispatcher;
import com.shnud.noxray.Packets.PacketListener;
import com.shnud.noxray.Settings.NoXraySettings;
import org.bukkit.Bukkit;
import org.bukkit.World;
import com.shnud.noxray.Packets.PacketEventListener;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitTask;

/**
 * Created by Andrew on 23/12/2013.
 */
public class PlayerHider implements PacketEventListener {
    private World _world;
    private EntityCoupleList<EntityCoupleHidable> _coupleWatchList = new EntityCoupleList<EntityCoupleHidable>();
    private BukkitTask _checkingTask;
    private boolean _isActive;

    public PlayerHider(World world) {
        if(world == null)
            throw new IllegalArgumentException("World cannot be null");

        _world = world;
    }

    public PlayerHider(World world, boolean activate) {
        this(world);
        if(activate)
            activate();
    }

    /*
     * Run when the player hider is constructed to give it chance
     * to find all couples that should be tracked if there are already
     * players in the world. It then has chance to hide those that can
     * even see each other already.
     */
    private void resetWatchers() {
        cancelCheckingTask();
        _coupleWatchList.clear();
        PacketDispatcher.resendAllPlayerSpawnPacketsForWorld(_world);
        scheduleCheckingTask();
    }

    public void deactivate() {
        if(!_isActive)
            return;

        PacketListener.removeEventListener(this);
        cancelCheckingTask();
        _coupleWatchList.clear();
        _isActive = false;
    }

    public void activate() {
        if(_isActive)
            return;

        PacketListener.addEventListener(this);
        resetWatchers();
        _isActive = true;
    }

    public boolean isActive() {
        return _isActive;
    }

    @Override
    public void receivePacketEvent(BasePacketEvent event) {
        // Only deal with packets to do with this world
        if(!event.getReceiver().getWorld().equals(_world))
            return;

        else if (event instanceof PlayerSpawnPacketEvent)
            onPlayerSpawnPacketEvent((PlayerSpawnPacketEvent) event);
        else if (event instanceof EntityUpdatePacketEvent)
            onEntityUpdatePacketEvent((EntityUpdatePacketEvent) event);
    }

    public void onPlayerSpawnPacketEvent(PlayerSpawnPacketEvent event) {
        // No point adding a couple with the same player, cannot hide from yourself
        if(event.getSubject().equals(event.getReceiver()))
            return;

        // If we're already monitoring the couple, no need to re-add it to the watch list
        if(_coupleWatchList.containsCoupleFromEntities(event.getReceiver(), event.getSubject())) {

            if(_coupleWatchList.getCoupleFromEntities(event.getReceiver(), event.getSubject()).areHidden())
                event.cancel();

            return;
        }

        /*
         * Always cancel the event so that we can handle the hidden/show status of
         * the player completely through the couple object. We will set the intial
         * value to hidden and then if they can see each other resend the spawn packet
         */

        event.cancel();
        EntityCoupleHidable couple = new EntityCoupleHidable(event.getReceiver(), event.getSubject(), true);

        _coupleWatchList.addCouple(couple);
        /*
         * It is absolutely necessary that the couple is added before the hidden status
         * is updated. Otherwise, while updating the couple's status, a new spawning packet
         * could be sent if the couple has LOS, and it will call this function again before
         * it ever adds the couple to the watch list, which will happen over and over.
         */
        updateCoupleHiddenStatus(couple);
    }

    public void onEntityUpdatePacketEvent(EntityUpdatePacketEvent event) {
        // We're only interested in player entity updates
        if(event.getSubject().getType() != EntityType.PLAYER)
            return;

        // If we're currently hiding the couple, then don't send
        // any entity updates as it could give a clue as to whether
        // a player may be nearby
        if(_coupleWatchList.containsCoupleFromEntities(event.getReceiver(), event.getSubject()) && _coupleWatchList.getCoupleFromEntities(event.getReceiver(), event.getSubject()).areHidden())
            event.cancel();

        /* Maybe here we could add some sort of flag to the couple to store the last time
         * the server tried to send an entity update packet for the couple. This could then
         * be used instead of ProtocolManager's getEntityTrackers to remove couples from
         * the list which are no longer in need of being tracked
         */
    }

    public void updateCoupleHiddenStatus(EntityCoupleHidable couple) {
        if(couple.getEntity1().isDead() || couple.getEntity2().isDead())
            return;

        boolean LOS = couple.haveClearLOS();

        if(LOS && couple.areHidden())
            couple.show();
        else if(!LOS && !couple.areHidden())
            couple.hide();
    }

    private void scheduleCheckingTask() {
        if(_checkingTask != null)
            _checkingTask.cancel();

        _checkingTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                NoXray.getInstance(),
                new CoupleCheckThread(),
                NoXraySettings.PLAYER_TICK_CHECK_FREQUENCY,
                NoXraySettings.PLAYER_TICK_CHECK_FREQUENCY
        );
    }

    private void cancelCheckingTask() {
        if(_checkingTask != null)
            _checkingTask.cancel();
    }

    public class CoupleCheckThread implements Runnable {

        @Override
        public void run() {
            for (EntityCoupleHidable couple : _coupleWatchList) {
                updateCoupleHiddenStatus(couple);
            }
        }
    }
}