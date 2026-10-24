#!/usr/bin/env python3
"""
Generates src/main/java/com/sweatsenhancements/gui/SweatsConfig.java - the config object MoulConfig renders.

Run after Features.java changes:
    python3 tools/gen_gui.py

Why generate instead of hand-write: MoulConfig discovers options by reflecting over annotated
fields, so every feature needs a real field in a real class. 555 of those is not something to
maintain by hand, and the file must be regenerated whenever a feature starts working.

The panel settings (Scoreboard, Tab List) are written out in full here, in the order and with the
names the reference settings pages use, so the screen can be compared line by line against the
thing being replaced. Each entry is (kind, field, key, name, description, default, extra...):

    bool      - a switch
    text      - a line of text
    slider    - a whole number
    percent   - a slider that holds 1.0 for 100, stored as a whole number
    dropdown  - a choice, stored as the text of the choice
    list      - an orderable list, stored semicolon separated (see core/TextList)
    button    - a button that runs a runnable
    accordion - a folding section; everything after it belongs to it until the next accordion
"""
import re
import sys

SRC = "src/main/java/com/sweatsenhancements/config/Features.java"
IMPL = "src/main/java/com/sweatsenhancements/config/Implemented.java"
OUT = "src/main/java/com/sweatsenhancements/gui/SweatsConfig.java"

# ---------------------------------------------------------------- read the features
src = open(SRC, encoding="utf-8").read()
impl = open(IMPL, encoding="utf-8").read()

features = []          # (id, category, name)
for m in re.finditer(r'add\("([A-Z]{2,4}\d{3})",\s*"([^"]+)",\s*"(.*?)"\);', src, re.S):
    features.append((m.group(1), m.group(2), m.group(3).replace('\\"', '"')))

categories = re.findall(r'newCategory\("([^"]+)"\)', src)

live = set(re.findall(r'"([A-Z]{2,4}\d{3})"', impl))
off_block = impl.split("DEFAULT_OFF")[1] if "DEFAULT_OFF" in impl else ""
default_off = set(re.findall(r'"([A-Z]{2,4}\d{3})"', off_block))
if not features:
    sys.exit("parsed 0 features - Features.java format changed?")
if not live:
    sys.exit("parsed 0 implemented ids - Implemented.java format changed?")

missing = [i for i in live if i not in {f[0] for f in features}]
if missing:
    sys.exit("implemented ids not present in Features.java: %s" % missing)


# ---------------------------------------------------------------- helpers
# switches that live in their own settings category rather than in the Misc list
MOVED = {"MS063", "MS085", "MS223"}


def ident(cat):
    return re.sub(r'[^A-Za-z0-9]', '', cat)


# Names and descriptions for the features that are actually built, written the way SkyHanni
# writes its own rows: a few words on the left, the sentence explaining it on the right.
SPLIT = {
    "CH014": ("Fire Sale", "Hide the repeating fire sale reminder messages."),
    "CH031": ("Hide Empty Chat Lines", "Hide whitespace-only or empty chat lines; leave meaningful messages visible."),
    "CH033": ("Welcome Message", "Hide the welcome message when entering SkyBlock."),
    "CH034": ("Powder Mining", "Hide the powder gain messages while mining."),
    "CH200": ("Copy Chat Message", "Add a clickable copy/suggest button beside ordinary chat lines."),
    "CH201": ("Hide Watchdog / Info", "Hide watchdog announcements and player-information messages."),
    "CH202": ("Hide Friend Join / Leave", "Hide Friend > join and leave notifications."),
    "CH203": ("Hide Zombie Rare Drops", "Hide potato, poisonous potato and carrot rare-drop notices."),
    "CH204": ("Hide Selling Ranks", "Hide player chat advertising the sale of ranks."),
    "CH205": ("Hide Game Invites", "Hide server game invitation messages."),
    "CH206": ("Hide Server Announcements", "Hide recognised server announcements and promotional notices, not player chat."),
    "MS212": ("Mute Jerry-chine Gun", "Mute villager sounds while holding a Jerry-chine Gun."),
    "MS213": ("Middle-Click SkyBlock Menus", "Treat left-clicks on recognised SkyBlock menu items as middle-clicks."),
    "MS214": ("Prevent Ability Item Placement", "Prevent accidental block placement with specified ability items."),
    "MS215": ("Mute Zombie Drop Chime", "Mute a rare-drop chime immediately after a nearby zombie death."),
    "MS216": ("Show Unclaimed Contests", "Highlight contest rewards ready to claim in Your contests."),
    "MS217": ("Hide Flaming Fists", "Hide only the recognisable Flaming Fists skull stand cosmetic."),
    "MS218": ("Show Item ID (SweatsEnhancements Extra)", "Optional internal item ID; this is not part of the reference Item Data settings."),
    "MS219": ("Show Stacking Counter", "Show the stacking enchant, count and next threshold or MAX LEVEL."),
    "MS220": ("Show Admin Item Tag", "Show an admin-item warning with the matching metadata reason."),
    "MS221": ("Highlight Active Wardrobe Slot", "Highlight the wardrobe item labelled Equipped."),
    "MS222": ("Item Safeguard", "Hold an item and use /sweatsenhancements safeguard, or set its key here. Uses server UUID when available; UUID-less items match by type, metadata and NBT."),
    "MS224": ("Auto-Close Secret Chest", "Close only a menu titled Secret Chest; leave other chests open."),
    "MS225": ("Block Outline", "Draw a cyan outline around the block you are looking at."),
    "MS226": ("Clicks Per Second", "Show your local left clicks per second as a draggable HUD."),
    "DU001": ("Dungeon Run HUD", "Show server elapsed time and clear progress, then verified completion lines; draggable with H."),
    "DU002": ("Dungeon Requeue", "Use /sweatsenhancements requeue or set the Dungeon Requeue Key here. Sends /joindungeon for the last observed F1–F7 or M1–M7 floor; never automatic."),
    "DU003": ("No Cursor Reset", "Keep the pointer near its last position between GUI screens instead of recentering it."),
    "DU004": ("Mark Clicked Chests and Levers", "Show Clicked! above every chest or lever you right-click in a dungeon; no secret-count requirement."),
    "DU005": ("Blessing Display", "Display all observed Power, Time, Wisdom, Stone and Life blessings from the dungeon tab footer."),
    "DU006": ("Box Starred Mobs", "Box the living mob beneath a dungeon name stand marked with a star."),
    "DU007": ("Box Dropped Keys", "Box labelled Wither and Blood key stands while visible in a dungeon."),
    "DU008": ("Wither Door Boxes", "Red with no key, green when the server sidebar's Keys counter is positive; nearby doors only."),
    "DU009": ("Blood Mob Boxes", "Box live blood-room mobs associated with their name stands near the Watcher."),
    "DU010": ("Blood Timer", "Draggable elapsed timer from observed blood-door opening until the Watcher's completion line."),
    "DU011": ("Blood Ready Alert", "Notify after the Watcher announces the blood phase is done."),
    "DU012": ("Fel Mobs Display", "Show independently configurable boxes around recognised dungeon Fels."),
    "DU013": ("Bat Mobs Display", "Show independently configurable boxes around dungeon bats."),
    "DU014": ("Terracotta Elapsed Timer", "Show elapsed time from Sadan's army announcement until ENOUGH; no predicted countdown."),
    "DU015": ("End-of-Run Extra Stats", "Show a compact chat summary of server score, boss time, and observed F6 terracotta span."),
    "DU016": ("Dungeon Map HUD", "Show the received dungeon map using Minecraft's own map renderer (NEF-style). No custom room grid or labels."),
    "DU017": ("Wider Lever Hitbox", "Client-side larger lever selection area, only inside a dungeon."),
    "DU018": ("Wider Button Hitbox", "Client-side larger button selection area, only inside a dungeon."),
    "DU019": ("Clicked Chest/Lever Sound", "Play a local chime when a dungeon chest or lever is right-clicked; independent of clicked labels."),
    "DU021": ("Hide Blessing Announcements", "Hide only recognised dungeon blessing-found chat lines; does not filter player chat."),
    "DU022": ("Boss Highlights", "Outline positively named Bonzo, Scarf and Professor entities on their floors."),
    "DU023": ("Scarf Minion Highlights", "Outline identified Scarf minions on F2/M2 only."),
    "DU024": ("Dropped Spirit Bow Highlight", "Outline nearby dropped Spirit Bow items during a dungeon."),
    "DU025": ("Hide Boss / Watcher Chat", "Hide recognised boss and Watcher chat lines, not party or player chat."),
    "MS223": ("Smooth AOTV/AOTE View", "Visually smooth confirmed server teleports; never changes player movement."),
    "MS007": ("Hide Damage Splashes", "Hide the damage numbers that appear when you hit something."),
    "MS027": ("Hide Dead Entities", "Hide mobs that are already dead instead of letting them lie there."),
    "MS028": ("TPS Display", "Show the locally estimated server tick rate on a movable HUD; this is not a server-reported guarantee."),
    "MS200": ("Fullbright", "Brighten dark areas while enabled; restore your brightness when off."),
    "MS003": ("Hide Ground XP", "Hide dropped experience bottles and XP orbs on your client."),
    "MS201": ("Current Pet HUD", "Show your active pet from the tab list when available, with a generic vanilla item icon rather than a custom portrait."),
    "MS204": ("Always Sprint", "Hold the sprint input while enabled; Minecraft handles sprint restrictions."),
    "MS205": ("Hide Falling Blocks", "Hide falling block entities on your client only."),
    "MS206": ("Mute Teleport Item Sound", "Mute the teleport sound while holding Aspect of the End or Void."),
    "MS207": ("Disable Rain Rendering", "Hide rain on your client; server weather is unchanged."),
    "MS208": ("Wardrobe Shortcut", "Send /wardrobe with the key set below while enabled."),
    "MS209": ("Equipment Shortcut", "Send /equipment with the key set below while enabled."),
    "MS210": ("Wardrobe Slot Shortcuts", "Select wardrobe slots 1-9 with keys while the Wardrobe menu is open."),
    "MS211": ("Warp Shortcuts", "Use the configured keys to warp to your island, Hub or Dungeon Hub."),
    "FI030": ("Fishing Bobber Timer", "Show how long your bobber has been in the water, on the bobber itself."),
    "MS063": ("Compact Tab List", "Redraw the tab list with the padding and the advert rows dropped."),
    "MS085": ("Custom Scoreboard", "Replace the sidebar with SweatsEnhancements's own, with a title and footer of your choosing."),
}


# Every working toggle gets a truthful, actionable description. Keep this in the
# generator rather than patching the generated SweatsConfig by hand.
SPLIT.update({
    "MS222": ("Item Safeguard", "Protect the held or hovered item with /sweatsenhancements safeguard or its key. Uses server UUID when present; otherwise matches item type, metadata and NBT (identical copies are protected together)."),
    "MS227": ("No Hurt Camera", "Disable only the local camera shake when your player is hurt; damage still applies."),
    "MS228": ("Hide Blank Pane Tooltips", "Hide tooltips for unnamed decorative glass panes in menus; named items keep their tooltips."),
    "DU008": ("Wither Door Boxes", "Outline nearby Wither Doors. The sidebar Keys count controls red/green, but a numeric count alone cannot identify Wither versus Blood keys."),
    "DU016": ("Dungeon Map HUD", "Show the received dungeon map with Minecraft's map renderer. Movable in the HUD editor; no room labels or custom grid."),
    "DU020": ("Dungeon Score HUD", "Show a separate movable estimate from observed dungeon sidebar and tab values; missing inputs show ? and totals are not a guaranteed final score."),
    "DU026": ("Catacombs Gate Pass Floors", "Show observed floor information on a Catacombs gate pass item tooltip; no invented floor unlocks."),
    "DU027": ("Dungeon Key Pickup Alert", "Show a local alert for a recognised Wither or Blood key pickup. A sidebar count rise without a named pickup shows an unknown-type alert."),
    "DU028": ("Party Key Pickup Announcement", "Send a rate-limited /pc announcement for a recognised key pickup. A sidebar-only rise does not identify the key type."),
    "DU030": ("Estimated Score 270 Alert", "Show a one-time local alert when the available score estimate first reaches 270; not a guaranteed final score."),
    "DU031": ("Estimated Score 300 Alert", "Show a one-time local alert when the available score estimate first reaches 300; not a guaranteed final score."),
    "DU032": ("Hide Missing Sign Error", "Hide the exact server 'Unable to locate sign' error line; other chat stays visible."),
    "DU033": ("Hide Normal Mob HP Labels", "Hide unstarred ordinary dungeon mob HP nameplates. Preserve bosses, keys and starred mob labels."),
    "DU034": ("Hide Terminal Tooltips", "Hide item tooltips only inside recognised dungeon terminal menus; does not change terminal clicks."),
    "DU035": ("Party Blood Ready Announcement", "Send /pc Blood Ready! once on the confirmed Watcher completion cue; independently opt-in."),
    "DU036": ("Spirit Leap Teammate Picker", "Open the retained manual teammate picker on a Spirit Leap menu. It does not select a player for you."),
    "SL053": ("Slayer Sidebar Overlay", "Display the retained Slayer objective and progress overlay; customize position, scale and visibility on the Slayer Overlay page."),
    "SL054": ("Slayer Boss Kill Timer", "Time an observed active Slayer boss fight; no predicted spawn time."),
    "SL055": ("Hide Hellion Shield Spam", "Suppress the identified Hellion Shield and wrong-attunement hit messages; other chat is untouched."),
    "SL056": ("Auto Open Maddox on Server Cue", "Open Maddox's menu only after a recognised server cue; separate from the chat-link option."),
    "SL057": ("Click Chat to Open Maddox", "Make the recognised Maddox chat line clickable to open his menu manually."),
    "SL058": ("Miniboss Spawn Title", "Show a local title on a recognised Slayer miniboss spawn cue."),
    "SL059": ("Miniboss Spawn Sound", "Play a local sound on a recognised Slayer miniboss spawn cue."),
    "SL060": ("Slayer Boss Box", "Draw a bounding box around a positively identified Slayer boss."),
    "SL061": ("Slayer Boss Model Outline", "Outline the identified Slayer boss model rather than drawing a bounding box."),
    "SL062": ("Slayer Miniboss Box", "Draw a bounding box around positively identified Slayer minibosses."),
    "SL063": ("Voidgloom Beacon Waypoint", "Mark an identified Voidgloom beacon with a local waypoint."),
    "SL064": ("Voidgloom Beacon Tracer", "Draw a local tracer toward an identified Voidgloom beacon."),
    "SL065": ("Voidgloom Beacon Alert", "Show a local cue when an identified Voidgloom beacon appears."),
    "SL066": ("Blaze Fire Pillar Alert", "Show a local warning when an identified Blaze Slayer fire pillar is nearby."),
    "SL067": ("Blaze Attunement Model Outline", "Outline identified Blaze attunement minions in their attunement colour."),
    "SL068": ("Blaze Attunement Model Fill", "Fill identified Blaze attunement minion models in their attunement colour."),
    "SL069": ("Slayer Boss Health Label", "Display health for a positively identified active Slayer boss."),
    "SL070": ("Hide Slayer Progress Chat", "Hide only recognised Slayer progress lines; preserve player chat and other announcements."),
})

# Visual order only: IDs, defaults and persisted config keys never change.
ORDER = {
    "Chat": "CH033 CH031 CH014 CH034 CH201 CH202 CH203 CH204 CH205 CH206 CH200",
    "Misc": "MS201 MS028 MS226 MS200 MS207 MS027 MS003 MS007 MS205 MS225 MS227 MS228 MS204 MS206 MS212 MS215 MS217 MS218 MS219 MS220 MS222 MS221 MS216 MS208 MS209 MS210 MS211 MS213 MS214 MS224",
    "Dungeon": "DU001 DU020 DU016 DU002 DU003 DU036 DU006 DU012 DU013 DU007 DU008 DU009 DU022 DU023 DU024 DU005 DU010 DU011 DU035 DU027 DU028 DU030 DU031 DU014 DU004 DU019 DU017 DU018 DU026 DU021 DU025 DU032 DU033 DU034",
    "Fishing": "FI030",
}

# the element names the Appearance list starts with - kept in step with core/PanelSlots.ALL
# The elements this panel can draw, in the order a new install starts with.
#
# The player cut this list down themselves: "remove the area,server,player,motes,cold,soulflow,
# flintarrow,skills,stats,pet,party,active effect,website and extra from every list as either they
# do not do anything,not in the game or i dont need them in there". Everything they named is gone
# from the panel's own list of elements, not just from this page, so a row that used to land in one
# of those slots is not drawn at all.
APPEARANCE = ["SERVER_DATE", "TIME", "SEASON", "ISLAND", "LOCATION", "DUNGEON_SIDEBAR", "PROFILE", "SB_LEVEL", "PURSE",
              "BANK", "INTEREST", "BITS", "GEMS", "COPPER", "HEAT", "NORTH_STARS", "POWDER",
              "FORGES", "COOKIE", "POWER", "TUNING", "ELECTIONS", "EVENT", "SLAYER"]

# the same list plus the blank line: what the draggable list offers, while the default order stays
# without it - this server sends its own gaps and they are kept
APPEARANCE_DEFAULT = APPEARANCE
APPEARANCE = APPEARANCE_DEFAULT + ["EMPTY_LINE"]

EVENTS_PRIORITY = ["Farming Contest", "Mining Fiesta", "Fishing Festival", "Mythological Ritual",
                   "Spooky Festival", "Season of Jerry", "New Year Celebration", "Traveling Zoo",
                   "Hoppity's Hunt", "Better Together", "Election Booth Opens"]

CHUNKED = ["PURSE", "BANK", "BITS", "GEMS", "COPPER", "HEAT", "NORTH_STARS"]

TAB_LINES = ["PROFILE", "SB_LEVEL", "BANK", "INTEREST", "GEMS", "POWER", "ELECTIONS", "EVENT",
             "POWDER", "FORGES", "FAIRY_SOULS", "COPPER", "HEAT", "NORTH_STARS"]

TAB_LINES_DEFAULT = ["BANK", "INTEREST", "GEMS", "POWER", "SB_LEVEL", "PROFILE", "POWDER", "FORGES",
                     "EVENT", "ELECTIONS"]

TAB_LINE_LABELS = ["Profile", "SB Level", "Bank", "Interest", "Gems", "Power", "Elections",
                   "Event", "Powder", "Forges", "Fairy Souls", "Copper", "Heat", "North Stars"]

# --------------------------------------------------------------------------- the example text
#
# What each entry of a draggable list is drawn as in the config screen, in the colours the element
# itself is drawn in on the scoreboard - the player's own reference for this: "would like to see
# the list options have their respective color". The stored value stays the plain name (the lists
# hold positions into NAMES), so this is display only.
EXAMPLES = {
    "SERVER_DATE": "\u00a7f09/26/26 \u00a78hub-4",
    "TIME": "\u00a7f12:30pm \u00a7b\u263d",
    "SEASON": "\u00a7bWinter 19th",
    "ISLAND": "\u00a77\u23e3 \u00a7aSkyblock Hub",
    "LOCATION": "\u00a77\u2726 \u00a7bVillage",
    "DUNGEON_SIDEBAR": "\u00a7e[B] \u00a7b[MVP] Teammate \u00a7a4,260\u00a7c\u2764",
    "PROFILE": "\u00a7e\u00a7lProfile: \u00a7aCucumber",
    "SB_LEVEL": "\u00a7fSB Level: \u00a78[\u00a7b217\u00a78] \u00a7b51\u00a73/\u00a7b100 XP",
    "PURSE": "\u00a7fPurse: \u00a7682,935,529",
    "BANK": "\u00a7fBank: \u00a760",
    "INTEREST": "\u00a7fInterest: \u00a7e15h 22m 43s",
    "BITS": "\u00a7fBits: \u00a7b1,334",
    "GEMS": "\u00a7fGems: \u00a7a17",
    "COPPER": "\u00a7fCopper: \u00a7e359,485",
    "HEAT": "\u00a7fHeat: \u00a7c14",
    "NORTH_STARS": "\u00a7fNorth Stars: \u00a7b756",
    "POWDER": "\u00a7aPowders \u00a77Mithril Powder: \u00a7a58,608",
    "FORGES": "\u00a79Forges \u00a77 1\u00a78) \u00a7bRefined Mithril",
    "COOKIE": "\u00a7dCookie Buff\u00a78: \u00a7a67d 21h",
    "POWER": "\u00a7fPower: \u00a7dHurtful",
    "TUNING": "\u00a7fTuning: \u00a7d1,263",
    "ELECTIONS": "\u00a7e\u00a7lElection: \u00a7b1d 20h",
    "EVENT": "\u00a7e\u00a7lEvent: \u00a7fFishing Festival",
    "SLAYER": "\u00a7fSlayer Quest \u00a75Revenant Horror V",
    "EMPTY_LINE": "\u00a78(blank line)",
    "FAIRY_SOULS": "\u00a7fFairy Souls: \u00a7b250",
    "MITH": "\u00a7fMithril: \u00a7a1.2M",
    "Farming Contest": "\u00a76Farming Contest",
    "Mining Fiesta": "\u00a7bMining Fiesta",
    "Fishing Festival": "\u00a73Fishing Festival",
    "Mythological Ritual": "\u00a7dMythological Ritual",
    "Spooky Festival": "\u00a76Spooky Festival",
    "Season of Jerry": "\u00a7cSeason of Jerry",
    "New Year Celebration": "\u00a7eNew Year Celebration",
    "Traveling Zoo": "\u00a7aTraveling Zoo",
    "Hoppity\'s Hunt": "\u00a7dHoppity\'s Hunt",
    "Better Together": "\u00a7bBetter Together",
    "Election Booth Opens": "\u00a7eElection Booth Opens",
}


def java_examples(values):
    """The example text for a draggable list, as Java source with the colours written as escapes."""
    out = []
    for value in values:
        text = EXAMPLES.get(value, value)
        text = text.replace("\u00a7", "\\u00a7")          # \u00a7 in the file -> the colour in game
        out.append('"%s"' % text.replace('"', '\\"'))
    return ", ".join(out)

SCOREBOARD = [
    # ---------------------------------------------------------------- the page, in order
    # The order of this page is the order the player's own reference screenshots show, row for row:
    # the master switch, the appearance list, the reset button, then Display Options with its
    # Alignment / Arrow / Chunked Stats sections and the display settings under it, Background
    # Options, Information Filtering, and Events Options. Everything this mod has that those pages
    # do not show is in the Extra Options section at the end.
    ("bool", "MS085", "features.MS085", "Enabled",
     "Show a custom scoreboard instead of the vanilla one. Off: the client's own sidebar is left "
     "exactly as it is, and every setting below does nothing.", False),
    ("list", "appearance", "scoreboard.appearance", "Appearance",
     "Drag text to change the appearance of the advanced scoreboard. Every element is drawn in "
     "this order, and an element the server has no line for is skipped rather than left blank. "
     "EMPTY_LINE draws a blank line, for a gap where the server has none.",
     APPEARANCE_DEFAULT, APPEARANCE_DEFAULT + ["EMPTY_LINE"]),
    ("button", "resetAppearance", "scoreboard.reset", "Reset Appearance",
     "Reset the appearance of the advanced scoreboard: the element order, the events priority and "
     "the tab list lines go back to their defaults. Nothing else is touched.", "Reset"),

    ("accordion", "display", "Display Options", ""),
    ("accordion", "alignmentOptions", "Alignment Options", "", 2),
    ("dropdown", "horizontalAlign", "scoreboard.horizontalAlign", "Horizontal Alignment",
     "Choose the horizontal alignment of the scoreboard. Set to Free Move to place it with the "
     "Free X setting below.", "Right", ["Right", "Left", "Center", "Free Move"]),
    ("dropdown", "verticalAlign", "scoreboard.verticalAlign", "Vertical Alignment",
     "Choose the vertical alignment of the scoreboard. Set to Free Move to place it with the Free "
     "Y setting below.", "Center", ["Center", "Top", "Bottom", "Free Move"]),
    ("slider", "margin", "scoreboard.margin", "Margin",
     "Space between the border of your screen and the scoreboard. It moves the panel in from the "
     "edge it is anchored to, so Bottom and Left move with it.", 0, 0, 50, 1),

    ("accordion", "chunkedStatsOptions", "Chunked Stats Options", "", 2),
    ("list", "chunkedStats", "scoreboard.chunkedStats", "Chunked Stats",
     "Select the stats you want to display chunked on the scoreboard: the ones in this list are put "
     "several to a line. An empty list draws every stat on its own line, which is how the panel "
     "draws to start with.",
     [], CHUNKED),
    ("slider", "chunkedPerLine", "scoreboard.chunkedPerLine", "Max Stats per Line",
     "The maximum amount of stats that will be displayed in one line.", 3, 1, 8, 1),

    ("bool", "currencyGain", "scoreboard.currencyGain", "Show earned/lost",
     "Show the amount you earned or lost since the last time the server sent a number.", True),
    ("bool", "customLines", "scoreboard.customLines", "Use Custom Lines",
     "Draw only the elements in the Appearance list, instead of following the lines the server "
     "sends and putting the elements in among them.", False),
    ("bool", "bitsAvailable", "scoreboard.bitsAvailable", "Show unclaimed bits",
     "Show the Bits line: the amount of available Bits that can still be claimed.", True),
    ("dropdown", "powderDisplay", "scoreboard.powderDisplay", "Powder Display",
     "Select how the powder display should be formatted.", "Current",
     ["Current", "Max", "Both"]),
    ("dropdown", "numberFormat", "scoreboard.numberFormat", "Number Format",
     "Select how numbers should be formatted.", "1,234,567", ["1,234,567", "1.234.567"]),
    ("dropdown", "numberDisplay", "scoreboard.numberDisplay", "Number Display Format",
     "Select how a number and its label are put together.", "Purse: 123",
     ["Purse: 123", "123: Purse"]),
    ("bool", "time24h", "scoreboard.time24h", "SkyBlock Time 24h Format",
     "Display the current SkyBlock time in 24 hour format rather than 12 hour.", False),
    ("bool", "exactMinutes", "scoreboard.exactMinutes", "SkyBlock Time Exact Minutes",
     "Display the exact minutes in the SkyBlock time, rather than only ten minute increments.",
     False),
    ("bool", "dateInServer", "scoreboard.dateInServer", "Date in Lobby Code",
     "Show the current date in front of the server name, the way the server writes it.", True),
    ("dropdown", "dateFormat", "scoreboard.dateFormat", "Lobby Code Date Format",
     "Select your preferred date format.", "09/24/2026",
     ["09/24/2026", "09/24/26", "24/09/2026", "24.09.2026"]),
    ("slider", "lineSpacing", "scoreboard.lineSpacing", "Line Spacing",
     "The amount of space between each line.", 10, 9, 20, 1),
    ("slider", "maxLines", "scoreboard.maxLines", "Maximum Lines",
     "The most lines the scoreboard draws, whatever the server sends. Fifteen was the number it "
     "drew with, which is the length of the client's own sidebar - and since this panel also "
     "carries the elements it reads out of the tab list, fifteen cut the election block away after "
     "the second candidate, and the mayor with it. Raise it until every line is on the screen.",
     30, 5, 60, 1),

    ("accordion", "backgroundOptions", "Background Options", ""),
    ("bool", "backgroundEnabled", "scoreboard.backgroundEnabled", "Enabled",
     "Show a background behind the scoreboard.", True),
    ("text", "background", "scoreboard.background", "Background Color",
     "The background's colour as six hex digits, e.g. 000000 for black. The Opacity slider below "
     "sets how dark it is.", "000000"),
    ("slider", "opacity", "scoreboard.opacity", "Background Opacity",
     "How dark the panel behind the scoreboard is, from 0 for invisible to 255 for solid.",
     80, 0, 255, 5),
    ("slider", "borderSize", "scoreboard.borderSize", "Background Border Size",
     "The size of the border around the background, which is the padding inside the box.",
     5, 0, 20, 1),
    ("slider", "round", "scoreboard.round", "Rounded Corner Smoothness",
     "How round the corners of the background are. 0 gives square corners.", 10, 0, 30, 1),
    ("accordion", "outlineOptions", "Background Outline", "", 2),
    ("bool", "outline", "scoreboard.outline", "Outline",
     "Show an outline around the scoreboard.", False),
    ("slider", "outlineThickness", "scoreboard.outlineThickness", "Thickness",
     "Thickness of the outline.", 5, 1, 15, 1),
    ("text", "outlineTop", "scoreboard.outlineTop", "Color Top",
     "Color of the top of the outline.", "AF59FF"),
    ("text", "outlineBottom", "scoreboard.outlineBottom", "Color Bottom",
     "Color of the bottom of the outline; the two are blended down the side.", "7FEDFF"),

    ("accordion", "filtering", "Information Filtering", ""),
    ("bool", "activeOnly", "scoreboard.activeOnly", "Hide lines with no info",
     "Hide lines that have no info to display, like hiding the party when not being in one.",
     True),
    ("bool", "hideConsecutiveEmpty", "scoreboard.hideConsecutiveEmpty",
     "Hide consecutive empty lines",
     "Hide lines that are empty and have an empty line above them.", True),
    ("bool", "hideEmptyEdges", "scoreboard.hideEmptyEdges", "Hide empty lines at top/bottom",
     "Hide empty lines at the top or bottom of the scoreboard.", True),
    ("bool", "unknownLines", "scoreboard.unknownLines", "Unknown Lines warning",
     "Give a chat warning when unknown lines are found in the scoreboard, and name them. A line "
     "this mod cannot place is drawn where the server put it, never dropped.", True),

    ("accordion", "eventsOptions", "Events Options", ""),
    ("list", "eventsPriority", "scoreboard.eventsPriority", "Events Priority",
     "Drag your list to select the priority of each event. An event that is not on the list is "
     "still shown, after the ones that are.", EVENTS_PRIORITY, EVENTS_PRIORITY),
    ("bool", "allEvents", "scoreboard.allEvents", "Show all active events",
     "Show all active events in the scoreboard instead of the one with the highest priority.",
     False),
    ("bool", "eventPrefix", "scoreboard.eventPrefix", "Show Event Prefix",
     "Whether to show the 'Event: ' prefix before an event name.", True),
    ("bool", "betterTogetherTitle", "scoreboard.betterTogetherTitle", "Add Better Together Title",
     "Add a title above the Better Together event, which the server does not send one for.", False),

    ("accordion", "mayorOptions", "Mayor Options", ""),
    ("dropdown", "mayorPerks", "scoreboard.mayorPerks", "Show Mayor Perks",
     "Which of the mayor's perks to show, when the server sends a perk block.", "All Perks",
     ["All Perks", "Only Relevant"]),
    ("bool", "mayorTime", "scoreboard.mayorTime", "Show Time till Next Mayor",
     "Show the time till the next mayor election on the scoreboard.", False),

    ("accordion", "maxwellOptions", "Maxwell Options", ""),
    ("bool", "accessoryPower", "scoreboard.accessoryPower", "Show Magical Power",
     "Show your accessory power in brackets beside the selected power, e.g. Power: Hurtful (1,263). "
     "The number is learned from the same menu the power is, and the server never sends it.", True),
    ("bool", "powerFromBag", "scoreboard.powerFromBag", "Power From Accessory Bag",
     "Draw the Power line when the server sends none, from the power SweatsEnhancements has learned: the tooltip "
     "on the selected power's own item in the accessory bag menu, or the chat notice when you "
     "change your power. With nothing learned, no line is drawn. /sweatsenhancements power shows what it knows.",
     True),

    ("accordion", "slayerOptions", "Slayer Options", ""),
    ("bool", "slayerMeter", "scoreboard.slayerMeter", "Slayer Progress Meter",
     "Add a bar and a percentage on a line of its own under the quest's progress line, e.g. a bar "
     "of ten cells and 34% under (2,070/6,000) Combat.", False),
    ("bool", "slayerLevel", "scoreboard.slayerLevel", "Slayer Level",
     "Keep the level the server puts in front of the boss' name, e.g. the V of Revenant Horror V.",
     True),
    ("bool", "hideInactiveCookie", "scoreboard.hideInactiveCookie", "Hide Inactive Cookie Buff",
     "Hide the cookie buff line while no booster cookie is running. With one running, the line the "
     "server sends is kept as it is.", True),

    ("accordion", "titleFooterOptions", "Title and Footer Options", ""),
    ("dropdown", "titleAlign", "scoreboard.titleAlign", "Title Alignment",
     "Align the title in the scoreboard.", "CENTER", ["CENTER", "LEFT", "RIGHT"]),
    ("text", "title", "scoreboard.title", "Custom Title",
     "What is drawn as the title of the scoreboard. Supports & colour codes and the markup below: "
     "<bold> <strikethrough> <italic> <underlined> <obfuscated>, <green> and the other vanilla "
     "colours, <color:#f38ba8>, and <gradient:#f38ba8:#a6e3a1>text</gradient>.", ""),
    ("bool", "titleCustom", "scoreboard.titleCustom", "Use Custom Title",
     "Use a custom title instead of the default one.", False),
    ("bool", "titleAnywhere", "scoreboard.titleAnywhere", "Use Custom Title Outside SkyBlock",
     "Use the custom title outside of SkyBlock as well.", False),
    ("dropdown", "footerAlign", "scoreboard.footerAlign", "Footer Alignment",
     "Align the footer in the scoreboard.", "CENTER", ["CENTER", "LEFT", "RIGHT"]),
    ("text", "footerText", "scoreboard.footerText", "Custom Footer",
     "What is drawn as the footer of the scoreboard. The same markup as the title works here.", ""),
    ("bool", "footerCustom", "scoreboard.footerCustom", "Use Custom Footer",
     "Use the custom footer above instead of the server's own or the clock.", False),

    ("accordion", "extra", "Extra Options", ""),
    ("bool", "dungeonSidebar", "scoreboard.dungeonSidebar", "Dungeon Sidebar Rows",
     "Show dungeon keys, elapsed time, cleared percent and teammates from the server's own sidebar. "
     "Does nothing outside a dungeon.", True),
    ("list", "tabLines", "scoreboard.tabLines", "Select Tablist Lines",
     "Select the lines of the tab list that are drawn as elements of this scoreboard, in this "
     "order. A line the tab list does not carry is skipped rather than left blank. An empty list "
     "draws none of them.", TAB_LINES_DEFAULT, TAB_LINES),
    ("bool", "hideWhileTab", "scoreboard.hideWhileTab", "Hide While Tablist Open",
     "Hide the scoreboard while the tab list is open.", False),
    ("bool", "hideWhileChat", "scoreboard.hideWhileChat", "Hide While Chat Open",
     "Hide the scoreboard while the chat is open.", False),
    ("dropdown", "defaultAlign", "scoreboard.defaultAlign", "Text Alignment",
     "Align the text inside the scoreboard, without overriding the title and the footer, which "
     "have their own alignments.", "START", ["START", "CENTER", "END"]),
    ("bool", "profileName", "scoreboard.profileName", "Show Profile Name",
     "Show your profile name on the scoreboard.", True),
    ("bool", "textShadow", "scoreboard.textShadow", "Text Shadow",
     "Draw the scoreboard's text with a drop shadow.", True),
    ("bool", "showNumbers", "scoreboard.showNumbers", "Score Numbers",
     "Show the score the server gave each line, down the right hand side. Some servers put the "
     "value there instead of in the line itself.", False),
    ("bool", "useServerTitle", "scoreboard.useServerTitle", "Use The Server's Title",
     "Use the title the server sends for the sidebar instead of a built-in one.", True),
    ("text", "hideLines", "scoreboard.hideLines", "Hide Lines",
     "Comma separated phrases. A sidebar line containing one of them is not drawn. The filter runs "
     "before the elements are claimed, so a hidden line cannot hold a place in the arrangement.",
     ""),
    ("bool", "condenseSeparators", "scoreboard.condenseSeparators",
     "Condense Consecutive Separator",
     "Put the server's own blank lines together instead of keeping the gaps where it wrote them.",
     False),
    ("bool", "showPowderLine", "scoreboard.showPowderLine", "Show The Server's Powder Line",
     "Draw the server's powder line as it is, rather than the powder element's own formatting.",
     True),
    ("percent", "scale", "scoreboard.scale", "Scale",
     "Size of the scoreboard, where 1.0 is the size the client uses.", 1.0, 0.5, 2.0, 0.05),
    ("slider", "freeX", "scoreboard.freeX", "Free X",
     "How far to move the scoreboard sideways from the middle of the screen, used when Horizontal "
     "Alignment is Free Move.", 0, -300, 300, 5),
    ("slider", "freeY", "scoreboard.freeY", "Free Y",
     "How far to move the scoreboard up or down from the middle of the screen, used when Vertical "
     "Alignment is Free Move.", 0, -300, 300, 5),
    ("bool", "footer", "scoreboard.footer", "Show Clock And Ticks",
     "Show the real time and the server's tick rate under the scoreboard when no custom footer is "
     "set.", True),
    ("bool", "includeMinutes", "scoreboard.includeMinutes", "Include Minutes",
     "Whether to show minutes in timers when days and hours are available.", True),
    ("bool", "coloredMonth", "scoreboard.coloredMonth", "Colored Month",
     "Show the month in the colour of its season.", True),
    ("bool", "numberOverride", "scoreboard.numberOverride", "Number Format Override",
     "Always group the digits the way the Number Format says, whatever the machine's own locale "
     "would have done.", True),
    ("bool", "bankCompact", "scoreboard.bankCompact", "Bank Always Compact",
     "Makes the bank ignore the Number Format option and always show a shortened number, like "
     "137.6M.", True),
    ("dropdown", "coopBank", "scoreboard.coopBank", "Co-op Bank Layout",
     "When the server sends a personal and a co-op balance, which of the two to show.",
     "Personal", ["Personal", "Co-op", "Combined"]),
    ("bool", "piggyBank", "scoreboard.piggyBank", "Piggy Bank",
     "Use Piggy instead of Purse as the label when a piggy bank is active.", False),
    ("bool", "hidePurseDungeons", "scoreboard.hidePurseDungeons",
     "Hide Purse and Bank in Dungeons",
     "Hide the purse and the bank while a dungeon area is detected in the tab list.", False),
]
TAB = [
    ("bool", "MS063", "features.MS063", "Compact Tab List",
     "The panel itself. On: SweatsEnhancements draws the tab list, with the padding and the advert rows dropped. "
     "Off: the client's own list is left exactly as it is, and every setting below does nothing.",
     False),
    ("accordion", "display", "Display Options", ""),
    ("bool", "toggle", "tab.toggle", "Toggle Tab",
     "Press TAB to open the list and press it again to close it, instead of holding it down. "
     "Easier than holding a key on a phone.", False),
    ("slider", "maxLines", "tab.maxLines", "Lines Per Column",
     "How many lines a column holds before the next column starts. The client's own list holds 22.",
     22, 5, 40, 1),
    ("bool", "hideBackground", "tab.hideBackground", "Hide Tab Background",
     "Draw no panel behind the columns, so the list sits straight on the world.", False),

    ("accordion", "filter", "Information Filtering", ""),
    ("bool", "hideAdverts", "tab.hideAdverts", "Hide Server Adverts",
     "Hide the text advertising the server or its store. This server puts that advert in the "
     "footer of the list rather than in a row, so both the rows and the footer are checked.", False),
    ("bool", "hideFireSale", "tab.hideFireSale", "Hide Fire Sale Adverts",
     "Hide fire sale announcements from the tab list.", False),
    ("text", "hideLines", "tab.hideLines", "Hide Lines",
     "Comma separated phrases. A tab list row containing one of them is not drawn. The rows the "
     "server pads the list with, and the empty rows that separate its sections, are never drawn.",
     "discord, store, www., .gg, fakepixel.shop"),

    ("accordion", "layout", "Layout & Scaling", ""),
    ("percent", "scale", "tab.scale", "Scale",
     "Size of the tab list, where 1.0 is the size the client uses.", 1.0, 0.5, 2.0, 0.05),
    ("bool", "showHeader", "tab.showHeader", "Header",
     "Keep the text above the tab list.", True),
    ("bool", "showFooter", "tab.showFooter", "Footer",
     "Keep the extra lines the server puts under the list. The active effects and the cookie buff "
     "are the footer's own data on this server, so they are kept either way.", True),
]

ACCORDION_HELP = {
    "Display Options": "Visibility and appearance of the panel and its lines.",
    "Alignment Options": "Position and align the panel on screen.",
    "Chunked Stats Options": "Format large numbers and balances on the sidebar.",
    "Background Options": "Configure panel background and opacity.",
    "Background Outline": "Configure the panel border separately from its background.",
    "Information Filtering": "Choose which server lines and adverts to display.",
    "Events Options": "Arrange event details when the server provides them.",
    "Mayor Options": "Format the mayor and election information when observed.",
    "Maxwell Options": "Display observed accessory power and tuning data.",
    "Slayer Options": "Format the Slayer information from the server sidebar.",
    "Title and Footer Options": "Configure the panel title, clock and footer.",
    "Extra Options": "Additional panel behaviour and formatting controls.",
    "Layout & Scaling": "Adjust the size and placement of the tab list.",
}

SETTINGS = [("Scoreboard", SCOREBOARD), ("Tab List", TAB)]


def esc(s):
    return s.replace("\\", "\\\\").replace('"', '\\"')


def const_name(field):
    """APPEARANCE_NAMES for the field `appearance`: the offered names, as a Java constant."""
    return re.sub(r"(?<!^)(?=[A-Z])", "_", field).upper() + "_NAMES"


def java_list(values):
    return "TextList.names(%s)" % ", ".join('"%s"' % esc(v) for v in values)


def java_array(values):
    return ", ".join('"%s"' % esc(v) for v in values)


def class_name(holder):
    """The generated class that holds a group of options."""
    return {"scoreboard": "Scoreboard", "tablist": "TabList"}.get(holder, holder.capitalize())


def option_lines(options, holder, load):
    """The config read or write lines for the settings above, emitted into the generated class."""
    out = []
    for opt in options:
        kind, field = opt[0], opt[1]
        if kind == "accordion":
            continue
        key, default = opt[2], opt[5]
        target = "%s.%s" % (holder, field)
        if kind == "button":
            continue
        if kind == "bool":
            if load:
                out.append('        %s = cfg.getBool("%s", %s);' % (target, key, "true" if default else "false"))
            else:
                out.append('        cfg.setBool("%s", %s);' % (key, target))
        elif kind == "text":
            if load:
                out.append('        %s = cfg.get("%s", "%s");' % (target, key, esc(default)))
            else:
                out.append('        cfg.set("%s", %s);' % (key, target))
        elif kind == "percent":
            if load:
                out.append('        %s = cfg.getInt("%s", %d) / 100.0F;'
                           % (target, key, int(round(default * 100))))
            else:
                out.append('        cfg.setInt("%s", (int) Math.round(%s * 100));' % (key, target))
        elif kind == "slider":
            if load:
                out.append('        %s = cfg.getInt("%s", %d);' % (target, key, default))
            else:
                out.append('        cfg.setInt("%s", (int) %s);' % (key, target))
        elif kind == "dropdown":
            if load:
                out.append('        %s = cfg.get("%s", "%s");' % (target, key, esc(default)))
            else:
                out.append('        cfg.set("%s", %s);' % (key, target))
        elif kind == "list":
            # The list itself is a list of positions into the names it offers, because that is the
            # only shape the config screen's draggable list can label: the library looks each entry
            # up in the list it was given, and an entry that is a name finds nothing and is drawn
            # as "<unknown NAME>". So the names live in a constant and the positions are what is
            # dragged; the config file still stores the names, so nothing on disk changes.
            #
            # The constant is a static field, and the library files an option by the field the
            # @ConfigOption sits on - a name written above the constant lands on the constant, and
            # a name is all the row gets: no editor, so no list to drag, and the row is drawn thin
            # and empty. That is how all three lists came to be missing from the page at once.
            const = "%s.%s" % (class_name(holder), const_name(field))
            if load:
                out.append('        %s = TextList.loadIndicesMerged(cfg.get("%s", ""), %s, %s);'
                           % (target, key, const, java_list(default)))
            else:
                out.append('        cfg.set("%s", TextList.joinIndices(%s, %s));'
                           % (key, target, const))
        else:
            sys.exit("unknown setting kind: %s" % kind)
    return out


def split_name(name):
    """
    Turns a SkyHanni feature line into (short name, description).

    A line that already fits becomes the name with nothing beside it. A longer one keeps its
    opening phrase as the name and the rest as the description - preferring the point where a
    parenthetical begins, since that is where these lines stop stating what they do and start
    adding detail.
    """
    text = " ".join(name.split()).strip().rstrip(".")
    if len(text) <= 30:
        return text, ""

    cut = text.find(" (")
    if 0 < cut <= 42:
        return text[:cut], text[cut + 2:].rstrip(")")

    limit = 28
    head = text[:limit]
    space = head.rfind(" ")
    if space > 10:
        head = head[:space]
    return head, text[len(head):].strip(" ,-.")


by_cat = {}
for fid, cat, name in features:
    by_cat.setdefault(cat, []).append((fid, name))

lines = []
w = lines.append

w("package com.sweatsenhancements.gui;")
w("")
w("import java.lang.reflect.Field;")
w("import java.util.List;")
w("")
w("import com.sweatsenhancements.SweatsEnhancements;")
w("import com.sweatsenhancements.config.Cfg;")
w("import com.sweatsenhancements.config.Feature;")
w("import com.sweatsenhancements.config.Features;")
w("import com.sweatsenhancements.core.TextList;")
w("")
w("import io.github.notenoughupdates.moulconfig.Config;")
w("import io.github.notenoughupdates.moulconfig.annotations.Category;")
w("import io.github.notenoughupdates.moulconfig.annotations.ConfigAccordionId;")
w("import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorAccordion;")
w("import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;")
w("import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;")
w("import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;")
w("import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;")
w("import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;")
w("import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind;")
w("import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;")
w("import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;")
w("import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;")
w("import io.github.notenoughupdates.moulconfig.common.text.StructuredText;")
w("import org.lwjgl.input.Keyboard;")
w("")
w("/**")
w(" * GENERATED by tools/gen_gui.py - do not edit by hand, run the tool again instead.")
w(" *")
w(" * The screen MoulConfig draws. Rows are shaped like the reference screens: a short name on the left and")
w(" * the sentence explaining it on the right, so a row can be matched to the lists in docs/ by")
w(" * its ID. Only features listed in config/Implemented.java get a working switch; everything")
w(" * else stays in the source catalog, not the settings menu: a switch that does nothing is")
w(" * worse than no switch at all.")
w(" *")
w(" * The Scoreboard and Tab List categories hold the settings for the two panels, written in the")
w(" * order and under the names the reference settings pages use, so the screen can be compared")
w(" * line by line with the thing it replaces.")
w(" *")
w(" * The General category holds SweatsEnhancements's own settings rather than feature switches; the wide layout")
w(" * lives there, off by default.")
w(" * Toggles are stored in SweatsEnhancements's own config file, not in MoulConfig's - loadFromCfg() and")
w(" * saveToCfg() copy between the two, so Cfg stays the single source of truth.")
w(" */")
w("import com.sweatsenhancements.feature.nef.exact.SlayerSettings;")
w("import com.sweatsenhancements.feature.nef.exact.SlayerSettingsStore;")
w("public class SweatsConfig extends Config {")
w("")
w("    /** runnable id MoulConfig calls whenever a switch is flipped */")
w("    private static final int SAVE = 1;")
w("    /** runnable id for the Reset Appearance button */")
w("    private static final int RESET = 2;")
w("")

w('    @Category(name = "General", desc = "SweatsEnhancements\'s own settings")')
w("    public General general = new General();")
w("")

for group, _options in SETTINGS:
    w('    @Category(name = "%s", desc = "Settings for the %s")' % (esc(group), esc(group.lower())))
    w("    public %s %s = new %s();" % (ident(group), ident(group).lower(), ident(group)))
    w("")

w('    @Category(name = "Smooth Teleport", desc = "AOTE, AOTV, AOTL and Wither Impact")')
w('    public SmoothTeleport smoothTeleport = new SmoothTeleport();')
w("")

for cat in categories:
    items = by_cat.get(cat, [])
    # some feature switches live with their settings rather than inside Misc
    live_items = [i for i in items if i[0] in live and i[0] not in MOVED]
    if not live_items:
        continue
    if cat == "Slayer":
        # Keep legacy booleans for migration only; do not display custom Slayer variants.
        w("    public Slayer slayer = new Slayer();")
        w("")
        continue
    count = len(live_items)
    w('    @Category(name = "%s", desc = "%d working %s")'
      % (esc(cat), count, "control" if count == 1 else "controls"))
    w("    public %s %s = new %s();" % (ident(cat), ident(cat).lower(), ident(cat)))
    w("")

w('    @Category(name = "Slayer", desc = "Slayer calling options")')
w('    public SlayerSettings nefSlayer = SlayerSettingsStore.get();')
w("")
for tab, field, klass in [
    ("Slayer Overlay", "slayerOverlaySettings", "SlayerOverlaySettings"),
    ("Slayer Minibosses", "minibossSettings", "MinibossSettings"),
    ("Slayer Bosses", "bossSettings", "BossSettings"),
    ("Slayer Beacon", "beaconSettings", "BeaconSettings"),
    ("Slayer Inferno", "infernoDemonlord", "InfernoDemonlord"),
    ("Slayer Attunements", "blazeAttunementSettings", "BlazeAttunementSettings"),
]:
    w('    @Category(name = "%s", desc = "Configure %s")' % (tab, tab.lower()))
    w('    public SlayerSettings.%s %s = SlayerSettingsStore.get().%s;' % (klass, field, field))
    w("")

# live categories
for cat in categories:
    items = by_cat.get(cat, [])
    # the two panels' switches are emitted inside their own settings categories instead, so the
    # switch sits at the top of the screen that configures it
    live_items = [i for i in items if i[0] in live and i[0] not in MOVED]
    rank = {fid: n for n, fid in enumerate(ORDER.get(cat, "").split())}
    live_items.sort(key=lambda item: rank.get(item[0], len(rank) + items.index(item)))
    if not live_items:
        continue
    w("    public static class %s {" % ident(cat))
    for fid, name in live_items:
        short, long_desc = SPLIT.get(fid, split_name(name))
        w('        @ConfigOption(name = "%s", desc = "%s")' % (esc(short), esc(long_desc)))
        w("        @ConfigEditorBoolean(runnableId = SAVE)")
        w("        public boolean %s = %s;" % (fid, "false" if fid in default_off else "true"))
        w("")
    if cat == "Dungeon":
        w('        @ConfigOption(name = "Dungeon Requeue Key", desc = "Manual requeue key, set here; unbound by default. /sweatsenhancements requeue also works.")')
        w("        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)")
        w("        public int requeueKey = Keyboard.KEY_NONE;")
        w("")
        w('        @ConfigOption(name = "Starred Mobs Display", desc = "Choose Box, Filled Box or Off for starred mobs.")')
        w('        @ConfigEditorDropdown(values = {"Box", "Filled Box", "Off"})')
        w('        public String starredDisplay = "Box";')
        w("")
        w('        @ConfigOption(name = "Starred Mobs ESP", desc = "Render starred boxes through walls; on by default while the feature is enabled.")')
        w('        @ConfigEditorBoolean(runnableId = SAVE)')
        w('        public boolean starredEsp = true;')
        w("")
        w('        @ConfigOption(name = "Fel Mobs Style", desc = "Choose Box, Filled Box or Off; requires Fel Mobs Display.")')
        w('        @ConfigEditorDropdown(values = {"Box", "Filled Box", "Off"})')
        w('        public String felStyle = "Box";')
        w("")
        w('        @ConfigOption(name = "Bat Mobs Style", desc = "Choose Box, Filled Box or Off; requires Bat Mobs Display.")')
        w('        @ConfigEditorDropdown(values = {"Box", "Filled Box", "Off"})')
        w('        public String batStyle = "Box";')
        w("")
        for field, title, colour in [
            ("felColour", "Fel Mob Color", "0:255:148:255:255"),
            ("batColour", "Bat Mob Color", "0:255:0:255:0"),
            ("starredColour", "Starred Mob Color", "0:255:148:255:255"),
            ("witherColour", "Withermancer Color", "0:255:164:154:255"),
            ("commanderColour", "Zombie Commander Color", "0:255:0:50:255"),
            ("masterColour", "Skeleton Master Color", "0:255:127:0:255"),
            ("stormyColour", "Stormy Color", "0:255:210:228:255")]:
            w('        @ConfigOption(name = "%s", desc = "Color of the highlighted dungeon mob.")' % title)
            w('        @ConfigEditorColour')
            w('        public String %s = "%s";' % (field, colour))
            w("")
        w('        @ConfigOption(name = "Roman Blessing Levels", desc = "Show Roman rather than numeric tiers on the blessing HUD.")')
        w("        @ConfigEditorBoolean(runnableId = SAVE)")
        w("        public boolean blessingsRoman = false;")
        w("")
    if cat == "Misc":
        keys = [("safeguardKey", "Item Safeguard Key", "Mark the item under your cursor in an inventory (or held in-game); unbound by default.", "NONE"),
                ("wardrobeKey", "Wardrobe Key", "Set the wardrobe shortcut key here; unbound by default.", "NONE"),
                ("equipmentKey", "Equipment Key", "Set the equipment shortcut key here; unbound by default.", "NONE")]
        for slot in range(1, 10):
            keys.append(("wardrobeSlotKey%d" % slot, "Wardrobe Slot %d Key" % slot,
                         "Keybind for wardrobe slot %d (inside Wardrobe only)." % slot, str(slot)))
        for field, label, dest, key in [("warpIslandKey", "Warp Island Shortcut Key", "island", "F7"),
                                         ("warpHubKey", "Warp Hub Shortcut Key", "hub", "F8"),
                                         ("warpDungeonHubKey", "Warp Dungeon Hub Shortcut Key", "dungeon hub", "F9")]:
            keys.append((field, label, "Keybind for warp %s." % dest, key))
        for field, label, description, default in keys:
            w('        @ConfigOption(name = "%s", desc = "%s")' % (label, description))
            w("        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_%s)" % default)
            w("        public int %s = Keyboard.KEY_%s;" % (field, default))
            w("")
        w('        @ConfigOption(name = "Block Outline Colour", desc = "Choose outline/fill colour and fill opacity.")')
        w("        @ConfigEditorColour")
        w('        public String blockOutlineColour = "0:150:51:187:255";')
        w("")
        w('        @ConfigOption(name = "Fill Whole Block", desc = "Tint the whole block volume instead of only outlining its selection shape.")')
        w("        @ConfigEditorBoolean(runnableId = SAVE)")
        w('        public boolean blockOutlineFill = false;')
        w("")
    if cat == "Chat":
        w('        @ConfigOption(name = "Copy Button Style", desc = "Choose a compact writing-hand icon or a visible COPY label.")')
        w('        @ConfigEditorDropdown(values = {"✍", "[COPY]"})')
        w('        public String copyButtonStyle = "✍";')
        w("")
        w('        @ConfigOption(name = "Copy Button Action", desc = "Copy to clipboard, or suggest the text in the chat input without sending it.")')
        w('        @ConfigEditorDropdown(values = {"Clipboard", "Chat Input"})')
        w('        public String copyAction = "Chat Input";')
        w("")
    w("    }")
    w("")

# Smooth-teleport control labels/slider ranges adapted from NEU 2.4.0 ItemOverlays.java
# (C) 2022-2024 NotEnoughUpdates contributors, LGPL-3.0-or-later.
# Modified for SweatsEnhancements in 2026: default-off controls, separate category, SweatsEnhancements config binding.
# The existing MS223 feature key remains the AOTE toggle.
w("    /** Teleport settings adapted from NEU 2.4.0 ItemOverlays.java (C) 2022-2024 NEU contributors;")
w("     *  modified for SweatsEnhancements in 2026, LGPL-3.0-or-later. See THIRD-PARTY-NEU-NOTICE.txt. */")
w("    public static class SmoothTeleport {")
w('        @ConfigOption(name = "Enable Smooth AOTE", desc = "Smooth confirmed AOTE, AOTV and AOTL teleports. Off by default.")')
w("        @ConfigEditorBoolean(runnableId = SAVE)")
w("        public boolean MS223 = false;")
w("")
w('        @ConfigOption(name = "Enable Smooth Hyperion", desc = "Smooth confirmed Shadow Warp teleports from Wither Impact swords. Off by default.")')
w("        @ConfigEditorBoolean(runnableId = SAVE)")
w("        public boolean hyperion = false;")
w("")
w('        @ConfigOption(name = "Smooth TP Time", desc = "Time in milliseconds for AOTE/AOTV/AOTL and Shadow Warp; 0 disables animation.")')
w("        @ConfigEditorSlider(minValue = 0F, maxValue = 500F, minStep = 25F)")
w("        public int normalMs = 125;")
w("")
w('        @ConfigOption(name = "Smooth TP Time (Etherwarp)", desc = "Separate time for sneak-Etherwarp with a merged AOTV/AOTE or conduit.")')
w("        @ConfigEditorSlider(minValue = 0F, maxValue = 500F, minStep = 25F)")
w("        public int etherwarpMs = 50;")
w("    }")
w("")

# settings classes, one per panel
for group, options in SETTINGS:
    w("    public static class %s {" % ident(group))
    accordion_id = 0
    open_accordion = False
    stack = []                 # (depth, id) of the folding sections currently open
    for opt in options:
        kind = opt[0]
        if kind == "accordion":
            if open_accordion:
                w("")
            accordion_id += 1
            field, name, desc = opt[1], opt[2], opt[3]
            # a depth of 2 means this section folds inside the section above it, which is how the
            # reference nests its Alignment / Arrow / Chunked Stats / ... sections inside Display
            # Options and its Background Outline inside Background Options
            depth = opt[4] if len(opt) > 4 else 1
            while stack and stack[-1][0] >= depth:
                stack.pop()
            parent = stack[-1][1] if stack else 0
            help_text = ACCORDION_HELP.get(name, desc)
            w('        @ConfigOption(name = "%s", desc = "%s")' % (esc(name), esc(help_text)))
            w("        @ConfigEditorAccordion(id = %d)" % accordion_id)
            if parent:
                w("        @ConfigAccordionId(id = %d)" % parent)
            w("        public boolean %s = false;" % field)
            w("            // ---- the settings under this folding section")
            stack.append((depth, accordion_id))
            open_accordion = True
            continue

        field, kind_name = opt[1], opt[0]
        name, desc, default = opt[3], opt[4], opt[5]
        current = stack[-1][1] if stack else 0

        def annotations():
            """The option's own two annotations, in the order the library reads them."""
            w('        @ConfigOption(name = "%s", desc = "%s")' % (esc(name), esc(desc)))
            if current:
                # @ConfigAccordionId is what files an option under the folding section above it.
                # The library refuses to mix that with @Accordion, which is its other mechanism.
                w("        @ConfigAccordionId(id = %d)" % current)

        # A draggable list is emitted the other way round: its list of offered names is a static
        # constant that has to be declared first, so the annotations are written below it, on the
        # list field itself. See the "list" branch.
        if kind != "list":
            annotations()
        if kind == "bool":
            w("        @ConfigEditorBoolean(runnableId = SAVE)")
            w("        public boolean %s = %s;" % (field, "true" if default else "false"))
        elif kind == "text":
            w("        @ConfigEditorText")
            w('        public String %s = "%s";' % (field, esc(default)))
        elif kind == "slider":
            w("        @ConfigEditorSlider(minValue = %sF, maxValue = %sF, minStep = %sF)"
              % (opt[6], opt[7], opt[8]))
            w("        public int %s = %s;" % (field, default))
        elif kind == "percent":
            w("        @ConfigEditorSlider(minValue = %sF, maxValue = %sF, minStep = %sF)"
              % (opt[6], opt[7], opt[8]))
            w("        public float %s = %sF;" % (field, default))
        elif kind == "dropdown":
            w("        @ConfigEditorDropdown(values = {%s})" % java_array(opt[6]))
            w('        public String %s = "%s";' % (field, esc(default)))
        elif kind == "list":
            # the names first, then the option's own annotations on the list field itself
            w("        /** the names this list offers, in the order the add menu shows them */")
            w("        public static final List<String> %s = TextList.names(%s);"
              % (const_name(field), ", ".join('"%s"' % esc(v) for v in opt[6])))
            w("")
            annotations()
            w("        @ConfigEditorDraggableList(exampleText = {%s}, requireNonEmpty = false, "
              "allowDeleting = true)" % java_examples(opt[6]))
            w("        public List<Integer> %s = TextList.indices(%s, %s);"
              % (field, const_name(field), java_list(default)))
        elif kind == "button":
            w('        @ConfigEditorButton(runnableId = RESET, buttonText = "%s")' % esc(default))
            w('        public String %s = "";' % field)
        else:
            sys.exit("unknown setting kind: %s" % kind)
        w("")
    w("    }")
    w("")

# general + roadmap
w("    /** Settings that belong to SweatsEnhancements itself rather than to any one feature. */")
w("    public static class General {")
w('        @ConfigOption(name = "Wide Layout", desc = "Spread each option across a wider panel, with the description beside the name instead of under it. Takes effect the next time this screen is opened.")')
w("        @ConfigEditorBoolean(runnableId = SAVE)")
w("        public boolean guiWide = false;")
for field, label, default in [("menuKey", "Config Menu Key", "P"),
                              ("hudKey", "HUD Editor Key", "H"),
                              ("dumpKey", "Probe Dump Key", "I")]:
    w('        @ConfigOption(name = "%s", desc = "Set this SweatsEnhancements shortcut here, not in Minecraft Controls.")' % label)
    w('        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_%s)' % default)
    w('        public int %s = Keyboard.KEY_%s;' % (field, default))
    w("")
w("    }")
w("")
w('    private static final String WIDE_KEY = "gui.wide";')
w("")
w("    @Override")
w("    public StructuredText getTitle() {")
w('        return StructuredText.of("SweatsEnhancements " + SweatsEnhancements.VERSION);')
w("    }")
w("")
w("    @Override")
w("    public boolean isValidRunnable(int runnableId) {")
w("        return runnableId == SAVE || runnableId == RESET || runnableId == 7 || runnableId == 8;")
w("    }")
w("")
w("    @Override")
w("    public void executeRunnable(int runnableId) {")
w("        if (runnableId == SAVE) saveToCfg();")
w("        if (runnableId == RESET) resetAppearance();")
w("        if (runnableId == 7 || runnableId == 8) {")
w("            saveToCfg();")
w("            SweatsEnhancements.mc.addScheduledTask(() -> SweatsEnhancements.mc.displayGuiScreen(new com.sweatsenhancements.gui.SlayerPositionEditor(runnableId == 8)));")
w("        }")
w("    }")
w("")
w("    /** Puts the scoreboard's appearance settings back to their defaults. */")
w("    public void resetAppearance() {")
w("        Scoreboard scoreboard = this.scoreboard;")
w("        if (scoreboard == null) return;")
w("        scoreboard.appearance = TextList.indices(Scoreboard.%s, %s);"
  % (const_name("appearance"), java_list(APPEARANCE_DEFAULT)))
w("        scoreboard.eventsPriority = TextList.indices(Scoreboard.%s, %s);"
  % (const_name("eventsPriority"), java_list(EVENTS_PRIORITY)))
w("        scoreboard.tabLines = TextList.indices(Scoreboard.%s, %s);"
  % (const_name("tabLines"), java_list(TAB_LINES_DEFAULT)))
w("        saveToCfg();")
w("    }")
w("")
w("    /** Pull the switches in from SweatsEnhancements's config. Called before the screen is built. */")
w("    public void loadFromCfg() {")
w("        Cfg cfg = SweatsEnhancements.cfg;")
w("        if (cfg == null) return;")
w("        general.guiWide = cfg.getBool(WIDE_KEY, false);")
for field, default in [("menuKey", "P"), ("hudKey", "H"), ("dumpKey", "I")]:
    w('        general.%s = cfg.getInt("shortcuts.%s", Keyboard.KEY_%s);' % (field, field, default))
w('        chat.copyButtonStyle = cfg.get("chat.copyButtonStyle", "✍");')
w('        chat.copyAction = cfg.get("chat.copyAction", "Chat Input");')
w('        misc.safeguardKey = cfg.getInt("shortcuts.safeguardKey", Keyboard.KEY_NONE);')
w('        dungeon.requeueKey = cfg.getInt("shortcuts.dungeonRequeueKey", Keyboard.KEY_NONE);')
w('        dungeon.starredDisplay = cfg.get("dungeonMobs.starredDisplay", "Box");')
w('        dungeon.starredEsp = cfg.getBool("dungeonMobs.starredEsp", true);')
w('        dungeon.felStyle = cfg.get("dungeonMobs.felStyle", "Box");')
w('        dungeon.batStyle = cfg.get("dungeonMobs.batStyle", "Box");')
for field, colour in [("felColour", "0:255:148:255:255"), ("batColour", "0:255:0:255:0"),
                      ("starredColour", "0:255:148:255:255"), ("witherColour", "0:255:164:154:255"),
                      ("commanderColour", "0:255:0:50:255"), ("masterColour", "0:255:127:0:255"),
                      ("stormyColour", "0:255:210:228:255")]:
    w('        dungeon.%s = cfg.get("dungeonMobs.%s", "%s");' % (field, field, colour))
w('        dungeon.blessingsRoman = cfg.getBool("dungeonBlessings.roman", false);')
w('        misc.blockOutlineColour = cfg.get("blockOutline.colour", "0:150:51:187:255");')
w('        misc.blockOutlineFill = cfg.getBool("blockOutline.fill", false);')
w('        smoothTeleport.hyperion = cfg.getBool("smoothTeleport.hyperion", false);')
w('        smoothTeleport.normalMs = cfg.getInt("smoothTeleport.normalMs", 125);')
w('        smoothTeleport.etherwarpMs = cfg.getInt("smoothTeleport.etherwarpMs", 50);')
w('        misc.wardrobeKey = cfg.getInt("shortcuts.wardrobeKey", Keyboard.KEY_NONE);')
w('        misc.equipmentKey = cfg.getInt("shortcuts.equipmentKey", Keyboard.KEY_NONE);')
for slot in range(1, 10):
    w('        misc.wardrobeSlotKey%d = cfg.getInt("shortcuts.wardrobeSlotKey%d", Keyboard.KEY_%d);' % (slot, slot, slot))
for field, default in [("warpIslandKey", "F7"), ("warpHubKey", "F8"), ("warpDungeonHubKey", "F9")]:
    w('        misc.%s = cfg.getInt("shortcuts.%s", Keyboard.KEY_%s);' % (field, field, default))
for line in option_lines(SCOREBOARD, "scoreboard", True):
    w(line)
for line in option_lines(TAB, "tablist", True):
    w(line)
w("        forEachToggle(new ToggleVisitor() {")
w("            public void visit(Field field, Object holder, Feature feature) {")
w("                try {")
w("                    boolean fallback = field.getBoolean(holder);")
w("                    field.setBoolean(holder, cfg.getBool(Cfg.key(feature.id), fallback));")
w("                } catch (Throwable t) {")
w('                    System.out.println("[SweatsEnhancements] could not read " + feature.id + ": " + t);')
w("                }")
w("            }")
w("        });")
w("    }")
w("")
w("    /** Write the switches back to SweatsEnhancements's config and save the file. */")
w("    public void saveToCfg() {")
w("        Cfg cfg = SweatsEnhancements.cfg;")
w("        if (cfg == null) return;")
w("        cfg.setBool(WIDE_KEY, general.guiWide);")
for field in ("menuKey", "hudKey", "dumpKey"):
    w('        cfg.setInt("shortcuts.%s", general.%s);' % (field, field))
w('        cfg.set("chat.copyButtonStyle", chat.copyButtonStyle);')
w('        cfg.set("chat.copyAction", chat.copyAction);')
w('        cfg.setInt("shortcuts.safeguardKey", misc.safeguardKey);')
w('        cfg.setInt("shortcuts.dungeonRequeueKey", dungeon.requeueKey);')
w('        cfg.set("dungeonMobs.starredDisplay", dungeon.starredDisplay);')
w('        cfg.setBool("dungeonMobs.starredEsp", dungeon.starredEsp);')
w('        cfg.set("dungeonMobs.felStyle", dungeon.felStyle);')
w('        cfg.set("dungeonMobs.batStyle", dungeon.batStyle);')
for field in ["felColour", "batColour", "starredColour", "witherColour", "commanderColour", "masterColour", "stormyColour"]:
    w('        cfg.set("dungeonMobs.%s", dungeon.%s);' % (field, field))
w('        cfg.setBool("dungeonBlessings.roman", dungeon.blessingsRoman);')
w('        cfg.set("blockOutline.colour", misc.blockOutlineColour);')
w('        cfg.setBool("blockOutline.fill", misc.blockOutlineFill);')
w('        cfg.setBool("smoothTeleport.hyperion", smoothTeleport.hyperion);')
w('        cfg.setInt("smoothTeleport.normalMs", smoothTeleport.normalMs);')
w('        cfg.setInt("smoothTeleport.etherwarpMs", smoothTeleport.etherwarpMs);')
w('        cfg.setInt("shortcuts.wardrobeKey", misc.wardrobeKey);')
w('        cfg.setInt("shortcuts.equipmentKey", misc.equipmentKey);')
for slot in range(1, 10):
    w('        cfg.setInt("shortcuts.wardrobeSlotKey%d", misc.wardrobeSlotKey%d);' % (slot, slot))
for field in ("warpIslandKey", "warpHubKey", "warpDungeonHubKey"):
    w('        cfg.setInt("shortcuts.%s", misc.%s);' % (field, field))
for line in option_lines(SCOREBOARD, "scoreboard", False):
    w(line)
for line in option_lines(TAB, "tablist", False):
    w(line)
w("        forEachToggle(new ToggleVisitor() {")
w("            public void visit(Field field, Object holder, Feature feature) {")
w("                try {")
w("                    cfg.setBool(Cfg.key(feature.id), field.getBoolean(holder));")
w("                } catch (Throwable t) {")
w('                    System.out.println("[SweatsEnhancements] could not write " + feature.id + ": " + t);')
w("                }")
w("            }")
w("        });")
w("        SlayerSettingsStore.save();")
w("        cfg.save();")
w("    }")
w("")
w("    private interface ToggleVisitor {")
w("        void visit(Field field, Object holder, Feature feature);")
w("    }")
w("")
w("    /** Walks the category fields, handing every switch that has a matching feature to the visitor. */")
w("    private void forEachToggle(ToggleVisitor visitor) {")
w("        try {")
w("            for (Field category : getClass().getFields()) {")
w("                Object holder = category.get(this);")
w("                if (holder == null) continue;")
w("                for (Field option : holder.getClass().getFields()) {")
w("                    if (option.getType() != boolean.class) continue;")
w("                    Feature feature = Features.byId(option.getName());")
w("                    if (feature == null) continue;")
w("                    visitor.visit(option, holder, feature);")
w("                }")
w("            }")
w("        } catch (Throwable t) {")
w('            System.out.println("[SweatsEnhancements] config sync failed: " + t);')
w("        }")
w("    }")
w("}")

# Keep each auxiliary control next to the switch it customizes. This changes
# presentation only; toggle IDs, field names, defaults and persistence are untouched.
FOLLOW = {
    "Chat": {"CH200": ["copyButtonStyle", "copyAction"]},
    "Misc": {
        "MS222": ["safeguardKey"], "MS208": ["wardrobeKey"],
        "MS209": ["equipmentKey"],
        "MS210": ["wardrobeSlotKey%d" % n for n in range(1, 10)],
        "MS211": ["warpIslandKey", "warpHubKey", "warpDungeonHubKey"],
        "MS225": ["blockOutlineColour", "blockOutlineFill"],
    },
    "Dungeon": {
        "DU002": ["requeueKey"],
        "DU006": ["starredDisplay", "starredEsp", "starredColour",
                  "witherColour", "commanderColour", "masterColour", "stormyColour"],
        "DU012": ["felStyle", "felColour"],
        "DU013": ["batStyle", "batColour"],
        "DU005": ["blessingsRoman"],
    },
}

def arrange_controls(text, category):
    opening = "    public static class %s {\n" % category
    start = text.index(opening) + len(opening)
    end = text.index("    }\n", start)
    body = text[start:end]
    pattern = re.compile(r'        @ConfigOption\([^\n]*\)\n(?:        @[^\n]+\n)+        public (?:boolean|int|String) ([A-Za-z0-9_]+) = [^\n]+;\n\n')
    entries = {m.group(1):m.group(0) for m in pattern.finditer(body)}
    if len(entries) != len(list(pattern.finditer(body))) or body.strip() != "".join(entries.values()).strip():
        raise ValueError("Unexpected controls or duplicate fields in " + category)
    target = []
    for fid in ORDER.get(category, "").split():
        if fid not in entries:
            raise ValueError("Missing toggle " + fid + " in " + category)
        target.append(fid)
        target.extend(FOLLOW.get(category, {}).get(fid, []))
    if set(target) != set(entries) or len(target) != len(entries):
        raise ValueError("Incomplete or duplicate UI control mapping in " + category)
    return text[:start] + "\n" + "".join(entries[field] for field in target) + text[end:]

def arrange_categories(text):
    start = text.index('    @Category(name = "General"')
    end = text.index('    public static class Chat {', start)
    header = text[start:end]
    matcher = re.compile(r'    @Category\(name = "([^"]+)", desc = "[^\n]*"\)\n    public [^\n]+;\n\n')
    entries = {m.group(1): m.group(0) for m in matcher.finditer(header)}
    expected = "General Dungeon Slayer Slayer Overlay Slayer Minibosses Slayer Bosses Slayer Beacon Slayer Inferno Slayer Attunements Misc Smooth Teleport Chat Fishing Scoreboard Tab List".split(" | ") if False else [
        "General", "Dungeon", "Slayer", "Slayer Overlay", "Slayer Minibosses",
        "Slayer Bosses", "Slayer Beacon", "Slayer Inferno", "Slayer Attunements",
        "Misc", "Smooth Teleport", "Chat", "Fishing", "Scoreboard", "Tab List"]
    if len(entries) != len(expected) or set(entries) != set(expected):
        raise ValueError("Category list changed; revisit visual order")
    rest = matcher.sub('', header).strip()
    if rest != "public Slayer slayer = new Slayer();":
        raise ValueError("Unexpected non-category declaration: " + rest)
    return text[:start] + "".join(entries[name] for name in expected) + "    " + rest + "\n\n" + text[end:]

output = "\n".join(lines) + "\n"
for group in ("Chat", "Misc", "Dungeon"):
    output = arrange_controls(output, group)
output = arrange_categories(output)
open(OUT, "w", encoding="utf-8").write(output)

live_cats = [c for c in categories if any(f[0] in live for f in by_cat.get(c, []))]
print("wrote %s" % OUT)
print("  categories with switches : %s" % ", ".join(live_cats))
print("  switches                 : %d  (%d of them start off)" % (len(live), len(default_off)))
print("  scoreboard settings      : %d" % len([o for o in SCOREBOARD if o[0] != "accordion"]))
print("  tab list settings        : %d" % len([o for o in TAB if o[0] != "accordion"]))
print("  planned catalog (not GUI): %d" % (len(features) - len(live)))
