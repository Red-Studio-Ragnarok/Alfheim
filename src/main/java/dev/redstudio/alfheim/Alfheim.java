package dev.redstudio.alfheim;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;

import static dev.redstudio.alfheim.ProjectConstants.ID;

//    /$$$$$$  /$$  /$$$$$$  /$$                 /$$
//   /$$__  $$| $$ /$$__  $$| $$                |__/
//  | $$  \ $$| $$| $$  \__/| $$$$$$$   /$$$$$$  /$$ /$$$$$$/$$$$
//  | $$$$$$$$| $$| $$$$    | $$__  $$ /$$__  $$| $$| $$_  $$_  $$
//  | $$__  $$| $$| $$_/    | $$  \ $$| $$$$$$$$| $$| $$ \ $$ \ $$
//  | $$  | $$| $$| $$      | $$  | $$| $$_____/| $$| $$ | $$ | $$
//  | $$  | $$| $$| $$      | $$  | $$|  $$$$$$$| $$| $$ | $$ | $$
//  |__/  |__/|__/|__/      |__/  |__/ \_______/|__/|__/ |__/ |__/
@Mod(modid = ID, useMetadata = true, dependencies = "required-after:mixinbooter@[8.9,);required-after:redcore@[0.5,)")
public final class Alfheim {

    public static final int FLAG_COUNT = 32; // 2 light types * 4 directions * 2 halves * (inwards + outwards)

    public static final boolean IS_NOTHIRIUM_LOADED = Loader.isModLoaded("nothirium");
}
