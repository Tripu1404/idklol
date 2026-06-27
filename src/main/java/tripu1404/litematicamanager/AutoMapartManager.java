package com.tripu1404.litematicamanager;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AutoMapartManager extends PluginBase {

    private File mapartFolder;
    // Cambiamos la clave a un String "id:meta" o un hash único para que diferencie las lanas correctamente
    private final Map<String, Color> blockColors = new HashMap<>();

    @Override
    public void onEnable() {
        this.mapartFolder = new File(this.getDataFolder(), "maparts");
        if (!this.mapartFolder.exists()) {
            this.mapartFolder.mkdirs();
        }
        
        // Registro exacto basado en la paleta de IDs de tu imagen (ID: 35 para Lana)
        registerBlockColor(35, 0, new Color(221, 221, 221));       // White Wool (35:0)
        registerBlockColor(35, 1, new Color(219, 125, 62));        // Orange Wool (35:1)
        registerBlockColor(35, 2, new Color(179, 80, 188));        // Magenta Wool (35:2)
        registerBlockColor(35, 3, new Color(107, 138, 201));       // Light Blue Wool (35:3)
        registerBlockColor(35, 4, new Color(177, 166, 39));        // Yellow Wool (35:4)
        registerBlockColor(35, 5, new Color(65, 174, 56));         // Lime Wool (35:5)
        registerBlockColor(35, 6, new Color(208, 132, 153));       // Pink Wool (35:6)
        registerBlockColor(35, 7, new Color(64, 64, 64));          // Gray Wool (35:7)
        registerBlockColor(35, 8, new Color(154, 161, 161));       // Light Gray Wool (35:8)
        registerBlockColor(35, 9, new Color(46, 110, 137));        // Cyan Wool (35:9)
        registerBlockColor(35, 10, new Color(126, 61, 181));       // Purple Wool (35:10)
        registerBlockColor(35, 11, new Color(46, 56, 141));        // Blue Wool (35:11)
        registerBlockColor(35, 12, new Color(79, 50, 31));         // Brown Wool (35:12)
        registerBlockColor(35, 13, new Color(53, 70, 27));         // Green Wool (35:13)
        registerBlockColor(35, 14, new Color(150, 52, 48));        // Red Wool (35:14)
        registerBlockColor(35, 15, new Color(25, 22, 22));         // Black Wool (35:15)

        this.getLogger().info(TextFormat.GREEN + "AutoMapartManager corregido cargado exitosamente.");
    }

    private void registerBlockColor(int id, int meta, Color color) {
        blockColors.put(id + ":" + meta, color);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("amap")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(TextFormat.RED + "Comando exclusivo para jugadores.");
                return true;
            }

            if (args.length < 1) {
                sender.sendMessage(TextFormat.RED + "Uso: /amap <nombre_imagen.png> [usar_inventario: true/false]");
                return true;
            }

            Player player = (Player) sender;
            String fileName = args[0].endsWith(".png") ? args[0] : args[0] + ".png";
            File file = new File(mapartFolder, fileName);

            if (!file.exists()) {
                player.sendMessage(TextFormat.RED + "No se encontró la imagen: " + fileName);
                return true;
            }

            boolean useOnlyInventory = args.length > 1 && Boolean.parseBoolean(args[1]);
            player.sendMessage(TextFormat.YELLOW + "Procesando imagen con soporte de metadatos de color...");

            try {
                BufferedImage image = ImageIO.read(file);
                int width = image.getWidth();
                int height = image.getHeight();

                if (width > 256 || height > 256) {
                    player.sendMessage(TextFormat.RED + "Imagen demasiado grande. Máximo: 256x256.");
                    return true;
                }

                Vector3 basePos = player.getPosition().floor();
                Level level = player.getLevel();
                PlayerInventory inv = player.getInventory();
                int placedCount = 0;

                for (int x = 0; x < width; x++) {
                    for (int z = 0; z < height; z++) {
                        int rgb = image.getRGB(x, z);
                        
                        if (((rgb >> 24) & 0xff) == 0) continue; // Salta transparencias

                        Color pixelColor = new Color((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);

                        // Buscamos el String identificador más adecuado (ej: "35:14")
                        String bestBlockKey = getClosestBlockKey(pixelColor, useOnlyInventory, inv);

                        if (bestBlockKey != null) {
                            String[] parts = bestBlockKey.split(":");
                            int id = Integer.parseInt(parts[0]);
                            int meta = Integer.parseInt(parts[1]);

                            // Obtenemos la instancia específica pasándole el ID y el Meta exacto de la lana
                            Block blockToPlace = Block.get(id, meta);

                            Vector3 targetPos = basePos.add(x, 0, z);
                            level.setBlock(targetPos, blockToPlace, true, true);
                            placedCount++;
                        }
                    }
                }

                player.sendMessage(TextFormat.GREEN + "¡Mapart generado con éxito! Bloques mapeados: " + placedCount);

            } catch (IOException e) {
                player.sendMessage(TextFormat.RED + "Error cargando la imagen.");
                this.getLogger().error("Excepción en AutoMapartManager: ", e);
            }
            return true;
        }
        return false;
    }

    private double getDifference(Color c1, Color c2) {
        int dR = c1.getRed() - c2.getRed();
        int dG = c1.getGreen() - c2.getGreen();
        int dB = c1.getBlue() - c2.getBlue();
        return Math.sqrt(dR * dR + dG * dG + dB * dB);
    }

    private String getClosestBlockKey(Color pixelColor, boolean useInventory, PlayerInventory inv) {
        String bestKey = null;
        double closestColor = 999.0;

        for (Map.Entry<String, Color> entry : blockColors.entrySet()) {
            String key = entry.getKey();
            
            if (useInventory) {
                String[] parts = key.split(":");
                int id = Integer.parseInt(parts[0]);
                int meta = Integer.parseInt(parts[1]);
                if (!inv.contains(Item.get(id, meta))) {
                    continue;
                }
            }

            double difference = getDifference(pixelColor, entry.getValue());
            if (difference < closestColor) {
                closestColor = difference;
                bestKey = key;
            }
        }
        return bestKey;
    }
}
