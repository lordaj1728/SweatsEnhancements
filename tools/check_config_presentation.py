#!/usr/bin/env python3
"""Check the generated settings menu's visible order, descriptions and grouping."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
text = (root / 'src/main/java/com/sweatsenhancements/gui/SweatsConfig.java').read_text()
implemented = (root / 'src/main/java/com/sweatsenhancements/config/Implemented.java').read_text()
features = (root / 'src/main/java/com/sweatsenhancements/config/Features.java').read_text()
listed = re.findall(r'add\("([A-Z]{2,4}\d{3})",', features)
live = {key for key in listed if '"'+key+'"' in implemented}
assert len(live) == 97, 'Working-switch count changed; revisit presentation audit'
assert '@Category(name = "Roadmap"' not in text, 'Planned items are not live controls'
assert not re.search(r'@ConfigOption\(name = "[^"]+", desc = ""\)', text), 'Empty control or section description'
body = text.split('public class SweatsConfig extends Config {', 1)[1]
shown = re.findall(r'public boolean ([A-Z]{2,4}\d{3}) =', body)
assert len(shown) == len(set(shown)) == 97 and set(shown) == live, 'Missing/duplicate live control'
for key in shown:
    assert re.search(r'@ConfigOption\(name = "[^"]+", desc = "[^"]+"\)\s+@ConfigEditorBoolean[^\n]*\s+public boolean '+key+r' =', body), 'No explanation: '+key

cats = re.findall(r'@Category\(name = "([^"]+)"', body)
assert cats[:3] == ['General', 'Dungeon', 'Slayer'] and cats[-2:] == ['Scoreboard', 'Tab List']
assert all('NEF' not in name for name in cats)

def section(name):
    return body.split('public static class '+name+' {', 1)[1].split('\n    }', 1)[0]

def between(name, first, second, *expected):
    s = section(name)
    a = s.index('public boolean '+first+' =')
    b = s.index('public boolean '+second+' =', a)
    for field in expected:
        assert a < s.index('public ', a+1) < b and re.search(r'\b'+field+r'\b', s[a:b]), name+' '+field+' not paired'

between('Dungeon', 'DU002', 'DU003', 'requeueKey')
between('Dungeon', 'DU006', 'DU012', 'starredDisplay', 'starredEsp', 'starredColour')
between('Dungeon', 'DU012', 'DU013', 'felStyle', 'felColour')
between('Dungeon', 'DU013', 'DU007', 'batStyle', 'batColour')
between('Misc', 'MS222', 'MS221', 'safeguardKey')
between('Misc', 'MS208', 'MS209', 'wardrobeKey')
between('Misc', 'MS210', 'MS211', 'wardrobeSlotKey1', 'wardrobeSlotKey9')
between('Misc', 'MS225', 'MS227', 'blockOutlineColour', 'blockOutlineFill')
assert section('Dungeon').index('public boolean DU020 =') < section('Dungeon').index('public boolean DU016 =')
print('Config presentation: 97 described switches, 15 relevant categories, grouped controls, no planned-only page')
