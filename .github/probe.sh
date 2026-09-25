#!/bin/bash
# Temporär: gibt API-Signaturen der verwendeten Client-Klassen (26.3) aus.
./gradlew compileJava -q >/dev/null 2>&1 || true
ALL=$(find ~/.gradle/caches .gradle -name '*.jar' 2>/dev/null | grep -v sources)
MC=/home/runner/.gradle/caches/fabric-loom/26.3/minecraft-client.jar
REND=$(for j in $ALL; do unzip -l "$j" 2>/dev/null | grep -q 'rendering/v1/hud/HudElement.class' && echo "$j"; done)
echo "REND=$REND"
CP="$MC:$(echo "$REND" | tr '\n' ':')"
p(){ echo "== $1 | $2"; javap -cp "$CP" "$1" 2>&1 | grep -E "$2" | head -60; }
for r in $REND; do echo "== HudElement in $r"; javap -cp "$MC:$r" net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement; javap -cp "$MC:$r" net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry | grep add; done
p net.minecraft.client.gui.GuiGraphicsExtractor 'public'
