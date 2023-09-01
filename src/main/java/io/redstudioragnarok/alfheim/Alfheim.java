package io.redstudioragnarok.alfheim;

import net.minecraftforge.fml.common.Mod;

import static io.redstudioragnarok.alfheim.utils.ModReference.ID;
import static io.redstudioragnarok.alfheim.utils.ModReference.NAME;
import static io.redstudioragnarok.alfheim.utils.ModReference.VERSION;

//    /$$$$$$  /$$  /$$$$$$  /$$                 /$$
//   /$$__  $$| $$ /$$__  $$| $$                |__/
//  | $$  \ $$| $$| $$  \__/| $$$$$$$   /$$$$$$  /$$ /$$$$$$/$$$$
//  | $$$$$$$$| $$| $$$$    | $$__  $$ /$$__  $$| $$| $$_  $$_  $$
//  | $$__  $$| $$| $$_/    | $$  \ $$| $$$$$$$$| $$| $$ \ $$ \ $$
//  | $$  | $$| $$| $$      | $$  | $$| $$_____/| $$| $$ | $$ | $$
//  | $$  | $$| $$| $$      | $$  | $$|  $$$$$$$| $$| $$ | $$ | $$
//  |__/  |__/|__/|__/      |__/  |__/ \_______/|__/|__/ |__/ |__/
@Mod(modid = ID, name = NAME, version = VERSION, dependencies = "required-after:mixinbooter@[8.0,);required-after:redcore@[0.4-Dev-5,)"/*, updateJSON = "https://raw.githubusercontent.com/Red-Studio-Ragnarok/Alfheim/main/update.json"*/) // Todo: Uncomment once the repo is public
public final class Alfheim {

    public static final int FLAG_COUNT = 32; // 2 light types * 4 directions * 2 halves * (inwards + outwards)
}
