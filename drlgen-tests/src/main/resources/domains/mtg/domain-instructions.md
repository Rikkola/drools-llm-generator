# Magic: The Gathering Domain Instructions

## Domain Overview
Magic: The Gathering (MTG) is a trading card game where players build decks and compete using cards with various types, abilities, and mana costs.

## Key Terminology

### Card Types
- **Creature** - Cards that can attack and block, have power/toughness
- **Instant** - Cards played at any time
- **Sorcery** - Cards played only during your main phase
- **Enchantment** - Permanent cards with ongoing effects
- **Artifact** - Colorless permanent cards
- **Land** - Cards that produce mana (the resource to cast spells)
- **Planeswalker** - Powerful cards with loyalty abilities

### Mana and Colors
- **W** (White), **U** (Blue), **B** (Black), **R** (Red), **G** (Green)
- **Mana Value** (formerly CMC) - Total mana cost to cast a card

### Card Copy Rules
- **Standard rule**: Maximum 4 copies of any card (except basic lands)
- **Basic lands**: Unlimited copies (Forest, Island, Swamp, Mountain, Plains)
- **Special cards**: Some cards override the 4-copy limit:
  - "Relentless Rats" - Any number allowed
  - "Shadowborn Apostle" - Any number allowed
  - "Seven Dwarves" - Up to 7 copies allowed
- **copyLimit field**: Use -1 for unlimited, positive number for specific limit

### Deck Construction (Standard/Modern)
- **Main deck**: Minimum 60 cards
- **Sideboard**: Maximum 15 cards
- **No maximum** main deck size (but 60 is typical)

### Card Rarity
- **COMMON** - Most frequent, usually simple cards
- **UNCOMMON** - Less frequent, moderate complexity
- **RARE** - One per pack, often powerful
- **MYTHIC** - Rarest, most impactful cards

### Deck Archetypes by Mana Curve
- **Aggro** (avg mana value ≤ 2.0) - Fast, low-cost threats
- **Tempo** (avg 2.0-2.5) - Efficient threats with interaction
- **Midrange** (avg 2.5-3.5) - Balanced threats and answers
- **Control** (avg > 3.5) - Answers and late-game finishers

## Common Validation Patterns

### Card Copy Validation
```
copyLimit = -1  → unlimited copies allowed (basic lands, Relentless Rats)
copyLimit > 0   → check if copies <= copyLimit
copies > copyLimit AND copyLimit > 0 → INVALID
```

### Deck Size Validation
```
mainDeckSize < 60 → INVALID
sideboardSize > 15 → INVALID
mainDeckSize >= 60 AND sideboardSize <= 15 → VALID
```

### Mana Curve Classification
```
averageManaValue <= 2.0 → AGGRO
averageManaValue 2.0-2.5 → TEMPO
averageManaValue 2.5-3.5 → MIDRANGE
averageManaValue > 3.5 → CONTROL
```
