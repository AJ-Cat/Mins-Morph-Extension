/*
 * Min's Morph Extension
 * A GPLv3 derivative of Min's Morph by min01 (https://github.com/min2222/Mins-Morph).
 * Modifications Copyright (C) 2026 AJ-Cat.
 * Licensed under GPL-3.0-only. See LICENSE and NOTICE.
 */
package com.ajcat.minsmorph.gui;

import com.ajcat.minsmorph.network.MorphNetwork;
import com.ajcat.minsmorph.network.SelectGoalPacket;
import com.ajcat.minsmorph.util.MorphClientState;
import com.ajcat.minsmorph.util.MorphClientUtil;
import com.ajcat.minsmorph.util.MorphSelectionType;
import com.ajcat.minsmorph.util.MorphUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class GoalMenuScreen extends Screen {
    public static final GoalMenuScreen INSTANCE = new GoalMenuScreen();
    public static boolean active = false;

    public GoalMenuScreen() {
        super(Component.translatable("minsmorph.goalmenu.title"));
    }

    public static void activate() {
        if (Minecraft.getInstance().screen == null && MorphUtil.hasMorph(MorphClientUtil.MC.player)) {
            active = true;
            Minecraft.getInstance().setScreen(INSTANCE);
        }
    }

    public static void deactivate() {
        active = false;
        if (Minecraft.getInstance().screen == INSTANCE) {
            Minecraft.getInstance().setScreen(null);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (active && this.getMinecraft().player != null) {
            MorphClientState.Snapshot snapshot = MorphClientState.getSnapshot(this.getMinecraft().player.getUUID());
            if(snapshot == null) {
            	this.getMinecraft().player.displayClientMessage(Component.translatable("minsmorph.sync.waiting"), true);
            	return true;
            }
            double mouseAngle = MorphClientUtil.getMouseAngle();
            int goalSize = snapshot.getGoals().size();
            int animationSize = snapshot.getAnimations().size();
            int totalSize = goalSize + animationSize;
            if(totalSize <= 0) {
            	return true;
            }
            float angle = 360 / totalSize;
            mouseAngle -= angle / 2;
            mouseAngle = 360 - mouseAngle;
            mouseAngle = MorphClientUtil.correctAngle(mouseAngle);

            if (!this.getMinecraft().options.hideGui) {
                for (int i = 0; i < totalSize; i++) {
                    double currAngle = angle * i;
                    double nextAngle = currAngle + angle;
                    currAngle = MorphClientUtil.correctAngle(currAngle);
                    nextAngle = MorphClientUtil.correctAngle(nextAngle);

                    boolean mouseIn = mouseAngle > currAngle && mouseAngle < nextAngle;

                    if (mouseIn) {
                        if (button == 0) {
                        	int index = i - goalSize;
                        	int selectionId = -1;
                        	MorphSelectionType selectionType = MorphSelectionType.GOAL;
                        	if(i < goalSize) {
                        		selectionId = i;
                        		selectionType = MorphSelectionType.GOAL;
                        	}
                        	else if(index < animationSize && index >= 0) {
                        		selectionId = index;
                        		selectionType = MorphSelectionType.ANIMATION;
                    		}
                        	if(selectionId < 0) {
                        		deactivate();
                        		return true;
                        	}
                        	MorphNetwork.sendToServer(new SelectGoalPacket(this.getMinecraft().player.getUUID(), selectionType, selectionId, snapshot.getRevision()));
                        	this.getMinecraft().player.playSound(SoundEvents.UI_BUTTON_CLICK.get());
                            deactivate();
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        active = false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}