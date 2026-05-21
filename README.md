# Super Printer

A Meteor addon for printing from litematica.

## WARNING

_This only works on servers with NO anti-cheat. Using it anywhere else will not work and likely result in a ban._

## How to use

For 1.21+ use fork of litematica maintaind by [sakura-ryoko](https://github.com/sakura-ryoko)

Stand in range of the schematic and have the correct items in your inventory

_Use 1.5 blocks per second on a default paper server for maximum efficiency_

## How it works

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

## Known Issues

- If two people are running the printer and try to place a block in the same location, it will cause extra blocks to be placed
- Won't place doors sometimes
- Won't close fence gates
- Will not place ignore: signs, potted plants, banners, and many more

## Attribution

This project was initially a fork of https://github.com/kkllffaa/meteor-litematica-printer, so thank you to kkllffaa 
for the base of this addon
