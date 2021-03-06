/*
 * This file is part of MineQuest, The ultimate MMORPG plugin!.
 * MineQuest is licensed under GNU General Public License v3.
 * Copyright (C) 2012 The MineQuest Team
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.theminequest.MineQuest.Events;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.theminequest.MineQuest.API.Managers;
import com.theminequest.MineQuest.API.Events.EventManager;
import com.theminequest.MineQuest.API.Events.QuestEvent;
import com.theminequest.MineQuest.API.Quest.Quest;

/**
 * Because we don't know what classes will be available on runtime, we need to
 * keep track of all classes that extend QuestEvent and record them here.
 * 
 * @author xu_robert <xu_robert@linux.com>
 * 
 */
public class MQEventManager implements Listener, EventManager {
	
	private LinkedHashMap<String, Class<? extends QuestEvent>> classes;
	private List<QuestEvent> activeevents;
	private Runnable activechecker;
	private volatile boolean stop;
	
	public MQEventManager() {
		Managers.log("[Event] Starting Manager...");
		classes = new LinkedHashMap<String, Class<? extends QuestEvent>>(0);
		activeevents = new CopyOnWriteArrayList<QuestEvent>();
		stop = false;
		activechecker = new Runnable() {
			
			@Override
			public void run() {
				while (!stop) {
					checkAllEvents();
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}
			
		};
		Thread t = new Thread(activechecker);
		t.setDaemon(true);
		t.setName("MineQuest-MQEventManager");
		t.start();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.theminequest.MineQuest.Events.EventManager#dismantleRunnable()
	 */
	@Override
	public void dismantleRunnable() {
		stop = true;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.theminequest.MineQuest.Events.EventManager#registerEvent(java.lang
	 * .String, java.lang.Class)
	 */
	@Override
	public void addEvent(String eventname, Class<? extends QuestEvent> event) {
		if (classes.containsKey(eventname) || classes.containsValue(event))
			throw new IllegalArgumentException("We already have this class!");
		try {
			event.getConstructor();
		} catch (Exception e) {
			throw new IllegalArgumentException("Constructor tampered with!");
		}
		classes.put(eventname, event);
	}
	
	@Override
	public QuestEvent constructEvent(String eventname, Quest q, int eventnum, String d) {
		if (!classes.containsKey(eventname))
			return null;
		Class<? extends QuestEvent> cl = classes.get(eventname);
		try {
			QuestEvent e = cl.getConstructor().newInstance();
			e.setupProperties(q, eventnum, d);
			return e;
		} catch (Exception e) {
			Managers.log(Level.SEVERE, "[Event] In retrieving event " + eventname + " from Quest ID " + q + ":");
			e.printStackTrace();
			return null;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.theminequest.MineQuest.Events.EventManager#addEventListener(com.
	 * theminequest.MineQuest.API.Events.QuestEvent)
	 */
	@Override
	public void registerEventListeners(Collection<QuestEvent> events) {
		activeevents.addAll(events);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.theminequest.MineQuest.Events.EventManager#rmEventListener(com.
	 * theminequest.MineQuest.API.Events.QuestEvent)
	 */
	@Override
	public void deregisterEventListener(QuestEvent e) {
		activeevents.remove(e);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.theminequest.MineQuest.Events.EventManager#checkAllEvents()
	 */
	@Override
	public void checkAllEvents() {
		for (QuestEvent e : activeevents)
			try {
				e.check();
			} catch (Throwable t) {
				t.printStackTrace();
			}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.theminequest.MineQuest.Events.EventManager#onPlayerInteract(org.bukkit
	 * .event.player.PlayerInteractEvent)
	 */
	@Override
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerInteract(final PlayerInteractEvent event) {
		if (event.isCancelled())
			return;
		for (QuestEvent e : activeevents)
			e.onPlayerInteract(event);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.theminequest.MineQuest.Events.EventManager#onBlockBreak(org.bukkit
	 * .event.block.BlockBreakEvent)
	 */
	@Override
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockBreak(final BlockBreakEvent event) {
		if (event.isCancelled())
			return;
		for (QuestEvent e : activeevents)
			e.onBlockBreak(event);
	}
	
	@Override
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDamageEvent(final EntityDamageEvent event) {
		if (event.isCancelled())
			return;
		for (QuestEvent e : activeevents)
			e.onEntityDamage(event);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.theminequest.MineQuest.Events.EventManager#onEntityDeathEvent(org
	 * .bukkit.event.entity.EntityDeathEvent)
	 */
	@Override
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDeathEvent(final EntityDeathEvent event) {
		for (QuestEvent e : activeevents)
			e.onEntityDeath(event);
	}
	
}
