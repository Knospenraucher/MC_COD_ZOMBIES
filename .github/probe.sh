#!/bin/bash
# Temporär: gibt API-Signaturen der verwendeten Client-Klassen (26.3) aus.
./gradlew compileJava -q >/dev/null 2>&1 || true
ALL=$(find ~/.gradle/caches .gradle -name '*.jar' 2>/dev/null | grep -v sources)
MCJARS=$(for j in $ALL; do case "$j" in *26.3*) unzip -l "$j" 2>/dev/null | grep -q 'net/minecraft/client/Minecraft.class' && echo "$j";; esac; done)
echo "CLIENTJARS=$MCJARS"
MC=$(echo "$MCJARS" | head -1)
FAPI=$(echo "$ALL" | grep -E '0\.161\.0\+26\.3|26\.3' | grep fabric | tr '\n' ':')
CP="$MC:$(echo "$ALL" | grep -E '26\.3' | tr '\n' ':')"
echo "== gui classes"; unzip -l "$MC" | awk '{print $4}' | grep -E '^net/minecraft/client/gui/[A-Za-z]+\.class$'
echo "== graphics-like"; unzip -l "$MC" | awk '{print $4}' | grep -iE 'Graphics|GuiRender|Extractor' | grep -v '\$' | head -40
echo "== fabric hud jars"; echo "$ALL" | grep -i 'rendering' | grep 26.3
p(){ echo "== $1 | $2"; javap -cp "$CP" "$1" 2>&1 | grep -E "$2" | head -40; }
p net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement 'render|void'
p net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry 'add'
p net.minecraft.client.Minecraft 'Font font|options|player;|getInstance'
p net.minecraft.client.gui.Font 'width\(|lineHeight'
p net.minecraft.world.entity.EntityTypes ' ZOMBIE;'
