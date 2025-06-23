package dev.redstudio.alfheim;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;

import static dev.redstudio.alfheim.ProjectConstants.ID;
import static dev.redstudio.alfheim.ProjectConstants.VERSION;

//    /$$$$$$  /$$  /$$$$$$  /$$                 /$$
//   /$$__  $$| $$ /$$__  $$| $$                |__/
//  | $$  \ $$| $$| $$  \__/| $$$$$$$   /$$$$$$  /$$ /$$$$$$/$$$$
//  | $$$$$$$$| $$| $$$$    | $$__  $$ /$$__  $$| $$| $$_  $$_  $$
//  | $$__  $$| $$| $$_/    | $$  \ $$| $$$$$$$$| $$| $$ \ $$ \ $$
//  | $$  | $$| $$| $$      | $$  | $$| $$_____/| $$| $$ | $$ | $$
//  | $$  | $$| $$| $$      | $$  | $$|  $$$$$$$| $$| $$ | $$ | $$
//  |__/  |__/|__/|__/      |__/  |__/ \_______/|__/|__/ |__/ |__/

/// The main class of Alfheim.
///
/// This class mainly contains constants and metadata.
///
/// @author Luna Mira Lage (Desoroxxx)
/// @since 1.0
@Mod(modid = ID, version = VERSION, updateJSON = "https://forge.curseupdate.com/910715/" + ID, useMetadata = true, dependencies = "required-after:mixinbooter@[10.6,);required-after:redcore@[0.6,);")
public final class Alfheim {

	// TODO: Separate bugfixes and lighting engine

	/// `2 light types * 4 directions * 2 halves * (inwards + outwards)`
	public static final byte FLAG_COUNT = 32;

    public static final boolean IS_DYNAMIC_LIGHTS_LOADED = Loader.isModLoaded("dynamiclights");
	public static final boolean IS_NOTHIRIUM_LOADED = Loader.isModLoaded("nothirium");
	public static final boolean IS_VINTAGIUM_LOADED = Loader.isModLoaded("vintagium");
	public static final boolean IS_CELERITAS_LOADED = Loader.isModLoaded("celeritas");
}
