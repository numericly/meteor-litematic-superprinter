# Super Printer

A Meteor addon for printing from litematica.

## WARNING

_This only works on servers with NO anti-cheat. Using it anywhere else will not work and likely result in a ban._

## How to use

For 1.21+ use fork of litematica maintained by [sakura-ryoko](https://github.com/sakura-ryoko)

Stand in range of the schematic and have the correct items in your inventory

_Use 1.5 blocks per second on a default paper server for maximum efficiency_

## How it works

For each missing block in the schematic, the printer iterates over candidate
hit positions, click faces, and player yaw/pitch values, simulates what
`BlockItem.getPlacementState` would produce for each combination, and uses the
first one whose resulting block state matches the schematic. The interaction is
then sent with the player's rotation and sneak state temporarily swapped out so
the placement matches the simulation.

It will match the following block state properties:

- BLOCK_HALF
- AXIS
- FACING
- FACING_HOPPER
- HORIZONTAL_FACING
- BLOCK_FACE
- BED_PART
- DOUBLE_BLOCK_HALF
- DOOR_HINGE
- ATTACHMENT
- ATTACHED
- HANGING
- ORIENTATION
- VERTICAL_DIRECTION
- ROTATION
- Slab type (single vs. double)
- Counted properties: LAYERS (snow), CANDLES, FLOWER_AMOUNT, SEGMENT_AMOUNT

### Post-place interactions

For blocks that need to be tweaked after placement, the printer right-clicks
the existing block to step it toward the required state:

- Open/close doors and trap doors
- Open/close fence gates (rotates to face the gate first)
- Toggle comparator mode (subtract / compare)
- Toggle redstone wire between connected and dot
- Cycle copper golem statue pose
- Adjust repeater delay
- Toggle levers
- Toggle daylight detector inverted state

### Sign editing

When the printer places a sign, the sign edit screen would normally pop open
and block further placements. The `auto-close-sign-gui` setting cancels that
screen if it opens within one second of the placement, leaving the sign blank
so the printer can continue.

## Known Issues

- Random placement issues
- Two players printing nearby causes issues
- Some block states not working including potted plants, tripwires, and water cauldrons
- Post place interactions fairly broken on high-ping

### Placed block tweaks not yet implemented

These are interactions performed on an already-placed block to modify its state:

- Water logging
- Putting out campfires
- Lighting candles
- Filling up respawn anchors
- Placing eyes in end portal frames
- Lighting nether portals

## Attribution

This project was initially a fork of https://github.com/kkllffaa/meteor-litematica-printer, so thank you to kkllffaa 
for the base of this addon
