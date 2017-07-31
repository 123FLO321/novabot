package raids;

import core.DBManager;
import core.ScheduledExecutor;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.utils.SimpleLog;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static core.MessageListener.*;
import static net.dv8tion.jda.core.utils.SimpleLog.Level.INFO;
import static net.dv8tion.jda.core.utils.SimpleLog.Level.WARNING;

/**
 * Created by Owner on 2/07/2017.
 */
public class RaidLobby {

    String roleId = null;
    String channelId = null;

    public String lobbyCode;

    public RaidSpawn spawn;

    HashSet<String> memberIds = new HashSet<>();

    static SimpleLog raidLobbyLog = SimpleLog.getLog("raid-lobbies");

    ScheduledExecutor shutDownService = null;

    public long nextTimeLeftUpdate = 15;

//    Runnable runnable = () -> {
//        if(spawn.raidEnd.after(DBManager.getCurrentTime())){
//            //lobby has ended, let's end
//            end(15);
//            shutDownService.shutdown();
//            return;
//        }
//
//        long timeLeft = spawn.raidEnd.getTime() - DBManager.getCurrentTime().getTime();
//        double minutes = timeLeft / 1000 / 60;
//
//        if(minutes <= nextTimeLeftUpdate){
//            alertRaidNearlyOver();
//            shutDownService.shutdown();
//            return;
//        }
//    };


    public RaidLobby(RaidSpawn raidSpawn, String lobbyCode){
        this.spawn = raidSpawn;
        this.lobbyCode = lobbyCode;

        long timeLeft = spawn.raidEnd.getTime() - DBManager.getCurrentTime().getTime();

        double minutes = timeLeft / 1000 / 60;

        while(minutes <= nextTimeLeftUpdate){
            nextTimeLeftUpdate -= 5;
        }

    }


    public RaidLobby(RaidSpawn spawn, String lobbyCode,String channelId, String roleId) {
        this(spawn,lobbyCode);
        this.channelId = channelId;
        this.roleId = roleId;

        long timeLeft = spawn.raidEnd.getTime() - DBManager.getCurrentTime().getTime();

        if(nextTimeLeftUpdate == 15){
            getChannel().sendMessageFormat("%s the raid is going to end in %s minutes!",getRole(),15).queueAfter(
                    timeLeft - (15*60*1000),TimeUnit.MILLISECONDS);
        }

        getChannel().sendMessageFormat("%s, the raid has ended and the raid lobby will be closed in %s minutes",getRole(),15).
                queueAfter(timeLeft,TimeUnit.MILLISECONDS,success -> end(15));


    }

    public void joinLobby(String userId){
        if(spawn.raidEnd.before(DBManager.getCurrentTime())) return;

        TextChannel channel = null;

        if(memberIds.size() == 0 && shutDownService == null){
            roleId = guild.getController().createRole().complete().getId();

            Role role = guild.getRoleById(roleId);


            String channelName = spawn.properties.get("gym_name").replace(" ","-").replaceAll("[\\W0-9]-|_", "");

            channelName = channelName.substring(0,Math.min(25,channelName.length()));

            role.getManagerUpdatable()
                    .getNameField().setValue(String.format("raid-%s",channelName))
                    .getMentionableField().setValue(true)
                    .update()
                    .queue();


            channelId = guild.getController().createTextChannel(String.format("raid-%s",channelName)).complete().getId();

            channel = guild.getTextChannelById(channelId);

            channel.createPermissionOverride(guild.getPublicRole()).setDeny(Permission.MESSAGE_READ).queue();

            channel.createPermissionOverride(role).setAllow(Permission.MESSAGE_READ,Permission.MESSAGE_WRITE, Permission.MESSAGE_HISTORY).queue();
            channel.createPermissionOverride(guild.getRoleById(config.novabotRole())).setAllow(Permission.MESSAGE_READ,Permission.MESSAGE_WRITE).complete();

            raidLobbyLog.log(INFO,String.format("First join for lobbyCode %s, created channel",lobbyCode));

            DBManager.newLobby(lobbyCode,spawn.gymId,memberCount(),channelId,roleId,nextTimeLeftUpdate);

            long timeLeft = spawn.raidEnd.getTime() - DBManager.getCurrentTime().getTime();
            double minutes = timeLeft / 1000 / 60;

            if(nextTimeLeftUpdate == 15){
                getChannel().sendMessageFormat("%s the raid is going to end in %s minutes!",getRole(),15).queueAfter(
                        timeLeft - (15*60*1000),TimeUnit.MILLISECONDS);
            }

            getChannel().sendMessageFormat("%s, the raid has ended and the raid lobby will be closed in %s minutes",getRole(),15).
                    queueAfter(timeLeft,TimeUnit.MILLISECONDS,success -> end(15));
        }

        Member member = guild.getMemberById(userId);

        guild.getController().addRolesToMember(member,guild.getRoleById(roleId)).queue();

        memberIds.add(userId);

        if(channel == null){
            channel = guild.getTextChannelById(channelId);
        }

        if(shutDownService != null){
            raidLobbyLog.log(INFO,"Cancelling lobby shutdown");
            shutDownService.shutdown();
            shutDownService = null;
        }

        channel.sendMessageFormat("Welcome %s to the raid lobby!\nThere are now %s users in the lobby.",member,memberIds.size()).queue();
        channel.sendMessage(getStatusMessage()).queue();

        DBManager.updateLobby(lobbyCode,memberCount(), (int) nextTimeLeftUpdate);
    }

    public Role getRole() {
        return guild.getRoleById(roleId);
    }

    public int memberCount() {
        return memberIds.size();
    }

    public void leaveLobby(String id) {
        guild.getController().removeRolesFromMember(guild.getMemberById(id),getRole()).queue();
        memberIds.remove(id);
        getChannel().sendMessageFormat("%s left the lobby, there are now %s users in the lobby.",guild.getMemberById(id),memberCount()).queue();

        DBManager.updateLobby(lobbyCode,memberCount(), (int) nextTimeLeftUpdate);

        if(memberCount() == 0){
            getChannel().sendMessage(String.format("There are no users in the lobby, it will be closed in %s minutes", 10)).queue();
            end(10);
        }
    }

    public TextChannel getChannel() {
        return guild.getTextChannelById(channelId);
    }

    public void end(int delay) {
        if(channelId == null || roleId == null) return;

        Runnable shutDownTask = () -> {
            getChannel().delete().queue();
            getRole().delete().queue();
            raidLobbyLog.log(INFO, String.format("Ended raid lobby %s", lobbyCode));
            lobbyManager.activeLobbies.remove(lobbyCode);
            DBManager.endLobby(lobbyCode);
            //TODO: remove all emojis and edit messages so its clear this raid is ended
        };

        shutDownService = new ScheduledExecutor(1);

        shutDownService.schedule(shutDownTask, delay, TimeUnit.MINUTES);
        shutDownService.shutdown();
    }

    public Message getStatusMessage(){
        EmbedBuilder embedBuilder = new EmbedBuilder();

        if(spawn.bossId != 0) {
            embedBuilder.setTitle(String.format("Raid status for %s (lvl %s) in %s - Lobby %s",
                    spawn.properties.get("pkmn"),
                    spawn.properties.get("level"),
                    spawn.properties.get("city"),
                    lobbyCode),
                    spawn.properties.get("gmaps"));

            embedBuilder.setDescription("Type `!status` to see this message again, and `!help` to see all available raid lobby commands.");

            embedBuilder.addField("Lobby Members", String.valueOf(memberCount()), false);
            embedBuilder.addField("Address", String.format("%s %s, %s",
                    spawn.properties.get("street_num"),
                    spawn.properties.get("street"),
                    spawn.properties.get("city")
            ), false);
            embedBuilder.addField("Gym Name", spawn.properties.get("gym_name"), false);
            embedBuilder.addField("Raid End Time", String.format("%s (%s)",
                    spawn.properties.get("24h_end"),
                    spawn.timeLeft(spawn.raidEnd)),
                    false);
            embedBuilder.addField("Boss Moveset", String.format("%s - %s", spawn.move_1, spawn.move_2), false);

            String weaknessEmoteStr = "";

            for (String s : Raid.getBossWeaknessEmotes(spawn.bossId)) {
                Emote emote = Raid.emotes.get(s);
                if(emote != null) {
                    weaknessEmoteStr += emote.getAsMention();
                }
            }

            embedBuilder.addField("Weak To", weaknessEmoteStr, true);

            String strengthEmoteStr = "";

            for (String s : Raid.getBossStrengthsEmote(spawn.bossId)) {
                Emote emote = Raid.emotes.get(s);
                strengthEmoteStr += (emote == null ? "" :emote.getAsMention());
            }

            embedBuilder.addField("Strong Against", strengthEmoteStr, true);

            embedBuilder.setThumbnail(spawn.getIcon());
            embedBuilder.setImage(spawn.getImage());
        }else{
            embedBuilder.setTitle(String.format("Raid status for level %s egg in %s - Lobby %s",
                    spawn.properties.get("level"),
                    spawn.properties.get("city"),
                    lobbyCode));

            embedBuilder.setDescription("Type `!status` to see this message again, and `!help` to see all available raid lobby commands.");

            embedBuilder.addField("Lobby Members", String.valueOf(memberCount()), false);
            embedBuilder.addField("Address", String.format("%s %s, %s",
                    spawn.properties.get("street_num"),
                    spawn.properties.get("street"),
                    spawn.properties.get("city")
            ), false);
            embedBuilder.addField("Gym Name", spawn.properties.get("gym_name"), false);
            embedBuilder.addField("Raid Start Time", String.format("%s (%s)",
                    spawn.properties.get("24h_start"),
                    spawn.timeLeft(spawn.battleStart)),
                    false);

            embedBuilder.setThumbnail(spawn.getIcon());
            embedBuilder.setImage(spawn.getImage());
        }

        MessageBuilder messageBuilder = new MessageBuilder().setEmbed(embedBuilder.build());

        return messageBuilder.build();
    }

    public Message getBossInfoMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        embedBuilder.setTitle(String.format("%s - Level %s raid",spawn.properties.get("pkmn"),spawn.properties.get("level")));
        embedBuilder.addField("CP",spawn.properties.get("cp"),false);
        embedBuilder.addField("Moveset",String.format("%s - %s",spawn.move_1,spawn.move_2),false);
        String weaknessEmoteStr = "";

        for (String s : Raid.getBossWeaknessEmotes(spawn.bossId)) {
            Emote emote = Raid.emotes.get(s);
            weaknessEmoteStr += (emote == null ? "" :emote.getAsMention());
        }

        embedBuilder.addField("Weak To",weaknessEmoteStr,true);

        String strengthEmoteStr = "";

        for (String s : Raid.getBossStrengthsEmote(spawn.bossId)) {
            Emote emote = Raid.emotes.get(s);
            strengthEmoteStr += (emote == null ? "" :emote.getAsMention());
        }

        embedBuilder.addField("Strong Against",strengthEmoteStr,true);

        embedBuilder.setThumbnail(spawn.getIcon());

        MessageBuilder messageBuilder = new MessageBuilder().setEmbed(embedBuilder.build());

        return messageBuilder.build();
    }

    public String getTeamMessage() {
        String str = String.format("There are %s users in this raid team:\n\n", memberCount());

        for (String memberId : memberIds) {
            str += String.format("  %s%n",guild.getMemberById(memberId).getEffectiveName());
        }

        return str;
    }

    public boolean containsUser(String id) {
        return memberIds.contains(id);
    }

    public void alertEggHatched() {
        if(channelId != null) {
            getChannel().sendMessageFormat("%s the raid egg has hatched into a %s!", getRole(), spawn.properties.get("pkmn")).queue();
            getChannel().sendMessage(getStatusMessage()).queue();
        }
    }

    public void alertRaidNearlyOver() {
        getChannel().sendMessageFormat("%s the raid is going to end in %s!",getRole(),spawn.timeLeft(spawn.raidEnd)).queue();
        DBManager.updateLobby(lobbyCode,memberCount(), (int) nextTimeLeftUpdate);
    }

    public Message getInfoMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        if(spawn.bossId != 0) {
            String timeLeft = spawn.timeLeft(spawn.raidEnd);

            embedBuilder.setTitle(String.format("[%s] %s (%s)- Lobby %s"
                    , spawn.properties.get("city")
                    , spawn.properties.get("pkmn")
                    , timeLeft
                    , lobbyCode),spawn.properties.get("gmaps"));
            embedBuilder.setDescription(String.format("Join the discord lobby to coordinate with other players by clicking the ✅ emoji below this post, or by typing `!joinraid %s` in any raid channel or PM with novabot.",lobbyCode));
            embedBuilder.addField("Team Size", String.valueOf(memberCount()), false);
            embedBuilder.addField("Gym Name", spawn.properties.get("gym_name"), false);
            embedBuilder.addField("Raid End",String.format("%s (%s remaining)"
                    ,spawn.properties.get("24h_end")
                    ,timeLeft
                    ),false);

            embedBuilder.setThumbnail(spawn.getIcon());
        }else{
            String timeLeft = spawn.timeLeft(spawn.battleStart);

            embedBuilder.setTitle(String.format("[%s] Lvl %s Egg (Hatches in %s) - Lobby %s"
                    , spawn.properties.get("city")
                    , spawn.properties.get("level")
                    , timeLeft
                    , lobbyCode),spawn.properties.get("gmaps"));
            embedBuilder.setDescription(String.format("Join the discord lobby to coordinate with other players by clicking the ✅ emoji below this post, or by typing `!joinraid %s` in any raid channel or PM with novabot.",lobbyCode));
            embedBuilder.addField("Team Size", String.valueOf(memberCount()), false);
            embedBuilder.addField("Gym Name", spawn.properties.get("gym_name"), false);
            embedBuilder.addField("Raid Start",String.format("%s (%s remaining)"
                    ,spawn.properties.get("24h_start")
                    ,timeLeft
            ),false);

            embedBuilder.setThumbnail(spawn.getIcon());
        }
        MessageBuilder messageBuilder = new MessageBuilder().setEmbed(embedBuilder.build());

        return messageBuilder.build();
    }

    public void loadMembers() {
        try {
            Role role = guild.getRoleById(roleId);
            for (Member member : guild.getMembersWithRoles(role)) {
                memberIds.add(member.getUser().getId());
            }
        }catch (NullPointerException e){
            raidLobbyLog.log(WARNING,"Couldn't load members, couldnt find role by Id");
        }
    }

}
