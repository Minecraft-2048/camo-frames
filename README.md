# Camo Frames

Fabric mod for **Minecraft 1.21.11**. A framed block is an empty wooden frame:
right click it with any block and it takes that block on — its texture, its
colour, its light. Sneak and right click with an empty hand to take the camo
back off; the block is returned to you.

Published on Modrinth: https://modrinth.com/mod/framed-blocks-for-fabric

## What it adds

- **49 shapes**: block, slab, double slab, vertical slab, slab corner, slab edge,
  stairs, half stairs, vertical stairs, slope, inner corner slope, vertical
  slope, pyramid, panel, double panel, pillar, half pillar, corner pillar, post,
  wall, fence, fence gate, floor board, wall board, bits, small bits, door, iron
  door, trapdoor, ladder, bars, button, stone button, large button, pressure
  plate, stone pressure plate, lever, torch, soul torch, redstone torch, sign,
  item frame, chest, secret storage, bookshelf, cushion, bouncy cube and glowing
  cube.
- A few do more than look the part: the bouncy cube throws you back, the glowing
  cube lights the room, the cushion breaks your fall, the secret storage is a
  container nothing on the outside gives away.
- **Framed armour** — helmet, chestplate, leggings and boots. Combine a piece
  with a block in an anvil and it takes that block on, worn and in the
  inventory; the protection follows the block chosen.
- **Framed hammer** and **framed wrench** to reshape placed blocks.

## Requirements

Fabric Loader and Fabric API, Minecraft 1.21.11, Java 21.

## Building

```
./gradlew build
```

The jar lands in `build/libs/`.

## About the name

Camo Frames is an independent Fabric mod. The idea of a block that borrows the
look of another comes from [FramedBlocks](https://modrinth.com/mod/framedblocks)
by XFactHD, which exists only for Forge and NeoForge. This mod is **not** a port
or a fork of it: it shares no code, texture or asset with the original and was
written from scratch against the Fabric API. It carries a different name so the
two are not mistaken for one another.

## Licence

MIT — see [LICENSE](LICENSE).
