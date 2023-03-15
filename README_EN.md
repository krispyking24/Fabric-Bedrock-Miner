**中文** | [English](https://github.com/Bunnui/Fabric-Bedrock-Miner/blob/main/README_EN.md)

# Fabric-Bedrock-Miner
A Fabric client mod to "mine" bedrock!

# illustrate
This project fork modified from "LXYan2333/Fabric-Bedrock-Miner"<br>
Original project address: https://github.com/LXYan2333/Fabric-Bedrock-Miner

## 2023-3-16

1. Support destroying block whitelist
   - Bedrock
   - End Portal
   - End Portal - Frame
   - End gateway
2. Does not support breaking block blacklist (server)
   - Command Block
   - Chain Command Block
   - Repeating Command Block
   - Structure Void
   - Structure Block
3. Add commands
4. Remove default support for obsidian blocks
5. Fix some bugs
6. Optimize processing logic

### Command description
- `/bedrockMiner` enable/disable
- `/bedrockMiner block add whitelist <block>` add whitelist block list
- `/bedrockMiner block add blacklist <block>` add blacklist block list
- `/bedrockMiner block remove whitelist <block>` remove whitelist block list
- `/bedrockMiner block remove blacklist <block>` remove blacklist block list
- `/bedrockMiner debug true` enables debug mode
- `/bedrockMiner debug false` turn off debug mode
~~~~
# Showcase
https://www.youtube.com/watch?v=b8Y86yxjr_Y  
https://www.bilibili.com/video/BV1Fv411P7Vc

# Usage
Have the following items ready:
1. Efficiency V diamond (or netherite) pickaxe
2. Haste II beacon
3. Pistons
4. Redstone torches
5. Slime blocks

Right click bedrock **with an empty hand** to switch on/off.

While the mod is enabled, left click bedrock to "mine" it.

If my mod saves you tons of time, please considering leave me a star.

# Compile
Checkout to the corresponding Minecraft version and compile following the Fabric wiki.
