package net.nekozouneko.playerguard.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;

public enum ProtectionGuideTopic {
    ABOUT(Material.SHIELD, ChatColor.YELLOW + "土地保護とは", new String[]{"自分の建物や土地を", "他人の破壊や無断操作から", "守るための機能です。"}, new String[]{"土地保護を作ると、範囲内の破壊や設置などを保護ごとに管理できます。", "自分の保護は /pg から一覧で確認できます。", "メンバーやサブオーナーを追加して共同管理もできます。"}),
    AXE_USAGE(Material.GOLDEN_AXE, ChatColor.GOLD + "金の斧の使い方", new String[]{"1点目を右クリック", "2点目を右クリック", "最後に確認して作成します。"}, new String[]{"金の斧で保護したい範囲の角を2点、どちらも右クリックで選択します。", "2点目を選ぶと料金と範囲の確認が表示されます。", "確認するまで課金や保護作成は行われません。"}),
    RANGE(Material.COMPASS, ChatColor.AQUA + "保護範囲の選び方", new String[]{"建物全体が入るように", "対角線上の2点を", "選択してください。"}, new String[]{"地下や屋根も含める場合は高さも考えて選んでください。", "広すぎる範囲は無料枠を多く消費します。", "他の保護と重なる範囲は作成できません。"}),
    CONFIRMATION(Material.OAK_DOOR, ChatColor.GREEN + "保護作成の流れ", new String[]{"範囲を選択したら", "内容と料金を確認し", "最後に作成します。"}, new String[]{"確認方式はGUI確認とチャット確認から選べます。", "チャット確認では /claim で作成、/cancel でキャンセルします。", "確定直前に料金や競合を再確認します。"}),
    CREATE(Material.CRAFTING_TABLE, ChatColor.GREEN + "保護作成", new String[]{"選択した範囲を", "内容を確認して", "保護として作成します。"}, new String[]{"金の斧で範囲を選択した後、/claim で保護作成を開始します。", "確認画面で料金と範囲を確認してから作成してください。"}),
    FREE_LIMIT(Material.EMERALD_BLOCK, ChatColor.GREEN + "無料保護上限", new String[]{"無料上限までは0円です。", "超過分だけ料金が", "発生します。"}, new String[]{"無料上限はランクや権限に応じて決まります。", "作成済み保護の総体積を基準に計算します。", "確認画面で無料対象と有料対象を確認できます。"}),
    RATES(Material.EMERALD, ChatColor.GREEN + "有料保護の価格帯", new String[]{"無料枠を超えた分は", "現在の価格帯に応じて", "料金が発生します。"}, new String[]{"価格は段階的に計算されます。", "無料枠内の体積には料金がかかりません。", "現在適用中の価格帯は下に表示されます。"}),    PAYMENT(Material.GOLD_INGOT, ChatColor.GOLD + "支払い方法", new String[]{"保護作成を確定した時に", "必要金額だけ", "支払います。"}, new String[]{"確認画面を開いただけでは支払いません。", "確定前に所持金と料金をもう一度確認します。", "作成に失敗した場合は支払いを取り消します。"}),
    REFUND(Material.CLOCK, ChatColor.AQUA + "削除時の返金制度", new String[]{"実際の支払額を基準に", "経過時間ごとの率で", "返金します。"}, new String[]{"30分以内は誤操作救済として100%返金です。", "30分超～7日は80%、7日超は50%返金です。", "現在のオーナーではなく実際の支払者へ返金され、管理者無料土地の返金は0円です。"}), MEMBERS(Material.PLAYER_HEAD, ChatColor.GREEN + "メンバー", new String[]{"保護内で操作できる", "プレイヤーを", "管理できます。"}, new String[]{"メンバー管理から追加や削除ができます。", "権限設定により操作範囲を調整できます。", "削除や返金は正式なオーナーだけが実行できます。"}),
    SUBOWNERS(Material.GOLDEN_HELMET, ChatColor.YELLOW + "サブオーナー", new String[]{"一部の管理を", "任せられる", "役割です。"}, new String[]{"サブオーナーはメンバーを管理できます。", "正式なオーナー専用操作は実行できません。", "削除や返金はオーナーだけが実行できます。"}),
    FLAGS(Material.REDSTONE_TORCH, ChatColor.RED + "フラグ", new String[]{"PVPや爆発などを", "保護ごとに", "切り替えます。"}, new String[]{"フラグ管理から保護ごとの動作を変更できます。", "変更した内容はすぐに反映されます。", "変更内容は保護ログに記録されます。"}),
    PERMISSIONS(Material.COMPARATOR, ChatColor.YELLOW + "権限", new String[]{"建築や操作などの", "許可範囲を", "管理します。"}, new String[]{"権限管理からメンバーの操作範囲を設定します。", "メンバーごとに必要な操作だけを許可できます。", "一般プレイヤーは自分の保護を管理できます。"}),
    CATEGORY(Material.ITEM_FRAME, ChatColor.YELLOW + "保護カテゴリ", new String[]{"自宅やショップなど", "用途別に分けて", "見つけやすくします。"}, new String[]{"保護カスタマイズからカテゴリを変更できます。", "選んだカテゴリは保護データに保存されます。", "一覧や詳細画面にも表示されます。"}),
    TELEPORT(Material.ENDER_PEARL, ChatColor.AQUA + "保護テレポート", new String[]{"保護内に移動地点を", "1か所だけ設定できます。", "作成はオーナーのみです。"}, new String[]{"地点を作成・更新・削除できるのは正式なオーナーだけです。", "メンバーとサブオーナーは、設定済み地点への移動だけできます。", "危険な地点には移動できません。"}),
    LOGS(Material.BOOK, ChatColor.YELLOW + "保護ログ", new String[]{"訪問や操作履歴を", "カテゴリ別に", "確認できます。"}, new String[]{"メンバー変更、フラグ変更、支払い、返金などを記録します。", "ログはカテゴリ別に確認できます。", "訪問ログは件数上限に応じて古いものから整理されます。"}),
    DELETE(Material.BARRIER, ChatColor.RED + "保護削除", new String[]{"削除前に必ず", "確認画面で返金額を", "確認します。"}, new String[]{"一覧から即削除はできません。", "正式なオーナーだけが削除できます。", "返金に失敗した場合、保護は削除されません。"}),
    FAQ(Material.WRITABLE_BOOK, ChatColor.YELLOW + "よくある質問", new String[]{"作成できない時は", "範囲・重なり・残高を", "確認してください。"}, new String[]{"他の保護と重なる範囲は作成できません。", "有料保護では残高不足の場合も作成できません。", "入場できない保護へ入ると「その土地には入れません」と表示され、外側へ出されます。", "困った場合は保護IDを添えて管理者へ相談してください。"});

    public final Material material;
    public final String title;
    public final String[] summary;
    public final String[] detail;

    ProtectionGuideTopic(Material material, String title, String[] summary, String[] detail) {
        this.material = material;
        this.title = title;
        this.summary = summary;
        this.detail = detail;
    }
}
