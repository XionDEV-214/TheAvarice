package com.eisiadev.theavarice.enchant;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EnchantNbt {

    public static final String TAG_ROOT = "TheAvarice";
    public static final String TAG_PURCHASED = "Purchased";
    private static final String TAG_ENCHANTMENTS = "Enchantments";
    private static final String TAG_STORED = "StoredEnchantments";
    private static final String KEY_AMOUNT = "Amount";
    private static final String KEY_LEVEL = "Level";

    private EnchantNbt() {}

    public static boolean isBook(ItemStack stack) {
        return stack.getItem() instanceof EnchantedBookItem;
    }

    public static String listKey(ItemStack stack) {
        return isBook(stack) ? TAG_STORED : TAG_ENCHANTMENTS;
    }

    public static LinkedHashMap<ResourceLocation, Integer> read(ItemStack stack) {
        return readList(stack, listKey(stack));
    }

    public static Map<ResourceLocation, Integer> readAll(ItemStack stack) {
        Map<ResourceLocation, Integer> out = readList(stack, TAG_ENCHANTMENTS);
        readList(stack, TAG_STORED).forEach((id, lv) -> out.merge(id, lv, Math::max));
        return out;
    }

    private static LinkedHashMap<ResourceLocation, Integer> readList(ItemStack stack, String key) {
        LinkedHashMap<ResourceLocation, Integer> out = new LinkedHashMap<>();
        CompoundTag tag = stack.getTag();
        if (stack.isEmpty() || tag == null || !tag.contains(key, Tag.TAG_LIST)) return out;
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ResourceLocation id = EnchantmentHelper.getEnchantmentId(entry);
            if (id == null) continue;
            out.remove(id);
            out.put(id, EnchantmentHelper.getEnchantmentLevel(entry));
        }
        return out;
    }

    public static void writeLevels(ItemStack stack, Map<ResourceLocation, Integer> levels) {
        String key = listKey(stack);
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(key, Tag.TAG_LIST)) return;
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = list.size() - 1; i >= 0; i--) {
            CompoundTag entry = list.getCompound(i);
            ResourceLocation id = EnchantmentHelper.getEnchantmentId(entry);
            if (id == null || !levels.containsKey(id)) continue;
            int level = levels.get(id);
            if (level <= 0) {
                list.remove(i);
            } else {
                EnchantmentHelper.setEnchantmentLevel(entry, Math.min(level, CostMath.ABSOLUTE_MAX_LEVEL));
            }
        }
        if (list.isEmpty()) {
            tag.remove(key);
        } else {
            tag.put(key, list);
        }
    }

    public static ItemStack finish(ItemStack stack) {
        if (isBook(stack) && read(stack).isEmpty()) {
            ItemStack book = new ItemStack(Items.BOOK, stack.getCount());
            if (stack.hasCustomHoverName()) book.setHoverName(stack.getHoverName());
            return book;
        }
        sanitizePurchased(stack);
        return stack;
    }

    public static Map<ResourceLocation, Integer> readPurchased(ItemStack stack) {
        Map<ResourceLocation, Integer> out = new LinkedHashMap<>();
        CompoundTag root = stack.getTagElement(TAG_ROOT);
        if (root == null || !root.contains(TAG_PURCHASED, Tag.TAG_COMPOUND)) return out;
        CompoundTag purchased = root.getCompound(TAG_PURCHASED);
        Map<ResourceLocation, Integer> levels = read(stack);
        for (String key : purchased.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id == null || !purchased.contains(key, Tag.TAG_COMPOUND)) continue;
            CompoundTag entry = purchased.getCompound(key);
            int value = entry.getInt(KEY_AMOUNT);
            int level = levels.getOrDefault(id, 0);
            if (value > 0 && level > 0 && entry.getInt(KEY_LEVEL) == level) out.put(id, Math.min(value, level));
        }
        return out;
    }

    public static void writePurchased(ItemStack stack, Map<ResourceLocation, Integer> purchased) {
        Map<ResourceLocation, Integer> levels = read(stack);
        CompoundTag data = new CompoundTag();
        purchased.forEach((id, value) -> {
            int level = levels.getOrDefault(id, 0);
            if (value == null || value <= 0 || level <= 0) return;
            CompoundTag entry = new CompoundTag();
            entry.putInt(KEY_AMOUNT, Math.min(value, level));
            entry.putInt(KEY_LEVEL, level);
            data.put(id.toString(), entry);
        });
        CompoundTag root = stack.getTagElement(TAG_ROOT);
        if (data.isEmpty()) {
            if (root != null) {
                root.remove(TAG_PURCHASED);
                if (root.isEmpty()) stack.removeTagKey(TAG_ROOT);
            }
            return;
        }
        stack.getOrCreateTagElement(TAG_ROOT).put(TAG_PURCHASED, data);
    }

    public static void sanitizePurchased(ItemStack stack) {
        if (stack.isEmpty() || stack.getTagElement(TAG_ROOT) == null) return;
        writePurchased(stack, readPurchased(stack));
    }
}
