#!/bin/bash
curl -s "https://site.api.espn.com/apis/site/v2/sports/soccer/fifa.world/scoreboard?dates=20260611" | python3 -c "
import json,sys
d = json.load(sys.stdin)
events = d.get('events', [])
print('events:', len(events))
for e in events[:3]:
    comp = e['competitions'][0]
    print(json.dumps(comp.get('venue'), indent=2))
    print('---')
"
