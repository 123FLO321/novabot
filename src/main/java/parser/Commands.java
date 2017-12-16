package parser;

import core.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class Commands {
    private HashMap<String, Command> commands;

    public Commands(Config config) {
        commands = new HashMap<>();
        final Command clearLocation = new Command()
                .setValidArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr, ArgType.Locations)))
                .setRequiredArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr, ArgType.Locations)))
                .setArgRange(1, 1);

        commands.put("!clearlocation", clearLocation);

        if(config.pokemonEnabled() || config.raidsEnabled()){
            Command loadPreset = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr,ArgType.Preset,ArgType.Locations)))
                    .setRequiredArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr,ArgType.Preset)))
                    .setArgRange(1,3);

            Command delPreset = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr,ArgType.Preset,ArgType.Locations)))
                    .setRequiredArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr,ArgType.Preset)))
                    .setArgRange(1,3);

            commands.put("!loadpreset", loadPreset);
            commands.put("!delpreset", delPreset);
        }

        if(config.pokemonEnabled()) {
            final Command addPokemon = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr, ArgType.Pokemon, ArgType.Locations, ArgType.Float, ArgType.Int)))
                    .setRequiredArgTypes(new HashSet<>(Collections.singletonList(ArgType.Pokemon)))
                    .setArgRange(1, 3);

            final Command delPokemon = new Command()
                    .setValidArgTypes(addPokemon.getValidArgTypes())
                    .setRequiredArgTypes(addPokemon.getRequiredArgTypes())
                    .setArgRange(1, 3);

            final Command clearPokemon = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr, ArgType.Pokemon)))
                    .setRequiredArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr, ArgType.Pokemon)))
                    .setArgRange(1, 1);

            commands.put("!addpokemon", addPokemon);
            commands.put("!delpokemon", delPokemon);
            commands.put("!clearpokemon", clearPokemon);
            commands.put("!clearpokelocation", clearLocation);
        }

        if(config.raidsEnabled()){
            final Command addRaid = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr, ArgType.Pokemon, ArgType.Locations)))
                    .setRequiredArgTypes(new HashSet<>(Collections.singletonList(ArgType.Pokemon)))
                    .setArgRange(1, 3);

            final Command delRaid = new Command()
                    .setValidArgTypes(addRaid.getValidArgTypes())
                    .setRequiredArgTypes(addRaid.getRequiredArgTypes())
                    .setArgRange(1, 3);

            Command clearRaid = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr, ArgType.Pokemon)))
                    .setRequiredArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr, ArgType.Pokemon)))
                    .setArgRange(1, 1);

            commands.put("!addraid", addRaid);
            commands.put("!delraid", delRaid);
            commands.put("!clearraid", clearRaid);
            commands.put("!clearraidlocation", clearLocation);
        }

        if(config.statsEnabled()) {
            final Command stats = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr, ArgType.Pokemon, ArgType.Int, ArgType.TimeUnit)))
                    .setRequiredArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr, ArgType.Pokemon)))
                    .setArgRange(1, 3);
            commands.put("!stats", stats);
        }
    }

    public Command get(final String firstArg) {
        return commands.get(firstArg);
    }

    public boolean isCommandWithArgs(final String s) {
        return commands.get(s) != null;
    }

}
