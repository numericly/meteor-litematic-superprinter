# Super Printer

A Meteor addon for printing from litematica.

## WARNING

_This only works on servers with NO anti-cheat. Using it anywhere else will not work and likely result in a ban._

## How to use

Just have the layers render and the items in your inventory and turn it on

Use 1.5 blocks per second on default paper for maximum efficiency

### How it works

This mod simulates the block state after placing and will match the following block properties

- BLOCK_HALF
- AXIS
- FACING
- HOPPER_FACING
- HORIZONTAL_FACING
- BLOCK_FACE
- BED_PART
- DOUBLE_BLOCK_HALF
- DOOR_HINGE
- ATTACHMENT
- ATTACHED

It will also close trap doors and doors if needed

### Known Issues

- Won't place doors sometimes
- Won't close fence gates
- Will not place ignore: signs, potted plants, banners, and many more
