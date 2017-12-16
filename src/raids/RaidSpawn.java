package raids;

import core.Spawn;
import core.Team;
import core.Util;
import maps.GeofenceIdentifier;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import pokemon.PokeMove;
import pokemon.Pokemon;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static maps.Geofencing.getGeofence;

/**
 * Created by Owner on 27/06/2017.
 */
public class RaidSpawn extends Spawn {

    private static final String NORMAL_EGG = "https://raw.githubusercontent.com/ZeChrales/PogoAssets/master/static_assets/png/ic_raid_egg_normal.png";
    private static final String RARE_EGG = "https://raw.githubusercontent.com/ZeChrales/PogoAssets/master/static_assets/png/ic_raid_egg_rare.png";
    private static final String LEGENDARY_EGG = "https://raw.githubusercontent.com/ZeChrales/PogoAssets/master/static_assets/png/ic_raid_egg_legendary.png";
    private final HashMap<String, Message> builtMessages = new HashMap<>();
    public Instant raidEnd;
    public Instant battleStart;
    public int bossId;
    public int raidLevel;
    public String gymId;
    public int move1Id;
    public int move2Id;
    private String name;
    private int bossCp;
    private String imageUrl;
    private int lobbyCode;

    public RaidSpawn(int id, boolean egg) {
        super();
        if (egg) {
            raidLevel = id;
        } else {
            bossId = id;
        }
    }

    public RaidSpawn(String name, String gymId, double lat, double lon, Team team, Instant raidEnd, Instant battleStart, int bossId, int bossCp, int move_1, int move_2, int raidLevel) {
        this.name = name;
        properties.put("gym_name", name);

        this.gymId = gymId;

        this.lat = lat;
        properties.put("lat", String.valueOf(lat));

        this.lon = lon;
        properties.put("lng", String.valueOf(lon));

        properties.put("team_name", team.toString());

        this.geofenceIdentifiers = getGeofence(lat, lon);

        properties.put("geofence", GeofenceIdentifier.listToString(geofenceIdentifiers));

        properties.put("gmaps", getGmapsLink());
        properties.put("applemaps", getAppleMapsLink());

        if (novaBot.config.suburbsEnabled()) {
            novaBot.reverseGeocoder.geocodedLocation(lat, lon).getProperties().forEach(properties::put);
        }

        this.raidEnd = raidEnd;
        properties.put("24h_end", getDisappearTime(printFormat24hr));
        properties.put("12h_end", getDisappearTime(printFormat12hr));
        properties.put("time_left", timeLeft(raidEnd));

        this.battleStart = battleStart;
        properties.put("24h_start", getStartTime(printFormat24hr));
        properties.put("12h_start", getStartTime(printFormat12hr));
        properties.put("time_left_start", timeLeft(battleStart));


        this.bossId = bossId;
        this.bossCp = bossCp;
        this.move1Id = move_1;
        this.move2Id = move_2;
        this.move_1 = PokeMove.idToName(move1Id);
        this.move_2 = PokeMove.idToName(move2Id);

        if (bossId != 0) {
            properties.put("pkmn", Util.capitaliseFirst(Pokemon.idToName(bossId)));
            properties.put("cp", String.valueOf(bossCp));
            properties.put("lvl20cp", String.valueOf(Pokemon.maxCpAtLevel(bossId, 20)));
            properties.put("lvl25cp", String.valueOf(Pokemon.maxCpAtLevel(bossId, 25)));
            properties.put("quick_move", this.move_1);
            properties.put("charge_move", this.move_2);
        }

        this.raidLevel = raidLevel;
        properties.put("level", String.valueOf(raidLevel));

        properties.put("lobbycode", "unkn");
    }

    public Message buildMessage(String formatFile) {

        if (builtMessages.get(formatFile) == null) {

            if (!properties.containsKey("city")) {
                novaBot.reverseGeocoder.geocodedLocation(lat, lon).getProperties().forEach(properties::put);
            }

            final MessageBuilder messageBuilder = new MessageBuilder();
            final EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setColor(getColor());

            if (bossId == 0) {
                formatKey = "raidEgg";
                embedBuilder.setDescription(novaBot.config.formatStr(properties, novaBot.config.getBodyFormatting(formatFile, formatKey) + (
                        raidLevel >= 3 && novaBot.config.isRaidOrganisationEnabled()
                                ? "\n\nJoin the discord lobby to coordinate with other players, and be alerted when this egg hatches. Join by clicking the ✅ emoji below this post, or by typing `!joinraid <lobbycode>` in any novabot channel."
                                : "")));
            } else {
                formatKey = "raidBoss";
                embedBuilder.setDescription(novaBot.config.formatStr(properties, novaBot.config.getBodyFormatting(formatFile, formatKey) + (
                        raidLevel >= 3 && novaBot.config.isRaidOrganisationEnabled()
                                ? "\n\nJoin the discord lobby to coordinate with other players by clicking the ✅ emoji below this post, or by typing `!joinraid <lobbycode>` in any novabot channel."
                                : "")));
            }
            embedBuilder.setTitle(novaBot.config.formatStr(properties, novaBot.config.getTitleFormatting(formatFile, formatKey)), novaBot.config.formatStr(properties, novaBot.config.getTitleUrl(formatFile, formatKey)));
            embedBuilder.setThumbnail(getIcon());
            if (novaBot.config.showMap(formatFile, formatKey)) {
                embedBuilder.setImage(getImage(formatFile));
            }
            embedBuilder.setFooter(novaBot.config.getFooterText(), null);
            embedBuilder.setTimestamp(Instant.now());
            messageBuilder.setEmbed(embedBuilder.build());

            String contentFormatting = novaBot.config.getContentFormatting(formatFile, formatKey);

            if (contentFormatting != null && !contentFormatting.isEmpty()) {
                messageBuilder.append(novaBot.config.formatStr(properties, novaBot.config.getContentFormatting(formatFile, formatKey)));
            }

            builtMessages.put(formatFile, messageBuilder.build());
        }
        return builtMessages.get(formatFile);
    }

    public String getDisappearTime(DateTimeFormatter printFormat) {
        return printFormat.format(ZonedDateTime.ofInstant(raidEnd, novaBot.config.getTimeZone()));
    }

    public String getIcon() {
        if (bossId == 0) {
            switch (raidLevel) {
                case 1:
                case 2:
                    return NORMAL_EGG;
                case 3:
                case 4:
                    return RARE_EGG;
                case 5:
                    return LEGENDARY_EGG;
            }
        }
        return Pokemon.getIcon(bossId);
    }

    public String getLobbyCode() {
        return String.format("%04d", lobbyCode);
    }

    public void setLobbyCode(int id) {
        this.lobbyCode = id;

        properties.put("lobbycode", getLobbyCode());
    }

    public String getStartTime(DateTimeFormatter printFormat) {
        return printFormat.format(ZonedDateTime.ofInstant(battleStart, novaBot.config.getTimeZone()));
    }

//    public static void main(String[] args) {
//
//        loadConfig();
//        loadGeofences();
//        DBManager.novabotdbConnect();
//        RaidSpawn spawn = new RaidSpawn("gym",
//                "123", -35.34200996278955, 149.05508042811897,
//                Team.Valor, Util.getCurrentTime(novaBot.config.getTimeZone()).toInstant().plusMillis(504000),
//                Util.getCurrentTime(novaBot.config.getTimeZone()).toInstant().plusMillis(6000000),
//                6,
//                11003,
//                2,
//                4,
//                3);
//
//        spawn.setLobbyCode(1);
//
//        Message message = spawn.buildMessage("formatting.ini");
//        System.out.println(message.getEmbeds().get(0).getTitle());
//        System.out.println(message.getEmbeds().get(0).getDescription());
//    }

    public void setLobbyCode(String lobbyCode) {
        this.lobbyCode = Integer.parseInt(lobbyCode);
        properties.put("lobbycode", getLobbyCode());
    }

    public String timeLeft(Instant untilTime) {
        ZonedDateTime currentTime = Util.getCurrentTime(novaBot.config.getTimeZone());

        long diff = Duration.between(untilTime, currentTime).toMillis();

        String time;
        if (MILLISECONDS.toHours(diff) > 0) {
            time = String.format("%02dh %02dm %02ds", MILLISECONDS.toHours(Math.abs(diff)),
                                 MILLISECONDS.toMinutes(Math.abs(diff)) -
                                 (MILLISECONDS.toHours(Math.abs(diff)) * 60),
                                 MILLISECONDS.toSeconds(Math.abs(diff)) -
                                 MILLISECONDS.toMinutes(Math.abs(diff) * 60)
                                );
        } else {
            time = String.format("%02dm %02ds",
                                 MILLISECONDS.toMinutes(Math.abs(diff)),
                                 MILLISECONDS.toSeconds(Math.abs(diff)) -
                                 (MILLISECONDS.toMinutes(Math.abs(diff)) * 60)
                                );
        }

        if (diff > 0) {
            time = "-" + time;
        }

        return time;
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", name, gymId, lat, lon, raidEnd, battleStart, bossId, bossCp, raidLevel, move_1, move_2);
    }

    private Color getColor() {
        switch (raidLevel) {
            case 1:
                return new Color(0x9d9d9d);
            case 2:
                return new Color(0xdb3b78);
            case 3:
                return new Color(0xff8000);
            case 4:
                return new Color(0xffe100);
            case 5:
                return new Color(0x00082d);
        }
        return Color.WHITE;
    }
}
