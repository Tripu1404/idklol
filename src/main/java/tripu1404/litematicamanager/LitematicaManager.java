package com.tripu1404.litematicamanager;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.level.Level;
import cn.nukkit.math.BlockVector3;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class LitematicaManager extends PluginBase {

    private File schematicFolder;

    // Clase interna para estructurar los bloques leídos del binario de C++
    public static class LitematicaBlock {
        private final BlockVector3 offset;
        private final int blockId;
        private final int runtimeId;

        public LitematicaBlock(BlockVector3 offset, int blockId, int runtimeId) {
            this.offset = offset;
            this.blockId = blockId;
            this.runtimeId = runtimeId;
        }

        public BlockVector3 getOffset() { return offset; }
        public int getBlockId() { return blockId; }
        public int getRuntimeId() { return runtimeId; }
    }

    @Override
    public void onEnable() {
        this.schematicFolder = new File(this.getDataFolder(), "schematics");
        if (!this.schematicFolder.exists()) {
            this.schematicFolder.mkdirs();
        }
        this.getLogger().info(TextFormat.GREEN + "LitematicaManager activado de emergencia.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("npaste")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(TextFormat.RED + "Comando exclusivo para jugadores en línea.");
                return true;
            }

            if (args.length < 1) {
                sender.sendMessage(TextFormat.RED + "Uso: /npaste <archivo>");
                return true;
            }

            Player player = (Player) sender;
            String fileName = args[0].endsWith(".lite") ? args[0] : args[0] + ".lite";
            File file = new File(schematicFolder, fileName);

            if (!file.exists()) {
                player.sendMessage(TextFormat.RED + "No se encontró el archivo: " + fileName);
                return true;
            }

            player.sendMessage(TextFormat.YELLOW + "Procesando estructura binaria...");
            BlockVector3 basePos = player.getPosition().asBlockVector3();
            Level level = player.getLevel();

            try {
                List<LitematicaBlock> blocks = loadLitematicaFile(file);
                int placedCount = 0;

                for (LitematicaBlock lBlock : blocks) {
                    BlockVector3 targetPos = basePos.add(lBlock.getOffset());
                    Block blockToPlace = Block.get(lBlock.getBlockId());
                    
                    // Colocación directa e instantánea sin actualizaciones pesadas en bucle
                    level.setBlock(targetPos, blockToPlace, true, true);
                    placedCount++;
                }

                player.sendMessage(TextFormat.GREEN + "¡Listo! Se inyectaron " + placedCount + " bloques.");
            } catch (IOException e) {
                player.sendMessage(TextFormat.RED + "Error crítico leyendo el archivo de estructura.");
                this.getLogger().error("Excepción en LitematicaManager: ", e);
            }
            return true;
        }
        return false;
    }

    private List<LitematicaBlock> loadLitematicaFile(File file) throws IOException {
        List<LitematicaBlock> list = new ArrayList<>();

        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            byte[] allBytes = dis.readAllBytes();
            ByteBuffer buffer = ByteBuffer.wrap(allBytes);
            buffer.order(ByteOrder.LITTLE_ENDIAN); // Mantiene compatibilidad con el volcado crudo de memoria x86/ARM

            if (buffer.remaining() < 8) return list;

            // Lee el size_t de C++ (8 bytes / uint64_t)
            long size = buffer.getLong();

            for (long i = 0; i < size; i++) {
                if (buffer.remaining() < 20) break; // 12 bytes de BlockPos + 4 del ID + 4 del RuntimeID

                int x = buffer.getInt();
                int y = buffer.getInt();
                int z = buffer.getInt();
                BlockVector3 offset = new BlockVector3(x, y, z);

                int blockId = buffer.getInt();
                int runtimeId = buffer.getInt();

                list.add(new LitematicaBlock(offset, blockId, runtimeId));
            }
        }
        return list;
    }
}
