/*******************************************************************************************************
 * Continued by PikaMug (formerly HappyPikachu) with permission from _Blackvein_. All rights reserved.
 * 
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN
 * NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************************************/

package me.blackvein.quests.actions;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import me.blackvein.quests.Quest;
import me.blackvein.quests.QuestMob;
import me.blackvein.quests.Quester;
import me.blackvein.quests.Quests;
import me.blackvein.quests.convo.actions.main.ActionMainPrompt;
import me.blackvein.quests.convo.actions.menu.ActionMenuPrompt;
import me.blackvein.quests.interfaces.ReloadCallback;
import me.blackvein.quests.util.CK;
import me.blackvein.quests.util.ConfigUtil;
import me.blackvein.quests.util.ItemUtil;
import me.blackvein.quests.util.Lang;

public class ActionFactory implements ConversationAbandonedListener {

    private final Quests plugin;
    private final ConversationFactory convoCreator;
    private Map<UUID, Block> selectedExplosionLocations = new HashMap<UUID, Block>();
    private Map<UUID, Block> selectedEffectLocations = new HashMap<UUID, Block>();
    private Map<UUID, Block> selectedMobLocations = new HashMap<UUID, Block>();
    private Map<UUID, Block> selectedLightningLocations = new HashMap<UUID, Block>();
    private Map<UUID, Block> selectedTeleportLocations = new HashMap<UUID, Block>();
    private List<String> editingActionNames = new LinkedList<String>();

    public ActionFactory(final Quests plugin) {
        this.plugin = plugin;
        // Ensure to initialize convoCreator last so that 'this' is fully initialized before it is passed
        this.convoCreator = new ConversationFactory(plugin).withModality(false).withLocalEcho(false)
                .withFirstPrompt(new ActionMenuPrompt(new ConversationContext(plugin, null, null))).withTimeout(3600)
                .thatExcludesNonPlayersWithMessage("Console may not perform this operation!")
                .addConversationAbandonedListener(this);
    }
    
    public Map<UUID, Block> getSelectedExplosionLocations() {
        return selectedExplosionLocations;
    }

    public void setSelectedExplosionLocations(
            final Map<UUID, Block> selectedExplosionLocations) {
        this.selectedExplosionLocations = selectedExplosionLocations;
    }

    public Map<UUID, Block> getSelectedEffectLocations() {
        return selectedEffectLocations;
    }

    public void setSelectedEffectLocations(final Map<UUID, Block> selectedEffectLocations) {
        this.selectedEffectLocations = selectedEffectLocations;
    }

    public Map<UUID, Block> getSelectedMobLocations() {
        return selectedMobLocations;
    }

    public void setSelectedMobLocations(final Map<UUID, Block> selectedMobLocations) {
        this.selectedMobLocations = selectedMobLocations;
    }

    public Map<UUID, Block> getSelectedLightningLocations() {
        return selectedLightningLocations;
    }

    public void setSelectedLightningLocations(
            final Map<UUID, Block> selectedLightningLocations) {
        this.selectedLightningLocations = selectedLightningLocations;
    }

    public Map<UUID, Block> getSelectedTeleportLocations() {
        return selectedTeleportLocations;
    }

    public void setSelectedTeleportLocations(
            final Map<UUID, Block> selectedTeleportLocations) {
        this.selectedTeleportLocations = selectedTeleportLocations;
    }

    public ConversationFactory getConversationFactory() {
        return convoCreator;
    }
    
    public List<String> getNamesOfActionsBeingEdited() {
        return editingActionNames;
    }
    
    public void setNamesOfActionsBeingEdited(final List<String> actionNames) {
        this.editingActionNames = actionNames;
    }

    @Override
    public void conversationAbandoned(final ConversationAbandonedEvent abandonedEvent) {
        final Player player = (Player) abandonedEvent.getContext().getForWhom();
        selectedExplosionLocations.remove(player.getUniqueId());
        selectedEffectLocations.remove(player.getUniqueId());
        selectedMobLocations.remove(player.getUniqueId());
        selectedLightningLocations.remove(player.getUniqueId());
        selectedTeleportLocations.remove(player.getUniqueId());
    }
    
    public Prompt returnToMenu(final ConversationContext context) {
        return new ActionMainPrompt(context);
    }
    
    public void loadData(final Action event, final ConversationContext context) {
        if (event.message != null) {
            context.setSessionData(CK.E_MESSAGE, event.message);
        }
        if (event.clearInv == true) {
            context.setSessionData(CK.E_CLEAR_INVENTORY, Lang.get("yesWord"));
        } else {
            context.setSessionData(CK.E_CLEAR_INVENTORY, Lang.get("noWord"));
        }
        if (event.failQuest == true) {
            context.setSessionData(CK.E_FAIL_QUEST, Lang.get("yesWord"));
        } else {
            context.setSessionData(CK.E_FAIL_QUEST, Lang.get("noWord"));
        }
        if (event.items != null && event.items.isEmpty() == false) {
            final LinkedList<ItemStack> items = new LinkedList<ItemStack>();
            items.addAll(event.items);
            context.setSessionData(CK.E_ITEMS, items);
        }
        if (event.explosions != null && event.explosions.isEmpty() == false) {
            final LinkedList<String> locs = new LinkedList<String>();
            for (final Location loc : event.explosions) {
                locs.add(ConfigUtil.getLocationInfo(loc));
            }
            context.setSessionData(CK.E_EXPLOSIONS, locs);
        }
        if (event.effects != null && event.effects.isEmpty() == false) {
            final LinkedList<String> locs = new LinkedList<String>();
            final LinkedList<String> effs = new LinkedList<String>();
            for (final Entry<Location, Effect> e : event.effects.entrySet()) {
                locs.add(ConfigUtil.getLocationInfo(e.getKey()));
                effs.add(e.getValue().toString());
            }
            context.setSessionData(CK.E_EFFECTS, effs);
            context.setSessionData(CK.E_EFFECTS_LOCATIONS, locs);
        }
        if (event.stormWorld != null) {
            context.setSessionData(CK.E_WORLD_STORM, event.stormWorld.getName());
            context.setSessionData(CK.E_WORLD_STORM_DURATION, event.stormDuration);
        }
        if (event.thunderWorld != null) {
            context.setSessionData(CK.E_WORLD_THUNDER, event.thunderWorld.getName());
            context.setSessionData(CK.E_WORLD_THUNDER_DURATION, event.thunderDuration);
        }
        if (event.mobSpawns != null && event.mobSpawns.isEmpty() == false) {
            final LinkedList<String> questMobs = new LinkedList<String>();
            for (final QuestMob questMob : event.mobSpawns) {
                questMobs.add(questMob.serialize());
            }
            context.setSessionData(CK.E_MOB_TYPES, questMobs);
        }
        if (event.lightningStrikes != null && event.lightningStrikes.isEmpty() == false) {
            final LinkedList<String> locs = new LinkedList<String>();
            for (final Location loc : event.lightningStrikes) {
                locs.add(ConfigUtil.getLocationInfo(loc));
            }
            context.setSessionData(CK.E_LIGHTNING, locs);
        }
        if (event.potionEffects != null && event.potionEffects.isEmpty() == false) {
            final LinkedList<String> types = new LinkedList<String>();
            final LinkedList<Long> durations = new LinkedList<Long>();
            final LinkedList<Integer> mags = new LinkedList<Integer>();
            for (final PotionEffect pe : event.potionEffects) {
                types.add(pe.getType().getName());
                durations.add((long) pe.getDuration());
                mags.add(pe.getAmplifier());
            }
            context.setSessionData(CK.E_POTION_TYPES, types);
            context.setSessionData(CK.E_POTION_DURATIONS, durations);
            context.setSessionData(CK.E_POTION_STRENGHT, mags);
        }
        if (event.hunger > -1) {
            context.setSessionData(CK.E_HUNGER, event.hunger);
        }
        if (event.saturation > -1) {
            context.setSessionData(CK.E_SATURATION, event.saturation);
        }
        if (event.health > -1) {
            context.setSessionData(CK.E_HEALTH, event.health);
        }
        if (event.teleport != null) {
            context.setSessionData(CK.E_TELEPORT, ConfigUtil.getLocationInfo(event.teleport));
        }
        if (event.commands != null) {
            context.setSessionData(CK.E_COMMANDS, event.commands);
        }
        if (event.timer > 0) {
            context.setSessionData(CK.E_TIMER, event.timer);
        }
        if (event.cancelTimer) {
            context.setSessionData(CK.E_CANCEL_TIMER, true);
        }
    }

    public void clearData(final ConversationContext context) {
        context.setSessionData(CK.E_OLD_EVENT, null);
        context.setSessionData(CK.E_NAME, null);
        context.setSessionData(CK.E_MESSAGE, null);
        context.setSessionData(CK.E_CLEAR_INVENTORY, null);
        context.setSessionData(CK.E_FAIL_QUEST, null);
        context.setSessionData(CK.E_ITEMS, null);
        context.setSessionData(CK.E_ITEMS_AMOUNTS, null);
        context.setSessionData(CK.E_EXPLOSIONS, null);
        context.setSessionData(CK.E_EFFECTS, null);
        context.setSessionData(CK.E_EFFECTS_LOCATIONS, null);
        context.setSessionData(CK.E_WORLD_STORM, null);
        context.setSessionData(CK.E_WORLD_STORM_DURATION, null);
        context.setSessionData(CK.E_WORLD_THUNDER, null);
        context.setSessionData(CK.E_WORLD_THUNDER_DURATION, null);
        context.setSessionData(CK.E_MOB_TYPES, null);
        context.setSessionData(CK.E_LIGHTNING, null);
        context.setSessionData(CK.E_POTION_TYPES, null);
        context.setSessionData(CK.E_POTION_DURATIONS, null);
        context.setSessionData(CK.E_POTION_STRENGHT, null);
        context.setSessionData(CK.E_HUNGER, null);
        context.setSessionData(CK.E_SATURATION, null);
        context.setSessionData(CK.E_HEALTH, null);
        context.setSessionData(CK.E_TELEPORT, null);
        context.setSessionData(CK.E_COMMANDS, null);
        context.setSessionData(CK.E_TIMER, null);
        context.setSessionData(CK.E_CANCEL_TIMER, null);
    }

    public void deleteAction(final ConversationContext context) {
        final YamlConfiguration data = new YamlConfiguration();
        final File actionsFile = new File(plugin.getDataFolder(), "actions.yml");
        try {
            data.load(actionsFile);
        } catch (final IOException e) {
            e.printStackTrace();
            ((Player) context.getForWhom()).sendMessage(ChatColor.RED + Lang.get("questErrorReadingFile")
                    .replace("<file>", actionsFile.getName()));
            return;
        } catch (final InvalidConfigurationException e) {
            e.printStackTrace();
            ((Player) context.getForWhom()).sendMessage(ChatColor.RED + Lang.get("questErrorReadingFile")
                    .replace("<file>", actionsFile.getName()));
            return;
        }
        final String action = (String) context.getSessionData(CK.ED_EVENT_DELETE);
        String key = "actions";
        ConfigurationSection sec = data.getConfigurationSection(key);
        if (sec == null) {
            key = "events";
            sec = data.getConfigurationSection(key);
        }
        sec.set(action, null);
        try {
            data.save(actionsFile);
        } catch (final IOException e) {
            ((Player) context.getForWhom()).sendMessage(ChatColor.RED + Lang.get("questSaveError"));
            return;
        }
        final ReloadCallback<Boolean> callback = new ReloadCallback<Boolean>() {
            @Override
            public void execute(final Boolean response) {
                if (!response) {
                    context.getForWhom().sendRawMessage(ChatColor.RED + Lang.get("unknownError"));
                }
            }
        };
        plugin.reload(callback);
        ((Player) context.getForWhom()).sendMessage(ChatColor.YELLOW + Lang.get("eventEditorDeleted"));
        for (final Quester q : plugin.getQuesters()) {
            for (final Quest quest : q.getCurrentQuests().keySet()) {
                q.checkQuest(quest);
            }
        }
        clearData(context);
    }

    @SuppressWarnings("unchecked")
    public void saveAction(final ConversationContext context) {
        final YamlConfiguration data = new YamlConfiguration();
        final File actionsFile = new File(plugin.getDataFolder(), "actions.yml");
        try {
            data.load(actionsFile);
        } catch (final IOException e) {
            e.printStackTrace();
            ((Player) context.getForWhom()).sendMessage(ChatColor.RED + Lang.get("questErrorReadingFile")
                    .replace("<file>", actionsFile.getName()));
            return;
        } catch (final InvalidConfigurationException e) {
            e.printStackTrace();
            ((Player) context.getForWhom()).sendMessage(ChatColor.RED + Lang.get("questErrorReadingFile")
                    .replace("<file>", actionsFile.getName()));
            return;
        }
        String key = "actions";
        ConfigurationSection sec = data.getConfigurationSection(key);
        if (sec == null) {
            key = "events";
            sec = data.getConfigurationSection(key);
        }
        if (((String) context.getSessionData(CK.E_OLD_EVENT)).isEmpty() == false) {
            data.set(key + "." + (String) context.getSessionData(CK.E_OLD_EVENT), null);
            final LinkedList<Action> temp = plugin.getActions();
            temp.remove(plugin.getAction((String) context.getSessionData(CK.E_OLD_EVENT)));
            plugin.setActions(temp);
        }
        final ConfigurationSection section = data.createSection(key + "." + (String) context.getSessionData(CK.E_NAME));
        editingActionNames.remove(context.getSessionData(CK.E_NAME));
        if (context.getSessionData(CK.E_MESSAGE) != null) {
            section.set("message", context.getSessionData(CK.E_MESSAGE));
        }
        if (context.getSessionData(CK.E_CLEAR_INVENTORY) != null) {
            final String s = (String) context.getSessionData(CK.E_CLEAR_INVENTORY);
            if (s.equalsIgnoreCase(Lang.get("yesWord"))) {
                section.set("clear-inventory", true);
            }
        }
        if (context.getSessionData(CK.E_FAIL_QUEST) != null) {
            final String s = (String) context.getSessionData(CK.E_FAIL_QUEST);
            if (s.equalsIgnoreCase(Lang.get("yesWord"))) {
                section.set("fail-quest", true);
            }
        }
        if (context.getSessionData(CK.E_ITEMS) != null) {
            section.set("items", context.getSessionData(CK.E_ITEMS));
        }
        if (context.getSessionData(CK.E_EXPLOSIONS) != null) {
            section.set("explosions", context.getSessionData(CK.E_EXPLOSIONS));
        }
        if (context.getSessionData(CK.E_EFFECTS) != null) {
            section.set("effects", context.getSessionData(CK.E_EFFECTS));
            section.set("effect-locations", context.getSessionData(CK.E_EFFECTS_LOCATIONS));
        }
        if (context.getSessionData(CK.E_WORLD_STORM) != null) {
            section.set("storm-world", context.getSessionData(CK.E_WORLD_STORM));
            section.set("storm-duration", context.getSessionData(CK.E_WORLD_STORM_DURATION));
        }
        if (context.getSessionData(CK.E_WORLD_THUNDER) != null) {
            section.set("thunder-world", context.getSessionData(CK.E_WORLD_THUNDER));
            section.set("thunder-duration", context.getSessionData(CK.E_WORLD_THUNDER_DURATION));
        }
        try {
            if (context.getSessionData(CK.E_MOB_TYPES) != null) {
                int count = 0;
                for (final String s : (LinkedList<String>) context.getSessionData(CK.E_MOB_TYPES)) {
                    ConfigurationSection ss = section.getConfigurationSection("mob-spawns." + count);
                    if (ss == null) {
                        ss = section.createSection("mob-spawns." + count);
                    }
                    final QuestMob questMob = QuestMob.fromString(s);
                    if (questMob == null) {
                        continue;
                    }
                    ss.set("name", questMob.getName());
                    ss.set("spawn-location", ConfigUtil.getLocationInfo(questMob.getSpawnLocation()));
                    ss.set("mob-type", questMob.getType().name());
                    ss.set("spawn-amounts", questMob.getSpawnAmounts());
                    ss.set("held-item", ItemUtil.serializeItemStack(questMob.getInventory()[0]));
                    ss.set("held-item-drop-chance", questMob.getDropChances()[0]);
                    ss.set("boots", ItemUtil.serializeItemStack(questMob.getInventory()[1]));
                    ss.set("boots-drop-chance", questMob.getDropChances()[1]);
                    ss.set("leggings", ItemUtil.serializeItemStack(questMob.getInventory()[2]));
                    ss.set("leggings-drop-chance", questMob.getDropChances()[2]);
                    ss.set("chest-plate", ItemUtil.serializeItemStack(questMob.getInventory()[3]));
                    ss.set("chest-plate-drop-chance", questMob.getDropChances()[3]);
                    ss.set("helmet", ItemUtil.serializeItemStack(questMob.getInventory()[4]));
                    ss.set("helmet-drop-chance", questMob.getDropChances()[4]);
                    count++;
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        if (context.getSessionData(CK.E_LIGHTNING) != null) {
            section.set("lightning-strikes", context.getSessionData(CK.E_LIGHTNING));
        }
        if (context.getSessionData(CK.E_COMMANDS) != null) {
            final LinkedList<String> commands = (LinkedList<String>) context.getSessionData(CK.E_COMMANDS);
            if (commands.isEmpty() == false) {
                section.set("commands", commands);
            }
        }
        if (context.getSessionData(CK.E_POTION_TYPES) != null) {
            section.set("potion-effect-types", context.getSessionData(CK.E_POTION_TYPES));
            section.set("potion-effect-durations", context.getSessionData(CK.E_POTION_DURATIONS));
            section.set("potion-effect-amplifiers", context.getSessionData(CK.E_POTION_STRENGHT));
        }
        if (context.getSessionData(CK.E_HUNGER) != null) {
            section.set("hunger", context.getSessionData(CK.E_HUNGER));
        }
        if (context.getSessionData(CK.E_SATURATION) != null) {
            section.set("saturation", context.getSessionData(CK.E_SATURATION));
        }
        if (context.getSessionData(CK.E_HEALTH) != null) {
            section.set("health", context.getSessionData(CK.E_HEALTH));
        }
        if (context.getSessionData(CK.E_TELEPORT) != null) {
            section.set("teleport-location", context.getSessionData(CK.E_TELEPORT));
        }
        if (context.getSessionData(CK.E_TIMER) != null && (int) context.getSessionData(CK.E_TIMER) > 0) {
            section.set("timer", context.getSessionData(CK.E_TIMER));
        }
        if (context.getSessionData(CK.E_CANCEL_TIMER) != null) {
            final String s = (String) context.getSessionData(CK.E_CANCEL_TIMER);
            if (s.equalsIgnoreCase(Lang.get("yesWord"))) {
                section.set("cancel-timer", true);
            }
        }
        if (context.getSessionData(CK.E_DENIZEN) != null) {
            section.set("denizen-script", context.getSessionData(CK.E_DENIZEN));
        }
        try {
            data.save(actionsFile);
        } catch (final IOException e) {
            ((Player) context.getForWhom()).sendMessage(ChatColor.RED + Lang.get("questSaveError"));
            return;
        }
        final ReloadCallback<Boolean> callback = new ReloadCallback<Boolean>() {
            @Override
            public void execute(final Boolean response) {
                if (!response) {
                    context.getForWhom().sendRawMessage(ChatColor.RED + Lang.get("unknownError"));
                }
            }
        };
        plugin.reload(callback);
        ((Player) context.getForWhom()).sendMessage(ChatColor.YELLOW + Lang.get("eventEditorSaved"));
        for (final Quester q : plugin.getQuesters()) {
            for (final Quest quest : q.getCurrentQuests().keySet()) {
                q.checkQuest(quest);
            }
        }
        clearData(context);
    }
}