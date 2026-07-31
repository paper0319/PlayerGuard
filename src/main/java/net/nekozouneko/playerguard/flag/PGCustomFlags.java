package net.nekozouneko.playerguard.flag;

import com.sk89q.worldguard.protection.flags.SetFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;

/**
 * PlayerGuard が領域データ内に保存するカスタムフラグ。
 * {@code onLoad} で FlagRegistry に登録される(衝突時は既登録インスタンスへ差し替え)。
 * 単体テストでは未登録のままでも {@code region.setFlag} で値を保持できる。
 */
public final class PGCustomFlags {

    /** 主オーナーのUUID文字列。 */
    public static StringFlag PRIMARY_OWNER = new StringFlag("pg-primary-owner");
    /** Payment history serialized with the protected region. */
    public static StringFlag PAYMENT_RECORD = new StringFlag("pg-payment-record");
    /** 貸出エントリ "&lt;uuid&gt;:&lt;期限epochミリ秒&gt;" の集合。 */
    public static SetFlag<String> RENTALS = new SetFlag<>("pg-rentals", new StringFlag(null));

    private PGCustomFlags() {}
}
