[繁體中文](./README_TW.md) | [简体中文](./README.md) | **English**

# Fabric-Bedrock-Miner

A Fabric client mod to "mine" bedrock!

# Description

This project is a modified fork of [LXYan2333/Fabric-Bedrock-Miner](https://github.com/LXYan2333/Fabric-Bedrock-Miner) <br>

## Default block lists

#### Block whitelist (break supported)
- Bedrock

#### Server block blacklist (Break is not supported, cannot be changed by command, built-in filter)
- Barrier
- Command Block
- Chain Command Block
- Repeating Command Block
- Structure Void
- Structure Block
- Jigsaw Block

#### Block for adding command filters
- Air
- Replaceable blocks

### Command description
- `/bedrockMiner` on/off
- `/bedrockMiner disable` disable the mod (the mod will not continue to process after it is turned on)
- `/bedrockMiner behavior floor add <floor>` add a floor to the blacklist.
- `/bedrockMiner behavior floor remove <floor>` remove a floor from the blacklist.
- `/bedrockMiner behavior block whitelist add <block>` add a block to the whitelist
- `/bedrockMiner behavior block whitelist remove <block>` remove a block from the whitelist
- `/bedrockMiner task add <x, y, z>` add a task
- `/bedrockMiner task shortWait <bool>` set short custom wait time
- `/bedrockMiner task clear` clear the task
- `/bedrockMiner debug true` enables debug mode
- `/bedrockMiner debug false` turn off debug mode

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

If my mod saves you tons of time, please consider leaving a star.

# Compile

Checkout to the corresponding Minecraft version and compile following the Fabric wiki.

## QQ Group

![群二维码](https://github.com/Bunnui/Fabric-Bedrock-Miner/assets/37466008/7f1c2bc7-876b-4d34-9534-c72a3b555a2a)
