package net.technicpack.legacywrapper;

import java.applet.Applet;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import net.technicpack.legacywrapper.MinecraftClassLoader;
import net.technicpack.legacywrapper.exception.*;

public class MinecraftLauncher {
	private static MinecraftClassLoader loader = null;

	public static void resetClassLoader() {
		loader = null;
	}

	@SuppressWarnings("rawtypes")
	public static Applet getMinecraftApplet(StartupParameters startupParams) throws CorruptedMinecraftJarException, MinecraftVerifyException  {
		File mcBinFolder = new File(startupParams.getGameDirectory() , "bin");

		try {
			ClassLoader classLoader = getClassLoader(startupParams);

			String nativesPath = new File(mcBinFolder, "natives").getAbsolutePath();
			System.setProperty("org.lwjgl.librarypath", nativesPath);
			System.setProperty("net.java.games.input.librarypath", nativesPath);
			System.setProperty("org.lwjgl.util.Debug", "true");
			System.setProperty("org.lwjgl.util.NoChecks", "false");

			setMinecraftDirectory(classLoader, new File(startupParams.getGameDirectory()));

			Class minecraftClass = classLoader.loadClass("net.minecraft.client.MinecraftApplet");
			return (Applet) minecraftClass.newInstance();
		} catch (ClassNotFoundException ex) {
			throw new CorruptedMinecraftJarException(ex);
		} catch (IllegalAccessException ex) {
			throw new CorruptedMinecraftJarException(ex);
		} catch (InstantiationException ex) {
			throw new CorruptedMinecraftJarException(ex);
		} catch (VerifyError ex) {
			throw new MinecraftVerifyException(ex);
		} catch (Throwable t) {
			throw new UnknownMinecraftException(t);
		}
	}

	public static MinecraftClassLoader getClassLoader(StartupParameters startupParameters) {
		if (loader == null) {
			File mcBinFolder = new File(startupParameters.getGameDirectory(), "bin");

			File modpackJar = new File(mcBinFolder, "modpack.jar");
			File minecraftJar = new File(mcBinFolder, "minecraft.jar");

			File[] files = new File[2];

			try {
				files[0] = modpackJar;
				files[1] = minecraftJar;

				loader = new MinecraftClassLoader(ClassLoader.getSystemClassLoader(), modpackJar, files, startupParameters);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return loader;
	}

	/*
	 * This method works based on the assumption that there is only one field in
	 * Minecraft.class that is a private static File, this may change in the
	 * future and so should be tested with new minecraft versions.
	 */
	private static void setMinecraftDirectory(ClassLoader loader, File directory) throws MinecraftVerifyException {
		try {
			Class<?> clazz = loader.loadClass("net.minecraft.client.Minecraft");
			Field[] fields = clazz.getDeclaredFields();

			int fieldCount = 0;
			Field mineDirField = null;
			for (Field field : fields) {
				if (field.getType() == File.class) {
					int mods = field.getModifiers();
					if (Modifier.isStatic(mods) && Modifier.isPrivate(mods)) {
						mineDirField = field;
						fieldCount++;
					}
				}
			}
			if (fieldCount != 1) {
				throw new MinecraftVerifyException("Cannot find directory field in minecraft");
			}

			mineDirField.setAccessible(true);
			mineDirField.set(null, directory);

		} catch (Exception e) {
			throw new MinecraftVerifyException(e, "Cannot set directory in Minecraft class");
		}

	}
}