package com.walnutbeachdigital.joulesmith.network;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.storage.bin.BinContents;

import net.minecraft.world.item.ItemStack;

/**
 * Works out how to make {@code count} of an item from what's on the network and the patterns it knows:
 * which items to take from storage, and which patterns to run how many times, ingredients first. Items made
 * along the way that aren't used up count toward later steps.
 */
public final class CraftingPlan {
    public static final int MAX_DEPTH = 8, MAX_STEPS = 64;

    public record Step(PatternContents pattern, int batches) {}

    public final List<BinContents> take = new ArrayList<>();
    public final List<Step> steps = new ArrayList<>();
    public final List<BinContents> missing = new ArrayList<>();
    private final List<BinContents> available;
    /** Made by earlier steps and not used yet. */
    private final List<BinContents> made = new ArrayList<>();
    private final List<PatternContents> patterns;

    private CraftingPlan(List<BinContents> available, List<PatternContents> patterns) {
        this.available = new ArrayList<>(available);
        this.patterns = patterns;
    }

    /** The plan for {@code count} more of {@code item}, made rather than taken (the first batch is always crafted). */
    public static CraftingPlan make(List<BinContents> stored, List<PatternContents> patterns, ItemStack item, int count) {
        CraftingPlan plan = new CraftingPlan(stored, patterns);
        PatternContents p = plan.patternFor(item);
        if (p == null) {
            plan.missing.add(new BinContents(item, count));
            return plan;
        }
        plan.craft(p, count, 0);
        return plan;
    }

    public boolean ok() {
        return missing.isEmpty() && steps.size() <= MAX_STEPS;
    }

    private PatternContents patternFor(ItemStack item) {
        for (PatternContents p : patterns) if (ItemStack.isSameItemSameComponents(p.output(), item)) return p;
        return null;
    }

    private void craft(PatternContents p, int count, int depth) {
        int per = Math.max(1, p.output().getCount());
        int batches = (count + per - 1) / per;
        List<BinContents> needs = new ArrayList<>();
        for (ItemStack s : p.grid()) {
            if (s.isEmpty()) continue;
            int i = DriveItem.indexOf(needs, s);
            if (i < 0) needs.add(new BinContents(s, batches));
            else needs.set(i, new BinContents(s, needs.get(i).count() + batches));
        }
        for (BinContents n : needs) need(n.item(), n.count(), depth + 1);
        steps.add(new Step(p, batches));
        int extra = batches * per - count;
        if (extra > 0) add(made, p.output(), extra);
    }

    private void need(ItemStack item, int count, int depth) {
        int m = DriveItem.indexOf(made, item);
        if (m >= 0) {
            int use = Math.min(made.get(m).count(), count);
            remove(made, item, use);
            count -= use;
            if (count <= 0) return;
        }
        int i = DriveItem.indexOf(available, item);
        int have = i < 0 ? 0 : available.get(i).count();
        int used = Math.min(have, count);
        if (used > 0) {
            if (used == have) available.remove(i);
            else available.set(i, new BinContents(item, have - used));
            add(take, item, used);
        }
        int rest = count - used;
        if (rest <= 0) return;
        PatternContents p = depth <= MAX_DEPTH ? patternFor(item) : null;
        if (p == null) add(missing, item, rest);
        else craft(p, rest, depth);
    }

    static void add(List<BinContents> list, ItemStack item, int count) {
        int i = DriveItem.indexOf(list, item);
        if (i < 0) list.add(new BinContents(item, count));
        else list.set(i, new BinContents(item, list.get(i).count() + count));
    }

    /** Takes {@code count} out of the list; false if it doesn't have that many. */
    static boolean remove(List<BinContents> list, ItemStack item, int count) {
        int i = DriveItem.indexOf(list, item);
        if (i < 0 || list.get(i).count() < count) return false;
        if (list.get(i).count() == count) list.remove(i);
        else list.set(i, new BinContents(item, list.get(i).count() - count));
        return true;
    }
}
