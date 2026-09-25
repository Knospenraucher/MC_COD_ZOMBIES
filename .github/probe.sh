#!/bin/bash
CP=$(./gradlew -q printCp 2>/dev/null | grep '^CP=' | sed 's/^CP=//')
p(){ echo "== $1 | $2"; javap -cp "$CP" "$1" 2>&1 | grep -E "$2" | head -40; }
p net.minecraft.world.entity.Entity 'invulnerab|Invulnerab'
p net.minecraft.world.entity.LivingEntity 'invulnerab|Invulnerab|hurtTime|lastHurt'
p net.minecraft.world.level.Level 'getEntitiesOfClass'
p net.minecraft.world.level.EntityGetter 'getEntitiesOfClass'
p net.minecraft.world.phys.AABB 'getCenter|AABB\('
p net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput 'public|accept'
p net.minecraft.world.item.CreativeModeTab\$Output 'accept'
p net.minecraft.client.gui.GuiGraphicsExtractor 'guiHeight|guiWidth'
p net.minecraft.core.particles.ParticleTypes 'ENCHANTED_HIT|EXPLOSION_EMITTER| ENCHANT;| SMOKE;| CRIT;'
for c in $(echo "$CP" | tr ':' '\n' | grep -i fabric); do unzip -l "$c" 2>/dev/null | awk '{print $4}' | grep -iE 'FabricItem\.class$'; done | sort -u
p net.fabricmc.fabric.api.item.v1.FabricItem 'public'
p net.minecraft.world.item.ItemStack 'getOrDefault|<T> T get|is\('
p net.minecraft.core.component.DataComponentHolder 'get'
