package yan.lx.bedrockminer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import org.jetbrains.annotations.Nullable;
import yan.lx.bedrockminer.BedrockMinerMod;
import yan.lx.bedrockminer.utils.BlockUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Config {
    private static final File file = new File(FabricLoader.getInstance().getConfigDir().toFile(), "bedrockminer.json");
    private static @Nullable Config instance;
    public boolean debug;
    public List<String> blockWhitelist = new ArrayList<>();
    public List<String> blockBlacklist = new ArrayList<>();

    public Config() {
        Initialization();
        instance = this;
    }

    public void Initialization() {
        // 方块白名单
        blockWhitelist.add(BlockUtils.getId(Blocks.BEDROCK));                  // 基岩
        blockWhitelist.add(BlockUtils.getId(Blocks.END_PORTAL));               // 末地传送门
        blockWhitelist.add(BlockUtils.getId(Blocks.END_PORTAL_FRAME));         // 末地传送门-框架
        blockWhitelist.add(BlockUtils.getId(Blocks.END_GATEWAY));              // 末地折跃门
    }

    public static void load() {
        Gson gson = new Gson();
        try (Reader reader = new FileReader(file)) {
            instance = gson.fromJson(reader, Config.class);
            BedrockMinerMod.logger.info("已成功加载配置文件");
        } catch (Exception e) {
            if (file.exists()) {
                if (file.delete()) {
                    BedrockMinerMod.logger.info("无法加载配置,已成功删除配置文件");
                } else {
                    BedrockMinerMod.logger.info("无法加载配置,删除配置文件失败");
                }
            } else {
                BedrockMinerMod.logger.info("找不到配置文件");
            }
        }
    }

    public static void save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(instance, writer);
        } catch (IOException e) {
            BedrockMinerMod.logger.info("无法保存配置文件");
            e.printStackTrace();
        }
    }

    public static Config getInstance() {
        if (instance == null) {
            // 检查配置是否已加载
            instance = new Config();
            save();
        }
        return instance;
    }

}
