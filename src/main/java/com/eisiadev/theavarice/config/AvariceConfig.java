package com.eisiadev.theavarice.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class AvariceConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CURRENCIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PICKUP_SOUNDS;
    public static final ForgeConfigSpec.ConfigValue<String> WALLET_TITLE;
    public static final ForgeConfigSpec.ConfigValue<String> WALLET_OPEN_SOUND;

    public static final ForgeConfigSpec.IntValue AVARICE_DROP_MERGE_LIMIT;
    public static final ForgeConfigSpec.IntValue AVARICE_MAX_DROP_ENTITIES;
    public static final ForgeConfigSpec.BooleanValue BLOCK_ANVIL_CURSE_REMOVAL;
    public static final ForgeConfigSpec.IntValue ANVIL_MAX_COST;

    public static final ForgeConfigSpec.DoubleValue UPGRADE_BASE_COST;
    public static final ForgeConfigSpec.DoubleValue UPGRADE_EXPONENT;
    public static final ForgeConfigSpec.DoubleValue UPGRADE_STEEPNESS;
    public static final ForgeConfigSpec.DoubleValue REFUND_RATIO;
    public static final ForgeConfigSpec.DoubleValue NATURAL_REFUND_RATIO;
    public static final ForgeConfigSpec.DoubleValue CURSE_BASE_COST;
    public static final ForgeConfigSpec.DoubleValue WEIGHT_COMMON;
    public static final ForgeConfigSpec.DoubleValue WEIGHT_UNCOMMON;
    public static final ForgeConfigSpec.DoubleValue WEIGHT_RARE;
    public static final ForgeConfigSpec.DoubleValue WEIGHT_VERY_RARE;
    public static final ForgeConfigSpec.DoubleValue TREASURE_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue UPGRADE_MAX_LEVEL;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> LEVEL_CAP_OVERRIDES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> COST_OVERRIDES;

    public static final ForgeConfigSpec.BooleanValue COMPAT_AOA3_COINS;
    public static final ForgeConfigSpec.BooleanValue COMPAT_AOD3_TOKENS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        CURRENCIES = builder.defineListAllowEmpty("currencies", List.of(
                "gtceu:gtcoin_copper, 1",
                "gtceu:gtcoin_bronze, 100",
                "gtceu:gtcoin_silver, 10000",
                "gtceu:gtcoin_gold, 1000000",
                "gtceu:gtcoin_diamond, 100000000",
                "gtceu:gtcoin_emerald, 10000000000"
        ), o -> o instanceof String);
        PICKUP_SOUNDS = builder.defineListAllowEmpty("pickupSounds", List.of(
                "minecraft:entity.horse.saddle, 1.0, 1.0"
        ), o -> o instanceof String);
        WALLET_TITLE = builder.define("walletTitle", "");
        WALLET_OPEN_SOUND = builder.define("walletOpenSound",
                "minecraft:item.armor.equip_leather, 1.0, 1.0");
        AVARICE_DROP_MERGE_LIMIT = builder
                .comment("탐욕으로 불어난 드롭을 아이템 엔티티 하나에 최대 몇 개까지 합칠지.",
                        "0 이면 아이템 자체의 최대 묶음 크기를 따른다.")
                .defineInRange("avariceDropMergeLimit", 1_048_576, 0, Integer.MAX_VALUE);
        AVARICE_MAX_DROP_ENTITIES = builder
                .comment("탐욕 드롭 하나가 만들 수 있는 아이템 엔티티 최대 개수. 넘는 분량은 버려진다.")
                .defineInRange("avariceMaxDropEntities", 64, 1, 4096);
        ANVIL_MAX_COST = builder
                .comment("모루 작업의 최대 경험치 레벨 비용. 이 값으로 상한이 걸려 \"너무 비쌉니다\"가 뜨지 않는다.",
                        "고레벨 인챈트 아이템도 수리/이름 변경/인챈트 옮기기가 가능해진다. -1 이면 바닐라 그대로.")
                .defineInRange("anvilMaxCost", 39, -1, 1_000_000);

        builder.comment("화폐로 인챈트 레벨을 올리고 내리는 기능 (지갑 → 인챈트 버튼).",
                "단계 비용 step(L) = baseCost × 가중치 × L^exponent × 성장배율 × 급등배율",
                "성장배율은 costOverrides 에 지정한 인챈트만 growth^(L-1) 로 붙는다 (기본: 탐욕 1.5).",
                "급등배율은 max(바닐라 최대 레벨, 상한/2) 를 넘는 구간에서 상한에 다가갈수록 1 → steepness 까지 지수적으로 커진다.",
                "올리기 비용 = 현재+1 ~ 목표 레벨 단계 비용의 합. 내리기는 같은 금액에 환급 비율을 곱해 내림(floor)한 만큼 돌려준다.",
                "모든 금액 단위는 최하위 화폐 (기본 설정에서 구리 동전 1).")
                .push("enchantUpgrade");
        UPGRADE_BASE_COST = builder
                .comment("레벨 1 단계의 기본 비용. 기본 6750 = 체력 20 좀비 50마리의 기본 화폐 드롭량",
                        "(좀비 1마리 = floor(20 × 6.5 × (1 + 0.01 × √20)) = 135).")
                .defineInRange("baseCost", 6750.0, 0.0, 1.0e15);
        UPGRADE_EXPONENT = builder
                .comment("단계 비용의 레벨 지수. 클수록 고레벨이 비싸진다.")
                .defineInRange("exponent", 2.0, 0.0, 10.0);
        UPGRADE_STEEPNESS = builder
                .comment("상한에 도달했을 때 단계 비용에 곱해지는 급등 배율. 1 이면 급등 없음.")
                .defineInRange("steepness", 1000.0, 1.0, 1.0e12);
        REFUND_RATIO = builder
                .comment("화폐로 구매한 레벨을 내릴 때 환급 비율.")
                .defineInRange("refundRatio", 0.9, 0.0, 1.0);
        NATURAL_REFUND_RATIO = builder
                .comment("자연 획득(루팅/인챈트 테이블 등) 레벨을 내릴 때 환급 비율.",
                        "비용이 크게 오른 만큼 낮게 둔다. 높이면 인챈트 테이블 책을 내려서 돈을 버는 루프가 생긴다.")
                .defineInRange("naturalRefundRatio", 0.005, 0.0, 1.0);
        CURSE_BASE_COST = builder
                .comment("저주 1레벨 제거 비용의 기본값. 실제 비용 = curseBaseCost × 가중치 × L^exponent.",
                        "기본 100000 × 귀속/소실 저주 가중치 16 = 1,600,000 (금화 1.6개, 기본 좀비 약 11,850마리 분량).")
                .defineInRange("curseBaseCost", 100_000.0, 0.0, 1.0e15);
        WEIGHT_COMMON = builder.defineInRange("weightCommon", 1.0, 0.0, 1.0e6);
        WEIGHT_UNCOMMON = builder.defineInRange("weightUncommon", 2.0, 0.0, 1.0e6);
        WEIGHT_RARE = builder.defineInRange("weightRare", 4.0, 0.0, 1.0e6);
        WEIGHT_VERY_RARE = builder.defineInRange("weightVeryRare", 8.0, 0.0, 1.0e6);
        TREASURE_MULTIPLIER = builder
                .comment("보물 인챈트(수선, 탐욕, 저주 등) 가중치 배율.")
                .defineInRange("treasureMultiplier", 2.0, 0.0, 1.0e6);
        UPGRADE_MAX_LEVEL = builder
                .comment("업그레이드로 도달할 수 있는 최대 레벨 (전체 공통 상한).")
                .defineInRange("maxLevel", 32767, 1, 32767);
        LEVEL_CAP_OVERRIDES = builder
                .comment("인챈트별 업그레이드 상한 덮어쓰기. 형식: \"modid:enchant=레벨\".",
                        "기본 하드코드 상한(급류 7, 빠른 장전 5, 미끼 5, 물갈퀴 3, 수선 1 등)을 바꾸거나 모드 인챈트에 상한을 추가할 때 사용.",
                        "0 이면 업그레이드 금지 (내리기만 가능).")
                .defineListAllowEmpty("levelCaps", List.of(), o -> o instanceof String);
        COST_OVERRIDES = builder
                .comment("인챈트별 비용 조정. 형식: \"modid:enchant=배수,성장\".",
                        "배수는 단계 비용 전체에 곱해지고, 성장(1 이상)은 레벨마다 한 번씩 더 곱해진다 (step × 성장^(L-1)).",
                        "탐욕은 화폐 드롭까지 불려 스스로 돈을 버는 인챈트라 기본으로 1.5 성장을 준다.")
                .defineListAllowEmpty("costOverrides", List.of("theavarice:avarice=1.0,1.5"), o -> o instanceof String);
        BLOCK_ANVIL_CURSE_REMOVAL = builder
                .comment("모루에서 저주가 사라지는 결과를 막는다 (예: Apotheosis 프리즘 거미줄).",
                        "저주 제거를 화폐 지출로만 가능하게 유지하려면 true.")
                .define("blockAnvilCurseRemoval", true);
        builder.pop();

        builder.comment("다른 모드의 화폐를 지갑에서 함께 다루는 선택적 호환성. 해당 모드가 없으면 무시된다.")
                .push("compat");
        COMPAT_AOA3_COINS = builder
                .comment("Advent of Ascension 3 의 코인(구리 → 은 → 금 → 루나버 → 라낙스 → 솔데나 → 이클립시아 → 크레아데, 20:1)을",
                        "지갑의 '코인' 탭에서 다룬다.")
                .define("aoa3Coins", true);
        COMPAT_AOD3_TOKENS = builder
                .comment("Advent of Descension 의 지역별 토큰(기본 → 정제된 → 고대의 → 강화된 → 승천한, 100:1)을",
                        "지갑의 '토큰' 탭에서 다룬다.")
                .define("aod3Tokens", true);
        builder.pop();

        SPEC = builder.build();
    }

    private AvariceConfig() {}
}
