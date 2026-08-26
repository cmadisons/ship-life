# 🚀 Ship Life

A Minecraft mod where you live on a giant space ship — quests, floors you unlock
one at a time, arcade games, pets, races and events.

**Status: chapters 1 to 4 are playable.** The rest of this file is the full design;
what is built so far is listed under [What works today](#what-works-today).

- 📄 [QUESTS.md](QUESTS.md) — all 650 quests

## What works today

- **Ship Life world type** on the Create World screen. Whatever mode you pick alongside it, you play survival.
- The **town** — three houses, the lawn, the bushes — and the **ship**, all fourteen floors, built into an empty void.
- **Quest Book** locked in slot 9 and the **passport** locked in slot 8, neither of which can be lost.
- **Chapter 1** — wash the ten dishes with the sponge and the towel, bin them, mow all 121 squares of the lawn and whack the weeds, and find the penny in the second bush. $5.00 + $94.99 + $0.01.
- **Chapter 2** — board the ship, get your passport off security, ride the lift to floor 5 and find your bathroom, fridge, bed and TV.
- **Chapter 3 and 4** — Charlie in the lobby, and his quest: the plunger meter, the bed, and the moldy fridge, with mopping when you get it wrong.
- The **elevator** — locked floors don't show, a floor your quest is opening shows with a lock, and the doors close, the car whirs and the doors open.
- The **star** and the **clock** on the action bar: how far the quest is, in green, yellow or red, and the ship's date and time on a real 20-minute day.

Still to build: the arcade and its three games, the pool, the race track, the pets,
the shops, the events, floors 9 and 10, and a proper drawn heads-up display.

### Build it

```bash
export JAVA_HOME="$HOME/Library/Application Support/minecraft/runtime/java-runtime-epsilon/mac-os/java-runtime-epsilon/jre.bundle/Contents/Home"
./gradlew build
```

The mod lands in `build/libs/`.

---

## What it is

**Ship Life** is picked in the New World **options**, like creative / hardcore / survival.
Pick any of those alongside it and you still play **survival**.

## Core systems

| System | How it works |
|---|---|
| **Quest Book** | Locked in **slot 9**. Shows the quest name, what to do right now, the rewards, the distance, and your money. Click a quest to set its star. |
| **Passport** | Locked in **slot 8**. Unlocks floors in the elevator. |
| **Waypoint star** | A green star at the quest, visible through walls at any distance, with the distance under it. One quest tracked at a time, and it points only at the **current part**. |
| **Distance colours** | 🟢 1–499 · 🟡 500–999 · 🔴 1000+ |
| **Clock** | Top-right, with the in-game date and month. **Real time** — 20 real minutes = 1 day, 30 days = 1 month = **10 real hours**. It keeps ticking with the game closed. |
| **Calls** | A ringing sound and a popup on screen. |
| **Elevator** | Locked floors don't show at all — a floor you have a quest for shows with a 🔒. Doors close, the car moves with a whirring sound, doors open, and the floor you're going to shows on a display up top. |
| **Money** | 💵 dollars · 🎟️ arcade tickets · 🎟️ event tickets. They're separate. Tickets can go below 0 — you just can't buy anything until you can afford it. |

## The floors

| # | What's there | How you unlock it |
|---|---|---|
| 1 | Lobby — the security desk and Charlie's table | Passport |
| 2 | 🕹️ Arcade | Quest 5 |
| 3 | 🏊 Swimming pool — laps and records | Quest 5 |
| 4 | 🍽️ Buffet — eat and get your hearts back | Quest 5 |
| 5 | 🛏️ Your room — bathroom, fridge, bed, TV, phone | Passport |
| 6 | 🏎️ Race car track, and Charlie | Buy a cat |
| 7 | 🎪 Event place | Charlie's 5-laps-in-2-minutes quest |
| 8 | 🏪 Store — free quests, 1 at a time | Get 50 arcade tickets |
| 9 | ⚔️ Fight room | Swim a lap in 30 seconds |
| 10 | 👹 Boss room | Do an event |
| 11 | 🎁 A free reward every month you play | 1000 event tickets (buys 11, 12 and 13) |
| 12 | 🐾 Pet store | " |
| 13 | 🍽️ The Keg — a fancy restaurant | " |
| 14 | ❓ | 4.9% roll on floor 11 |

## The story

### Chapter 1 — the mail
Get **$100** and you can live on the ship for 1 month.

**Quest 1 — Make the Money.** The book shows all three parts and what they pay up front,
but you do them in order, and the star only points at the part you're on.

| Part | Where | What | Pay |
|---|---|---|---|
| 1 | First house | Wash 10 dishes — left-hold the **sponge**, right-hold the **towel**, one dish at a time. Both get dirty, then throw them in the garbage. | $5.00 |
| 2 | Second house | Mow the whole yard -- right-click all 121 squares of grass inside house two, and each cut square turns to moss -- then right-click each of the 10 weeds once. | $94.99 |
| 3 | Third house | Find the penny — it's in the **2nd bush from the left**. | $0.01 |

### Chapter 2 — you go to the ship
- **Quest 2** — ① go in the ship ② talk to security at the desk, who gives you your **passport** (floors 1 and 5).
- **Quest 3** — ① go to the elevator ② press the floor 5 button ③ check out your floor: right-click the bathroom, the fridge, your bed and your TV.

Quests come from finishing other quests, talking to people, reaching new floors, and events.

### Chapter 3 — the call
> *"Come to floor 1, I am going to be at the table at the lobby."*

- **Quest 4** — ① go to floor 1 ② talk to the person at the table.

### Chapter 4 — Charlie
> *"Hi, I am Charlie the manager. I will give you quests to unlock floors, upgrade your
> passport, get new friends and do activities."*

**Quest 5**, all in your room:

1. 🪠 **The toilet is plugged.** Right-hold the plunger and let go in the green.
   🔴 red 5 sec → 🟢 green 3 sec → 💥 it explodes. If it explodes, mop it up — hold and move around the bathroom.
2. 🛏️ **The bed isn't made.** Right-click it.
3. 🧀 **The fridge has moldy food.** Same meter — miss it and the food falls on the floor, then mop it.

**Rewards:** floors 2, 3 and 4, a 📱 phone (it sits beside your bed, for ordering food) and a 🛏️ bed to sleep on.

## The race track (floor 6)

Three lanes, and the track is curvy. Hit an NPC racer and they slow down; they hit you and
you slow down; and if one is beside you, you can't crash into them or you slow down.

| Action | Key |
|---|---|
| Boost — 1 gas a second, double the speed of holding nothing | right-hold or **W** |
| Get gas back | left-hold or **S** |
| Left | **A** |
| Right | **D** |

You start with **250 gas**. Run out and you're out of the race.

**Charlie's quest:** 5 laps in 2 minutes. An easy lap is about 25 seconds — five of those is
too slow. A clean lap, no crashes and no running out of gas, is about 17.

## The arcade (floor 2)

| Game | Tickets |
|---|---|
| Pac-Man | 5 for every new record |
| Galaga | 5 per round you pass |
| Snake | 1 per food you eat |

**Prizes**

| Cost | Prize |
|---|---|
| 25 🎟️ | The next 3 quests — drawn at random from the [250-quest pool](QUESTS.md) once you own floors 8, 9 and 10. They pay out in event tickets. |
| 10 🎟️ | A pet |

## Pets

| Pet | What it does |
|---|---|
| 🦁 Lion | Helps you fight |
| 🐕 Dog | Swimming boost every 7 seconds |
| 🐈 Cat | Unlocks floor 6, permanently |
| 🐬 Dolphin | Does a random one of the other three each day |
| 💀 Skeleton | Combat boost — 50 event tickets at the pet store |
| 👤 Shadow | Adds 0.25 of every pet's boost — 250 event tickets at the pet store |

All your pets follow you at once, and you can buy as many as you like.
**Two of a kind doubles that boost** — the cat is the exception, since its unlock is permanent.

Pet food at the pet store makes **one** pet ×1.1 better for 100 event tickets. It stacks and
compounds, up to **×2 per pet**.

## Events (floor 7)

You can't do any event without floor 7.

| Event | When | What happens |
|---|---|---|
| 👻 **Spooky Shooter** | Every Sunday in October | You're in a crowd of 100 people, some of them lookalikes. A picture of one shows at a time and you find and shoot that one. Right = **1–200 event tickets** (targets holding a lightsaber or a shield are worth more). Wrong = **–50**, and that innocent dies and is gone. Runs all day; the crowd refills to 100 with new looks. |
| 🎄 **Christmas** | Every Sunday in December | The same, but 100 Santa Clauses that look mostly the same — no two identical. Right = **+250**. Innocent = **–50**. |
| 🕹️ **Summer Break** | Weekends in summer and March break | **Double arcade tickets.** |
| 📜 **Quest Day** | Every other Monday | Four quests — a super easy, an easy, a medium and a hard. Finish all four for **500 event tickets**, then get another four, and so on. |
| ⭐ **May the Fourth** | May 4 | The ship takes you to a Star Wars planet. **25 event tickets** per lightsaber fight you take, **100** if you win. |

## Shops

**Floor 8 — the store.** Free quests, one at a time.

**Event ticket shop**

| Item | Cost |
|---|---|
| Floors 11, 12 and 13 (all three) | 1000 🎟️ |
| ⭐ Go To Event Star — one use, skip straight to any event you pick | 250 🎟️ |
| ×2.5 tickets per event, for your next 3 ticket events | 1000 🎟️ |

**Floor 11 — a free reward every month you play** (every 10 real hours)

| Reward | Chance |
|---|---|
| 📞 A random phone store — call store, arcade store or event store. You carry the phone but can call from anywhere, they stack on the one phone, and you can get more than one of each. | 70% |
| ×2.5 once, on a single event that pays event tickets | 25% |
| 🔓 Floor 14 | 4.9% |
| ×2.5 on event tickets, **permanently** | 0.1% |

**Floor 12 — the pet store.** Pet food, the Skeleton and the Shadow.

**Floor 13 — The Keg.** Food for yourself, 25 event tickets each, and the boost lasts one
in-game day: 💧 water for swimming speed, a racing dish for more gas and a faster car, and a
fighting dish for more hearts.

## Fighting

**Floor 9** — endermen, creepers, ghasts and random hard enemies.
**Floor 10** — the bosses: **Arachnes** and the **Ender Dragon**.

---

Made by [@cmadisons](https://github.com/cmadisons)
